from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL = ROOT / "sql" / "mysql" / "20260915_mes_team_leader_cleanup_permission_menu.sql"


def test_cleanup_permission_is_attached_to_current_production_leader_menu():
    source = SQL.read_text(encoding="utf-8")
    assert "mes/pro/processpool/ProductionLeaderWorkbenchPage" in source
    assert "mes:pro-process-pool-team-leader:maintain" in source
    assert "SET `parent_id` = production_leader_menu_id" in source
    assert "Team leader maintain permission menu is not unique" in source
    assert "SIGNAL SQLSTATE '45000'" in source


def test_cleanup_permission_migration_is_idempotent():
    source = SQL.read_text(encoding="utf-8")
    assert "DROP PROCEDURE IF EXISTS sync_mes_team_leader_cleanup_permission_menu" in source
    assert source.count("CALL sync_mes_team_leader_cleanup_permission_menu()") == 1
    assert "INSERT INTO `system_role_menu`" not in source
