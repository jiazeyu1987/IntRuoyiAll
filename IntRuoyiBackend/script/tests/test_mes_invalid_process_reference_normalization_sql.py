from pathlib import Path


SQL_PATH = Path("sql/mysql/20260717_mes_invalid_process_reference_normalization.sql")


def read_sql() -> str:
    assert SQL_PATH.exists(), "invalid process reference normalization migration must exist"
    return SQL_PATH.read_text(encoding="utf-8")


def test_invalid_process_reference_normalization_has_release_contract_and_backup() -> None:
    sql = read_sql()

    for token in [
        "release-migration:",
        "allowedEnvironments=test,backup,prod",
        "dependsOn=20260717_mes_balloon_excel_device_workstation_binding",
        "type=data",
        "riskLevel=high",
        "CREATE TABLE IF NOT EXISTS `mes_invalid_process_reference_normalization_20260717`",
        "`table_name`",
        "`column_name`",
        "`old_process_id`",
        "`new_process_id`",
        "`mapping_rule`",
    ]:
        assert token in sql


def test_invalid_process_reference_normalization_covers_all_direct_process_columns() -> None:
    sql = read_sql()

    for token in [
        "mes_dv_machinery_process`.`process_id",
        "mes_md_workstation`.`process_id",
        "mes_pro_edhr_batch_execution_task`.`process_id",
        "mes_pro_edhr_work_task`.`process_id",
        "mes_pro_feedback`.`process_id",
        "mes_pro_feedback_surplus_pool`.`process_id",
        "mes_pro_route_process`.`process_id",
        "mes_pro_route_process`.`next_process_id",
        "mes_pro_route_product_bom`.`process_id",
        "mes_pro_schedule_issue`.`process_id",
        "mes_pro_schedule_order_process`.`process_id",
        "mes_pro_task`.`process_id",
        "mes_pro_task_dependency`.`source_process_id",
        "mes_pro_task_dependency`.`target_process_id",
        "mes_wm_item_consume`.`process_id",
        "mes_wm_product_produce`.`process_id",
        "mes_qc_ipqc`.`process_id",
        "mes_pro_schedule_order_daily_compare`.`process_id",
        "mes_pro_edhr_traveler_instance`.`process_id",
        "mes_pro_edhr_traveler_template`.`applicable_process_id",
    ]:
        assert token in sql


def test_invalid_process_reference_normalization_sets_unmapped_invalid_references_to_zero() -> None:
    sql = read_sql()

    assert "valid_process.`id` IS NULL" in sql
    assert "COALESCE(" in sql
    assert ", 0) AS `new_process_id`" in sql
    assert "SET target.`process_id` = candidate.`new_process_id`" in sql
    assert "SET target.`source_process_id` = candidate.`new_process_id`" in sql
    assert "SET target.`target_process_id` = candidate.`new_process_id`" in sql
    assert "SET target.`next_process_id` = candidate.`new_process_id`" in sql
    assert "SET target.`applicable_process_id` = candidate.`new_process_id`" in sql


def test_invalid_process_reference_normalization_is_non_destructive_and_history_aware() -> None:
    sql = read_sql()
    executable = "\n".join(line for line in sql.splitlines() if not line.strip().startswith("--")).upper()

    for forbidden in [
        "DELETE FROM",
        "TRUNCATE",
        "DROP TABLE `MES_",
        "SIGNAL SQLSTATE",
        "TARGET.`DELETED` = B'0'",
    ]:
        assert forbidden not in executable
