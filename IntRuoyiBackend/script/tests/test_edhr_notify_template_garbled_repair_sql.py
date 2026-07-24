from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260715_mes_edhr_notify_template_garbled_repair.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR站内信乱码修复 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_notify_template_garbled_repair_updates_bad_templates_and_messages() -> None:
    text = read_sql()

    for template_code in [
        "MES_EDHR_ARCHIVE_TASK_ASSIGNED",
        "MES_EDHR_WORK_TASK_OVERDUE",
    ]:
        assert template_code in text

    for expected_text in [
        "eDHR最终归档任务通知",
        "eDHR工作任务逾期提醒",
        "eDHR任务中心",
        "工作到你了：请完成工单{workOrderCode}批次{batchCode}的最终归档。入口：{actionUrl}",
        "工作任务已逾期：工单{workOrderCode}批次{batchCode}的{processName}批记录应于{dueTime}前处理。入口：{actionUrl}",
        "工作到你了：请完成工单",
        "工作任务已逾期：工单",
    ]:
        assert expected_text in text

    assert "UPDATE `system_notify_template`" in text
    assert "UPDATE `system_notify_message`" in text
    assert "JSON_VALID(`template_params`) = 1" in text
    assert "JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.workOrderCode'))" in text
    assert "JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.batchCode'))" in text
    assert "JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.processName'))" in text
    assert "JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.dueTime'))" in text
    assert "JSON_UNQUOTE(JSON_EXTRACT(`template_params`, '$.actionUrl'))" in text
    assert "LOCATE('??'," in text


def test_edhr_notify_template_garbled_repair_is_fail_fast_and_no_fallback() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "missing eDHR notify template for garbled repair" in text
    assert "unrepairable eDHR notify message garbled text remains" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE" not in upper_text
    assert "IFNULL(" not in upper_text
    assert "COALESCE(" not in upper_text
