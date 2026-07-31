from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "sql" / "mysql" / "20260725_system_backup_plan_menu.sql"


def test_system_backup_plan_menu_sql_declares_simple_page_and_permissions() -> None:
    text = MIGRATION.read_text(encoding="utf-8")

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=menu; riskLevel=medium"
    )
    required_snippets = [
        "SET NAMES utf8mb4;",
        "ensure_system_backup_plan_menu",
        "Missing system management root menu",
        "Conflicting system_menu id exists for system backup plan menus",
        "备份计划",
        "保存备份计划",
        "立即备份一次",
        "system/backup-plan/index",
        "SystemBackupPlan",
        "system:backup-plan:query",
        "system:backup-plan:update",
        "system:backup-plan:execute",
        "901100",
        "901101",
        "901102",
        "system_tenant_package",
        "JSON_VALID(`package`.`menu_ids`)",
        "JSON_CONTAINS(`package`.`menu_ids`, CAST('1' AS JSON), '$')",
        "system_role_menu",
        "`role`.`code` = 'super_admin'",
        "`role`.`code` = 'tenant_admin'",
    ]

    for snippet in required_snippets:
        assert snippet in text


def test_system_backup_plan_menu_sql_does_not_reuse_old_low_menu_ids() -> None:
    text = MIGRATION.read_text(encoding="utf-8")

    for forbidden_id in ("5900", "5901", "5902", "5903"):
        assert forbidden_id not in text
