import pathlib
import unittest


WORKFLOW = pathlib.Path(__file__).parents[2] / ".github" / "workflows" / "release.yml"


class ReleaseWorkflowTest(unittest.TestCase):
    def test_verifies_both_artifacts_before_release_outputs(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        ordered_gates = (
            "apksigner\" verify --verbose",
            "jarsigner -verify -strict -verbose",
            "sha256sum --check --strict",
            'bundletool.jar" validate --bundle',
            "Generate checksums",
            "attest-build-provenance",
            "action-gh-release",
        )
        positions = [workflow.index(gate) for gate in ordered_gates]
        self.assertEqual(sorted(positions), positions)

    def test_bundletool_download_is_version_and_digest_pinned(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar", workflow)
        self.assertIn(
            "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
