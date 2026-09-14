import re
from pathlib import Path


BACKEND_ROOT = Path(__file__).resolve().parents[2]
ERROR_CODES = (
    BACKEND_ROOT
    / "yudao-module-dcc"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "dcc"
    / "enums"
    / "ErrorCodeConstants.java"
)
REGISTRATION_PACKAGE = (
    BACKEND_ROOT
    / "yudao-module-dcc"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "dcc"
    / "registrationcertificate"
)

HAN = re.compile(r"[\u4e00-\u9fff]")
LATIN = re.compile(r"[A-Za-z]")
ALLOWED_TERMS = re.compile(r"(?:DCC|BPM|ID|SHA)(?=[^A-Za-z]|$)", re.IGNORECASE)


def assert_chinese_copy(message: str, source: str) -> None:
    assert HAN.search(message), f"{source} must contain Chinese copy: {message}"
    normalized = ALLOWED_TERMS.sub("", message)
    assert not LATIN.search(normalized), f"{source} still contains English copy: {message}"


def test_registration_certificate_error_code_messages_are_chinese():
    text = ERROR_CODES.read_text(encoding="utf-8")
    matches = re.findall(
        r"ErrorCode\s+(REGISTRATION_CERTIFICATE_[A-Z0-9_]+)\s*=\s*"
        r"new\s+ErrorCode\([^,]+,\s*\"([^\"]+)\"\s*\);",
        text,
        flags=re.DOTALL,
    )

    assert len(matches) == 94, "registration certificate error-code scope changed; review all user messages"
    for name, message in matches:
        assert_chinese_copy(message, name)


def test_registration_certificate_api_documentation_is_chinese():
    annotations = []
    for path in REGISTRATION_PACKAGE.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for kind, message in re.findall(
            r"@(Operation|Tag|Schema)\([^\r\n]*?(?:summary|name|description)\s*=\s*\"([^\"]+)\"",
            text,
        ):
            annotations.append((path, kind, message))

    assert annotations, "registration certificate API documentation annotations must exist"
    for path, kind, message in annotations:
        assert_chinese_copy(message, f"{path.name} @{kind}")


def test_registration_certificate_direct_user_errors_are_chinese():
    patterns = [
        re.compile(r"invalidParamException\(\"([^\"]+)\"\)"),
        re.compile(r"new\s+(?:IllegalArgumentException|IllegalStateException)\(\"([^\"]+)\""),
    ]
    messages = []
    for path in REGISTRATION_PACKAGE.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for pattern in patterns:
            messages.extend((path, message) for message in pattern.findall(text))

    assert messages, "registration certificate direct error messages must exist"
    for path, message in messages:
        assert_chinese_copy(message, path.name)
