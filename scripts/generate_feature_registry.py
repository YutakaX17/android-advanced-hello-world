#!/usr/bin/env python3
"""Validate modules.json and deterministically generate the Kotlin feature registry."""

from __future__ import annotations

import argparse
import json
import re
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "modules.json"
SCHEMA = ROOT / "modules.schema.json"
CATALOG = ROOT / "gradle" / "libs.versions.toml"
OUTPUT = ROOT / "app" / "src" / "main" / "kotlin" / "io" / "github" / "yutakax17" / "advancedhelloworld" / "android" / "GeneratedFeatureRegistry.kt"
ID_PATTERN = re.compile(r"^[a-z][a-z0-9-]*$")
COORDINATE_PATTERN = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_.-]+$")
VERSION_PATTERN = re.compile(r"^\d+\.\d+\.\d+$")


def load_and_validate() -> dict:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
    if schema.get("$schema") != "https://json-schema.org/draft/2020-12/schema":
        raise ValueError("modules.schema.json must declare JSON Schema draft 2020-12")
    required = {"$schema", "schemaVersion", "distributionVersion", "featureContractVersion", "startFeatureId", "modules"}
    if set(manifest) != required:
        raise ValueError(f"manifest keys must be exactly {sorted(required)}")
    if manifest["$schema"] != "./modules.schema.json" or manifest["schemaVersion"] != 1:
        raise ValueError("unsupported module manifest schema")
    if not VERSION_PATTERN.fullmatch(manifest["distributionVersion"]):
        raise ValueError("distributionVersion must be semantic x.y.z")
    if not isinstance(manifest["featureContractVersion"], int) or manifest["featureContractVersion"] < 1:
        raise ValueError("featureContractVersion must be a positive integer")
    modules = manifest["modules"]
    if not isinstance(modules, list) or not modules:
        raise ValueError("modules must be a non-empty array")
    ids: set[str] = set()
    coordinates: set[str] = set()
    features: set[str] = set()
    for module in modules:
        base = {"id", "kind", "coordinate", "version"}
        allowed = base | ({"factoryClass", "dependenciesClass"} if module.get("kind") == "feature" else set())
        if set(module) != allowed:
            raise ValueError(f"invalid fields for module {module.get('id')}")
        if not ID_PATTERN.fullmatch(module["id"]):
            raise ValueError(f"invalid module id: {module['id']}")
        if module["kind"] not in {"foundation", "domain", "feature"}:
            raise ValueError(f"invalid module kind: {module['kind']}")
        if not COORDINATE_PATTERN.fullmatch(module["coordinate"]):
            raise ValueError(f"invalid coordinate: {module['coordinate']}")
        if not VERSION_PATTERN.fullmatch(module["version"]):
            raise ValueError(f"invalid version for {module['id']}")
        if module["id"] in ids or module["coordinate"] in coordinates:
            raise ValueError("module ids and coordinates must be unique")
        ids.add(module["id"])
        coordinates.add(module["coordinate"])
        if module["kind"] == "feature":
            if not module["factoryClass"].rsplit(".", 1)[-1].endswith("Factory"):
                raise ValueError(f"feature factory must end in Factory: {module['id']}")
            if not module["dependenciesClass"].rsplit(".", 1)[-1].endswith("Dependencies"):
                raise ValueError(f"feature dependencies must end in Dependencies: {module['id']}")
            features.add(module["id"])
    if manifest["startFeatureId"] not in features:
        raise ValueError("startFeatureId must identify a declared feature")
    catalog = tomllib.loads(CATALOG.read_text(encoding="utf-8"))
    catalog_versions = catalog["versions"]
    if manifest["distributionVersion"] != catalog_versions["distribution"]:
        raise ValueError("distributionVersion must match the distribution version catalog")
    family_libraries = {
        library["module"]: catalog_versions[library["version"]["ref"]]
        for name, library in catalog["libraries"].items()
        if name.startswith("advanced-hello-world-")
    }
    for module in modules:
        expected_version = family_libraries.get(module["coordinate"])
        if module["version"] != expected_version:
            raise ValueError(f"module version does not match the version catalog: {module['id']}")
    catalog_coordinates = set(family_libraries)
    if coordinates != catalog_coordinates:
        raise ValueError("module coordinates must exactly match the version catalog")
    return manifest


def render(manifest: dict) -> str:
    features = [module for module in manifest["modules"] if module["kind"] == "feature"]
    feature_imports = "\n".join(
        f"import {class_name}"
        for class_name in sorted(
            class_name
            for module in features
            for class_name in (module["dependenciesClass"], module["factoryClass"])
        )
    )
    parameters = ",\n".join(
        f'        {module["id"].replace("-", "_")}Dependencies: {module["dependenciesClass"].rsplit(".", 1)[-1]}'
        for module in features
    )
    factories = "\n".join(
        f'        {module["factoryClass"].rsplit(".", 1)[-1]}.create({module["id"].replace("-", "_")}Dependencies),'
        for module in features
    )
    descriptors = "\n".join(
        "\n".join(
            [
                "        ModuleDescriptor(",
                f'            id = "{module["id"]}",',
                f'            kind = "{module["kind"]}",',
                f'            coordinate = "{module["coordinate"]}",',
                f'            version = "{module["version"]}",',
                "        ),",
            ]
        )
        for module in manifest["modules"]
    )
    return f'''package io.github.yutakax17.advancedhelloworld.android

import io.github.yutakax17.advancedhelloworld.compose.core.FeatureDestination
import io.github.yutakax17.advancedhelloworld.compose.core.FeatureUi
{feature_imports}

public data class ModuleDescriptor(
    public val id: String,
    public val kind: String,
    public val coordinate: String,
    public val version: String,
)

public object GeneratedFeatureRegistry {{
    public const val FEATURE_CONTRACT_VERSION: Int = {manifest["featureContractVersion"]}
    public const val START_FEATURE_ID: String = "{manifest["startFeatureId"]}"

    public val modules: List<ModuleDescriptor> = listOf(
{descriptors}
    )

    public fun createFeatures(
{parameters},
    ): List<FeatureUi> = listOf(
{factories}
    )

    public fun startDestination(features: List<FeatureUi>): FeatureDestination {{
        require(features.map(FeatureUi::id).distinct().size == features.size) {{
            "feature ids must be unique"
        }}
        val feature = features.singleOrNull {{ it.id == START_FEATURE_ID }}
            ?: error("missing start feature: $START_FEATURE_ID")
        require(feature.contractVersion == FEATURE_CONTRACT_VERSION) {{
            "incompatible feature UI contract"
        }}
        return feature.destinations.firstOrNull()
            ?: error("start feature has no destinations: $START_FEATURE_ID")
    }}
}}
'''


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        generated = render(load_and_validate())
    except (KeyError, TypeError, ValueError, json.JSONDecodeError) as error:
        print(f"module manifest validation failed: {error}", file=sys.stderr)
        return 1
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != generated:
            print("GeneratedFeatureRegistry.kt is stale; run scripts/generate_feature_registry.py", file=sys.stderr)
            return 1
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(generated, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
