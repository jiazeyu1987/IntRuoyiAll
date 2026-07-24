from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL = (REPO_ROOT / "sql/mysql/20260721_mes_edhr_execution_list_retire.sql").read_text(
    encoding="utf-8"
)


def test_execution_list_menu_is_retired_not_hidden_route() -> None:
    old_route = "/mes/pro/feedback/" + "edhr-execution"
    old_component = "mes/pro/edhr/" + "ExecutionListPage"
    old_component_name = "MesProFeedbackEdhr" + "ExecutionListPage"

    assert "ensure_mes_edhr_execution_list_retired" in SQL
    assert "`name` = '已废弃-eDHR执行列表'" in SQL
    assert "`permission` = 'RETIRED_EDHR_EXECUTION_LIST'" in SQL
    assert "`path` = '/retired/edhr-execution-list'" in SQL
    assert "`component_name` = 'RETIRED_EDHR_EXECUTION_LIST'" in SQL
    assert "`deleted` = b'1'" in SQL
    assert "DELETE FROM `system_role_menu`" in SQL
    assert "WHERE `menu_id` = 900023" in SQL
    assert "`existing_menu`.`menu_id` <> 900023" in SQL
    assert old_route not in SQL
    assert old_component not in SQL
    assert old_component_name not in SQL
