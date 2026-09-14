from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = REPO_ROOT / "sql" / "mysql" / "20260911_dcc_controlled_file_submit_idempotency.sql"


def test_submit_idempotency_migration_has_release_metadata():
    sql = SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )


def test_submit_idempotency_migration_adds_formal_unique_contract():
    sql = SQL.read_text(encoding="utf-8")

    assert "ADD COLUMN `submit_idempotency_key` VARCHAR(128) NULL" in sql
    assert "ADD COLUMN `submit_payload_hash` CHAR(64) NULL" in sql
    assert "ADD UNIQUE KEY `uk_dcc_file_submit_idempotency`" in sql
    assert "`tenant_id`, `submitter_id`, `submit_idempotency_key`, `deleted`" in sql
