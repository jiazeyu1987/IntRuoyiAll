import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql/mysql/20260831_mes_frontline_feedback_material.sql"
OPTIONAL_BOM_PATH = REPO_ROOT / "sql/mysql/20260901_mes_frontline_feedback_material_optional_bom_quantity.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing frontline feedback material schema migration"
    return SQL_PATH.read_text(encoding="utf-8")


def compact(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).strip().lower()


def test_migration_declares_release_metadata_and_material_fact_table() -> None:
    sql = read_sql()
    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260730_mes_process_pool_foundation; type=schema; riskLevel=medium"
    )
    flat = compact(sql)
    assert "create table if not exists mes_pro_feedback_material (" in flat
    for column in [
        "feedback_id",
        "active_order_id",
        "route_version_id",
        "route_process_id",
        "material_id",
        "bom_quantity",
        "output_quantity",
        "loss_quantity",
        "loss_details_json",
        "selected_device_json",
        "device_parameter_readings_json",
        "tenant_id",
    ]:
        assert f" {column} " in flat
    assert "unique key uk_mes_feedback_material (tenant_id,feedback_id,material_id,deleted)" in flat
    assert "key idx_mes_feedback_material_order_process" in flat
    assert "key idx_mes_feedback_material_feedback" in flat
    for forbidden_column in [
        "erp_batch_code",
        "erp_receipt_no",
        "erp_batch_status",
        "erp_batch_returned_at",
    ]:
        assert forbidden_column not in flat


def test_migration_is_additive_and_preserves_existing_feedback() -> None:
    upper_sql = read_sql().upper()
    assert upper_sql.count("CREATE TABLE IF NOT EXISTS") == 1
    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM MES_PRO_FEEDBACK",
        "UPDATE MES_PRO_FEEDBACK",
        "ALTER TABLE MES_PRO_FEEDBACK",
    ]:
        assert forbidden not in upper_sql


def test_batch_record_material_source_makes_legacy_bom_quantity_optional() -> None:
    assert OPTIONAL_BOM_PATH.exists(), "missing optional BOM quantity migration"
    sql = OPTIONAL_BOM_PATH.read_text(encoding="utf-8")
    assert sql.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260831_mes_frontline_feedback_material; type=schema; riskLevel=low"
    )
    flat = compact(sql)
    assert "alter table `mes_pro_feedback_material`" in flat
    assert (
        "modify column `bom_quantity` decimal(24,6) default null "
        "comment 'legacy bom usage ratio; null for batch-record material configuration'"
    ) in flat
    for forbidden in ["drop table", "truncate table", "delete from", "update mes_pro_feedback_material"]:
        assert forbidden not in flat
