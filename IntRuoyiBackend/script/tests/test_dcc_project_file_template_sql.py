from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260910_dcc_project_file_template.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"
TEST_CLEAN_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/clean.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_project_file_template_migration_has_release_and_schema_contract() -> None:
    sql = _read(MIGRATION_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium"
    )
    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_project_file_template_item`",
        "`project_code_id` BIGINT NOT NULL",
        "`file_type_taxonomy_id` BIGINT NOT NULL",
        "`file_name` VARCHAR(255) NOT NULL",
        "`sort_order` INT NOT NULL DEFAULT 0",
        "`tenant_id` BIGINT NOT NULL DEFAULT 0",
        "`active_unique_flag` BIGINT GENERATED ALWAYS AS",
        "uk_dcc_project_file_template_item_active",
        "idx_dcc_project_file_template_project",
        "idx_dcc_project_file_template_taxonomy",
    ]:
        assert token in sql, f"project template migration must include {token}"

    upper = sql.upper()
    for forbidden in ["INSERT INTO `DCC_PROJECT_FILE_TEMPLATE_ITEM`", "UPDATE `DCC_PROJECT_CODE`", "TRUNCATE TABLE"]:
        assert forbidden not in upper, f"schema migration must not guess/backfill template data: {forbidden}"


def test_project_file_template_test_schema_and_cleanup_are_aligned() -> None:
    schema = _read(TEST_SCHEMA_SQL)
    clean = _read(TEST_CLEAN_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_project_file_template_item`" in schema
    assert "`active_unique_flag` BIGINT GENERATED ALWAYS AS" in schema
    assert "uk_dcc_project_file_template_item_active" in schema
    assert "DELETE FROM `dcc_project_file_template_item`;" in clean
    assert clean.index("DELETE FROM `dcc_project_file_template_item`;") < clean.index(
        "DELETE FROM `dcc_project_code`;"
    )
