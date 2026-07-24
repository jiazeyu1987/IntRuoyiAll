const fs = require('fs')
const path = require('path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  assert.ok(source.includes(expected), `missing ${label}: ${expected}`)
}

function assertNotContains(source, forbidden, label) {
  assert.equal(source.includes(forbidden), false, `forbidden ${label}: ${forbidden}`)
}

const scriptPath = 'tests/e2e/runtime-control-promote-backup-real-flow.e2e.js'
assert.ok(fs.existsSync(path.join(repoRoot, scriptPath)), `missing ${scriptPath}`)

const script = readUtf8(scriptPath)
const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const foolproofStatic = readUtf8('tests/e2e/runtime-control-foolproof-static.spec.js')

for (const field of [
  'testedRecoverySetCandidateId?: string',
  'testedRecoverySetId?: string',
  'testedRecoverySetManifestHash?: string'
]) {
  assertContains(api, field, `release package API recovery binding field ${field}`)
}

assertContains(
  script,
  'getRuntimeControlActionOrigin',
  'explicit runtime-control backend action origin'
)
assertContains(
  script,
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1',
  'explicit backup promotion approval guard'
)
assertContains(
  script,
  'RUNTIME_CONTROL_BACKUP_BACKEND_HEALTH_URL is required',
  'explicit backup backend health URL'
)
assertContains(
  script,
  'RUNTIME_CONTROL_BACKUP_FRONTEND_URL is required',
  'explicit backup frontend URL'
)
assertContains(
  script,
  'RUNTIME_CONTROL_BACKUP_WEBSITE_URL is required',
  'explicit backup website URL'
)
assertContains(
  script,
  'RUNTIME_CONTROL_BACKUP_SHOWROOM_URL is required',
  'explicit backup showroom URL'
)
assertContains(
  script,
  'RUNTIME_CONTROL_BACKUP_DCC_READBACK_URL is required',
  'explicit backup DCC readback URL'
)
assertContains(script, "operation.action, 'promote-backup'", 'submitted promote-backup action')
assertContains(script, 'testedRecoverySetCandidateId', 'release package tested recovery candidate proof')
assertContains(script, 'testedRecoverySetManifestHash', 'release package tested recovery hash proof')
assertContains(script, 'BACKUP_HEALTH_OK ${url}', 'backup health evidence output')
assertContains(script, 'DCC_READBACK_OK', 'DCC readback evidence output')
assertContains(script, "window.localStorage.getItem('ACCESS_TOKEN')", 'browser access token lookup')
assertContains(script, "window.localStorage.getItem('tenantId')", 'browser tenant id lookup')
assertContains(script, 'headers.Authorization = `Bearer ${accessToken}`', 'authenticated DCC readback authorization header')
assertContains(script, "headers['tenant-id'] = String(tenantId)", 'authenticated DCC readback tenant header')
assertContains(script, "headers['visit-tenant-id'] = String(visitTenantId)", 'authenticated DCC readback visit tenant header')
assertContains(script, 'DCC readback verification requires ACCESS_TOKEN from browser storage', 'access token fail-fast gate')
assertContains(script, 'DCC readback verification requires tenant-id from browser storage', 'tenant fail-fast gate')
assertContains(script, 'application/json', 'json failure-envelope guard')
assertContains(script, 'PROMOTE_BACKUP_SUCCEEDED', 'backup promotion operation success marker')
assertContains(
  script,
  'PASS: runtime control real promote-backup flow',
  'real promote-backup completion marker'
)

for (const forbidden of [
  "process.env.RUNTIME_CONTROL_E2E_BASE_URL ||",
  "process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN ||",
  '172.30.30.59',
  'http://127.0.0.1:48081',
  'http://localhost:8081'
]) {
  assertNotContains(script, forbidden, `implicit target fallback ${forbidden}`)
}

assertContains(
  foolproofStatic,
  "readUtf8('tests/e2e/runtime-control-promote-backup-real-flow.e2e.js')",
  'foolproof static coverage for promote-backup real flow'
)
assertContains(
  foolproofStatic,
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1',
  'foolproof static backup promotion approval guard'
)

console.log('PASS: runtime-control promote-backup real-flow static contract is wired')
