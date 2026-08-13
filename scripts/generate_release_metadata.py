#!/usr/bin/env python3
"""Generate deterministic release component metadata."""

from __future__ import annotations

import argparse
import json
import pathlib


def generate(manifest: dict, revision: str) -> dict:
    components = [
        {
            "id": module["id"],
            "coordinate": module["coordinate"],
            "version": module["version"],
        }
        for module in manifest["modules"]
    ]
    return {
        "schemaVersion": 1,
        "distribution": "android-advanced-hello-world",
        "version": manifest["distributionVersion"],
        "revision": revision,
        "components": components,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=pathlib.Path, required=True)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--metadata", type=pathlib.Path, required=True)
    args = parser.parse_args()
    metadata = generate(json.loads(args.manifest.read_text()), args.revision)
    args.metadata.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n")


if __name__ == "__main__":
    main()
