import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { createApprovalInvalidationState, createApprovalListLoader, completeApprovalWrite, createApprovalBadgeLoader, approvalTaskIdentityKey } from '../src/store/modules/approvalCenterInvalidationState.ts'

const session = (key = 'tenant1:user1:1') => ({ key, tenantId: '1', loginUserId: '1' })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
const fixture = () => {
  const state = createApprovalInvalidationState()
  let scope = session()
  state.setSession(scope.key)
  let routeKey = 'todo?module=EDHR&keyword=batch'
  let query = { viewType: 'TODO', moduleCode: 'EDHR', keyword: 'batch', pageNo: 3, pageSize: 10 }
  let calls = [], commits = [], badges = 0
  let request = async () => ({ list: ['task'], total: 35 })
  let badge = async () => { badges++ }
  const loader = createApprovalListLoader({
    context: () => ({ sessionKey: scope.key, routeKey, query: { ...query }, revision: state.revision(query.moduleCode, query.viewType) }),
    request: (q) => { calls.push({ ...q }); return request(q) },
    commit: (data, q) => { commits.push(data); query.pageNo = q.pageNo },
    refreshBadge: () => badge()
  })
  const publish = (identity = { sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '9001' }) => state.publish(state.capture(scope, identity))
  const activate = () => loader.isDirty() ? loader.load() : Promise.resolve('cached')
  return { state, loader, publish, activate, get query() { return query }, calls, commits, get badges() { return badges },
    request: (fn) => { request = fn }, badge: (fn) => { badge = fn }, route: (key) => { routeKey = key },
    switchSession: (key) => { scope = session(key); state.setSession(key) } }
}

test('official success freezes source identity; deduplicates; only EDHR TODO/DONE dirty', () => {
  const state = createApprovalInvalidationState(); state.setSession(session().key)
  const identity = { sourceTaskType: 'ACTIVE_ORDER_HANDOFF', businessSourceType: 'PQC_INSPECTION_TASK', businessSourceId: '123' }
  const event = state.capture(session(), identity); identity.businessSourceId = '999'
  assert.equal(event.identity.businessSourceId, '123'); assert.equal(event.moduleCode, 'EDHR')
  assert.equal(state.publish(event), true); assert.equal(state.publish(event), false)
  assert.equal(state.revision('EDHR', 'TODO'), 1); assert.equal(state.revision(undefined, 'DONE'), 1)
  for (const view of ['CC', 'MY_INITIATED']) assert.equal(state.revision('EDHR', view), 0)
  assert.equal(state.revision('DCC', 'TODO'), 0)
})
test('cancelled and failed writes never publish; successful write publishes once', async () => {
  let events = 0, successes = 0, reads = 0
  const options = { publish: () => events++, success: () => successes++, refresh: async () => reads++, refreshFailed: () => {} }
  await assert.rejects(completeApprovalWrite({ ...options, write: async () => { throw Error('cancel') } }))
  await assert.rejects(completeApprovalWrite({ ...options, write: async () => { throw Error('business failure') } }))
  assert.equal(events, 0)
  await completeApprovalWrite({ ...options, write: async () => true })
  assert.deepEqual([events, successes, reads], [1, 1, 1])
})
test('clean return zero requests; dirty activation coalesces and keeps filter/page', async () => {
  const f = fixture(); assert.equal(await f.activate(), 'cached'); assert.equal(f.calls.length, 0)
  f.publish(); const wait = deferred(); f.request(() => wait.promise)
  const a = f.activate(), b = f.activate(); assert.equal(a, b); assert.equal(f.calls.length, 1)
  wait.resolve({ list: [], total: 35 }); await a
  assert.equal(f.badges, 1); assert.equal(f.query.pageNo, 3); assert.equal(f.query.keyword, 'batch')
  await f.activate(); assert.equal(f.calls.length, 1)
})
test('new success during request remains dirty; request only consumes its captured revision', async () => {
  const f = fixture(); f.publish(); const wait = deferred(); f.request(() => wait.promise)
  const pending = f.loader.load(); f.publish(); wait.resolve({ list: [], total: 35 }); await pending
  assert.equal(f.loader.isDirty(), true)
  f.request(async () => ({ list: [], total: 35 })); await f.activate(); assert.equal(f.loader.isDirty(), false)
})
test('route and account switches discard old responses and errors', async () => {
  for (const switchContext of [f => f.route('done'), f => f.switchSession('tenant2:user2:1')]) {
    const f = fixture(); f.publish(); const wait = deferred(); f.request(() => wait.promise)
    const pending = f.loader.load(); switchContext(f); wait.resolve({ list: ['old'], total: 35 })
    assert.equal(await pending, 'stale'); assert.equal(f.commits.length, 0)
  }
  const f = fixture(); const wait = deferred(); f.request(() => wait.promise)
  const pending = f.loader.load(); f.route('away'); wait.reject(Error('old failure'))
  assert.equal(await pending, 'stale')
})
test('list or badge failure preserves dirty; completed write reports success and refresh failure', async () => {
  for (const failure of ['list', 'badge']) {
    const f = fixture(); const messages = []
    if (failure === 'list') f.request(async () => { throw Error('list unavailable') })
    else f.badge(async () => { throw Error('badge unavailable') })
    await completeApprovalWrite({ write: async () => true, publish: () => f.publish(), success: () => messages.push('success'),
      refresh: () => f.loader.load(), refreshFailed: error => messages.push(`refresh failed:${error.message}`) })
    assert.equal(f.loader.isDirty(), true); assert.equal(f.commits.length, 0)
    assert.deepEqual(messages, ['success', `refresh failed:${failure} unavailable`])
  }
})
test('page shrink loads last valid page once; zero rows use page one', async () => {
  for (const total of [11, 0]) {
    const f = fixture(); f.request(async () => ({ list: [], total })); await f.loader.load()
    assert.equal(f.calls.length, 2); assert.equal(f.query.pageNo, total ? 2 : 1)
    assert.equal(f.calls[1].keyword, 'batch'); assert.equal(f.calls[1].moduleCode, 'EDHR')
  }
})
test('logout clears revisions and rejects old success even after same-account login', () => {
  const state = createApprovalInvalidationState(); state.setSession(session().key)
  const event = state.capture(session(), { sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '1' }); state.publish(event)
  state.setSession('logout:2'); assert.equal(state.revision('EDHR', 'TODO'), 0)
  state.setSession('tenant1:user1:3'); assert.equal(state.publish(event), false)
})
test('badge old request cannot overwrite new account or new success revision', async () => {
  let key = 'user1:rev0'; const waits = [deferred(), deferred()]; let calls = 0, committed = [], errors = []
  const badge = createApprovalBadgeLoader({ contextKey: () => key, request: () => waits[calls++].promise,
    commit: total => committed.push(total), start: () => {}, fail: error => errors.push(error.message), finish: () => {} })
  const old = badge.load(); assert.equal(old, badge.load())
  key = 'user2:rev1'; const current = badge.load(); waits[1].resolve(2); await current
  waits[0].resolve(99); await old; assert.deepEqual(committed, [2]); assert.deepEqual(errors, [])
})
test('deactivation invalidates pending request; reset allows new session load', async () => {
  const f = fixture(); const wait = deferred(); f.request(() => wait.promise)
  const old = f.loader.load(); f.loader.invalidate(); wait.resolve({ list: ['old'], total: 35 })
  assert.equal(await old, 'stale'); assert.equal(f.commits.length, 0)
  f.loader.reset(); f.request(async () => ({ list: ['new'], total: 35 })); await f.loader.load()
  assert.equal(f.commits.length, 1)
})

// Execute the production Vue setup handler with its runtime dependencies supplied.
// No copied handler implementation or source-keyword assertion substitutes for behavior.
const vueHandler = (file, name, nextName, dependencies) => {
  const source = readFileSync(new URL(`../src/views/${file}`, import.meta.url), 'utf8')
  const start = source.indexOf(`const ${name} =`)
  const end = source.indexOf(nextName === 'watch' ? '\nwatch(' : `const ${nextName} =`, start)
  assert.ok(start >= 0 && end > start)
  const context = { ...dependencies }
  runInNewContext(stripTypeScriptTypes(source.slice(start, end) + `\nglobalThis.handler = ${name}`), context)
  context.handler.runtime = context
  return context.handler
}
test('actual candidate signature handler: cancel/fail no event; success publishes; refresh error stays read error', async () => {
  for (const outcome of ['cancel', 'write-fail', 'success', 'read-fail']) {
    let published = 0, writes = 0; const notices = []
    const event = { session: { key: 'user1' } }
    const taskSource = readFileSync(new URL('../src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue', import.meta.url), 'utf8')
    const identityStart = taskSource.indexOf('const candidateSignatureTaskIdentity =')
    const identityEnd = taskSource.indexOf('const handleCompleteCandidateSignature =', identityStart)
    const identity = runInNewContext(stripTypeScriptTypes(taskSource.slice(identityStart, identityEnd) + '\ncandidateSignatureTaskIdentity'))
    const handler = vueHandler('mes/pro/edhr-work-task/WorkTaskBoardPage.vue', 'handleCompleteCandidateSignature', 'initializeRouteFilters', {
      candidateSignatureWrites: new Set(), candidateSignatureGeneration: 0,
      candidateSignatureTaskIdentity: identity, candidateSignatureContextKey: () => '/edhr/work-task?taskType=REVIEW:session:1',
      route: { fullPath: '/edhr/work-task?taskType=REVIEW' },
      approvalInvalidation: { syncSession: () => ({ key: 'user1' }), captureEdhrSuccess: () => event, isCurrent: () => true, publishEdhrSuccess: () => published++ },
      completeApprovalWrite,
      message: { confirm: async () => { if (outcome === 'cancel') throw 'cancel' }, success: text => notices.push(text), warning: text => notices.push(text), error: text => notices.push(text) },
      completeEdhrCandidateSignatureTask: async () => { writes++; if (outcome === 'write-fail') throw Error('write failed'); return true },
      getList: async () => {}, loadError: { value: outcome === 'read-fail' ? 'list unavailable' : '' },
      resolveErrorMessage: error => error.message
    })
    await handler({ id: 1, executionId: 2, processName: 'review' })
    assert.equal(published, ['success', 'read-fail'].includes(outcome) ? 1 : 0)
    assert.equal(writes, outcome === 'cancel' ? 0 : 1)
    if (outcome === 'read-fail') assert.deepEqual(notices, ['签名完成', '签名已成功，列表刷新失败：list unavailable'])
  }
})
test('actual center submit handler freezes request, publishes once, and distinguishes read failure', async () => {
  for (const outcome of ['write-fail', 'success', 'read-fail']) {
    const notices = []; let published = 0, request
    const event = { session: { key: 'session' } }
    const reviewTask = { value: { moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '12', availableActions: ['APPROVE'] } }
    const reviewForm = { result: 'APPROVE', reason: ' original opinion ', signaturePassword: 'pw' }
    const reviewDialogVisible = { value: true }, reviewSubmitting = { value: false }
    const handler = vueHandler('approval-center/index.vue', 'submitReview', 'normalizeApprovalDisplayText', {
      reviewTask, reviewForm, reviewDialogVisible, reviewSubmitting, canReview: () => true,
      reviewGeneration: 1, approvalTaskIdentityKey,
      approvalInvalidation: { syncSession: () => ({ key: 'session' }), captureEdhrSuccess: () => event, isCurrent: () => true, publishEdhrSuccess: () => published++ },
      completeApprovalWrite, buildApprovalCenterRouteStateKey: () => 'todo',
      reviewApprovalTask: async payload => { request = payload; reviewForm.reason = 'changed while awaiting'; if (outcome === 'write-fail') throw Error('business failed'); return true },
      getList: async () => outcome !== 'read-fail', loadError: { value: 'read unavailable' },
      resolveErrorMessage: error => error.message,
      ElMessage: { error: text => notices.push(text), success: text => notices.push(text), warning: text => notices.push(text) }
    })
    await handler()
    assert.equal(request.reason, 'original opinion'); assert.equal(published, outcome === 'write-fail' ? 0 : 1)
    assert.equal(reviewSubmitting.value, false)
    if (outcome === 'read-fail') { assert.deepEqual(notices, ['审核已通过', '办理已成功，刷新失败：read unavailable']); assert.equal(reviewDialogVisible.value, false); assert.equal(reviewForm.signaturePassword, '') }
  }
})
test('actual QA dispose handler captures official source before await even after route switches', async () => {
  let captured, published; const selectedReview = { value: { id: 31 } }
  const handler = vueHandler('mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue', 'handleDispose', 'watch', {
    disposeLoading: { value: false }, reviewDialogVisible: { value: true }, selectedReviewReadOnly: { value: false }, selectedReview,
    reviewContextGeneration: 1, materialUploadsPending: { value: false },
    disposeForm: { reviewMaterialUrls: ['material'], reviewMaterialEvents: [], reviewOpinion: 'qa opinion', signaturePassword: 'pw' },
    errorText: { value: '' }, buildReviewMaterials: () => [],
    approvalInvalidation: { syncSession: () => ({ key: 'user1' }), captureHandoffSourceSuccess: (type, id) => { captured = { type, id, session: { key: 'user1' } }; return captured }, publishEdhrSuccess: event => { published = event }, isCurrent: () => true },
    disposeNonconformanceReview: async () => { selectedReview.value = { id: 99 }; return { id: 31 } },
    message: { error: () => {}, success: () => {} }, loadReviews: async () => {}, resolveDispositionLabel: text => text,
    resolveErrorMessage: error => error.message, watch: () => {}
  })
  await handler('rework')
  assert.deepEqual(captured, { type: 'NONCONFORMANCE_REVIEW', id: 31, session: { key: 'user1' } }); assert.equal(published, captured)
  assert.equal(selectedReview.value.id, 99)
})

test('actual session adapter isolates tenant/user and clears logout before same-account login', () => {
  let access = true, tenant = 1, visitTenant
  const user = { getUser: { id: 10 } }, watches = []
  const source = readFileSync(new URL('../src/store/modules/approvalCenterInvalidation.ts', import.meta.url), 'utf8').replace(/^import .*\r?\n/gm, '')
  const context = {
    createApprovalInvalidationState, defineStore: (_name, setup) => setup,
    useUserStore: () => user,
    getAccessToken: () => access, getTenantId: () => tenant, getVisitTenantId: () => visitTenant,
    ref: value => ({ value }), computed: getter => ({ get value() { return getter() } }),
    watch: (_getter, callback) => watches.push(callback)
  }
  runInNewContext(stripTypeScriptTypes(source.replace('export const ', 'const ') + '\nglobalThis.useStore = useApprovalCenterInvalidationStore'), context)
  const store = context.useStore()
  const first = store.captureEdhrSuccess({ sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '1' })
  store.publishEdhrSuccess(first); assert.equal(store.revision('EDHR', 'TODO'), 1)
  visitTenant = 2; assert.equal(store.isCurrent(first.session.key), false); assert.equal(store.revision('EDHR', 'TODO'), 0)
  const second = store.captureHandoffSourceSuccess('ACTIVE_ORDER', 100)
  access = false; user.getUser.id = 0; watches.forEach(callback => callback())
  assert.equal(store.revision('EDHR', 'TODO'), 0); assert.equal(store.isCurrent(second.session.key), false)
  access = true; user.getUser.id = 10; visitTenant = undefined; watches.forEach(callback => callback())
  store.publishEdhrSuccess(first); assert.equal(store.revision('EDHR', 'TODO'), 0)
  const oldReceipt = store.captureHandoffReceiptSuccess(first.session, 'ACTIVE_ORDER', 100)
  store.publishEdhrSuccess(oldReceipt); assert.equal(store.revision('EDHR', 'TODO'), 0)
  assert.equal(oldReceipt.session.key, first.session.key)
  const current = store.captureHandoffSourceSuccess('ACTIVE_ORDER', 100); store.publishEdhrSuccess(current)
  assert.equal(store.revision('EDHR', 'DONE'), 1)
  assert.throws(() => store.captureHandoffSourceSuccess('ACTIVE_ORDER', undefined), /正式账号、租户或任务身份/)
})

test('actual center old submit cannot close a new same-ID task from another source/module', async () => {
  const wait = deferred(), messages = []; let published = 0
  const task = { moduleCode: 'EDHR', sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: '12', availableActions: ['APPROVE'] }
  const reviewTask = { value: task }, reviewForm = { result: 'APPROVE', reason: 'opinion', signaturePassword: 'pw' }
  const reviewDialogVisible = { value: true }, reviewSubmitting = { value: false }
  const handler = vueHandler('approval-center/index.vue', 'submitReview', 'normalizeApprovalDisplayText', {
    reviewTask, reviewForm, reviewDialogVisible, reviewSubmitting, reviewGeneration: 1, approvalTaskIdentityKey,
    canReview: () => true, completeApprovalWrite, buildApprovalCenterRouteStateKey: () => 'todo',
    approvalInvalidation: { syncSession: () => ({ key: 'user1' }), captureEdhrSuccess: () => ({}), isCurrent: () => true, publishEdhrSuccess: () => published++ },
    reviewApprovalTask: () => wait.promise, getList: async () => true, loadError: { value: '' },
    resolveErrorMessage: error => error.message,
    ElMessage: { success: text => messages.push(text), error: text => messages.push(text), warning: text => messages.push(text) }
  })
  const pending = handler(); assert.equal(reviewSubmitting.value, true)
  // Route/reset and new-dialog paths synchronously advance their generation.
  handler.runtime.reviewGeneration++
  reviewSubmitting.value = false
  reviewTask.value = { ...task, moduleCode: 'DCC', sourceTaskType: 'DCC_CONTROLLED_FILE_TASK' }
  reviewForm.signaturePassword = 'new dialog password'
  reviewSubmitting.value = true
  wait.resolve(true); await pending
  assert.equal(published, 1); assert.deepEqual(messages, [])
  assert.equal(reviewDialogVisible.value, true); assert.equal(reviewSubmitting.value, true)
  assert.equal(reviewForm.signaturePassword, 'new dialog password')
})
test('actual reset/open-dialog path clears stale submit loading and increments request generation', () => {
  const dependencies = { reviewGeneration: 1, reviewSubmitting: { value: true }, reviewForm: { signaturePassword: 'old password' },
    reviewTask: { value: undefined }, reviewDialogVisible: { value: true }, canReview: () => true,
    ElMessage: { error: () => {} }, watch: () => {} }
  const source = readFileSync(new URL('../src/views/approval-center/index.vue', import.meta.url), 'utf8')
  const start = source.indexOf('const resetReviewRequestContext =')
  const end = source.indexOf('const submitReview =', start)
  runInNewContext(stripTypeScriptTypes(source.slice(start, end) + '\nglobalThis.open = openReviewDialog; globalThis.reset = resetReviewRequestContext'), dependencies)
  dependencies.reset()
  assert.equal(dependencies.reviewGeneration, 2); assert.equal(dependencies.reviewSubmitting.value, false); assert.equal(dependencies.reviewForm.signaturePassword, '')
  dependencies.reviewSubmitting.value = true
  dependencies.open({ moduleCode: 'EDHR', sourceTaskType: 'ACTIVE_ORDER_HANDOFF', sourceTaskId: '12' })
  assert.equal(dependencies.reviewGeneration, 3); assert.equal(dependencies.reviewSubmitting.value, false)
  assert.equal(dependencies.reviewTask.value.sourceTaskType, 'ACTIVE_ORDER_HANDOFF')
})

test('actual route-return function retains clean cache and coalesces dirty refresh without resetting query', async () => {
  const source = readFileSync(new URL('../src/views/approval-center/index.vue', import.meta.url), 'utf8')
  const keepStart = source.indexOf('const shouldKeepApprovalCenterLoadedStateOnRouteReturn =')
  const keepEnd = source.indexOf('const isApprovalTaskViewType =', keepStart)
  const loadStart = source.indexOf('const loadRouteQuery =')
  const loadEnd = source.indexOf('const handleQuery =', loadStart)
  let dirty = false, reads = 0
  const wait = deferred()
  const context = {
    approvalModulesLoaded: true, approvalLastLoadedRouteStateKey: 'todo', loading: { value: false }, loadError: { value: '' },
    approvalListLoader: { isDirty: () => dirty }, buildApprovalCenterRouteStateKey: () => 'todo',
    approvalInvalidation: { syncSession: () => ({ key: 'user1' }), isCurrent: () => true },
    queryParams: { viewType: 'TODO', pageNo: 3, pageSize: 10, moduleCode: 'EDHR', keyword: 'batch' },
    applyRouteQuery: () => {}, syncRouteToCanonicalPath: () => {}, syncApprovalQuickFilterStateFromQuery: async () => {},
    getList: async () => { reads++; await wait.promise; dirty = false }, loadModules: async () => {},
    list: { value: [] }, total: { value: 0 }, resolveErrorMessage: error => error.message, ElMessage: { error: () => {} },
    routeLoadPromise: undefined, routeLoadKey: undefined
  }
  runInNewContext(stripTypeScriptTypes(source.slice(keepStart, keepEnd) + '\n' + source.slice(loadStart, loadEnd) + '\nglobalThis.activate = applyRouteQueryAndLoad'), context)
  await context.activate(); assert.equal(reads, 0)
  dirty = true
  const first = context.activate(), second = context.activate(); assert.equal(first, second)
  await new Promise(setImmediate); assert.equal(reads, 1)
  wait.resolve(); await first; await context.activate(); assert.equal(reads, 1)
  assert.equal(context.queryParams.pageNo, 3); assert.equal(context.queryParams.keyword, 'batch')
})

test('actual badge synchronous request throw preserves original error, clears pending and permits read retry', async () => {
  let calls = 0, starts = 0, finishes = 0, failed, committed
  const original = Error('synchronous transport failure')
  const loader = createApprovalBadgeLoader({ contextKey: () => 'session1', request: () => { calls++; if (calls === 1) throw original; return Promise.resolve(4) },
    start: () => starts++, finish: () => finishes++, fail: e => { failed = e }, commit: total => { committed = total } })
  const pending = loader.load(); assert.equal(pending, loader.load())
  await assert.rejects(pending, error => error === original)
  assert.equal(failed, original); assert.equal(finishes, 1)
  await loader.load(); assert.equal(calls, 2); assert.equal(starts, 2); assert.equal(finishes, 2); assert.equal(committed, 4)
})
test('actual badge old cleanup cannot clear a newer pending read after reset', async () => {
  const old = deferred(), current = deferred(); let key = 's1', calls = 0, finishes = 0
  const loader = createApprovalBadgeLoader({ contextKey: () => key, request: () => calls++ === 0 ? old.promise : current.promise,
    start: () => {}, finish: () => finishes++, fail: () => {}, commit: () => {} })
  const first = loader.load(); key = 's2'; loader.reset(); const second = loader.load()
  old.resolve(1); await first
  assert.equal(second, loader.load()); assert.equal(calls, 2); assert.equal(finishes, 0)
  current.resolve(2); await second; assert.equal(finishes, 1)
})
