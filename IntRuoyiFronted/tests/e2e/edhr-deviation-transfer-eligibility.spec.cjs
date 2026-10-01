const { test } = require('node:test')
const assert = require('node:assert/strict'),
  fs = require('node:fs'),
  vm = require('node:vm'),
  ts = require('typescript'),
  path = require('node:path')
const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/DeviationDetail.vue'),
  'utf8'
)
function harness(options = {}) {
  const calls = []
  const errors = []
  const props = { id: 1, readonly: false }
  const context = {
    ref: (value) => ({ value }),
    reactive: (value) => value,
    computed: (fn) => ({
      get value() {
        return fn()
      }
    }),
    watch: () => {},
    useRouter: () => ({}),
    useMessage: () => ({ confirm: async () => {} }),
    defineProps: () => props,
    withDefaults: (v) => v,
    getDeviation:
      options.getDeviation ||
      (async () => ({ id: 1, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: true })),
    ElMessage: { warning: () => {}, error: (message) => errors.push(message) },
    createCriticalDeviationReview: async (body) => calls.push(body)
  }
  let script = source
    .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*$/gm, '')
  script +=
    '\nglobalThis.api={props,invalidateLoad:()=>{loadSequence++},detail,ncrCreateVisible,ncrCreateForm,openNcrCreateDialog,submitNcrCreate,ncrCreateError,startHandling,handling,handlingForm,refreshTransferEligibility:typeof refreshTransferEligibility === "undefined" ? undefined : refreshTransferEligibility};'
  vm.runInNewContext(
    ts.transpileModule(script, {
      compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.None }
    }).outputText,
    context
  )
  return { ...context.api, calls, errors }
}
test('server disallowed transfer cannot open or submit but investigation remains usable', async () => {
  const h = harness()
  h.detail.value = {
    id: 1,
    batchExecutionId: 2,
    status: 'OPEN',
    level: 'CRITICAL',
    canTransferToNcr: false,
    transferToNcrBlockedReason: '已返工；请按常规关闭'
  }
  h.openNcrCreateDialog()
  assert.equal(h.ncrCreateVisible.value, false)
  h.ncrCreateForm.nonconformanceReason = 'reason'
  h.ncrCreateForm.signaturePassword = 'password'
  await h.submitNcrCreate()
  assert.equal(h.calls.length, 0)
  assert.match(h.ncrCreateError.value, /常规/)
  h.startHandling()
  assert.equal(h.handling.value.id, 0)
})
test('refreshed allowed transfer can open dialog', () => {
  const h = harness()
  h.detail.value = { id: 1, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: false }
  h.openNcrCreateDialog()
  assert.equal(h.ncrCreateVisible.value, false)
  h.detail.value.canTransferToNcr = true
  h.openNcrCreateDialog()
  assert.equal(h.ncrCreateVisible.value, true)
})
test('template exposes disabled reason and refresh action', () => {
  assert.match(source, /:disabled="detail.canTransferToNcr !== true"/)
  assert.match(source, /:title="detail.transferToNcrBlockedReason"/)
  assert.match(source, /@click="refreshTransferEligibility">刷新转审状态/)
})

test('eligibility refresh preserves unsaved investigation', async () => {
  const h = harness()
  h.detail.value = { id: 1, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: false }
  h.startHandling()
  h.handlingForm.rootCauseAnalysis = 'unsaved investigation'
  await h.refreshTransferEligibility()
  assert.equal(h.detail.value.canTransferToNcr, true)
  assert.equal(h.handlingForm.rootCauseAnalysis, 'unsaved investigation')
})

test('bulk selection excludes server-blocked transfer', () => {
  const index = fs.readFileSync(
    path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue'),
    'utf8'
  )
  const selector = index.match(/const isCriticalOpen = (.*)/)[1]
  const context = {}
  vm.runInNewContext(
    ts.transpileModule('globalThis.selectable = ' + selector, {
      compilerOptions: { target: ts.ScriptTarget.ES2020 }
    }).outputText,
    context
  )
  assert.equal(
    context.selectable({ status: 'OPEN', level: 'CRITICAL', canTransferToNcr: false }),
    false
  )
  assert.equal(
    context.selectable({ status: 'OPEN', level: 'CRITICAL', canTransferToNcr: true }),
    true
  )
})

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => {
    resolve = yes
    reject = no
  })
  return { promise, resolve, reject }
}

test('older refresh success cannot overwrite newer eligibility or investigation draft', async () => {
  const first = deferred(),
    second = deferred()
  const queue = [first, second]
  const h = harness({ getDeviation: () => queue.shift().promise })
  h.detail.value = { id: 1, status: 'OPEN', level: 'CRITICAL', canTransferToNcr: false }
  h.startHandling()
  h.handlingForm.rootCauseAnalysis = 'unsaved investigation'
  const older = h.refreshTransferEligibility(),
    newer = h.refreshTransferEligibility()
  second.resolve({ canTransferToNcr: false, transferToNcrBlockedReason: '已冻结' })
  await newer
  first.resolve({ canTransferToNcr: true })
  await older
  assert.equal(h.detail.value.canTransferToNcr, false)
  assert.equal(h.detail.value.transferToNcrBlockedReason, '已冻结')
  assert.equal(h.handlingForm.rootCauseAnalysis, 'unsaved investigation')
})

test('older refresh error is ignored after newer refresh succeeds', async () => {
  const first = deferred(),
    second = deferred()
  const queue = [first, second]
  const h = harness({ getDeviation: () => queue.shift().promise })
  h.detail.value = { id: 1, canTransferToNcr: false }
  const older = h.refreshTransferEligibility(),
    newer = h.refreshTransferEligibility()
  second.resolve({ canTransferToNcr: true })
  await newer
  first.reject(new Error('stale failure'))
  await older
  assert.deepEqual(h.errors, [])
  assert.equal(h.detail.value.canTransferToNcr, true)
})

for (const change of ['record', 'reload']) {
  test(`refresh response is isolated after ${change}`, async () => {
    const request = deferred()
    const h = harness({ getDeviation: () => request.promise })
    h.detail.value = { id: 1, canTransferToNcr: false }
    const pending = h.refreshTransferEligibility()
    if (change === 'record') h.props.id = 2
    else h.invalidateLoad()
    request.resolve({ canTransferToNcr: true })
    await pending
    assert.equal(h.detail.value.canTransferToNcr, false)
  })
}
