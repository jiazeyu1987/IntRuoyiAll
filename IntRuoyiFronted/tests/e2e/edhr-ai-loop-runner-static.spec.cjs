const fs = require('node:fs')
const assert = require('node:assert/strict')
const { EventEmitter } = require('node:events')
const { trackTargetRequests } = require('./edhr-ai-loop/runner.cjs')

const source = fs.readFileSync('tests/e2e/edhr-ai-loop/runner.cjs', 'utf8')
assert.match(source, /chromium\.launch\(/)
assert.match(source, /page\.goto\(/)
assert.match(source, /create-ai-e2e-production-order/)
assert.match(source, /data-edhr-ai-e2e-open-global/, 'runner must open AI E2E creation from the global toolbar, not from a local template row')
assert.doesNotMatch(source, /if \(!templateWorkOrderCode\) return exitWithFailure/, 'runner must not block S00 when the configured ERP template endpoint can create AI E2E orders')
assert.doesNotMatch(source, /templateWorkOrderCodeRequired/, 'failure reports must not describe template work-order code as a required input when configured template mode is supported')
assert.doesNotMatch(source, /locator\('\\.el-table__row'\)\\.filter\\(\\{ hasText: templateId \\}\\)/, 'runner must not require a visible local template work-order row')
assert.match(source, /context\.tracing\.start\(/)
assert.match(source, /trace\.zip/)
assert.match(source, /writeRunReport\(/)
assert.match(source, /function exitWithFailure\(error,[\s\S]*?\)\s*\{/, 'runner must write a structured report when setup fails before Playwright stage handling starts')
assert.match(source, /writeFailureArtifacts\(\{[\s\S]*candidateCodePaths: stageCandidateCodePaths\(failedStage\)/, 'top-level failures must include candidate code paths in the report')
assert.match(source, /main\(\)\.catch\(\(error\) => exitWithFailure\(error, 'S00', 'INFRASTRUCTURE_BLOCKED'\)\)/, 'top-level main failures must not call an undefined failure handler')
assert.match(source, /classifyError\(error\)/, 'runner must classify UI/infrastructure/business failures before writing result')
assert.match(source, /async function runWithStage\(stage, action, expected, fn\)/, 'runner must wrap each top-level phase with an explicit failure stage')
assert.match(source, /await runWithStage\('S00',\s*'S00登录页与账号会话初始化'/, 'login and session setup failures must be reported as S00, not business S01')
assert.match(source, /await runWithStage\('PREPARE',\s*'PREPARE确定性ERP生产订单创建'/, 'ERP order creation failures must be reported as PREPARE before business S01')
assert.match(source, /await runWithStage\('VERIFY_READY',\s*'VERIFY_READY本地工单同步与队列隔离'/, 'local sync and isolation failures must stay in VERIFY_READY')
assert.doesNotMatch(source, /const failedStage = error\.stage \|\| 'S01'/, 'unstaged infrastructure or login failures must not default to business S01')
assert.match(source, /async function verifyReady\(page, manifest, erpOrders\)/, 'runner must verify local work-order sync and queue isolation before S01')
assert.match(source, /async function joinActiveOrder\(page, manifestOrder\)/, 'runner must implement business S01 through the production leader page')
assert.match(source, /runIncrementalSync|incremental-sync/, 'runner must trigger ERP production-order sync through the real page path')
assert.match(source, /active-order\/candidates/, 'runner must verify active-order candidate state through the natural page request')
assert.match(source, /active-order\/add/, 'runner must submit active-order add through the natural page request')
assert.match(source, /ACTION_ADD|receipt\.action[\s\S]*ADD/, 'S01 must assert first-time ADD instead of REUSE or RECOVER')
assert.match(source, /async function submitProductionReports\(page, manifestOrder, activeOrder\)/, 'runner must implement business S02 through the frontline production and production leader pages')
assert.match(source, /\/mes\/pro\/feedback\/frontline\/submit/, 'runner must submit production reports through the natural page request')
assert.match(source, /\/mes\/pro\/process-pool\/team-leader\/submission\/allocation\/confirm/, 'runner must confirm production report allocation through the natural page request')
assert.match(source, /async function submitPqcInspections\(page, manifestOrder, production\)/, 'runner must implement business S03 through frontline PQC and PQC leader pages')
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
assert.match(source, /blocked = \['INFRASTRUCTURE_BLOCKED', 'UI_ACTION_FAILED', 'TEST_HARNESS_FAILURE', 'PRECONDITION_BLOCKED'\]/, 'UI action failures must stay BLOCKED instead of entering product business failure')
assert.match(source, /stageResults\(failedStage, blocked \? 'BLOCKED' : 'FAIL'\)/, 'blocked classifications must not mark a business stage as FAIL')
assert.match(source, /function ordersForMode\(manifest\)/, 'runner must select the required order slots per mode instead of always preparing all five slots')
assert.match(source, /manifest\.mode === 'full'[\s\S]*order\.slot === 'O01'/, 'full mode must prepare only O01 for the main chain')
assert.match(source, /ordersForMode\(manifest\)/, 'runner must use mode-specific order selection when creating ERP orders')
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
assert.match(source, /data-edhr-ai-e2e-run-id/, 'runner must fill runId through stable automation selector')
assert.match(source, /data-edhr-ai-e2e-slot/, 'runner must select slot through stable automation selector')
assert.match(source, /data-edhr-ai-e2e-batch-number/, 'runner must fill batch number through stable automation selector')
assert.match(source, /data-edhr-ai-e2e-submit/, 'runner must submit through stable automation selector')
assert.match(source, /data-work-order-sync-kingdee/, 'runner must trigger sync through a stable page selector')
assert.match(source, /data-work-order-code/, 'runner must verify synced work order through a stable page selector')
assert.match(source, /data-team-leader-open-active-order-dialog/, 'runner must open active-order dialog through the production leader page')
assert.match(source, /data-team-leader-active-order-candidate-select/, 'runner must search candidate through a stable selector')
assert.match(source, /data-team-leader-active-order-candidate-option/, 'runner must select the exact candidate through a stable selector')
assert.match(source, /data-team-leader-active-order-add-submit/, 'runner must submit active-order add through a stable selector')
assert.match(source, /data-team-leader-active-order-work-order-code/, 'runner must verify active-order list row after S01')
assert.doesNotMatch(source, /getByLabel\('运行编号'\)|getByLabel\('订单槽位'\)|getByLabel\('生产批号'\)/, 'runner must not depend on Element Plus label association for AI E2E dialog')
assert.match(source, /targetRequests,\s*targetRequestEvidenceFlushed:\s*true,\s*candidateCodePaths: stageCandidateCodePaths\(failedStage\)/, 'failure result must include flushed request evidence and candidate code paths')
assert.match(source, /expected: error\.expected \|\|/, 'failure result must include expected values')
assert.match(source, /actual: error\.actual \|\|/, 'failure result must include actual values')
assert.match(source, /dialog\.locator\('input\[data-edhr-ai-e2e-run-id\]'\)/, 'runner must target the actual el-input DOM attribute for runId')
assert.match(source, /dialog\.locator\('input\[data-edhr-ai-e2e-batch-number\]'\)/, 'runner must target the actual el-input DOM attribute for batch number')
assert.doesNotMatch(source, /\[data-edhr-ai-e2e-run-id\] input/, 'runId data selector is rendered on the input itself')
assert.doesNotMatch(source, /\[data-edhr-ai-e2e-batch-number\] input/, 'batch number data selector is rendered on the input itself')
assert.match(source, /EDHR_AI_E2E_TEMPLATE_WORK_ORDER_CODE/)
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
    url: () => 'http://127.0.0.1:8081/admin-api/mes/pro/process-pool/team-leader/active-order/add',
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
  assert.equal(targetRequests[0].label, 'ACTIVE_ORDER_ADD')
  assert.equal(targetRequests[0].method, 'POST')
  assert.equal(targetRequests[0].url, '/admin-api/mes/pro/process-pool/team-leader/active-order/add')
  assert.equal(targetRequests[0].httpStatus, 200)
  assert.equal(targetRequests[0].businessCode, 0)
}

async function verifyTargetRequestParseError() {
  const page = new EventEmitter()
  const targetRequests = trackTargetRequests(page)
  page.emit('response', {
    url: () => 'http://127.0.0.1:8081/admin-api/mes/pro/process-pool/team-leader/active-order/add',
    request: () => ({ method: () => 'POST' }),
    status: () => 200,
    headers: () => ({ 'content-type': 'application/json' }),
    json: () => Promise.reject(new Error('invalid json'))
  })
  await targetRequests.flush()
  assert.equal(targetRequests[0].label, 'ACTIVE_ORDER_ADD')
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








