const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const { ref, watch } = require('vue')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1], ts.ScriptTarget.Latest, true)
const names = ['openReviewTab', 'closeReviewTab', 'handleTabChange', 'buildReviewQuery']
const parts = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
const closeWatcher = ast.statements.find(s => ts.isExpressionStatement(s) && s.getText(ast).startsWith('watch(reviewTabOpen,'))
assert.equal(parts.length, names.length)
assert.ok(closeWatcher)

function harness() {
  const calls = []
  const context = {
    watch, reviewTabOpen: ref(false), activeTab: ref('pending'), listTab: ref('pending'),
    selectedReview: ref(), unassignedReviewerId: ref(), errorText: ref(''),
    disposeLoading: ref(false), assigningReviewer: ref(false),
    queryParams: { pageNo: 3, pageSize: 10 }, REVIEW_STATUS_PENDING_REVIEW: 'pending_review',
    invalidateReviewContext: () => calls.push('invalidate'),
    fillDisposeForm: () => calls.push('fill'), loadReviews: () => calls.push('list')
  }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(parts.map(p => p.getText(ast)).join('\n') + '\n' + closeWatcher.getText(ast)
    + '\n' + names.map(n => `globalThis.${n} = ${n}`).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText, context)
  return { context, calls }
}

test('processing selects the review tab without discarding the pending list filter', () => {
  const { context: c, calls } = harness()
  c.openReviewTab({ id: 109 })
  assert.equal(c.reviewTabOpen.value, true)
  assert.equal(c.activeTab.value, 'review')
  assert.equal(c.selectedReview.value.id, 109)
  c.handleTabChange()
  assert.deepEqual(calls, ['invalidate', 'fill'])
  assert.equal(c.buildReviewQuery().reviewStatus, 'pending_review')
})

test('switching between list and processing preserves the selected review and form context', () => {
  const { context: c, calls } = harness()
  c.openReviewTab({ id: 109 })
  c.activeTab.value = 'all'
  c.handleTabChange()
  c.activeTab.value = 'review'
  c.handleTabChange()
  assert.equal(c.selectedReview.value.id, 109)
  assert.equal(c.reviewTabOpen.value, true)
  assert.equal(c.queryParams.pageNo, 1)
  assert.equal(c.buildReviewQuery().reviewStatus, undefined)
  assert.deepEqual(calls, ['invalidate', 'fill', 'list'])
})

test('closing processing returns to its list tab and invalidates outstanding callbacks', () => {
  const { context: c, calls } = harness()
  c.openReviewTab({ id: 109 })
  c.closeReviewTab()
  assert.equal(c.reviewTabOpen.value, false)
  assert.equal(c.activeTab.value, 'pending')
  assert.deepEqual(calls, ['invalidate', 'fill', 'invalidate'])
})

test('submitting or assigning prevents tab closure and review replacement', () => {
  for (const state of ['disposeLoading', 'assigningReviewer']) {
    const { context: c, calls } = harness()
    c.openReviewTab({ id: 109 })
    c[state].value = true
    c.closeReviewTab()
    c.openReviewTab({ id: 110 })
    assert.equal(c.reviewTabOpen.value, true)
    assert.equal(c.selectedReview.value.id, 109)
    assert.deepEqual(calls, ['invalidate', 'fill'])
  }
})
