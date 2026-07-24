const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')

function read(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

const packageJson = JSON.parse(read('package.json'))
const flowScript = read('tests/e2e/mes-pro-route-version-real-flow.e2e.js')

assert.equal(
  packageJson.scripts['e2e:controlled-content:mes-route-version:real'],
  'node tests/e2e/mes-pro-route-version-real-flow.e2e.js',
  'MES route version controlled-content real flow must have a stable package entry.'
)

for (const marker of [
  'controlled-content-mes-route-version-full-flow-real.json',
  'executionMode',
  'playwright-ui',
  'writeChannel',
  'frontend-ui',
  'directApiWrites',
  'sqlBusinessDataWritePerformed',
  'mockDataUsed',
  'writeRequests',
  'finalAssertions',
  'domain',
  'MES_ROUTE'
]) {
  assert.match(flowScript, new RegExp(escapeRegExp(marker)), `${marker} must be written into release-gate evidence.`)
}

for (const marker of [
  'withdrawCandidateThroughWorkspace',
  'verifyDraftCanEnterEditor',
  'editedAfterWithdraw',
  'resubmitAfterWithdraw',
  'ROUTE_VERSION_E2E_APPROVER_USERNAME',
  'ROUTE_VERSION_E2E_APPROVER_PASSWORD',
  'ROUTE_VERSION_E2E_APPROVER_USER_ID',
  'approvalAssigneeUserId',
  'switchUser(page, config.approver',
  'configured approver must match actual approval assignee',
  'oldActiveStatus',
  'newActiveStatus',
  'SUPERSEDED',
  'ACTIVE'
]) {
  assert.match(flowScript, new RegExp(escapeRegExp(marker)), `${marker} must be covered by the MES controlled-content flow.`)
}

assert.doesNotMatch(
  flowScript,
  /ROUTE_VERSION_E2E_WORD_SAMPLE|wordSample/,
  'MES route version flow must not require unused Word sample input.'
)

console.log('mes-pro-route-version-real-flow-contract-static PASS')
