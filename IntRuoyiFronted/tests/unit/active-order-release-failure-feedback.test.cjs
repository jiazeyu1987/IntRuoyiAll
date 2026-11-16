const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const start = source.indexOf('const submitActiveOrderReleaseApplication =')
const end = source.indexOf('\nconst handleRebuildActiveOrder', start)
assert.ok(start > 0 && end > start)
const handler = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const ref = value => ({ value })

function harness(failure) {
  let writes = 0
  const errors = [], successes = []
  const context = {
    activeOrderTestResetSubmitting: ref(false), releaseApplicationSubmittingId: ref(undefined),
    releaseApplicationBlockers: ref([]), releaseApplicationErrorMessage: ref('上次错误'),
    releaseApplicationErrorOrder: ref(undefined), releaseApplicationUncertainMessage: ref(''),
    releaseApplicationLocks: new Map(),
    leaderReleaseGuard: { invalidate() {}, capture: () => () => true },
    canApplyActiveOrderRelease: () => true, isActiveOrderReleaseApplicationLocked: () => false,
    freezeActiveOrderReleaseRequest: row => ({ rowId: row.id, activeOrderId: String(row.id) }),
    h: () => ({}), ElMessageBox: { confirm: async () => {} },
    ElMessage: { error: value => errors.push(value), success: value => successes.push(value) },
    applyTeamLeaderActiveOrderRelease: async () => { writes++; throw failure },
    approvalInvalidation: { syncSession: () => ({ key: 'session' }) },
    isActiveOrderReleaseTransportTimeout: () => false,
    resolveActiveOrderReleaseFailure: error => error.details,
    resolveErrorMessage: (error, defaultMessage) => error.message || defaultMessage
  }
  vm.createContext(context)
  vm.runInContext(handler + '\nglobalThis.run = submitActiveOrderReleaseApplication', context)
  return { context, errors, successes, get writes() { return writes } }
}

for (const structured of [false, true]) {
  test(`${structured ? 'structured' : 'ordinary'} rejection remains visible with its order and never reports success`, async () => {
    const blocker = { reason: '补料单物料尚未关联所属工序', suggestion: '请打开订单详情签名关联' }
    const failure = Object.assign(new Error('申请被拒绝'), structured ? { details: { blockers: [blocker] } } : {})
    const h = harness(failure)
    const row = { id: 1009200589, workOrderId: '30', routeId: '40', routeVersionId: '50', version: 1 }
    await h.context.run(row)
    assert.equal(h.context.releaseApplicationErrorMessage.value, structured ? blocker.reason : failure.message)
    assert.equal(h.context.releaseApplicationErrorOrder.value, row)
    assert.equal(h.context.releaseApplicationBlockers.value.length, structured ? 1 : 0)
    assert.equal(h.context.releaseApplicationSubmittingId.value, undefined)
    assert.equal(h.context.releaseApplicationLocks.has(row.id), false)
    assert.equal(h.errors.length, 1)
    assert.equal(h.successes.length, 0)
    assert.equal(h.writes, 1)
  })
}

test('persistent feedback has a non-expiring error panel and a detail processing entry', () => {
  const panel = source.slice(source.indexOf('v-if="releaseApplicationErrorMessage"'),
    source.indexOf('v-if="releaseApplicationBlockers.length"'))
  assert.match(panel, /:closable="false"/)
  assert.match(panel, /openActiveOrderSubmissionDetail\(releaseApplicationErrorOrder\)/)
})

test('missing pick list keeps the actual order number and ERP synchronization instruction visible', async () => {
  const message = '当前系统缺少生产订单(MO-001)对应的领料单,请在确保ERP里存在然后在系统里进行同步'
  const h = harness(new Error(message))
  await h.context.run({ id: 1009200589, workOrderId: '30', routeId: '40', routeVersionId: '50', version: 1 })
  assert.equal(h.context.releaseApplicationErrorMessage.value, message)
  assert.equal(h.errors[0], message)
  assert.equal(h.successes.length, 0)
})
