from pathlib import Path
import re


BACKEND_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = BACKEND_ROOT / "sql" / "mysql" / "20260808_mes_qa_optional_equipment_items.sql"


def read_migration() -> str:
    assert MIGRATION.exists(), f"missing migration: {MIGRATION}"
    return MIGRATION.read_text(encoding="utf-8")


def test_optional_equipment_migration_is_release_managed_and_fail_fast() -> None:
    sql = read_migration()

    assert sql.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260803_mes_pqc_item_equipment_standard_snapshot; type=data; riskLevel=medium\n"
    )
    assert "SIGNAL SQLSTATE '45000'" in sql
    assert "QA optional equipment repair requires mes_qa_inspection_regulation_item" in sql
    assert "QA optional equipment repair still has inconsistent item equipment flags" in sql


def test_optional_equipment_migration_normalizes_from_formal_equipment_bindings() -> None:
    sql = read_migration()
    normalized = " ".join(sql.split())

    assert "UPDATE `mes_qa_inspection_regulation_item` `item`" in normalized
    assert "`item`.`equipment_required` =" in normalized
    assert "EXISTS ( SELECT 1 FROM `mes_qa_inspection_regulation_item_equipment` `equipment`" in normalized
    assert "`equipment`.`regulation_version_id` = `item`.`regulation_version_id`" in normalized
    assert "`equipment`.`inspection_type` = `item`.`inspection_type`" in normalized
    assert "`equipment`.`item_code` = `item`.`item_code`" in normalized
    assert "`equipment`.`deleted` = b'0'" in normalized
    assert "WHERE `item`.`deleted` = b'0'" in normalized
    assert "NOT EXISTS ( SELECT 1 FROM `mes_qa_inspection_regulation_item_equipment` `equipment`" in normalized


def test_optional_equipment_migration_does_not_guess_or_create_equipment_rows() -> None:
    sql = read_migration()

    assert re.search(
        r"\bINSERT\s+INTO\s+`?mes_qa_inspection_regulation_item_equipment`?",
        sql,
        re.IGNORECASE,
    ) is None
    assert re.search(
        r"\bDELETE\s+FROM\s+`?mes_qa_inspection_regulation_item_equipment`?",
        sql,
        re.IGNORECASE,
    ) is None
    for forbidden in (
        "COALESCE(`equipment_required`",
        "IFNULL(`equipment_required`",
        "ORDER BY `id` LIMIT 1",
        "DEFAULT b'1'",
    ):
        assert forbidden not in sql
