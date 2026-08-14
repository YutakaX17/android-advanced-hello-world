import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "verify_release_build_config.py"
SPEC = importlib.util.spec_from_file_location("verify_release_build_config", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ReleaseBuildConfigVerificationTest(unittest.TestCase):
    def test_accepts_exact_release_endpoint_without_debug_url(self):
        MODULE.verify_build_config(
            'public static final String API_BASE_URL = "https://api.example.test/v1";',
            "https://api.example.test/v1",
        )

    def test_rejects_different_embedded_endpoint(self):
        with self.assertRaisesRegex(ValueError, "exactly"):
            MODULE.verify_build_config(
                'public static final String API_BASE_URL = "https://other.example.test";',
                "https://api.example.test",
            )

    def test_rejects_debug_emulator_url(self):
        with self.assertRaisesRegex(ValueError, "forbidden endpoint"):
            MODULE.verify_build_config(
                'public static final String API_BASE_URL = "https://api.example.test"; '
                + MODULE.DEBUG_EMULATOR_URL,
                "https://api.example.test",
                (MODULE.DEBUG_EMULATOR_URL,),
            )


if __name__ == "__main__":
    unittest.main()
