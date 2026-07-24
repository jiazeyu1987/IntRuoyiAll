from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_ROOT = REPO_ROOT / "sql" / "mysql"


SCRIPTS = {
    "20260610_mes_schedule_order_p1.sql": [
        "mes_pro_schedule_order",
        "mes_pro_schedule_order_process",
        "mes_pro_schedule_order_diff",
        "work_order_id",
        "promise_date",
        "resource_snapshot_json",
        "shift_capacity_total",
    ],
    "20260610_mes_schedule_snapshot_p2.sql": [
        "mes_pro_schedule_order_process",
        "hourly_capacity_total",
        "shift_hours",
        "shift_capacity_total",
        "resource_snapshot_json",
    ],
    "20260610_mes_route_use_config_p3.sql": [
        "mes_pro_route_use_config",
        "mes_pro_route_use_process_config",
        "use_type",
        "config_version",
    ],
    "20260610_mes_schedule_resource_adjustment_p4.sql": [
        "mes_pro_schedule_resource_adjustment",
        "calendar_date",
        "resource_type",
        "single_hourly_capacity_override",
    ],
    "20260610_mes_auto_schedule_schedule_order_p5.sql": [
        "mes_pro_task_schedule_ext",
        "schedule_order_id",
        "schedule_order_process_id",
    ],
    "20260610_mes_feedback_import_attribution_p6.sql": [
        "mes_pro_feedback_import_record",
        "attribution_status",
        "schedule_order_process_id",
        "source_payload_json",
    ],
    "20260610_mes_scheduler_workbench_p7.sql": [
        "mes:pro-scheduler-workbench:query",
        "scheduler-workbench",
        "system_menu",
        "排产员工作台",
    ],
    "20260619_mes_schedule_order_process_snapshot_identity_p8.sql": [
        "mes_pro_schedule_order_process",
        "process_code",
        "process_name",
        "mes_pro_process",
    ],
    "20260626_mes_schedule_issue_lifecycle.sql": [
        "release-migration:",
        "dependsOn=20260610_mes_schedule_order_p1",
        "mes_pro_schedule_issue",
        "ADD COLUMN `status`",
        "ADD COLUMN `source_type`",
        "ADD COLUMN `source_id`",
        "ADD COLUMN `resolution_reason`",
        "ADD COLUMN `resolved_by`",
        "ADD COLUMN `resolved_at`",
    ],
}


def _read_sql(name: str) -> str:
    return (SQL_ROOT / name).read_text(encoding="utf-8")


def test_scheduling_closed_loop_sql_scripts_are_present_and_complete() -> None:
    for script_name, required_tokens in SCRIPTS.items():
        sql = _read_sql(script_name)
        for token in required_tokens:
            assert token in sql, f"{script_name} missing {token}"


def test_scheduling_closed_loop_sql_is_not_destructive() -> None:
    forbidden_tokens = [
        "delete from",
        "truncate table",
        "drop table",
        "drop database",
    ]

    for script_name in SCRIPTS:
        sql = _read_sql(script_name).lower()
        for token in forbidden_tokens:
            assert token not in sql, f"{script_name} contains {token}"


def test_schedule_order_menu_sql_repairs_legacy_page_permission_rows() -> None:
    sql = _read_sql("20260610_mes_schedule_order_p1.sql")

    required_tokens = [
        "WHERE `id` = 5580",
        "`component` = 'mes/pro/scheduleorder/index'",
        "WHERE `id` = 5581",
        "`permission` = 'mes:pro-schedule-order:query'",
        "`type` = 3",
        "`component` = ''",
        "WHERE `id` = 5582",
        "`permission` = 'mes:pro-schedule-order:create'",
    ]

    for token in required_tokens:
        assert token in sql


def test_kingdee_production_order_sync_job_runs_every_day_at_2am() -> None:
    sql = _read_sql("20260610_mes_schedule_order_p1.sql")

    required_tokens = [
        "kingdeeProductionOrderSyncJob",
        "每天凌晨 2 点同步 ERP 生产工单",
        "'0 0 2 * * ?'",
        "infra_job",
    ]

    for token in required_tokens:
        assert token in sql


def test_mes_nightly_replan_job_runs_every_day_after_erp_sync() -> None:
    sql = _read_sql("20260610_mes_schedule_order_p1.sql")

    required_tokens = [
        "mesProNightlyReplanJob",
        "每天凌晨 2 点 30 分重排 MES 排产工单",
        "'0 30 2 * * ?'",
        "infra_job",
    ]

    for token in required_tokens:
        assert token in sql


def test_schedule_order_process_snapshot_identity_fix_adds_missing_columns_and_backfills() -> None:
    sql = _read_sql("20260619_mes_schedule_order_process_snapshot_identity_p8.sql")

    required_tokens = [
        "release-migration:",
        "allowedEnvironments=test,backup,prod",
        "dependsOn=20260610_mes_schedule_order_p1",
        "ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `process_code`",
        "ALTER TABLE `mes_pro_schedule_order_process` ADD COLUMN `process_name`",
        "UPDATE `mes_pro_schedule_order_process` process_snapshot",
        "LEFT JOIN `mes_pro_process` process",
        "process_snapshot.`process_code` = COALESCE(process_snapshot.`process_code`, process.`code`)",
        "process_snapshot.`process_name` = COALESCE(process_snapshot.`process_name`, process.`name`)",
    ]

    for token in required_tokens:
        assert token in sql


def test_schedule_issue_lifecycle_migration_is_idempotent_and_complete() -> None:
    sql = _read_sql("20260626_mes_schedule_issue_lifecycle.sql")

    required_tokens = [
        "SELECT COUNT(*) INTO @mes_schedule_issue_status_column_count",
        "column_name = 'status'",
        "ADD COLUMN `status` varchar(32) NOT NULL DEFAULT ''OPEN''",
        "column_name = 'source_type'",
        "ADD COLUMN `source_type` varchar(64)",
        "column_name = 'source_id'",
        "ADD COLUMN `source_id` bigint",
        "column_name = 'resolution_reason'",
        "ADD COLUMN `resolution_reason` varchar(500)",
        "column_name = 'resolved_by'",
        "ADD COLUMN `resolved_by` bigint",
        "column_name = 'resolved_at'",
        "ADD COLUMN `resolved_at` datetime",
    ]

    for token in required_tokens:
        assert token in sql
