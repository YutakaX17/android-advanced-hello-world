#!/usr/bin/env python3
"""Validate the public backend endpoint embedded in an Android release build."""

from __future__ import annotations

import argparse
import sys
from urllib.parse import urlsplit


def validate_release_endpoint(value: str) -> str:
    endpoint = value.strip()
    if not endpoint:
        raise ValueError(
            "releaseApiBaseUrl is required; supply -PreleaseApiBaseUrl=https://host"
        )
    if endpoint != value or endpoint.endswith("/"):
        raise ValueError(
            "releaseApiBaseUrl must be canonical: no surrounding whitespace or trailing slash"
        )

    parsed = urlsplit(endpoint)
    if parsed.scheme.lower() != "https":
        raise ValueError("releaseApiBaseUrl must use HTTPS")
    if not parsed.hostname:
        raise ValueError("releaseApiBaseUrl must be an absolute URL with a host")
    if parsed.username or parsed.password:
        raise ValueError("releaseApiBaseUrl must not contain credentials")
    if parsed.query or parsed.fragment:
        raise ValueError("releaseApiBaseUrl must not contain a query or fragment")

    try:
        parsed.port
    except ValueError as error:
        raise ValueError("releaseApiBaseUrl contains an invalid port") from error

    return endpoint


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    args = parser.parse_args()

    try:
        validate_release_endpoint(args.url)
    except ValueError as error:
        print(f"release endpoint validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
