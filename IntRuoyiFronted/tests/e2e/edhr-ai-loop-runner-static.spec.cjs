const fs = require('node:fs')
const assert = require('node:assert/strict')
const { EventEmitter } = require('node:events')
const { trackTargetRequests } = require('./edhr-ai-loop/runner.cjs')

const source = fs.readFileSync('tests/e2e/edhr-ai-loop/runner.cjs', 'utf8')
assert.match(source, /chromium\.launch\(/)
assert.match(source, /page\.goto\(/)
assert.match(source, /FIXED_RESET_WORK_ORDER_CODE/, 'runner must default to the fixed reset test order')
assert.match(source, /active-order\/simulation\/test-reset/, 'runner must use the fixed test-reset page action')
assert.doesNotMatch(source, /active-order\/simulation\/copy-latest/, 'E2E01 must not use copy-latest after switching to reset')
assert.match(source, /data-team-leader-reset-fixed-active-order/, 'runner must click the real reset-fixed-test-order button')
assert.doesNotMatch(source, /data-team-leader-copy-latest-simulation-order/, 'E2E01 must not click the copy-test-order button')
assert.doesNotMatch(source, /confirmDialog|confirmText/, 'S01 must click reset directly without a confirmation dialog')
assert.doesNotMatch(source, /create-ai-e2e-production-order/, 'runner must not depend on K3 or ERP write-style order creation')
assert.doesNotMatch(source, /data-edhr-ai-e2e-open-global/, 'runner must not open the old AI E2E create-order dialog')
assert.match(source, /context\.tracing\.start\(/)
assert.match(source, /trace\.zip/)
assert.match(source, /writeRunReport\(/)
assert.match(source, /function exitWithFailure\(error,[\s\S]*?\)\s*\{/, 'runner must write a structured report when setup fails before Playwright stage handling starts')
assert.match(source, /writeFailureArtifacts\(\{[\s\S]*candidateCodePaths: stageCandidateCodePaths\(failedStage\)/, 'top-level failures must include candidate code paths in the report')
assert.match(source, /main\(\)\.catch\(\(error\) => exitWithFailure\(error, 'S00', 'INFRASTRUCTURE_BLOCKED'\)\)/, 'top-level main failures must not call an undefined failure handler')
assert.match(source, /classifyError\(error\)/, 'runner must classify UI/infrastructure/business failures before writing result')
assert.match(source, /async function runWithStage\(stage, action, expected, fn\)/, 'runner must wrap each top-level phase with an explicit failure stage')
assert.match(source, /await runWithStage\('S00',\s*'S00登录页与账号会话初始化'/, 'login and session setup failures must be reported as S00, not business S01')
assert.match(source, /async function selectLoginTenant\(page, tenantName = '芋道源码'\)/, 'runner must isolate login tenant selection in a stable helper')
assert.match(source, /\.el-select-dropdown__item:visible'[\s\S]*filter\(\{ hasText: tenantName \}\)/, 'login tenant selection must click the visible dropdown option')
assert.doesNotMatch(source, /getByText\('芋道源码', \{ exact: true \}\)\.last\(\)\.click\(\)/, 'login tenant selection must not click the selected-value span intercepted by Element Plus input')
assert.doesNotMatch(source, /await runWithStage\('PREPARE'/, 'reset-based AI loop must not create upstream ERP/K3 orders in PREPARE')
assert.doesNotMatch(source, /const failedStage = error\.stage \|\| 'S01'/, 'unstaged infrastructure or login failures must not default to business S01')
assert.match(source, /async function resetFixedTestActiveOrder\(page, manifestOrder, manifest\)/, 'runner must implement business S01 through the fixed reset button')
assert.match(source, /active-order\/list/, 'runner must read the active-order page list response')
assert.match(source, /erpFixedQuantitySnapshot/, 'runner must verify reset-order quantity from active-order list data')
assert.doesNotMatch(source, /active-order\/candidates/, 'reset-based S01 must not use old candidate query')
assert.doesNotMatch(source, /active-order\/add/, 'reset-based S01 must not use old active-order add endpoint')
assert.match(source, /RESET_FIXED_TEST_ORDER/, 'S01 must assert reset-fixed-test-order behavior instead of COPY/ADD/REUSE/RECOVER')
assert.match(source, /async function executeProductionAndPqcInterleaved\(page, manifestOrder, activeOrder\)/, 'runner must implement business S02 through the frontline production and production leader pages')
assert.match(source, /\/mes\/pro\/feedback\/frontline\/submit/, 'runner must submit production reports through the natural page request')
assert.match(source, /\/mes\/pro\/process-pool\/team-leader\/submission\/allocation\/confirm/, 'runner must confirm production report allocation through the natural page request')
assert.match(source, /async function discoverPendingPqcTasksForOrder\(/, 'runner must implement business S03 through frontline PQC and PQC leader pages')
assert.match(source, /\/mes\/pro\/feedback\/frontline\/device-account\/pqc\/submit/, 'runner must submit PQC inspections through the natural page request')
assert.match(source, /\/mes\/pro\/process-pool\/team-leader\/submission\/review/, 'runner must confirm PQC submissions through the natural page request')
assert.match(source, /async function completeActiveOrderAndApplyRelease\(page, manifestOrder\)/, 'runner must implement business S04 completion and release application through the production leader page')
assert.match(source, /PRODUCTION_PICK_LIST/, 'runner must trigger production pick-list sync during S04 through the real page path')
assert.match(source, /NO_REPLENISHMENT_CONFIRMATION_REQUIRED/, 'runner must verify the no-replenishment confirmation gate during S04')
assert.match(source, /const archiveTrace = await runWithStage\('S08',\s*'S08归档及历史追溯'[\s\S]*archiveAndVerifyHistoryS08\(page, mainOrder, finalRelease\)/, 'runner must execute S08 archive and history verification after S07')
assert.match(source, /action:\s*'S01-S08 eDHR主流程完成'/, 'runner must report the full S01-S08 chain as complete after S08')
assert.match(source, /stageResults\(null,\s*'PASS'\)/, 'S01-S08 PASS must be explicit after archive and trace verification')
assert.match(source, /TEST_HARNESS_FAILURE/, 'missing stage implementation must be reported as test harness failure')
assert.doesNotMatch(source, /PREPARE确定性订单已完成；S01加入活跃订单/, 'ERP order creation alone must not be reported as business S01 startable')
assert.doesNotMatch(source, /确定性ERP生产订单创建/, 'runner must not describe K3 or ERP write-style order creation as PREPARE')
assert.match(source, /blocked = \['INFRASTRUCTURE_BLOCKED', 'UI_ACTION_FAILED', 'TEST_HARNESS_FAILURE', 'PRECONDITION_BLOCKED'\]/, 'UI action failures must stay BLOCKED instead of entering product business failure')
assert.match(source, /stageResults\(failedStage, blocked \? 'BLOCKED' : 'FAIL'\)/, 'blocked classifications must not mark a business stage as FAIL')
assert.match(source, /function ordersForMode\(manifest\)/, 'runner must select the fixed reset order slot instead of preparing five physical orders')
assert.match(source, /function ordersForMode\(manifest\) \{[\s\S]*order\.slot === 'O01'[\s\S]*\}/, 'all modes must prepare only O01 because reset reuses one fixed physical test order')
assert.match(source, /ordersForMode\(manifest\)/, 'runner must use reset-compatible order selection')
assert.match(source, /function trackTargetRequests\(page\)/, 'runner must retain natural page request evidence for AI failure reports')
assert.match(source, /function targetRequestLabel\(method, pathname\)/, 'runner must classify target requests with stable labels')
assert.match(source, /label:\s*targetRequestLabel\(method, parsedUrl\.pathname\)/, 'target request evidence must include a stable label')
assert.match(source, /httpStatus:\s*response\.status\(\)/, 'target request evidence must use httpStatus')
assert.match(source, /businessCode:\s*null/, 'target request evidence must always include businessCode')
assert.match(source, /entry\.businessCode = Number\(body\.code\)/, 'target request evidence must parse unified response businessCode')
assert.match(source, /entry\.parseError = true/, 'JSON response parse failures must be explicit in target request evidence')
assert.doesNotMatch(source, /status:\s*response\.status\(\)/, 'target request evidence must not use ambiguous status field')
assert.doesNotMatch(source, /entry\.code = body\.code/, 'target request evidence must not use ambiguous code field')
assert.doesNotMatch(source, /response\.json\(\)\.catch\(\(\) => null\)/, 'JSON parse failures must not be silently converted to null')
assert.match(source, /targetRequests\.flush = async function flushTargetRequests\(\)/, 'runner must expose a flush hook for asynchronous response parsing')
assert.match(source, /await targetRequests\.flush\(\)/, 'runner must flush pending target request evidence before writing PASS or failure results')
assert.match(source, /targetRequestEvidenceFlushed:\s*true/, 'runner results must mark target request evidence as flushed')
assert.match(source, /data-team-leader-active-order-work-order-filter/, 'runner must filter the reset active-order row through a stable selector')
assert.match(source, /activeOrderId:\s*requirePositiveIdString\(receipt\.activeOrderId/, 'runner must preserve the reset active-order identity returned by the real page action')
assert.match(source, /data-team-leader-reset-fixed-active-order/, 'runner must submit reset through a stable selector')
assert.match(source, /data-team-leader-active-order-work-order-code/, 'runner must verify active-order list row after S01')
assert.doesNotMatch(source, /getByLabel\('运行编号'\)|getByLabel\('订单槽位'\)|getByLabel\('生产批号'\)/, 'runner must not depend on Element Plus label association for AI E2E dialog')
assert.match(source, /targetRequests,\s*targetRequestEvidenceFlushed:\s*true,\s*candidateCodePaths: stageCandidateCodePaths\(failedStage\)/, 'failure result must include flushed request evidence and candidate code paths')
assert.match(source, /expected: error\.expected \|\|/, 'failure result must include expected values')
assert.match(source, /actual: \{[^\n]*error\.actual \|\|/, 'failure result must include actual values')
assert.doesNotMatch(source, /data-edhr-ai-e2e-run-id/, 'reset-based runner must not fill the old create-order dialog runId')
assert.doesNotMatch(source, /data-edhr-ai-e2e-batch-number/, 'reset-based runner must not fill the old create-order dialog batch number')
assert.match(source, /EDHR_AI_E2E_RESET_WORK_ORDER_CODE/)
assert.match(source, /http:\/\/127\.0\.0\.1:8081/)
assert.doesNotMatch(source, /127\.0\.0\.1:48080/)
assert.doesNotMatch(source, /page\.request\.(post|get|put|delete)/)
assert.doesNotMatch(source, /fetch\(/)
assert.match(source, /function requirePositiveIdString\(value, label\)/, 'runner must preserve backend Long IDs as strings')
assert.doesNotMatch(source, /activeOrderId:\s*Number\(receipt\.activeOrderId\)/, 'activeOrderId must not be converted to JavaScript Number')
assert.doesNotMatch(source, /eventId:\s*Number\(eventId\)/, 'review event IDs must not be converted to JavaScript Number')
assert.doesNotMatch(source, /Number\(receipt\.activeOrderId\)\s*>\s*0/, 'activeOrderId presence check must not rely on Number precision')
assert.doesNotMatch(source, /waitForTimeout\(/, 'runner must wait on real page/request state instead of fixed sleeps')

async function verifyTargetRequestFlush() {
  const page = new EventEmitter()
  const targetRequests = trackTargetRequests(page)
  let jsonResolved = false
  page.emit('response', {
    url: () => 'http://127.0.0.1:8081/admin-api/mes/pro/process-pool/team-leader/active-order/simulation/test-reset',
    request: () => ({ method: () => 'POST' }),
    status: () => 200,
    headers: () => ({ 'content-type': 'application/json' }),
    json: () => new Promise((resolve) => {
      setTimeout(() => {
        jsonResolved = true
        resolve({ code: 0, msg: 'OK' })
      }, 10)
    })
  })
  assert.equal(targetRequests.length, 0)
  await targetRequests.flush()
  assert.equal(jsonResolved, true)
  assert.equal(targetRequests.length, 1)
  assert.equal(targetRequests[0].label, 'SIMULATION_TEST_RESET')
  assert.equal(targetRequests[0].method, 'POST')
  assert.equal(targetRequests[0].url, '/admin-api/mes/pro/process-pool/team-leader/active-order/simulation/test-reset')
  assert.equal(targetRequests[0].httpStatus, 200)
  assert.equal(targetRequests[0].businessCode, 0)
}

async function verifyTargetRequestParseError() {
  const page = new EventEmitter()
  const targetRequests = trackTargetRequests(page)
  page.emit('response', {
    url: () => 'http://127.0.0.1:8081/admin-api/mes/pro/process-pool/team-leader/active-order/simulation/test-reset',
    request: () => ({ method: () => 'POST' }),
    status: () => 200,
    headers: () => ({ 'content-type': 'application/json' }),
    json: () => Promise.reject(new Error('invalid json'))
  })
  await targetRequests.flush()
  assert.equal(targetRequests[0].label, 'SIMULATION_TEST_RESET')
  assert.equal(targetRequests[0].httpStatus, 200)
  assert.equal(targetRequests[0].businessCode, null)
  assert.equal(targetRequests[0].parseError, true)
}

Promise.all([verifyTargetRequestFlush(), verifyTargetRequestParseError()])
  .then(() => console.log('PASS: eDHR AI loop runner uses Playwright UI writes and structured reports'))
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })










assert.doesNotMatch(source, /verifyResetReady|readyVerified/, 'S01 must not have a separate reset preflight')
assert.match(source, /if \(onlyS01\)/, 'runner must support S01-only verification')
assert.match(source, /NOT_RUN/, 'unexecuted stages must not be reported as PASS')
