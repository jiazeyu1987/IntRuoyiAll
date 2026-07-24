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


if __name__ == "__main__":
    unittest.main()
