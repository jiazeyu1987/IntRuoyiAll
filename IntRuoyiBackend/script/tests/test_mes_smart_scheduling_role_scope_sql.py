from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260629_mes_smart_scheduling_role_scope.sql"


def _read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES smart scheduling role scope migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_role_scope_sql_declares_expected_roles_and_targets() -> None:
    text = _read_sql()

    required = [
        "SET NAMES utf8mb4;",
        "ensure_mes_smart_scheduling_role_scope",
        "排产员",
        "车间主任",
        "班组长",
        "mes_scheduler",
        "mes_workshop_director",
        "mes_team_leader",
        "900120",
        "900104",
        "5550",
        "5580",
        "900121",
        "tmp_mes_role_scope_target_tenants",
        "tmp_mes_role_scope_effective_allowed_menu",
        "system_tenant_package",
        "JSON_VALID(`tenant_package`.`menu_ids`)",
    ]

    for snippet in required:
        assert snippet in text


def test_role_scope_sql_contains_three_distinct_allowed_menu_sets() -> None:
    text = _read_sql()

    assert "tmp_mes_role_scope_allowed_menu" in text
    assert "scheduler" in text
    assert "workshop_director" in text
    assert "team_leader" in text
    for menu_id in ["900120", "5590", "5580", "5550", "5262", "900121", "5540", "5985"]:
        assert f"UNION ALL SELECT {menu_id}" in text or f"SELECT {menu_id} AS `menu_id`" in text
    for menu_id in ["900120", "5580", "5550", "900121"]:
        assert f"UNION ALL SELECT {menu_id}" in text or f"SELECT {menu_id} AS `menu_id`" in text
    for menu_id in ["900120", "5550", "5551", "5552", "5553"]:
        assert f"UNION ALL SELECT {menu_id}" in text or f"SELECT {menu_id} AS `menu_id`" in text
    assert "900104" in text


def test_role_scope_sql_excludes_puhui_from_scheduler_and_workbench_from_workshop_director() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]

    assert "900104" not in scheduler_block
    assert "5590" not in workshop_block


def test_role_scope_sql_keeps_scheduler_workbench_update_for_scheduler_only() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]

    assert "900170" in scheduler_block
    assert "900171" not in scheduler_block
    assert "900170" not in workshop_block


def test_role_scope_sql_keeps_auto_schedule_permissions_for_scheduler_only() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    for menu_id in ["900180", "900181", "900182"]:
        assert menu_id in scheduler_block, f"scheduler block must keep auto-schedule permission {menu_id}"
        assert menu_id not in workshop_block, f"workshop director block must not expand to auto-schedule permission {menu_id}"
        assert menu_id not in team_leader_block, f"team leader block must not expand to auto-schedule permission {menu_id}"


def test_role_scope_sql_keeps_schedule_order_sync_permissions_for_scheduler() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]

    for menu_id in ["5581", "5582", "5584", "5585", "5587"]:
        assert menu_id in scheduler_block, f"scheduler block must keep schedule-order permission {menu_id}"


def test_role_scope_sql_keeps_scheduler_task_query_for_pro_task_page() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    assert "5540" in scheduler_block, "scheduler block must keep the pro-task page menu"
    assert "5541" in scheduler_block, "scheduler block must keep pro-task query permission for gantt/page/get requests"
    assert "5541" not in workshop_block, "workshop director block must not grow into scheduler-only pro-task query scope"
    assert "5541" not in team_leader_block, "team leader block must not grow into scheduler-only pro-task query scope"


def test_role_scope_sql_keeps_scheduler_schedule_order_update_only_for_scheduler() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    assert "5583" in scheduler_block, "scheduler block must keep schedule-order update permission for freeze/unfreeze/adjust/sync-progress"
    assert "5583" not in workshop_block, "workshop director block must not grow into scheduler-only schedule-order update scope"
    assert "5583" not in team_leader_block, "team leader block must not grow into scheduler-only schedule-order update scope"


def test_role_scope_sql_keeps_scheduler_process_schedule_route_save_permission() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]

    assert "900121" in scheduler_block, "scheduler block must keep process schedule route page permission"
    assert "900122" in scheduler_block, "scheduler block must keep process schedule route save permission"


def test_role_scope_sql_keeps_scheduler_route_flow_list_operation_permissions() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    for menu_id in ["5723", "5730"]:
        assert menu_id in scheduler_block, f"scheduler block must keep route-flow list operation permission {menu_id}"
        assert menu_id not in workshop_block, f"workshop director block must not grow into scheduler-only route-flow list permission {menu_id}"
        assert menu_id not in team_leader_block, f"team leader block must not grow into scheduler-only route-flow list permission {menu_id}"


def test_role_scope_sql_keeps_scheduler_work_order_button_permissions_only_for_scheduler() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    for menu_id in ["5532", "5535", "900200"]:
        assert menu_id in scheduler_block, f"scheduler block must keep work-order button permission {menu_id}"
        assert menu_id not in workshop_block, f"workshop director block must not grow into scheduler-only work-order button permission {menu_id}"
        assert menu_id not in team_leader_block, f"team leader block must not grow into scheduler-only work-order button permission {menu_id}"


def test_role_scope_sql_keeps_scheduler_feedback_button_permissions_without_expanding_other_roles() -> None:
    text = _read_sql()

    scheduler_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'scheduler'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'"
    )[0]
    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]
    team_leader_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'")[1]

    for menu_id in ["5552", "5553", "5555", "5969"]:
        assert menu_id in scheduler_block, f"scheduler block must keep feedback button permission {menu_id}"
        assert menu_id not in workshop_block, f"workshop director block must not grow into scheduler-only feedback button permission {menu_id}"
    for menu_id in ["5555", "5969"]:
        assert menu_id not in team_leader_block, f"team leader block must not grow into scheduler-only feedback button permission {menu_id}"


def test_role_scope_sql_declares_button_permission_menu_baseline() -> None:
    text = _read_sql()

    assert "WHERE `id` IN (900120, 5590, 5580, 5550, 5262, 900121, 900122, 5540, 900104, 5985, 5551, 5552, 5553, 5532, 5535, 5555, 5969, 900200, 5723, 5730)" in text
    assert ") <> 20 THEN" in text


def test_role_scope_sql_keeps_minimum_schedule_order_query_for_workshop_director() -> None:
    text = _read_sql()

    workshop_block = text.split("INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'workshop_director'")[1].split(
        "INSERT INTO `tmp_mes_role_scope_allowed_menu` (`scope_key`, `menu_id`)\n  SELECT 'team_leader'"
    )[0]

    assert "5581" in workshop_block, "workshop director block must keep schedule-order query permission"
    for menu_id in ["5582", "5584", "5585"]:
        assert menu_id not in workshop_block, f"workshop director block must not expand to schedule-order permission {menu_id}"


def test_role_scope_sql_soft_deletes_out_of_scope_role_menus_instead_of_deleting() -> None:
    text = _read_sql()

    assert "UPDATE `system_role_menu` AS `role_menu`" in text
    assert "`role_menu`.`deleted` = b'1'" in text
    assert "`effective_allowed_menu`.`menu_id` IS NULL" in text
    assert "DELETE FROM `system_role_menu`" not in text


def test_role_scope_sql_restores_or_inserts_missing_bindings_idempotently() -> None:
    text = _read_sql()

    assert "SET `role_menu`.`deleted` = b'0'" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "NOT EXISTS (" in text
    assert "FROM `system_role_menu` AS `existing`" in text
    assert "JOIN `tmp_mes_role_scope_effective_allowed_menu` AS `effective_allowed_menu`" in text


def test_role_scope_sql_creates_team_leader_role_when_missing() -> None:
    text = _read_sql()

    assert "INSERT INTO `system_role`" in text
    assert "'mes_team_leader'" in text
    assert "'班组长'" in text
    assert "SELECT COALESCE(MAX(`existing_role`.`id`), 910238) + 1" in text
    assert "WHERE NOT EXISTS (" in text


def test_role_scope_sql_creates_workshop_director_role_when_missing() -> None:
    text = _read_sql()

    assert "'车间主任', 'mes_workshop_director'" in text
    assert "'MES 智能排产车间主任'" in text
    assert "SELECT COALESCE(MAX(`existing_role`.`id`), 910237) + 1" in text
    assert "WHERE NOT EXISTS (" in text
    assert "Missing enabled MES workshop director role in tenant 1" not in text


def test_role_scope_sql_creates_scheduler_role_when_missing_in_admin_tenant() -> None:
    text = _read_sql()

    assert "DECLARE v_scheduler_role_id BIGINT DEFAULT NULL;" in text
    assert "SET `name` = '排产员'," in text
    assert "`code` = 'mes_scheduler'" in text
    assert "'排产员', 'mes_scheduler'" in text
    assert "'MES 智能排产排产员'" in text
    assert "SELECT COALESCE(MAX(`existing_role`.`id`), 910236) + 1" in text
    assert "SELECT `id`\n  INTO v_scheduler_role_id" in text
    assert "SELECT 'scheduler', v_scheduler_role_id, 1" in text
    assert "Missing enabled MES scheduler role in tenant 1" not in text


def test_role_scope_sql_recovers_disabled_or_deleted_director_and_team_leader_roles() -> None:
    text = _read_sql()

    assert "UPDATE `system_role`" in text
    assert "SET `name` = '车间主任'," in text
    assert "`code` = 'mes_workshop_director'" in text
    assert "SET `name` = '班组长'," in text
    assert "`code` = 'mes_team_leader'" in text
    assert "`status` = 0," in text
    assert "`deleted` = b'0'," in text
    assert "WHERE `tenant_id` = 1\n    AND (`name` = '车间主任' OR `code` = 'mes_workshop_director');" in text
    assert "WHERE `tenant_id` = 1\n    AND (`name` = '班组长' OR `code` = 'mes_team_leader');" in text


def test_role_scope_sql_uses_insert_select_where_false_safe_pattern_for_role_creation() -> None:
    text = _read_sql()

    assert "FROM DUAL" in text
    assert "WHERE NOT EXISTS (\n    SELECT 1\n    FROM `system_role`\n    WHERE `deleted` = b'0'\n      AND `tenant_id` = 1\n      AND (`name` = '车间主任' OR `code` = 'mes_workshop_director')\n  );" in text
    assert "WHERE NOT EXISTS (\n    SELECT 1\n    FROM `system_role`\n    WHERE `deleted` = b'0'\n      AND `tenant_id` = 1\n      AND (`name` = '班组长' OR `code` = 'mes_team_leader')\n  );" in text


def test_role_scope_sql_resolves_single_enabled_target_role_ids_for_followup_assignment() -> None:
    text = _read_sql()

    assert "DECLARE v_workshop_director_role_id BIGINT DEFAULT NULL;" in text
    assert "SELECT `id`\n  INTO v_workshop_director_role_id" in text
    assert "ORDER BY CASE WHEN `code` = 'mes_workshop_director' THEN 0 ELSE 1 END, `id`" in text
    assert "SELECT 'workshop_director', v_workshop_director_role_id, 1" in text
    assert "SELECT `id`\n  INTO v_team_leader_role_id" in text


def test_role_scope_sql_targets_non_admin_tenants_with_existing_mes_scheduler_and_smart_package() -> None:
    text = _read_sql()

    assert "INSERT IGNORE INTO `tmp_mes_role_scope_target_tenants` (`tenant_id`)" in text
    assert "VALUES (1);" in text
    assert "FROM `system_role` AS `role`" in text
    assert "`role`.`tenant_id` <> 1" in text
    assert "(`role`.`name` = '排产员' OR `role`.`code` = 'mes_scheduler')" in text
    assert "JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST('900120' AS JSON), '$')" in text


def test_role_scope_sql_collects_non_admin_workshop_director_and_team_leader_roles_from_target_tenants() -> None:
    text = _read_sql()

    assert "INSERT IGNORE INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)\n  SELECT 'workshop_director', `role`.`id`, `role`.`tenant_id`" in text
    assert "INSERT IGNORE INTO `tmp_mes_role_scope_targets` (`scope_key`, `role_id`, `tenant_id`)\n  SELECT 'team_leader', `role`.`id`, `role`.`tenant_id`" in text
    assert "(`role`.`name` = '车间主任' OR `role`.`code` = 'mes_workshop_director')" in text
    assert "(`role`.`name` = '班组长' OR `role`.`code` = 'mes_team_leader')" in text


def test_role_scope_sql_filters_non_admin_effective_menus_by_tenant_package() -> None:
    text = _read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_mes_role_scope_effective_allowed_menu`" in text
    assert "WHERE `target_role`.`tenant_id` = 1;" in text
    assert "WHERE `target_role`.`tenant_id` <> 1" in text
    assert "JSON_CONTAINS(CAST(`tenant_package`.`menu_ids` AS JSON), CAST(CONCAT('', `allowed_menu`.`menu_id`) AS JSON), '$')" in text
    assert "JOIN `tmp_mes_role_scope_effective_allowed_menu` AS `effective_allowed_menu`" in text
