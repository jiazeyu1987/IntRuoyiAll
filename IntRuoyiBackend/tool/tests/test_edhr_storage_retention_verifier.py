import contextlib
import importlib.util
import io
import json
import os
import sys
import unittest
from pathlib import Path
from unittest import mock


MODULE_PATH = Path(__file__).resolve().parents[1] / "edhr-storage-retention-verifier" / "verify.py"
SPEC = importlib.util.spec_from_file_location("edhr_storage_retention_verifier", MODULE_PATH)
verifier = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = verifier
SPEC.loader.exec_module(verifier)


class EdhrStorageRetentionVerifierTestCase(unittest.TestCase):
    def test_main_blocks_when_required_environment_is_missing(self) -> None:
        env = {name: "" for name in verifier.REQUIRED_ENV}
        stdout = io.StringIO()

        with mock.patch.dict(os.environ, env, clear=False), contextlib.redirect_stdout(stdout):
            exit_code = verifier.main()

        payload = json.loads(stdout.getvalue())

        self.assertEqual(exit_code, verifier.EXIT_BLOCKED)
        self.assertEqual(payload["status"], "BLOCKED")
        self.assertCountEqual(payload["missingPrerequisites"], verifier.REQUIRED_ENV)
        self.assertEqual(payload["checks"][0]["name"], "requiredEnvironment")
        self.assertEqual(payload["checks"][0]["status"], "BLOCKED")

    def test_parse_config_normalizes_valid_retention_values(self) -> None:
        config, errors = verifier._parse_config(
            {
                "EDHR_S3_ENDPOINT": "http://127.0.0.1:9000",
                "EDHR_S3_BUCKET": "edhr-retention-verifier-test",
                "EDHR_S3_REGION": "us-east-1",
                "EDHR_S3_ACCESS_KEY": "access-value",
                "EDHR_S3_SECRET_KEY": "secret-value",
                "EDHR_S3_RETENTION_MODE": "compliance",
                "EDHR_S3_RETAIN_UNTIL_DAYS": "7",
                "EDHR_S3_REQUIRE_LEGAL_HOLD": "ON",
            }
        )

        self.assertEqual(errors, [])
        self.assertIsNotNone(config)
        self.assertEqual(config.retention_mode, "COMPLIANCE")
        self.assertEqual(config.retain_until_days, 7)
        self.assertTrue(config.require_legal_hold)

    def test_parse_config_rejects_invalid_retention_values(self) -> None:
        config, errors = verifier._parse_config(
            {
                "EDHR_S3_ENDPOINT": "http://127.0.0.1:9000",
                "EDHR_S3_BUCKET": "edhr-retention-verifier-test",
                "EDHR_S3_REGION": "us-east-1",
                "EDHR_S3_ACCESS_KEY": "access-value",
                "EDHR_S3_SECRET_KEY": "secret-value",
                "EDHR_S3_RETENTION_MODE": "delete",
                "EDHR_S3_RETAIN_UNTIL_DAYS": "0",
                "EDHR_S3_REQUIRE_LEGAL_HOLD": "maybe",
            }
        )

        self.assertIsNone(config)
        self.assertIn("EDHR_S3_RETENTION_MODE must be GOVERNANCE or COMPLIANCE", errors)
        self.assertIn("EDHR_S3_RETAIN_UNTIL_DAYS must be a positive integer", errors)
        self.assertIn("EDHR_S3_REQUIRE_LEGAL_HOLD must be true or false", errors)

    def test_sanitize_redacts_access_key_and_secret_key(self) -> None:
        env = {
            "EDHR_S3_ACCESS_KEY": "visible-access",
            "EDHR_S3_SECRET_KEY": "visible-secret",
        }

        sanitized = verifier._sanitize(
            "request failed for visible-access with visible-secret",
            env,
        )

        self.assertNotIn("visible-access", sanitized)
        self.assertNotIn("visible-secret", sanitized)
        self.assertEqual(sanitized.count("[REDACTED]"), 2)

    def test_s3_client_uses_bounded_timeout_and_retry_config(self) -> None:
        captured: dict[str, object] = {}

        class CapturingConfig:
            def __init__(self, **kwargs: object) -> None:
                captured["config"] = kwargs

        class FakeClient:
            def get_bucket_versioning(self, Bucket: str) -> dict[str, str]:  # noqa: N803 - boto3 uses Bucket.
                return {"Status": "Suspended"}

        class FakeBoto3:
            def client(self, service_name: str, **kwargs: object) -> FakeClient:
                captured["service_name"] = service_name
                captured["client"] = kwargs
                return FakeClient()

        env = {
            "EDHR_S3_ENDPOINT": "http://127.0.0.1:9000",
            "EDHR_S3_BUCKET": "edhr-retention-verifier-test",
            "EDHR_S3_REGION": "us-east-1",
            "EDHR_S3_ACCESS_KEY": "access-value",
            "EDHR_S3_SECRET_KEY": "secret-value",
            "EDHR_S3_RETENTION_MODE": "COMPLIANCE",
            "EDHR_S3_RETAIN_UNTIL_DAYS": "7",
            "EDHR_S3_REQUIRE_LEGAL_HOLD": "true",
        }
        dependencies = {
            "boto3": FakeBoto3(),
            "Config": CapturingConfig,
            "ClientError": Exception,
            "BotoCoreError": RuntimeError,
            "missing": [],
        }
        stdout = io.StringIO()

        with mock.patch.object(verifier, "_load_dependencies", return_value=dependencies), \
                contextlib.redirect_stdout(stdout):
            verifier._run(env, verifier._base_result(env))

        config = captured["config"]
        self.assertIsInstance(config, dict)
        self.assertEqual(config["signature_version"], "s3v4")
        self.assertLessEqual(config["connect_timeout"], 5)
        self.assertLessEqual(config["read_timeout"], 15)
        self.assertEqual(config["retries"], {"max_attempts": 2, "mode": "standard"})


if __name__ == "__main__":
    unittest.main()
