from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260726_system_codex_test_case_project.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return read_sql().replace("`", "").lower()


def test_project_migration_declares_release_contract_and_schema_change() -> None:
    sql = read_sql()
    normalized = normalized_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260726_system_codex_smart_scheduling_test_items,20260726_dcc_codex_test_items_seed; "
        "type=schema; riskLevel=medium\n"
    )
    assert "CREATE PROCEDURE ensure_system_codex_test_case_project" in sql
    assert "ALTER TABLE `system_codex_test_case`" in sql
    assert "ADD COLUMN `project` varchar(16) NULL COMMENT '所属项目：智能排产/文控/批记录' AFTER `name`" in sql
    assert "MODIFY COLUMN `project` varchar(16) NOT NULL COMMENT '所属项目：智能排产/文控/批记录'" in sql
    assert "idx_system_codex_test_case_tenant_project" in normalized


def test_project_migration_classifies_existing_cases_and_fails_fast_on_unknowns() -> None:
    sql = read_sql()
    normalized = normalized_sql()

    for project in ("智能排产", "文控", "批记录"):
        assert project in sql

    for marker in ("排产", "smart-scheduling", "文控", "/dcc/", "controlled-file", "批记录", "edhr", "recordbook"):
        assert marker in normalized or marker in sql

    assert "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unclassified codex test case project'" in sql
    assert "project not in ('智能排产', '文控', '批记录')" in normalized
