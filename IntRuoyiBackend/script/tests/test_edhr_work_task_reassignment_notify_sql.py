from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260718_mes_edhr_fill_task_reassignment_notify.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR fill task reassignment notify migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_reassignment_notify_template_is_seeded_idempotently() -> None:
    sql = read_sql()

    assert "system_notify_template" in sql
    assert "MES_EDHR_FILL_TASK_REASSIGNED" in sql
    assert "eDHR填写任务转派通知" in sql
    assert "批记录填写任务已转给你" in sql
    assert '"workOrderCode"' in sql
    assert '"batchCode"' in sql
    assert '"processName"' in sql
    assert '"actionUrl"' in sql
    assert '"workTaskId"' in sql
    assert '"reason"' in sql
    assert "WHERE NOT EXISTS" in sql
    assert "AND `deleted` = b'0'" in sql


def test_reassignment_notify_template_migration_does_not_overwrite_custom_templates() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    assert "UPDATE `SYSTEM_NOTIFY_TEMPLATE`" not in upper_sql
    assert "ON DUPLICATE KEY UPDATE" not in upper_sql
    assert "INSERT IGNORE" not in upper_sql
    assert "SYSTEM_MENU" not in upper_sql
    assert "SYSTEM_ROLE_MENU" not in upper_sql
    assert "SYSTEM_USER_ROLE" not in upper_sql
