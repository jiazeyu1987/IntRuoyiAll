from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260829_mes_form_center_unified_import_menu.sql"
FORM_CENTER_NAME_EXPR = "CONVERT(UNHEX('E8A1A8E58D95E4B8ADE5BF83') USING utf8mb4) COLLATE utf8mb4_unicode_ci"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MES form center unified import menu SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_form_center_unified_import_menu_declares_release_metadata_and_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260804_mes_edhr_qa_menu" in text
    assert "ensure_mes_form_center_unified_import_menu" in text
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing MES batch record form list menu 900365" in text
    assert "MES batch record form list menu rename failed" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_form_center_unified_import_menu_renames_only_visible_tab_text() -> None:
    text = read_sql()
    update_start = text.index("UPDATE `system_menu`")
    update_end = text.index("IF NOT EXISTS", update_start)
    update_block = text[update_start:update_end]
    set_clause = update_block.split("WHERE", 1)[0]

    assert "`id` = 900365" in update_block
    assert "`path` = '/mes/pro/batch-record-form-list'" in update_block
    assert "`component` = 'mes/pro/batchrecordformlist/index'" in update_block
    assert "`component_name` = 'MesProBatchRecordFormList'" in update_block
    assert "`permission` = 'mes:pro-batch-record-template:query'" in update_block
    assert f"`name` = {FORM_CENTER_NAME_EXPR}" in set_clause
    assert "`updater` = 'codex'" in set_clause
    assert "`update_time` = NOW()" in set_clause
    assert "COLLATE utf8mb4_unicode_ci" in set_clause

    for forbidden_assignment in [
        "`path` =",
        "`component` =",
        "`component_name` =",
        "`permission` =",
        "`visible` =",
        "`type` =",
    ]:
        assert forbidden_assignment not in set_clause


def test_form_center_unified_import_menu_verifies_new_name_after_update() -> None:
    text = read_sql()

    assert f"`name` = {FORM_CENTER_NAME_EXPR}" in text
    assert "CALL `ensure_mes_form_center_unified_import_menu`();" in text
    assert "DROP PROCEDURE IF EXISTS `ensure_mes_form_center_unified_import_menu`;" in text
