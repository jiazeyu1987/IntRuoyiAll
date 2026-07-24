const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const backendRoot = path.resolve(frontendRoot, '../ruoyi-vue-pro')

function readFrontend(relativePath) {
  const absolutePath = path.resolve(frontendRoot, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function readBackend(relativePath) {
  const absolutePath = path.resolve(backendRoot, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist in backend worktree`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const procurementApi = readFrontend('src/api/srm/procurement-plan/index.ts')
const frameworkApi = readFrontend('src/api/srm/framework-plan/index.ts')
const procurementPage = readFrontend('src/views/srm/procurement-plan/index.vue')
const frameworkPage = readFrontend('src/views/srm/framework-plan/index.vue')
const sql = readBackend('sql/mysql/20260619_srm_d7_3_plan_framework.sql')
const procurementService = readBackend(
  'yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/procurement/SrmProcurementPlanServiceImpl.java'
)
const frameworkService = readBackend(
  'yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/framework/SrmFrameworkAgreementServiceImpl.java'
)
const errorCodes = readBackend(
  'yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/enums/ErrorCodeConstants.java'
)

for (const fragment of [
  '/srm/procurement-plan/page',
  '/srm/procurement-plan/get',
  '/srm/procurement-plan/create',
  '/srm/procurement-plan/submit',
  '/srm/procurement-plan/approve',
  '/srm/procurement-plan/reject',
  '/srm/procurement-plan/generate-sourcing',
  'srmProcurementMethodOptions',
  'srmProcurementPlanStatusOptions',
  'TENDER',
  'NON_BIDDING',
  'GENERATED'
]) {
  assert(procurementApi.includes(fragment), `procurement-plan API must include ${fragment}`)
}

for (const fragment of [
  '/srm/framework-plan/page',
  '/srm/framework-plan/get',
  '/srm/framework-plan/create',
  '/srm/framework-plan/submit',
  '/srm/framework-plan/approve',
  '/srm/framework-plan/reject',
  '/srm/framework-plan/create-agreement',
  '/srm/framework-plan/agreement-page',
  'srmFrameworkPlanStatusOptions',
  'AGREEMENT_CREATED'
]) {
  assert(frameworkApi.includes(fragment), `framework-plan API must include ${fragment}`)
}

for (const fragment of [
  "defineOptions({ name: 'SrmProcurementPlan' })",
  "v-hasPermi=\"['srm:procurement-plan:create']\"",
  "v-hasPermi=\"['srm:procurement-plan:submit']\"",
  "v-hasPermi=\"['srm:procurement-plan:audit']\"",
  "v-hasPermi=\"['srm:procurement-plan:generate']\"",
  '新增计划',
  '采购计划详情',
  '计划行项目',
  '生成项目',
  '非招标项目',
  '招标项目',
  'message.error'
]) {
  assert(procurementPage.includes(fragment), `procurement-plan page must include ${fragment}`)
}

for (const fragment of [
  "defineOptions({ name: 'SrmFrameworkPlan' })",
  "v-hasPermi=\"['srm:framework-plan:create']\"",
  "v-hasPermi=\"['srm:framework-plan:submit']\"",
  "v-hasPermi=\"['srm:framework-plan:audit']\"",
  "v-hasPermi=\"['srm:framework-plan:agreement']\"",
  '新增框架计划',
  '合格供应商',
  '框架计划详情',
  '框架物料行',
  '框架协议',
  '生成协议',
  'message.error'
]) {
  assert(frameworkPage.includes(fragment), `framework-plan page must include ${fragment}`)
}

for (const forbidden of [
  /catch\s*\{\s*\}/,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  /mock/i,
  /fallback/i,
  /默认成功/
]) {
  assert(!forbidden.test(procurementApi), `procurement-plan API must not contain ${forbidden}`)
  assert(!forbidden.test(frameworkApi), `framework-plan API must not contain ${forbidden}`)
  assert(!forbidden.test(procurementPage), `procurement-plan page must not contain ${forbidden}`)
  assert(!forbidden.test(frameworkPage), `framework-plan page must not contain ${forbidden}`)
}

for (const fragment of [
  'CREATE TABLE IF NOT EXISTS `srm_procurement_plan`',
  'CREATE TABLE IF NOT EXISTS `srm_procurement_plan_line`',
  'CREATE TABLE IF NOT EXISTS `srm_procurement_approval_record`',
  'CREATE TABLE IF NOT EXISTS `srm_sourcing_project`',
  'CREATE TABLE IF NOT EXISTS `srm_sourcing_project_line`',
  'CREATE TABLE IF NOT EXISTS `srm_framework_plan`',
  'CREATE TABLE IF NOT EXISTS `srm_framework_plan_line`',
  'CREATE TABLE IF NOT EXISTS `srm_framework_agreement`',
  'CREATE TABLE IF NOT EXISTS `srm_framework_agreement_line`',
  "`component` = 'srm/procurement-plan/index'",
  "`component_name` = 'SrmProcurementPlan'",
  "`component` = 'srm/framework-plan/index'",
  "`component_name` = 'SrmFrameworkPlan'",
  "'srm:procurement-plan:generate'",
  "'srm:framework-plan:agreement'",
  'INSERT INTO `srm_code_rule`',
  "'SRM_PROCUREMENT_PLAN'",
  "'SRM_PROCUREMENT_PLAN_LINE'",
  "'SRM_NON_TENDER_PROJECT'",
  "'SRM_TENDER_PROJECT'",
  "'SRM_FRAMEWORK_PLAN'",
  "'SRM_FRAMEWORK_AGREEMENT'",
  'UNIQUE KEY `uk_srm_sourcing_project_tenant_source_plan`',
  'UNIQUE KEY `uk_srm_framework_agreement_tenant_plan`',
  '`system_role_menu`',
  'SIGNAL SQLSTATE \'45000\''
]) {
  assert(sql.includes(fragment), `T2 SQL must include ${fragment}`)
}

for (const fragment of [
  'PROCUREMENT_PLAN_AUDIT_REMARK_REQUIRED',
  'PROCUREMENT_PLAN_GENERATE_NOT_APPROVED',
  'PROCUREMENT_PLAN_GENERATE_DUPLICATE',
  'FRAMEWORK_PLAN_AUDIT_REMARK_REQUIRED',
  'FRAMEWORK_AGREEMENT_NOT_APPROVED',
  'FRAMEWORK_AGREEMENT_DUPLICATE'
]) {
  assert(errorCodes.includes(fragment), `error codes must include ${fragment}`)
}

for (const fragment of [
  'insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, id, SrmProcurementApprovalActionEnum.SUBMIT',
  'insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.APPROVE',
  'insertApprovalRecord(BIZ_TYPE_PROCUREMENT_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.REJECT',
  'SrmProcurementPlanStatusEnum.APPROVED',
  'SrmProcurementPlanStatusEnum.GENERATED',
  'PROCUREMENT_PLAN_GENERATE_DUPLICATE',
  'PROCUREMENT_PLAN_GENERATE_NOT_APPROVED',
  'sourcePlanId',
  'sourcePlanLineId'
]) {
  assert(procurementService.includes(fragment), `procurement service must include ${fragment}`)
}

for (const fragment of [
  'supplierAccessRiskService.checkSupplierEligibility(createReqVO.getSupplierId())',
  'insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, id, SrmProcurementApprovalActionEnum.SUBMIT',
  'insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.APPROVE',
  'insertApprovalRecord(BIZ_TYPE_FRAMEWORK_PLAN, plan.getId(), SrmProcurementApprovalActionEnum.REJECT',
  'SrmFrameworkPlanStatusEnum.APPROVED',
  'SrmFrameworkPlanStatusEnum.AGREEMENT_CREATED',
  'FRAMEWORK_AGREEMENT_DUPLICATE',
  'FRAMEWORK_AGREEMENT_NOT_APPROVED',
  'frameworkPlanLineId'
]) {
  assert(frameworkService.includes(fragment), `framework service must include ${fragment}`)
}

console.log('PASS: SRM D7-3 procurement/framework static contract')
