from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260719_business_approval_policy_menu.sql"

MENU_IDS = [605071300, 605071301, 605071302, 605071303, 605071304]


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing business approval policy menu migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_menu_migration_declares_metadata_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.startswith(
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260719_business_approval_policy; type=menu; riskLevel=low"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "ensure_business_approval_policy_menu" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing workflow management parent menu 1186" in text
    assert "Conflicting business approval policy system_menu id or permission exists" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "Missing or duplicated business approval policy menu rows" in text

    upper_sql = text.upper()
    assert "TRUNCATE TABLE" not in upper_sql
    assert "DELETE FROM" not in upper_sql
    assert "DROP TABLE `SYSTEM_MENU`" not in upper_sql
    assert "DROP TABLE SYSTEM_MENU" not in upper_sql


def test_menu_migration_creates_platform_policy_menu_and_permissions() -> None:
    text = read_sql()

    for menu_id in MENU_IDS:
        assert str(menu_id) in text, f"missing menu id {menu_id}"

    assert "'业务审批策略'" in text
    assert "'bpm/businessApprovalPolicy/index'" in text
    assert "'BpmBusinessApprovalPolicy'" in text
    assert ", 1186, 'business-approval-policy'," in text

    for permission in [
        "bpm:business-approval-policy:query",
        "bpm:business-approval-policy:create",
        "bpm:business-approval-policy:publish",
        "bpm:business-approval-policy:disable",
    ]:
        assert f"'{permission}'" in text, f"missing permission {permission}"

    assert "bpm:business-approval-policy:update" not in text
    assert "bpm:business-approval-policy:delete" not in text


def test_menu_migration_merges_tenant_packages_and_roles() -> None:
    text = read_sql()

    assert "`system_tenant_package`" in text
    assert "JSON_VALID(`package`.`menu_ids`)" in text
    assert "JSON_TABLE(" in text
    assert "JSON_ARRAYAGG" in text
    assert "JSON_CONTAINS(`package`.`menu_ids`, CAST('1186' AS JSON), '$')" in text
    assert "tmp_business_approval_policy_menu_ids" in text

    assert "INSERT INTO `system_role_menu`" in text
    assert "CROSS JOIN `tmp_business_approval_policy_menu_ids`" in text
    assert "`role`.`code` = 'super_admin'" in text
    assert "`role`.`code` = 'tenant_admin'" in text
    assert "`role`.`code` = 'bpm_admin'" in text
    assert "NOT EXISTS (" in text
