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

const apiSource = read('src/api/mes/pro/edhr/oqPq.ts')
const pageSource = read('src/views/mes/pro/edhr-oq-pq/OqPqPage.vue')
const combinedSource = `${apiSource}\n${pageSource}`

for (const endpoint of [
  '/mes/pro/edhr-oq-pq/case/page',
  '/mes/pro/edhr-oq-pq/case/create',
  '/mes/pro/edhr-oq-pq/run/page',
  '/mes/pro/edhr-oq-pq/run/create',
  '/mes/pro/edhr-oq-pq/run/submit-step',
  '/mes/pro/edhr-oq-pq/run/complete',
  '/mes/pro/edhr-oq-pq/deviation/page',
  '/mes/pro/edhr-oq-pq/deviation/remediate',
  '/mes/pro/edhr-oq-pq/deviation/retest',
  '/mes/pro/edhr-oq-pq/deviation/close'
]) {
  assertIncludes(apiSource, endpoint, `OQ/PQ API must keep endpoint ${endpoint}`)
}

for (const permission of [
  'mes:pro-edhr-oq-pq:query',
  'mes:pro-edhr-oq-pq:create',
  'mes:pro-edhr-oq-pq:execute',
  'mes:pro-edhr-oq-pq:retest',
  'mes:pro-edhr-oq-pq:close'
]) {
  assertIncludes(apiSource, permission, `OQ/PQ API must export permission ${permission}`)
  assertIncludes(pageSource, permission, `OQ/PQ page must gate action with ${permission}`)
}

for (const contract of [
  'EdhrOqPqCaseCreateReqVO',
  'EdhrOqPqCaseRespVO',
  'EdhrOqPqRunCreateReqVO',
  'EdhrOqPqRunRespVO',
  'EdhrOqPqStepSubmitReqVO',
  'EdhrOqPqStepResultRespVO',
  'EdhrOqPqDeviationRespVO',
  'EdhrOqPqDeviationRemediateReqVO',
  'EdhrOqPqDeviationRetestReqVO',
  'EdhrOqPqDeviationCloseReqVO',
  'getEdhrOqPqCasePage',
  'createEdhrOqPqCase',
  'getEdhrOqPqRunPage',
  'createEdhrOqPqRun',
  'submitEdhrOqPqStepResult',
  'completeEdhrOqPqRun',
  'getEdhrOqPqDeviationPage',
  'remediateEdhrOqPqDeviation',
  'retestEdhrOqPqDeviation',
  'closeEdhrOqPqDeviation'
]) {
  assertIncludes(apiSource, contract, `OQ/PQ API must expose ${contract}`)
}

for (const copy of [
  "defineOptions({ name: 'MesProEdhrOqPq' })",
  'OQ/PQ执行台',
  '验证包',
  '用例',
  '执行记录',
  '执行环境',
  '发布标签',
  'schema版本',
  '真实业务路径',
  '真实测试数据来源',
  '目标环境证明',
  '步骤结果',
  '失败项',
  '偏差',
  '整改措施',
  '复测结果',
  '复核人',
  '关闭签核',
  '开放偏差',
  '下一步动作',
  '阻断'
]) {
  assertIncludes(pageSource, copy, `OQ/PQ page must expose ${copy}`)
}

for (const behavior of [
  'resolveErrorMessage',
  'loadError',
  '<el-alert',
  'handleCreateCase',
  'handleCreateRun',
  'handleSubmitFailedStep',
  'handleCompleteRun',
  'handleRemediateDeviation',
  'handleRetestDeviation',
  'handleCloseDeviation',
  'syncRunListRow',
  'loadDeviationList',
  'selectedRun',
  'openDeviationCount',
  'realTestDataSource'
]) {
  assertIncludes(pageSource, behavior, `OQ/PQ page must implement visible behavior ${behavior}`)
}

assertIncludes(pageSource, 'empty-text="请选择验证包后查看OQ/PQ用例"', 'case table must not show silent success before package selection')
assertIncludes(pageSource, 'empty-text="请选择执行记录后查看偏差"', 'deviation table must not show silent success before run selection')
assertIncludes(pageSource, 'ElMessage.error(resolveErrorMessage(error))', 'frontend must show backend gate failures')

for (const forbidden of [
  'VALIDATION_SIGNED',
  'trainingConfirm',
  'deliveryLicense',
  'backupRehearsalPassed',
  'DEFAULT_SUCCESS',
  'mockDeviation'
]) {
  assertNotIncludes(combinedSource, forbidden, `OQ/PQ slice must not implement non-goal or fallback ${forbidden}`)
}

assert(
  !/catch\s*\(\s*[^)]*\s*\)\s*\{\s*\}/.test(pageSource),
  'OQ/PQ page must not silently swallow frontend errors'
)

console.log('PASS: eDHR OQ/PQ static contract')
