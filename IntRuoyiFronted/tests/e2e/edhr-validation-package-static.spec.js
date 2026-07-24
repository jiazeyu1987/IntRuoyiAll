const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (relativePath) => {
  const absolutePath = path.resolve(root, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (source, fragment, message) => {
  assert(source.includes(fragment), message)
}

const assertNotIncludes = (source, fragment, message) => {
  assert(!source.includes(fragment), message)
}

const apiSource = read('src/api/mes/pro/edhr/validation.ts')
const pageSource = read('src/views/mes/pro/edhr-validation/ValidationPage.vue')
const combinedSource = `${apiSource}\n${pageSource}`

for (const endpoint of [
  '/mes/pro/edhr-validation-package/page',
  '/mes/pro/edhr-validation-package/create',
  '/mes/pro/edhr-validation-package/detail',
  '/mes/pro/edhr-validation-package/evaluate-trace',
  '/mes/pro/edhr-validation-requirement-item/page',
  '/mes/pro/edhr-validation-requirement-item/create',
  '/mes/pro/edhr-validation-trace-link/create'
]) {
  assertIncludes(apiSource, endpoint, `validation API must keep endpoint ${endpoint}`)
}

for (const permission of [
  'mes:pro-edhr-validation:query',
  'mes:pro-edhr-validation:create',
  'mes:pro-edhr-validation:evaluate-trace'
]) {
  assertIncludes(apiSource, permission, `validation API must export permission ${permission}`)
  assertIncludes(pageSource, permission, `validation page must gate action with ${permission}`)
}

for (const contract of [
  'EdhrValidationPackageCreateReqVO',
  'EdhrValidationPackageRespVO',
  'EdhrValidationRequirementItemCreateReqVO',
  'EdhrValidationRequirementItemRespVO',
  'EdhrValidationTraceLinkCreateReqVO',
  'EdhrValidationTraceEvaluateRespVO',
  'getEdhrValidationPackagePage',
  'createEdhrValidationPackage',
  'getEdhrValidationPackageDetail',
  'evaluateEdhrValidationTrace',
  'getEdhrValidationRequirementItemPage',
  'createEdhrValidationRequirementItem',
  'createEdhrValidationTraceLink'
]) {
  assertIncludes(apiSource, contract, `validation API must expose ${contract}`)
}

for (const copy of [
  "defineOptions({ name: 'MesProEdhrValidation' })",
  '验证包',
  'CSV基础信息',
  '客户项目',
  '发布标签',
  'schema版本',
  '目标环境',
  'URS',
  'FRS',
  '风险',
  'IQ',
  'OQ',
  'PQ',
  '追溯矩阵',
  'OQ Ready',
  '断裂明细',
  '责任人',
  '下一步动作',
  '阻塞'
]) {
  assertIncludes(pageSource, copy, `validation page must expose ${copy}`)
}

for (const behavior of [
  'resolveErrorMessage',
  'loadError',
  '<el-alert',
  "validationStatus: ''",
  'handleCreatePackage',
  'handleCreateItem',
  'handleCreateTraceLink',
  'handleEvaluateTrace',
  'syncPackageListRow',
  'getItemList',
  'brokenItems',
  'traceSummary',
  'oqReady',
  'nextAction'
]) {
  assertIncludes(pageSource, behavior, `validation page must implement visible behavior ${behavior}`)
}

assertIncludes(pageSource, 'empty-text="请选择验证包后查看条目"', 'item table must not show silent success before package selection')
assertIncludes(pageSource, 'empty-text="请选择验证包并评估追溯门禁"', 'trace issue table must not show silent success before gate evaluation')
assertIncludes(
  pageSource,
  'packageQueryParams.validationStatus = \'\'',
  'reset query must not keep the package list filtered to BLOCKED after OQ Ready'
)
assertIncludes(
  pageSource,
  'syncPackageListRow(latestPackage)',
  'evaluating trace must synchronize the package list row status'
)

for (const forbidden of [
  'VALIDATION_STATUS_PASSED',
  'signoffComplete',
  'customerCsvExport',
  'oqExecutionStart',
  'pqExecutionStart',
  'deviationCloseApprove'
]) {
  assertNotIncludes(combinedSource, forbidden, `validation first trace slice must not implement non-goal ${forbidden}`)
}

assert(
  !/catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/.test(pageSource),
  'validation page must not silently swallow frontend errors'
)

console.log('PASS: eDHR validation package static contract')
