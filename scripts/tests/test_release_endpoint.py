import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "validate_release_endpoint.py"
SPEC = importlib.util.spec_from_file_location("validate_release_endpoint", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ReleaseEndpointValidationTest(unittest.TestCase):
    def test_accepts_absolute_https_endpoint(self):
        self.assertEqual(
            "https://api.example.test/v1",
            MODULE.validate_release_endpoint("https://api.example.test/v1"),
        )

    def test_rejects_noncanonical_endpoint(self):
        for endpoint in (
            " https://api.example.test/v1",
            "https://api.example.test/v1 ",
            "https://api.example.test/v1/",
        ):
            with self.subTest(endpoint=endpoint):
                with self.assertRaisesRegex(ValueError, "canonical"):
                    MODULE.validate_release_endpoint(endpoint)

    def test_rejects_missing_endpoint(self):
        with self.assertRaisesRegex(ValueError, "required"):
            MODULE.validate_release_endpoint("  ")

    def test_rejects_malformed_endpoint(self):
        with self.assertRaisesRegex(ValueError, "absolute URL"):
            MODULE.validate_release_endpoint("https:///messages")

    def test_rejects_non_https_endpoint(self):
        with self.assertRaisesRegex(ValueError, "HTTPS"):
            MODULE.validate_release_endpoint("http://10.0.2.2:8000")

    def test_rejects_credentials(self):
        with self.assertRaisesRegex(ValueError, "credentials"):
            MODULE.validate_release_endpoint("https://user:secret@example.test")


if __name__ == "__main__":
    unittest.main()
