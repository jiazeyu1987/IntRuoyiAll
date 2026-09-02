from pathlib import Path


REPO_ROOT = Path(__file__).parents[2]
MIGRATION = REPO_ROOT / "sql/mysql/20260903_dcc_explicit_data_relation.sql"


def test_explicit_relation_migration_has_three_id_identity_and_tenant_indexes():
    sql = MIGRATION.read_text(encoding="utf-8")
    assert "CREATE TABLE IF NOT EXISTS `dcc_data_relation`" in sql
    assert "`product_catalog_id` bigint NOT NULL" in sql
    assert "`project_code_id` bigint NOT NULL" in sql
    assert "`registration_certificate_id` bigint NOT NULL" in sql
    assert "UNIQUE KEY `uk_dcc_data_relation_identity` (`tenant_id`, `product_catalog_id`, `project_code_id`, `registration_certificate_id`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_dcc_data_relation_catalog_project` (`tenant_id`, `product_catalog_id`, `project_code_id`, `deleted`)" in sql
    assert "KEY `idx_dcc_data_relation_project_code` (`tenant_id`, `project_code_id`, `deleted`)" in sql
    assert "KEY `idx_dcc_data_relation_registration_certificate` (`tenant_id`, `registration_certificate_id`, `deleted`)" in sql


def test_explicit_relation_migration_has_recovery_and_no_destructive_statement():
    sql = MIGRATION.read_text(encoding="utf-8")
    assert "Rollback:" in sql
    assert "DROP TABLE" in sql
    assert "ALTER TABLE" not in sql
