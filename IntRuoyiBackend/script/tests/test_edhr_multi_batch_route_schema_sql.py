from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MULTI_ROUTE_SQL = REPO_ROOT / "sql" / "mysql" / "20260612_mes_edhr_multi_batch_route.sql"
BATCH_EXECUTION_SQL = REPO_ROOT / "sql" / "mysql" / "20260608_edhr_batch_execution_schema.sql"


def read(path: Path) -> str:
    assert path.exists(), f"required migration missing: {path}"
    return path.read_text(encoding="utf-8")


def test_multi_batch_route_schema_declares_ordered_report_table_and_mode() -> None:
    text = read(MULTI_ROUTE_SQL)

    assert "ALTER TABLE `mes_pro_route_use_process_config`" in text
    assert "`execution_mode` varchar(16)" in text
    assert "CREATE TABLE IF NOT EXISTS `mes_pro_route_use_process_batch_record`" in text

    for column in [
        "`route_use_process_config_id` bigint NOT NULL",
        "`route_id` bigint NOT NULL",
        "`route_process_id` bigint NOT NULL",
        "`use_type` varchar(32) NOT NULL",
        "`batch_record_report_id` varchar(64) NOT NULL",
        "`report_sort` int NOT NULL",
        "`tenant_id` bigint NOT NULL DEFAULT 0",
    ]:
        assert column in text

    assert (
        "UNIQUE KEY `uk_mes_pro_route_use_process_report_sort` "
        "(`tenant_id`, `route_process_id`, `use_type`, `report_sort`, `deleted`)"
    ) in text
    assert (
        "UNIQUE KEY `uk_mes_pro_route_use_process_report` "
        "(`tenant_id`, `route_process_id`, `use_type`, `batch_record_report_id`, `deleted`)"
    ) in text


def test_multi_batch_route_schema_migrates_legacy_single_bindings_fail_fast() -> None:
    text = read(MULTI_ROUTE_SQL)

    assert "report_sort" in text
    assert "mes_pro_route_use_process_config" in text
    assert "mes_pro_route_process" in text
    assert "batch_record_report_id" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing batch record report referenced by eDHR batch route configuration" in text
    assert "ON DUPLICATE KEY UPDATE" not in text.upper()
    assert "INSERT IGNORE" not in text.upper()


def test_edhr_batch_execution_task_schema_allows_multiple_reports_per_process() -> None:
    text = read(BATCH_EXECUTION_SQL)

    assert "`batch_record_sort` int NOT NULL" in text
    assert "`execution_mode` varchar(16) NOT NULL" in text
    assert (
        "UNIQUE KEY `uk_mes_pro_edhr_batch_task_process_report` "
        "(`tenant_id`, `batch_execution_id`, `route_process_id`, `batch_record_sort`, `deleted`)"
    ) in text
    assert "uk_mes_pro_edhr_batch_task_process` (`tenant_id`, `batch_execution_id`, `route_process_id`, `deleted`)" not in text
