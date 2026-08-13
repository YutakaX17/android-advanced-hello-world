#!/usr/bin/env python3
"""Verify that generated release configuration contains only the release endpoint."""

from __future__ import annotations

import argparse
import pathlib
import sys


DEBUG_EMULATOR_URL = "http://10.0.2.2:8000"


def java_string_literal(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def verify_build_config(source: str, expected_endpoint: str) -> None:
    expected_declaration = (
        "public static final String API_BASE_URL = "
        f"{java_string_literal(expected_endpoint)};"
    )
    if expected_declaration not in source:
        raise ValueError("generated release BuildConfig does not embed the configured endpoint exactly")
    if DEBUG_EMULATOR_URL in source:
        raise ValueError("generated release BuildConfig contains the debug emulator endpoint")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-config", type=pathlib.Path, required=True)
    parser.add_argument("--expected-url", required=True)
    args = parser.parse_args()

    try:
        verify_build_config(
            args.build_config.read_text(encoding="utf-8"),
            args.expected_url,
        )
    except (OSError, ValueError) as error:
        print(f"release BuildConfig verification failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
