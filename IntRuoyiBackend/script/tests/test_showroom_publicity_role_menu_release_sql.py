from __future__ import annotations

import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_showroom_publicity_role_menu_scope.sql"


SHOWROOM_MENU_IDS = ("980100", "980101", "980118", "980102", "980119", "980103", "980104")
DCC_ROOT_MENU_ID = "6800"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing required SQL: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def normalize(sql: str) -> str:
    return re.sub(r"\s+", " ", sql).lower()


def test_sql_is_release_scanned_migration_with_explicit_risk_contract() -> None:
    sql = read_sql()

    assert sql.startswith("-- release-migration:")
    assert "allowedEnvironments=test,backup,prod" in sql
    assert "type=menu" in sql
    assert "riskLevel=medium" in sql
    assert "20260618_showroom_publicity_role_menu_scope" in SQL_PATH.name


def test_sql_fails_fast_when_role_or_required_showroom_menus_are_missing() -> None:
    sql = read_sql()

    required_snippets = [
        "Missing prerequisite role showroom_publicity",
        "Missing prerequisite showroom publicity menu ids",
        "SIGNAL SQLSTATE '45000'",
        "980100, 980101, 980118, 980102, 980119, 980103, 980104",
    ]
    for snippet in required_snippets:
        assert snippet in sql


def test_sql_removes_dcc_bindings_only_for_showroom_publicity_role() -> None:
    sql = read_sql()
    compact = normalize(sql)

    assert "update `system_role_menu` as `role_menu`" in compact
    assert "join `system_role` as `role`" in compact
    assert "`role`.`code` = 'showroom_publicity'" in compact
    assert "`role_menu`.`deleted` = b'1'" in compact
    assert "`role_menu`.`menu_id` not in (980100, 980101, 980118, 980102, 980119, 980103, 980104)" in compact
    assert "dcc menu bindings are removed because they are outside the approved showroom tab set" in compact
    assert "delete from `system_role_menu`" not in compact
    assert "truncate table" not in compact
    assert "set `role_menu`.`deleted` = b'0'" in compact


def test_sql_keeps_only_approved_showroom_tab_bindings_for_role() -> None:
    sql = read_sql()
    compact = normalize(sql)

    assert "insert into `system_role_menu`" in compact
    assert "from `system_role` as `role`" in compact
    assert "`role`.`code` = 'showroom_publicity'" in compact
    assert "join `system_menu` as `menu`" in compact
    assert "`menu`.`id` in (980100, 980101, 980118, 980102, 980119, 980103, 980104)" in compact

    for menu_id in SHOWROOM_MENU_IDS:
        assert menu_id in sql
    assert DCC_ROOT_MENU_ID not in SHOWROOM_MENU_IDS
