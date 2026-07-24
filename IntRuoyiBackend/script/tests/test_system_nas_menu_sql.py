from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


def test_system_nas_management_menu_sql_declares_page_and_permissions() -> None:
    sql_path = REPO_ROOT / "sql" / "mysql" / "20260520_system_nas_management_menu.sql"
    text = sql_path.read_text(encoding="utf-8")

    required_snippets = [
        "SET NAMES utf8mb4;",
        "NAS 管理",
        "NAS 配置查询",
        "NAS 配置保存",
        "NAS 连接测试",
        "system/nas/index",
        "SystemNasManagement",
        "infra:nas:query",
        "infra:nas:update",
        "infra:nas:test",
        "parent_id`, `path`",
        "ON DUPLICATE KEY UPDATE",
        "ensure_system_nas_management_tenant_package_menus",
        "system_tenant_package",
        "JSON_VALID(`package`.`menu_ids`)",
        "JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$')",
        "system_role_menu",
        "tenant_admin",
    ]

    for snippet in required_snippets:
        assert snippet in text

    assert "WHERE `existing`.`role_id` = `role`.`id`" in text
    assert "SELECT`role`.`id`," not in text.replace(" ", "")
    assert "SELECT\n    `role`.`id`," in text

    forbidden_snippets = [
        "NAS??",
        "NAS????",
        "'NAS管理'",
        "'NAS配置查询'",
        "'NAS配置保存'",
        "'NAS连接测试'",
    ]
    for snippet in forbidden_snippets:
        assert snippet not in text
