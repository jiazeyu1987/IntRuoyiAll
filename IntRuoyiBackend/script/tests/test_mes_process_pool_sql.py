from pathlib import Path


SQL_PATH = (
    Path(__file__).resolve().parents[2]
    / "sql"
    / "mysql"
    / "20260730_mes_process_pool_foundation.sql"
)


def test_process_pool_sql_creates_dedicated_tables():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260729_dcc_product_catalog_remove_subsidiary_source; type=schema; riskLevel=medium\n"
    )
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_event`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_quantity_fragment`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_process_pool_pqc_record`" in sql
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_feedback_surplus_pool`" not in sql


def test_process_pool_event_sql_requires_traceable_context_and_unique_signature():
    sql = SQL_PATH.read_text(encoding="utf-8")

    required_event_columns = [
        "`work_order_id` bigint NOT NULL",
        "`route_id` bigint NOT NULL",
        "`route_process_id` bigint NOT NULL",
        "`process_id` bigint NOT NULL",
        "`actual_employee_id` bigint NOT NULL",
        "`device_account_id` bigint NOT NULL",
        "`device_id` bigint NOT NULL",
        "`workstation_id` bigint NOT NULL",
        "`template_type` varchar(64) NOT NULL",
        "`feedback_source_type` varchar(64) NOT NULL",
        "`feedback_source_id` bigint NOT NULL",
        "`recordbook_source_type` varchar(64) NOT NULL",
        "`recordbook_source_id` bigint NOT NULL",
        "`raw_payload` json NOT NULL",
        "`server_submit_time` datetime NOT NULL",
        "`signature_id` bigint NOT NULL",
        "`signature_user_id` bigint NOT NULL",
    ]
    for column in required_event_columns:
        assert column in sql
    assert "UNIQUE KEY `uk_mes_pro_process_pool_event_signature` (`tenant_id`, `signature_id`, `deleted`)" in sql
    assert "KEY `idx_mes_pro_process_pool_event_time` (`tenant_id`, `server_submit_time`)" in sql


def test_process_pool_quantity_and_pqc_records_are_linked_to_event():
    sql = SQL_PATH.read_text(encoding="utf-8")

    assert "`event_id` bigint NOT NULL COMMENT '工序池提交事件ID'" in sql
    assert "`source_quantity_type` varchar(64) NOT NULL" in sql
    assert "`total_quantity` decimal(24,6) NOT NULL" in sql
    assert "`allocated_quantity` decimal(24,6) NOT NULL DEFAULT 0" in sql
    assert "`available_quantity` decimal(24,6) NOT NULL DEFAULT 0" in sql
    assert "`allocation_status` varchar(32) NOT NULL DEFAULT 'AVAILABLE'" in sql
    assert "`inspection_result` varchar(32) NOT NULL" in sql
    assert "KEY `idx_mes_pro_process_pool_fragment_event` (`tenant_id`, `event_id`)" in sql
    assert "KEY `idx_mes_pro_process_pool_pqc_event` (`tenant_id`, `event_id`)" in sql


if __name__ == "__main__":
    test_process_pool_sql_creates_dedicated_tables()
    test_process_pool_event_sql_requires_traceable_context_and_unique_signature()
    test_process_pool_quantity_and_pqc_records_are_linked_to_event()
    print("PASS: MES process pool SQL contract")
