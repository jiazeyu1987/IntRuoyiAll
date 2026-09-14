from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260831_mes_pqc_release_review_work_order_freeze.sql"


def test_nonconformance_review_freeze_snapshot_migration_is_idempotent() -> None:
    migration = MIGRATION.read_text(encoding="utf-8")

    assert "release-migration: allowedEnvironments=test,backup,prod" in migration
    assert "dependsOn=20260831_mes_pqc_release_nonconformance_scope" in migration
    assert "CREATE PROCEDURE upgrade_mes_pqc_release_review_work_order_freeze" in migration
    assert "TABLE_NAME = 'mes_pro_edhr_nonconformance_review'" in migration
    assert "COLUMN_NAME = 'previous_work_order_temporary_frozen'" in migration
    assert "`previous_work_order_temporary_frozen` bit(1) DEFAULT NULL" in migration
    assert "AFTER `previous_batch_status`" in migration
    assert "CALL upgrade_mes_pqc_release_review_work_order_freeze();" in migration
    assert "DROP PROCEDURE IF EXISTS upgrade_mes_pqc_release_review_work_order_freeze;" in migration
