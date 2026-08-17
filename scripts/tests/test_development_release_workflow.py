import pathlib
import unittest


ROOT = pathlib.Path(__file__).parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "development-release.yml"
CI_WORKFLOW = ROOT / ".github" / "workflows" / "ci.yml"
BUILD = ROOT / "app" / "build.gradle.kts"


class DevelopmentReleaseWorkflowTest(unittest.TestCase):
    def test_uses_non_production_tag_and_prerelease(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn('tags: ["dev-v*"]', workflow)
        self.assertIn('"dev-v$(python3', workflow)
        self.assertIn("prerelease: true", workflow)
        self.assertNotIn('tags: ["v*"]', workflow)

    def test_verifies_both_artifacts_before_publication(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        ordered_gates = (
            "verify_development_artifact_signing.py",
            "sha256sum --check --strict",
            'bundletool.jar" validate --bundle',
            "Generate checksums",
            "attest-build-provenance",
            "action-gh-release",
        )
        positions = [workflow.index(gate) for gate in ordered_gates]
        self.assertEqual(sorted(positions), positions)

    def test_uses_development_signer_equality_without_weakening_production(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        production_workflow = (ROOT / ".github" / "workflows" / "release.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn("--apksigner", workflow)
        self.assertIn("--apk app/build/outputs/apk/development/app-development.apk", workflow)
        self.assertIn("--aab app/build/outputs/bundle/development/app-development.aab", workflow)
        self.assertNotIn("jarsigner -verify -strict", workflow)
        self.assertIn("jarsigner -verify -strict", production_workflow)

    def test_development_variant_is_isolated_and_debug_signed(self):
        build = BUILD.read_text(encoding="utf-8")
        self.assertIn('create("development")', build)
        self.assertIn('applicationIdSuffix = ".development"', build)
        self.assertIn('signingConfigs.getByName("debug")', build)
        self.assertIn('orElse("http://127.0.0.1:8000")', build)

    def test_pull_request_ci_builds_apk_and_aab(self):
        workflow = CI_WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("lintDevelopment", workflow)
        self.assertIn("assembleDevelopment", workflow)
        self.assertIn("bundleDevelopment", workflow)


if __name__ == "__main__":
    unittest.main()
