import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260714_approval_center_workflow_menu_consolidation.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing approval center workflow menu consolidation migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_migration_declares_metadata_and_fail_fast_guards() -> None:
    text = read_sql()
    first_line = text.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260624_unified_approval_phase5_retire_legacy_menus,"
        "20260630_approval_center_role_visibility; type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_approval_center_workflow_menu_consolidation" in text
    assert "SIGNAL SQLSTATE '45000'" in text

    for required in [
        "Missing workflow root menu 1185",
        "Missing approval center menu 1200",
        "Missing workflow management menu 1186",
        "Missing OA example menu 5",
        "Missing approval center task menus",
        "Invalid system_tenant_package.menu_ids JSON",
    ]:
        assert required in text


def test_migration_reparents_visible_menu_tree_to_approval_center() -> None:
    text = read_sql()

    assert re.search(r"UPDATE `system_menu`[\s\S]*?WHERE `id` = 1200;", text)
    assert "`parent_id` = 0" in text
    assert "`path` = '/approval-center'" in text
    assert "`type` = 1" in text
    assert "`visible` = b'1'" in text

    expected_updates = {
        1186: ("流程管理", 10, 1200, "manager"),
        1207: ("待办", 20, 1200, "todo"),
        1208: ("已办", 30, 1200, "done"),
        1201: ("我发起的", 40, 1200, "my-initiated"),
        2713: ("抄送我的", 50, 1200, "cc"),
    }

    for menu_id, (name, sort, parent_id, path) in expected_updates.items():
        assert re.search(
            rf"UPDATE `system_menu`[\s\S]*?`name` = '{name}'[\s\S]*?`sort` = {sort}[\s\S]*?`parent_id` = {parent_id}[\s\S]*?`path` = '{path}'[\s\S]*?WHERE `id` = {menu_id};",
            text,
        )


def test_migration_hides_oa_example_entry_without_deleting_deep_links() -> None:
    text = read_sql()

    assert re.search(
        r"UPDATE `system_menu`[\s\S]*?`name` = 'OA 示例'[\s\S]*?`parent_id` = 1200[\s\S]*?`path` = 'oa'[\s\S]*?`visible` = b'0'[\s\S]*?`always_show` = b'0'[\s\S]*?`deleted` = b'0'[\s\S]*?WHERE `id` = 5;",
        text,
    )
    assert re.search(
        r"UPDATE `system_menu`[\s\S]*?`name` = '请假查询'[\s\S]*?`parent_id` = 5[\s\S]*?`path` = 'leave'[\s\S]*?`visible` = b'0'[\s\S]*?`deleted` = b'0'[\s\S]*?WHERE `id` = 1118;",
        text,
    )


def test_migration_hides_legacy_workflow_entry_and_retains_deep_links() -> None:
    text = read_sql()
    upper_text = text.upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM SYSTEM_MENU",
        "TRUNCATE TABLE `SYSTEM_MENU`",
        "DROP TABLE `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM SYSTEM_ROLE_MENU",
    ]:
        assert forbidden not in upper_text

    assert re.search(r"UPDATE `system_menu`[\s\S]*?`visible` = b'0'[\s\S]*?WHERE `id` = 1185;", text)
    assert "legacy /bpm route is retained by frontend hidden routes" in text
    assert "bpm:task:query" in text
    assert "bpm:task:update" in text


def test_migration_updates_tenant_package_without_readding_workflow_root() -> None:
    text = read_sql()

    assert "system_tenant_package" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_TABLE" in text
    assert "JSON_ARRAYAGG" in text
    assert "`existing_menu`.`menu_id` <> 1185" in text
    assert "tmp_approval_center_workflow_required_menu_ids" in text
    assert "tmp_approval_center_workflow_target_packages" in text

    for retained_id in (1200, 1186, 1193, 1187, 2714, 1209, 2731, 1207, 1208, 1201, 2713, 5, 1118, 1221, 1222):
        assert f"SELECT {retained_id} AS `menu_id`" in text or f"UNION ALL SELECT {retained_id}" in text
