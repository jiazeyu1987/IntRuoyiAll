from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260731_dcc_file_category_match_rule_seed.sql"


def migration_text() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def extract_block(sql: str, start: str, end: str) -> str:
    assert start in sql, f"Missing block start: {start}"
    assert end in sql, f"Missing block end: {end}"
    return sql[sql.index(start) : sql.index(end)]


def test_dcc_file_category_match_rule_seed_has_release_metadata() -> None:
    sql = migration_text()

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260731_dcc_file_category_match_rule; type=seed; riskLevel=low"
    )
    assert "CREATE PROCEDURE apply_dcc_file_category_match_rule_seed" in sql
    assert "CALL apply_dcc_file_category_match_rule_seed();" in sql


def test_dcc_file_category_match_rule_seed_temp_table_matches_target_collation() -> None:
    sql = migration_text()
    table_sql = extract_block(
        sql,
        "CREATE TEMPORARY TABLE `tmp_dcc_file_category_match_rule_seed`",
        "INSERT INTO `tmp_dcc_file_category_match_rule_seed`",
    )

    assert (
        "`category_name` VARCHAR(128) CHARACTER SET utf8mb4 "
        "COLLATE utf8mb4_unicode_ci NOT NULL"
    ) in table_sql
    assert (
        "`match_text` VARCHAR(255) CHARACTER SET utf8mb4 "
        "COLLATE utf8mb4_0900_ai_ci NOT NULL"
    ) in table_sql
    assert (
        "`match_type` VARCHAR(32) CHARACTER SET utf8mb4 "
        "COLLATE utf8mb4_0900_ai_ci NOT NULL"
    ) in table_sql
    assert "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci" in table_sql
    assert "seed.`category_name` = category.`name`" in sql
    assert "existing.`match_text` = seed.`match_text`" in sql
    assert "existing.`match_type` = seed.`match_type`" in sql


def test_dcc_file_category_match_rule_seed_required_business_rules_present() -> None:
    sql = migration_text()

    for text in ("OQ方案", "PQ报告", "sldprt", "step", "零件图纸"):
        assert text in sql
    assert "DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_MISSING" in sql
    assert "DCC_FILE_CATEGORY_MATCH_RULE_SEED_CATEGORY_AMBIGUOUS" in sql
    assert "DCC_FILE_CATEGORY_MATCH_RULE_SEED_INSERT_INCOMPLETE" in sql
