from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260612_erp_sync_menu_base_data.sql"


EXPECTED_UPDATES = {
    2565: ("erp-product", 20),
    2590: ("erp-stock", 21),
    2666: ("erp-purchase-order", 22),
    2638: ("erp-sale-order", 23),
    5530: ("work-order", 24),
}


def _sql() -> str:
    return MIGRATION.read_text(encoding="utf-8")


def _normalized_sql() -> str:
    return " ".join(_sql().split())


def test_erp_sync_menu_migration_targets_expected_page_menus() -> None:
    text = _normalized_sql()

    assert "UPDATE `system_menu`" in text
    assert "`parent_id` = 5101" in text
    assert "`deleted` = b'0'" in text
    assert "`type` = 2" in text

    for menu_id, (path, sort) in EXPECTED_UPDATES.items():
        assert f"`id` = {menu_id}" in text
        assert f"`path` = '{path}'" in text
        assert f"`sort` = {sort}" in text


def test_erp_sync_menu_migration_uses_unique_paths_under_base_data() -> None:
    text = _sql()
    paths = re.findall(r"`path`\s*=\s*'([^']+)'", text)

    assert len(paths) == len(EXPECTED_UPDATES)
    assert sorted(paths) == sorted(path for path, _ in EXPECTED_UPDATES.values())
    assert paths.count("order") == 0


def test_erp_sync_menu_migration_does_not_move_button_permissions_or_roles() -> None:
    text = _sql()

    assert "system_role_menu" not in text
    assert "erp:purchase-order:sync-kingdee" not in text
    assert "`type` = 3" not in text
    assert "DELETE FROM" not in text.upper()
    assert "TRUNCATE" not in text.upper()
