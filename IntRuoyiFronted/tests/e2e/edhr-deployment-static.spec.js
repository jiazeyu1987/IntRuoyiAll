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

const apiSource = read('src/api/mes/pro/edhr/deployment.ts')
const pageSource = read('src/views/mes/pro/edhr-deployment/DeploymentPage.vue')
const combinedSource = `${apiSource}\n${pageSource}`

for (const endpoint of [
  '/mes/pro/edhr-deployment/page',
  '/mes/pro/edhr-deployment/create',
  '/mes/pro/edhr-deployment/detail',
  '/mes/pro/edhr-deployment/update-evidence',
  '/mes/pro/edhr-deployment/precheck'
]) {
  assertIncludes(apiSource, endpoint, `deployment API must keep endpoint ${endpoint}`)
}

for (const permission of [
  'mes:pro-edhr-deployment:query',
  'mes:pro-edhr-deployment:create',
  'mes:pro-edhr-deployment:update',
  'mes:pro-edhr-deployment:precheck'
]) {
  assertIncludes(apiSource, permission, `deployment API must export permission ${permission}`)
  assertIncludes(pageSource, permission, `deployment page must gate action with ${permission}`)
}

for (const contract of [
  'EdhrDeploymentCreateReqVO',
  'EdhrDeploymentPageReqVO',
  'EdhrDeploymentUpdateReqVO',
  'EdhrDeploymentRespVO',
  'EdhrDeploymentGateItemRespVO',
  'EdhrDeploymentPrecheckRespVO',
  'getEdhrDeploymentPage',
  'createEdhrDeploymentEvidence',
  'getEdhrDeploymentDetail',
  'updateEdhrDeploymentEvidence',
  'precheckEdhrDeploymentEvidence'
]) {
  assertIncludes(apiSource, contract, `deployment API must expose ${contract}`)
}

for (const updateField of [
  'targetEnvironment',
  'environmentAuthorized',
  'environmentCheckSummary',
  'serverSummary',
  'networkSummary',
  'objectStorageSummary',
  'capacitySummary',
  'permissionSummary',
  'releaseTag',
  'artifactVersion',
  'artifactChecksum',
  'schemaVersion',
  'migrationManifest',
  'requiredSqlManifest',
  'appImportResult'
]) {
  assertIncludes(apiSource, updateField, `deployment update API must accept deployment evidence ${updateField}`)
  assertIncludes(pageSource, `updateForm.${updateField}`, `deployment update dialog must bind ${updateField}`)
}

for (const copy of [
  "defineOptions({ name: 'MesProEdhrDeployment' })",
  '部署交付',
  '环境检查',
  '环境授权',
  '服务器',
  '网络',
  '对象存储',
  '容量',
  '权限',
  '安装包版本',
  'releaseTag',
  'schema版本',
  '迁移清单',
  'required SQL',
  '应用导入',
  '授权许可',
  '授权范围',
  '有效期',
  '授权文件',
  '接口范围',
  '接口版本',
  '联调环境',
  '真实请求',
  '真实响应',
  '失败整改',
  '复测证据',
  '门禁预检',
  '缺失证据',
  '责任人',
  '下一步动作',
  '签核影响',
  '阻断',
  'INTEGRATED'
]) {
  assertIncludes(pageSource, copy, `deployment page must expose ${copy}`)
}

for (const behavior of [
  'resolveErrorMessage',
  'loadError',
  '<el-alert',
  'handleCreateEvidence',
  'handleUpdateEvidence',
  'handlePrecheckEvidence',
  'selectedEvidence',
  'gateItemList',
  'gatePassed',
  'blockedReason',
  'responseEvidence'
]) {
  assertIncludes(pageSource, behavior, `deployment page must implement visible behavior ${behavior}`)
}

assertIncludes(pageSource, 'empty-text="暂无部署交付记录"', 'deployment table must not show silent success before data exists')
assertIncludes(pageSource, 'empty-text="请选择部署记录后查看门禁项"', 'gate table must not show silent success before selection')
assertIncludes(pageSource, 'ElMessage.error(resolveErrorMessage(error))', 'frontend must show backend gate failures')

for (const forbidden of [
  'backupRehearsalPassed',
  'deliverySignoffComplete',
  'validationSigned',
  'DEFAULT_SUCCESS',
  'mockLicense',
  'mockInterfaceResponse'
]) {
  assertNotIncludes(combinedSource, forbidden, `deployment slice must not implement non-goal or fallback ${forbidden}`)
}

assert(
  !/catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/.test(pageSource),
  'deployment page must not silently swallow frontend errors'
)

console.log('PASS: eDHR deployment static contract')
