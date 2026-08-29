from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = ROOT / "sql" / "mysql" / "20260829_dcc_registration_certificate_upload_approver_role.sql"


def read_sql() -> str:
    assert SQL_PATH.exists(), f"Missing SQL script: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def test_upload_approver_role_sql_declares_release_contract_and_fail_fast_guards() -> None:
    text = read_sql()

    assert text.splitlines()[0] == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260816_dcc_registration_certificate_menu,20260707_system_role_category_management; "
        "type=permission; riskLevel=medium"
    )
    assert "SET NAMES utf8mb4;" in text
    assert "START TRANSACTION;" in text
    assert "ensure_dcc_reg_cert_upload_role_20260829" in text
    assert "SIGNAL SQLSTATE '45000'" in text
    assert "Missing enabled upload approval permission menu for registration certificate upload approver role" in text
    assert "Missing registration department approval-center user for registration certificate upload approver role" in text
    assert "Duplicate registration certificate upload approver role code in target tenant" in text
    assert "Registration certificate upload approver role permission grant incomplete" in text
    assert "Registration certificate upload approver user binding incomplete" in text


def test_upload_approver_role_sql_creates_exact_registration_manager_role() -> None:
    text = read_sql()

    assert "dcc_registration_certificate_approver" in text
    assert "注册部经理" in text
    assert "INSERT INTO `system_role`" in text
    assert "UPDATE `system_role` AS `role`" in text
    assert "`category`.`code` = 'dcc'" in text
    assert "注册证上传审批角色，权限由注册证上传审批迁移精确维护" in text


def test_upload_approver_role_sql_grants_upload_approval_and_approval_center_permissions() -> None:
    text = read_sql()

    assert "tmp_dcc_reg_cert_upload_approver_required_permission" in text
    assert "dcc:registration-certificate:upload:approve" in text
    assert "bpm:task:query" in text
    assert "bpm:process-instance:query" in text
    assert "INSERT INTO `system_role_menu`" in text
    assert "FROM `system_role_menu` AS `existing_role_menu`" in text


def test_upload_approver_role_sql_selects_initial_members_from_registration_department() -> None:
    text = read_sql()

    assert "tmp_dcc_reg_cert_upload_approver_initial_user" in text
    assert "`approval_role`.`code` = 'approval_center_entry'" in text
    assert "`dept`.`name` COLLATE utf8mb4_unicode_ci = '注册部'" in text
    assert "`user`.`username` <> 'admin'" in text
    assert "INSERT INTO `system_user_role`" in text
    assert "UPDATE `system_user_role` AS `user_role`" in text
    assert "`user_role`.`creator` = 'dcc-reg-cert-upload-approver-role'" in text
    assert "Registration certificate upload approver stale migration user binding cleanup failed" in text
    assert "'lipeiwen'" not in text
    assert "'xujianhai'" not in text


def test_upload_approver_role_sql_is_non_destructive_and_avoids_dynamic_identity_shortcuts() -> None:
    upper = read_sql().upper()

    for forbidden in [
        "DELETE FROM `SYSTEM_ROLE`",
        "DELETE FROM `SYSTEM_ROLE_MENU`",
        "DELETE FROM `SYSTEM_USER_ROLE`",
        "TRUNCATE TABLE",
        "MAX(`EXISTING_ROLE`.`ID`)",
        "MAX(ID)",
        "LAST_INSERT_ID()",
    ]:
        assert forbidden not in upper
