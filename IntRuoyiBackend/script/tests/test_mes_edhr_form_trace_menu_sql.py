from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260714_mes_edhr_form_trace_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR form trace menu SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not re.match(r"^\s*--", line)
    ).upper()


def test_form_trace_menu_keeps_one_visible_trace_entry() -> None:
    sql = read_sql()

    for required in [
        "release-migration: allowedEnvironments=test,backup,prod",
        "dependsOn=20260714_mes_edhr_release_trace_menu",
        "表单追溯",
        "/mes/pro/feedback/edhr-form-trace",
        "mes/pro/edhr/FormTracePage",
        "MesProFeedbackEdhrFormTrace",
        "mes:pro-batch-record-execution:track",
        "900025",
        "900260",
        "`visible` = b'0'",
        "`visible` = CASE WHEN `id` = 900260 THEN b'0' ELSE b'1' END",
        "`deleted` = b'0'",
    ]:
        assert required in sql

    assert "审计与追溯" in sql
    assert "放行追溯" in sql or "放行与归档" in sql


def test_release_permissions_are_retained_under_form_trace_menu() -> None:
    sql = read_sql()

    for permission in [
        "mes:pro-edhr-release:query",
        "mes:pro-edhr-release:event-query",
        "mes:pro-edhr-release:precheck",
        "mes:pro-edhr-release:submit",
        "mes:pro-edhr-release:approve",
        "mes:pro-edhr-release:reject",
        "mes:pro-edhr-release:withdraw",
    ]:
        assert permission in sql

    assert "`parent_id` = 900025" in sql or "parent_id` = 900025" in sql


def test_form_trace_menu_sql_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = executable_sql(sql)

    for required in [
        "DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_menu",
        "CREATE PROCEDURE ensure_mes_edhr_form_trace_menu",
        "SIGNAL SQLSTATE '45000'",
        "system_role_menu",
        "system_tenant_package",
        "JSON_VALID",
        "JSON_TABLE",
        "JSON_ARRAYAGG",
    ]:
        assert required in sql

    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql
