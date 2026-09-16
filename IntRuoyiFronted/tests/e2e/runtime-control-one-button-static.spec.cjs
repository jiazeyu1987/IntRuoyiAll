const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const apiPath = path.join(frontendRoot, 'src', 'api', 'infra', 'runtimeControl', 'index.ts')
const source = fs.readFileSync(apiPath, 'utf8')
const pagePath = path.join(frontendRoot, 'src', 'views', 'infra', 'runtime-control', 'index.vue')
const page = fs.readFileSync(pagePath, 'utf8')

const createRequest = source.match(
  /export interface RuntimeControlReleaseWorkflowCreateReqVO\s*\{([\s\S]*?)\n\}/
)
assert.ok(createRequest, 'P1 must define the server-owned release workflow create request')
assert.match(createRequest[1], /\breason\s*:\s*string/)
assert.match(createRequest[1], /\bsourceSelectionId\s*:\s*string/)

for (const forbidden of [
  'releaseTag',
  'publishScope',
  'host',
  'nasConfigPath',
  'remoteMinioContainer',
  'backendRepoRoot',
  'frontendRepoRoot',
  'localCacheRoot'
]) {
  assert.doesNotMatch(createRequest[1], new RegExp(`\\b${forbidden}\\b`), forbidden)
}

assert.match(source, /export type RuntimeControlAppReleaseScope\s*=\s*'app-release'/)
assert.doesNotMatch(source, /RuntimeControlAppReleaseScope\s*=.*with-data/)
assert.doesNotMatch(source, /'code-only'\s*\|\s*'with-data'/)
assert.doesNotMatch(page, /code-only|with-data/)

for (const label of ['生成程序安装包', '发布测试服', '晋级正式服', '程序包（不含数据）']) {
  assert.match(page, new RegExp(label), `one-button release UI must render ${label}`)
}
assert.match(source, /createRuntimeControlReleaseWorkflow/)
assert.match(source, /getRuntimeControlReleaseWorkflows/)
assert.match(source, /publishRuntimeControlReleaseWorkflowToTest/)
assert.match(source, /acceptRuntimeControlReleaseWorkflowTest/)
assert.match(source, /export type RuntimeControlReleaseWorkflowTestAcceptanceResult\s*=\s*'PASS'\s*\|\s*'FAIL'/)
assert.match(source, /RuntimeControlReleaseWorkflowTestAcceptanceReqVO\s*\{[\s\S]*result\s*:\s*RuntimeControlReleaseWorkflowTestAcceptanceResult[\s\S]*conclusion\s*:\s*string[\s\S]*\}/)
assert.match(
  source,
  /RuntimeControlActionReqVO\s*\{[\s\S]*testResult\?\s*:\s*'PASS'[\s\S]*testConclusion\?\s*:\s*string/,
  'legacy mark-tested operation request must carry the structured PASS result'
)
assert.match(source, /authorizeRuntimeControlReleaseWorkflowProduction/)
assert.match(source, /promoteRuntimeControlReleaseWorkflowProduction/)
assert.match(source, /\/infra\/runtime-control\/release-workflows/)
assert.doesNotMatch(
  page.match(/const operationActions\s*=\s*\[([\s\S]*?)\n\]/)?.[1] || '',
  /build-release|publish-test|mark-release-tested|promote-prod/,
  'legacy release operations must not remain in the visible generic toolbar'
)
assert.doesNotMatch(
  page,
  /releaseWorkflows\.value\s*\[\s*0\s*\]/,
  'one-button release UI must not operate whichever workflow happens to sort first'
)
assert.match(
  page,
  /selectedReleaseWorkflowId/,
  'one-button release UI must persist an explicit operator-selected workflowId'
)
assert.match(
  page,
  /<el-select[\s\S]*selectedReleaseWorkflowId/,
  'one-button release UI must expose a workflow/package selector'
)
assert.match(
  page,
  /acceptReleaseWorkflowTest\('PASS'\)/,
  'one-button release UI must expose an explicit PASS acceptance action'
)
assert.match(
  page,
  /acceptReleaseWorkflowTest\('FAIL'\)/,
  'one-button release UI must expose an explicit FAIL acceptance action'
)
assert.match(
  page,
  /result,\s*conclusion:\s*promptResult\.value\.trim\(\)/,
  'test acceptance payload must submit structured result and typed conclusion'
)
assert.match(
  page,
  /testResult:\s*operationDialog\.action === 'mark-release-tested' \? 'PASS' : undefined/,
  'legacy mark-tested operation payload must submit PASS explicitly'
)
assert.doesNotMatch(
  page,
  /const conclusion = releaseWorkflowReason\.value\.trim\(\)[\s\S]*acceptRuntimeControlReleaseWorkflowTest/,
  'test acceptance must not reuse the generic release reason as the validation conclusion'
)

console.log('runtime-control one-button P1 static contract passed')
