from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260715_mes_edhr_form_trace_change_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR form trace change menu SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def executable_sql(sql: str) -> str:
    return "\n".join(
        line for line in sql.splitlines()
        if not re.match(r"^\s*--", line)
    ).upper()


def test_change_menu_is_hidden_under_form_trace() -> None:
    sql = read_sql()

    for required in [
        "release-migration: allowedEnvironments=test,backup,prod",
        "dependsOn=20260714_mes_edhr_form_trace_menu",
        "900025",
        "900235",
        "表单追溯",
        "变更与异常",
        "`visible` = b'0'",
        "`type` = 3",
        "`parent_id` = 900025",
    ]:
        assert required in sql


def test_change_permissions_are_retained_under_form_trace_menu() -> None:
    sql = read_sql()

    for permission in [
        "mes:pro-edhr-change:query",
        "mes:pro-edhr-change:void",
        "mes:pro-edhr-change:reopen",
        "mes:pro-edhr-change:supplement",
        "mes:pro-edhr-change:approve",
    ]:
        assert permission in sql

    assert "system_role_menu" in sql
    assert "system_tenant_package" in sql
    assert "JSON_VALID" in sql
    assert "JSON_TABLE" in sql


def test_change_menu_sql_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = executable_sql(sql)

    for required in [
        "DROP PROCEDURE IF EXISTS ensure_mes_edhr_form_trace_change_menu",
        "CREATE PROCEDURE ensure_mes_edhr_form_trace_change_menu",
        "SIGNAL SQLSTATE '45000'",
        "CALL ensure_mes_edhr_form_trace_change_menu()",
    ]:
        assert required in sql

    assert "DELETE FROM" not in upper_sql
    assert "TRUNCATE" not in upper_sql
    assert "DROP TABLE" not in upper_sql
