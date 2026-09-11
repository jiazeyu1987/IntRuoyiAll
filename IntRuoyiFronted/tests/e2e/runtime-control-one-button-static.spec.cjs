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

for (const label of ['生成程序安装包', '发布测试服', '晋级正式服', '程序包（不含数据）']) {
  assert.match(page, new RegExp(label), `one-button release UI must render ${label}`)
}
assert.match(source, /createRuntimeControlReleaseWorkflow/)
assert.match(source, /getRuntimeControlReleaseWorkflows/)
assert.match(source, /publishRuntimeControlReleaseWorkflowToTest/)
assert.match(source, /acceptRuntimeControlReleaseWorkflowTest/)
assert.match(source, /authorizeRuntimeControlReleaseWorkflowProduction/)
assert.match(source, /promoteRuntimeControlReleaseWorkflowProduction/)
assert.match(source, /\/infra\/runtime-control\/release-workflows/)
assert.doesNotMatch(
  page.match(/const operationActions\s*=\s*\[([\s\S]*?)\n\]/)?.[1] || '',
  /build-release|publish-test|mark-release-tested|promote-prod/,
  'legacy release operations must not remain in the visible generic toolbar'
)

console.log('runtime-control one-button P1 static contract passed')
