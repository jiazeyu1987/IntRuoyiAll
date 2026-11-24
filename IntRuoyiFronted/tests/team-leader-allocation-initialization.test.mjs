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
    resetLeaderReviewContext() {}, requireWritableHandoff: () => true, requirePositiveNumber: value => value,
    canAllocateSubmission: () => true, reviewDialogMode: { value: 'REVIEW' },
    reviewEvent: { value: undefined }, structuredClone, toRaw: value => value,
    reviewForm: {}, resetReviewAllocation() {}, reviewVisible: { value: false },
    allocationRows: { value: [] }, addAllocationLine() { context.allocationRows.value.push({ allocatedQuantity: 0 }) },
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

test('opening another allocation closes the old dialog before reading its snapshot', async () => {
  const h = harness()
  h.context.reviewVisible.value = true
  const pending = h.context.openAllocation({ id: 43 })
  assert.equal(h.context.reviewVisible.value, false)
  h.snapshot.resolve({ lines: [] })
  h.orders.resolve()
  await pending
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

for (const [label, snapshot] of [
  ['missing snapshot', undefined],
  ['snapshot from another report', { eventId: 41, version: 1 }],
  ['missing version', { eventId: 42 }]
]) {
  test(label + ' cannot send a production confirmation', async () => {
    const errors = []
    const context = {
      requireWritableHandoff: () => true, reviewSubmitting: { value: false },
      leaderReviewGuard: { capture: () => () => true }, reviewDialogMode: { value: 'ALLOCATION' },
      reviewForm: { reviewStatus: 'APPROVED', reviewRemark: '确认分配' },
      isProductionLeader: { value: true }, reviewEvent: { value: { id: 42 } },
      requirePositiveNumber: value => value, allocationSnapshot: { value: snapshot },
      approvalInvalidation: { captureHandoffSourceSuccess() {} },
      resolveCurrentLeaderType: () => 'PRODUCTION', isReportAllocationVersionConflict: () => false,
      ElMessage: { error: message => errors.push(message) }, resolveErrorMessage: error => error.message,
      confirmTeamLeaderReportAllocation() { assert.fail('must not send a confirmation') }
    }
    const begin = source.indexOf('const submitReview = async () =>')
    const finish = source.indexOf('const resetCorrectionFormForEvent =', begin)
    runInNewContext(ts.transpileModule(source.slice(begin, finish) + '\nglobalThis.submit = submitReview', {
      compilerOptions: { target: ts.ScriptTarget.ES2020 }
    }).outputText, context)
    await context.submit()
    assert.match(errors[0], /尚未加载完成/)
    assert.equal(context.reviewSubmitting.value, false)
  })
}
