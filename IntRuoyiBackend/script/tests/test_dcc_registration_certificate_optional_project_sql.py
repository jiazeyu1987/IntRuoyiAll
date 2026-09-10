from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


OPTIONAL_PROJECT_MIGRATIONS = [
    "20260904_dcc_registration_certificate_download_optional_project_code.sql",
    "20260904_dcc_registration_certificate_download_project_optional.sql",
    "20260904_dcc_registration_certificate_download_without_project_code.sql",
]


def _sql(name: str) -> str:
    return (ROOT / "sql" / "mysql" / name).read_text(encoding="utf-8")


def test_optional_project_check_drop_migrations_are_repeatable() -> None:
    for migration in OPTIONAL_PROJECT_MIGRATIONS:
        sql = _sql(migration)

        assert "information_schema.TABLES" in sql
        assert "information_schema.TABLE_CONSTRAINTS" in sql
        assert "CONSTRAINT_TYPE = 'CHECK'" in sql
        assert "CREATE PROCEDURE" in sql
        assert "IF EXISTS (" in sql
        assert "DROP CHECK `chk_dcc_reg_cert_access_request_project`" in sql
        assert "DCC access request table missing" in sql


def test_optional_project_check_drop_migrations_do_not_use_bare_drop() -> None:
    for migration in OPTIONAL_PROJECT_MIGRATIONS:
        sql = _sql(migration)
        bare_drop = (
            "ALTER TABLE `dcc_registration_certificate_access_request`\n"
            "  DROP CHECK `chk_dcc_reg_cert_access_request_project`;"
        )

        assert bare_drop not in sql
