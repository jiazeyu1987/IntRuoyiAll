from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_SQL = REPO_ROOT / "sql/mysql/20260803_dcc_nas_control_audit_file.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_dcc_nas_control_audit_file_schema_has_required_contract() -> None:
    sql = _read(SCHEMA_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260730_dcc_nas_control_audit; type=schema; riskLevel=medium"
    )
    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_nas_control_audit_file`",
        "`task_id` bigint NOT NULL",
        "`nas_share_name` varchar(128) NOT NULL",
        "`root_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
        "`normalized_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
        "`path_hash` char(64) NOT NULL",
        "`file_size` bigint NOT NULL",
        "`modified_at` datetime NOT NULL",
        "`source_signature` char(64) NOT NULL",
        "`control_status` varchar(32) NOT NULL DEFAULT 'NOT_CONTROLLED'",
        "`classification_status` varchar(32) NOT NULL DEFAULT 'PENDING_RECOGNITION'",
        "`classification_candidates_json` text DEFAULT NULL",
        "`expected_local_relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL",
        "`download_status` varchar(32) NOT NULL DEFAULT 'NOT_SELECTED'",
        "`archive_status` varchar(32) NOT NULL DEFAULT 'NOT_STARTED'",
        "`local_write_error_code` varchar(64) DEFAULT NULL",
        "`archive_error_code` varchar(64) DEFAULT NULL",
        "KEY `idx_dcc_nas_audit_file_task` (`tenant_id`, `task_id`, `id`)",
        "KEY `idx_dcc_nas_audit_file_path_hash` (`tenant_id`, `nas_share_name`, `path_hash`, `deleted`)",
        "KEY `idx_dcc_nas_audit_file_status` (`tenant_id`, `classification_status`, `download_status`, `archive_status`)",
    ]:
        assert token in sql, f"schema migration must include {token}"

    upper = sql.upper()
    assert "DROP TABLE" not in upper
    assert "TRUNCATE TABLE" not in upper
    assert "DELETE FROM" not in upper
    assert "UNIQUE KEY" not in upper
    assert "UNIQUE INDEX" not in upper


def test_dcc_nas_control_audit_file_test_schema_is_aligned() -> None:
    sql = _read(TEST_SCHEMA_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_nas_control_audit_file`" in sql
    for token in [
        "`source_signature` CHAR(64) NOT NULL",
        "`classification_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_RECOGNITION'",
        "`classification_candidates_json` TEXT NULL",
        "`expected_local_relative_path` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL",
        "`download_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_SELECTED'",
        "`archive_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED'",
        "`local_write_error_code` VARCHAR(64) NULL",
        "`archive_error_code` VARCHAR(64) NULL",
    ]:
        assert token in sql, f"test schema must include {token}"
