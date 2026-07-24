from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_r12_approval_print_template_migration_is_tenant_scoped_and_menu_seeded() -> None:
    migration_path = REPO_ROOT / "sql" / "mysql" / "20260527_dcc_approval_print_template.sql"
    text = migration_path.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `dcc_approval_print_template`" in text
    assert "`template_file_id` bigint NOT NULL" in text
    assert "`tenant_id` bigint NOT NULL" in text
    assert "dcc:controlled-file:print-template:manage" in text
    assert "controlled-file/print-template" in text


def test_r12_approval_print_template_test_schema_contains_table() -> None:
    schema_path = REPO_ROOT / "yudao-module-dcc" / "src" / "test" / "resources" / "sql" / "create_tables.sql"
    text = schema_path.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `dcc_approval_print_template`" in text
    assert "`template_file_id` BIGINT NOT NULL" in text
    assert "`tenant_id` BIGINT NOT NULL" in text
