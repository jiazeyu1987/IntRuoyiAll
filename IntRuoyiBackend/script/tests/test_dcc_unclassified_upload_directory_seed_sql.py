from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260803_dcc_unclassified_upload_directory_seed.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_unclassified_upload_directory_seed_has_release_metadata_and_contract() -> None:
    sql = _read(SQL_PATH)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260513_dcc_base_schema; type=seed; riskLevel=low"
    )
    for token in [
        "CREATE PROCEDURE apply_dcc_unclassified_upload_directory_seed()",
        "DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_TABLE_MISSING",
        "DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_DUPLICATE_ACTIVE",
        "DCC_UNCLASSIFIED_UPLOAD_DIRECTORY_SEED_INSERT_INCOMPLETE",
        "`dcc_file_directory`",
        "'UNCLASSIFIED'",
        "CONVERT(UNHEX('E69CAAE58886E7B1BB') USING utf8mb4)",
        "parent_id`, `code`",
    ]:
        assert token in sql, f"seed migration must include {token}"


def test_unclassified_upload_directory_seed_is_idempotent_and_non_destructive() -> None:
    sql = _read(SQL_PATH)
    upper = sql.upper()

    assert "NOT EXISTS" in sql
    for forbidden in [
        "DELETE FROM `DCC_FILE_DIRECTORY`",
        "TRUNCATE TABLE",
        "ON DUPLICATE KEY UPDATE",
    ]:
        assert forbidden not in upper, f"seed migration must not use destructive/upsert fallback: {forbidden}"
