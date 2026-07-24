from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]

TABLES = [
    "dcc_nas_acl_snapshot",
    "dcc_nas_acl_directory_snapshot",
    "dcc_nas_acl_descriptor",
    "dcc_nas_acl_ace",
    "dcc_nas_acl_identity_mapping",
    "dcc_nas_acl_restore_plan",
    "dcc_nas_acl_restore_plan_item",
    "dcc_nas_acl_restore_log",
]

UNIQUE_KEYS = [
    "uk_dcc_nas_acl_snapshot_key",
    "uk_dcc_nas_acl_dir_snapshot_path",
    "uk_dcc_nas_acl_descriptor_hash",
    "uk_dcc_nas_acl_ace_order",
    "uk_dcc_nas_acl_identity_sid",
    "uk_dcc_nas_acl_restore_plan_key",
    "uk_dcc_nas_acl_restore_item_dir",
    "uk_dcc_nas_acl_restore_log_attempt",
]

RESTORE_PLAN_VERSION_COLUMNS = [
    "`semantic_policy_version` varchar(64) DEFAULT NULL",
    "`identity_mapping_version` varchar(64) DEFAULT NULL",
]

CANONICAL_HASH_COLUMNS = [
    "`expected_after_hash` varchar(128) DEFAULT NULL",
    "`actual_after_hash` varchar(128) DEFAULT NULL",
    "`before_hash` varchar(128) DEFAULT NULL",
]

EXACT_IDENTIFIER_SNIPPETS = [
    "`file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin",
    "`nas_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL",
]


def test_dcc_nas_acl_snapshot_restore_migration_is_idempotent_and_complete() -> None:
    migration = (REPO_ROOT / "sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql").read_text(
        encoding="utf-8"
    )

    assert "DROP TABLE" not in migration.upper()
    assert "TRUNCATE TABLE" not in migration.upper()
    assert "DELETE FROM `DCC_" not in migration.upper()
    for table in TABLES:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in migration
    for unique_key in UNIQUE_KEYS:
        assert f"`{unique_key}`" in migration
    assert "`tenant_id` bigint NOT NULL DEFAULT 0" in migration
    assert "`normalized_descriptor_json` longtext NOT NULL" in migration
    assert "`raw_descriptor_blob` longblob DEFAULT NULL" in migration
    for column in RESTORE_PLAN_VERSION_COLUMNS:
        assert column in migration
    for column in CANONICAL_HASH_COLUMNS:
        assert column in migration


def test_dcc_base_schema_includes_nas_acl_snapshot_restore_tables() -> None:
    base_schema = (REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql").read_text(
        encoding="utf-8"
    )

    for table in TABLES:
        assert f"CREATE TABLE IF NOT EXISTS `{table}`" in base_schema
    for unique_key in UNIQUE_KEYS:
        assert f"`{unique_key}`" in base_schema
    for column in RESTORE_PLAN_VERSION_COLUMNS:
        assert column in base_schema
    for column in CANONICAL_HASH_COLUMNS:
        assert column in base_schema


def test_dcc_exact_nas_identifiers_use_binary_collation() -> None:
    base_schema = (REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql").read_text(
        encoding="utf-8"
    )
    repair_schema = (
        REPO_ROOT / "sql/mysql/20260515_dcc_runtime_schema_repair.sql"
    ).read_text(encoding="utf-8")
    transfer_schema = (
        REPO_ROOT / "sql/mysql/20260523_dcc_nas_transfer_task.sql"
    ).read_text(encoding="utf-8")
    exact_identifier_migration = (
        REPO_ROOT / "sql/mysql/20260530_dcc_exact_nas_identifier_collation.sql"
    ).read_text(encoding="utf-8")

    for snippet in EXACT_IDENTIFIER_SNIPPETS:
        assert snippet in base_schema
    assert EXACT_IDENTIFIER_SNIPPETS[1] in transfer_schema
    assert "CHARACTER SET utf8mb4 COLLATE utf8mb4_bin" in repair_schema
    assert "ALTER TABLE `dcc_controlled_file_nas_transfer_task_item`" in exact_identifier_migration
    assert "ALTER TABLE `dcc_controlled_file_master`" in exact_identifier_migration
    assert "ALTER TABLE `dcc_controlled_file`" in exact_identifier_migration
    for snippet in EXACT_IDENTIFIER_SNIPPETS:
        assert snippet in exact_identifier_migration


def test_dcc_long_file_name_migration_extends_dcc_name_columns() -> None:
    migration = (
        REPO_ROOT / "sql/mysql/20260530_dcc_long_file_name_length.sql"
    ).read_text(encoding="utf-8")

    assert "DROP TABLE" not in migration.upper()
    assert "TRUNCATE TABLE" not in migration.upper()
    assert "DELETE FROM `DCC_" not in migration.upper()
    assert (
        "MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL"
        in migration
    )
    assert (
        "MODIFY `file_name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL"
        in migration
    )
    assert "MODIFY `title` varchar(256) NOT NULL" in migration


def test_dcc_long_nas_source_remark_migration_extends_remark_column() -> None:
    migration = (
        REPO_ROOT / "sql/mysql/20260530_dcc_long_nas_source_remark.sql"
    ).read_text(encoding="utf-8")
    base_schema = (REPO_ROOT / "sql/mysql/20260513_dcc_base_schema.sql").read_text(
        encoding="utf-8"
    )

    assert "DROP TABLE" not in migration.upper()
    assert "TRUNCATE TABLE" not in migration.upper()
    assert "DELETE FROM `DCC_" not in migration.upper()
    assert "MODIFY `remark` varchar(1024) DEFAULT NULL" in migration
    assert "`remark` varchar(1024) DEFAULT NULL" in base_schema
