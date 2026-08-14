import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "verify_development_artifact_signing.py"
SPEC = importlib.util.spec_from_file_location("verify_development_artifact_signing", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)

FINGERPRINT = "0123456789abcdef" * 4


class DevelopmentArtifactSigningTest(unittest.TestCase):
    def test_accepts_verified_artifacts_with_same_signer(self):
        MODULE.verify_signer_outputs(
            f"Signer #1 certificate SHA-256 digest: {FINGERPRINT}\n",
            "jar verified, with signer errors.\n",
            "Certificate fingerprints:\n\t SHA256: "
            + ":".join(FINGERPRINT[index : index + 2] for index in range(0, 64, 2)).upper()
            + "\n",
        )

    def test_rejects_unverified_aab(self):
        with self.assertRaisesRegex(ValueError, "integrity verification"):
            MODULE.verify_signer_outputs(
                f"Signer #1 certificate SHA-256 digest: {FINGERPRINT}\n",
                "jar is unsigned.\n",
                f"SHA256: {FINGERPRINT}\n",
            )

    def test_rejects_different_signers(self):
        with self.assertRaisesRegex(ValueError, "fingerprints differ"):
            MODULE.verify_signer_outputs(
                f"Signer #1 certificate SHA-256 digest: {FINGERPRINT}\n",
                "jar verified.\n",
                f"SHA256: {'f' * 64}\n",
            )

    def test_rejects_missing_or_malformed_fingerprints(self):
        with self.assertRaisesRegex(ValueError, "missing"):
            MODULE.extract_fingerprint("", MODULE.APK_FINGERPRINT, "APK")
        with self.assertRaisesRegex(ValueError, "malformed"):
            MODULE.normalize_fingerprint("abcd")


if __name__ == "__main__":
    unittest.main()
