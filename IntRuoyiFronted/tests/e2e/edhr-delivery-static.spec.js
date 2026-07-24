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

const apiSource = read('src/api/mes/pro/edhr/delivery.ts')
const pageSource = read('src/views/mes/pro/edhr-delivery/DeliveryPage.vue')
const combinedSource = `${apiSource}\n${pageSource}`

for (const endpoint of [
  '/mes/pro/edhr-delivery-cockpit/project/page',
  '/mes/pro/edhr-delivery-cockpit/project/create',
  '/mes/pro/edhr-delivery-cockpit/project/detail',
  '/mes/pro/edhr-delivery-cockpit/evidence-package/page',
  '/mes/pro/edhr-delivery-cockpit/gate-summary'
]) {
  assertIncludes(apiSource, endpoint, `delivery API must keep endpoint ${endpoint}`)
}

for (const permission of ['mes:pro-edhr-delivery:query', 'mes:pro-edhr-delivery:create']) {
  assertIncludes(apiSource, permission, `delivery API must export permission ${permission}`)
  assertIncludes(pageSource, permission, `delivery page must gate action with ${permission}`)
}

for (const contract of [
  'EdhrDeliveryProjectCreateReqVO',
  'EdhrDeliveryProjectRespVO',
  'EdhrEvidencePackageRespVO',
  'EdhrDeliveryGateSummaryRespVO',
  'getEdhrDeliveryProjectPage',
  'createEdhrDeliveryProject',
  'getEdhrDeliveryProjectDetail',
  'getEdhrEvidencePackagePage',
  'getEdhrDeliveryGateSummary'
]) {
  assertIncludes(apiSource, contract, `delivery API must expose ${contract}`)
}

for (const copy of [
  "defineOptions({ name: 'MesProEdhrDelivery' })",
  '交付驾驶舱',
  '交付项目',
  '证据包',
  '缺失证据',
  '责任人',
  '下一步动作',
  '签核影响',
  '不允许签核',
  '恢复演练',
  '培训覆盖',
  '发布标签',
  'schema版本'
]) {
  assertIncludes(pageSource, copy, `delivery page must expose ${copy}`)
}

for (const behavior of [
  'resolveErrorMessage',
  'loadError',
  '<el-alert',
  'handleCreateProject',
  'handleSelectProject',
  'getPackageList',
  'getGateSummary',
  'signoffAllowed',
  'missingEvidenceJson',
  'signoffImpact',
  'nextAction'
]) {
  assertIncludes(pageSource, behavior, `delivery page must implement visible behavior ${behavior}`)
}

assertIncludes(pageSource, 'empty-text="请选择交付项目后查看证据包"', 'package table must not show silent success before project selection')
assertIncludes(pageSource, 'empty-text="请选择交付项目后查看门禁项"', 'gate table must not show silent success before project selection')

for (const forbidden of [
  '商业化交付完成',
  'signoffComplete',
  'backupRestoreExecute',
  'oqPqExecutor',
  'trainingSignoffApprove',
  'deploymentAuthorizeApprove',
  'interfaceSyncJob'
]) {
  assertNotIncludes(combinedSource, forbidden, `first delivery slice must not implement non-goal ${forbidden}`)
}

assert(
  !/catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/.test(pageSource),
  'delivery page must not silently swallow frontend errors'
)

console.log('PASS: eDHR delivery cockpit static contract')
