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

const accessApi = readFrontend('src/api/srm/supplier-access/index.ts')
const riskApi = readFrontend('src/api/srm/supplier-risk/index.ts')
const accessPage = readFrontend('src/views/srm/supplier-access/index.vue')
const riskPage = readFrontend('src/views/srm/supplier-risk/index.vue')
const sql = readBackend('sql/mysql/20260619_srm_d7_2_supplier_access_risk.sql')
const service = readBackend(
  'yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/service/supplier/SrmSupplierAccessRiskServiceImpl.java'
)
const errorCodes = readBackend(
  'yudao-module-srm/src/main/java/cn/iocoder/yudao/module/srm/enums/ErrorCodeConstants.java'
)

for (const fragment of [
  '/srm/supplier-access/page',
  '/srm/supplier-access/create',
  '/srm/supplier-access/update',
  '/srm/supplier-access/approve',
  '/srm/supplier-access/reject',
  '/srm/supplier-access/enable',
  '/srm/supplier-access/check',
  '/srm/supplier-access/reference-suppliers',
  'srmSupplierAccessStatusOptions',
  'PENDING',
  'APPROVED',
  'REJECTED'
]) {
  assert(accessApi.includes(fragment), `supplier-access API must include ${fragment}`)
}

for (const fragment of [
  '/srm/supplier-risk/page',
  '/srm/supplier-risk/create',
  '/srm/supplier-risk/resolve',
  'srmSupplierRiskLevelOptions',
  'srmSupplierRiskStatusOptions',
  'srmSupplierRiskSourceTypeOptions',
  'ACCESS_REQUEST',
  'PROCUREMENT_CONTRACT'
]) {
  assert(riskApi.includes(fragment), `supplier-risk API must include ${fragment}`)
}

for (const fragment of [
  "defineOptions({ name: 'SrmSupplierAccess' })",
  "v-hasPermi=\"['srm:supplier-access:create']\"",
  "v-hasPermi=\"['srm:supplier-access:audit']\"",
  "v-hasPermi=\"['srm:supplier-access:enable']\"",
  "v-hasPermi=\"['srm:supplier-access:check']\"",
  '新增准入',
  '资格校验',
  '供应商准入',
  'ERP供应商',
  '未处理高风险',
  'message.error'
]) {
  assert(accessPage.includes(fragment), `supplier-access page must include ${fragment}`)
}

for (const fragment of [
  "defineOptions({ name: 'SrmSupplierRisk' })",
  "v-hasPermi=\"['srm:supplier-risk:create']\"",
  "v-hasPermi=\"['srm:supplier-risk:resolve']\"",
  '新增风险',
  '处理供应商风险',
  '来源类型',
  '风险描述',
  '风险总数',
  'message.error'
]) {
  assert(riskPage.includes(fragment), `supplier-risk page must include ${fragment}`)
}

for (const forbidden of [
  /catch\s*\{\s*\}/,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  /console\.log\([^)]*error/i
]) {
  assert(!forbidden.test(accessPage), `supplier-access page must not contain ${forbidden}`)
  assert(!forbidden.test(riskPage), `supplier-risk page must not contain ${forbidden}`)
}

for (const fragment of [
  'CREATE TABLE IF NOT EXISTS `srm_supplier_access`',
  'CREATE TABLE IF NOT EXISTS `srm_supplier_risk`',
  "`component` = 'srm/supplier-access/index'",
  "`component_name` = 'SrmSupplierAccess'",
  "`component` = 'srm/supplier-risk/index'",
  "`component_name` = 'SrmSupplierRisk'",
  "'srm:supplier-access:check'",
  "'srm:supplier-risk:resolve'",
  '`system_role_menu`',
  'SIGNAL SQLSTATE \'45000\''
]) {
  assert(sql.includes(fragment), `supplier access/risk SQL must include ${fragment}`)
}

for (const fragment of [
  'SUPPLIER_REFERENCE_CROSS_TENANT',
  'SUPPLIER_ACCESS_SELF_AUDIT_FORBIDDEN',
  'SUPPLIER_ACCESS_APPROVE_HIGH_RISK_BLOCKED',
  'SUPPLIER_ACCESS_DISABLE_REASON_REQUIRED'
]) {
  assert(errorCodes.includes(fragment), `error codes must include ${fragment}`)
}

for (const fragment of [
  'validateNotSelfAudit(access, userId);',
  'supplierRiskMapper.selectOpenHighRiskListBySupplierId(getRequiredTenantId(), access.getSupplierId())',
  'SUPPLIER_REFERENCE_CROSS_TENANT',
  'filter(supplier -> Objects.equals(supplier.getTenantId(), tenantId))'
]) {
  assert(service.includes(fragment), `supplier service must include ${fragment}`)
}

console.log('PASS: SRM D7-2 supplier-access-risk static contract')
