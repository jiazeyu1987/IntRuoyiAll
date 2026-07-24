from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260715_showroom_notify_template_garbled_repair.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "展厅站内信乱码修复 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_showroom_notify_template_garbled_repair_updates_templates_and_repairable_messages() -> None:
    text = read_sql()

    for template_code in [
        "SHOWROOM_APPROVAL_PENDING",
        "SHOWROOM_APPROVAL_PUBLISHED",
        "SHOWROOM_APPROVAL_REJECTED",
    ]:
        assert template_code in text

    for expected_text in [
        "展厅审批待办通知",
        "展厅发布完成通知",
        "展厅审批驳回通知",
        "展厅系统",
        "展厅{targetTypeText}【{targetName}】待{approvalStage}，点击查看对应内容。",
        "展厅{targetTypeText}【{targetName}】已审批通过并发布，点击查看对应内容。",
        "展厅{targetTypeText}【{targetName}】在{approvalStage}被驳回，原因：{rejectionReason}。点击后可继续修改原提交内容。",
        "JSON_SET(`m`.`template_params`, '$.targetName'",
        "showroom_change_request",
        "showroom_product_revision",
        "showroom_company",
    ]:
        assert expected_text in text

    assert "UPDATE `system_notify_template`" in text
    assert "UPDATE `system_notify_message` AS `m`" in text
    assert "JSON_VALID(`m`.`template_params`) = 1" in text
    assert "LOCATE('??'," in text


def test_showroom_notify_template_garbled_repair_is_fail_fast_and_no_fallback() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "missing Showroom notify template for garbled repair" in text
    assert "unrepairable Showroom notify template garbled text remains" in text
    assert "unrepairable repairable Showroom notify message garbled text remains" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE" not in upper_text
    assert "IFNULL(" not in upper_text
    assert "COALESCE(" not in upper_text
    assert "UNKNOWN" not in upper_text
    assert "不可恢复" not in text
