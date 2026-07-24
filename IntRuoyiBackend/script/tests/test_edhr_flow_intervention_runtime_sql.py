from pathlib import Path


SQL_PATH = Path(__file__).resolve().parents[2] / "sql" / "mysql" / "20260626_mes_edhr_flow_intervention_runtime.sql"


def test_edhr_flow_intervention_runtime_sql_creates_missing_tables_without_menu_side_effects():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260618_mes_edhr_flow_intervention_log; type=schema; riskLevel=low"
    )
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_event`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_edhr_flow_intervention`" in sql
    assert "idx_mes_pro_edhr_flow_event_object" in sql
    assert "idx_mes_pro_edhr_flow_event_instance" in sql
    assert "uk_mes_pro_edhr_flow_intervention_idempotency" in sql
    assert "idx_mes_pro_edhr_flow_intervention_object" in sql

    for forbidden in [
        "INSERT INTO `system_menu`",
        "system_tenant_package",
        "system_role_menu",
        "tmp_mes_edhr_flow_intervention_legacy_menu_map",
    ]:
        assert forbidden not in sql
