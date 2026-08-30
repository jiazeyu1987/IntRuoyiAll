from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260830_dcc_nas_original_path_sync.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_dcc_nas_original_path_sync_migration_contract() -> None:
    sql = _read(MIGRATION_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260803_dcc_nas_uncontrolled_import_task_snapshot; type=schema; riskLevel=medium"
    )
    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_nas_original_path_sync_file`",
        "`audit_task_id` bigint NOT NULL",
        "`audit_file_id` bigint NOT NULL",
        "`transfer_task_id` bigint NOT NULL",
        "`transfer_task_item_id` bigint NOT NULL",
        "`source_file_id` bigint NOT NULL",
        "`nas_share_name` varchar(128) NOT NULL",
        "`root_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
        "`normalized_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
        "`path_hash` char(64) NOT NULL",
        "`file_size` bigint NOT NULL",
        "`modified_at` datetime NOT NULL",
        "`source_signature` char(64) NOT NULL",
        "`sync_status` varchar(32) NOT NULL DEFAULT 'ACTIVE'",
        "`synced_by_user_id` bigint NOT NULL",
        "`synced_at` datetime NOT NULL",
        "`deleted_by_user_id` bigint DEFAULT NULL",
        "`deleted_at` datetime DEFAULT NULL",
        "KEY `idx_dcc_nas_original_path_sync_path` (`tenant_id`, `nas_share_name`, `path_hash`, `sync_status`, `deleted`)",
        "KEY `idx_dcc_nas_original_path_sync_audit_file` (`tenant_id`, `audit_file_id`, `deleted`)",
        "KEY `idx_dcc_nas_original_path_sync_source_file` (`tenant_id`, `source_file_id`, `deleted`)",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_status` varchar(32) DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_file_id` bigint DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_task_id` bigint DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_task_item_id` bigint DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_error_code` varchar(64) DEFAULT NULL",
        "`dcc_nas_control_audit_file` ADD COLUMN `original_path_sync_error` varchar(512) DEFAULT NULL",
        "ADD INDEX `idx_dcc_nas_audit_file_original_sync` (`tenant_id`, `original_path_sync_status`, `original_path_sync_file_id`, `deleted`)",
    ]:
        assert token in sql, f"schema migration must include {token}"

    upper = sql.upper()
    assert "DROP TABLE" not in upper
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper
    assert "UNIQUE KEY" not in upper
    assert "UNIQUE INDEX" not in upper


def test_dcc_nas_original_path_sync_test_schema_is_aligned() -> None:
    sql = _read(TEST_SCHEMA_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_nas_original_path_sync_file`" in sql
    for token in [
        "`transfer_task_id` BIGINT NOT NULL",
        "`transfer_task_item_id` BIGINT NOT NULL",
        "`source_file_id` BIGINT NOT NULL",
        "`normalized_relative_path` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
        "`source_signature` CHAR(64) NOT NULL",
        "`sync_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'",
        "`synced_by_user_id` BIGINT NOT NULL",
        "`deleted_by_user_id` BIGINT NULL",
        "`original_path_sync_status` VARCHAR(32) NULL",
        "`original_path_sync_file_id` BIGINT NULL",
        "`original_path_sync_task_id` BIGINT NULL",
        "`original_path_sync_task_item_id` BIGINT NULL",
        "`original_path_sync_error_code` VARCHAR(64) NULL",
        "`original_path_sync_error` VARCHAR(512) NULL",
    ]:
        assert token in sql, f"test schema must include {token}"
