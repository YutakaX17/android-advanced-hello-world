import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).parents[1] / "generate_release_metadata.py"
SPEC = importlib.util.spec_from_file_location("generate_release_metadata", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ReleaseMetadataTest(unittest.TestCase):
    def test_records_revision_and_component_versions(self):
        manifest = {
            "distributionVersion": "0.2.0",
            "modules": [
                {"id": "messages", "coordinate": "example:messages", "version": "0.2.0"},
            ],
        }
        metadata = MODULE.generate(manifest, "abc123")
        self.assertEqual("abc123", metadata["revision"])
        self.assertEqual("example:messages", metadata["components"][0]["coordinate"])


if __name__ == "__main__":
    unittest.main()
