from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SCHEMA_SQL = ROOT / "sql" / "mysql" / "20260614_dcc_product_visibility_group.sql"
MATRIX_SQL = ROOT / "sql" / "mysql" / "20260613_dcc_file_view_matrix_seed.sql"


def test_product_visibility_schema_defines_group_member_and_product_tables():
    sql = SCHEMA_SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260513_dcc_base_schema; type=schema; riskLevel=medium"
    )
    assert "CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group`" in sql
    assert "CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group_member`" in sql
    assert "CREATE TABLE IF NOT EXISTS `dcc_product_visibility_group_product`" in sql
    assert "`dept_id` bigint NOT NULL" in sql
    assert "`user_id` bigint NOT NULL" in sql
    assert "`product_master_id` bigint NOT NULL" in sql
    assert "uk_dcc_pvg_member" in sql
    assert "uk_dcc_pvg_product" in sql


def test_permission_rule_schema_adds_product_group_scope():
    sql = SCHEMA_SQL.read_text(encoding="utf-8")

    assert "INFORMATION_SCHEMA.COLUMNS" in sql
    assert "v_scope_type_column_count" in sql
    assert "IF v_scope_type_column_count = 0 THEN" in sql
    assert "`scope_type` varchar(32) NOT NULL DEFAULT 'GLOBAL'" in sql
    assert "ALTER TABLE `dcc_file_category_permission_rule`" in sql
    assert "idx_dcc_pvgp_product" in sql


def test_matrix_seed_marks_new_product_department_rules_as_product_group_scope():
    sql = MATRIX_SQL.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260614_dcc_product_visibility_group; type=seed; riskLevel=low"
    )
    assert "'PRODUCT_GROUP'" in sql
    assert "'GLOBAL'" in sql
    assert "WHEN grant_rule.matrix_department = '新品开发部'" in sql
    assert "subject_type = 'DEPT'" in sql
