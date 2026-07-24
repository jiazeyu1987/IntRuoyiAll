from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
READINESS_SERVICE = (
    REPO_ROOT
    / "yudao-module-mes"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "mes"
    / "service"
    / "pro"
    / "batchrecord"
    / "MesProEdhrRehearsalReadinessServiceImpl.java"
)


def test_rehearsal_readiness_checks_record_scope_sign_and_approve_abilities() -> None:
    source = READINESS_SERVICE.read_text(encoding="utf-8")

    assert 'REQUIRED_EXECUTOR_RECORD_ABILITIES = List.of("VIEW", "FILL", "SIGN")' in source
    assert 'REQUIRED_APPROVER_RECORD_ABILITIES = List.of("VIEW", "APPROVE")' in source
    assert "permissionScopeService.evaluate" in source
    assert '"PERMISSION_RULE_MISSING"' in source
