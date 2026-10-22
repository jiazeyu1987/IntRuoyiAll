"""Static confidentiality contract for the two user password mutation paths."""
from pathlib import Path
import re
import unittest

BACKEND = Path(__file__).resolve().parents[2]
SYSTEM = BACKEND / "yudao-module-system/src/main/java/cn/iocoder/yudao/module/system"


class PasswordSecretContractTest(unittest.TestCase):
    def test_reset_log_only_describes_target_and_action(self):
        text = (SYSTEM / "enums/LogRecordConstants.java").read_text(encoding="utf-8")
        template = re.search(r'SYSTEM_USER_UPDATE_PASSWORD_SUCCESS\s*=\s*"([^"]+)"', text).group(1)
        self.assertNotIn("password", template.lower(), "Password hashes must not be log template variables")
        self.assertIn("targetNickname", template)
        text = (SYSTEM / "service/user/AdminUserServiceImpl.java").read_text(encoding="utf-8")
        method = text.split("public void updateUserPassword(Long id, String password)", 1)[1].split("\n    @Override", 1)[0]
        self.assertNotRegex(method, r'putVariable\("(?:user|newPassword)"',
                            "The whole user object and password hashes must stay out of LogRecord context")

    def test_password_apis_disable_request_logging_and_mark_secrets(self):
        for filename, fields in (
            ("UserController.java", ("password",)),
            ("UserProfileController.java", ("oldPassword", "newPassword")),
        ):
            with self.subTest(controller=filename):
                text = (SYSTEM / "controller/admin/user" / filename).read_text(encoding="utf-8")
                endpoint = text.split('@PutMapping("/update-password")', 1)[1].split("public ", 1)[0]
                annotation = re.search(r'@ApiAccessLog\(([^)]*)\)', endpoint)
                self.assertIsNotNone(annotation, "Password endpoints must explicitly protect request logs")
                self.assertRegex(annotation.group(1), r'requestEnable\s*=\s*false',
                                 "Password request bodies must never be read for logging")
                keys = re.search(r'sanitizeKeys\s*=\s*\{([^}]*)\}', annotation.group(1))
                self.assertIsNotNone(keys)
                self.assertEqual(set(re.findall(r'"([^\"]+)"', keys.group(1))), set(fields),
                                 "Both endpoints must retain their explicit secret-field contract")


if __name__ == "__main__":
    unittest.main()
