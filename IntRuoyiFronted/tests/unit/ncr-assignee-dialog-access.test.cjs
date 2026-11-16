const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const { renderToString } = require('vue/server-renderer')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest, true)
function declaration(name) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  return statement.getText(ast)
}
const names = ['isAssignedQaReviewer', 'canDisposeReview', 'canDisposeSelectedReview',
  'reviewTabTitle', 'openReviewTab', 'requireReviewIdentity', 'loadReviewEntry']
const code = ts.transpileModule(names.map(declaration).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const review = (qaReviewerUserId = 12, reviewStatus = 'pending_review') =>
  ({ id: 48, qaReviewerUserId, reviewStatus })

function harness(userId = 12, permission = true, row = review()) {
  const calls = []
  const context = {
    computed: vue.computed, useUserStore: () => ({ getUser: { id: userId } }),
    hasPermission: () => permission, REVIEW_STATUS_PENDING_REVIEW: 'pending_review',
    selectedReview: vue.ref(), selectedReviewReadOnly: vue.ref(false),
    disposeLoading: vue.ref(false), assigningReviewer: vue.ref(false), activeTab: vue.ref('all'),
    reviewTabOpen: vue.ref(false), errorText: vue.ref(''), unassignedReviewerId: vue.ref(),
    route: { name: 'MesProFeedbackEdhrNonconformanceReview', query: { reviewId: '48' } },
    reviews: vue.ref([]), total: vue.ref(0), listLoading: vue.ref(false), reviewContextGeneration: 0,
    closeCreateDialog() {}, invalidateReviewContext: () => calls.push('invalidate'),
    fillDisposeForm: () => calls.push('fill'), loadReviews: () => calls.push('list'),
    getNonconformanceReview: async () => row,
    resolveErrorMessage: error => error.message
  }
  const api = Function(...Object.keys(context), `${code}; return { ${names.join(',')} }`)(...Object.values(context))
  return { ...api, context, calls }
}

const actionSlot = source.match(/<el-table-column[^>]*prop="actions"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>/)[1]
const render = vue.compile(actionSlot)
async function renderAction(h, row) {
  const Button = { setup: (_, { slots }) => () => vue.h('button', slots.default?.()) }
  const app = vue.createSSRApp({ setup: () => ({ ...h, row }), render })
  app.component('el-button', Button)
  return renderToString(app)
}

test('other QA and administrator can view details but cannot process another QA review', async () => {
  for (const userId of [1, 13, undefined]) {
    const h = harness(userId === undefined ? null : userId)
    assert.equal(h.isAssignedQaReviewer(review()), false)
    const rendered = await renderAction(h, review())
    assert.match(rendered, /详情/); assert.doesNotMatch(rendered, /处理/)
    h.openReviewTab(review())
    assert.equal(h.context.reviewTabOpen.value, true)
    assert.equal(h.canDisposeSelectedReview.value, false)
    assert.deepEqual(h.calls, ['invalidate', 'fill'])
  }
})

test('only assigned QA opens processing; numeric and string user identities agree', async () => {
  const h = harness('12')
  assert.match(await renderAction(h, review()), /处理/)
  h.openReviewTab(review())
  assert.equal(h.context.reviewTabOpen.value, true)
  assert.equal(h.canDisposeSelectedReview.value, true)
  assert.equal(h.reviewTabTitle.value, '处理不合格评审')
  assert.deepEqual(h.calls, ['invalidate', 'fill'])
})

test('unassigned review retains details but missing login identity cannot process it', async () => {
  const h = harness(null)
  assert.equal(h.isAssignedQaReviewer(undefined), false)
  const rendered = await renderAction(h, review(null))
  assert.match(rendered, /详情/); assert.doesNotMatch(rendered, /处理/)
  h.openReviewTab(review(null))
  assert.equal(h.context.reviewTabOpen.value, true)
  assert.equal(h.canDisposeSelectedReview.value, false)
})

test('assigned QA without disposition permission or with completed review gets view only', async () => {
  for (const [permission, status] of [[false, 'pending_review'], [true, 'closed']]) {
    const h = harness(12, permission)
    const row = review(12, status)
    assert.match(await renderAction(h, row), /详情/)
    assert.doesNotMatch(await renderAction(h, row), /处理/)
    h.openReviewTab(row)
    assert.equal(h.context.reviewTabOpen.value, true)
    assert.equal(h.canDisposeSelectedReview.value, false)
    assert.equal(h.reviewTabTitle.value, '评审详情')
  }
})

test('non-assignee exact links open readable review without disposition access', async () => {
  for (const reviewer of [12, null]) {
    const h = harness(1, true, review(reviewer))
    await h.loadReviewEntry()
    assert.equal(h.context.reviewTabOpen.value, true)
    assert.equal(h.context.selectedReview.value.id, 48)
    assert.equal(h.canDisposeSelectedReview.value, false)
    assert.equal(h.context.errorText.value, '')
    assert.deepEqual(h.calls, ['invalidate', 'fill', 'list'])
    assert.equal(h.context.listLoading.value, false)
  }
})

test('assignee exact link opens review; readonly handoff title never offers processing', async () => {
  const h = harness()
  await h.loadReviewEntry()
  assert.equal(h.context.reviewTabOpen.value, true)
  assert.equal(h.context.selectedReview.value.id, 48)
  assert.deepEqual(h.calls, ['invalidate', 'fill', 'list'])
  h.context.selectedReviewReadOnly.value = true
  assert.equal(h.reviewTabTitle.value, '评审详情')
})
