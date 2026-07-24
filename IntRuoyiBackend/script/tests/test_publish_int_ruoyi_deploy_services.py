import re
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "deploy" / "publish-int-ruoyi.ps1"


class PublishIntRuoyiDeployServicesTestCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.script = SCRIPT_PATH.read_text(encoding="utf-8")

    def test_deploy_release_does_not_hardcode_onlyoffice_service_start(self) -> None:
        self.assertNotIn(
            "docker compose up -d onlyoffice backend frontend",
            self.script,
        )

    def test_deploy_release_discovers_services_from_remote_compose(self) -> None:
        self.assertIn("Get-RemoteComposeServices", self.script)
        self.assertRegex(
            self.script,
            re.compile(r"docker compose config --services"),
        )

    def test_nas_share_defaults_to_config_file_value(self) -> None:
        self.assertIn("[string]$NasShare = '',", self.script)

    def test_deployment_history_does_not_require_test_conclusion(self) -> None:
        match = re.search(
            r"function Write-NasReleaseDeploymentHistory \{([\s\S]*?)\n\}",
            self.script,
        )
        self.assertIsNotNone(match)
        self.assertNotIn("TestConclusion is required", match.group(1))

    def test_deploy_release_fails_fast_when_required_services_are_missing(self) -> None:
        self.assertIn("Assert-RemoteComposeService", self.script)
        for service in ("mysql", "redis", "backend", "frontend", "website"):
            self.assertRegex(
                self.script,
                re.compile(rf"Assert-RemoteComposeService[^\n]+{service}"),
            )

    def test_onlyoffice_readiness_is_waited_only_when_compose_declares_it(self) -> None:
        onlyoffice_wait_pattern = re.compile(
            r"if\s*\(\s*Test-RemoteComposeService\s+-Services\s+\$remoteComposeServices\s+-ServiceName\s+'onlyoffice'\s*\)"
            r"[\s\S]*?Wait-RemoteHttpOk -Url \"http://127\.0\.0\.1:\$OnlyOfficeHostPort/healthcheck\"",
            re.MULTILINE,
        )
        self.assertRegex(self.script, onlyoffice_wait_pattern)


if __name__ == "__main__":
    unittest.main()
