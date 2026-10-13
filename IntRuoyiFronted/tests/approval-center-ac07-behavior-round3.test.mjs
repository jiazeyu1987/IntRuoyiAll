import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { stripTypeScriptTypes } from 'node:module'
import { runInNewContext } from 'node:vm'
import { webcrypto } from 'node:crypto'
import CryptoJS from 'crypto-js'

// Actual production setup functions; controlled ports are not browser or DB evidence.
const source = file => readFileSync(new URL(`../src/views/mes/pro/${file}`, import.meta.url), 'utf8')
const block = (text, start, end) => {
  const a = text.indexOf(start), b = text.indexOf(end, a)
  assert.ok(a >= 0 && b > a, `${start} -> ${end}`)
  return text.slice(a, b)
}
const execute = (code, names, runtime) => runInNewContext(stripTypeScriptTypes(code +
  `\nObject.assign(globalThis,{${names.join(',')}})`), runtime)
const ref = value => ({ value })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a,b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const tick = () => new Promise(setImmediate)
const timeout = () => ({ isAxiosError: true, code: 'ECONNABORTED', request: {}, message: 'timeout' })

const marketHarness = () => {
  const text = source('edhr-batch/BatchExecutionListPage.vue')
  let session = { key: 's1' }, writes = 0, captures = 0
  const events = [], notices = [], requests = []
  const context = { batchExecutionId: '11', releaseTransactionId: '21', releaseApprovalWorkTaskId: '31', releaseStatus: 'PENDING_APPROVAL', version: 3 }
  const runtime = {
    Map, Object, String, Number, JSON,
    route: { fullPath: '/market?batchExecutionId=11', query: {} },
    approvalInvalidation: {
      syncSession: () => session, isCurrent: key => session.key === key,
      captureEdhrSuccess: identity => { captures++; return { session: { ...session }, identity } },
      publishEdhrSuccess: event => events.push(event)
    },
    selectedReleaseBatch: ref({ id: '11' }), releaseContext: ref(context),
    releaseDialogVisible: ref(true), releaseLoading: ref(false), releaseContextLoading: ref(false),
    releaseCompletedReadOnly: ref(false), releaseTransactionMissing: ref(false), releaseError: ref(''),
    releaseForm: { password: 'original-password', idempotencyKey: 'original-key' },
    approveEdhrRelease: async (payload, options) => { writes++; requests.push({ payload, options }); return { ...context, releaseStatus: 'RELEASED' } },
    router: { push: async () => {} },
    message: { success: text => notices.push(text), warning: text => notices.push(text) },
    resolveErrorMessage: (error, description) => error?.message ?? description
  }
  execute(block(text, 'let marketReleaseRouteGeneration =', 'const readMarketReleaseRouteContext =') +
    block(text, 'const submitRelease =', 'const openActiveOrderOtherUploadTab ='),
    ['submitRelease', 'resetMarketReleaseDialog', 'marketReleaseWrites', 'marketReleaseWriteScope'], runtime)
  const navigate = mode => {
    if (mode === 'session') session = { key: 's2' }
    if (mode === 'route') runtime.route.fullPath = '/market?batchExecutionId=12'
    runtime.resetMarketReleaseDialog()
    runtime.releaseDialogVisible.value = true
    runtime.selectedReleaseBatch.value = { id: mode === 'route' ? '12' : '11' }
    runtime.releaseContext.value = mode === 'route' ? { ...context, batchExecutionId: '12', releaseTransactionId: '22', releaseApprovalWorkTaskId: '32' } : { ...context }
    runtime.releaseForm.password = 'new-password'; runtime.releaseForm.idempotencyKey = 'new-key'
    runtime.releaseError.value = 'new-error'
  }
  return { runtime, context, events, notices, requests, navigate, counts: () => ({ writes, captures }) }
}

for (const entry of ['route-batch', 'task-context', 'list-context']) {
  for (const change of ['route', 'session']) {
    for (const outcome of ['success', 'failure']) {
      test(`actual market ${entry} read after ${change} ${outcome} cannot alter new dialog`, async () => {
        const h = marketHarness(), wait = deferred()
        const taskContext = { batchExecutionId: '11', releaseTransactionId: '21', workTaskId: '31' }
        Object.assign(h.runtime, {
          userStore: { permissions: new Set(['mes:pro-edhr-release:approve']) },
          generateUUID: () => 'test-nonce', loadError: ref(''),
          isVoidedBatchExecutionStatus: () => false,
          getEdhrBatchExecution: async (id, options) => {
            assert.equal(id, '11'); assert.equal(options.ignoreErrorMessage, true); return wait.promise
          },
          getEdhrRelease: async (id, options) => {
            assert.equal(id, '21'); assert.equal(options.ignoreErrorMessage, true); return wait.promise
          },
          getEdhrReleasePage: async (params, options) => {
            assert.equal(options.ignoreErrorMessage, true); return wait.promise
          }
        })
        h.runtime.route.query = { action: 'marketRelease', ...taskContext }
        execute(block(source('edhr-batch/BatchExecutionListPage.vue'),
          'const readMarketReleaseRouteContext =', 'const openCompletedReleaseHistory ='),
          ['openMarketReleaseFromRoute', 'openReleaseDialog'], h.runtime)
        const pending = entry === 'route-batch' ? h.runtime.openMarketReleaseFromRoute() :
          h.runtime.openReleaseDialog({ id: '11', batchExecutionCode: 'batch11' },
            entry === 'task-context' ? taskContext : undefined)
        h.navigate(change)
        h.runtime.releaseContextLoading.value = true
        h.runtime.loadError.value = 'new-load-error'
        const newContext = h.runtime.releaseContext.value
        if (outcome === 'failure') wait.reject(Error('obsolete-read-error'))
        else wait.resolve(entry === 'route-batch' ? { id: '11' } :
          entry === 'task-context' ? { ...h.context } : { list: [{ ...h.context }] })
        await pending
        assert.equal(h.runtime.releaseContext.value, newContext)
        assert.equal(h.runtime.releaseContextLoading.value, true)
        assert.equal(h.runtime.releaseError.value, 'new-error')
        assert.equal(h.runtime.loadError.value, 'new-load-error')
        assert.equal(h.runtime.releaseForm.password, 'new-password')
        assert.equal(h.notices.length, 0)
      })
    }
  }
}

for (const entry of ['double Enter', 'Enter and button', 'button and Enter']) {
  test(`actual market release ${entry} has one write and one original capture`, async () => {
    const h = marketHarness(), wait = deferred(), original = h.runtime.approveEdhrRelease
    h.runtime.approveEdhrRelease = async (...args) => { const result = await original(...args); await wait.promise; return result }
    const first = h.runtime.submitRelease(), second = h.runtime.submitRelease()
    assert.deepEqual(h.counts(), { writes: 1, captures: 1 })
    assert.equal(h.events.length, 0)
    assert.equal(h.requests[0].options.ignoreErrorMessage, true)
    wait.resolve(); await Promise.all([first, second])
    assert.equal(h.events.length, 1)
    assert.equal(h.runtime.marketReleaseWrites.get(h.runtime.marketReleaseWriteScope(h.context)).status, 'CONFIRMED')
    h.runtime.releaseDialogVisible.value = true; h.runtime.releaseLoading.value = false
    h.runtime.releaseForm.password = 'entered-again'
    await h.runtime.submitRelease()
    assert.equal(h.counts().writes, 1); assert.equal(h.events.length, 1)
  })
}

for (const [kind, error, definitive] of [
  ['business', { code: 500, message: 'formal rejection' }, true],
  ['HTTP', { ...timeout(), response: { status: 503 } }, true],
  ['timeout', timeout(), false],
  ['network', { ...timeout(), code: 'ERR_NETWORK' }, false],
  ['plain', Error('unclassified failure'), false]
]) {
  test(`actual market release ${kind} preserves correct write state and visible failure`, async () => {
    const h = marketHarness(); let writes = 0
    h.runtime.approveEdhrRelease = async () => { writes++; throw error }
    await h.runtime.submitRelease()
    assert.equal(writes, 1); assert.equal(h.events.length, 0)
    assert.equal(h.runtime.releaseLoading.value, false)
    assert.ok(h.runtime.releaseError.value)
    const key = h.runtime.marketReleaseWriteScope(h.context)
    assert.equal(h.runtime.marketReleaseWrites.has(key), !definitive)
    if (!definitive) assert.equal(h.runtime.marketReleaseWrites.get(key).status, 'UNCERTAIN')
    await h.runtime.submitRelease()
    assert.equal(writes, definitive ? 2 : 1)
  })
}

for (const change of ['route', 'session', 'reopen']) {
  for (const outcome of ['success', 'failure']) {
    test(`actual old market ${outcome} after ${change} never clears the new write lock or UI`, async () => {
      const h = marketHarness(), old = deferred(), next = deferred()
      let writes = 0
      h.runtime.approveEdhrRelease = () => { writes++; return writes === 1 ? old.promise : next.promise }
      const first = h.runtime.submitRelease()
      h.navigate(change)
      const second = h.runtime.submitRelease()
      assert.equal(writes, change === 'reopen' ? 1 : 2)
      if (change === 'reopen') h.runtime.releaseLoading.value = true
      const key = h.runtime.marketReleaseWriteScope(h.runtime.releaseContext.value)
      const owned = h.runtime.marketReleaseWrites.get(key)
      if (outcome === 'success') old.resolve({ ...h.context, releaseStatus: 'RELEASED' })
      else old.reject({ code: 500, message: 'old business failure' })
      await first
      assert.equal(h.runtime.releaseLoading.value, true)
      assert.equal(h.runtime.releaseError.value, change === 'reopen' ? '本次正式事务已提交或结果尚未确认，请只读核对正式回执；禁止重复提交。' : '')
      assert.equal(h.runtime.releaseForm.password, 'new-password')
      assert.equal(h.notices.length, 0)
      if (change !== 'reopen') assert.equal(h.runtime.marketReleaseWrites.get(key), owned)
      assert.equal(h.events.length, outcome === 'success' ? 1 : 0)
      if (change !== 'reopen') next.reject({ code: 500, message: 'current business failure' })
      await second
    })
  }
}

const managerHarness = () => {
  const text = source('edhr-work-task/WorkTaskBoardPage.vue')
  let session = { key: 's1', loginUserId: '7' }, reads = 0, writes = 0
  const events = [], notices = [], requests = []
  const row = { id: '31', businessScopeId: '21' }
  let receipt = { releaseTransactionId: '21', releaseApprovalWorkTaskId: '31', batchExecutionId: '11', version: 3,
    releaseStatus: 'RELEASED', approvedBy: '7', approvedAt: '2026-10-07', approvalOpinion: 'original opinion',
    approvalIdempotencyKey: 'APPROVE-31', approvalSignoffEvidenceHash: 'formal-evidence' }
  const runtime = {
    Object, String, Number, Map, Set,
    route: { fullPath: '/manager?task=31', query: {} },
    approvalInvalidation: { syncSession: () => session, isCurrent: key => session.key === key,
      captureEdhrSuccess: identity => ({ session: { ...session }, identity }), publishEdhrSuccess: event => events.push(event) },
    managerReleaseDialogGeneration: 1, managerReleaseDialogVisible: ref(true), managerReleaseTask: ref(row),
    managerReleaseApprovalPreview: ref({ prepareId: 'prepare-31', content: { allowedActions: ['APPROVE'], subjectVersion: 'EDHR1:manager-version', contentHash: 'a'.repeat(64), contentPolicyVersion: 'EDHR_CONTENT_V1' } }), managerReleaseRequestKey: ref('review-31'),
    managerReleaseReceipt: ref({ ...receipt, releaseStatus: 'PENDING_APPROVAL' }), managerReleaseSubmitting: ref(false),
    managerReleaseForm: { signaturePassword: 'original-password', approvalOpinion: 'original opinion' },
    managerReleaseFormRef: ref({ validate: async () => {} }), managerReleaseBlockers: ref([]),
    managerReleaseUncertainMessage: ref(''), managerReleaseLockedTransactionIds: new Set(),
    getEdhrRelease: async (id, options) => { reads++; assert.equal(id, '21'); assert.equal(options.ignoreErrorMessage, true); return receipt },
    reviewApprovalTask: async (payload, options) => { writes++; requests.push({ payload, options }); assert.equal(payload.prepareId, 'prepare-31'); assert.equal(payload.idempotencyKey, 'review-31'); assert.equal(payload.expectedContentHash, 'a'.repeat(64)); return { receiptId: 'receipt-31', signatureId: 'signature-31', prepareId: 'prepare-31', idempotencyKey: 'review-31', evidenceHash: 'b'.repeat(64), resultCode: 'APPROVE', resultingStatus: 'RELEASED', committedAt: '2026-10-10T00:00:00Z' } },
    resolvePqcProductionReleaseFailure: () => undefined,
    requireJsonLongId: (id, description) => { if (!/^[1-9]\d*$/.test(String(id))) throw Error(description); return String(id) },
    requireNonNegativeVersion: value => { assert.ok(Number.isInteger(value) && value >= 0); return value },
    resolveManagerReleaseStatusLabel: value => value,
    resolveErrorMessage: (error, description) => error?.message ?? description,
    message: { error: text => notices.push(text), warning: text => notices.push(text), success: text => notices.push(text) },
    getList: async () => {}, loadError: ref(''), loadStage5ReleaseSnapshot: async () => {}
  }
  execute(block(text, 'const captureManagerReleaseContext =', 'watch(managerReleaseDialogVisible') +
    block(text, 'const requireManagerReleaseTaskContext =', 'const openManagerReleaseDialog =') +
    block(text, 'const isManagerReleaseTransportTimeout =', 'const loadStage5ReleaseSnapshot =') +
    block(text, 'const submitManagerReleaseApproval =', 'const openTask ='),
    ['submitManagerReleaseApproval', 'recoverUncertainManagerReleaseApproval'], runtime)
  const navigate = mode => {
    if (mode === 'session') session = { key: 's2', loginUserId: '8' }
    if (mode === 'route') runtime.route.fullPath = '/other'
    if (mode === 'dialog') { runtime.managerReleaseDialogGeneration++; runtime.managerReleaseDialogVisible.value = false }
    runtime.managerReleaseSubmitting.value = true; runtime.managerReleaseUncertainMessage.value = 'new-error'
    runtime.managerReleaseForm.signaturePassword = 'new-password'
  }
  return { runtime, row, events, notices, requests, navigate, counts: () => ({ reads, writes }), setReceipt: value => { receipt = value }, receipt: () => receipt }
}

for (const error of [{ code: 500, message: 'explicit rejection' }, { ...timeout(), response: { status: 403 } }]) {
  test(`actual manager explicit ${error.code} failure never reads or publishes historical RELEASED`, async () => {
    const h = managerHarness()
    h.runtime.reviewApprovalTask = async () => { throw error }
    h.setReceipt({ ...h.receipt(), approvedBy: '999', approvalIdempotencyKey: 'another-request' })
    await h.runtime.submitManagerReleaseApproval()
    assert.equal(h.counts().reads, 0); assert.equal(h.events.length, 0)
    assert.equal(h.runtime.managerReleaseLockedTransactionIds.size, 0)
    assert.ok(h.notices.length); assert.equal(h.runtime.managerReleaseSubmitting.value, false)
    await assert.rejects(h.runtime.recoverUncertainManagerReleaseApproval(h.row, error, () => true), /传输超时/)
  })
}

for (const variant of ['other-actor', 'other-key', 'different-opinion', 'different-signature', 'same-visible-request', 'read-failed']) {
  test(`actual manager timeout ${variant} lacks persisted unique attempt identity and stays uncertain`, async () => {
    const h = managerHarness(); let writes = 0
    h.runtime.reviewApprovalTask = async () => { writes++; throw timeout() }
    const receipt = { ...h.receipt() }
    if (variant === 'other-actor') receipt.approvedBy = '999'
    if (variant === 'other-key') receipt.approvalIdempotencyKey = 'another-request'
    if (variant === 'different-opinion') receipt.approvalOpinion = 'other opinion'
    if (variant === 'different-signature') receipt.approvalSignoffEvidenceHash = 'other signature'
    h.setReceipt(receipt)
    if (variant === 'read-failed') h.runtime.getEdhrRelease = async () => { throw Error('get failed') }
    await h.runtime.submitManagerReleaseApproval()
    assert.equal(h.events.length, 0); assert.equal(writes, 1)
    assert.equal(h.runtime.managerReleaseLockedTransactionIds.has('21'), true)
    assert.match(h.runtime.managerReleaseUncertainMessage.value, /不确定/)
    assert.equal(h.runtime.managerReleaseReceipt.value.releaseStatus, 'PENDING_APPROVAL')
    assert.equal(h.runtime.managerReleaseSubmitting.value, false)
    await h.runtime.submitManagerReleaseApproval(); assert.equal(writes, 1)
  })
}

test('actual manager true write publishes once and locks after receipt refresh fails', async () => {
  const h = managerHarness(); h.runtime.getEdhrRelease = async () => { throw Error('read failed') }
  await h.runtime.submitManagerReleaseApproval()
  assert.equal(h.events.length, 1); assert.equal(h.counts().writes, 1)
  assert.equal(h.requests[0].options.ignoreErrorMessage, true)
  assert.match(h.runtime.managerReleaseUncertainMessage.value, /已成功.*刷新失败/)
  await h.runtime.submitManagerReleaseApproval(); assert.equal(h.counts().writes, 1)
})

for (const mode of ['route', 'session', 'dialog']) {
  test(`actual manager obsolete timeout after ${mode} has zero notifications and UI mutations`, async () => {
    const h = managerHarness(), write = deferred()
    h.runtime.reviewApprovalTask = () => write.promise
    const pending = h.runtime.submitManagerReleaseApproval(); await tick()
    h.navigate(mode); write.reject(timeout()); await pending
    assert.equal(h.counts().reads, 0); assert.equal(h.events.length, 0); assert.equal(h.notices.length, 0)
    assert.equal(h.runtime.managerReleaseUncertainMessage.value, 'new-error')
    assert.equal(h.runtime.managerReleaseForm.signaturePassword, 'new-password')
    assert.equal(h.runtime.managerReleaseSubmitting.value, true)
    assert.equal(h.runtime.managerReleaseLockedTransactionIds.has('21'), true)
  })
}

const requestBoundary = () => {
  const serviceText = readFileSync(new URL('../src/config/axios/service.ts', import.meta.url), 'utf8')
  const requestText = readFileSync(new URL('../src/config/axios/index.ts', import.meta.url), 'utf8')
  const globalNotices = [], pending = [], callbacks = {}
  const service = async config => {
    const wait = deferred(); pending.push({ config, wait })
    const outcome = await wait.promise
    if (outcome.kind === 'transport') return callbacks.reject({ ...outcome.error, config })
    return callbacks.resolve({ data: outcome.data, config, headers: {}, request: {} })
  }
  service.interceptors = { response: { use: (resolve, reject) => Object.assign(callbacks, { resolve, reject }) } }
  const runtime = {
    service, Promise, Error, String, Object, Array,
    result_code: 200, ignoreMsgs: [], errorCode: { default: 'error' },
    useI18n: () => ({ t: text => text }), ApiEncrypt: { getEncryptHeader: () => 'encrypted' },
    ElMessage: { error: text => globalNotices.push(text), warning: text => globalNotices.push(text) },
    ElNotification: { error: text => globalNotices.push(text) }, console: { log: () => {} },
    config: { default_headers: 'application/json' }
  }
  execute(block(serviceText, 'const assertRequestContextCurrent =', 'const isBinaryAttachmentResponse =') +
    block(serviceText, 'const createApiError =', '// 创建axios实例') +
    block(serviceText, 'service.interceptors.response.use(', 'const refreshToken ='), [], runtime)
  execute(requestText.replace(/^import .*\r?\n/gm, '').replace('export default', 'globalThis.request ='), [], runtime)
  const wrapper = (path, name, next) => {
    const apiText = readFileSync(new URL(`../src/api/${path}`, import.meta.url), 'utf8')
    const apiRuntime = { ...runtime, request: runtime.request }
    const code = next ? block(apiText, `export const ${name} =`, `export const ${next} =`) : apiText.slice(apiText.indexOf(`export const ${name} =`))
    execute(code.replace(/^export /gm, ''), [name], apiRuntime)
    return apiRuntime[name]
  }
  return { pending, globalNotices, wrapper }
}

for (const page of ['market', 'manager']) {
  for (const kind of ['business', 'HTTP', 'timeout', 'network']) {
    for (const change of ['current', 'route', 'session']) {
      test(`actual ${page} handler -> API wrapper -> interceptor ${kind} after ${change}`, async () => {
        const boundary = requestBoundary(), h = page === 'market' ? marketHarness() : managerHarness()
        if (page === 'market') h.runtime.approveEdhrRelease = boundary.wrapper('mes/pro/edhr/release.ts', 'approveEdhrRelease', 'rejectEdhrRelease')
        else h.runtime.reviewApprovalTask = boundary.wrapper('approval-center/index.ts', 'reviewApprovalTask')
        const pending = page === 'market' ? h.runtime.submitRelease() : h.runtime.submitManagerReleaseApproval()
        await tick()
        assert.equal(boundary.pending.length, 1)
        const request = boundary.pending[0]
        assert.equal(request.config.ignoreErrorMessage, true)
        if (change !== 'current') h.navigate(change)
        if (kind === 'business') request.wait.resolve({ kind: 'business', data: { code: 500, msg: 'formal business rejection' } })
        else request.wait.resolve({ kind: 'transport', error: kind === 'HTTP'
          ? { ...timeout(), response: { status: 503 }, message: 'Request failed with status code 503' }
          : kind === 'network' ? { ...timeout(), code: 'ERR_NETWORK', message: 'Network Error' } : timeout() })
        await pending
        assert.equal(boundary.globalNotices.length, 0); assert.equal(h.events.length, 0)
        if (change === 'current') {
          if (page === 'market') assert.ok(h.runtime.releaseError.value)
          else assert.ok(h.notices.length)
        } else {
          assert.equal(h.notices.length, 0)
          if (page === 'market') { assert.equal(h.runtime.releaseError.value, 'new-error'); assert.equal(h.runtime.releaseForm.password, 'new-password') }
          else { assert.equal(h.runtime.managerReleaseUncertainMessage.value, 'new-error'); assert.equal(h.runtime.managerReleaseForm.signaturePassword, 'new-password') }
        }
      })
    }
  }
}

for (const kind of ['business', 'transport']) {
  test(`actual API default caller preserves global ${kind} failure`, async () => {
    const boundary = requestBoundary()
    const approve = boundary.wrapper('mes/pro/edhr/release.ts', 'approveEdhrRelease', 'rejectEdhrRelease')
    const pending = approve({ releaseTransactionId: '21', password: 'pw', idempotencyKey: 'key' })
    await tick()
    const request = boundary.pending[0]
    assert.equal(request.config.ignoreErrorMessage, undefined)
    if (kind === 'business') request.wait.resolve({ kind: 'business', data: { code: 500, msg: 'formal rejection' } })
    else request.wait.resolve({ kind: 'transport', error: timeout() })
    await assert.rejects(pending)
    assert.equal(boundary.globalNotices.length, 1)
  })
}

const pqcBoundaryHarness = boundary => {
  const text = source('production-release/PqcProductionReleasePage.vue')
  let session = { key: 's1', loginUserId: '7' }
  const events = [], notices = []
  const row = { applicationId: '11', pqcReleaseWorkTaskId: '31', version: 3 }
  const runtime = {
    Object, String, Number, Date, Map, Set, TextEncoder, Uint8Array, DataView, Array, CryptoJS,
    crypto: { getRandomValues: values => webcrypto.getRandomValues(values) },
    route: { fullPath: '/pqc?task=31' },
    approvalInvalidation: { syncSession: () => session, captureEdhrSuccess: identity => ({ session: { ...session }, identity }), publishEdhrSuccess: event => events.push(event) },
    releaseDialogGeneration: 1, selectedRow: ref(row), releaseDialogVisible: ref(true),
    releaseSubmitting: ref(false), releaseOutcomeUncertain: ref(false), releaseError: ref(''), releaseResult: ref(undefined),
    releaseForm: { signaturePassword: 'original-password', udiControlDocumentNo: 'UDI-01', approvalOpinion: 'original opinion' },
    resolveErrorMessage: (error, description) => error?.message ?? description,
    message: { warning: text => notices.push(text), success: text => notices.push(text) },
    approvePqcProductionRelease: boundary.wrapper('mes/pro/productionRelease/index.ts', 'approvePqcProductionRelease', 'rejectPqcProductionRelease'),
    getPqcProductionRelease: boundary.wrapper('mes/pro/productionRelease/index.ts', 'getPqcProductionRelease', 'getPqcProductionReleasePage')
  }
  execute(block(text, 'const releaseRequestOwner =', 'let listRequestSequence =') +
    block(text, 'const isExactPositiveId =', 'const openCompletedPqcReleaseDetail =') +
    block(text, 'const releaseRequestScopeKey =', 'const invalidatePqcReleasePage =') +
    block(text, 'const getOrCreateReleaseIdempotencyKey =', 'const openNonconformanceReview ='),
    ['submitRelease', 'releaseIdempotencyKeys', 'releaseRequestStates'], runtime)
  const navigate = change => {
    if (change === 'session') session = { key: 's2', loginUserId: '8' }
    if (change === 'route') runtime.route.fullPath = '/other'
    if (change === 'dialog') { runtime.releaseDialogGeneration++; runtime.releaseDialogVisible.value = false }
    runtime.releaseForm.signaturePassword = 'new-password'; runtime.releaseError.value = 'new-error'
    runtime.releaseSubmitting.value = true
  }
  return { runtime, events, notices, navigate }
}

for (const phase of ['write', 'recovery']) {
  for (const kind of ['business', 'transport']) {
    for (const change of ['current', 'route', 'session', 'dialog']) {
      test(`actual PQC ${phase} -> API wrapper -> interceptor ${kind} after ${change}`, async () => {
        const boundary = requestBoundary(), h = pqcBoundaryHarness(boundary)
        const pending = h.runtime.submitRelease(); await tick()
        assert.equal(boundary.pending.length, 1)
        if (phase === 'recovery') {
          boundary.pending[0].wait.resolve({ kind: 'transport', error: timeout() })
          await tick(); assert.equal(boundary.pending.length, 2)
        }
        const request = boundary.pending.at(-1)
        assert.equal(request.config.ignoreErrorMessage, true)
        if (change !== 'current') h.navigate(change)
        if (kind === 'business') request.wait.resolve({ kind: 'business', data: { code: 500, msg: 'formal business rejection' } })
        else request.wait.resolve({ kind: 'transport', error: { ...timeout(), code: 'ERR_NETWORK', message: 'Network Error' } })
        await pending
        assert.equal(boundary.globalNotices.length, 0); assert.equal(h.events.length, 0)
        if (change === 'current') assert.ok(h.runtime.releaseError.value)
        else {
          assert.equal(h.runtime.releaseError.value, 'new-error'); assert.equal(h.runtime.releaseForm.signaturePassword, 'new-password')
          assert.equal(h.runtime.releaseSubmitting.value, true); assert.equal(h.notices.length, 0)
        }
      })
    }
  }
}
