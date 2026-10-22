import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { h } from 'vue'
import { createFormalActionGuard, createFormalWriteReceiptState, createApprovalInvalidationState } from '../src/store/modules/approvalCenterInvalidationState.ts'

import { applyWithNoReplenishmentConfirmation } from '../src/views/mes/pro/processpool/activeOrderReplenishmentConfirmation.ts'

const ref = value => ({ value })
const computed = getter => ({ get value() { return getter() } })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
const extract = (file, start, end) => {
  const source = readFileSync(new URL(`../src/views/mes/pro/${file}`, import.meta.url), 'utf8')
  const a = source.indexOf(`const ${start} =`), b = source.indexOf(`const ${end} =`, a)
  assert.ok(a >= 0 && b > a)
  return source.slice(a, b)
}
const execute = (source, names, runtime) => {
  runInNewContext(stripTypeScriptTypes(source + `\nObject.assign(globalThis, {${names.join(',')}})`), runtime)
  return runtime
}
const invalidation = () => {
  const events = [], captures = []
  let session = 'account1'
  const store = {
    syncSession: () => ({ key: session }), isCurrent: key => key === session,
    captureEdhrSuccess: identity => { const event = { identity: { ...identity }, session: { key: session } }; captures.push(event); return event },
    captureHandoffSourceSuccess: (type, id) => store.captureEdhrSuccess({ sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) }),
    captureHandoffReceiptSuccess: (scope, type, id) => ({ session: scope, identity: { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) } }),
    publishEdhrSuccess: event => { events.push(event) }
  }
  return { store, events, captures, switchSession: () => { session = 'account2' } }
}

test('actual guard partitions full identity and generations; receipts persist only in exact scope', () => {
  let key = 'EDHR:WORK_TASK:12'; const guard = createFormalActionGuard(() => key)
  const old = guard.capture(); key = 'EDHR:HANDOFF:12'; assert.equal(old(), false)
  key = 'EDHR:WORK_TASK:12'; guard.invalidate(); assert.equal(old(), false)
  const receipts = createFormalWriteReceiptState(); receipts.complete(key, { id: 1 })
  assert.equal(receipts.begin(key), false); assert.equal(receipts.begin('other-context'), true)
  assert.equal(receipts.begin('other-context'), false); receipts.settle('other-context'); assert.equal(receipts.isWriting('other-context'), false)
  assert.equal(receipts.has('EDHR:HANDOFF:12'), false); assert.equal(receipts.get(key).id, 1)
  receipts.reconcile(key); assert.equal(receipts.has(key), false)
})

for (const single of [false, true]) {
  for (const outcome of ['success', 'write-fail', 'read-fail', 'missing-source', 'stale']) {
    test(`actual ${single ? 'single' : 'batch'} critical deviation transfer: ${outcome}`, async () => {
      const inv = invalidation(), notices = [], wait = deferred(); let writes = 0, reads = 0, key = 'original'
      const guard = createFormalActionGuard(() => key)
      const runtime = {
        approvalInvalidation: inv.store, props: { readonly: false },
        detail: ref({ id: 7, batchExecutionId: 4, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: true }),
        selectedCritical: ref([{ id: 7, batchExecutionId: 4 }]), isCriticalOpen: () => true,
        batchNcrLoading: ref(false), batchNcrVisible: ref(true), batchNcrGuard: guard,
        batchNcrForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '' },
        ncrCreateLoading: ref(false), ncrCreateVisible: ref(true), ncrCreateGuard: guard, ncrCreateError: ref(''),
        ncrCreateForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '', idempotencyKey: 'request1' },
        error: ref(''),
        getEdhrBatchExecution: async () => ({ id: 4, activeOrderId: outcome === 'missing-source' ? undefined : 33 }),
        createCriticalDeviationReview: async payload => { writes++; assert.equal(payload.batchExecutionId, 4); if (outcome === 'write-fail') throw Error('write failed'); if (outcome === 'stale') return wait.promise; return { batchExecutionId: 4, activeOrderId: outcome === 'missing-source' ? undefined : 33 } },
        load: async () => { reads++; if (outcome === 'read-fail') throw Error('read failed'); return true },
        message: { success: x => notices.push(x), error: x => notices.push(x) },
        ElMessage: { success: x => notices.push(x) }
      }
      const name = single ? 'submitNcrCreate' : 'submitBatchNcr'
      execute(extract(single ? 'edhr-deviation/DeviationDetail.vue' : 'edhr-deviation/index.vue', name, single ? 'loadNcrReviewDetail' : 'loadBatchOptions'), [name], runtime)
      const pending = runtime[name]()
      if (outcome === 'stale') { await new Promise(setImmediate); key = 'new-dialog'; guard.invalidate(); runtime.ncrCreateLoading.value = true; runtime.batchNcrLoading.value = true; wait.resolve({ batchExecutionId: 4, activeOrderId: 33 }) }
      await pending
      const wrote = ['success', 'read-fail', 'stale'].includes(outcome)
      assert.equal(inv.events.length, wrote ? 1 : 0)
      assert.equal(writes, 1)
      if (wrote) assert.equal(inv.events[0].identity.businessSourceId, '33')
      if (outcome === 'stale') { assert.equal(reads, 0); assert.equal(notices.length, 0); assert.equal(runtime.batchNcrLoading.value, true); assert.equal(runtime.ncrCreateLoading.value, true) }
      if (outcome === 'read-fail') assert.match(single ? runtime.ncrCreateError.value : notices.at(-1), /已发起.*刷新失败/)
    })
  }
}

const productionFixture = () => {
  const inv = invalidation(); let key = 'order33:process5:employee8', writes = 0, reads = 0, failedRead = true, resets = 0
  const guard = createFormalActionGuard(() => key), receipts = createFormalWriteReceiptState(), revision = ref(0), notices = []
  const runtime = {
    approvalInvalidation: inv.store, productionSubmitContextKey: () => key, productionSubmitGuard: guard,
    productionSubmitReceipts: receipts, productionSubmitReceiptRevision: revision,
    productionSubmitReadPending: computed(() => receipts.has(key)), payloadLoading: ref(false), productionActionInProgress: ref(false),
    hasReturnNotification: ref(false), submitConfirmationOpen: ref(false), productionSubmitSuccessOpen: ref(false),
    deviceState: { selectedActiveOrder: { activeOrderId: 33 }, selectedProcess: { activeOrderId: 33, routeProcessId: 5 }, lastError: undefined },
    isFrontlineProductionProcess: p => Boolean(p?.routeProcessId), isSameProcess: (a, b) => a.routeProcessId === b.routeProcessId,
    context: {}, draft: { fieldValues: {} }, productionSignaturePassword: ref('pw'),
    assertProductionSubmissionReady: () => [], buildProductionFieldValues: () => ({}), assertFormalPayloadContext: () => {},
    buildFrontlineTemplatePayload: () => ({}), requestProductionFormalSubmitConfirmation: async () => true,
    buildProductionFormalSubmitConfirmation: () => ({}), buildFrontlineFormalSubmitPayload: () => ({ processPoolContext: { activeOrderId: 33 } }),
    ProFeedbackApi: {
      frontlineSubmit: async () => { writes++; return { id: 10 } },
      getFrontlineProductionActiveOrderProcesses: async () => { reads++; if (failedRead) throw Error('read offline'); return [{ activeOrderId: 33, routeProcessId: 5 }] }
    },
    rememberProductionMaterialDeviceDrafts: () => {}, resetProductionSubmissionDraft: () => resets++,
    openProductionSubmitSuccessDialog: () => {}, showParameterAuditWarning: () => {}, clearFrontlineError: () => {},
    showFrontlineError: error => notices.push(String(error)), resolveErrorMessage: e => e.message,
    message: { warning: text => notices.push(text) }
  }
  execute(extract('feedback/FrontlineFixedTemplatePanel.vue', 'refreshProductionSelectedProcessSubmittedQuantity', 'assertPqcSignatureAndQuantityReady'), ['handleProductionFormalSubmit', 'retryProductionSubmitRead'], runtime)
  return { runtime, inv, receipts, notices, get writes() { return writes }, get reads() { return reads }, get resets() { return resets }, readReady: () => { failedRead = false }, change: () => { key = 'new-context'; guard.invalidate() } }
}
test('actual production write is terminal before failed read; repeat clicks cannot write; retry only reads', async () => {
  const f = productionFixture(); await f.runtime.handleProductionFormalSubmit()
  assert.equal(f.writes, 1); assert.equal(f.inv.events.length, 1); assert.equal(f.runtime.productionSubmitReadPending.value, true)
  assert.equal(f.resets, 0); assert.match(f.notices.at(-1), /提交已成功.*刷新失败/)
  await f.runtime.handleProductionFormalSubmit(); await f.runtime.retryProductionSubmitRead()
  assert.equal(f.writes, 1); assert.equal(f.runtime.productionSubmitReadPending.value, true)
  f.readReady(); await f.runtime.retryProductionSubmitRead()
  assert.equal(f.writes, 1); assert.equal(f.resets, 1); assert.equal(f.runtime.productionSubmitReadPending.value, false)
  assert.equal(f.inv.events.length, 1)
})
test('actual production read discards stale success/error/finally and retains original completed receipt', async () => {
  for (const reject of [false, true]) {
    const f = productionFixture(), wait = deferred()
    f.runtime.ProFeedbackApi.getFrontlineProductionActiveOrderProcesses = () => wait.promise
    const pending = f.runtime.handleProductionFormalSubmit(); await new Promise(setImmediate)
    f.change(); f.runtime.payloadLoading.value = true; f.runtime.deviceState.selectedProcess = { activeOrderId: 99, routeProcessId: 44 }
    if (reject) wait.reject(Error('old read failed')); else wait.resolve([{ activeOrderId: 33, routeProcessId: 5 }])
    await pending
    assert.equal(f.inv.events.length, 1); assert.equal(f.resets, 0); assert.equal(f.notices.length, 0)
    assert.equal(f.runtime.deviceState.selectedProcess.routeProcessId, 44); assert.equal(f.runtime.payloadLoading.value, true)
    assert.equal(f.receipts.has('order33:process5:employee8'), true)
  }
})

test('actual production cancellation, failed write and switched confirmation never publish', async () => {
  for (const outcome of ['cancel', 'write-fail', 'switch']) {
    const f = productionFixture(), wait = deferred()
    if (outcome === 'cancel') f.runtime.requestProductionFormalSubmitConfirmation = async () => false
    if (outcome === 'write-fail') f.runtime.ProFeedbackApi.frontlineSubmit = async () => { throw Error('write failed') }
    if (outcome === 'switch') f.runtime.requestProductionFormalSubmitConfirmation = () => wait.promise
    const pending = f.runtime.handleProductionFormalSubmit()
    if (outcome === 'switch') { f.change(); wait.resolve(true) }
    await pending
    assert.equal(f.inv.events.length, 0); assert.equal(f.runtime.productionSubmitReadPending.value, false)
  }
})

test('actual owner release stale write still publishes frozen transaction without changing new dialog', async () => {
  const inv = invalidation(), wait = deferred(), receipts = createFormalWriteReceiptState(); let key = 'old-batch', reads = 0
  const guard = createFormalActionGuard(() => key), runtime = {
    approvalInvalidation: inv.store, releaseActionContextKey: () => key, releaseSignatureGuard: guard,
    releaseSubmitReceipts: receipts, releaseSubmitReceiptRevision: ref(0), releaseSubmitReadPending: computed(() => receipts.has(key)),
    releaseSignatureSubmitting: ref(false), releaseSignatureError: ref(''), releaseSignatureConfirmVisible: ref(true),
    workbench: ref({ releaseSummary: { releaseTransactionId: 'tx11' } }),
    reconcileReleaseSubmitRead: async () => { reads++; return true }, resolveErrorMessage: e => e.message,
    message: { error: () => assert.fail('stale error'), success: () => assert.fail('stale success') }
  }
  execute(extract('edhr-batch/BatchExecutionDetailPage.vue', 'runReleaseSignatureConfirmAction', 'submitReleaseByOwnerSignature'), ['runReleaseSignatureConfirmAction'], runtime)
  const pending = runtime.runReleaseSignatureConfirmAction(() => wait.promise, 'success')
  key = 'new-batch'; guard.invalidate(); runtime.workbench.value = { releaseSummary: { releaseTransactionId: 'tx22' } }; runtime.releaseSignatureSubmitting.value = true
  wait.resolve({ releaseTransactionId: 'tx11' }); await pending
  assert.equal(inv.events[0].identity.businessSourceId, 'tx11'); assert.equal(reads, 0)
  assert.equal(runtime.releaseSignatureConfirmVisible.value, true); assert.equal(runtime.releaseSignatureSubmitting.value, true)
  assert.equal(receipts.has('old-batch'), true); assert.equal(receipts.has('new-batch'), false)
})

test('actual batch release return stale success/error/finally cannot mutate new transaction dialog', async () => {
  for (const failed of [false, true]) {
    const inv = invalidation(), wait = deferred(); let key = 'old', reads = 0
    const guard = createFormalActionGuard(() => key), runtime = {
      approvalInvalidation: inv.store, releaseReturnGuard: guard, releaseReturnSubmitting: ref(false), releaseReturnError: ref(''),
      canReturnRelease: ref(true), releaseReturnDialogVisible: ref(true), workbench: ref({ releaseSummary: { releaseTransactionId: 'tx11' } }),
      releaseReturnForm: { rejectReason: 'reason' }, ensureViewedReleaseStageWritable: () => true,
      buildReleaseIdempotencyKey: () => 'key', rejectEdhrRelease: () => wait.promise, loadDetail: async () => reads++, loadError: ref(''),
      resolveErrorMessage: e => e.message, message: { error: () => assert.fail('stale error'), success: () => assert.fail('stale success') }
    }
    execute(extract('edhr-batch/BatchExecutionDetailPage.vue', 'submitReleaseReturn', 'openNonconformanceReviewEntry'), ['submitReleaseReturn'], runtime)
    const pending = runtime.submitReleaseReturn(); key = 'new'; guard.invalidate(); runtime.releaseReturnSubmitting.value = true
    if (failed) wait.reject(Error('old error')); else wait.resolve({})
    await pending
    assert.equal(inv.events.length, failed ? 0 : 1); assert.equal(reads, 0)
    assert.equal(runtime.releaseReturnSubmitting.value, true); assert.equal(runtime.releaseReturnDialogVisible.value, true)
  }
})

test('actual leader review stale response freezes event and cannot close a new dialog', async () => {
  for (const failed of [false, true]) {
    const inv = invalidation(), wait = deferred(); let key = 'event12', reads = 0
    const guard = createFormalActionGuard(() => key), runtime = {
      approvalInvalidation: inv.store, leaderReviewGuard: guard, reviewSubmitting: ref(false), reviewVisible: ref(true),
      reviewDialogMode: ref('REVIEW'), reviewEvent: ref({ id: 12, expectedReviews: [] }), isProductionLeader: ref(false),
      reviewForm: { reviewStatus: 'APPROVED', reviewRemark: 'opinion' }, requirePositiveNumber: x => x,
      resolveCurrentLeaderType: () => 'PQC', buildReviewSignaturePayload: () => ({ signaturePassword: 'pw' }),
      reviewTeamLeaderSubmission: () => wait.promise, getSubmissionList: async () => reads++, loadActiveOrders: async () => reads++,
      resolveErrorMessage: e => e.message, ElMessage: { error: () => assert.fail('stale error'), success: () => assert.fail('stale success') }
    }
    execute(extract('processpool/TeamLeaderWorkbenchPage.vue', 'submitReview', 'resetCorrectionFormForEvent'), ['submitReview'], runtime)
    const pending = runtime.submitReview(); key = 'event22'; guard.invalidate(); runtime.reviewEvent.value = { id: 22 }; runtime.reviewSubmitting.value = true
    if (failed) wait.reject(Error('old failure')); else wait.resolve({})
    await pending
    assert.equal(inv.events.length, failed ? 0 : 1); if (!failed) assert.equal(inv.events[0].identity.businessSourceId, '12')
    assert.equal(reads, 0); assert.equal(runtime.reviewVisible.value, true); assert.equal(runtime.reviewSubmitting.value, true)
  }
})

test('actual owner release wrapper retains receipt on read failure and retries only reading', async () => {
  const inv = invalidation(), receipts = createFormalWriteReceiptState(), pending = computed(() => receipts.has('batch4'))
  let writes = 0, reads = 0, fail = true
  const runtime = {
    approvalInvalidation: inv.store, releaseActionContextKey: () => 'batch4', releaseSignatureGuard: createFormalActionGuard(() => 'batch4'),
    releaseSubmitReceipts: receipts, releaseSubmitReceiptRevision: ref(0), releaseSubmitReadPending: pending,
    releaseSignatureSubmitting: ref(false), releaseSignatureError: ref(''), releaseSignatureConfirmVisible: ref(true),
    workbench: ref({ releaseSummary: { releaseTransactionId: 'tx11' } }),
    reconcileReleaseSubmitRead: async () => { reads++; if (fail) throw Error('read offline'); return true },
    resolveErrorMessage: e => e.message, message: { error: () => {}, success: () => {} }
  }
  execute(extract('edhr-batch/BatchExecutionDetailPage.vue', 'runReleaseSignatureConfirmAction', 'submitReleaseByOwnerSignature') + extract('edhr-batch/BatchExecutionDetailPage.vue', 'retryReleaseSubmitRead', 'openReleaseSignatureConfirmDialog'), ['runReleaseSignatureConfirmAction', 'retryReleaseSubmitRead'], runtime)
  const write = async () => { writes++; return { releaseTransactionId: 'tx11' } }
  await runtime.runReleaseSignatureConfirmAction(write, 'success')
  assert.equal(writes, 1); assert.equal(pending.value, true); assert.equal(inv.events[0].identity.businessSourceId, 'tx11')
  assert.match(runtime.releaseSignatureError.value, /已提交.*刷新失败/)
  await runtime.runReleaseSignatureConfirmAction(write, 'success'); assert.equal(writes, 1)
  fail = false; await runtime.retryReleaseSubmitRead(); assert.equal(writes, 1); assert.equal(reads, 2); assert.equal(pending.value, false)
})

for (const kind of ['approve', 'batch-reject']) {
  for (const outcome of ['success', 'error']) test(`actual ${kind} old response cannot close or clear new route/dialog: ${outcome}`, async () => {
    const inv = invalidation(), wait = deferred(), notices = []; let key = 'original', reads = 0
    const guard = createFormalActionGuard(() => key)
    const runtime = {
      approvalInvalidation: inv.store, approvalActionGuard: guard, rejectContextGuard: guard,
      actionLoading: ref(false), actionError: ref(''), actionMode: ref('approve'), actionDialogVisible: ref(true),
      actionForm: { password: 'pw', comment: 'opinion', rejectReason: '' },
      detail: ref({ executionId: 4, processInstanceId: 'instance1', approvalSnapshotId: 's1', approvalSnapshotHash: 'hash1', bpmTaskId: 'b1' }),
      workTaskId: ref(11), resolveActionDisabledReason: () => '', buildSignatureTimePayload: () => ({}),
      approveEdhrExecution: () => wait.promise, rejectEdhrExecution: () => wait.promise,
      resolveApproveSuccessMessage: () => 'success', resolveRejectSuccessMessage: () => 'success', EDHR_EXECUTION_STATUS: { REJECTED: 2 },
      rejectLoading: ref(false), rejectError: ref(''), rejectDialogVisible: ref(true), selectedRejectBatch: ref({ id: 4 }),
      rejectForm: { nonconformanceReason: 'reason', signaturePassword: 'pw' }, rejectEdhrBatchExecutionToNonconformanceReview: () => wait.promise,
      loadDetail: async () => reads++, getList: async () => reads++, loadError: ref(''), resolveErrorMessage: e => e.message,
      message: { success: x => notices.push(x), error: x => notices.push(x) }
    }
    const name = kind === 'approve' ? 'submitAction' : 'submitRejectBatchExecution'
    execute(extract(kind === 'approve' ? 'edhr/ApprovalDetailPage.vue' : 'edhr-batch/BatchExecutionListPage.vue', name, kind === 'approve' ? 'openArchiveGenerateDialog' : 'releaseForm'), [name], runtime)
    const pending = runtime[name](); key = 'new-dialog'; guard.invalidate()
    runtime.actionLoading.value = true; runtime.rejectLoading.value = true
    runtime.actionForm.password = 'new password'; runtime.rejectForm.signaturePassword = 'new password'
    if (outcome === 'error') wait.reject(Error('old failure')); else wait.resolve({ status: 1 })
    await pending
    assert.equal(inv.events.length, outcome === 'success' ? 1 : 0); assert.equal(reads, 0); assert.equal(notices.length, 0)
    assert.equal(runtime.actionDialogVisible.value, true); assert.equal(runtime.rejectDialogVisible.value, true)
    assert.equal(runtime.actionLoading.value, true); assert.equal(runtime.rejectLoading.value, true)
    assert.equal(runtime.actionForm.password, 'new password'); assert.equal(runtime.rejectForm.signaturePassword, 'new password')
  })
}

test('actual manager predicate freezes session, route, task and dialog generation', () => {
  const inv = invalidation(), runtime = { approvalInvalidation: inv.store, route: { fullPath: '/task1' }, managerReleaseDialogGeneration: 1, managerReleaseTask: ref({ id: 11 }), managerReleaseDialogVisible: ref(true) }
  const source = extract('edhr-work-task/WorkTaskBoardPage.vue', 'captureManagerReleaseContext', 'resetManagerReleaseDialog').split('watch(managerReleaseDialogVisible')[0]
  execute(source, ['captureManagerReleaseContext'], runtime)
  const current = runtime.captureManagerReleaseContext(); assert.equal(current(), true)
  inv.switchSession(); assert.equal(current(), false)
  const sameSession = runtime.captureManagerReleaseContext(); runtime.route.fullPath = '/task2'; assert.equal(sameSession(), false)
  const sameRoute = runtime.captureManagerReleaseContext(); runtime.managerReleaseTask.value = { id: 22 }; assert.equal(sameRoute(), false)
  const sameTask = runtime.captureManagerReleaseContext(); runtime.managerReleaseDialogGeneration++; assert.equal(sameTask(), false)
})

test('actual terminal receipt contexts cannot be bypassed by route-query-only changes', () => {
  const inv = invalidation(), route = { fullPath: '/form?view=1', query: { id: '4' } }, runtime = {
    approvalInvalidation: inv.store, route,
    deviceState: { selectedActiveOrder: { activeOrderId: 33 }, selectedProcess: { routeProcessId: 5, processId: 6 }, selectedEmployee: { userId: 8 } },
    isFrontlineProductionProcess: p => Boolean(p?.routeProcessId)
  }
  execute(extract('feedback/FrontlineFixedTemplatePanel.vue', 'productionSubmitContextKey', 'productionSubmitGuard'), ['productionSubmitContextKey', 'productionActionContextKey'], runtime)
  execute(extract('edhr-batch/BatchExecutionDetailPage.vue', 'releaseActionContextKey', 'releaseSignatureGuard'), ['releaseActionContextKey', 'releaseUiContextKey'], runtime)
  const productionReceiptKey = runtime.productionSubmitContextKey(), releaseReceiptKey = runtime.releaseActionContextKey()
  const productionUiKey = runtime.productionActionContextKey(), releaseUiKey = runtime.releaseUiContextKey()
  route.fullPath = '/form?view=2'
  assert.equal(runtime.productionSubmitContextKey(), productionReceiptKey); assert.equal(runtime.releaseActionContextKey(), releaseReceiptKey)
  assert.notEqual(runtime.productionActionContextKey(), productionUiKey); assert.notEqual(runtime.releaseUiContextKey(), releaseUiKey)
})

test('actual approval detail reset/open paths reset loading and advance dialog generation', () => {
  const guard = createFormalActionGuard(() => 'route1'), runtime = {
    approvalActionGuard: guard, actionLoading: ref(true), loading: ref(true), actionForm: { password: 'old', comment: '', rejectReason: '' },
    actionMode: ref('approve'), actionDialogVisible: ref(false), actionError: ref(''),
    resolveActionDisabledReason: () => '', message: { error: () => {} }, watch: () => {}, onDeactivated: () => {}, onBeforeUnmount: () => {}
  }
  const reset = extract('edhr/ApprovalDetailPage.vue', 'resetApprovalActionContext', 'allowedSignatureCellKey')
  execute(reset + extract('edhr/ApprovalDetailPage.vue', 'openActionDialog', 'submitAction'), ['resetApprovalActionContext', 'openActionDialog'], runtime)
  const old = guard.capture(); runtime.resetApprovalActionContext()
  assert.equal(old(), false); assert.equal(runtime.actionLoading.value, false); assert.equal(runtime.actionForm.password, '')
  runtime.actionLoading.value = true; runtime.actionForm.password = 'old'
  runtime.openActionDialog('reject'); assert.equal(runtime.actionLoading.value, false); assert.equal(runtime.actionForm.password, '')
  assert.equal(runtime.actionDialogVisible.value, true); assert.equal(runtime.actionMode.value, 'reject')
})

test('actual owner read reconciliation rejects missing or different transaction and keeps receipt', async () => {
  const receipts = createFormalWriteReceiptState(); receipts.complete('batch4', { releaseTransactionId: 'tx11' })
  for (const tx of [undefined, 'tx22', 'tx11']) {
    const runtime = {
      releaseSubmitReceipts: receipts, releaseActionContextKey: () => 'batch4',
      loadDetail: async () => {}, loadError: ref(''), cancelDeferredBatchDetailSecondaryLoad: () => {},
      loadBatchDetailSecondaryData: async () => {}, assertBatchExecutionId: () => 4, batchDetailRequestSerial: 1,
      secondaryLoadError: ref(''), workbench: ref({ releaseSummary: { releaseTransactionId: tx } })
    }
    execute(extract('edhr-batch/BatchExecutionDetailPage.vue', 'reconcileReleaseSubmitRead', 'retryReleaseSubmitRead'), ['reconcileReleaseSubmitRead'], runtime)
    if (tx === 'tx11') assert.equal(await runtime.reconcileReleaseSubmitRead(() => true), true)
    else await assert.rejects(runtime.reconcileReleaseSubmitRead(() => true), /回执不一致/)
    assert.equal(receipts.has('batch4'), true)
  }
})

// Execute the production receipt validation/recovery/submit code with controllable transport promises.
const releaseHarness = () => {
  let session = { key: 's1', tenantId: '1', loginUserId: '7' }, routeKey = 'order1'
  const state = createApprovalInvalidationState(); state.setSession(session.key)
  const events = [], notices = [], payloads = []
  const row = { id: '9007199254740993', workOrderId: 41, routeId: 42, routeVersionId: 43 }
  const receipt = () => ({ applicationId: '11', activeOrderId: row.id, workOrderId: '41', routeId: '42', routeVersionId: '43', batchExecutionId: '12', pqcReleaseWorkTaskId: '13', status: 'PQC_RELEASE_PENDING', sourceSnapshotHash: 'formal-hash', version: 1, appliedAt: '2026-10-07', requestIdempotencyKey: 'current-request' })
  let reads = 0, writes = 0, refreshes = 0
  const runtime = {
    Object, Date, Number, String, Set, Map, h,
    releaseApplicationIdempotencyKeys: new Map([[row.id, 'current-request']]), releaseApplicationLocks: new Map(),
    releaseApplicationSubmittingId: ref(undefined), releaseApplicationBlockers: ref([]), releaseApplicationUncertainMessage: ref(''), activeOrderTestResetSubmitting: ref(false),
    approvalInvalidation: {
      syncSession: () => session,
      captureHandoffReceiptSuccess: (scope, type, id) => state.capture(scope, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: id }),
      publishEdhrSuccess: event => { events.push(event); state.publish(event) }
    },
    leaderReleaseGuard: createFormalActionGuard(() => JSON.stringify([session.key, routeKey])),
    canApplyActiveOrderRelease: () => true, isActiveOrderReleaseApplicationLocked: id => runtime.releaseApplicationLocks.has(id), resolveActiveOrderReleaseApplyDisabledReason: () => 'locked',
    getTeamLeaderActiveOrderRelease: async id => { reads++; assert.equal(id, '9007199254740993'); return receipt() },
    applyTeamLeaderActiveOrderRelease: async payload => { writes++; payloads.push(payload); return receipt() },
    loadActiveOrders: async () => { refreshes++ }, activeOrderOptions: ref([]), applyWithNoReplenishmentConfirmation,
    requirePositiveNumber: (value, message) => { if (!(typeof value === 'number' && Number.isFinite(value) && value > 0)) throw Error(message); return value },
    resolveErrorMessage: e => e?.message ?? String(e), ElMessageBox: { confirm: async () => {} },
    ElMessage: { success: x => notices.push(x), error: x => notices.push(x), warning: x => notices.push(x) }
  }
  const file = 'processpool/TeamLeaderWorkbenchPage.vue'
  execute(extract(file, 'getOrCreateActiveOrderReleaseIdempotencyKey', 'leaderReleaseGuard') + extract(file, 'assertActiveOrderReleaseApplicationReceipt', 'submitMoveActiveOrder'), ['freezeActiveOrderReleaseRequest', 'confirmActiveOrderReleaseApplicationReceipt', 'recoverUncertainActiveOrderReleaseApplication', 'submitActiveOrderReleaseApplication', 'requireActiveOrderReleaseIdentity', 'isActiveOrderReleaseTransportTimeout'], runtime)
  return { runtime, row, receipt, events, notices, payloads, state, counts: () => ({ reads, writes, refreshes }), route: () => { routeKey = 'order2'; runtime.leaderReleaseGuard.invalidate() }, account: () => { session = { key: 's2', tenantId: '2', loginUserId: '8' }; state.setSession(session.key) } }
}
const transportTimeout = () => ({ isAxiosError: true, code: 'ECONNABORTED', request: {}, message: 'timeout' })

for (const change of ['route', 'session', 'source-version', 'order-version', 'cancel', 'current']) {
  test(`actual leader secondary no-replenishment confirmation: ${change}`, async () => {
    const h = releaseHarness(), confirmation = deferred(), confirmationStarted = deferred()
    let confirms = 0, writes = 0
    h.runtime.ElMessageBox.confirm = async () => {
      confirms++
      if (confirms === 2) { confirmationStarted.resolve(); return confirmation.promise }
    }
    h.runtime.applyTeamLeaderActiveOrderRelease = async payload => {
      writes++; h.payloads.push(payload)
      if (writes === 1) throw { code: 100, message: 'NO_REPLENISHMENT_CONFIRMATION_REQUIRED' }
      return h.receipt()
    }
    const pending = h.runtime.submitActiveOrderReleaseApplication(h.row)
    await confirmationStarted.promise
    if (change === 'route') h.route()
    if (change === 'session') h.account()
    if (change === 'source-version') h.row.routeVersionId = '99'
    if (change === 'order-version') h.row.version = 2
    if (change === 'cancel') confirmation.reject('cancel'); else confirmation.resolve()
    await pending
    assert.equal(writes, change === 'current' ? 2 : 1)
    assert.equal(confirms, 2)
    assert.equal(h.events.length, change === 'current' ? 1 : 0)
    if (change === 'current') assert.equal(h.payloads[1].confirmNoReplenishmentInfo, true)
    if (!['current', 'cancel'].includes(change)) assert.equal(h.notices.length, 0)
    if (change === 'cancel') assert.equal(h.runtime.releaseApplicationLocks.has(h.row.id), false)
  })
}

for (const change of ['route', 'session', 'source-version', 'order-version']) {
  test(`actual leader obsolete first prerequisite opens no second dialog: ${change}`, async () => {
    const h = releaseHarness(), firstWrite = deferred(), started = deferred()
    let confirms = 0, writes = 0
    h.runtime.ElMessageBox.confirm = async () => { confirms++ }
    h.runtime.applyTeamLeaderActiveOrderRelease = () => { writes++; started.resolve(); return firstWrite.promise }
    const pending = h.runtime.submitActiveOrderReleaseApplication(h.row)
    await started.promise
    if (change === 'route') h.route()
    if (change === 'session') h.account()
    if (change === 'source-version') h.row.routeVersionId = '99'
    if (change === 'order-version') h.row.version = 2
    firstWrite.reject({ code: 100, message: 'NO_REPLENISHMENT_CONFIRMATION_REQUIRED' })
    await pending
    assert.equal(writes, 1); assert.equal(confirms, 1)
    assert.equal(h.events.length, 0); assert.equal(h.notices.length, 0)
  })
}

test('actual replenishment helper checks context before first write and rethrows current failure', async () => {
  let writes = 0, confirms = 0
  const apply = async () => { writes++; throw { code: 500, message: 'formal rejected' } }
  const confirm = async () => { confirms++ }
  assert.equal(await applyWithNoReplenishmentConfirmation(apply, confirm, e => e.message, () => false), undefined)
  assert.equal(writes, 0)
  await assert.rejects(applyWithNoReplenishmentConfirmation(apply, confirm, e => e.message, () => true), e => e.code === 500)
  assert.equal(writes, 1); assert.equal(confirms, 0)
})

for (const variant of ['match', 'old-key', 'missing-key', 'empty-key', 'empty-receipt', 'wrong-order', 'wrong-work-order', 'wrong-route', 'wrong-version', 'unknown-status', 'missing-hash', 'missing-p2', 'missing-pqc', 'missing-application', 'missing-formal-version', 'fractional-formal-version', 'missing-applied-at', 'read-failure']) {
  test(`actual release submit timeout receipt: ${variant}`, async () => {
    const h = releaseHarness(), { runtime, row } = h
    runtime.applyTeamLeaderActiveOrderRelease = async payload => { h.payloads.push(payload); throw transportTimeout() }
    const originalRead = runtime.getTeamLeaderActiveOrderRelease
    runtime.getTeamLeaderActiveOrderRelease = async id => {
      const r = await originalRead(id)
      if (variant === 'read-failure') throw Error('read failed')
      if (variant === 'empty-receipt') return null
      if (variant === 'old-key') r.requestIdempotencyKey = 'old-request'
      if (variant === 'missing-key') delete r.requestIdempotencyKey
      if (variant === 'empty-key') r.requestIdempotencyKey = ''
      if (variant === 'wrong-order') r.activeOrderId = '9007199254740992'
      if (variant === 'wrong-work-order') r.workOrderId = '44'
      if (variant === 'wrong-route') r.routeId = '44'
      if (variant === 'wrong-version') r.routeVersionId = '44'
      if (variant === 'unknown-status') r.status = 'UNKNOWN'
      if (variant === 'missing-hash') r.sourceSnapshotHash = ''
      if (variant === 'missing-p2') r.batchExecutionId = ''
      if (variant === 'missing-pqc') r.pqcReleaseWorkTaskId = ''
      if (variant === 'missing-application') r.applicationId = ''
      if (variant === 'missing-formal-version') r.version = 0
      if (variant === 'fractional-formal-version') r.version = 1.5
      if (variant === 'missing-applied-at') r.appliedAt = ''
      return r
    }
    await runtime.submitActiveOrderReleaseApplication(row)
    assert.equal(h.payloads.length, 1); assert.equal(h.payloads[0].activeOrderId, row.id)
    assert.equal(h.counts().reads, 1); assert.equal(h.events.length, variant === 'match' ? 1 : 0)
    assert.equal(runtime.releaseApplicationLocks.get(row.id), variant === 'match' ? 'CONFIRMED' : 'UNCERTAIN')
    assert.equal(runtime.releaseApplicationIdempotencyKeys.has(row.id), variant !== 'match')
    assert.equal(row.releaseApplicationId, variant === 'match' ? '11' : undefined)
    assert.equal(runtime.releaseApplicationSubmittingId.value, undefined)
    if (variant !== 'match') { await runtime.submitActiveOrderReleaseApplication(row); assert.equal(h.payloads.length, 1) }
  })
}
for (const [name, error] of [
  ['business', { code: 100, message: 'timeout business rejected' }],
  ['http', { ...transportTimeout(), response: { status: 500 } }],
  ['audit', { code: 500, message: 'audit failed' }],
  ['plain', Error('timeout')], ['string', 'timeout'],
  ['network', { isAxiosError: true, code: 'ERR_NETWORK', request: {}, message: 'Network Error' }],
  ['no-request', { isAxiosError: true, code: 'ETIMEDOUT', message: 'timeout' }]
]) test(`actual release ${name} rejection never reads a receipt`, async () => {
  const h = releaseHarness(); h.runtime.applyTeamLeaderActiveOrderRelease = async () => { throw error }
  await h.runtime.submitActiveOrderReleaseApplication(h.row)
  assert.equal(h.counts().reads, 0); assert.equal(h.events.length, 0); assert.equal(h.row.releaseApplicationId, undefined)
})
for (const stale of ['route-before-timeout', 'route-during-get', 'account-during-get', 'source-during-get', 'failure-during-get']) {
  test(`actual release recovery isolates ${stale}`, async () => {
    const h = releaseHarness(), wait = deferred(), write = deferred()
    h.runtime.applyTeamLeaderActiveOrderRelease = () => write.promise
    const originalRead = h.runtime.getTeamLeaderActiveOrderRelease
    h.runtime.getTeamLeaderActiveOrderRelease = async id => { const receipt = await originalRead(id); await wait.promise; return receipt }
    const pending = h.runtime.submitActiveOrderReleaseApplication(h.row)
    await new Promise(setImmediate)
    if (stale === 'route-before-timeout') h.route()
    write.reject(transportTimeout()); await new Promise(setImmediate)
    assert.equal(h.counts().reads, 1)
    if (stale === 'account-during-get') h.account()
    else if (stale === 'source-during-get') h.row.routeVersionId = '99'
    else h.route()
    h.runtime.releaseApplicationSubmittingId.value = 'new-task'
    h.runtime.releaseApplicationUncertainMessage.value = 'new-message'
    const lockBefore = h.runtime.releaseApplicationLocks.get(h.row.id)
    if (stale === 'failure-during-get') wait.reject(Error('old read failure')); else wait.resolve()
    await pending
    assert.equal(h.events.length, stale === 'failure-during-get' ? 0 : 1)
    assert.equal(h.state.revision('EDHR', 'TODO'), stale === 'failure-during-get' || stale === 'account-during-get' ? 0 : 1)
    assert.equal(h.row.releaseApplicationId, undefined); assert.equal(h.notices.length, 0); assert.equal(h.counts().refreshes, 0)
    assert.equal(h.runtime.releaseApplicationSubmittingId.value, 'new-task'); assert.equal(h.runtime.releaseApplicationUncertainMessage.value, 'new-message')
    assert.equal(h.runtime.releaseApplicationLocks.get(h.row.id), lockBefore)
    assert.equal(h.runtime.releaseApplicationIdempotencyKeys.get(h.row.id), 'current-request')
    if (h.events.length) { assert.equal(h.events[0].session.key, 's1'); assert.equal(h.events[0].identity.businessSourceId, '9007199254740993') }
  })
}
test('actual release identity freezing rejects missing and rounded Long sources before writes', async () => {
  for (const bad of [undefined, '', 9007199254740992]) {
    const h = releaseHarness(); h.row.workOrderId = bad
    await h.runtime.submitActiveOrderReleaseApplication(h.row)
    assert.equal(h.counts().writes, 0); assert.equal(h.counts().reads, 0); assert.equal(h.events.length, 0)
  }
})
test('actual recovered success keeps read-refresh failure separate and prevents another write', async () => {
  const h = releaseHarness(); let writes = 0
  h.runtime.applyTeamLeaderActiveOrderRelease = async () => { writes++; throw transportTimeout() }
  h.runtime.loadActiveOrders = async () => { throw Error('refresh failed') }
  await h.runtime.submitActiveOrderReleaseApplication(h.row)
  assert.equal(h.events.length, 1); assert.equal(h.runtime.releaseApplicationLocks.get(h.row.id), 'CONFIRMED_REFRESH_FAILED')
  assert.match(h.runtime.releaseApplicationUncertainMessage.value, /已提交/)
  await h.runtime.submitActiveOrderReleaseApplication(h.row); assert.equal(writes, 1)
})
test('actual synchronous release success publishes original event before stale UI check', async () => {
  const h = releaseHarness(), write = deferred(); h.runtime.applyTeamLeaderActiveOrderRelease = () => write.promise
  const pending = h.runtime.submitActiveOrderReleaseApplication(h.row); await new Promise(setImmediate)
  h.route(); h.runtime.releaseApplicationSubmittingId.value = 'new-task'; write.resolve(h.receipt()); await pending
  assert.equal(h.events.length, 1); assert.equal(h.counts().reads, 0); assert.equal(h.row.releaseApplicationId, undefined)
  assert.equal(h.runtime.releaseApplicationSubmittingId.value, 'new-task'); assert.equal(h.notices.length, 0)
})

test('actual release ETIMEDOUT without response recovers an advanced exact receipt', async () => {
  const h = releaseHarness()
  h.runtime.applyTeamLeaderActiveOrderRelease = async () => { throw { ...transportTimeout(), code: 'ETIMEDOUT' } }
  const original = h.runtime.getTeamLeaderActiveOrderRelease
  h.runtime.getTeamLeaderActiveOrderRelease = async id => ({ ...await original(id), status: 'RELEASED', version: 2 })
  await h.runtime.submitActiveOrderReleaseApplication(h.row)
  assert.equal(h.events.length, 1); assert.equal(h.row.releaseApplicationStatus, 'RELEASED'); assert.equal(h.counts().reads, 1)
})
test('actual release source identity uses lossless string Longs without conversion', () => {
  const h = releaseHarness()
  h.row.workOrderId = '9007199254740995'; h.row.routeId = '9007199254740997'; h.row.routeVersionId = '9007199254740999'
  const frozen = h.runtime.freezeActiveOrderReleaseRequest(h.row)
  assert.equal(frozen.workOrderId, h.row.workOrderId); assert.equal(frozen.routeId, h.row.routeId); assert.equal(frozen.routeVersionId, h.row.routeVersionId)
  assert.equal(Object.isFrozen(frozen), true)
})
test('actual release late business error cannot clear new context state', async () => {
  const h = releaseHarness(), write = deferred(); h.runtime.applyTeamLeaderActiveOrderRelease = () => write.promise
  const pending = h.runtime.submitActiveOrderReleaseApplication(h.row); await new Promise(setImmediate)
  h.route(); h.runtime.releaseApplicationSubmittingId.value = 'new-task'; h.runtime.releaseApplicationUncertainMessage.value = 'new-message'
  write.reject({ code: 500, message: 'audit rejected' }); await pending
  assert.equal(h.counts().reads, 0); assert.equal(h.events.length, 0); assert.equal(h.notices.length, 0)
  assert.equal(h.runtime.releaseApplicationSubmittingId.value, 'new-task'); assert.equal(h.runtime.releaseApplicationUncertainMessage.value, 'new-message')
})

test('actual deviation quick filter callback adapts load result to void and preserves rejection', async () => {
  let calls = 0, failure = false
  const runtime = { filterDefinitions: [], query: {}, load: async () => { calls++; if (failure) throw Error('load failed'); return true },
    useTableQuickFilter: (_key, _definitions, _query, reload) => ({ reload }) }
  execute(extract('edhr-deviation/index.vue', 'quickFilter', 'handleSortChange'), ['quickFilter'], runtime)
  assert.equal(await runtime.quickFilter.reload(), undefined); assert.equal(calls, 1)
  failure = true; await assert.rejects(runtime.quickFilter.reload(), /load failed/); assert.equal(calls, 2)
})
