from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260907_dcc_publication_impact_assessment.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing DCC publication impact-assessment migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_schema_has_release_metadata_and_no_historical_dml() -> None:
    sql = read_sql()
    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260907_dcc_publication_followup; type=schema; riskLevel=medium"
    )
    lowered = sql.lower()
    assert "update `dcc_publication_relation_snapshot`" not in lowered
    assert "insert into `dcc_publication_relation_snapshot`" not in lowered


def test_schema_defines_unique_versioned_task_and_immutable_audit() -> None:
    sql = read_sql()
    assert "CREATE TABLE IF NOT EXISTS `dcc_publication_impact_task`" in sql
    assert "CREATE TABLE IF NOT EXISTS `dcc_publication_impact_audit`" in sql
    assert "UNIQUE KEY `uk_dcc_pub_impact_master` (`tenant_id`, `batch_id`, `related_master_id`, `deleted`)" in sql
    assert "`row_version` INT NOT NULL DEFAULT 0" in sql
    assert "`linked_revision_controlled_file_id` BIGINT NULL" in sql
    assert "`revision_tracking_status` VARCHAR(32) NOT NULL" in sql
    assert "`creation_token` VARCHAR(36) NOT NULL" in sql
    assert "KEY `idx_dcc_pub_impact_linked_revision`" in sql


def test_schema_keeps_decision_and_audit_facts_structured() -> None:
    sql = read_sql()
    for column in (
        "`decision` VARCHAR(32) NULL",
        "`decision_reason` VARCHAR(1000) NULL",
        "`action_type` VARCHAR(32) NOT NULL",
        "`row_version_before` INT NOT NULL",
        "`row_version_after` INT NOT NULL",
        "`occurred_at` DATETIME NOT NULL",
    ):
        assert column in sql
    assert "decision_history_json" not in sql.lower()
