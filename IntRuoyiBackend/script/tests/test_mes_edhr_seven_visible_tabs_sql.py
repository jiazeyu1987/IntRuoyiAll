import re
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260702_mes_edhr_seven_visible_tabs.sql"


EDHR_VISIBLE_TABS = (
    (900002, "模板与配置", "mes:pro-batch-record-template:query", "/mes/pro/batch-record-template"),
    (900365, "批记录表单", "mes:pro-batch-record-template:query", "/mes/pro/batch-record-form-list"),
    (900033, "批次执行", "mes:pro-edhr-batch-execution:query", "/mes/pro/feedback/edhr-batch-execution"),
    (900025, "审计与追溯", "mes:pro-batch-record-execution:track", "/mes/pro/feedback/edhr-tracking"),
    (900235, "变更与异常", "mes:pro-edhr-change:query", "/mes/pro/feedback/edhr-change"),
    (900260, "放行与归档", "mes:pro-edhr-release:query", "/mes/pro/feedback/edhr-release"),
)


HIDDEN_BUT_RETAINED = (
    900024,  # eDHR审批，融合到审批中心，详情路由隐藏保留
    900026,  # eDHR签名记录，融合到电子签名一级页签
    900230,  # eDHR工作任务
    900241,  # eDHR操作审计
    900243,  # eDHR对象权限
    900266,  # eDHR流转单
    900272,  # eDHR独立表单
    900280,  # eDHR报表目录
    900283,  # eDHR交付驾驶舱
    900286,  # eDHR验证包矩阵
    900290,  # eDHR DHR模板
    900293,  # eDHR统一变更
    900301,  # eDHR记录本
    900315,  # eDHR部署授权接口
    900332,  # eDHR OQ/PQ
    900338,  # eDHR打印策略
    900356,  # eDHR流程干预管理
)


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing eDHR visible tabs SQL migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_edhr_visible_tabs_sql_declares_release_metadata_and_guards() -> None:
    text = read_sql()
    first_line = text.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260624_unified_approval_phase5_retire_legacy_menus,"
        "20260624_unified_electronic_signature_menu,"
        "20260630_approval_center_role_visibility; type=menu; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_mes_edhr_seven_visible_tabs" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing eDHR batch processing parent menu 900220" in text
    assert "Missing approval center menu 1200" in text
    assert "Missing unified electronic signature batch-signature menu 900412" in text
    assert "SIGNAL SQLSTATE '45000'" in text


def test_edhr_batch_processing_declares_exactly_six_visible_tabs() -> None:
    text = read_sql()

    assert "CREATE TEMPORARY TABLE `tmp_mes_edhr_visible_tabs`" in text
    assert "CREATE TEMPORARY TABLE `tmp_mes_edhr_hidden_retained_tabs`" in text
    assert "SELECT COUNT(*) FROM `tmp_mes_edhr_visible_tabs`" in text
    assert "<> 6" in text
    assert "eDHR batch processing visible tab contract must declare exactly six tabs" in text

    for menu_id, name, permission, path in EDHR_VISIBLE_TABS:
        assert f"SELECT {menu_id} AS `id`, '{name}' AS `name`" in text
        assert f"'{permission}'" in text
        assert f"'{path}'" in text
        assert re.search(
            rf"UPDATE `system_menu` AS `menu`[\s\S]*?JOIN `tmp_mes_edhr_visible_tabs` AS `visible_tab`[\s\S]*?`parent_id` = 900220[\s\S]*?`visible` = b'1'[\s\S]*?WHERE `menu`\.`id` = `visible_tab`\.`id`",
            text,
        )

    assert "审批模块" not in text
    assert "电子签名模块" not in text
    assert "900420" not in text


def test_extra_edhr_tabs_are_hidden_but_not_deleted() -> None:
    text = read_sql()
    upper_text = text.upper()

    for forbidden in (
        "DELETE FROM `SYSTEM_MENU`",
        "TRUNCATE TABLE `SYSTEM_MENU`",
        "DROP TABLE `SYSTEM_MENU`",
    ):
        assert forbidden not in upper_text

    for menu_id in HIDDEN_BUT_RETAINED:
        assert f"SELECT {menu_id} AS `id`" in text

    assert "`deleted` = b'0'" in text
    assert "多余 eDHR 功能保留但隐藏" in text
    assert re.search(
        r"UPDATE `system_menu` AS `menu`[\s\S]*?JOIN `tmp_mes_edhr_hidden_retained_tabs` AS `hidden_tab`[\s\S]*?`visible` = b'0'",
        text,
    )


def test_approval_and_signature_are_fused_into_existing_top_level_modules() -> None:
    text = read_sql()

    assert "eDHR审批统一到审批中心一级页签，详情路由保留但菜单隐藏" in text
    assert "WHERE `id` = 900024" in text
    assert "`parent_id` = 1200" in text
    assert "'mes/pro/edhr/ApprovalPage'" in text
    assert "`visible` = b'0'" in text

    assert "eDHR签名记录统一到电子签名一级页签下的批记录签名记录，原功能保留但隐藏" in text
    assert "WHERE `id` = 900026" in text
    assert "`parent_id` = 900412" in text
    assert "`type` = 3" in text
    assert "'/signature-governance/batch-signatures'" not in text
    assert "'signature-governance/index'" not in text
    assert "'SignatureGovernanceBatchSignatures'" not in text


def test_package_and_role_bindings_only_include_edhr_visible_tabs() -> None:
    text = read_sql()

    assert "tmp_mes_edhr_target_packages" in text
    assert "JSON_CONTAINS(CAST(`package`.`menu_ids` AS JSON), CAST('900220' AS JSON), '$')" in text
    assert "JSON_ARRAY_APPEND" in text

    for menu_id, *_ in EDHR_VISIBLE_TABS:
        assert f"CAST('{menu_id}' AS JSON)" in text
        assert f"$', {menu_id})" in text

    for non_edhr_menu_id in (900024, 900026, 900420):
        assert f"CAST('{non_edhr_menu_id}' AS JSON)" not in text
        assert f"$', {non_edhr_menu_id})" not in text

    assert "INSERT INTO `system_role_menu`" in text
    assert "`role_menu`.`menu_id` = 900220" in text
    assert "`visible_tab`.`id`" in text
