from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260723_mes_route_form_business_approval_policy_backfill.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES route form business approval policy backfill migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_route_form_business_policy_backfill_declares_release_metadata_and_guards() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy,20260722_mes_route_form_center_runtime_columns; "
        "type=data; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in sql
    assert "ensure_mes_route_form_business_approval_policy" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "requires bpm_form_action_policy" in sql
    assert "requires bpm_business_approval_policy" in sql
    assert "unsupported approval_mode" in sql
    assert "conflicting published business policy" in sql


def test_route_form_business_policy_backfill_copies_published_route_form_policies() -> None:
    sql = read_sql()

    for literal in [
        "'MES'",
        "'EDHR_ROUTE_FORM'",
        "'PUBLISHED'",
        "`form_policy`.`approval_mode`",
        "`form_policy`.`bpm_process_key`",
        "`form_policy`.`effect_executor_code`",
        "MES route dynamic eDHR form business approval backfill",
    ]:
        assert literal in sql

    assert re.search(
        r"INSERT\s+INTO\s+`bpm_business_approval_policy`[\s\S]+"
        r"FROM\s+`bpm_form_action_policy`\s+AS\s+`form_policy`",
        sql,
        re.I,
    )
    assert "`business_policy`.`policy_mode` = `form_policy`.`approval_mode`" in sql
    assert "`business_policy`.`status` = `form_policy`.`status`" in sql
    assert "AND NOT EXISTS (" in sql


def test_route_form_business_policy_backfill_is_idempotent_and_non_destructive() -> None:
    sql = read_sql()

    assert "DROP PROCEDURE IF EXISTS ensure_mes_route_form_business_approval_policy" in sql
    assert "CALL ensure_mes_route_form_business_approval_policy();" in sql
    assert not re.search(r"\b(DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM)\b", sql, re.I)
    assert not re.search(r"\bON\s+DUPLICATE\s+KEY\s+UPDATE\b", sql, re.I)
    assert "INSERT IGNORE" not in sql
    assert "fallback" not in sql.lower()
