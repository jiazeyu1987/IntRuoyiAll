from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260623_dcc_browser_batch_recognition_task.sql"
REPAIR_SQL = REPO_ROOT / "sql/mysql/20260629_dcc_batch_recognition_task_schema_repair.sql"
WORKER_LEDGER_SQL = REPO_ROOT / "sql/mysql/20260701_dcc_batch_recognition_worker_ledger_export.sql"
RECOGNITION_RECORD_SQL = REPO_ROOT / "sql/mysql/20260629_dcc_controlled_file_recognition_record.sql"
TRACEABLE_FAILURE_SQL = REPO_ROOT / "sql/mysql/20260706_dcc_recognition_traceable_failure_messages.sql"
EXISTING_RECORD_POLICY_SQL = REPO_ROOT / "sql/mysql/20260706_dcc_batch_recognition_existing_record_policy.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_batch_recognition_migration_has_release_metadata_and_table_definition():
    sql = _read(MIGRATION_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )
    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_batch_recognition_task`" in sql
    assert "`candidate_ids_json` longtext NOT NULL" in sql
    assert "`sync_file_name_title` bit(1) NOT NULL DEFAULT b'1'" in sql
    assert "`existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING'" in sql
    assert "`worker_count` int NOT NULL DEFAULT 1" in sql
    assert "`status` varchar(16) NOT NULL" in sql


def test_batch_recognition_task_table_exists_in_test_schema():
    sql = _read(TEST_SCHEMA_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_controlled_file_batch_recognition_task`" in sql
    assert "`candidate_ids_json` longtext NOT NULL" in sql
    assert "`existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING'" in sql
    assert "`worker_count` int NOT NULL DEFAULT 1" in sql
    assert "`skipped_existing_count` bigint NOT NULL DEFAULT 0" in sql
    assert "`completed_at` datetime DEFAULT NULL" in sql
    assert "`batch_task_id` bigint DEFAULT NULL" in sql
    assert "`idx_dcc_file_recognition_record_batch`" in sql


def test_batch_recognition_task_schema_repair_adds_missing_recognition_version_snapshot_column():
    sql = _read(REPAIR_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260623_dcc_browser_batch_recognition_task; type=schema; riskLevel=medium"
    )
    assert "information_schema.COLUMNS" in sql
    assert "TABLE_NAME = 'dcc_controlled_file_batch_recognition_task'" in sql
    assert "COLUMN_NAME = 'recognition_version_snapshot'" in sql
    assert "ALTER TABLE `dcc_controlled_file_batch_recognition_task` ADD COLUMN `recognition_version_snapshot`" in sql
    assert "varchar(64) NOT NULL DEFAULT ''v1''" in sql


def test_recognition_record_migration_links_records_to_batch_tasks():
    sql = _read(RECOGNITION_RECORD_SQL)

    assert "`batch_task_id` bigint DEFAULT NULL" in sql
    assert "`idx_dcc_file_recognition_record_batch`" in sql
    assert "(`tenant_id`, `batch_task_id`)" in sql


def test_worker_ledger_export_repair_is_non_destructive_and_idempotent():
    sql = _read(WORKER_LEDGER_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260629_dcc_controlled_file_recognition_record; type=schema; riskLevel=medium"
    )
    assert "INFORMATION_SCHEMA.COLUMNS" in sql
    assert "INFORMATION_SCHEMA.STATISTICS" in sql
    assert "ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '`" in sql
    assert "'dcc_controlled_file_batch_recognition_task'" in sql
    assert "'worker_count'" in sql
    assert "'dcc_controlled_file_recognition_record'" in sql
    assert "'batch_task_id'" in sql
    assert "CREATE INDEX `idx_dcc_file_recognition_record_batch`" in sql


def test_traceable_failure_message_repair_expands_batch_and_record_failure_columns():
    sql = _read(TRACEABLE_FAILURE_SQL)
    test_schema = _read(TEST_SCHEMA_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260701_dcc_batch_recognition_worker_ledger_export; type=schema; riskLevel=medium"
    )
    assert "INFORMATION_SCHEMA.COLUMNS" in sql
    assert "MODIFY COLUMN `last_failure_message` varchar(2048)" in sql
    assert "MODIFY COLUMN `failure_message` varchar(2048)" in sql
    assert "`last_failure_message` varchar(2048) DEFAULT NULL" in test_schema
    assert "`failure_message` varchar(2048) DEFAULT NULL" in test_schema


def test_existing_record_policy_repair_adds_three_state_policy_column():
    sql = _read(EXISTING_RECORD_POLICY_SQL)
    test_schema = _read(TEST_SCHEMA_SQL)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260706_dcc_recognition_traceable_failure_messages; type=schema; riskLevel=medium"
    )
    assert "INFORMATION_SCHEMA.COLUMNS" in sql
    assert "COLUMN_NAME = 'existing_record_policy'" in sql
    assert "ALTER TABLE `dcc_controlled_file_batch_recognition_task`" in sql
    assert "`existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING'" in sql
    assert "UPDATE `dcc_controlled_file_batch_recognition_task`" in sql
    assert "`existing_record_policy` = 'OVERWRITE_ALL'" in sql
    assert "`existing_record_policy` varchar(32) NOT NULL DEFAULT 'SKIP_ALL_EXISTING'" in test_schema
