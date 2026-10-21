import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const source = readFileSync(new URL('../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue', import.meta.url), 'utf8')
const start = source.indexOf('const openAllocation = async (event:')
const end = source.indexOf('const requireProductionDisplayedReview =', start)
assert.ok(start >= 0 && end > start)
const deferred = () => {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}
const harness = () => {
  const snapshot = deferred(), orders = deferred(), errors = [], applied = []
  let current = true
  const context = {
    resetLeaderReviewContext() {}, requirePositiveNumber: value => value,
    canAllocateSubmission: () => true, reviewDialogMode: { value: 'REVIEW' },
    reviewEvent: { value: undefined }, structuredClone, toRaw: value => value,
    reviewForm: {}, resetReviewAllocation() {}, reviewVisible: { value: false },
    leaderReviewGuard: { capture: () => () => current },
    getCurrentTeamLeaderReportAllocation: () => snapshot.promise,
    resolveCurrentLeaderType: () => 'PRODUCTION', loadActiveOrders: () => orders.promise,
    applyAllocationSnapshot(value) { assert.equal(context.reviewVisible.value, false); applied.push(value) },
    prefillSelectedOrderAllocation() { assert.equal(context.reviewVisible.value, false); applied.push('prefill') },
    ElMessage: { error: message => errors.push(message) }, resolveErrorMessage: error => error.message
  }
  runInNewContext(ts.transpileModule(source.slice(start, end) + '\nglobalThis.openAllocation = openAllocation', {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText, context)
  return { context, snapshot, orders, errors, applied, invalidate: () => { current = false } }
}

test('allocation stays closed until both reads and the original prefill finish', async () => {
  const h = harness(), pending = h.context.openAllocation({ id: 42 })
  assert.equal(h.context.reviewVisible.value, false)
  h.snapshot.resolve({ lines: [] })
  await Promise.resolve()
  assert.equal(h.context.reviewVisible.value, false)
  assert.equal(h.applied.length, 0)
  h.orders.resolve()
  await pending
  assert.equal(h.applied.length, 2)
  assert.equal(h.context.reviewVisible.value, true)
})

test('failed allocation load reports the failure without opening an editable draft', async () => {
  const h = harness(), pending = h.context.openAllocation({ id: 42 })
  h.snapshot.reject(new Error('allocation unavailable'))
  h.orders.resolve()
  await pending
  assert.equal(h.context.reviewVisible.value, false)
  assert.deepEqual(h.errors, ['allocation unavailable'])
  assert.equal(h.applied.length, 0)
})

test('superseded allocation reads cannot reopen a stale dialog', async () => {
  const h = harness(), pending = h.context.openAllocation({ id: 42 })
  h.invalidate()
  h.snapshot.resolve({ lines: [] })
  h.orders.resolve()
  await pending
  assert.equal(h.context.reviewVisible.value, false)
  assert.equal(h.applied.length, 0)
})
