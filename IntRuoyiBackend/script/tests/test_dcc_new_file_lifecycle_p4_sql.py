from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260906_dcc_new_file_lifecycle_p4.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC new-file lifecycle P4 migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_p4_migration_declares_order_and_direct_publish_contract() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260906_dcc_new_file_lifecycle_p3,20260720_dcc_publish_form_policy_seed; "
        "type=data; riskLevel=medium"
    )
    for literal in [
        "'DCC'",
        "'CONTROLLED_FILE'",
        "'PUBLISH'",
        "'READY_TO_PUBLISH'",
        "'DIRECT'",
        "'DCC_PUBLISH'",
        "'PUBLISHED'",
    ]:
        assert literal in sql


def test_p4_migration_replaces_only_the_published_dcc_publish_policy() -> None:
    sql = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_dcc_publish_direct_source`" in sql
    assert re.search(
        r"UPDATE\s+`bpm_business_approval_policy`[\s\S]+"
        r"SET\s+`policy`\.`status`\s*=\s*'DISABLED'[\s\S]+"
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`",
        sql,
        re.I,
    )
    assert "`source`.`policy_mode` <> 'DIRECT'" in sql
    assert "`action_code` = 'PUBLISH'" in sql
    assert "`object_state` = 'READY_TO_PUBLISH'" in sql
    assert "`effect_executor_code` = 'DCC_PUBLISH'" in sql


def test_p4_migration_fails_fast_and_is_transactional() -> None:
    sql = read_sql()

    assert "DECLARE EXIT HANDLER FOR SQLEXCEPTION" in sql
    assert "ROLLBACK;" in sql
    assert "RESIGNAL;" in sql
    assert "START TRANSACTION;" in sql
    assert "COMMIT;" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "DCC direct publish policy source is missing or duplicated" in sql
    assert "DCC direct publish policy conversion failed" in sql
    assert "fallback" not in sql.lower()
