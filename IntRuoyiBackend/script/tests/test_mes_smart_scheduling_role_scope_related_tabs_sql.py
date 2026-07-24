from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260629_mes_smart_scheduling_role_scope.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES smart scheduling role scope migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_related_tabs_sql_keeps_route_dependency_chain_for_scheduler_and_workshop_director() -> None:
    text = _read_sql()

    for menu_id in ["5101", "5160", "5161", "5170", "5171", "5300", "5310", "5311", "5320", "5321", "5700", "5720", "5721"]:
        assert f"UNION ALL SELECT {menu_id}" in text or f"SELECT {menu_id} AS `menu_id`" in text


def test_related_tabs_sql_keeps_work_order_dependency_for_scheduler_and_workshop_director() -> None:
    text = _read_sql()

    for menu_id in ["5530", "5531"]:
        assert f"UNION ALL SELECT {menu_id}" in text or f"SELECT {menu_id} AS `menu_id`" in text


def test_related_tabs_sql_does_not_expand_team_leader_to_route_dependency_chain() -> None:
    text = _read_sql()

    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1].split(
        "UPDATE `system_role_menu` AS `role_menu`"
    )[0]

    for required in ["5530", "5531", "5720", "5721"]:
        assert required in team_leader_block

    for forbidden in ["5160", "5161", "5170", "5171", "5310", "5311", "5320", "5321"]:
        assert forbidden not in team_leader_block
