import pathlib
import unittest


WORKFLOW = pathlib.Path(__file__).parents[2] / ".github" / "workflows" / "integration.yml"


class IntegrationWorkflowTest(unittest.TestCase):
    def test_installs_pinned_modules_and_fails_fast_on_imports(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        install = "pip install --no-deps ../advanced-hello-world-be-core ../advanced-hello-world-be-messages"
        import_check = (
            "import advanced_hello_world_core.module, advanced_hello_world_messages.module"
        )
        manifest_check = "module_manifest modules.json --check-installed"
        database_start = "docker run --name advanced-hello-world-e2e-db"
        positions = [workflow.index(value) for value in (install, import_check, manifest_check, database_start)]
        self.assertEqual(sorted(positions), positions)
        self.assertNotIn("module_installer modules.json --local-root", workflow)


if __name__ == "__main__":
    unittest.main()
