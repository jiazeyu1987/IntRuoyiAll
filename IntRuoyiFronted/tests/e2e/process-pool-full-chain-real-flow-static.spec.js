const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const realFlowPath = path.join(
  frontendRoot,
  'tests/e2e/process-pool-full-chain-real-flow.e2e.js'
)

assert.ok(
  fs.existsSync(realFlowPath),
  `missing full-chain real Playwright runner: ${realFlowPath}`
)

const source = fs.readFileSync(realFlowPath, 'utf8').replace(/\r\n/g, '\n')

for (const fragment of [
  'assertAllowedRuntimePair',
  'readFrontendEnvValue',
  'VITE_APP_DEFAULT_LOGIN_PASSWORD',
  'codexedhrcell01',
  'int-ruoyi-mysql',
  'prepareFixtures',
  'restoreSignatureAuthorization',
  'cleanupFixtures',
  'INSERT INTO system_post',
  '@operator_post_id',
  'operatorPostId',
  'postCount',
  'DELETE FROM system_post',
  'const markerSql = sqlString(runMarker)',
  'const markerPrefixSql = sqlString(`${runMarker}%`)',
  'const markerContainsSql = sqlString(`%${runMarker}%`)',
  '/mes/pro/feedback/edhr-batch-production-fill',
  '/mes/pro/feedback/edhr-batch-pqc-fill',
  '/mes/pro/process-pool/fifo-orchestration',
  '/mes/pro/process-pool/review-copy',
  '/mes/pro/process-pool/team-leader-workbench',
  '/admin-api/mes/pro/feedback/frontline/submit',
  '/admin-api/mes/pro/process-pool/fifo-orchestration/allocate-available-output',
  '/admin-api/mes/pro/process-pool/review-copy/generate-submit-from-rules',
  '/admin-api/mes/pro/process-pool/team-leader-workbench/page',
  '/admin-api/mes/pro/process-pool/team-leader-workbench/detail',
  'previousProcessInputQuantity: 60',
  'outputQuantity: 50',
  'lossQuantity: 10',
  'earlyDemandQuantity: 20',
  'lateDemandQuantity: 30',
  'OUTPUT_QUANTITY',
  'correctedValue',
  'actualEmployeeUserName',
  'fifoAllocationStatus',
  'writeEvidence',
  'page.screenshot',
  'finally'
]) {
  assert.ok(source.includes(fragment), `full-chain E2E runner must include: ${fragment}`)
}

assert.match(
  source,
  /VITE_APP_DEFAULT_LOGIN_PASSWORD\s*\\s\*=\s*\\s\*/,
  'frontend env parsing must tolerate whitespace around the equals sign'
)
assert.match(
  source,
  /targetWorkOrderIdsText[\s\S]*lateWorkOrderId[\s\S]*earlyWorkOrderId/,
  'FIFO page must receive the late work order before the early work order to prove backend sorting'
)
assert.match(
  source,
  /finally\s*\{[\s\S]*restoreSignatureAuthorization[\s\S]*cleanupFixtures/,
  'fixture cleanup and signature authorization restoration must run from finally'
)
assert.match(
  source,
  /INSERT INTO mes_md_workstation_worker[\s\S]*@operator_post_id[\s\S]*INSERT INTO system_user_post[\s\S]*@operator_post_id/,
  'workstation workers and the login user must use the same task-owned post.'
)
assert.match(
  source,
  /DELETE FROM system_user_post[\s\S]*post_id = @operator_post_id[\s\S]*DELETE FROM system_post/,
  'cleanup must remove the task-owned user-post binding before removing the task-owned post.'
)
assert.match(
  source,
  /async function assertFrontlineProcess[\s\S]*filter\(\{\s*hasText:\s*expectedProcessName\s*\}\)[\s\S]*waitFor\(\{[\s\S]*state:\s*'visible'/,
  'the real flow must wait for the exact selected process label after asynchronous page initialization.'
)
const ensureEmployeeBlock = source.match(
  /async function ensureFrontlineEmployee[\s\S]*?(?=\nasync function assertFrontlineProcess)/
)?.[0]
assert.ok(ensureEmployeeBlock, 'the real flow must define ensureFrontlineEmployee.')
assert.match(
  ensureEmployeeBlock,
  /\.frontline-operator-screen:visible/,
  'employee verification must be scoped to the currently visible frontline operator instance.'
)
assert.doesNotMatch(
  ensureEmployeeBlock,
  /employeeCard\.click\(\)/,
  'the isolated fixture must wait for the component auto-selection instead of reopening the employee picker.'
)
const assertProcessBlock = source.match(
  /async function assertFrontlineProcess[\s\S]*?(?=\nfunction recordWriteResponse)/
)?.[0]
assert.ok(assertProcessBlock, 'the real flow must define assertFrontlineProcess.')
assert.match(
  assertProcessBlock,
  /\.frontline-operator-screen:visible/,
  'process verification must be scoped to the currently visible frontline operator instance.'
)
assert.match(
  source,
  /function verifyDatabaseState[\s\S]*const markerContainsSql = sqlString\(`%\$\{state\.runMarker\}%`\)[\s\S]*'productionRecordbookContainsMarker'[\s\S]*entry_content_json LIKE \$\{markerContainsSql\}/,
  'final recordbook verification must search for the task marker as contained payload text.'
)
assert.doesNotMatch(
  source,
  /JSON_SEARCH\(\s*entry_content_json,\s*'one',\s*\$\{sqlString\(state\.runMarker\)\}/,
  'final recordbook verification must not require the marker to be a standalone exact JSON value.'
)
assert.match(
  source,
  /function runTeamLeaderWorkbench[\s\S]*responseUrl\.searchParams\.get\('submitDate'\) === state\.databaseEvidence\.submitDate[\s\S]*responseUrl\.searchParams\.get\('employeeUserId'\) === String\(LOGIN_USER_ID\)/,
  'team-leader response waiting must match the submitted date and employee instead of racing the initial page load.'
)
assert.match(
  source,
  /const detailResponsePromise = page\.waitForResponse[\s\S]*responseUrl\.searchParams\.get\('id'\) ===\s*String\(state\.productionSubmit\.processPoolEventId\)[\s\S]*await detailResponsePromise/,
  'team-leader detail verification must wait for the selected production event response.'
)
assert.match(
  source,
  /assert\.match\(\s*String\(detailData\.fifoAllocationSummary\),[\s\S]*new RegExp\([\s\S]*productionValues\.outputQuantity[\s\S]*getByText\(String\(detailData\.fifoAllocationSummary\),\s*\{ exact: true \}\)[\s\S]*getByText\(String\(detailData\.auditCopySummary\),\s*\{ exact: true \}\)/,
  'team-leader detail verification must validate numeric summary semantics and render the exact API summaries.'
)
assert.doesNotMatch(
  source,
  /const detailText = await drawer\.innerText\(\)/,
  'team-leader detail verification must not read the drawer before asynchronous detail content is rendered.'
)

for (const forbiddenPattern of [
  /page\.evaluate\([\s\S]*frontline\/submit/,
  /fetch\([\s\S]*fifo-orchestration\/allocate-available-output/,
  /fetch\([\s\S]*review-copy\/generate-submit-from-rules/,
  /const\s+OPERATOR_POST_ID\s*=/,
  /(?:=|LIKE)\s+CONCAT\([^;\n]*@run_key/i,
  /(?:=|LIKE)\s+@run_key\b/i,
  /\bIN\s*\([^;\n]*@run_key/i
]) {
  assert.ok(
    !forbiddenPattern.test(source),
    `full-chain E2E must not replace a frontend write path with API-only execution: ${forbiddenPattern}`
  )
}

console.log('PASS: process-pool full-chain real-flow static contract')
