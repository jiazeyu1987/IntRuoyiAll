from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260624_unified_approval_phase5_retire_legacy_menus.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "Phase5 unified approval retirement SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def test_retirement_sql_hides_all_declared_legacy_approval_entries() -> None:
    text = read_sql()

    for legacy_entry in [
        "bpm/task/todo/index",
        "bpm/task/done/index",
        "bpm/processInstance/index",
        "controlled-file/approval-tasks",
        "feedback/edhr-approval",
        "ShowroomAdminApproval",
    ]:
        assert legacy_entry in text

    normalized = " ".join(text.lower().split())
    assert "update system_menu set `visible` = b'0'" in normalized


def test_retirement_sql_declares_release_migration_metadata() -> None:
    first_line = read_sql().splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=menu; riskLevel=low"
    )


def test_retirement_sql_is_non_destructive_and_not_a_private_center_backfill() -> None:
    upper_text = read_sql().upper()

    for forbidden in [
        "DELETE FROM SYSTEM_MENU",
        "TRUNCATE TABLE SYSTEM_MENU",
        "DROP TABLE SYSTEM_MENU",
        "ON DUPLICATE KEY UPDATE",
        "INSERT INTO SYSTEM_MENU",
    ]:
        assert forbidden not in upper_text
