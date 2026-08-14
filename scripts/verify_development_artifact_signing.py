#!/usr/bin/env python3
"""Verify self-signed development APK/AAB integrity and signer equality."""

from __future__ import annotations

import argparse
import pathlib
import re
import subprocess
import sys


APK_FINGERPRINT = re.compile(
    r"^Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)\s*$",
    re.MULTILINE,
)
AAB_FINGERPRINT = re.compile(
    r"^\s*SHA256:\s*([0-9a-fA-F:]+)\s*$",
    re.MULTILINE,
)


def normalize_fingerprint(value: str) -> str:
    normalized = value.replace(":", "").lower()
    if len(normalized) != 64 or not re.fullmatch(r"[0-9a-f]{64}", normalized):
        raise ValueError("signer SHA-256 fingerprint is malformed")
    return normalized


def extract_fingerprint(output: str, pattern: re.Pattern[str], artifact: str) -> str:
    match = pattern.search(output)
    if not match:
        raise ValueError(f"{artifact} signer SHA-256 fingerprint is missing")
    return normalize_fingerprint(match.group(1))


def verify_signer_outputs(
    apk_output: str,
    aab_verification_output: str,
    aab_certificate_output: str,
) -> None:
    if not re.search(
        r"^jar verified(?:\.|, with signer errors\.)$",
        aab_verification_output,
        re.MULTILINE,
    ):
        raise ValueError("AAB integrity verification did not report a verified archive")

    apk_fingerprint = extract_fingerprint(apk_output, APK_FINGERPRINT, "APK")
    aab_fingerprint = extract_fingerprint(aab_certificate_output, AAB_FINGERPRINT, "AAB")
    if apk_fingerprint != aab_fingerprint:
        raise ValueError("APK and AAB signer SHA-256 fingerprints differ")


def run(command: list[str]) -> str:
    completed = subprocess.run(
        command,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    print(completed.stdout, end="")
    return completed.stdout


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apksigner", type=pathlib.Path, required=True)
    parser.add_argument("--apk", type=pathlib.Path, required=True)
    parser.add_argument("--aab", type=pathlib.Path, required=True)
    args = parser.parse_args()

    try:
        apk_output = run(
            [str(args.apksigner), "verify", "--verbose", "--print-certs", str(args.apk)]
        )
        aab_verification_output = run(
            ["jarsigner", "-verify", "-verbose", str(args.aab)]
        )
        aab_certificate_output = run(["keytool", "-printcert", "-jarfile", str(args.aab)])
        verify_signer_outputs(
            apk_output,
            aab_verification_output,
            aab_certificate_output,
        )
    except (OSError, subprocess.CalledProcessError, ValueError) as error:
        print(f"development signing verification failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
