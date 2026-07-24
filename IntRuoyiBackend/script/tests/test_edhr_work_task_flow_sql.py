from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260611_mes_edhr_work_task_flow.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR工作任务流转 SQL 必须存在。"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_work_task_flow_declares_task_and_assignment_tables() -> None:
    text = read_sql()

    for table_name in [
        "mes_pro_edhr_work_task_assignment_rule",
        "mes_pro_edhr_work_task",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in text

    for column in [
        "`route_process_id` bigint NOT NULL",
        "`task_type` varchar(32) NOT NULL",
        "`assignee_user_id` bigint NOT NULL",
        "`review_user_id` bigint DEFAULT NULL",
        "`due_minutes` int DEFAULT NULL",
        "`task_code` varchar(64) NOT NULL",
        "`batch_execution_id` bigint NOT NULL",
        "`batch_task_id` bigint NOT NULL",
        "`execution_id` bigint DEFAULT NULL",
        "`status` varchar(32) NOT NULL",
        "`due_time` datetime DEFAULT NULL",
        "`overdue_at` datetime DEFAULT NULL",
        "`overdue_reason` varchar(500) DEFAULT NULL",
        "`action_url` varchar(500) NOT NULL",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_work_task_rule` "
        "(`tenant_id`, `route_process_id`, `task_type`, `deleted`)"
    ) in text
    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_work_task_active` "
        "(`tenant_id`, `batch_task_id`, `task_type`, `status`, `deleted`)"
    ) in text
    assert (
        "KEY `idx_mes_pro_edhr_work_task_due` "
        "(`tenant_id`, `status`, `due_time`, `deleted`)"
    ) in text
    assert "ensure_mes_edhr_work_task_flow_schema" in text
    assert "`information_schema`.`COLUMNS`" in text
    assert "ADD COLUMN `due_minutes` int DEFAULT NULL" in text
    assert "ADD COLUMN `overdue_at` datetime DEFAULT NULL" in text
    assert "ADD COLUMN `overdue_reason` varchar(500) DEFAULT NULL" in text
    assert "`information_schema`.`STATISTICS`" in text
    assert "ADD INDEX `idx_mes_pro_edhr_work_task_due`" in text


def test_edhr_work_task_flow_declares_menu_permissions_and_notifications() -> None:
    text = read_sql()

    for permission in [
        "mes:pro-edhr-work-task:query",
        "mes:pro-edhr-work-task:update",
        "mes:pro-edhr-work-task-rule:query",
        "mes:pro-edhr-work-task-rule:update",
    ]:
        assert permission in text

    for template_code in [
        "MES_EDHR_FILL_TASK_ASSIGNED",
        "MES_EDHR_REVIEW_TASK_ASSIGNED",
        "MES_EDHR_APPROVE_TASK_ASSIGNED",
        "MES_EDHR_REWORK_TASK_ASSIGNED",
        "MES_EDHR_WORK_TASK_OVERDUE",
    ]:
        assert template_code in text

    assert "system_notify_template" in text
    assert "system_tenant_package" in text
    assert "system_role_menu" in text
    assert "tenant_admin" in text
    assert "工作到你了" in text
    assert "请批准工单{workOrderCode}批次{batchCode}的{processName}批记录" in text
    assert "已逾期" in text
    assert "workTaskId" in text


def test_edhr_work_task_flow_declares_overdue_job_seed() -> None:
    text = read_sql()

    assert "infra_job" in text
    assert "mesEdhrWorkTaskOverdueJob" in text
    assert "eDHR工作任务逾期处理 Job" in text
    assert "0 0/5 * * * ?" in text


def test_edhr_work_task_flow_migration_is_fail_fast_and_no_fallback() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing eDHR work task system_menu rows" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "ON DUPLICATE KEY UPDATE" not in upper_text
    assert "INSERT IGNORE INTO `SYSTEM_NOTIFY_TEMPLATE`" not in upper_text
    assert "ADMIN" not in upper_text or "自动派给管理员" not in text
