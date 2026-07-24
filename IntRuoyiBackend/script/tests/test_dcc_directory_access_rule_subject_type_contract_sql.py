from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_dcc_directory_access_rule_subject_type_contract_migration_is_idempotent() -> None:
    migration = (
        REPO_ROOT / "sql/mysql/20260527_dcc_directory_access_rule_subject_type_contract.sql"
    ).read_text(encoding="utf-8")

    assert "DROP TABLE" not in migration.upper()
    assert "TRUNCATE TABLE" not in migration.upper()
    assert "DELETE FROM" not in migration.upper()
    assert "UPDATE `dcc_directory_access_rule`" in migration
    assert "WHEN '1' THEN 'USER'" in migration
    assert "WHEN '2' THEN 'DEPT'" in migration
    assert "WHEN '3' THEN 'ROLE'" in migration
    assert "WHEN '4' THEN 'POSITION'" in migration
    assert "WHERE `subject_type` IN ('1', '2', '3', '4')" in migration

