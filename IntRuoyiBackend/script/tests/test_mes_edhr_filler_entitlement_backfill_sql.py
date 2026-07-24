from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260718_mes_edhr_filler_entitlement_backfill.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def test_filler_entitlement_backfill_declares_release_metadata() -> None:
    sql = read_sql()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260718_system_entitlement_management; type=data; riskLevel=medium\n"
    )


def test_filler_entitlement_backfill_uses_dynamic_entitlement_ledger_only() -> None:
    sql = read_sql()

    assert "MES_EDHR_FILLER_MINIMAL" in sql
    assert "system_entitlement_claim" in sql
    assert "system_entitlement_grant" in sql
    assert "system_entitlement_audit_event" in sql

    forbidden_static_mutations = {
        "system_user_role",
        "system_role_menu",
        "INSERT INTO system_role",
        "INSERT INTO `system_role`",
        "@EDHR_FILLER_ROLE_CODE",
    }
    normalized_sql = sql.lower()
    for forbidden in forbidden_static_mutations:
        assert forbidden.lower() not in normalized_sql


def test_filler_entitlement_backfill_supports_user_and_role_sources_with_fail_fast_checks() -> None:
    sql = read_sql()

    assert "candidate_source_type IN ('USER', 'USERS')" in sql
    assert "candidate_source_type = 'ROLE'" in sql
    assert "JSON_TABLE" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "Missing eDHR filler entitlement policy" in sql
    assert "Unsupported eDHR filler candidate source type" in sql
    assert "Missing or disabled eDHR filler user" in sql
    assert "Missing or disabled eDHR filler role" in sql
    assert "Empty eDHR filler role candidate pool" in sql


def test_filler_entitlement_backfill_builds_stable_source_keys_and_rebuilds_grants() -> None:
    sql = read_sql()

    assert "FORM|" in sql
    assert "ROUTE|" in sql
    assert "EDHR_PROCESS_FORM_FILLER" in sql
    assert "UPDATE system_entitlement_claim c" in sql
    assert "UPDATE system_entitlement_grant g" in sql
    assert "active_claim_count" in sql
