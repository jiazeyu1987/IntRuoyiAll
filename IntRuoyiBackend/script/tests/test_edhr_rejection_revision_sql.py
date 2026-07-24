from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260611_mes_edhr_rejection_revision_flow.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR驳回受控修订 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_execution_revision_chain_columns_are_declared() -> None:
    text = read_sql()

    for column in [
        "`revision_root_execution_id` bigint DEFAULT NULL",
        "`revision_no` int NOT NULL DEFAULT 1",
        "`source_rejected_execution_id` bigint DEFAULT NULL",
        "`superseded_by_execution_id` bigint DEFAULT NULL",
        "`revision_reason` varchar(500) DEFAULT NULL",
        "`revision_parent_hash` char(64) DEFAULT NULL",
        "`active_revision_flag` bit(1) NOT NULL DEFAULT b''1''",
    ]:
        assert column in text

    for index_name in [
        "idx_mes_pro_bre_revision_root",
        "idx_mes_pro_bre_source_rejected",
        "idx_mes_pro_bre_superseded",
    ]:
        assert index_name in text


def test_work_task_rework_source_columns_are_declared() -> None:
    text = read_sql()

    for column in [
        "`source_execution_id` bigint DEFAULT NULL",
        "`reason` varchar(500) DEFAULT NULL",
    ]:
        assert column in text

    assert "idx_mes_pro_edhr_work_task_source_execution" in text


def test_rejection_revision_migration_is_idempotent_and_fail_fast() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "INFORMATION_SCHEMA.COLUMNS" in upper_text
    assert "INFORMATION_SCHEMA.STATISTICS" in upper_text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "CONCAT(target_table, ' is missing; cannot apply eDHR rejection revision migration')" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
