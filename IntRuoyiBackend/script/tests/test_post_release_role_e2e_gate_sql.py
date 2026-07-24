from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_post_release_role_e2e_gate.sql"
PASSWORD_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_post_release_role_e2e_gate_password_fix.sql"
SMOKE_CONTRACT_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_post_release_role_e2e_gate_smoke_contract.sql"
SMOKE_USERNAME_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_post_release_role_e2e_gate_smoke_username_fix.sql"
SMOKE_PASSWORD_FRESHNESS_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_post_release_role_e2e_gate_smoke_username_password_freshness_fix.sql"
SMOKE_ERP_JOB_PERMISSION_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_post_release_role_e2e_gate_smoke_z_erp_job_permission_fix.sql"
SMOKE_ADMIN_RESOURCE_CAPACITY_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_post_release_role_e2e_gate_smoke_admin_resource_capacity_fix.sql"
SMOKE_ROUTE_900026_LINE_FIX_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_post_release_role_e2e_gate_smoke_route_900026_line_fix.sql"


def read_sql() -> str:
    return SQL_PATH.read_text(encoding="utf-8")


def read_password_fix_sql() -> str:
    return PASSWORD_FIX_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_contract_sql() -> str:
    return SMOKE_CONTRACT_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_username_fix_sql() -> str:
    return SMOKE_USERNAME_FIX_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_password_freshness_fix_sql() -> str:
    return SMOKE_PASSWORD_FRESHNESS_FIX_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_erp_job_permission_fix_sql() -> str:
    return SMOKE_ERP_JOB_PERMISSION_FIX_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_admin_resource_capacity_fix_sql() -> str:
    return SMOKE_ADMIN_RESOURCE_CAPACITY_FIX_SQL_PATH.read_text(encoding="utf-8")


def read_smoke_route_900026_line_fix_sql() -> str:
    return SMOKE_ROUTE_900026_LINE_FIX_SQL_PATH.read_text(encoding="utf-8")


def test_post_release_role_gate_has_release_metadata():
    sql = read_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260617_mes_scheduler_role_smart_scheduling_tab,20260618_showroom_publicity_role_menu_scope,20260513_dcc_base_schema" in sql
    assert "SIGNAL SQLSTATE '45000'" in sql


def test_post_release_role_gate_prepares_required_users_and_roles():
    sql = read_sql()

    for token in [
        "'gaomin'",
        "'zhaojie'",
        "'wangsiyu'",
        "'showroom_publicity'",
        "'排产员'",
        "system_user_role",
        "SELECT gaomin_user_id, showroom_role_id",
    ]:
        assert token in sql

    assert "$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO" in sql


def test_post_release_role_gate_accepts_current_doc_control_role_baseline_for_dcc_account():
    sql = read_sql()

    assert "'doc_control'" in sql
    assert "'wenkong_download'" in sql
    assert "Missing enabled wenkong role; cannot prepare wangsiyu DCC E2E account" not in sql
    assert "Missing enabled DCC role; cannot prepare wangsiyu DCC E2E account" in sql


def test_post_release_role_gate_bootstraps_missing_scheduler_role_for_admin_tenant():
    sql = read_sql()

    assert "IF scheduler_role_id IS NULL THEN" in sql
    assert "INSERT INTO `system_role`" in sql
    assert "SET scheduler_role_id = LAST_INSERT_ID();" in sql
    assert "UPDATE `system_role`" in sql
    assert "`name` = '排产员'" in sql
    assert "'post-release scheduler E2E gate role'" in sql
    assert "Missing enabled scheduler role; cannot prepare zhaojie smart scheduling E2E account" not in sql


def test_post_release_role_gate_enforces_menu_contracts():
    sql = read_sql()

    for menu_id in ["980100", "980101", "980118", "980102", "980119", "980103", "980104"]:
        assert menu_id in sql

    for menu_id in ["900120", "5590", "5580", "5550", "5262", "5540", "900104"]:
        assert menu_id in sql

    assert "`parent_id` = 0" in sql
    assert "`path` = 'smart-scheduling'" in sql
    assert "`menu_id` NOT IN (SELECT `menu_id` FROM `tmp_post_release_scheduler_menus`)" in sql
    assert "SELECT dcc_role_id, 1221" in sql


def test_post_release_role_gate_password_fix_uses_authorized_password_hash():
    sql = read_password_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260618_post_release_role_e2e_gate" in sql
    assert "$2a$10$EzpuIftrlM8pmMAKMbPCqeGV/NOHGXMGwH8nKg3G0eNJr8Sg0hs0K" in sql
    assert "active_user_count <> 3" in sql
    for username in ["'gaomin'", "'zhaojie'", "'wangsiyu'"]:
        assert username in sql


def test_post_release_role_gate_smoke_contract_prepares_real_accounts_and_permissions():
    sql = read_smoke_contract_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260618_post_release_role_e2e_gate_password_fix" in sql
    assert "type=permission" in sql
    assert "$2a$10$EzpuIftrlM8pmMAKMbPCqeGV/NOHGXMGwH8nKg3G0eNJr8Sg0hs0K" in sql

    for token in [
        "'mes_smoke_erp_creator'",
        "'mes_smoke_supervisor'",
        "'mes_smoke_non_approver'",
        "'aoteman'",
        "'eDHR矩阵-审批人'",
        "'芋道1'",
        "'post_release_mes_smoke_erp_creator'",
        "'post_release_mes_smoke_supervisor'",
        "'post_release_mes_smoke_non_approver'",
    ]:
        assert token in sql

    for menu_id in [
        "2563",
        "6013",
        "6014",
        "1075",
        "5531",
        "5532",
        "5542",
        "5581",
        "5582",
        "5584",
        "5585",
        "5262",
        "5550",
        "5551",
        "5552",
        "5553",
        "5969",
    ]:
        assert menu_id in sql

    assert "(scheduler_role_id, 5531)" in sql
    assert "(scheduler_role_id, 5582)" in sql
    assert "(supervisor_role_id, 5969)" in sql
    assert "(non_approver_role_id, 5969)" not in sql


def test_post_release_role_gate_smoke_username_fix_uses_login_compatible_accounts():
    sql = read_smoke_username_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260618_post_release_role_e2e_gate_smoke_contract" in sql
    assert "^[a-zA-Z0-9]{4,30}$" in sql
    for username in ["'messmokeerp'", "'messmokesupervisor'", "'messmokenonapprover'"]:
        assert username in sql
    for invalid_username in ["'mes_smoke_erp_creator'", "'mes_smoke_supervisor'", "'mes_smoke_non_approver'"]:
        assert invalid_username not in sql
    assert "active_user_count <> 3" in sql
    assert "role_bind_count <> 3" in sql


def test_post_release_role_gate_smoke_password_freshness_fix_prevents_expired_login():
    sql = read_smoke_password_freshness_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260619_post_release_role_e2e_gate_smoke_username_fix" in sql
    assert "`password_update_time` = NOW()" in sql
    for username in ["'messmokeerp'", "'messmokesupervisor'", "'messmokenonapprover'"]:
        assert username in sql
    assert "fresh_password_count <> 3" in sql


def test_post_release_role_gate_smoke_erp_job_permission_fix_surfaces_manual_trigger_button():
    sql = read_smoke_erp_job_permission_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260619_post_release_role_e2e_gate_smoke_username_password_freshness_fix" in sql
    assert "type=permission" in sql

    assert "'post_release_mes_smoke_erp_creator'" in sql
    assert "`id` = 6013" in sql
    assert "`parent_id` = erp_sync_menu_id" in sql
    assert "'infra:job:query'" in sql
    assert "'infra:job:trigger'" in sql
    assert "'ERP同步任务查询'" in sql
    assert "'ERP同步任务触发'" in sql
    assert "erp_job_permission_count <> 2" in sql
    assert "erp_job_role_menu_count <> 2" in sql


def test_post_release_role_gate_smoke_admin_resource_capacity_fix_removes_a03388_conflicts():
    sql = read_smoke_admin_resource_capacity_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260619_post_release_role_e2e_gate_smoke_z_erp_job_permission_fix" in sql
    assert "type=permission" in sql
    assert "`tenant_id` = 1" in sql
    assert "`machinery_id` = 47" in sql
    assert "`process_id` = 900370" in sql
    assert "`process_id` = 900371" in sql
    assert "`standard_hourly_capacity` = 61.904762" in sql
    assert "`standard_hourly_capacity` = 80.000000" in sql
    assert "`standard_hourly_capacity` = 25.714286" in sql
    assert "`standard_hourly_capacity` = 40.000000" in sql
    assert "`deleted` = b'1'" in sql
    assert "remaining_conflict_count <> 0" in sql
    assert "apply_pr_role_e2e_smoke_admin_capacity_fix" in sql


def test_post_release_role_gate_smoke_route_900026_line_fix_binds_sting_process_workstations():
    sql = read_smoke_route_900026_line_fix_sql()

    assert "release-migration:" in sql
    assert "allowedEnvironments=test,backup" in sql
    assert "dependsOn=20260619_post_release_role_e2e_gate_smoke_admin_resource_capacity_fix" in sql
    assert "type=config" in sql
    assert "`id` IN (900113, 900114, 900115, 900116, 900117, 900118, 900119, 900120, 900121)" in sql
    assert "`production_line_id` = 900040" in sql
    assert "`workshop_id` = 900011" in sql
    assert "`calendar_plan_id` = 900030" in sql
    assert "remaining_null_binding_count <> 0" in sql
    assert "apply_pr_role_e2e_smoke_route_900026_line_fix" in sql
