from pathlib import Path
import re


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = (
    BACKEND_ROOT
    / "sql"
    / "mysql"
    / "20260809_mes_qa_inspection_item_display_fields.sql"
)


def read_migration() -> str:
    assert MIGRATION.exists(), f"missing migration: {MIGRATION}"
    return MIGRATION.read_text(encoding="utf-8")


def test_display_fields_migration_is_release_managed_and_additive() -> None:
    sql = read_migration()
    ddl_text = sql.replace("''", "'")

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260803_mes_pqc_item_equipment_standard_snapshot; "
        "type=schema; riskLevel=low\n"
    )
    assert (
        "`inspection_tool` varchar(512) DEFAULT NULL "
        "COMMENT '检验器具及设备原文'"
    ) in ddl_text
    assert (
        "`sampling_plan_text` varchar(512) DEFAULT NULL "
        "COMMENT '抽样方案原文'"
    ) in ddl_text
    assert "information_schema.COLUMNS" in sql


def test_display_fields_migration_does_not_guess_historical_text() -> None:
    sql = read_migration()

    assert "Existing published rows are intentionally not guessed" in sql
    for statement in ("UPDATE", "INSERT", "DELETE", "REPLACE"):
        assert re.search(rf"\b{statement}\b", sql, re.IGNORECASE) is None
    for fallback in ("COALESCE", "IFNULL", "CONCAT", "DEFAULT ''"):
        assert fallback not in sql.upper()
