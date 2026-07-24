from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260719_dcc_file_type_taxonomy.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "DCC file type taxonomy migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_dcc_file_type_taxonomy_migration_has_release_metadata() -> None:
    sql = read_sql()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )


def test_dcc_file_type_taxonomy_migration_defines_schema_and_indexes() -> None:
    sql = read_sql()

    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_file_type_taxonomy`",
        "`parent_id` BIGINT NOT NULL DEFAULT 0",
        "`level_no` TINYINT NOT NULL",
        "uk_dcc_file_type_taxonomy_tenant_code_deleted",
        "uk_dcc_file_type_taxonomy_sibling_name_deleted",
        "idx_dcc_file_type_taxonomy_parent",
        "file_type_taxonomy_id",
        "idx_dcc_file_category_taxonomy",
        "idx_dcc_controlled_file_taxonomy",
        "idx_dcc_recognition_record_taxonomy",
    ]:
        assert token in sql, f"taxonomy migration must include {token}"


def test_dcc_file_type_taxonomy_migration_uses_idempotent_mysql_ddl() -> None:
    sql = read_sql()

    for token in [
        "information_schema.COLUMNS",
        "information_schema.STATISTICS",
        "PREPARE dcc_file_category_taxonomy_column_stmt",
        "PREPARE dcc_controlled_file_taxonomy_column_stmt",
        "PREPARE dcc_recognition_record_taxonomy_column_stmt",
        "PREPARE dcc_file_category_taxonomy_index_stmt",
        "PREPARE dcc_controlled_file_taxonomy_index_stmt",
        "PREPARE dcc_recognition_record_taxonomy_index_stmt",
    ]:
        assert token in sql, f"taxonomy migration must use idempotent DDL guard {token}"

    assert "ADD COLUMN IF NOT EXISTS" not in sql


def test_dcc_file_type_taxonomy_menu_seed_is_collation_safe() -> None:
    sql = read_sql()

    for token in [
        "CONVERT(UNHEX('E59FBAE7A180E695B0E68DAE') USING utf8mb4) COLLATE utf8mb4_unicode_ci",
        "CONVERT(UNHEX('444343E69687E4BBB6E58886E7B1BB') USING utf8mb4) COLLATE utf8mb4_unicode_ci",
        "'file-type-taxonomy'",
        "'dcc/controlled-file/basic-data/file-type-taxonomy/index'",
        "'DccFileTypeTaxonomyBasicDataPage'",
        "source_menu.`path` = 'controlled-file/categories'",
        "target_menu.`id` = 990230",
    ]:
        assert token in sql, f"taxonomy menu seed must include {token}"

    assert "????" not in sql


def test_dcc_file_type_taxonomy_migration_is_non_destructive() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `DCC_FILE_TYPE_TAXONOMY`",
        "DELETE FROM `DCC_FILE_CATEGORY`",
        "DELETE FROM `DCC_CONTROLLED_FILE`",
    ]:
        assert forbidden not in upper
