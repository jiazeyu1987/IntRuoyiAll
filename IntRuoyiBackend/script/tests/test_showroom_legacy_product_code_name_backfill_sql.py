import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
BACKFILL_SQL = ROOT / "sql/mysql/20260705_showroom_legacy_product_code_name_backfill.sql"
MANUAL_TEMPLATE_SQL = ROOT / "doc/tasks/20260705-showroom-legacy-product-code-mapping/20260705_showroom_legacy_product_code_manual_decision_template.sql"


def test_showroom_legacy_product_code_name_backfill_has_safe_transaction_contract():
    sql = BACKFILL_SQL.read_text(encoding="utf-8")
    normalized = " ".join(sql.lower().split())

    assert normalized.startswith("-- release-migration:")
    assert "-- 20260705 showroom legacy product code name backfill" in normalized
    assert "start transaction;" in normalized
    assert normalized.endswith("commit;")
    assert "delete from" not in normalized
    assert "drop table" not in normalized
    assert "truncate table" not in normalized
    assert "alter table" not in normalized


def test_showroom_legacy_product_code_name_backfill_resets_only_legacy_product_codes():
    sql = BACKFILL_SQL.read_text(encoding="utf-8")
    reset_statement = (
        "UPDATE showroom_product SET legacy_product_code = NULL "
        "WHERE legacy_product_code LIKE 'product\\_%';"
    )

    assert reset_statement in sql
    assert sql.count(reset_statement) == 1


def test_showroom_legacy_product_code_name_backfill_updates_current_int_products_by_tenant():
    sql = BACKFILL_SQL.read_text(encoding="utf-8")
    update_pattern = re.compile(
        r"UPDATE showroom_product SET legacy_product_code = 'product_\d+' "
        r"WHERE tenant_id = (1|122) AND product_code = 'INT-\d+' AND deleted = 0;"
    )
    update_lines = [
        line.strip()
        for line in sql.splitlines()
        if line.strip().startswith("UPDATE showroom_product SET legacy_product_code = 'product_")
    ]

    assert update_lines
    assert all(update_pattern.fullmatch(line) for line in update_lines)
    assert any("tenant_id = 1" in line for line in update_lines)
    assert any("tenant_id = 122" in line for line in update_lines)


def test_showroom_legacy_product_code_name_backfill_has_only_sql_or_comment_lines():
    sql = BACKFILL_SQL.read_text(encoding="utf-8")
    allowed_prefixes = (
        "--",
        "START TRANSACTION;",
        "COMMIT;",
        "UPDATE showroom_product SET legacy_product_code",
    )
    executable_lines = [
        (line_number, line.strip())
        for line_number, line in enumerate(sql.splitlines(), start=1)
        if line.strip() and not line.strip().startswith(allowed_prefixes)
    ]

    assert executable_lines == []


def test_showroom_legacy_product_code_manual_template_is_comment_only():
    sql = MANUAL_TEMPLATE_SQL.read_text(encoding="utf-8")
    executable_lines = [
        line.strip()
        for line in sql.splitlines()
        if line.strip() and not line.strip().startswith("--")
    ]

    assert not executable_lines
    assert "Do not execute directly" in sql
    assert "Strict rule" in sql
    assert "-- REVIEW ONLY: UPDATE showroom_product" in sql
