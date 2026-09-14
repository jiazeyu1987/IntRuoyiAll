from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_route_dcc_binding_schema_is_repeatable_and_complete() -> None:
    migration = (
        REPO_ROOT / "sql" / "mysql" / "20260813_mes_route_dcc_project_binding_schema.sql"
    ).read_text(encoding="utf-8")

    assert "-- release-migration:" in migration
    assert "dependsOn=20260512_mes_base_schema,20260513_dcc_base_schema" in migration
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_dcc_project_binding`" in migration
    assert "`route_id` bigint NOT NULL" in migration
    assert "`dcc_project_code_id` bigint NOT NULL" in migration
    assert "`version` bigint NOT NULL" in migration
    assert "`active_route_id` BIGINT GENERATED ALWAYS AS" in migration
    assert "UNIQUE KEY `uk_mes_pro_route_dcc_current` (`tenant_id`, `active_route_id`)" in migration
    assert (
        "UNIQUE KEY `uk_mes_pro_route_dcc_history_version` "
        "(`tenant_id`, `route_id`, `version`)" in migration
    )
    assert "KEY `idx_mes_pro_route_dcc_project` (`tenant_id`, `dcc_project_code_id`)" in migration
    assert "DROP TABLE" not in migration


def test_local_restart_applies_route_dcc_binding_schema() -> None:
    restart_script = (
        REPO_ROOT / "script" / "deploy" / "restart-int-ruoyi-local.ps1"
    ).read_text(encoding="utf-8")

    assert "MES route DCC project binding schema" in restart_script
    assert "20260813_mes_route_dcc_project_binding_schema.sql" in restart_script
    assert "TABLE_NAME = 'mes_pro_route_dcc_project_binding'" in restart_script
    assert "COLUMN_NAME IN ('route_id', 'dcc_project_code_id', 'version', 'active_route_id')" in restart_script
    assert "INDEX_NAME IN (" in restart_script
    assert "'uk_mes_pro_route_dcc_current'" in restart_script
    assert "'uk_mes_pro_route_dcc_history_version'" in restart_script
    assert "'idx_mes_pro_route_dcc_project'" in restart_script
