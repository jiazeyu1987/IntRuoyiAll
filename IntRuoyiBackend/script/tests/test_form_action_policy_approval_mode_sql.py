from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
BASE_SQL_PATH = ROOT / "sql" / "mysql" / "20260719_business_approval_policy.sql"
MIGRATION_SQL_PATH = ROOT / "sql" / "mysql" / "20260721_form_action_policy_approval_mode.sql"
EDHR_SEED_SQL_PATH = ROOT / "sql" / "mysql" / "20260720_edhr_release_void_form_policy_seed.sql"
TEST_SCHEMA_PATH = ROOT / "yudao-module-bpm" / "src" / "test" / "resources" / "sql" / "create_tables.sql"


def read(path: Path) -> str:
    assert path.exists(), f"missing SQL artifact: {path}"
    return path.read_text(encoding="utf-8")


def test_business_approval_policy_base_and_test_schema_expose_form_slots() -> None:
    base_sql = read(BASE_SQL_PATH)
    test_schema = read(TEST_SCHEMA_PATH)

    for source in [base_sql, test_schema]:
        assert "form_policy_type" in source
        assert "form_slots_json" in source
        assert "policy_mode" in source
        assert "BPM_REQUIRED" in source

    assert re.search(
        r"`form_policy_type`\s+varchar\(32\)\s+DEFAULT\s+NULL",
        base_sql,
        re.I,
    )
    assert re.search(
        r'"form_policy_type"\s+varchar\(32\)',
        test_schema,
        re.I,
    )


def test_business_approval_policy_form_slots_migration_is_fail_fast_and_non_destructive() -> None:
    sql = read(MIGRATION_SQL_PATH)

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=schema; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_business_approval_policy_form_slots" in sql
    assert "bpm_business_approval_policy" in sql
    assert "form_policy_type" in sql
    assert "form_slots_json" in sql
    assert "policy_mode" in sql
    assert "BPM_REQUIRED" in sql
    assert "DIRECT" in sql
    assert "BUSINESS_APPROVAL_POLICY_MODE_INVALID" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Business approval policy form slots require bpm_business_approval_policy" in sql
    assert "bpm_form_action_policy" not in sql

    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert "fallback" not in sql.lower()


def test_edhr_release_void_seed_sets_explicit_bpm_required_policy_mode() -> None:
    sql = read(EDHR_SEED_SQL_PATH)

    assert "`policy_mode`" in sql
    assert "'BPM_REQUIRED'" in sql
    assert "COALESCE(`policy`.`policy_mode`, '') <> 'BPM_REQUIRED'" in sql
    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"`policy_mode`[\s\S]+`process_definition_key`[\s\S]+`effect_executor_code`",
        sql,
        re.I,
    )
