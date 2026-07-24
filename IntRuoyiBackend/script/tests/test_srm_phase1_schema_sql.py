from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260618_srm_d7_1_code_rule_baseline.sql"
ACCESS_PROFILE_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260620_srm_phase1_supplier_access_profile.sql"
PORTAL_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260621_srm_phase1_supplier_portal.sql"
CODE_RULE_DO_PATH = (
    REPO_ROOT
    / "yudao-module-srm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "srm"
    / "dal"
    / "dataobject"
    / "coderule"
    / "SrmCodeRuleDO.java"
)
CODE_RULE_MAPPER_PATH = (
    REPO_ROOT
    / "yudao-module-srm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "srm"
    / "dal"
    / "mysql"
    / "coderule"
    / "SrmCodeRuleMapper.java"
)
CODE_RULE_COUNTER_MAPPER_PATH = (
    REPO_ROOT
    / "yudao-module-srm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "srm"
    / "dal"
    / "mysql"
    / "coderule"
    / "SrmCodeRuleCounterMapper.java"
)
CODE_RULE_SERVICE_PATH = (
    REPO_ROOT
    / "yudao-module-srm"
    / "src"
    / "main"
    / "java"
    / "cn"
    / "iocoder"
    / "yudao"
    / "module"
    / "srm"
    / "service"
    / "coderule"
    / "SrmCodeRuleServiceImpl.java"
)


def read_sql() -> str:
    assert SQL_PATH.exists(), f"missing required D7-1 SRM schema/menu SQL: {SQL_PATH}"
    return SQL_PATH.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return " ".join(read_sql().split())


def read_access_profile_sql() -> str:
    assert ACCESS_PROFILE_SQL_PATH.exists(), f"missing Phase 1 access profile SQL: {ACCESS_PROFILE_SQL_PATH}"
    return ACCESS_PROFILE_SQL_PATH.read_text(encoding="utf-8")


def read_portal_sql() -> str:
    assert PORTAL_SQL_PATH.exists(), f"missing Phase 1 portal SQL: {PORTAL_SQL_PATH}"
    return PORTAL_SQL_PATH.read_text(encoding="utf-8")


def test_srm_d7_1_release_migration_metadata_uses_manifest_contract() -> None:
    first_line = read_sql().splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=; type=schema; riskLevel=medium"
    )
    assert "type=schema-menu" not in first_line


def test_srm_code_rule_tables_and_columns_are_declared() -> None:
    sql = read_sql()

    for table_name in ["srm_code_rule", "srm_code_rule_counter"]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for column in [
        "`tenant_id` bigint NOT NULL",
        "`rule_code` varchar(64) NOT NULL",
        "`target_form` varchar(64) NOT NULL",
        "`prefix` varchar(32) NOT NULL",
        "`date_pattern` varchar(32) DEFAULT NULL",
        "`date_segment_enabled` bit(1) NOT NULL",
        "`serial_width` int NOT NULL",
        "`step` int NOT NULL",
        "`min_serial` bigint NOT NULL",
        "`max_serial` bigint NOT NULL",
        "`separator` varchar(8) DEFAULT NULL",
        "`enabled` bit(1) NOT NULL",
        "`remark` varchar(500) DEFAULT NULL",
        "`creator` varchar(64) DEFAULT",
        "`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "`updater` varchar(64) DEFAULT",
        "`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP",
        "`deleted` bit(1) NOT NULL DEFAULT b'0'",
    ]:
        assert column in sql

    for column in [
        "`rule_id` bigint NOT NULL",
        "`period_key` varchar(32) NOT NULL",
        "`current_serial` bigint NOT NULL",
        "`last_code` varchar(128) DEFAULT NULL",
        "`last_generated_at` datetime DEFAULT NULL",
        "`version` int NOT NULL DEFAULT 0",
    ]:
        assert column in sql


def test_srm_code_rule_unique_keys_and_indexes_are_tenant_scoped() -> None:
    sql = normalized_sql()

    assert "UNIQUE KEY `uk_srm_code_rule_tenant_target_form` (`tenant_id`, `target_form`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_srm_code_rule_tenant_rule_code` (`tenant_id`, `rule_code`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_srm_code_rule_counter_tenant_rule_period` (`tenant_id`, `rule_id`, `period_key`, `deleted`)" in sql
    assert "UNIQUE KEY `uk_srm_code_rule_counter_tenant_last_code` (`tenant_id`, `last_code`, `deleted`)" in sql
    assert "KEY `idx_srm_code_rule_tenant_enabled` (`tenant_id`, `enabled`)" in sql
    assert "KEY `idx_srm_code_rule_counter_tenant_target_period` (`tenant_id`, `target_form`, `period_key`)" in sql


def test_srm_code_rule_menu_permissions_and_runtime_component_are_declared() -> None:
    sql = read_sql()

    for snippet in [
        "SRM",
        "`path` = '/srm'",
        "基础配置",
        "编码规则",
        "`path` = 'code-rule'",
        "`component` = 'srm/code-rule/index'",
        "`component_name` = 'SrmCodeRule'",
        "'srm:code-rule:query'",
        "'srm:code-rule:create'",
        "'srm:code-rule:update'",
        "'srm:code-rule:enable'",
        "`system_menu`",
        "`system_role_menu`",
        "`system_tenant_package`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert snippet in sql

    assert re.search(
        r"INSERT\s+INTO\s+`system_role_menu`[\s\S]+NOT\s+EXISTS",
        sql,
        re.IGNORECASE,
    ), "menu SQL must grant roles idempotently without duplicate role-menu rows"


def test_srm_d7_1_sql_normalizes_legacy_srm_root_menu_before_clean_range_guard() -> None:
    sql = read_sql()

    assert (
        "UPDATE `system_menu`\n"
        "  SET `name` = 'SRM',\n"
        "      `updater` = 'srm-d7-1',\n"
        "      `update_time` = NOW()\n"
        "  WHERE `deleted` = b'0'\n"
        "    AND `id` = 991000\n"
        "    AND `path` = '/srm'\n"
        "    AND `name` = '供应商关系管理';"
    ) in sql

    update_pos = sql.index("UPDATE `system_menu`")
    guard_pos = sql.index("Missing SRM clean menu id range; conflicting system_menu rows exist")
    assert update_pos < guard_pos


def test_srm_d7_1_sql_is_fail_fast_and_non_destructive() -> None:
    sql = read_sql()
    upper_sql = sql.upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "DELETE FROM `SRM_",
        "DELETE FROM SRM_",
        "ON DUPLICATE KEY UPDATE",
        "INSERT IGNORE",
    ]:
        assert forbidden not in upper_sql

    assert "Missing SRM" in sql or "missing SRM" in sql
    assert "Invalid system_tenant_package.menu_ids JSON" in sql
    assert "get-permission-info" in sql


def test_srm_tenant_package_menu_merge_does_not_self_reference_insert_target() -> None:
    sql = read_sql()

    for statement in sql.split(";"):
        normalized = " ".join(statement.split())
        if "INSERT INTO `tmp_srm_d7_1_package_menu_ids`" in normalized:
            assert "FROM `tmp_srm_d7_1_package_menu_ids`" not in normalized


def test_srm_code_rule_do_quotes_mysql_separator_column() -> None:
    assert CODE_RULE_DO_PATH.exists(), f"missing SRM code-rule DO: {CODE_RULE_DO_PATH}"
    source = CODE_RULE_DO_PATH.read_text(encoding="utf-8")

    assert '@TableField("`separator`")' in source


def test_srm_code_generation_uses_transactional_for_update_locks() -> None:
    for path in [CODE_RULE_MAPPER_PATH, CODE_RULE_COUNTER_MAPPER_PATH, CODE_RULE_SERVICE_PATH]:
        assert path.exists(), f"missing SRM source file: {path}"

    code_rule_mapper = CODE_RULE_MAPPER_PATH.read_text(encoding="utf-8")
    counter_mapper = CODE_RULE_COUNTER_MAPPER_PATH.read_text(encoding="utf-8")
    service = CODE_RULE_SERVICE_PATH.read_text(encoding="utf-8")

    assert "selectByTargetFormForUpdate" in code_rule_mapper
    assert '.last("FOR UPDATE")' in code_rule_mapper
    assert "selectByRuleIdAndPeriodKeyForUpdate" in counter_mapper
    assert '.last("FOR UPDATE")' in counter_mapper
    generate_code = re.search(
        r"@Override\s+@Transactional\(rollbackFor = Exception\.class\)\s+public String generateCode"
        r"[\s\S]+?\n    private void validateCodeRuleExists",
        service,
    )
    assert generate_code, "generateCode must remain transactional before selecting locked rows"
    generate_code_body = generate_code.group(0)
    assert "codeRuleMapper.selectByTargetFormForUpdate(targetForm)" in generate_code_body
    assert "codeRuleCounterMapper.selectByRuleIdAndPeriodKeyForUpdate(codeRule.getId(), periodKey)" in generate_code_body
    assert "codeRuleMapper.selectByTargetForm(targetForm)" not in generate_code_body
    assert "codeRuleCounterMapper.selectByRuleIdAndPeriodKey(codeRule.getId(), periodKey)" not in generate_code_body


def test_srm_phase1_access_profile_sql_declares_portal_fields_and_stage_columns() -> None:
    sql = read_access_profile_sql()

    first_line = sql.splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260619_srm_d7_2_supplier_access_risk; type=schema; riskLevel=medium"
    )

    for snippet in [
        "`portal_contact_name` varchar(64)",
        "`portal_contact_phone` varchar(32)",
        "`qualification_expire_date` date",
        "`sample_test_status` varchar(32)",
        "`sample_audit_by` bigint",
        "`sample_audit_name` varchar(64)",
        "`sample_audit_time` datetime",
        "`sample_audit_remark` varchar(500)",
        "`trial_order_status` varchar(32)",
        "`trial_audit_by` bigint",
        "`trial_audit_name` varchar(64)",
        "`trial_audit_time` datetime",
        "`trial_audit_remark` varchar(500)",
    ]:
        assert snippet in sql

    for snippet in [
        "DROP PROCEDURE IF EXISTS ensure_srm_phase1_supplier_access_profile",
        "CREATE PROCEDURE ensure_srm_phase1_supplier_access_profile()",
        "FROM information_schema.COLUMNS",
        "TABLE_NAME = 'srm_supplier_access'",
        "CALL ensure_srm_phase1_supplier_access_profile();",
    ]:
        assert snippet in sql

    assert "ADD COLUMN IF NOT EXISTS" not in sql


def test_srm_phase1_portal_sql_declares_application_table_and_permissions() -> None:
    sql = read_portal_sql()
    normalized = " ".join(sql.split())
    first_line = sql.splitlines()[0]

    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260620_srm_phase1_supplier_access_profile; type=schema; riskLevel=medium"
    )

    assert "CREATE TABLE IF NOT EXISTS `srm_supplier_portal_application`" in sql
    for snippet in [
        "`tenant_id` bigint NOT NULL",
        "`user_id` bigint NOT NULL",
        "`supplier_id` bigint DEFAULT NULL",
        "`company_name` varchar(128)",
        "`unified_social_credit_code` varchar(64)",
        "`contact_name` varchar(64)",
        "`contact_phone` varchar(32)",
        "`contact_email` varchar(128)",
        "`qualification_attachment_urls` varchar(2000)",
        "`qualification_expire_date` date DEFAULT NULL",
        "`bank_name` varchar(128)",
        "`bank_account` varchar(128)",
        "`bank_address` varchar(255)",
        "`application_status` varchar(32)",
        "`submitter_name` varchar(64)",
        "`submitted_time` datetime DEFAULT NULL",
        "`audit_by` bigint DEFAULT NULL",
        "`audit_name` varchar(64)",
        "`audit_time` datetime DEFAULT NULL",
        "`audit_remark` varchar(500)",
        "UNIQUE KEY `uk_srm_supplier_portal_application_tenant_user` (`tenant_id`,`user_id`,`deleted`)",
        "KEY `idx_srm_supplier_portal_application_tenant_status` (`tenant_id`,`application_status`,`submitted_time`)",
        "供应商门户注册申请",
        "991026",
        "991027",
        "991028",
        "srm:supplier-portal:review",
        "srm:supplier-portal:audit",
        "srm/supplier-portal/review/index",
    ]:
        assert snippet in sql

    assert "ON DUPLICATE KEY UPDATE" not in normalized
    assert "INSERT IGNORE" not in normalized
