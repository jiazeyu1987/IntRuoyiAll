from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SQL = REPO_ROOT / "sql/mysql/20260710_dcc_product_catalog_database.sql"
PROJECT_CODE_COLUMNS_SQL = REPO_ROOT / "sql/mysql/20260729_dcc_product_catalog_project_code_columns.sql"
APPLICATION_YAML = REPO_ROOT / "yudao-server/src/main/resources/application.yaml"


def test_product_catalog_migration_declares_global_table_and_seed_rows() -> None:
    sql = MIGRATION_SQL.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `dcc_product_catalog`" in sql
    assert "`tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号'" in sql
    assert "`uk_dcc_product_catalog_source_row`" in sql
    assert "ON DUPLICATE KEY UPDATE `id` = `id`" in sql
    assert "('子公司产品'," not in sql
    assert sql.count("('瑛泰产品',") == 181


def test_product_catalog_table_is_excluded_from_tenant_interceptor() -> None:
    application_yaml = APPLICATION_YAML.read_text(encoding="utf-8")

    assert "ignore-tables:\n      - dcc_product_catalog" in application_yaml


def test_product_catalog_project_code_columns_migration_backfills_exact_matches_only() -> None:
    sql = PROJECT_CODE_COLUMNS_SQL.read_text(encoding="utf-8")

    assert "dependsOn=20260710_dcc_product_catalog_database; type=data; riskLevel=medium" in sql
    assert "ADD COLUMN `project_name` varchar(255) DEFAULT NULL COMMENT ''项目名称''" in sql
    assert "ADD COLUMN `project_code` varchar(64) DEFAULT NULL COMMENT ''项目代码''" in sql
    assert "CREATE TEMPORARY TABLE `tmp_dcc_product_catalog_project_match`" in sql
    assert "`project_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL" in sql
    assert "`project_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL" in sql
    assert "HEX(catalog.`data_source`) = 'E7919BE6B3B0E4BAA7E59381'" in sql
    assert "catalog.`project_name` = project_match.`project_name`" in sql
    assert "catalog.`project_code` = project_match.`project_code`" in sql

    value_lines = [
        line
        for line in sql.splitlines()
        if line.startswith("  (") and "dcc-project-code-backfill" not in line
    ]
    assert len(value_lines) == 115
    assert "(2, '一次性使用血管鞘', 'VS')" in sql
    assert "(8, '介入手术器械包', 'ISK')" not in sql
    assert "(29, " not in sql
