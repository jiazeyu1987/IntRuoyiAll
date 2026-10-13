import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'
import axios from 'axios'
import { createPinia, defineStore, setActivePinia } from 'pinia'
import { effectScope, watch, computed, ref as vueRef, reactive } from 'vue'
import { createFormalActionGuard, createFormalWriteReceiptState, createApprovalInvalidationState, completeApprovalWrite, createApprovalListLoader, createApprovalBadgeLoader } from '../src/store/modules/approvalCenterInvalidationState.ts'

// Only external network, credentials, encryption and UI ports are replaced.
// API wrapper, request adapter and both Axios interceptor callbacks are current production code.
const read = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const withoutImports = source => {
  const ast = ts.createSourceFile('source.ts', source, ts.ScriptTarget.Latest, true)
  for (const statement of [...ast.statements].reverse()) {
    if (ts.isImportDeclaration(statement)) source = source.slice(0, statement.getStart(ast)) + source.slice(statement.end)
  }
  return source
}
const run = (source, runtime) => runInNewContext(stripTypeScriptTypes(source), runtime)
const loadApi = (file, request) => {
  const source = withoutImports(read(file)).replace(/\bexport\s+(?=(?:const|type|interface))/g, '')
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const names = ast.statements.filter(ts.isVariableStatement).flatMap(statement => statement.declarationList.declarations)
    .filter(declaration => ts.isIdentifier(declaration.name) && declaration.initializer && ts.isArrowFunction(declaration.initializer))
    .map(declaration => declaration.name.text)
  const runtime = { request, Error, FormData, Blob, console }
  run(source + `\nObject.assign(globalThis, { ${names.join(',')} })`, runtime)
  return runtime
}
const extract = (file, names) => {
  let source = read(file)
  if (file.endsWith('.vue')) source = source.slice(source.indexOf('>', source.indexOf('<script')) + 1, source.indexOf('</script>'))
  const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
  const selected = ast.statements.filter(ts.isVariableStatement).filter(statement =>
    statement.declarationList.declarations.some(declaration => ts.isIdentifier(declaration.name) && names.includes(declaration.name.text)))
  assert.equal(selected.length, names.length, `${file}: all production declarations found`)
  return selected.map(statement => statement.getText(ast)).join('\n') + `\nObject.assign(globalThis, { ${names.join(',')} })`
}
const ref = value => ({ value })
const flush = () => new Promise(setImmediate)
const network = (ports = {}) => {
  const notices = [], pending = [], all = []
  const runtime = {
    axios, Error, Promise, Response, console: { log() {}, error() {} },
    config: { result_code: 200, base_url: '/test-only', request_timeout: 30000, default_headers: 'application/json' },
    qs: { stringify: () => '' }, getAccessToken: () => '', getRefreshToken: () => '', getTenantId: () => '1', getVisitTenantId: () => '',
    removeToken() {}, setRefreshedToken() {}, deleteUserCache() {},
    ApiEncrypt: { getEncryptHeader: () => 'x-test-encryption' },
    errorCode: { default: 'business failure' }, useI18n: () => ({ t: key => key }),
    ElMessage: { error: x => notices.push(x), warning: x => notices.push(x) },
    ElNotification: { error: x => notices.push(x) }, ElMessageBox: { confirm: () => Promise.reject(Error('not authorized in test')) },
    window: { location: { href: '/test', pathname: '/test', search: '' } },
    ...ports
  }
  const source = withoutImports(read('src/config/axios/service.ts')).replace(/import\.meta\.env\.VITE_APP_TENANT_ENABLE/g, '"false"')
    .replace(/export const /g, 'const ').replace(/export \{ service \}/, 'globalThis.service = service')
  run(source, runtime)
  runtime.service.defaults.adapter = config => new Promise((resolve, reject) => {
    const item = {
      config,
      success: data => resolve({ config, data: { code: 200, data }, headers: {}, status: 200, statusText: 'OK', request: {} }),
      unauthorized: () => resolve({ config, data: { code: 401, msg: 'expired access token' }, headers: {}, status: 200, statusText: 'OK', request: {} }),
      fail: kind => {
        if (kind === 'business' || kind === 'business500' || kind === 'business901') resolve({ config, data: { code: kind === 'business901' ? 901 : kind === 'business500' ? 500 : 1001, msg: 'formal business rejection' }, headers: {}, status: 200, statusText: 'OK', request: {} })
        else {
          const message = kind === 'timeout' ? 'timeout of 30000ms exceeded' : kind === 'http' ? 'Request failed with status code 503' : 'Network Error'
          const error = new axios.AxiosError(message, kind === 'timeout' ? 'ECONNABORTED' : kind === 'http' ? 'ERR_BAD_RESPONSE' : 'ERR_NETWORK', config, {})
          if (kind === 'http') error.response = { status: 503, data: { msg: 'formal HTTP failure' }, config }
          reject(error)
        }
      }
    }
    pending.push(item); all.push(item)
  })
  const adapter = { ...runtime, service: runtime.service }
  run(withoutImports(read('src/config/axios/index.ts')).replace('export default {', 'globalThis.request = {'), adapter)
  return {
    request: adapter.request, notices, all, runtime,
    next: async () => { for (let i = 0; i < 20 && !pending.length; i++) await flush(); assert.ok(pending.length, 'production wrapper dispatched through Axios'); return pending.shift() }
  }
}

const definitions = [
  ['src/api/approval-center/index.ts', 'reviewApprovalTask', [{}]],
  ['src/api/approval-center/index.ts', 'getApprovalCenterModules', []],
  ['src/api/approval-center/index.ts', 'getApprovalTaskPage', [{}]],
  ['src/api/approval-center/index.ts', 'getApprovalTaskTimeline', [{}]],
  ['src/api/mes/pro/productionRelease/index.ts', 'approvePqcProductionRelease', [{}]],
  ['src/api/mes/pro/productionRelease/index.ts', 'rejectPqcProductionRelease', [{}]],
  ['src/api/mes/pro/productionRelease/index.ts', 'getPqcProductionRelease', ['11']],
  ['src/api/mes/pro/productionRelease/index.ts', 'getPqcProductionReleasePage', [{}]],
  ['src/api/mes/pro/productionRelease/index.ts', 'getPqcProductionReleaseOrderDetail', ['11']],
  ['src/api/mes/pro/edhr/workTask.ts', 'completeEdhrCandidateSignatureTask', ['11', '12']],
  ['src/api/mes/pro/edhr/workTask.ts', 'getEdhrWorkTaskMyPage', [{}]],
  ['src/api/mes/pro/edhr/workTask.ts', 'getEdhrWorkTaskDonePage', [{}]],
  ['src/api/mes/pro/edhr/workTask.ts', 'getEdhrWorkTaskCandidateTodoPage', [{}]],
  ['src/api/mes/pro/edhr/workTask.ts', 'getEdhrWorkTaskStats', []],
  ['src/api/mes/pro/edhr/approval.ts', 'approveEdhrExecution', [{}]],
  ['src/api/mes/pro/edhr/approval.ts', 'rejectEdhrExecution', [{}]],
  ['src/api/mes/pro/edhr/approval.ts', 'getEdhrApprovalDetail', ['11', '12', undefined]],
  ['src/api/mes/pro/edhr/nonconformanceReview.ts', 'createCriticalDeviationReview', [{}]],
  ['src/api/mes/pro/edhr/nonconformanceReview.ts', 'getNonconformanceReview', ['11']],
  ['src/api/mes/pro/edhr/release.ts', 'submitEdhrRelease', [{}]],
  ['src/api/mes/pro/edhr/release.ts', 'approveEdhrRelease', [{}]],
  ['src/api/mes/pro/edhr/release.ts', 'rejectEdhrRelease', [{}]],
  ['src/api/mes/pro/edhr/release.ts', 'getEdhrRelease', ['11']],
  ['src/api/mes/pro/edhr/release.ts', 'getEdhrReleasePage', [{}]],
  ['src/api/mes/pro/edhr/release.ts', 'getEdhrReleaseCheckItemPage', [{}]],
  ['src/api/mes/pro/edhr/batchExecution.ts', 'getEdhrBatchExecution', ['11']],
  ['src/api/mes/pro/edhr/batchExecution.ts', 'getEdhrBatchActiveOrderDetail', [{}]],
  ['src/api/mes/pro/edhr/batchExecution.ts', 'getEdhrBatchWorkbench', ['11']],
  ['src/api/mes/pro/edhr/batchExecution.ts', 'getEdhrBatchTaskPreview', ['11', '12']],
  ['src/api/mes/pro/edhr/batchExecution.ts', 'getEdhrBatchReviewTimeline', ['11']],
  ['src/api/mes/pro/edhr/tracking.ts', 'getEdhrTrackingTimeline', [11]],
  ['src/api/mes/pro/edhr/signatures.ts', 'getEdhrExecutionSignaturePage', [{}]],
  ['src/api/mes/pro/edhr/deviation.ts', 'getDeviationPage', [{}]],
  ['src/api/mes/pro/edhr/deviation.ts', 'getDeviation', [11]],
  ['src/api/mes/pro/edhr/deviation.ts', 'getDeviationHandling', [11]]
]
for (const [file, name, args] of definitions) test(`real wrapper/interceptor rejects and preserves default notifications: ${name}`, async () => {
  for (const suppressed of [false, true]) for (const kind of ['business', 'business500', 'business901', 'http', 'timeout', 'network']) {
    const n = network(), api = loadApi(file, n.request)
    const promise = suppressed ? api[name](...args, { ignoreErrorMessage: true }) : api[name](...args)
    const rejection = assert.rejects(promise)
    const item = await n.next(); assert.equal(item.config.ignoreErrorMessage === true, suppressed)
    item.fail(kind); await rejection
    assert.equal(n.notices.length, suppressed ? 0 : 1, `${name}/${kind}/${suppressed}`)
  }
})

const badgeStoreHarness = (ports = {}) => {
  const authEffects = [], auth = { accessToken: 'session1-access', tenantId: '1' }
  const user = reactive({ getUser: { id: 7 } }), pinia = createPinia()
  setActivePinia(pinia)
  const n = network({ getAccessToken: () => auth.accessToken, getTenantId: () => auth.tenantId,
    removeToken: () => authEffects.push('remove-token'), setRefreshedToken: () => authEffects.push('set-token'), deleteUserCache: () => authEffects.push('delete-cache'),
    ElMessageBox: { confirm: () => { authEffects.push('confirm'); return Promise.resolve() } }, ...ports })
  const runtime = { defineStore, ref: vueRef, computed, watch, effectScope, Error, Object, String, Number, Date, JSON,
    createApprovalInvalidationState, createApprovalBadgeLoader, store: pinia, useUserStore: () => user,
    getAccessToken: () => auth.accessToken, getTenantId: () => auth.tenantId, getVisitTenantId: () => '',
    getApprovalTaskPage: loadApi('src/api/approval-center/index.ts', n.request).getApprovalTaskPage }
  const invalidationSource = withoutImports(read('src/store/modules/approvalCenterInvalidation.ts')).replace(/export const /g, 'const ')
  run(invalidationSource + '\nObject.assign(globalThis, { useApprovalCenterInvalidationStore })', runtime)
  const badgeSource = withoutImports(read('src/store/modules/approvalTodoBadge.ts')).replace(/export const /g, 'const ')
  run(badgeSource + '\nObject.assign(globalThis, { useApprovalTodoBadgeStore, resetBadgeLoader: () => badgeLoader.reset() })', runtime)
  const badge = runtime.useApprovalTodoBadgeStore(pinia), invalidation = runtime.useApprovalCenterInvalidationStore(pinia)
  badge.initializeScope()
  return { n, authEffects, badge, invalidation,
    change: kind => {
      if (kind === 'session') { user.getUser.id = 8; auth.accessToken = 'session2-access'; invalidation.syncSession() }
      else if (kind === 'tenant') { auth.tenantId = '2'; invalidation.syncSession() }
      else if (kind === 'revision') invalidation.publishEdhrSuccess(invalidation.captureEdhrSuccess({ sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '11' }))
      else if (kind === 'reset') runtime.resetBadgeLoader()
    }
  }
}

test('actual Pinia badge store forwards loader captured current predicate and coalesces same context', async () => {
  const h = badgeStoreHarness(), one = h.badge.refreshTodoTotal(), two = h.badge.refreshTodoTotal(), item = await h.n.next()
  assert.equal(item.config.ignoreErrorMessage, true)
  assert.equal(typeof item.config.isRequestContextCurrent, 'function'); assert.equal(item.config.isRequestContextCurrent(), true)
  assert.equal(h.n.all.length, 1)
  item.success({ list: [], total: 23 }); await Promise.all([one, two])
  assert.equal(h.badge.todoTotal, 23); assert.equal(h.badge.loaded, true); assert.equal(h.badge.error, ''); assert.equal(h.badge.loading, false)
  assert.ok(h.badge.lastLoadedAt > 0); assert.equal(h.n.notices.length, 0)
})

for (const kind of ['business500', 'business901', 'http', 'timeout', 'network']) test(`actual badge current ${kind} rejects and retains visible error`, async () => {
  const h = badgeStoreHarness(), promise = h.badge.refreshTodoTotal(), rejection = assert.rejects(promise), item = await h.n.next()
  item.fail(kind); await rejection
  assert.ok(h.badge.error); assert.equal(h.badge.loaded, false); assert.equal(h.badge.loading, false); assert.equal(h.badge.todoTotal, 0)
  assert.equal(h.n.notices.length, 0); assert.deepEqual(h.authEffects, [])
})

for (const context of ['session', 'tenant', 'revision', 'reset']) for (const kind of ['business500', 'business901', 'unauthorized', 'http', 'timeout', 'network', 'success']) {
  test(`actual badge old ${context}/${kind} cannot notify/authenticate or overwrite current badge`, async () => {
    const h = badgeStoreHarness(), old = h.badge.refreshTodoTotal(), oldItem = await h.n.next()
    h.change(context)
    assert.equal(oldItem.config.isRequestContextCurrent(), false)
    const current = h.badge.refreshTodoTotal(), newItem = await h.n.next()
    h.badge.error = 'current pending error'
    if (kind === 'unauthorized') oldItem.unauthorized(); else if (kind === 'success') oldItem.success({ list: [], total: 7 }); else oldItem.fail(kind)
    await old
    assert.equal(h.badge.loading, true); assert.equal(h.badge.error, 'current pending error'); assert.equal(h.badge.loaded, false)
    assert.equal(h.n.notices.length, 0); assert.deepEqual(h.authEffects, []); assert.equal(h.n.all.length, 2)
    newItem.success({ list: [], total: 91 }); await current
    assert.equal(h.badge.todoTotal, 91); assert.equal(h.badge.loaded, true); assert.equal(h.badge.loading, false); assert.equal(h.badge.error, '')
  })
}

test('actual badge current 401 rejects with visible error and normal current authentication', async () => {
  const h = badgeStoreHarness(), promise = h.badge.refreshTodoTotal(), rejection = assert.rejects(promise)
  ;(await h.n.next()).unauthorized(); await rejection; await flush()
  assert.ok(h.badge.error); assert.equal(h.badge.loading, false); assert.equal(h.badge.loaded, false)
  assert.deepEqual(h.authEffects, ['confirm', 'delete-cache', 'remove-token']); assert.equal(h.n.notices.length, 0)
})

for (const context of ['session', 'tenant', 'revision', 'reset']) for (const refreshFailure of [false, true]) {
  test(`actual badge delayed refresh after ${context}/failure=${refreshFailure} cannot change credentials or new badge`, async () => {
    let finishRefresh, failRefresh
    const refreshEffects = [], refreshResult = new Promise((resolve, reject) => { finishRefresh = resolve; failRefresh = reject })
    const h = badgeStoreHarness({ getRefreshToken: () => 'captured-refresh',
      axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => { refreshEffects.push('refresh'); return refreshResult } } })
    const old = h.badge.refreshTodoTotal(), oldItem = await h.n.next()
    oldItem.unauthorized(); await flush(); assert.deepEqual(refreshEffects, ['refresh'])
    h.change(context)
    const current = h.badge.refreshTodoTotal(), newItem = await h.n.next()
    newItem.success({ list: [], total: 93 }); await current
    const state = { ...h.badge.$state }
    if (refreshFailure) failRefresh(Error('refresh failed'))
    else finishRefresh({ data: { code: 200, data: { accessToken: 'late-access', refreshToken: 'late-refresh' } } })
    await old
    assert.deepEqual({ ...h.badge.$state }, state); assert.equal(h.n.all.length, 2)
    assert.deepEqual(h.authEffects, []); assert.equal(h.n.notices.length, 0)
  })
}

const handlerHarness = (target = 'center', n = network()) => {
  const notices = [], events = []
  let session = { key: 'session1', tenantId: '1', loginUserId: '7' }, key = 'route1:dialog1'
  const state = createApprovalInvalidationState(); state.setSession(session.key)
  const guard = createFormalActionGuard(() => JSON.stringify([session.key, key]))
  const inv = { syncSession: () => session, isCurrent: k => k === session.key,
    captureEdhrSuccess: identity => state.capture(session, identity),
    captureHandoffReceiptSuccess: (scope, type, id) => state.capture(scope, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) }),
    captureHandoffSourceSuccess: (type, id) => state.capture(session, { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: type, businessSourceId: String(id) }),
    publishEdhrSuccess: event => { events.push(event); state.publish(event) } }
  const runtime = { Error, Object, String, Date, Math, console, approvalInvalidation: inv,
    ElMessage: { error: x => notices.push(x), success: x => notices.push(x), warning: x => notices.push(x) },
    message: { error: x => notices.push(x), success: x => notices.push(x) },
    resolveErrorMessage: (error, text) => error?.response?.data?.msg || error?.message || text,
    ...loadApi('src/api/approval-center/index.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/approval.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/nonconformanceReview.ts', n.request),
    ...loadApi('src/api/mes/pro/edhr/release.ts', n.request),
    reviewPreview: ref({ prepareId: 'prepare-11', content: { allowedActions: ['APPROVE'], subjectVersion: 'EDHR1:test-version', contentHash: 'a'.repeat(64), contentPolicyVersion: 'EDHR_CONTENT_V1' } }), reviewIdempotencyKey: ref('review-11'), reviewPreviewError: ref(''),
    reviewSubmitting: ref(false), reviewTask: ref({ moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '11', businessKey: 'tx1', availableActions: ['APPROVE'] }),
    reviewForm: { result: 'APPROVE', reason: 'formal reason', signaturePassword: 'pw' }, reviewGeneration: 1,
    reviewDialogVisible: ref(true), canReview: () => true, approvalTaskIdentityKey: task => JSON.stringify(task),
    buildApprovalCenterRouteStateKey: () => key, completeApprovalWrite, getList: async () => true, loadError: ref(''),
    actionLoading: ref(false), actionError: ref(''), actionMode: ref('approve'), actionForm: { password: 'pw', comment: 'reason', rejectReason: 'reason' },
    detail: ref({ id: 11, executionId: 11, processInstanceId: 'bpm1', approvalSnapshotId: 12, approvalSnapshotHash: 'hash', bpmTaskId: 'task1', batchExecutionId: 4, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: true }),
    workTaskId: ref('11'), actionDialogVisible: ref(true), approvalActionGuard: guard, resolveActionDisabledReason: () => '', buildSignatureTimePayload: () => ({}),
    props: { readonly: false }, selectedCritical: ref([{ id: 11, batchExecutionId: 4 }]), isCriticalOpen: () => true,
    batchNcrLoading: ref(false), batchNcrVisible: ref(true), batchNcrGuard: guard, batchNcrForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '' },
    ncrCreateLoading: ref(false), ncrCreateVisible: ref(true), ncrCreateGuard: guard, ncrCreateError: ref(''), ncrCreateForm: { nonconformanceReason: 'reason', signaturePassword: 'pw', remark: '', idempotencyKey: 'request1' },
    releaseReturnSubmitting: ref(false), releaseReturnError: ref(''), releaseReturnDialogVisible: ref(true), releaseReturnGuard: guard,
    releaseReturnForm: { rejectReason: 'reason' }, workbench: ref({ releaseSummary: { releaseTransactionId: '11' } }),
    canReturnRelease: ref(true), ensureViewedReleaseStageWritable: () => true, buildReleaseIdempotencyKey: () => 'request1',
    loadDetail: async () => {}, load: async () => true
  }
  // API modules have their own UI-free VM globals; retain the handler UI ports here.
  runtime.Error = Error; runtime.ElMessage = { error: x => notices.push(x), success: x => notices.push(x), warning: x => notices.push(x) }
  const paths = {
    center: ['src/views/approval-center/index.vue', 'submitReview'],
    approve: ['src/views/mes/pro/edhr/ApprovalDetailPage.vue', 'submitAction'],
    ncrBatch: ['src/views/mes/pro/edhr-deviation/index.vue', 'submitBatchNcr'],
    ncrSingle: ['src/views/mes/pro/edhr-deviation/DeviationDetail.vue', 'submitNcrCreate'],
    releaseReturn: ['src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', 'submitReleaseReturn']
  }
  const [file, handler] = paths[target]; run(extract(file, [handler]), runtime)
  return { n, runtime, notices, events, state, handler,
    change: kind => {
      if (kind === 'session' || kind === 'tenant') { session = { key: 'session2', tenantId: kind === 'tenant' ? '2' : '1', loginUserId: '8' }; state.setSession(session.key) }
      else if (kind === 'route') key = 'route2:dialog1'
      else { runtime.reviewGeneration++; guard.invalidate() }
    }
  }
}
for (const target of ['center', 'approve', 'ncrBatch', 'ncrSingle', 'releaseReturn']) {
  for (const context of ['current', 'session', 'tenant', 'route', 'dialog']) for (const kind of ['business', 'http', 'timeout', 'network']) {
    test(`real wrapper/interceptor/production ${target} handler: ${context}/${kind}`, async () => {
      const h = handlerHarness(target), r = h.runtime
      const promise = r[h.handler](); const item = await h.n.next()
      if (context !== 'current') {
        h.change(context); r.actionError.value = 'new UI'; r.ncrCreateError.value = 'new UI'; r.releaseReturnError.value = 'new UI'
        r.reviewForm.signaturePassword = 'new password'; r.actionLoading.value = true; r.batchNcrLoading.value = true; r.ncrCreateLoading.value = true
      }
      item.fail(kind); await promise
      assert.equal(h.n.notices.length, 0, 'no global notification before guarded handler')
      assert.equal(h.events.length, 0, 'failures never publish success')
      if (context === 'current') {
        if (target === 'approve') assert.ok(r.actionError.value)
        else if (target === 'ncrSingle') assert.ok(r.ncrCreateError.value)
        else assert.equal(h.notices.length, 1)
      } else {
        assert.equal(h.notices.length, 0); assert.equal(r.actionError.value, 'new UI'); assert.equal(r.ncrCreateError.value, 'new UI'); assert.equal(r.releaseReturnError.value, 'new UI')
        assert.equal(r.reviewForm.signaturePassword, 'new password'); assert.equal(r.actionLoading.value, true); assert.equal(r.ncrCreateLoading.value, true); assert.equal(r.batchNcrLoading.value, true)
      }
    })
  }
}

for (const context of ['current', 'session', 'route', 'dialog']) for (const kind of ['business', 'http', 'timeout', 'network']) {
  test(`real wrapper/interceptor/production timeline read: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime
    Object.assign(r, { timelineTask: ref(null), timelineRows: ref([]), timelineError: ref(''), timelineDrawerVisible: ref(false), timelineLoading: ref(false), timelineGeneration: 0,
      isBpmProcessDetailOnly: () => false, hasTimelineCapability: () => true })
    run(extract('src/views/approval-center/index.vue', ['openTimeline']), r)
    const promise = r.openTimeline(r.reviewTask.value); const item = await h.n.next()
    if (context !== 'current') { h.change(context); if (context === 'dialog') r.timelineDrawerVisible.value = false; r.timelineError.value = 'new UI'; r.timelineRows.value = ['new row']; r.timelineLoading.value = true }
    item.fail(kind); await promise
    assert.equal(h.n.notices.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.timelineError.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.timelineError.value, 'new UI'); assert.deepEqual(r.timelineRows.value, ['new row']); assert.equal(r.timelineLoading.value, true) }
  })
}

test('production center success followed by real read failure publishes once and distinguishes refresh', async () => {
  const h = handlerHarness(), r = h.runtime
  r.getList = async () => { await r.getApprovalTaskPage({ pageNo: 1, pageSize: 10, viewType: 'TODO' }, { ignoreErrorMessage: true }); return true }
  const promise = r.submitReview(); (await h.n.next()).success({ receiptId: 'receipt-11', signatureId: 'signature-11', prepareId: 'prepare-11', idempotencyKey: 'review-11', evidenceHash: 'b'.repeat(64), resultCode: 'APPROVE', resultingStatus: 'RELEASED', committedAt: '2026-10-10T00:00:00Z' })
  const readRequest = await h.n.next(); assert.equal(h.events.length, 1); assert.equal(r.reviewDialogVisible.value, false)
  readRequest.fail('http'); await promise
  assert.equal(h.n.notices.length, 0); assert.equal(h.n.all.length, 2); assert.equal(h.events.length, 1)
  assert.ok(h.notices.some(message => String(message).includes('办理已成功，刷新失败')))
})

for (const context of ['current', 'session', 'route']) for (const kind of ['business', 'http', 'timeout', 'network']) {
  test(`production center list loader/read handler: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime
    Object.assign(r, { createApprovalListLoader, queryParams: { pageNo: 1, pageSize: 10, viewType: 'TODO', moduleCode: 'EDHR', keyword: '' },
      approvalTodoBadgeStore: { refreshTodoTotal: async () => {} }, list: ref([]), total: ref(0), loading: ref(false), listUiGeneration: 0,
      isUnfilteredTodoQuery: () => false })
    r.approvalInvalidation.revision = () => 0
    run(extract('src/views/approval-center/index.vue', ['approvalListLoader', 'getList']), r)
    const promise = r.getList(), item = await h.n.next()
    if (context !== 'current') { h.change(context); r.list.value = ['new row']; r.loadError.value = 'new UI'; r.loading.value = true }
    item.fail(kind); assert.equal(await promise, false)
    assert.equal(h.n.notices.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.loadError.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.loadError.value, 'new UI'); assert.deepEqual(r.list.value, ['new row']); assert.equal(r.loading.value, true) }
  })
}

for (const action of ['handleDispose', 'submitCreateReview']) for (const context of ['current', 'session', 'route', 'dialog']) for (const kind of ['business', 'http', 'timeout', 'network']) {
  test(`production QA ${action} including actual context watchers: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime, watchers = []
    Object.assign(r, { selectedReview: ref({ id: 11 }), selectedReviewReadOnly: ref(false), reviewDialogVisible: ref(true), createDialogVisible: ref(true),
      reviewContextGeneration: 0, createContextGeneration: 0, listRequestSequence: 0, materialContextGeneration: 0,
      disposeLoading: ref(false), createLoading: ref(false), listLoading: ref(false), activeOrderCandidatesLoading: ref(false), materialUploadsPending: ref(0),
      disposeForm: { reviewMaterialUrls: ['material'], reviewMaterialEvents: [], reviewOpinion: 'reason', signaturePassword: 'pw' },
      errorText: ref(''), buildReviewMaterials: () => [{ fileId: 12, url: 'material' }], selectedActiveOrderId: ref(4), entryForm: { nonconformanceReason: 'reason' },
      watch: (_getter, callback) => watchers.push(callback) })
    const file = 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'
    run(extract(file, ['invalidateCreateContext', 'invalidateReviewContext', action]), r)
    const source = read(file), script = source.slice(source.indexOf('>', source.indexOf('<script')) + 1, source.indexOf('</script>'))
    const ast = ts.createSourceFile(file, script, ts.ScriptTarget.Latest, true)
    const actualWatchers = ast.statements.filter(ts.isExpressionStatement).filter(statement =>
      ts.isCallExpression(statement.expression) && statement.expression.expression.getText(ast) === 'watch' &&
      /syncSession\(\)\.key|route\.fullPath/.test(statement.getText(ast)) && statement.getText(ast).includes('invalidateReviewContext()'))
    assert.equal(actualWatchers.length, 2)
    run(actualWatchers.map(statement => statement.getText(ast)).join('\n'), r)
    const promise = r[action]('concession_release'), item = await h.n.next()
    if (context !== 'current') {
      h.change(context)
      if (context === 'session') watchers[0]()
      else if (context === 'route') watchers[1]()
      else r.reviewContextGeneration++
      r.errorText.value = 'new UI'; r.disposeLoading.value = true; r.createLoading.value = true
    }
    item.fail(kind); await promise
    assert.equal(h.n.notices.length, 0); assert.equal(h.events.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.errorText.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.errorText.value, 'new UI'); assert.equal(r.disposeLoading.value, true); assert.equal(r.createLoading.value, true) }
  })
}

test('code901 obeys explicit suppression and still rejects', async () => {
  const n = network(), api = loadApi('src/api/approval-center/index.ts', n.request)
  const rejection = assert.rejects(api.reviewApprovalTask({}, { ignoreErrorMessage: true }))
  ;(await n.next()).fail('business901'); await rejection
  assert.equal(n.notices.length, 0)
})

test('obsolete 401 never requests relogin or clears current credentials', async () => {
  const authEffects = []
  let currentSession = 'session1'
  const n = network({
    ElMessageBox: { confirm: () => { authEffects.push(['confirm', currentSession]); return Promise.resolve() } },
    deleteUserCache: () => authEffects.push(['delete-cache', currentSession]),
    removeToken: () => authEffects.push(['remove-token', currentSession])
  })
  const h = handlerHarness('center', n), promise = h.runtime.submitReview(), item = await n.next()
  h.change('session'); currentSession = 'session2'; h.runtime.loadError.value = 'new UI'
  item.unauthorized(); await promise; await flush()
  assert.deepEqual(authEffects, [])
  assert.equal(n.notices.length, 0); assert.equal(h.notices.length, 0); assert.equal(h.events.length, 0)
  assert.equal(h.runtime.loadError.value, 'new UI')
})

test('obsolete 401 never refreshes or replays using current session credentials', async () => {
  const authEffects = []
  let currentSession = 'session1'
  const n = network({
    getAccessToken: () => `${currentSession}-access`, getRefreshToken: () => `${currentSession}-refresh`,
    setRefreshedToken: tokens => authEffects.push(['set-token', currentSession, tokens.accessToken]),
    axios: { create: axios.create, defaults: { headers: { common: {} } },
      post: async url => { authEffects.push(['refresh', currentSession, url]); return { data: { code: 200, data: { accessToken: 'refresh-result', refreshToken: 'refresh-result2' } } } } }
  })
  const h = handlerHarness('center', n), promise = h.runtime.submitReview(), item = await n.next()
  h.change('session'); currentSession = 'session2'; h.runtime.loadError.value = 'new UI'
  item.unauthorized()
  await promise
  assert.deepEqual(authEffects, [])
  assert.equal(n.all.length, 1)
  assert.equal(n.notices.length, 0); assert.equal(h.notices.length, 0)
  assert.equal(h.events.length, 0); assert.equal(h.state.revision('EDHR', 'TODO'), 0)
  assert.equal(h.runtime.loadError.value, 'new UI')
})

test('stale opt-in rejects before dispatch without reaching the network port', async () => {
  const n = network(), api = loadApi('src/api/approval-center/index.ts', n.request)
  await assert.rejects(api.reviewApprovalTask({}, { ignoreErrorMessage: true, isRequestContextCurrent: () => false }), { code: 'REQUEST_CONTEXT_EXPIRED' })
  assert.equal(n.all.length, 0); assert.equal(n.notices.length, 0)
})

for (const stale of [false, true]) test(`refresh await context recheck and queued replay: stale=${stale}`, async () => {
  let finishRefresh, current = true
  const effects = [], refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'original-refresh', getAccessToken: () => 'original-access',
    setRefreshedToken: tokens => effects.push(['set-token', tokens.accessToken]),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: async () => { effects.push(['refresh']); return refreshResult } } })
  const api = loadApi('src/api/approval-center/index.ts', n.request), options = { ignoreErrorMessage: true, isRequestContextCurrent: () => current }
  const first = api.reviewApprovalTask({}, options), second = api.reviewApprovalTask({}, options)
  const firstResult = first.then(value => ({ value }), error => ({ error })), secondResult = second.then(value => ({ value }), error => ({ error }))
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  if (stale) current = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'refreshed-access', refreshToken: 'refreshed-refresh' } } })
  if (!stale) { (await n.next()).success(true); (await n.next()).success(true) }
  const results = await Promise.all([firstResult, secondResult])
  if (stale) { assert.ok(results.every(x => x.error?.code === 'REQUEST_CONTEXT_EXPIRED')); assert.deepEqual(effects, [['refresh']]); assert.equal(n.all.length, 2) }
  else { assert.ok(results.every(x => x.value === true)); assert.deepEqual(effects, [['refresh'], ['set-token', 'refreshed-access']]); assert.equal(n.all.length, 4) }
  assert.equal(n.notices.length, 0)
})

test('unopted caller retains token refresh and replay', async () => {
  const effects = []
  const n = network({ getRefreshToken: () => 'refresh', getAccessToken: () => 'access', setRefreshedToken: () => effects.push('set-token'),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: async () => { effects.push('refresh'); return { data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } } } } } })
  const api = loadApi('src/api/approval-center/index.ts', n.request), promise = api.reviewApprovalTask({})
  ;(await n.next()).unauthorized(); (await n.next()).success(true)
  assert.equal(await promise, true); assert.deepEqual(effects, ['refresh', 'set-token']); assert.equal(n.all.length, 2)
})

test('refresh failure after context switch cannot trigger logout or replay', async () => {
  let failRefresh, current = true
  const effects = [], refreshResult = new Promise((_resolve, reject) => { failRefresh = reject })
  const n = network({ getRefreshToken: () => 'refresh', ElMessageBox: { confirm: () => { effects.push('confirm'); return Promise.resolve() } },
    removeToken: () => effects.push('remove-token'), setRefreshedToken: () => effects.push('set-token'),
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = loadApi('src/api/approval-center/index.ts', n.request)
  const promise = api.reviewApprovalTask({}, { isRequestContextCurrent: () => current }), rejection = assert.rejects(promise, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); current = false; failRefresh(Error('refresh failed')); await rejection
  assert.deepEqual(effects, []); assert.equal(n.all.length, 1)
})

test('stale queued request rejects while current refresh owner replays normally', async () => {
  let finishRefresh, queuedCurrent = true
  const refreshResult = new Promise(resolve => { finishRefresh = resolve })
  const n = network({ getRefreshToken: () => 'refresh', getAccessToken: () => 'access',
    axios: { create: axios.create, defaults: { headers: { common: {} } }, post: () => refreshResult } })
  const api = loadApi('src/api/approval-center/index.ts', n.request)
  const owner = api.reviewApprovalTask({}, { isRequestContextCurrent: () => true })
  const queued = api.reviewApprovalTask({}, { isRequestContextCurrent: () => queuedCurrent })
  const queuedRejection = assert.rejects(queued, { code: 'REQUEST_CONTEXT_EXPIRED' })
  ;(await n.next()).unauthorized(); await flush(); (await n.next()).unauthorized(); await flush()
  queuedCurrent = false
  finishRefresh({ data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh' } } })
  ;(await n.next()).success(true); assert.equal(await owner, true); await queuedRejection
  assert.equal(n.all.length, 3); assert.equal(n.notices.length, 0)
})

test('pending relogin confirmation cannot clear a later session; default caller still can', async () => {
  for (const optedIn of [false, true]) {
    let confirmLogin, current = true
    const effects = [], confirmation = new Promise(resolve => { confirmLogin = resolve })
    const n = network({ ElMessageBox: { confirm: () => { effects.push('confirm'); return confirmation } },
      removeToken: () => effects.push('remove-token'), deleteUserCache: () => effects.push('delete-cache') })
    const api = loadApi('src/api/approval-center/index.ts', n.request)
    const promise = api.reviewApprovalTask({}, optedIn ? { isRequestContextCurrent: () => current } : undefined)
    const rejection = assert.rejects(promise); (await n.next()).unauthorized(); await rejection
    current = false; confirmLogin(); await flush()
    assert.deepEqual(effects, optedIn ? ['confirm'] : ['confirm', 'delete-cache', 'remove-token'])
  }
})

for (const context of ['current', 'session', 'route']) for (const kind of ['business', 'timeout', 'network']) {
  test(`production center module read: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime
    Object.assign(r, { approvalModulesLoaded: false, modules: ref([]) })
    run(extract('src/views/approval-center/index.vue', ['loadModules']), r)
    const promise = r.loadModules(), item = await h.n.next()
    if (context !== 'current') { h.change(context); r.modules.value = ['new module']; r.loadError.value = 'new UI' }
    item.fail(kind); assert.equal(await promise, false)
    assert.equal(h.n.notices.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.loadError.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.loadError.value, 'new UI'); assert.deepEqual(r.modules.value, ['new module']) }
  })
}

for (const context of ['current', 'session', 'route', 'dialog']) for (const kind of ['business', 'http', 'timeout', 'network']) {
  test(`production batch release check refresh: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime
    Object.assign(r, { releaseCheckReadGeneration: 0, batchDetailRequestSerial: 1,
      releaseUiContextKey: () => JSON.stringify([r.approvalInvalidation.syncSession().key, r.buildApprovalCenterRouteStateKey()]),
      isStaleBatchDetailRequest: serial => serial !== r.batchDetailRequestSerial,
      releaseCheckItems: ref([]), releaseCheckLoading: ref(false), clearReleaseActionError: () => {}, showReleaseActionError: text => { r.releaseReturnError.value = text } })
    run(extract('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', ['loadReleaseCheckItems']), r)
    const promise = r.loadReleaseCheckItems(), item = await h.n.next()
    if (context !== 'current') { h.change(context); if (context === 'dialog') r.workbench.value = { releaseSummary: { releaseTransactionId: '22' } }; r.releaseCheckItems.value = ['new check']; r.releaseReturnError.value = 'new UI'; r.releaseCheckLoading.value = true }
    item.fail(kind); await promise
    assert.equal(h.n.notices.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.releaseReturnError.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.releaseReturnError.value, 'new UI'); assert.deepEqual(r.releaseCheckItems.value, ['new check']); assert.equal(r.releaseCheckLoading.value, true) }
  })
}

for (const context of ['current', 'session', 'route', 'dialog']) for (const kind of ['business', 'http', 'timeout', 'network']) {
  test(`production batch owner signature action: ${context}/${kind}`, async () => {
    const h = handlerHarness(), r = h.runtime, receipts = createFormalWriteReceiptState()
    Object.assign(r, { releaseSignatureSubmitting: ref(false), releaseSubmitReadPending: ref(false), releaseSignatureGuard: r.releaseReturnGuard,
      releaseActionContextKey: () => 'batch4', releaseSubmitReceipts: receipts, releaseSubmitReceiptRevision: ref(0), releaseSignatureError: ref(''), releaseSignatureConfirmVisible: ref(true),
      releaseTransactionForm: { password: 'pw', idempotencyKey: 'request1', approvalOpinion: 'reason' } })
    run(extract('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', ['runReleaseSignatureConfirmAction', 'submitReleaseByOwnerSignature']), r)
    const promise = r.runReleaseSignatureConfirmAction(() => r.submitReleaseByOwnerSignature('11'), 'success'), item = await h.n.next()
    if (context !== 'current') { h.change(context); r.releaseSignatureError.value = 'new UI'; r.releaseSignatureSubmitting.value = true }
    item.fail(kind); await promise
    assert.equal(h.n.notices.length, 0); assert.equal(h.events.length, 0)
    if (context === 'current') { assert.equal(h.notices.length, 1); assert.ok(r.releaseSignatureError.value) }
    else { assert.equal(h.notices.length, 0); assert.equal(r.releaseSignatureError.value, 'new UI'); assert.equal(r.releaseSignatureSubmitting.value, true) }
    assert.equal(receipts.isWriting('batch4'), false)
  })
}
