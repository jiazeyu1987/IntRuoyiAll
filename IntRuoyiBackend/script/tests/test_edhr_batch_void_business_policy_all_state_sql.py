from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260723_z_edhr_batch_void_business_policy_all_state.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR batch void ALL-state business policy migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_batch_void_business_policy_all_state_declares_scope_and_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260723_unify_form_action_policy_into_business_approval_policy,20260714_mes_edhr_batch_execution_void_bpm_seed; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_edhr_batch_void_business_policy_all_state" in sql
    assert "requires bpm_business_approval_policy" in sql
    assert "requires active void process definition" in sql
    assert "EDHR batch void ALL-state business policy conflict" in sql


def test_edhr_batch_void_business_policy_all_state_keeps_existing_flow_contract() -> None:
    sql = read_sql()

    for literal in [
        "'MES'",
        "'EDHR_BATCH_EXECUTION'",
        "'VOID'",
        "'ALL'",
        "'BPM_REQUIRED'",
        "'mes-edhr-batch-execution-void-v1'",
        "'EDHR_BATCH_VOID'",
        "'PUBLISHED'",
    ]:
        assert literal in sql

    assert re.search(
        r"UPDATE\s+`bpm_business_approval_policy`[\s\S]+"
        r"`object_state`\s*=\s*'ALL'[\s\S]+"
        r"`object_state`\s*=\s*'CLOSED'",
        sql,
        re.I,
    )
    assert "process_definition_key" in sql
    assert "effect_executor_code" in sql


def test_edhr_batch_void_business_policy_all_state_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "CALL ensure_edhr_batch_void_business_policy_all_state();" in sql
    assert "HAVING COUNT(*) > 1" in sql
    assert "duplicate before update" in sql
    assert sql.index("duplicate before update") < sql.index("UPDATE `bpm_business_approval_policy`")
    assert "DROP PROCEDURE IF EXISTS ensure_edhr_batch_void_business_policy_all_state" in sql
    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert "fallback" not in sql.lower()
