// Local harness doubles only; no browser, API or real business actions.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const { freezeExecutionBaseline } = require('./edhr-ai-loop/coverage.cjs')
const runnerPath = path.join(__dirname, 'edhr-ai-loop/runner.cjs')
const source = fs.readFileSync(runnerPath, 'utf8')

function load(hooks = {}) {
  // Keep contract arrays in the same realm as coverage.cjs's frozen snapshots.
  hooks = { pqcInspectionContract: items => items.map(({ lastSelectedEquipmentId, lastSelectedEquipmentNumber, ...contract }) => contract), ...hooks }
  const sandbox = { require: createRequire(runnerPath), module: { exports: {} }, process, console, URL, hooks }
  vm.createContext(sandbox)
  vm.runInContext(source + '\n' + Object.keys(hooks).map(name => `${name} = hooks.${name}`).join('\n') +
    '\nmodule.exports.safety = { assertFreshPqcTaskGroup: typeof assertFreshPqcTaskGroup === "function" ? assertFreshPqcTaskGroup : undefined, selectExactPqcReviewRow: typeof selectExactPqcReviewRow === "function" ? selectExactPqcReviewRow : undefined, submitOnePqcInspectionRound, reviewPqcInspectionSubmission, submitOnePqcInspectionForProcess, readPqcProcessesFromPage, selectFrontlinePqcOrder }', sandbox)
  return sandbox.module.exports.safety
}
const clone = value => JSON.parse(JSON.stringify(value))
const order = { activeOrderId: '900', workOrderCode: 'LOCAL-SAFETY', quantity: 10 }
const task = (id, code) => ({ pqcTaskId: id, inspectionRuleKey: 'FIRST', inspectionType: 'FIRST', businessDate: '2026-10-02',
  shiftCode: 'DAY', roundNo: 1, plannedInspectionQuantity: 2, taskStatus: 'PENDING', inspectionItems: [{ itemCode: code, resultType: 'BOOLEAN' }] })
const processRow = { activeOrderId: 900, regulationVersionId: 70, qaProcessId: 168,
  inspectionItems: [{ itemCode: 'A' }, { itemCode: 'B' }], pqcTaskOptions: [task(101, 'A'), task(102, 'A'), task(201, 'B'), task(202, 'B')] }
const snapshots = processRow.pqcTaskOptions.map(t => ({ ...t, pqcTaskId: String(t.pqcTaskId), ruleKey: t.inspectionRuleKey,
  type: t.inspectionType, quantity: t.plannedInspectionQuantity }))
const baseline = freezeExecutionBaseline(order, [{ routeProcessId: '1', processKey: 'P1', targetQuantity: 10, outputMaterials: ['M'] }],
  [{ processKey: 'QA-70-168', processLabel: 'QA168', tasks: snapshots }])
const first = baseline.pqcGroups[0]
const second = baseline.pqcGroups[1]

function response(taskId, data = {}, overrides = {}) {
  return { ok: () => true, status: () => 200,
    json: async () => ({ code: 0, data: { pqcTaskId: taskId, pqcEventId: 1000 + Number(taskId), sourceRevision: 1000 + Number(taskId), inspectionResult: 'SUCCESS', ...data } }),
    url: () => 'http://local/admin-api/mes/pro/feedback/frontline/device-account/pqc/submit',
    request: () => ({ method: () => 'POST', postDataJSON: () => ({ activeOrderId: 900, pqcTaskId: taskId }) }), ...overrides }
}

async function submitCase(responses, { clickFails = false, requestFails = false } = {}) {
  let confirmations = 0
  let logins = 0
  let reads = 0
  const page = { url: () => 'http://local/mes/pro/feedback/edhr-batch-pqc-fill',
    waitForResponse: async predicate => {
      if (requestFails) throw new Error('net::ERR_CONNECTION_RESET')
      const match = responses.find(r => predicate(r))
      if (!match) throw new Error('Timeout 60000ms exceeded: missing receipt')
      return match
    },
    locator: selector => ({ isEnabled: async () => true, waitFor: async () => {}, fill: async () => {},
      click: async () => { if (selector.includes('submit-confirm-accept')) { confirmations += 1; if (clickFails) throw new Error('click interrupted') } },
      locator: child => page.locator(child) }) }
  const safety = load({ fillPqcInspectionItems: async () => [], isLoginPage: async () => false,
    login: async () => { logins += 1 }, ensurePqcSession: async () => { logins += 1 },
    readPqcProcessesFromPage: async () => { reads += 1; return [processRow] } })
  try { return { result: await safety.submitOnePqcInspectionRound(page, order, first), confirmations, logins, reads } }
  catch (error) { return { error, confirmations, logins, reads } }
}

async function reviewCase(code = 0, overrides = {}) {
  let confirmations = 0
  let logins = 0
  const row = { id: 9001, activeOrderId: 900, workOrderCode: order.workOrderCode, groupedEventIds: [1101, 1201], submissionReviewStatus: null }
  const page = { waitForResponse: async predicate => {
    if (overrides.waitError) throw new Error(overrides.waitError)
    const responseCode = confirmations === 0 ? code : 0
    const r = { ok: () => true, status: () => 200, url: () => 'http://local/admin-api/mes/pro/process-pool/team-leader/submission/review',
      request: () => ({ method: () => 'POST', postDataJSON: () => ({ eventId: 9001, leaderType: 'PQC' }) }),
      json: async () => ({ code: responseCode, data: 77 }), ...overrides }
    assert.ok(predicate(r)); return r
  }, locator: selector => ({ first() { return this }, nth() { return this }, filter() { return this },
    count: async () => 1, waitFor: async () => {}, fill: async () => {},
    getAttribute: async attribute => attribute.includes('submitted-event') ? '1201 1101' : '9001',
    click: async () => { if (selector.includes('review-submit')) confirmations += 1 }, locator: child => page.locator(child) }) }
  const safety = load({ isLoginPage: async () => false, login: async () => { logins += 1 }, openPqcReviewWorkbench: async () => ({ list: [row] }) })
  try { return { result: await safety.reviewPqcInspectionSubmission(page, order,
    { pqcTaskId: '101', submitSourceEventId: '1101', groupedPqcTaskIds: ['101', '201'], groupedPqcEventIds: ['1101', '1201'] }), confirmations, logins } }
  catch (error) { return { error, confirmations, logins } }
}

;(async () => {
  const rejectedReview = await reviewCase(401)
  assert.ok(rejectedReview.error, '401 review must stop instead of re-login and replay')
  assert.equal(rejectedReview.confirmations, 1, '401 review must issue one confirmation only')
  assert.equal(rejectedReview.logins, 0, '401 review must not automatically re-login')
  console.log('PASS: review 401 stops after one business confirmation without re-login')
  const safety = load()
  assert.deepEqual(clone(safety.assertFreshPqcTaskGroup([processRow], order, first)), ['101', '201'])
  assert.throws(() => safety.assertFreshPqcTaskGroup([processRow], order, second), /pending|待检|任务集合/)
  for (const mutate of [
    p => { p.activeOrderId = 901 },
    p => { p.pqcTaskOptions[0].taskStatus = 'SUBMITTED' },
    p => { p.pqcTaskOptions[0].pqcTaskId = 999 },
    p => { p.pqcTaskOptions.reverse() },
    p => { p.pqcTaskOptions[0].plannedInspectionQuantity = 3 },
    p => { p.pqcTaskOptions[0].inspectionItems[0].resultType = 'NUMBER' },
    // Item/rule clicks choose the first pending task across dates, before scope grouping.
    p => { p.pqcTaskOptions.unshift({ ...task(91, 'A'), businessDate: '2026-10-01' }, { ...task(92, 'B'), businessDate: '2026-10-01' }) },
    p => { p.pqcTaskOptions.push(task(301, 'C')); p.inspectionItems.push({ itemCode: 'C' }) },
    p => { p.pqcTaskOptions.push(clone(p.pqcTaskOptions[0])) }
  ]) {
    const changed = clone(processRow); mutate(changed)
    assert.throws(() => safety.assertFreshPqcTaskGroup([changed], order, first))
  }
  const next = clone(processRow)
  next.pqcTaskOptions.filter(t => [101, 201].includes(t.pqcTaskId)).forEach(t => { t.taskStatus = 'CONFIRMED' })
  assert.deepEqual(clone(safety.assertFreshPqcTaskGroup([next], order, second)), ['102', '202'])
  console.log('PASS: fresh explicit pending task IDs, per-item first task selection, consumed/replaced/reordered/extra/duplicate/drift rejection')

  let pendingResponse
  let naturalReads = 0
  let explicitClicks = 0
  const selectionPage = { goto: async () => {}, locator: selector => ({ first() { return this }, filter() { return this },
    waitFor: async () => {}, fill: async () => {}, click: async () => {
      if (selector === '[data-pqc-order-option]') {
        assert.ok(pendingResponse, 'listen to fresh response before selecting order')
        explicitClicks += 1
        const r = { request: () => ({ method: () => 'GET' }),
          url: () => 'http://local/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes?activeOrderId=900',
          ok: () => true, status: () => 200, json: async () => ({ code: 0, data: [processRow] }) }
        assert.ok(pendingResponse.predicate(r)); pendingResponse.resolve(r)
      }
    } }), waitForResponse: predicate => new Promise(resolve => { naturalReads += 1; pendingResponse = { predicate, resolve } }) }
  const selected = await load({ isLoginPage: async () => false }).selectFrontlinePqcOrder(selectionPage, order)
  assert.equal(selected[0].activeOrderId, 900)
  assert.equal(naturalReads, 1)
  assert.equal(explicitClicks, 1)
  let groupReads = 0
  let groupWrites = 0
  const groupPage = { locator: () => ({ waitFor: async () => {}, click: async () => {} }) }
  const groupSafety = load({ readPqcProcessesFromPage: async () => { groupReads += 1; return [groupReads === 1 ? processRow : next] },
    selectFrontlinePqcProcess: async () => 'QA168', submitOnePqcInspectionRound: async (_p, _o, step) => { groupWrites += 1; return step.tasks.map(t => ({ pqcTaskId: t.pqcTaskId })) } })
  await groupSafety.submitOnePqcInspectionForProcess(groupPage, order, first)
  await groupSafety.submitOnePqcInspectionForProcess(groupPage, order, second)
  assert.equal(groupReads, 2)
  assert.equal(groupWrites, 2)
  await assert.rejects(() => groupSafety.submitOnePqcInspectionForProcess(groupPage, order, first))
  assert.equal(groupReads, 3)
  assert.equal(groupWrites, 2, 'stale group must stop before a write')
  console.log('PASS: explicit UI selection response wait precedes click; every group rereads and stale group performs zero writes')

  const review = { id: 9001, activeOrderId: 900, workOrderCode: order.workOrderCode, groupedEventIds: [1101, 1201], submissionReviewStatus: null }
  const unrelated = { ...review, id: 9002, groupedEventIds: [1102, 1202] }
  assert.equal(safety.selectExactPqcReviewRow([unrelated, review], order, ['1201', '1101']).id, 9001)
  assert.equal(safety.selectExactPqcReviewRow([{ ...review, submittedEventIds: [] }], order, ['1101', '1201']).id, 9001)
  for (const ids of [[1101], [1101, 1201, 9999], [1101, 1101], [1102, 1202]]) {
    assert.throws(() => safety.selectExactPqcReviewRow([{ ...review, groupedEventIds: ids }], order, ['1101', '1201']))
  }
  assert.throws(() => safety.selectExactPqcReviewRow([review, { ...review, id: 9003 }], order, ['1101', '1201']))
  assert.throws(() => safety.selectExactPqcReviewRow([{ ...review, activeOrderId: 901 }], order, ['1101', '1201']))
  assert.throws(() => safety.selectExactPqcReviewRow([{ ...review, submissionReviewStatus: 'APPROVED' }], order, ['1101', '1201']))
  assert.throws(() => safety.selectExactPqcReviewRow([{ ...review, submissionReviewStatus: 'REJECTED' }], order, ['1101', '1201']))
  assert.throws(() => safety.selectExactPqcReviewRow([review], order, ['1101', '1101']))
  console.log('PASS: review selects one exact returned source event set amid other rows and rejects mismatched/duplicate/approved identities')

  const good = await submitCase([response(101), response(201)])
  assert.ifError(good.error)
  assert.equal(good.confirmations, 1)
  assert.equal(good.logins, 0)
  const failures = [
    [response(101), response(201, {}, { json: async () => ({ code: 401, msg: 'expired' }) })],
    [response(101), response(201, {}, { ok: () => false, status: () => 503 })],
    [response(101), response(201, {}, { json: async () => { throw new Error('invalid JSON') } })],
    [response(101)],
    [response(101), response(201, { sourceRevision: null })],
    [response(101), response(201, { pqcTaskId: 999 })],
    [response(101), response(201, { sourceRevision: 1101 })]
  ]
  for (const responses of failures) {
    const failed = await submitCase(responses)
    assert.ok(failed.error)
    assert.equal(failed.confirmations, 1)
    assert.equal(failed.logins, 0)
    assert.equal(failed.error.actual.automaticRetry, false)
    assert.ok(failed.error.actual.successfulSubmissions.some(s => String(s.pqcTaskId) === '101'))
  }
  const delayedReceipt = await submitCase([
    response(101, {}, { json: () => new Promise(resolve => setTimeout(() => resolve({ code: 0,
      data: { pqcTaskId: 101, pqcEventId: 1101, sourceRevision: 1101, inspectionResult: 'SUCCESS' } }), 0)) }),
    response(201, {}, { json: async () => ({ code: 401, msg: 'expired' }) })
  ])
  assert.ok(delayedReceipt.error)
  assert.equal(delayedReceipt.confirmations, 1)
  assert.ok(delayedReceipt.error.actual.successfulSubmissions.some(s => String(s.pqcTaskId) === '101'),
    'settle already-registered receipt waits before returning partial-success evidence')
  const interrupted = await submitCase([response(101), response(201)], { clickFails: true })
  assert.ok(interrupted.error)
  assert.equal(interrupted.confirmations, 1)
  assert.equal(interrupted.logins, 0)
  const disconnected = await submitCase([response(101), response(201)], { requestFails: true })
  assert.ok(disconnected.error)
  assert.equal(disconnected.confirmations, 1)
  assert.equal(disconnected.logins, 0)
  console.log('PASS: grouped writes stop after 401, HTTP failure, JSON/identity/event error, missing response and interrupted click; no login or second confirmation')

  const accepted = await reviewCase()
  assert.ifError(accepted.error)
  assert.equal(accepted.confirmations, 1)
  assert.equal(accepted.result.groupedPqcEventIds.join(','), '1101,1201')
  for (const overrides of [{ ok: () => false, status: () => 503 },
    { json: async () => { throw new Error('invalid JSON') } }, { waitError: 'Timeout 60000ms exceeded' },
    { waitError: 'net::ERR_CONNECTION_RESET' }, { json: async () => ({ code: 0, data: null }) }]) {
    const failed = await reviewCase(0, overrides)
    assert.ok(failed.error)
    assert.equal(failed.confirmations, 1)
    assert.equal(failed.logins, 0)
    assert.equal(failed.error.actual.automaticRetry, false)
  }
  console.log('PASS: exact grouped review returns identity on success and stops once on HTTP/JSON/timeout/network/missing receipt errors')

  const submitSource = source.slice(source.indexOf('async function submitOnePqcInspectionRound'), source.indexOf('async function openPqcReviewWorkbench'))
  const reviewSource = source.slice(source.indexOf('async function reviewPqcInspectionSubmission'), source.indexOf('async function readPageResponse'))
  assert.doesNotMatch(submitSource + reviewSource, /sessionRecoveryAttempted|ensurePqcSession|await login\(|return submitOnePqcInspectionRound|return reviewPqcInspectionSubmission/)
  assert.match(source, /groupedPqcEventIds:\s*submissions\.map/)
  assert.match(source, /assertFreshPqcTaskGroup\(processes, manifestOrder, step\)/)
  console.log('PASS: runner integrates fresh group identity and exact event set without business-write retry paths')
})().catch(error => { console.error(error); process.exitCode = 1 })
