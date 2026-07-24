from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260622_mes_edhr_bpm_notify_to_inbox.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR BPM 站内信迁移 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_bpm_notify_sql_seeds_required_notify_templates() -> None:
    text = read_sql()

    for template_code in [
        "MES_EDHR_BPM_TASK_ASSIGNED",
        "MES_EDHR_BPM_APPROVED",
        "MES_EDHR_BPM_REJECTED",
        "MES_EDHR_BPM_TASK_TIMEOUT",
    ]:
        assert template_code in text

    for content_hint in [
        "工作到你了：请审批流程{processInstanceName}",
        "你的流程{processInstanceName}已审批通过",
        "你的流程{processInstanceName}已被驳回",
        "你的审批任务{taskName}已超时",
    ]:
        assert content_hint in text

    assert "system_notify_template" in text
    assert "eDHR任务中心" in text


def test_edhr_bpm_notify_sql_is_fail_fast_without_sms_fallback() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "fallback" not in text.lower()
    assert "手机号" not in text
    assert "system_sms" not in text.lower()
    assert "INSERT IGNORE" not in upper_text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text


def test_edhr_bpm_notify_sql_release_migration_depends_on_uses_ids() -> None:
    header = read_sql().splitlines()[0]

    assert "dependsOn=" in header
    depends_on = header.split("dependsOn=", 1)[1].split(";", 1)[0]
    dependency_ids = [item.strip() for item in depends_on.split(",")]

    assert dependency_ids == [
        "20260611_mes_edhr_work_task_flow",
        "20260612_mes_edhr_final_archive_work_task",
    ]
    assert all(not item.endswith(".sql") for item in dependency_ids)
