from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ENUM_PATH = (
    ROOT
    / "yudao-module-system"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "system"
    / "enums"
    / "permission"
    / "RoleCodeEnum.java"
)


def read_enum() -> str:
    assert ENUM_PATH.exists(), "missing RoleCodeEnum"
    return ENUM_PATH.read_text(encoding="utf-8")


def test_full_scope_admin_roles_are_named_in_role_code_enum() -> None:
    text = read_enum()

    expected_constants = {
        "BPM_ADMIN": ('"bpm_admin"', '"BPM管理员"'),
        "APPROVAL_ADMIN": ('"approval_admin"', '"审批中心管理员"'),
        "AUDIT_ADMIN": ('"audit_admin"', '"审计管理员"'),
    }
    for constant, snippets in expected_constants.items():
        assert constant in text
        for snippet in snippets:
            assert snippet in text

    assert "审批管理员" not in text
