import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "generate_feature_registry.py"
SPEC = importlib.util.spec_from_file_location("module_registry", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ModuleRegistryTest(unittest.TestCase):
    def test_manifest_matches_schema_and_version_catalog(self):
        manifest = MODULE.load_and_validate()
        self.assertEqual("messages", manifest["startFeatureId"])
        self.assertEqual(4, len(manifest["modules"]))

    def test_generation_is_deterministic_and_current(self):
        manifest = MODULE.load_and_validate()
        self.assertEqual(MODULE.render(manifest), MODULE.OUTPUT.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
