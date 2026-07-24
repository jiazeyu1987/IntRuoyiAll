from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260621_srm_phase45_simulated_execution.sql"
ERROR_CODE_PATH = (
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
    / "enums"
    / "ErrorCodeConstants.java"
)
CODE_RULE_ENUM_PATH = (
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
    / "enums"
    / "coderule"
    / "SrmCodeRuleTargetFormEnum.java"
)
OUTSOURCE_SERVICE_PATH = (
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
    / "outsourceexecution"
    / "SrmOutsourceExecutionServiceImpl.java"
)
PAYMENT_SERVICE_PATH = (
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
    / "paymentexecution"
    / "SrmPaymentExecutionServiceImpl.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def normalized_sql() -> str:
    return " ".join(read(SQL_PATH).split())


def test_srm_phase45_release_migration_metadata_uses_manifest_contract() -> None:
    first_line = read(SQL_PATH).splitlines()[0]
    assert first_line == (
        "-- release-migration: allowedEnvironments=test,backup,prod; "
        "dependsOn=20260621_srm_phase3_purchase_order; type=schema; riskLevel=medium"
    )


def test_srm_phase45_sql_declares_simulated_execution_tables_and_columns() -> None:
    sql = read(SQL_PATH)

    for table_name in [
        "srm_outsource_execution",
        "srm_outsource_execution_event",
        "srm_reconciliation",
        "srm_payment_execution",
        "srm_payment_execution_event",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`simulation_source` varchar(32) NOT NULL",
        "`simulation_label` varchar(128) NOT NULL",
        "`simulation_remark` varchar(500) DEFAULT NULL",
        "`issue_notice_no` varchar(64) DEFAULT NULL",
        "`progress_percent` decimal(24,6) DEFAULT NULL",
        "`reconciliation_amount` decimal(24,2) NOT NULL",
        "`payment_term_summary` varchar(500) NOT NULL",
        "`apply_amount` decimal(24,2) NOT NULL",
        "`push_remark` varchar(500) DEFAULT NULL",
        "UNIQUE KEY `uk_srm_outsource_execution_tenant_order` (`tenant_id`, `source_purchase_order_id`, `deleted`)",
        "UNIQUE KEY `uk_srm_reconciliation_tenant_execution` (`tenant_id`, `execution_id`, `deleted`)",
        "UNIQUE KEY `uk_srm_payment_execution_tenant_reconciliation` (`tenant_id`, `reconciliation_id`, `deleted`)",
    ]:
        assert snippet in sql


def test_srm_phase45_sql_declares_menu_routes_permissions_and_code_rules() -> None:
    sql = read(SQL_PATH)

    for snippet in [
        "SELECT 991090, '委外执行'",
        "SELECT 991094, '供应商委外协同台'",
        "SELECT 991096, '付款执行'",
        "'srm:outsource-execution:query'",
        "'srm:outsource-execution:create'",
        "'srm:outsource-execution:update'",
        "'srm:payment-execution:query'",
        "'srm:payment-execution:create'",
        "'srm:payment-execution:approve'",
        "`component` = 'srm/outsource-execution/index'",
        "`component_name` = 'SrmOutsourceExecution'",
        "`component` = 'srm/outsource-execution/my'",
        "`component_name` = 'SrmOutsourceExecutionMy'",
        "`component` = 'srm/payment-execution/index'",
        "`component_name` = 'SrmPaymentExecution'",
        "'SRM_OUTSOURCE_EXECUTION'",
        "'OUTSOURCE_EXECUTION'",
        "'SRM_OUTSOURCE_EXECUTION_EVENT'",
        "'OUTSOURCE_EXECUTION_EVENT'",
        "'SRM_OUTSOURCE_RECONCILIATION'",
        "'OUTSOURCE_RECONCILIATION'",
        "'SRM_PAYMENT_EXECUTION'",
        "'PAYMENT_EXECUTION'",
        "'SRM_PAYMENT_EXECUTION_EVENT'",
        "'PAYMENT_EXECUTION_EVENT'",
    ]:
        assert snippet in sql


def test_srm_phase45_sql_is_fail_fast_and_non_destructive() -> None:
    sql = read(SQL_PATH)
    upper_sql = sql.upper()

    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DELETE FROM `SRM_OUTSOURCE_EXECUTION`",
        "DELETE FROM `SRM_PAYMENT_EXECUTION`",
    ]:
        assert forbidden not in upper_sql

    for required in [
        "缺少 SRM 基础菜单",
        "Invalid system_tenant_package.menu_ids JSON",
        "Missing SRM outsource-execution route menu for get-permission-info",
        "Missing SRM payment-execution route menu for get-permission-info",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert required in sql


def test_srm_phase45_java_contracts_cover_new_targets_and_error_codes() -> None:
    error_codes = read(ERROR_CODE_PATH)
    code_rule_enum = read(CODE_RULE_ENUM_PATH)
    outsource_service = read(OUTSOURCE_SERVICE_PATH)
    payment_service = read(PAYMENT_SERVICE_PATH)

    for snippet in [
        "OUTSOURCE_EXECUTION_NOT_EXISTS",
        "OUTSOURCE_EXECUTION_DUPLICATE",
        "OUTSOURCE_EXECUTION_SUPPLIER_CONTEXT_MISSING",
        "OUTSOURCE_EXECUTION_SUPPLIER_FORBIDDEN",
        "OUTSOURCE_EXECUTION_STATUS_INVALID",
        "OUTSOURCE_EXECUTION_RECONCILIATION_PREREQUISITE_MISSING",
        "PAYMENT_EXECUTION_RECONCILIATION_REQUIRED",
        "PAYMENT_EXECUTION_DUPLICATE",
        "PAYMENT_EXECUTION_CONTRACT_SUPPLIER_MISMATCH",
        "PAYMENT_EXECUTION_CONTRACT_PAYMENT_REQUIRED",
        "PAYMENT_EXECUTION_STATUS_INVALID",
        "PAYMENT_EXECUTION_PUSH_REMARK_REQUIRED",
    ]:
        assert snippet in error_codes

    for snippet in [
        'OUTSOURCE_EXECUTION("OUTSOURCE_EXECUTION", "委外执行单")',
        'OUTSOURCE_EXECUTION_EVENT("OUTSOURCE_EXECUTION_EVENT", "委外执行事件")',
        'OUTSOURCE_RECONCILIATION("OUTSOURCE_RECONCILIATION", "委外对账单")',
        'PAYMENT_EXECUTION("PAYMENT_EXECUTION", "付款执行单")',
        'PAYMENT_EXECUTION_EVENT("PAYMENT_EXECUTION_EVENT", "付款执行事件")',
    ]:
        assert snippet in code_rule_enum

    for snippet in [
        'private static final String SIMULATION_SOURCE = "LOCAL_SIMULATED";',
        'private static final String SIMULATION_LABEL = "测试租户受控模拟链路";',
        "supplierPortalApplicationService.getCurrentApplication()",
        "SrmOutsourceExecutionStatusEnum.RECONCILED",
        "SrmReconciliationStatusEnum.RECONCILED",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.OUTSOURCE_EXECUTION.getTargetForm())",
    ]:
        assert snippet in outsource_service

    for snippet in [
        "SrmPaymentExecutionStatusEnum.DRAFT",
        "SrmPaymentExecutionStatusEnum.PUSH_SUCCESS",
        "SrmPaymentExecutionStatusEnum.PUSH_FAILED",
        "buildPaymentTermSummary",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PAYMENT_EXECUTION.getTargetForm())",
        "paymentExecutionMapper.selectByReconciliationId(getRequiredTenantId(), reconciliation.getId())",
    ]:
        assert snippet in payment_service
