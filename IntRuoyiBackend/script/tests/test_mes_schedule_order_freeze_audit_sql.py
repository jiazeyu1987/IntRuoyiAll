from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260624_mes_schedule_order_freeze_audit.sql"
)


def test_schedule_order_freeze_audit_schema_exists():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_tokens = [
        "release-migration: allowedEnvironments=test,backup,prod",
        "schedule order freeze audit migration missing required base tables",
        "schedule order freeze audit migration missing schedule order parent menu 5580",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen_time`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `frozen_by`",
        "ALTER TABLE `mes_pro_schedule_order` ADD COLUMN `freeze_reason`",
        "CREATE TABLE IF NOT EXISTS `mes_pro_schedule_order_operation_log`",
        "`operation_type` varchar(32)",
        "`before_snapshot_json` text",
        "`after_snapshot_json` text",
        "`reason` varchar(500)",
        "idx_mes_pro_schedule_order_operation_log_order",
    ]
    for token in required_tokens:
        assert token in sql


def test_schedule_order_delete_permission_is_registered_and_granted():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_tokens = [
        "mes:pro-schedule-order:delete",
        "排产工单删除",
        "system_menu",
        "system_tenant_package",
        "system_role_menu",
        "tenant_admin",
        "JSON_TABLE",
        "@existing_delete_menu_id",
        "@preferred_delete_menu_id_blocked",
        "@delete_menu_id",
        "WHERE `id` = @delete_menu_id",
    ]
    for token in required_tokens:
        assert token in sql

    assert "WHERE `id` = 5586 OR `permission` = 'mes:pro-schedule-order:delete'" not in sql


def test_schedule_order_operation_log_reason_allows_null_for_replan_apply() -> None:
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "`reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作原因'" in sql
    assert "`reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作原因'" not in sql
