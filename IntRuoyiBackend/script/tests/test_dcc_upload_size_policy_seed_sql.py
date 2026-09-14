from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260803_dcc_upload_size_policy_default_seed.sql"


def _read(path: Path) -> str:
    assert path.exists(), f"missing SQL file: {path}"
    return path.read_text(encoding="utf-8")


def test_dcc_upload_size_policy_seed_has_release_metadata_and_fail_fast_contract() -> None:
    sql = _read(SQL_PATH)

    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260528_dcc_controlled_file_protection; type=seed; riskLevel=low"
    )
    for token in [
        "CREATE PROCEDURE apply_dcc_upload_size_policy_default_seed()",
        "DCC_UPLOAD_SIZE_POLICY_DEFAULT_SEED_TABLE_MISSING",
        "DCC_UPLOAD_SIZE_POLICY_DEFAULT_SEED_INSERT_INCOMPLETE",
        "tmp_dcc_upload_size_policy_default_seed",
        "dcc_controlled_file_upload_policy",
        "dcc_file_category",
        "NOT EXISTS",
    ]:
        assert token in sql, f"seed migration must include {token}"


def test_dcc_upload_size_policy_seed_covers_supported_upload_purposes() -> None:
    sql = _read(SQL_PATH)

    for purpose in [
        "SOURCE",
        "DRAWING_PDF",
        "TRAINING_RECORD",
        "EXTERNAL_REVIEW_OUTPUT",
    ]:
        assert f"'{purpose}'" in sql, f"seed migration must include {purpose}"
        assert f"'DCC_UPLOAD_DEFAULT_{purpose}_V1'" in sql

    assert "10485760" in sql, "default maxBytes must be an explicit 10 MiB policy value"


def test_dcc_upload_size_policy_seed_is_non_destructive_and_no_runtime_fallback() -> None:
    sql = _read(SQL_PATH)
    upper = sql.upper()

    for forbidden in [
        "UPDATE `DCC_CONTROLLED_FILE_UPLOAD_POLICY`",
        "DELETE FROM `DCC_CONTROLLED_FILE_UPLOAD_POLICY`",
        "TRUNCATE TABLE",
        "ON DUPLICATE KEY UPDATE",
    ]:
        assert forbidden not in upper, f"seed migration must not use destructive/upsert fallback: {forbidden}"
