from pathlib import Path
import re


REPO_ROOT = Path(__file__).resolve().parents[2]
SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_srm_d7_2_supplier_access_risk.sql"
T2_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_srm_d7_3_plan_framework.sql"
T3_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260619_srm_d8_1_non_bidding.sql"
T4_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260620_srm_d10_1_tender.sql"
T5_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260620_srm_d9_1_contract.sql"
T6_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260628_srm_t6_nas_locator.sql"
T6_BLACKLIST_SQL_PATH = REPO_ROOT / "sql" / "mysql" / "20260701_srm_t6_nas_locator_blacklist_config.sql"
ERROR_CODE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "enums" / "ErrorCodeConstants.java"
)
ACCESS_DO_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "dal" / "dataobject" / "supplier" / "SrmSupplierAccessDO.java"
)
RISK_DO_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "dal" / "dataobject" / "supplier" / "SrmSupplierRiskDO.java"
)
ERP_DO_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "dal" / "dataobject" / "supplier" / "SrmErpSupplierDO.java"
)
SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "supplier" / "SrmSupplierAccessRiskServiceImpl.java"
)
PROCUREMENT_SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "procurement" / "SrmProcurementPlanServiceImpl.java"
)
FRAMEWORK_SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "framework" / "SrmFrameworkAgreementServiceImpl.java"
)
NON_BIDDING_SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "nonbidding" / "SrmNonBiddingProcurementServiceImpl.java"
)
TENDER_SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "tender" / "SrmTenderProcurementServiceImpl.java"
)
TENDER_CONTROLLER_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "controller" / "admin" / "tender" / "SrmTenderProjectController.java"
)
CONTRACT_SERVICE_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "service" / "contract" / "SrmProcurementContractServiceImpl.java"
)
CONTRACT_CONTROLLER_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "controller" / "admin" / "contract" / "SrmProcurementContractController.java"
)
SOURCING_PROJECT_MAPPER_PATH = (
    REPO_ROOT / "yudao-module-srm" / "src" / "main" / "java" / "cn" / "iocoder" / "yudao"
    / "module" / "srm" / "dal" / "mysql" / "procurement" / "SrmSourcingProjectMapper.java"
)


def read(path: Path) -> str:
    assert path.exists(), f"missing required file: {path}"
    return path.read_text(encoding="utf-8")


def test_srm_d7_d10_release_migration_metadata_uses_manifest_contract() -> None:
    expected_metadata = {
        SQL_PATH: "dependsOn=20260618_srm_d7_1_code_rule_baseline; type=schema; riskLevel=medium",
        T2_SQL_PATH: "dependsOn=20260619_srm_d7_2_supplier_access_risk; type=schema; riskLevel=medium",
        T3_SQL_PATH: "dependsOn=20260619_srm_d7_3_plan_framework; type=schema; riskLevel=medium",
        T4_SQL_PATH: "dependsOn=20260619_srm_d8_1_non_bidding; type=schema; riskLevel=medium",
        T5_SQL_PATH: "dependsOn=20260620_srm_d10_1_tender; type=schema; riskLevel=medium",
        T6_SQL_PATH: "dependsOn=20260621_srm_phase45_simulated_execution; type=schema; riskLevel=medium",
        T6_BLACKLIST_SQL_PATH: "dependsOn=20260628_srm_t6_nas_locator; type=schema; riskLevel=medium",
    }
    for sql_path, suffix in expected_metadata.items():
        first_line = read(sql_path).splitlines()[0]
        assert first_line == f"-- release-migration: allowedEnvironments=test,backup,prod; {suffix}"
        assert ".sql" not in first_line
        assert "type=schema-menu" not in first_line


def normalized_sql() -> str:
    return " ".join(read(SQL_PATH).split())


def read_t6_combined_sql() -> str:
    return "\n".join([
        read(T6_SQL_PATH),
        read(T6_BLACKLIST_SQL_PATH),
    ])


def test_srm_t1_tables_and_columns_are_declared() -> None:
    sql = read(SQL_PATH)
    for table_name in ["srm_supplier_access", "srm_supplier_risk"]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`supplier_id` bigint NOT NULL",
        "`access_status` varchar(32) NOT NULL",
        "`enabled` bit(1) NOT NULL DEFAULT b'1'",
        "`submitted_by` bigint DEFAULT NULL",
        "`audit_remark` varchar(500) DEFAULT NULL",
        "`risk_level` varchar(16) NOT NULL",
        "`risk_status` varchar(16) NOT NULL",
        "`source_type` varchar(32) NOT NULL",
        "`risk_description` varchar(500) NOT NULL",
        "`resolution_remark` varchar(500) DEFAULT NULL",
    ]:
        assert snippet in sql


def test_srm_t1_menu_permissions_and_components_are_declared() -> None:
    sql = read(SQL_PATH)
    for snippet in [
        "供应商管理",
        "准入管理",
        "风险管理",
        "`component` = 'srm/supplier-access/index'",
        "`component_name` = 'SrmSupplierAccess'",
        "`component` = 'srm/supplier-risk/index'",
        "`component_name` = 'SrmSupplierRisk'",
        "'srm:supplier-access:query'",
        "'srm:supplier-access:create'",
        "'srm:supplier-access:update'",
        "'srm:supplier-access:audit'",
        "'srm:supplier-access:enable'",
        "'srm:supplier-access:check'",
        "'srm:supplier-risk:query'",
        "'srm:supplier-risk:create'",
        "'srm:supplier-risk:resolve'",
        "`system_role_menu`",
        "`system_tenant_package`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert snippet in sql




def test_srm_t1_profile_route_and_permission_are_declared() -> None:
    sql = read(SQL_PATH)
    for snippet in [
        "SELECT 991024",
        "SELECT 991025",
        "`path` <> 'profile'",
        "`component` = 'srm/supplier-profile/index'",
        "`component_name` = 'SrmSupplierProfile'",
        "'srm:supplier-profile:query'",
        "Missing SRM supplier-profile route menu for get-permission-info",
    ]:
        assert snippet in sql


def test_srm_t1_route_menu_guards_do_not_confuse_query_buttons_with_pages() -> None:
    sql = read(SQL_PATH)
    buggy_guards = [
        "(`id` = 991011 OR `component` = 'srm/supplier-access/index' OR `permission` = 'srm:supplier-access:query')",
        "(`id` = 991020 OR `component` = 'srm/supplier-risk/index' OR `permission` = 'srm:supplier-risk:query')",
        "(`id` = 991024 OR `component` = 'srm/supplier-profile/index' OR `permission` = 'srm:supplier-profile:query')",
    ]
    fixed_guards = [
        "(`id` = 991011 OR `component` = 'srm/supplier-access/index' OR (`permission` = 'srm:supplier-access:query' AND `type` = 2))",
        "(`id` = 991020 OR `component` = 'srm/supplier-risk/index' OR (`permission` = 'srm:supplier-risk:query' AND `type` = 2))",
        "(`id` = 991024 OR `component` = 'srm/supplier-profile/index' OR (`permission` = 'srm:supplier-profile:query' AND `type` = 2))",
    ]
    for snippet in buggy_guards:
        assert snippet not in sql
    for snippet in fixed_guards:
        assert snippet in sql


def test_srm_t1_sql_is_fail_fast_and_does_not_write_erp_supplier() -> None:
    sql = read(SQL_PATH)
    upper_sql = sql.upper()
    for forbidden in [
        "DROP TABLE",
        "TRUNCATE TABLE",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "UPDATE `ERP_SUPPLIER`",
        "DELETE FROM `ERP_SUPPLIER`",
        "INSERT INTO `ERP_SUPPLIER`",
    ]:
        assert forbidden not in upper_sql
    assert "缺少 SRM D7-1 基础菜单" in sql
    assert "Invalid system_tenant_package.menu_ids JSON" in sql


def test_srm_t1_error_codes_and_tenant_models_are_present() -> None:
    error_codes = read(ERROR_CODE_PATH)
    for symbol in [
        "SUPPLIER_ACCESS_NOT_EXISTS",
        "SUPPLIER_ACCESS_DUPLICATE",
        "SUPPLIER_REFERENCE_NOT_EXISTS",
        "SUPPLIER_REFERENCE_DISABLED",
        "SUPPLIER_ELIGIBILITY_BLOCKED",
        "SUPPLIER_RISK_ALREADY_RESOLVED",
    ]:
        assert symbol in error_codes

    access_do = read(ACCESS_DO_PATH)
    risk_do = read(RISK_DO_PATH)
    erp_do = read(ERP_DO_PATH)
    assert "extends TenantBaseDO" in access_do
    assert "extends TenantBaseDO" in risk_do
    assert "@TenantIgnore" in erp_do
    assert '@TableName("erp_supplier")' in erp_do


def test_srm_t1_service_keeps_reusable_eligibility_gate_and_no_erp_write_calls() -> None:
    service = read(SERVICE_PATH)
    for snippet in [
        "public SrmSupplierEligibilityRespVO checkSupplierEligibility(Long supplierId)",
        "public void validateSupplierEligible(Long supplierId)",
        "Long tenantId = getRequiredTenantId();",
        "supplierRiskMapper.selectOpenHighRiskListBySupplierId(tenantId, supplierId)",
        "supplierAccessMapper.selectBySupplierId(tenantId, supplierId)",
        "selectById(supplierId)",
        "Objects.equals(supplier.getTenantId(), tenantId)",
        "SUPPLIER_REFERENCE_CROSS_TENANT",
    ]:
        assert snippet in service

    assert "createSupplier(" not in service
    assert "updateSupplier(" not in service
    assert "deleteSupplier(" not in service


def test_srm_t1_role_menu_insert_is_idempotent() -> None:
    sql = read(SQL_PATH)
    assert re.search(
        r"INSERT\s+INTO\s+`system_role_menu`[\s\S]+NOT\s+EXISTS",
        sql,
        re.IGNORECASE,
    ), "role-menu grant must stay idempotent"


def test_srm_t2_plan_framework_schema_menu_and_services_are_contractual() -> None:
    sql = read(T2_SQL_PATH)
    for table_name in [
        "srm_procurement_plan",
        "srm_procurement_plan_line",
        "srm_procurement_approval_record",
        "srm_sourcing_project",
        "srm_sourcing_project_line",
        "srm_framework_plan",
        "srm_framework_plan_line",
        "srm_framework_agreement",
        "srm_framework_agreement_line",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`source_plan_id` bigint",
        "`source_plan_line_id` bigint",
        "`framework_plan_id` bigint NOT NULL",
        "`framework_plan_line_id` bigint NOT NULL",
        "`tenant_id` bigint NOT NULL",
        "`deleted` bit(1) NOT NULL DEFAULT b'0'",
        "UNIQUE KEY `uk_srm_sourcing_project_tenant_source_plan`",
        "UNIQUE KEY `uk_srm_framework_agreement_tenant_plan`",
        "`component` = 'srm/procurement-plan/index'",
        "`component_name` = 'SrmProcurementPlan'",
        "`component` = 'srm/framework-plan/index'",
        "`component_name` = 'SrmFrameworkPlan'",
        "'srm:procurement-plan:generate'",
        "'srm:framework-plan:agreement'",
        "INSERT INTO `srm_code_rule`",
        "'SRM_PROCUREMENT_PLAN'",
        "'PROCUREMENT_PLAN'",
        "'SRM_PROCUREMENT_PLAN_LINE'",
        "'PROCUREMENT_PLAN_LINE'",
        "'SRM_NON_TENDER_PROJECT'",
        "'NON_TENDER_PROJECT'",
        "'SRM_TENDER_PROJECT'",
        "'TENDER_PROJECT'",
        "'SRM_FRAMEWORK_PLAN'",
        "'FRAMEWORK_PLAN'",
        "'SRM_FRAMEWORK_AGREEMENT'",
        "'FRAMEWORK_AGREEMENT'",
        "`system_role_menu`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert snippet in sql

    upper_sql = sql.upper()
    for forbidden in [
        "INSERT INTO `ERP_",
        "UPDATE `ERP_",
        "DELETE FROM `ERP_",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DROP TABLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_sql

    procurement_service = read(PROCUREMENT_SERVICE_PATH)
    for snippet in [
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN.getTargetForm())",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_PLAN_LINE.getTargetForm())",
        "codeRuleService.generateCode(targetProjectForm)",
        "SrmProcurementPlanStatusEnum.APPROVED",
        "SrmProcurementPlanStatusEnum.GENERATED",
        "PROCUREMENT_PLAN_GENERATE_DUPLICATE",
    ]:
        assert snippet in procurement_service

    framework_service = read(FRAMEWORK_SERVICE_PATH)
    for snippet in [
        "supplierAccessRiskService.checkSupplierEligibility",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.FRAMEWORK_PLAN.getTargetForm())",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.FRAMEWORK_AGREEMENT.getTargetForm())",
        "SrmFrameworkPlanStatusEnum.AGREEMENT_CREATED",
        "FRAMEWORK_AGREEMENT_DUPLICATE",
    ]:
        assert snippet in framework_service


def test_srm_t3_non_bidding_schema_menu_and_services_are_contractual() -> None:
    sql = read(T3_SQL_PATH)
    for table_name in [
        "srm_non_bidding_supplier_scope",
        "srm_non_bidding_quote",
        "srm_non_bidding_quote_line",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`quote_start_time` datetime",
        "`quote_end_time` datetime",
        "`quote_mode` varchar(32) DEFAULT NULL",
        "`publish_attachment_url` varchar(500)",
        "`deal_quote_id` bigint",
        "`deal_supplier_id` bigint",
        "`deal_amount` decimal(18,2)",
        "`contract_id` bigint",
        "UNIQUE KEY `uk_srm_non_bidding_scope_tenant_project_supplier`",
        "UNIQUE KEY `uk_srm_non_bidding_quote_tenant_project_supplier`",
        "`component` = 'srm/non-bidding-project/index'",
        "`component_name` = 'SrmNonBiddingProject'",
        "'非招标项目'",
        "'非招标项目查询'",
        "'非招标项目发布'",
        "'非招标供应商报价'",
        "'非招标成交确认'",
        "'非招标可建合同'",
        "'srm:non-bidding-project:publish'",
        "'srm:non-bidding-project:quote'",
        "'srm:non-bidding-project:deal'",
        "'srm:non-bidding-project:contract'",
        "UPDATE `system_menu`",
        "`system_role_menu`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
    ]:
        assert snippet in sql

    upper_sql = sql.upper()
    for forbidden in [
        "INSERT INTO `ERP_",
        "UPDATE `ERP_",
        "DELETE FROM `ERP_",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DROP TABLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_sql

    service = read(NON_BIDDING_SERVICE_PATH)
    for snippet in [
        "supplierAccessRiskService.checkSupplierEligibility",
        "SrmSourcingProjectStatusEnum.PUBLISHED",
        "SrmSourcingProjectStatusEnum.DEAL_CONFIRMED",
        "NON_BIDDING_PUBLISH_ATTACHMENT_REQUIRED",
        "NON_BIDDING_QUOTE_MODE_INVALID",
        "NON_BIDDING_QUOTE_DUPLICATE",
        "project.getQuoteMode()",
        "QUOTE_MODE_PUBLIC.equals(normalizeQuoteMode(project.getQuoteMode()))",
        "setComparisonSummary(buildComparisonSummary(quotes))",
        "setPriceTrends(buildPriceTrends(project, respVO.getLines()))",
        "getContractableProjectPage",
    ]:
        assert snippet in service

    mapper = read(SOURCING_PROJECT_MAPPER_PATH)
    assert "selectContractableNonBiddingPage" in mapper
    assert "isNull(SrmSourcingProjectDO::getContractId)" in mapper


def test_srm_t3_phase2_quote_mode_and_price_trend_contract_is_declared() -> None:
    sql = read(T3_SQL_PATH)

    for snippet in [
        "column_name` = 'quote_mode'",
        "ADD COLUMN `quote_mode` varchar(32) DEFAULT NULL COMMENT '询价模式'",
        "'srm:non-bidding-project:publish'",
        "'srm:non-bidding-project:quote'",
        "'srm:non-bidding-project:deal'",
    ]:
        assert snippet in sql


def test_srm_t4_tender_schema_menu_and_services_are_contractual() -> None:
    sql = read(T4_SQL_PATH)
    for table_name in [
        "srm_tender_notice",
        "srm_tender_document",
        "srm_tender_submission",
        "srm_tender_expert",
        "srm_tender_expert_application",
        "srm_tender_committee_member",
        "srm_tender_candidate",
        "srm_tender_winning_result",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`tenant_id` bigint NOT NULL",
        "`deleted` bit(1) NOT NULL DEFAULT b'0'",
        "UNIQUE KEY `uk_srm_tender_notice_tenant_project`",
        "UNIQUE KEY `uk_srm_tender_document_tenant_project`",
        "UNIQUE KEY `uk_srm_tender_submission_tenant_project_supplier`",
        "UNIQUE KEY `uk_srm_tender_expert_application_tenant_no`",
        "UNIQUE KEY `uk_srm_tender_committee_tenant_project_expert`",
        "UNIQUE KEY `uk_srm_tender_candidate_tenant_project_submission`",
        "UNIQUE KEY `uk_srm_tender_winning_result_tenant_project`",
        "`component` = 'srm/tender-project/index'",
        "`component_name` = 'SrmTenderProject'",
        "'招标项目'",
        "'招标项目查询'",
        "'招标项目发布'",
        "'供应商投标'",
        "'专家库维护'",
        "'评委会组建'",
        "'中标候选'",
        "'中标结果'",
        "'srm:tender-project:query'",
        "'srm:tender-project:publish'",
        "'srm:tender-project:submit-bid'",
        "'srm:tender-project:expert'",
        "'srm:tender-project:committee'",
        "'srm:tender-project:candidate'",
        "'srm:tender-project:winning'",
        "`system_role_menu`",
        "`system_tenant_package`",
        "`tenant_id`)",
        "`tmp_srm_d10_1_package_menu_ids`",
        "JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$')",
        "LEFT JOIN `system_tenant` AS `tenant`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
        "INSERT INTO `srm_code_rule`",
        "'SRM_EXPERT_DRAW_APPLICATION'",
        "'EXPERT_DRAW_APPLICATION'",
    ]:
        assert snippet in sql

    upper_sql = sql.upper()
    for forbidden in [
        "INSERT INTO `ERP_",
        "UPDATE `ERP_",
        "DELETE FROM `ERP_",
        "INSERT INTO `K3_",
        "UPDATE `K3_",
        "DELETE FROM `K3_",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DROP TABLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_sql
    assert ".`user_id`" not in sql

    service = read(TENDER_SERVICE_PATH)
    for snippet in [
        "supplierAccessRiskService.checkSupplierEligibility",
        "TENDER_PUBLISH_ATTACHMENT_REQUIRED",
        "TENDER_SUBMISSION_WINDOW_INVALID",
        "TENDER_SUBMISSION_WINDOW_CLOSED",
        "TENDER_SUBMISSION_SUPPLIER_DUPLICATE",
        "TENDER_COMMITTEE_MEMBER_DUPLICATE",
        "TENDER_COMMITTEE_MEMBER_INSUFFICIENT",
        "TENDER_EXPERT_STATUS_INVALID",
        "TENDER_EXPERT_SPECIALTY_MISMATCH",
        "TENDER_CANDIDATE_SUBMISSION_REQUIRED",
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.EXPERT_DRAW_APPLICATION.getTargetForm())",
        "SrmSourcingProjectStatusEnum.COMMITTEE_CONFIRMED",
        "SrmSourcingProjectStatusEnum.CANDIDATE_CONFIRMED",
        "SrmSourcingProjectStatusEnum.WINNING_CONFIRMED",
        "project.setContractId(null)",
        "TENDER_WINNING_REMARK_REQUIRED",
        "if (submissionIds.isEmpty())",
    ]:
        assert snippet in service

    controller = read(TENDER_CONTROLLER_PATH)
    for snippet in [
        '@RequestMapping("/srm/tender-project")',
        '@GetMapping("/page")',
        '@PostMapping("/publish")',
        '@PostMapping("/submit-bid")',
        '@PostMapping("/expert/create")',
        '@PutMapping("/expert/approve")',
        '@PostMapping("/committee")',
        '@PostMapping("/candidate")',
        '@PostMapping("/winning")',
    ]:
        assert snippet in controller

    mapper = read(SOURCING_PROJECT_MAPPER_PATH)
    assert "selectTenderPage" in mapper
    assert "SrmProcurementMethodEnum.TENDER.getMethod()" in mapper


def test_srm_t5_contract_schema_menu_and_services_are_contractual() -> None:
    sql = read(T5_SQL_PATH)
    for table_name in [
        "srm_procurement_contract",
        "srm_procurement_contract_payment",
        "srm_procurement_contract_signing",
        "srm_procurement_contract_attachment",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`tenant_id` bigint NOT NULL",
        "`contract_no` varchar(64) NOT NULL",
        "`source_type` varchar(32) NOT NULL",
        "`source_id` bigint NOT NULL",
        "`supplier_id` bigint NOT NULL",
        "`contract_amount` decimal(18,2) NOT NULL",
        "`contract_status` varchar(32) NOT NULL",
        "`payment_stage` varchar(64) NOT NULL",
        "`payment_ratio` decimal(8,2) NOT NULL",
        "`signing_party` varchar(64) NOT NULL",
        "`attachment_url` varchar(500) NOT NULL",
        "UNIQUE KEY `uk_srm_procurement_contract_tenant_no`",
        "KEY `idx_srm_procurement_contract_tenant_source`",
        "`component` = 'srm/procurement-contract/index'",
        "`component_name` = 'SrmProcurementContract'",
        "'采购合同'",
        "'采购合同查询'",
        "'采购合同创建'",
        "'采购合同作废'",
        "'采购合同删除'",
        "'srm:procurement-contract:query'",
        "'srm:procurement-contract:create'",
        "'srm:procurement-contract:cancel'",
        "'srm:procurement-contract:delete'",
        "`system_role_menu`",
        "`system_tenant_package`",
        "`tmp_srm_d9_1_package_menu_ids`",
        "JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$')",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
        "INSERT INTO `srm_code_rule`",
        "'SRM_PROCUREMENT_CONTRACT'",
        "'PROCUREMENT_CONTRACT'",
    ]:
        assert snippet in sql

    upper_sql = sql.upper()
    for forbidden in [
        "INSERT INTO `ERP_",
        "UPDATE `ERP_",
        "DELETE FROM `ERP_",
        "INSERT INTO `K3_",
        "UPDATE `K3_",
        "DELETE FROM `K3_",
        "INSERT INTO `FINANCE_",
        "UPDATE `FINANCE_",
        "DELETE FROM `FINANCE_",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DROP TABLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_sql

    service = read(CONTRACT_SERVICE_PATH)
    for snippet in [
        "codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PROCUREMENT_CONTRACT.getTargetForm())",
        "PROCUREMENT_CONTRACT_PAYMENT_REQUIRED",
        "PROCUREMENT_CONTRACT_SIGNING_REQUIRED",
        "PROCUREMENT_CONTRACT_ATTACHMENT_REQUIRED",
        "SrmSourcingProjectStatusEnum.DEAL_CONFIRMED",
        "SrmSourcingProjectStatusEnum.WINNING_CONFIRMED",
        "SrmSourcingProjectStatusEnum.CONTRACT_CREATED",
        "contractMapper.selectEffectiveBySource",
        "source.setContractId(contract.getId())",
        "source.setProjectStatus(SrmSourcingProjectStatusEnum.CONTRACT_CREATED.getStatus())",
        "restoreSource(contract)",
        "clearContractAndRestoreStatus",
        "Explicit SET is required because entity update strategies skip null fields",
    ]:
        assert snippet in service

    controller = read(CONTRACT_CONTROLLER_PATH)
    for snippet in [
        '@RequestMapping("/srm/procurement-contract")',
        '@GetMapping("/page")',
        '@PostMapping("/create")',
        '@PutMapping("/cancel")',
        '@DeleteMapping("/delete")',
        "srm:procurement-contract:query",
        "srm:procurement-contract:create",
        "srm:procurement-contract:cancel",
        "srm:procurement-contract:delete",
    ]:
        assert snippet in controller

    mapper = read(SOURCING_PROJECT_MAPPER_PATH)
    assert "clearContractAndRestoreStatus" in mapper
    assert ".set(SrmSourcingProjectDO::getContractId, null)" in mapper


def test_srm_t6_nas_locator_schema_menu_and_guards_are_contractual() -> None:
    sql = read_t6_combined_sql()
    for table_name in [
        "srm_nas_locator_refresh_task",
        "srm_nas_locator_entry",
    ]:
        assert f"CREATE TABLE IF NOT EXISTS `{table_name}`" in sql

    for snippet in [
        "`tenant_id` bigint NOT NULL",
        "`status` varchar(16) NOT NULL",
        "`scope_share` varchar(255) NOT NULL",
        "`root_path` varchar(255) NOT NULL",
        "`directory_count` bigint NOT NULL DEFAULT 0",
        "`file_count` bigint NOT NULL DEFAULT 0",
        "`error_message` varchar(1000) DEFAULT NULL",
        "`refresh_task_id` bigint NOT NULL",
        "`entry_type` varchar(16) NOT NULL",
        "`name` varchar(255) NOT NULL",
        "`path` varchar(1000) NOT NULL",
        "`path_hash` char(64) NOT NULL",
        "`parent_path` varchar(1000) NOT NULL",
        "`modified_at` bigint DEFAULT NULL",
        "UNIQUE KEY `uk_srm_nas_locator_entry_task_type_path_hash`",
        "KEY `idx_srm_nas_locator_refresh_task_tenant_status`",
        "KEY `idx_srm_nas_locator_refresh_task_tenant_finish`",
        "KEY `idx_srm_nas_locator_entry_tenant_task_type`",
        "KEY `idx_srm_nas_locator_entry_tenant_name`",
        "SELECT 991100, 'NAS定位'",
        "SELECT 991101, 'NAS定位查询'",
        "SELECT 991102, 'NAS定位刷新'",
        "SELECT 991103, 'NAS定位下载'",
        "SET @SRM_NAS_BLACKLIST_MENU_ID := 991105;",
        "SELECT @SRM_NAS_BLACKLIST_MENU_ID, 'NAS定位黑名单'",
        "`path` = 'nas-locator'",
        "`component` = 'srm/nas-locator/index'",
        "`component_name` = 'SrmNasLocator'",
        "'srm:nas-locator:query'",
        "'srm:nas-locator:refresh'",
        "'srm:nas-locator:config'",
        "'srm:nas-locator:download'",
        "`system_role_menu`",
        "`system_tenant_package`",
        "JSON_VALID",
        "SIGNAL SQLSTATE '45000'",
        "JSON_CONTAINS(`package`.`menu_ids`, CAST('991000' AS JSON), '$')",
        "Missing SRM nas-locator route menu for get-permission-info",
        "Invalid system_tenant_package.menu_ids JSON",
        "缺少 SRM 基础菜单，禁止安装 T6 NAS定位 菜单",
        "缺少 SRM T6 NAS定位 路由菜单，禁止安装黑名单按钮权限",
        "SRM T6 NAS定位 黑名单菜单 ID 已被其他权限占用",
        "`role`.`code` = 'srm_admin'",
    ]:
        assert snippet in sql

    upper_sql = sql.upper()
    for forbidden in [
        "INSERT INTO `ERP_",
        "UPDATE `ERP_",
        "DELETE FROM `ERP_",
        "INSERT INTO `K3_",
        "UPDATE `K3_",
        "DELETE FROM `K3_",
        "INSERT INTO `FINANCE_",
        "UPDATE `FINANCE_",
        "DELETE FROM `FINANCE_",
        "INSERT IGNORE",
        "ON DUPLICATE KEY UPDATE",
        "DROP TABLE",
        "TRUNCATE TABLE",
    ]:
        assert forbidden not in upper_sql

    assert re.search(
        r"INSERT\s+INTO\s+`system_role_menu`[\s\S]+NOT\s+EXISTS",
        sql,
        re.IGNORECASE,
    ), "role-menu grant must stay idempotent"
