const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/dhrTemplate.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-dhr-template/DhrTemplatePage.vue')

assert(fs.existsSync(apiPath), 'eDHR DHR模板 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR DHR模板页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

for (const endpoint of [
  '/mes/pro/edhr-dhr-template/catalog/page',
  '/mes/pro/edhr-dhr-template/catalog/create',
  '/mes/pro/edhr-dhr-template/page',
  '/mes/pro/edhr-dhr-template/create',
  '/mes/pro/edhr-dhr-template/integrity-check',
  '/mes/pro/edhr-dhr-template/approve',
  '/mes/pro/edhr-dhr-template/signoff',
  '/mes/pro/edhr-dhr-template/activate',
  '/mes/pro/edhr-dhr-template/retire',
  '/mes/pro/edhr-dhr-template/void',
  '/mes/pro/edhr-dhr-template/impact/page'
]) {
  assert.match(api, new RegExp(endpoint.replaceAll('/', '\\/')), `API 必须声明接口 ${endpoint}`)
}

for (const token of [
  'EdhrDhrCatalogRespVO',
  'EdhrDhrTemplateRespVO',
  'EdhrDhrTemplateVersionRespVO',
  'EdhrDhrTemplateBindingRespVO',
  'EdhrDhrTemplateImpactRespVO',
  "status?: 'DRAFT' | 'PRECHECK_FAILED' | 'PENDING_REVIEW' | 'APPROVED' | 'SIGNOFF_PENDING' | 'EFFECTIVE' | 'SUSPENDED' | 'RETIRED' | 'OBSOLETE'",
  'reviewStatus',
  'signoffStatus',
  'bindingCount',
  'integrityIssueCount',
  'signoffEvidenceHash',
  'impactScopeJson',
  'runIntegrityCheck',
  'approveTemplate',
  'signoffTemplate',
  'activateTemplate',
  'retireTemplate',
  'voidTemplate'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API 类型和方法缺少 ${token}`)
}

for (const label of [
  'DHR目录',
  'DHR模板',
  '模板版本',
  '模板编码',
  '模板名称',
  '绑定产品',
  '绑定路线',
  '绑定工序',
  '批次类型',
  '完整性问题',
  '审核状态',
  '签核状态',
  '生效',
  '停用',
  '作废',
  '影响范围',
  '确认影响'
]) {
  assert.ok(page.includes(label), `页面必须呈现 DHR 模板生命周期字段：${label}`)
}

for (const token of [
  'catalogList',
  'templateList',
  'impactList',
  'templateDialogVisible',
  'impactDialogVisible',
  'loadCatalogList',
  'loadTemplateList',
  'loadImpactList',
  'submitTemplate',
  'runIntegrityCheck',
  'approveTemplate',
  'signoffTemplate',
  'activateTemplate',
  'openImpactDialog',
  'submitRetireTemplate',
  'submitVoidTemplate',
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:create']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:check']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:approve']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:signoff']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:activate']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:retire']\"",
  "v-hasPermi=\"['mes:pro-edhr-dhr-template:void']\""
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面交互契约缺少 ${token}`)
}

for (const token of [
  'loadError',
  'templateError',
  'impactError',
  'message.error(resolveErrorMessage',
  '<el-alert v-if="loadError"',
  '<el-alert v-if="templateError"',
  '<el-alert v-if="impactError"'
]) {
  assert.match(page, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `页面必须暴露后端失败原因：${token}`)
}

for (const forbidden of [
  'mock',
  'fixture',
  'demo',
  'DEFAULT_SUCCESS',
  'MOCK_SIGNOFF',
  'silent',
  'catch {}',
  'catch{}'
]) {
  assert.doesNotMatch(api, new RegExp(forbidden, 'i'), `API 不得伪造 DHR 模板生命周期：${forbidden}`)
  assert.doesNotMatch(page, new RegExp(forbidden, 'i'), `页面不得伪造 DHR 模板生命周期：${forbidden}`)
}

console.log('PASS: eDHR DHR template lifecycle static contract')
