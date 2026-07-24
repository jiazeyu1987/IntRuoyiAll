from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SERVICE_PATH = (
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
    / "MesProBatchRecordExecutionSignatureService.java"
)


def test_field_audit_server_time_signature_keeps_default_timezone() -> None:
    source = SERVICE_PATH.read_text(encoding="utf-8")

    assert 'public static final String DEFAULT_SIGNATURE_TIME_ZONE = "Asia/Shanghai";' in source
    assert "String selectedTimeZone = DEFAULT_SIGNATURE_TIME_ZONE;" in source
    assert 'String selectedTimeReason = "";' in source
    assert "selectedTimeZone = StrUtil.trim(command.getSelectedTimeZone());" in source
    assert "selectedTimeReason = StrUtil.trim(command.getSelectedTimeReason());" in source
