import pathlib
import unittest


WORKFLOW = pathlib.Path(__file__).parents[2] / ".github" / "workflows" / "integration.yml"
DEVICE_WAIT = pathlib.Path(__file__).parents[2] / "scripts" / "wait_for_android_device.sh"


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

    def test_authenticates_package_resolution_and_bounds_diagnostics(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("GITHUB_ACTOR: ${{ github.actor }}", workflow)
        self.assertIn("GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}", workflow)
        self.assertIn("timeout-minutes: 2", workflow)
        self.assertIn("timeout 20s adb logcat -d", workflow)
        self.assertIn("timeout 20s docker logs", workflow)

    def test_bridges_emulator_loopback_to_stable_backend(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runserver --noreload 0.0.0.0:8000", workflow)
        wait = "scripts/wait_for_android_device.sh"
        reverse = "adb reverse tcp:8000 tcp:8000"
        build = "-PdebugApiBaseUrl=http://127.0.0.1:8000 connectedDebugAndroidTest"
        positions = [workflow.index(value) for value in (wait, reverse, build)]
        self.assertEqual(sorted(positions), positions)

    def test_bounds_emulator_boot_and_recovers_offline_adb(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        wait_script = DEVICE_WAIT.read_text(encoding="utf-8")
        self.assertIn("api-level: 34", workflow)
        self.assertIn("emulator-boot-timeout: 300", workflow)
        self.assertIn("# v2.38.0", workflow)
        self.assertIn("readonly max_attempts=3", wait_script)
        self.assertIn('timeout "${wait_seconds}s" adb wait-for-device', wait_script)
        self.assertIn("adb reconnect offline", wait_script)
        self.assertIn("sys.boot_completed", wait_script)


if __name__ == "__main__":
    unittest.main()
