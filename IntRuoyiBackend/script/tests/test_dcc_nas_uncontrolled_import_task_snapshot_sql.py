from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260803_dcc_nas_uncontrolled_import_task_snapshot.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_dcc_nas_uncontrolled_import_task_snapshot_migration_contract() -> None:
    sql = _read(MIGRATION_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260803_dcc_nas_control_audit_file; type=schema; riskLevel=medium"
    )
    for token in [
        "CALL ensure_dcc_uncontrolled_import_column(",
        "`dcc_controlled_file_nas_transfer_task` ADD COLUMN `audit_task_id` bigint DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task` ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task` ADD COLUMN `request_hash` char(64) DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `audit_file_id` bigint DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `source_signature` char(64) DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `classification_status_snapshot` varchar(32) DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `classification_candidates_json_snapshot` text DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `local_write_status` varchar(32) DEFAULT NULL",
        "`dcc_controlled_file_nas_transfer_task_item` ADD COLUMN `archive_status` varchar(32) DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `selected_import_task_id` bigint DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `selected_import_task_item_id` bigint DEFAULT NULL",
        "ADD INDEX `idx_dcc_nas_transfer_import_idempotency` (`tenant_id`, `operator_user_id`, `idempotency_key`, `deleted`)",
        "ADD INDEX `idx_dcc_nas_transfer_item_audit_file` (`tenant_id`, `audit_file_id`, `deleted`)",
        "ADD INDEX `idx_dcc_nas_audit_file_import_task` (`tenant_id`, `selected_import_task_id`, `selected_import_task_item_id`, `deleted`)",
    ]:
        assert token in sql, f"schema migration must include {token}"

    upper = sql.upper()
    assert "DROP TABLE" not in upper
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper
    assert "UNIQUE KEY" not in upper
    assert "UNIQUE INDEX" not in upper


def test_dcc_nas_uncontrolled_import_task_snapshot_test_schema_is_aligned() -> None:
    sql = _read(TEST_SCHEMA_SQL)

    for token in [
        "`audit_task_id` BIGINT NULL",
        "`idempotency_key` VARCHAR(128) NULL",
        "`request_hash` CHAR(64) NULL",
        "`audit_file_id` BIGINT NULL",
        "`source_signature` CHAR(64) NULL",
        "`classification_status_snapshot` VARCHAR(32) NULL",
        "`classification_candidates_json_snapshot` TEXT NULL",
        "`local_relative_path` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL",
        "`local_write_status` VARCHAR(32) NULL",
        "`archive_status` VARCHAR(32) NULL",
        "`selected_import_task_id` BIGINT NULL",
        "`selected_import_task_item_id` BIGINT NULL",
    ]:
        assert token in sql, f"test schema must include {token}"
