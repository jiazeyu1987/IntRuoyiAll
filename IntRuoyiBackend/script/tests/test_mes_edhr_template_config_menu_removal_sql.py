from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260715_mes_edhr_template_config_menu_removal.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "eDHR template config menu removal SQL must exist."
    return SQL_PATH.read_text(encoding="utf-8")


def test_template_config_menu_removal_is_fail_fast_and_non_destructive() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260714_mes_edhr_form_trace_menu,20260713_mes_edhr_form_fill_log_menu" in text
    assert "ensure_mes_edhr_template_config_menu_removed" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Menu 900002 is not the legacy eDHR template config page; refusing to mutate" in text
    assert "Retained eDHR visible menu order is incomplete after template config removal" in text
    assert "Legacy eDHR template config page route still visible after removal" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_template_config_menu_row_becomes_hidden_button_permission() -> None:
    text = read_sql()

    for required in [
        "WHERE `id` = 900002",
        "`permission` = 'mes:pro-batch-record-template:query'",
        "`name` = '模板与配置'",
        "`type` = 3",
        "`path` = ''",
        "`icon` = ''",
        "`component` = ''",
        "`component_name` = ''",
        "`visible` = b'0'",
        "`name` = 'eDHR批记录'",
    ]:
        assert required in text


def test_template_config_removal_reorders_retained_visible_children() -> None:
    text = read_sql()

    expected_children = [
        (900365, "批记录表单", "mes:pro-batch-record-template:query", 0, "/mes/pro/batch-record-form-list", "mes/pro/batchrecordformlist/index", "MesProBatchRecordFormList"),
        (900033, "批次执行", "mes:pro-edhr-batch-execution:query", 1, "/mes/pro/feedback/edhr-batch-execution", "mes/pro/edhr-batch/BatchExecutionListPage", "MesProEdhrBatchExecutionListPage"),
        (900025, "表单追溯", "mes:pro-batch-record-execution:track", 2, "/mes/pro/feedback/edhr-form-trace", "mes/pro/edhr/FormTracePage", "MesProFeedbackEdhrFormTrace"),
        (900235, "变更与异常", "mes:pro-edhr-change:query", 3, "/mes/pro/feedback/edhr-change", "mes/pro/edhr/RecordChangePage", "MesProFeedbackEdhrRecordChange"),
        (900260, "放行与归档", "mes:pro-edhr-release:query", 4, "/mes/pro/feedback/edhr-release", "mes/pro/edhr-release/ReleasePage", "MesProEdhrReleasePage"),
        (900432, "表单日志", "mes:pro-edhr-form-fill-log:query", 5, "/mes/pro/feedback/edhr-form-fill-log", "mes/pro/edhr/FormFillLogPage", "MesProEdhrFormFillLogPage"),
    ]

    for menu_id, name, permission, sort, path, component, component_name in expected_children:
        assert f"WHEN {menu_id} THEN '{name}'" in text
        assert f"WHEN {menu_id} THEN '{permission}'" in text
        assert f"WHEN {menu_id} THEN {sort}" in text
        assert f"WHEN {menu_id} THEN '{path}'" in text
        assert f"WHEN {menu_id} THEN '{component}'" in text
        assert f"WHEN {menu_id} THEN '{component_name}'" in text

    assert "WHERE `id` IN (900365, 900033, 900025, 900235, 900260, 900432)" in text
