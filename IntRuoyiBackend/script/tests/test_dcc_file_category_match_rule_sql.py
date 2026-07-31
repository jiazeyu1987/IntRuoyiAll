from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_SQL = REPO_ROOT / "sql/mysql/20260731_dcc_file_category_match_rule.sql"
SEED_SQL = REPO_ROOT / "sql/mysql/20260731_dcc_file_category_match_rule_seed.sql"
TEST_SCHEMA_SQL = REPO_ROOT / "yudao-module-dcc/src/test/resources/sql/create_tables.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_dcc_file_category_match_rule_schema_has_release_metadata_and_contract() -> None:
    sql = _read(SCHEMA_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_dcc_file_type_taxonomy; type=schema; riskLevel=medium"
    )
    for token in [
        "CREATE TABLE IF NOT EXISTS `dcc_file_category_match_rule`",
        "`category_id` BIGINT NOT NULL",
        "`match_text` VARCHAR(255) NOT NULL",
        "`match_type` VARCHAR(32) NOT NULL DEFAULT 'CONTAINS'",
        "`weight` INT NOT NULL DEFAULT 0",
        "`active` TINYINT NOT NULL DEFAULT 1",
        "uk_dcc_file_category_match_rule_unique",
        "idx_dcc_file_category_match_rule_category",
        "idx_dcc_file_category_match_rule_type",
    ]:
        assert token in sql, f"schema migration must include {token}"


def test_dcc_file_category_match_rule_seed_is_idempotent_and_fail_fast() -> None:
    sql = _read(SEED_SQL)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260731_dcc_file_category_match_rule; type=seed; riskLevel=low"
    )
    for token in [
        "CREATE PROCEDURE apply_dcc_file_category_match_rule_seed()",
        "DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_MISSING",
        "DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_AMBIGUOUS",
        "DCC_FILE_CATEGORY_MATCH_RULE_SEED_INSERT_INCOMPLETE",
        "NOT EXISTS",
        "过程运行确认（OQ）报告",
        "过程性能确认（PQ）报告",
        "零配件图纸",
        "'sldprt', 'EXTENSION', 800",
    ]:
        assert token in sql, f"seed migration must include {token}"

    upper = sql.upper()
    assert "UPDATE `DCC_CONTROLLED_FILE`" not in upper
    assert "DELETE FROM `DCC_" not in upper
    assert "TRUNCATE TABLE" not in upper


def test_dcc_file_category_match_rule_test_schema_is_aligned() -> None:
    sql = _read(TEST_SCHEMA_SQL)

    assert "CREATE TABLE IF NOT EXISTS `dcc_file_category_match_rule`" in sql
    assert "uk_dcc_file_category_match_rule_unique" in sql
    assert "idx_dcc_file_category_match_rule_category" in sql
    assert "idx_dcc_file_category_match_rule_type" in sql
