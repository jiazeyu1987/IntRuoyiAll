from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260829_mdm_associated_company_menu.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), "missing MDM associated company menu migration"
    return SQL_PATH.read_text(encoding="utf-8")


def test_associated_company_menu_migration_declares_dependencies_and_fail_fast_guards() -> None:
    text = read_sql()
    upper_text = text.upper()

    assert "release-migration: allowedEnvironments=test,backup,prod" in text
    assert "dependsOn=20260816_mdm_enterprise_company_scope" in text
    assert "type=data" in text
    assert "riskLevel=medium" in text
    assert "SET NAMES utf8mb4;" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Invalid system_tenant_package.menu_ids JSON" in text
    assert "MDM associated company menu contract mismatch" in text

    for forbidden in [
        "DELETE FROM `SYSTEM_MENU`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_TENANT_PACKAGE`",
        "TRUNCATE TABLE",
        "DROP TABLE",
    ]:
        assert forbidden not in upper_text


def test_associated_company_menu_migration_covers_page_and_button_permissions() -> None:
    text = read_sql()

    assert "990200" in text
    assert "990249" in text
    assert "990250" in text
    assert "990251" in text
    assert "990252" in text
    assert "990220" not in text
    assert "990221" not in text
    assert "990222" not in text
    assert "990223" not in text
    assert "CONVERT(UNHEX('E585B3E88194E585ACE58FB8') USING utf8mb4)" in text
    assert "mdm:enterprise:query" in text
    assert "mdm:enterprise:create" in text
    assert "mdm:enterprise:update" in text
    assert "mdm:enterprise:delete" in text
    assert "'mdm/enterprise/index'" in text
    assert "'MdmEnterprise'" in text
    assert "Missing tenant 1 admin super_admin binding for MDM associated company menu" in text


def test_associated_company_menu_migration_merges_packages_and_admin_role_links() -> None:
    text = read_sql()

    assert "JSON_TABLE(" in text
    assert "UPDATE `system_tenant_package`" in text
    assert "UPDATE `system_role_menu`" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "`role`.`code` = 'tenant_admin'" in text
    assert "`role`.`code` = 'super_admin'" in text
    assert "`tenant`.`id` = 1 AND `role`.`code` = 'super_admin'" in text
    assert "`user`.`username` = 'admin'" in text
    assert "`tenant`.`id` = 1" in text
    assert "WHERE NOT EXISTS" in text
    assert "CALL ensure_mdm_associated_company_menu();" in text
