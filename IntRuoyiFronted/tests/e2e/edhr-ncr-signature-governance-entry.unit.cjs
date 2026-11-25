const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

// Execute the actual route loader with isolated unit-test dependencies; this is not browser E2E.
const source = fs.readFileSync(path.join(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const start = source.indexOf('const requireReviewIdentity =')
const end = source.indexOf('\nwatch(', start)
assert.ok(start > 0 && end > start)
const code = ts.transpileModule(source.slice(start, end), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function fixture(query, review = { id: '123', reviewStatus: 'PENDING_REVIEW' }) {
  const box = value => ({ value })
  const calls = { get: [], list: 0, fill: 0, handoff: 0 }
  const context = {
    route: { name: 'MesProFeedbackEdhrNonconformanceReview', query },
    unassignedReviewerId: box(undefined), reviewTabOpen: box(false), selectedReview: box(undefined), selectedReviewReadOnly: box(false),
    reviews: box([]), total: box(0), listLoading: box(false), errorText: box(''), activeTab: box('pending'),
    autoCreate: box(false), entryActiveOrderId: box(undefined), reviewContextGeneration: 0,
    closeCreateDialog() {}, invalidateReviewContext() { context.reviewContextGeneration++ },
    async loadReviews() { calls.list++ }, openCreateDialog() { throw new Error('unexpected create') },
    async getNonconformanceReview(id) { calls.get.push(id); return review },
    fillDisposeForm() { calls.fill++ }, resolveErrorMessage(error) { return error.message },
    async handoffNavigationContext() { calls.handoff++; throw new Error('unexpected handoff') },
    REVIEW_STATUS_PENDING_REVIEW: 'PENDING_REVIEW', SOURCE_TYPE_DEVIATION: 'DEVIATION', URL
  }
  vm.createContext(context)
  vm.runInContext(code + '\nglobalThis.run = loadReviewEntry', context)
  return { context, calls }
}

async function main() {
  for (const status of ['PENDING_REVIEW', 'COMPLETED']) {
    const { context, calls } = fixture({ reviewId: '123', from: 'signature-governance' }, { id: '123', reviewStatus: status })
    await context.run()
    assert.equal(context.errorText.value, '')
    assert.equal(context.selectedReviewReadOnly.value, true)
    assert.equal(context.reviewTabOpen.value, true)
    assert.equal(context.selectedReview.value.id, '123')
    assert.equal(context.activeTab.value, 'review')
    assert.deepEqual(calls.get, ['123'])
    assert.equal(calls.fill, 1)
  }
  for (const reviewId of ['0', '-1', 'abc', ['123'], undefined]) {
    const { context, calls } = fixture({ reviewId, from: 'signature-governance' })
    await context.run()
    assert.notEqual(context.errorText.value, '')
    assert.equal(context.reviewTabOpen.value, false)
    assert.equal(calls.get.length, 0)
  }
  for (const mixed of [{ autoCreate: '1' }, { activeOrderId: '2' }, { deviationId: '3' }, { handoffTaskId: '4' }, { viewer: '0' }]) {
    const { context, calls } = fixture({ reviewId: '123', from: 'signature-governance', ...mixed })
    await context.run()
    assert.match(context.errorText.value, /不能混入其他业务参数/)
    assert.equal(context.reviewTabOpen.value, false)
    assert.equal(calls.get.length, 0)
    assert.equal(calls.handoff, 0)
  }
  const mismatch = fixture({ reviewId: '123', from: 'signature-governance' }, { id: '456' })
  await mismatch.context.run()
  assert.match(mismatch.context.errorText.value, /单据身份不一致/)
  assert.equal(mismatch.context.reviewTabOpen.value, false)

  const normal = fixture({ reviewId: '123' })
  await normal.context.run()
  assert.equal(normal.context.selectedReviewReadOnly.value, false)
  assert.equal(normal.context.reviewTabOpen.value, true)

  const materials = fixture({ reviewId: '123', from: 'signature-governance' })
  materials.context.fillDisposeForm = () => { throw new Error('malformed materials') }
  await materials.context.run()
  assert.equal(materials.context.errorText.value, 'malformed materials')
  assert.equal(materials.context.reviewTabOpen.value, false)
  assert.equal(materials.context.selectedReview.value, undefined)

  console.log('PASS: QA signature governance opens exact read-only review; malformed and mixed identities are rejected')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
