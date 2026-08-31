from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260830_mes_batch_record_report_project_code.sql"


def test_project_code_migration_depends_on_report_schema_not_data_switch() -> None:
    metadata = SQL_PATH.read_text(encoding="utf-8").splitlines()[0]

    assert "dependsOn=20260514_mes_batch_record_report" in metadata
    assert "20260829_mes_old_form_template_binding_switch" not in metadata
    assert "type=schema" in metadata


def test_project_code_migration_only_adds_the_project_code_column() -> None:
    migration = SQL_PATH.read_text(encoding="utf-8")

    assert "TABLE_NAME = 'mes_pro_batch_record_report'" in migration
    assert "COLUMN_NAME = 'project_code'" in migration
    assert "ADD COLUMN `project_code` varchar(64) DEFAULT NULL" in migration
    assert "INSERT INTO" not in migration.upper()
    assert "UPDATE `" not in migration.upper()
    assert "DELETE FROM" not in migration.upper()
