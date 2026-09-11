const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const apiPath = path.join(frontendRoot, 'src', 'api', 'infra', 'runtimeControl', 'index.ts')
const source = fs.readFileSync(apiPath, 'utf8')

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

console.log('runtime-control one-button P1 static contract passed')
