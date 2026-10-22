import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { reactive } from 'vue'
import cloneDeep from 'lodash-es/cloneDeep.js'
import { createFormalActionGuard, completeApprovalWrite } from '../src/store/modules/approvalCenterInvalidationState.ts'

const frontlineFile = 'feedback/FrontlineFixedTemplatePanel.vue'
const candidateFile = 'edhr-work-task/WorkTaskBoardPage.vue'
const source = file => readFileSync(new URL(`../src/views/mes/pro/${file}`, import.meta.url), 'utf8')
const declaration = (file, name, nextName) => {
  const text = source(file)
  const start = text.indexOf(`const ${name} =`)
  const end = text.indexOf(`const ${nextName} =`, start)
  assert.ok(start >= 0 && end > start, name)
  return text.slice(start, end)
}
const evaluate = (text, dependencies) => {
  const context = { ...dependencies }
  runInNewContext(stripTypeScriptTypes(text), context)
  return context
}
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const tick = () => new Promise(resolve => setImmediate(resolve))
const timeout = () => ({ isAxiosError: true, code: 'ECONNABORTED', message: 'timed out' })
const receipt = task => ({ pqcTaskId: task, pqcEventId: task + 100, signatureId: task + 200 })
const frontlineFixture = () => {
  let sessionKey = 'tenant-1:user-1:1'
  const route = { fullPath: '/frontline/pqc?activeOrderId=11' }
  const taskOptions = [1, 2].map(pqcTaskId => ({ pqcTaskId, regulationVersionId: 31, qaProcessId: 21,
    inspectionRuleKey: 'FIRST', inspectionType: 'FIRST', businessDate: '2026-10-07', shiftCode: 'DAY',
    roundNo: 1, inspectionItems: [{ itemCode: `METHOD-${pqcTaskId}` }] }))
  const deviceState = { selectedActiveOrder: { activeOrderId: 11, workOrderId: 12 },
    selectedProcess: { activeOrderId: 11, routeId: 13, qaProcessId: 21, pqcTaskOptions: taskOptions },
    selectedEmployee: { userId: 41, systemUserId: 42 } }
  const domainContext = { routeId: 13, routeProcessId: 14, processId: 15, actualEmployeeId: 41 }
  const notices = [], events = [], writes = [], resets = []
  const hooks = {}
  let reader = async () => undefined
  let writer = async payload => receipt(payload.pqcTaskId)
  const payloads = taskOptions.map(option => ({ activeOrderId: 11, pqcTaskId: option.pqcTaskId,
    regulationVersionId: 31, qaProcessId: 21, actualEmployeeId: 41, signaturePassword: 'original-password',
    rawPayload: reactive({ method: { field: 'original' } }) }))
  const runtime = evaluate(
    declaration(frontlineFile, 'pqcSubmitContextKey', 'isSubmitBlocked')
    + declaration(frontlineFile, 'recoverPqcSubmitReceiptAfterUncertainError', 'assertFormalPayloadContext')
    + '\nglobalThis.api = { handleConfirmPqcSubmit, recoverPqcSubmitReceiptAfterUncertainError, pqcSubmitContextKey, invalidatePqcSubmitContext, pqcMethodKey, pqcMethodReceipts, pqcUncertainMethods, pqcMethodWrites, pqcSubmitGuard, pqcActionInProgress }', {
      ref: value => ({ value }), createFormalActionGuard, cloneDeep,
      watch: (key, invalidate, options) => { hooks.watch = { key, invalidate, options } },
      onBeforeUnmount: invalidate => { hooks.unmount = invalidate },
      onDeactivated: invalidate => { hooks.deactivate = invalidate },
      route, deviceState, context: domainContext, isPqcMode: { value: true },
      activePqcTaskOptionId: { value: 1 }, selectedPqcInspectionRuleKey: { value: 'FIRST' },
      isFrontlinePqcProcess: process => Boolean(process?.qaProcessId),
      approvalInvalidation: {
        syncSession: () => ({ key: sessionKey }),
        captureHandoffSourceSuccess: (type, id) => ({ session: { key: sessionKey }, type, id }),
        publishEdhrSuccess: event => events.push(event)
      },
      hasReturnNotification: { value: false }, isInitialHandoffBlocked: { value: false },
      payloadLoading: { value: false }, pqcSubmitResultUncertain: { value: false },
      pqcSignaturePassword: { value: 'original-password' }, pqcSignatureDialogVisible: { value: true },
      assertPqcFormalSubmissionReady: () => {}, assertPqcSignatureAndQuantityReady: () => {},
      assertPqcCurrentProcessAllMethodSubmissionReady: () => {}, buildPqcInspectionSubmitPayloads: () => payloads,
      ProFeedbackApi: {
        submitFrontlinePqcInspection: payload => { writes.push(payload); return writer(payload) },
        getFrontlinePqcSubmitReceipt: input => reader(input)
      },
      showFrontlineError: error => notices.push(String(error.message || error)),
      clearFrontlineError: () => notices.push('clear'),
      resetPqcSubmissionDrafts: ids => resets.push(Array.from(ids)),
      message: { success: text => notices.push(text) }, resolveErrorMessage: error => error.message,
      isAxiosError: error => error.isAxiosError === true
    })
  return { ...runtime.api, runtime, route, deviceState, domainContext, payloads, notices, events, writes, resets, hooks,
    setWriter: fn => { writer = fn }, setReader: fn => { reader = fn },
    setSession: value => { sessionKey = value },
    seedNewUi: () => {
      runtime.pqcSignaturePassword.value = 'new-password'
      runtime.payloadLoading.value = true
      runtime.pqcSignatureDialogVisible.value = true
    } }
}

for (const [name, switchContext] of [
  ['full route', f => { f.route.fullPath += '&new=1' }],
  ['active order', f => { f.deviceState.selectedActiveOrder.activeOrderId = 99 }],
  ['work order', f => { f.deviceState.selectedActiveOrder.workOrderId = 99 }],
  ['process', f => { f.deviceState.selectedProcess.qaProcessId = 99 }],
  ['employee', f => { f.deviceState.selectedEmployee.userId = 99 }],
  ['formal context', f => { f.domainContext.actualEmployeeId = 99 }],
  ['selected task', f => { f.runtime.activePqcTaskOptionId.value = 2 }],
  ['method set', f => { f.deviceState.selectedProcess.pqcTaskOptions[0].inspectionItems[0].itemCode = 'changed' }],
  ['session', f => f.setSession('tenant-2:user-2:2')],
  ['deactivation', f => f.hooks.deactivate()],
  ['unmount', f => f.hooks.unmount()]
]) {
  test(`actual multi-method PQC stops further writes and isolates old success after ${name}`, async () => {
    const f = frontlineFixture(), first = deferred()
    f.setWriter(() => first.promise)
    const pending = f.handleConfirmPqcSubmit()
    assert.equal(f.writes.length, 1)
    switchContext(f)
    f.seedNewUi()
    first.resolve(receipt(1))
    await pending
    assert.equal(f.writes.length, 1)
    assert.equal(f.events.length, 1)
    assert.equal(f.events[0].session.key, 'tenant-1:user-1:1')
    assert.equal(f.events[0].id, 1)
    assert.equal(f.runtime.pqcSignaturePassword.value, 'new-password')
    assert.equal(f.runtime.payloadLoading.value, true)
    assert.equal(f.runtime.pqcSignatureDialogVisible.value, true)
    assert.equal(f.resets.length, 0)
    assert.equal(f.notices.length, 0)
    assert.equal(f.pqcMethodReceipts.size, 1)
    assert.equal(f.pqcMethodWrites.size, 0)
  })
}
test('actual PQC payloads are detached from Vue reactive drafts across method awaits', async () => {
  const f = frontlineFixture(), first = deferred()
  f.setWriter(payload => payload.pqcTaskId === 1 ? first.promise : Promise.resolve(receipt(2)))
  const pending = f.handleConfirmPqcSubmit()
  f.payloads[1].rawPayload.method.field = 'edited'
  first.resolve(receipt(1))
  await pending
  assert.equal(f.writes[1].rawPayload.method.field, 'original')
  assert.equal(f.events.length, 2)
  assert.equal(f.resets.length, 1)
})
test('actual PQC registered context watcher revokes an attempt even after returning to its original route', async () => {
  const f = frontlineFixture(), first = deferred()
  f.setWriter(() => first.promise)
  const pending = f.handleConfirmPqcSubmit()
  assert.equal(f.hooks.watch.options.flush, 'sync')
  f.route.fullPath += '&away=1'
  f.hooks.watch.invalidate()
  f.route.fullPath = '/frontline/pqc?activeOrderId=11'
  f.hooks.watch.invalidate()
  f.seedNewUi()
  first.resolve(receipt(1))
  await pending
  assert.equal(f.writes.length, 1)
  assert.equal(f.events.length, 1)
  assert.equal(f.runtime.pqcSignaturePassword.value, 'new-password')
  assert.equal(f.runtime.payloadLoading.value, true)
  assert.equal(f.resets.length, 0)
  assert.equal(f.notices.length, 0)
})
test('actual PQC duplicates are locked, and partial success is never written again after another method fails', async () => {
  const f = frontlineFixture(), first = deferred()
  f.setWriter(payload => payload.pqcTaskId === 1 ? first.promise : Promise.reject(Error('method two rejected')))
  const pending = f.handleConfirmPqcSubmit()
  await f.handleConfirmPqcSubmit()
  assert.equal(f.writes.length, 1)
  first.resolve(receipt(1))
  await pending
  assert.equal(f.events.length, 1)
  assert.equal(f.resets.length, 0)
  assert.match(f.notices[0], /method two rejected/)
  f.runtime.pqcSignaturePassword.value = 'retry-password'
  f.setWriter(async payload => receipt(payload.pqcTaskId))
  await f.handleConfirmPqcSubmit()
  assert.deepEqual(f.writes.map(payload => payload.pqcTaskId), [1, 2, 2])
  assert.equal(f.events.length, 2)
})
for (const recovery of ['success', 'failure']) {
  test(`actual uncertain PQC receipt ${recovery} after a context switch cannot edit the new UI or prove success`, async () => {
    const f = frontlineFixture(), read = deferred()
    f.setWriter(async () => { throw timeout() })
    f.setReader(() => read.promise)
    const pending = f.handleConfirmPqcSubmit()
    await tick()
    assert.equal(f.runtime.pqcSubmitResultUncertain.value, true)
    f.route.fullPath += '&changed=1'
    f.invalidatePqcSubmitContext()
    f.seedNewUi()
    if (recovery === 'success') read.resolve(receipt(1))
    else read.reject(Error('late read failure'))
    await pending
    assert.equal(f.events.length, 0)
    assert.equal(f.notices.length, 0)
    assert.equal(f.runtime.pqcSignaturePassword.value, 'new-password')
    assert.equal(f.runtime.payloadLoading.value, true)
    assert.equal(f.pqcUncertainMethods.size, 1)
    f.route.fullPath = '/frontline/pqc?activeOrderId=11'
    f.runtime.payloadLoading.value = false
    f.runtime.pqcSignaturePassword.value = 'retry'
    await f.handleConfirmPqcSubmit()
    assert.equal(f.writes.length, 1)
    assert.equal(f.runtime.pqcSubmitResultUncertain.value, true)
  })
}
test('actual PQC structured timeout while stale records uncertainty without a GET or late error', async () => {
  const f = frontlineFixture(), write = deferred()
  let reads = 0
  f.setWriter(() => write.promise)
  f.setReader(async () => { reads++; return receipt(1) })
  const pending = f.handleConfirmPqcSubmit()
  f.setSession('new-session')
  f.seedNewUi()
  write.reject(timeout())
  await pending
  assert.equal(reads, 0)
  assert.equal(f.events.length, 0)
  assert.equal(f.pqcUncertainMethods.size, 1)
  assert.equal(f.notices.length, 0)
  assert.equal(f.runtime.payloadLoading.value, true)
})
test('actual PQC explicit response failure does not recover, publish, or continue other methods', async () => {
  const f = frontlineFixture()
  let reads = 0
  f.setWriter(async () => { throw { ...timeout(), response: { status: 403 } } })
  f.setReader(async () => { reads++; return receipt(1) })
  await f.handleConfirmPqcSubmit()
  assert.equal(reads, 0)
  assert.equal(f.events.length, 0)
  assert.equal(f.writes.length, 1)
  assert.equal(f.pqcUncertainMethods.size, 0)
  assert.match(f.notices[0], /timed out/)
})
test('actual PQC late explicit failure cannot change a new context or continue methods', async () => {
  const f = frontlineFixture(), write = deferred()
  let reads = 0
  f.setWriter(() => write.promise)
  f.setReader(async () => { reads++; return receipt(1) })
  const pending = f.handleConfirmPqcSubmit()
  f.route.fullPath += '&changed=1'
  f.seedNewUi()
  write.reject({ ...timeout(), response: { status: 403 } })
  await pending
  assert.equal(reads, 0)
  assert.equal(f.events.length, 0)
  assert.equal(f.writes.length, 1)
  assert.equal(f.notices.length, 0)
  assert.equal(f.runtime.pqcSignaturePassword.value, 'new-password')
  assert.equal(f.runtime.payloadLoading.value, true)
  assert.equal(f.runtime.pqcSignatureDialogVisible.value, true)
  assert.equal(f.pqcMethodWrites.size, 0)
})
test('actual PQC receipt read cannot prove the attempt even in the same original context', async () => {
  const f = frontlineFixture()
  f.setWriter(async () => { throw timeout() })
  f.setReader(async () => receipt(1))
  await f.handleConfirmPqcSubmit()
  assert.equal(f.events.length, 0)
  assert.equal(f.runtime.pqcSubmitResultUncertain.value, true)
  assert.match(f.notices[0], /无法确认其对应本次提交内容/)
  await f.handleConfirmPqcSubmit()
  assert.equal(f.writes.length, 1)
})

const candidateFixture = () => {
  let sessionKey = 'original-session'
  let confirm = async () => true, write = async () => true
  const notices = [], events = [], writes = []
  const hooks = {}
  let confirms = 0
  const route = { fullPath: '/work-task?taskType=REVIEW' }
  const activeTab = { value: 'candidate' }, queryParams = { pageNo: 1, taskType: 'REVIEW' }
  const row = { id: '9223372036854775806', executionId: '22', taskType: 'REVIEW', status: 'TODO',
    batchExecutionId: '33', signatureCellKey: 'cell-1', bpmTaskId: 'bpm-task-1' }
  const runtime = evaluate(
    declaration(candidateFile, 'candidateSignatureWrites', 'initializeRouteFilters')
    + '\nglobalThis.api = { handleCompleteCandidateSignature, invalidateCandidateSignatureContext, candidateSignatureTaskIdentity, candidateSignatureWrites }', {
      route, activeTab, queryParams, completeApprovalWrite,
      watch: (key, invalidate, options) => { hooks.watch = { key, invalidate, options } },
      onBeforeUnmount: invalidate => { hooks.unmount = invalidate },
      onDeactivated: invalidate => { hooks.deactivate = invalidate },
      approvalInvalidation: {
        syncSession: () => ({ key: sessionKey }),
        captureEdhrSuccess: identity => ({ session: { key: sessionKey }, identity }),
        isCurrent: key => sessionKey === key, publishEdhrSuccess: event => events.push(event)
      },
      message: { confirm: () => { confirms++; return confirm() },
        success: text => notices.push(text), warning: text => notices.push(text), error: text => notices.push(text) },
      completeEdhrCandidateSignatureTask: (id, executionId) => { writes.push({ id, executionId }); return write() },
      getList: async () => {}, loadError: { value: '' }, resolveErrorMessage: error => error.message
    })
  return { ...runtime.api, runtime, row, route, notices, events, writes, hooks,
    get confirms() { return confirms }, setConfirm: fn => { confirm = fn }, setWrite: fn => { write = fn },
    setSession: key => { sessionKey = key } }
}
for (const [name, change] of [
  ['route', f => { f.route.fullPath += '&away=1' }],
  ['session', f => f.setSession('new-session')],
  ['deactivate', f => f.hooks.deactivate()],
  ['unmount', f => f.hooks.unmount()],
  ['formal task', f => { f.row.bpmTaskId = 'another-task' }],
  ['list view', f => { f.runtime.activeTab.value = 'done' }],
  ['query', f => { f.runtime.queryParams.pageNo = 2 }]
]) {
  test(`actual candidate confirmation ${name} switch stops writes and duplicate confirmation`, async () => {
    const f = candidateFixture(), confirmation = deferred()
    f.setConfirm(() => confirmation.promise)
    const pending = f.handleCompleteCandidateSignature(f.row)
    await f.handleCompleteCandidateSignature({ ...f.row })
    assert.equal(f.confirms, 1)
    change(f)
    confirmation.resolve(true)
    await pending
    assert.equal(f.writes.length, 0)
    assert.equal(f.events.length, 0)
    assert.equal(f.notices.length, 0)
    assert.equal(f.candidateSignatureWrites.size, 0)
  })
}
test('actual candidate registered context watcher invalidates confirmation after leaving and returning', async () => {
  const f = candidateFixture(), confirmation = deferred()
  f.setConfirm(() => confirmation.promise)
  const pending = f.handleCompleteCandidateSignature(f.row)
  await f.handleCompleteCandidateSignature({ ...f.row, executionId: 'changed-execution' })
  assert.equal(f.confirms, 1)
  assert.equal(f.hooks.watch.options.flush, 'sync')
  f.route.fullPath += '&away=1'
  f.hooks.watch.invalidate()
  f.route.fullPath = '/work-task?taskType=REVIEW'
  f.hooks.watch.invalidate()
  confirmation.resolve(true)
  await pending
  assert.equal(f.writes.length, 0)
  assert.equal(f.events.length, 0)
  assert.equal(f.notices.length, 0)
  assert.equal(f.candidateSignatureWrites.size, 0)
})
for (const outcome of ['success', 'failure']) {
  test(`actual candidate late write ${outcome} cannot notify or load a new context`, async () => {
    const f = candidateFixture(), write = deferred()
    let reads = 0
    f.setWrite(() => write.promise)
    f.runtime.getList = async () => { reads++ }
    const pending = f.handleCompleteCandidateSignature(f.row)
    await tick()
    assert.equal(f.writes.length, 1)
    await f.handleCompleteCandidateSignature(f.row)
    assert.equal(f.confirms, 1)
    f.route.fullPath += '&away=1'
    f.setSession('new-session')
    if (outcome === 'success') write.resolve(true)
    else write.reject(Error('old failure'))
    await pending
    assert.equal(f.events.length, outcome === 'success' ? 1 : 0)
    if (outcome === 'success') assert.equal(f.events[0].session.key, 'original-session')
    assert.equal(reads, 0)
    assert.equal(f.notices.length, 0)
    assert.equal(f.candidateSignatureWrites.size, 0)
  })
}
test('actual candidate refresh receives the guard and its stale failure has no new UI warning', async () => {
  const f = candidateFixture(), read = deferred()
  let guard
  f.runtime.getList = current => { guard = current; return read.promise }
  const pending = f.handleCompleteCandidateSignature(f.row)
  await tick()
  assert.equal(guard(), true)
  f.route.fullPath += '&away=1'
  assert.equal(guard(), false)
  read.reject(Error('old read failure'))
  await pending
  assert.deepEqual(f.notices, ['签名完成'])
  assert.equal(f.events.length, 1)
})
test('actual work task list read/finally cannot clear or overwrite a new context during candidate refresh', async () => {
  const f = candidateFixture(), read = deferred()
  const listRuntime = evaluate('let workTaskListSequence = 0;\n'
    + declaration(candidateFile, 'getList', 'handleQuery') + '\nglobalThis.get = getList', {
      loading: { value: false }, loadError: f.runtime.loadError, list: { value: ['original'] }, total: { value: 1 },
      activeTab: { value: 'candidate' }, buildQuery: () => ({}), resolveMyPageStatus: () => 'TODO',
      getEdhrWorkTaskCandidateTodoPage: () => read.promise, refreshStats: assert.fail,
      resolveErrorMessage: error => error.message
    })
  f.runtime.getList = listRuntime.get
  const pending = f.handleCompleteCandidateSignature(f.row)
  await tick()
  f.invalidateCandidateSignatureContext()
  f.route.fullPath += '&new=1'
  listRuntime.loading.value = true
  listRuntime.list.value = ['new-context']
  listRuntime.loadError.value = 'new-error'
  read.resolve({ list: ['old'], total: 20 })
  await pending
  assert.equal(listRuntime.loading.value, true)
  assert.deepEqual(listRuntime.list.value, ['new-context'])
  assert.equal(listRuntime.loadError.value, 'new-error')
  assert.equal(f.events.length, 1)
})
