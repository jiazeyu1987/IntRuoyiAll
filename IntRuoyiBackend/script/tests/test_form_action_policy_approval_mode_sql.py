from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
BASE_SQL_PATH = ROOT / "sql" / "mysql" / "20260717_bpm_form_center.sql"
MIGRATION_SQL_PATH = ROOT / "sql" / "mysql" / "20260721_form_action_policy_approval_mode.sql"
EDHR_SEED_SQL_PATH = ROOT / "sql" / "mysql" / "20260720_edhr_release_void_form_policy_seed.sql"
TEST_SCHEMA_PATH = ROOT / "yudao-module-bpm" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read(path: Path) -> str:
    assert path.exists(), f"missing SQL artifact: {path}"
    return path.read_text(encoding="utf-8")


def test_form_action_policy_base_and_test_schema_expose_approval_mode() -> None:
    base_sql = read(BASE_SQL_PATH)
    test_schema = read(TEST_SCHEMA_PATH)

    for source in [base_sql, test_schema]:
        assert "approval_mode" in source
        assert "BPM_REQUIRED" in source

    assert re.search(
        r"`approval_mode`\s+varchar\(32\)\s+NOT\s+NULL\s+DEFAULT\s+'BPM_REQUIRED'",
        base_sql,
        re.I,
    )
    assert re.search(
        r'"approval_mode"\s+varchar\(32\)\s+NOT\s+NULL\s+DEFAULT\s+\'BPM_REQUIRED\'',
        test_schema,
        re.I,
    )


def test_form_action_policy_approval_mode_migration_is_fail_fast_and_non_destructive() -> None:
    sql = read(MIGRATION_SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260717_bpm_form_center; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_form_action_policy_approval_mode" in sql
    assert "bpm_form_action_policy" in sql
    assert "approval_mode" in sql
    assert "BPM_REQUIRED" in sql
    assert "DIRECT" in sql
    assert "FORM_APPROVAL_MODE_INVALID" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Form action approval mode requires bpm_form_action_policy" in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert "fallback" not in sql.lower()


def test_edhr_release_void_seed_sets_explicit_bpm_required_approval_mode() -> None:
    sql = read(EDHR_SEED_SQL_PATH)

    assert "`approval_mode`" in sql
    assert "'BPM_REQUIRED'" in sql
    assert "COALESCE(`policy`.`approval_mode`, '') <> 'BPM_REQUIRED'" in sql
    assert re.search(
        r"INSERT\s+INTO\s+`bpm_form_action_policy`[\s\S]+"
        r"`policy_type`[\s\S]+`approval_mode`[\s\S]+`bpm_process_key`",
        sql,
        re.I,
    )

