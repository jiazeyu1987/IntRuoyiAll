from pathlib import Path


SQL = Path("sql/mysql/20260612_erp_kingdee_sync_menu.sql").read_text(encoding="utf-8")


def test_sync_menu_is_idempotent_and_bound_to_erp_parent():
    assert "WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6013)" in SQL
    assert "WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6014)" in SQL
    assert "2563, 'kingdee-sync'" in SQL
    assert "'erp/sync/index'" in SQL


def test_sync_menu_grants_query_permission_without_extra_trigger_permission():
    assert "'erp:kingdee-sync:query'" in SQL
    assert "JSON_CONTAINS(CAST(`menu_ids` AS JSON), CAST('2563' AS JSON), '$')" in SQL
    assert "infra:job:trigger" not in SQL
