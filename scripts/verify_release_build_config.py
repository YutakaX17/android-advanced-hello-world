#!/usr/bin/env python3
"""Verify the API endpoint embedded in a generated Android BuildConfig."""

from __future__ import annotations

import argparse
import pathlib
import sys


DEBUG_EMULATOR_URL = "http://10.0.2.2:8000"


def java_string_literal(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def verify_build_config(
    source: str,
    expected_endpoint: str,
    forbidden_endpoints: tuple[str, ...] = (),
) -> None:
    expected_declaration = (
        "public static final String API_BASE_URL = "
        f"{java_string_literal(expected_endpoint)};"
    )
    if expected_declaration not in source:
        raise ValueError("generated BuildConfig does not embed the configured endpoint exactly")
    for forbidden_endpoint in forbidden_endpoints:
        if forbidden_endpoint in source:
            raise ValueError(f"generated BuildConfig contains forbidden endpoint: {forbidden_endpoint}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-config", type=pathlib.Path, required=True)
    parser.add_argument("--expected-url", required=True)
    parser.add_argument("--forbid-url", action="append", default=[])
    args = parser.parse_args()

    try:
        verify_build_config(
            args.build_config.read_text(encoding="utf-8"),
            args.expected_url,
            tuple(args.forbid_url),
        )
    except (OSError, ValueError) as error:
        print(f"BuildConfig verification failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
