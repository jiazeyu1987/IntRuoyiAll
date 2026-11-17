const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')
function production(file, name, context) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro', file), 'utf8')
  const ast = ts.createSourceFile('component.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  const code = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${code}; return ${name}`)(...Object.values(context))
}
const traceFile = 'edhr/components/DeviationTracePane.vue'
function trace(props) {
  const calls = []
  const context = { props, requestSerial: 0, loading: ref(false), error: ref(''), rows: ref([]), detailVisible: ref(true), selectedDeviationId: ref(99),
    getDeviationPage: async query => { calls.push(query); return { list: [{ id: 1 }], total: 1 } } }
  return { context, calls }
}
test('order trace sends exact order identity without requiring a completed batch', async () => {
  const { context, calls } = trace({ activeOrderId: '991' })
  await production(traceFile, 'load', context)()
  assert.deepEqual(calls, [{ pageNo: 1, pageSize: 200, batchExecutionId: undefined, activeOrderId: '991' }])
  assert.equal(context.rows.value[0].id, 1)
  assert.equal(context.detailVisible.value, false)
})
test('batch trace remains supported; missing or mixed scope never queries all tenant rows', async () => {
  for (const props of [{}, { activeOrderId: 991, batchExecutionId: 41 }, { activeOrderId: '0' }]) {
    const { context, calls } = trace(props)
    await production(traceFile, 'load', context)()
    assert.equal(calls.length, 0); assert.ok(context.error.value)
  }
  const { context, calls } = trace({ batchExecutionId: 41 })
  await production(traceFile, 'load', context)()
  assert.equal(calls[0].batchExecutionId, 41)
})
test('late order response cannot replace another cycle', async () => {
  const { context } = trace({ activeOrderId: 991 }); let finish
  context.getDeviationPage = () => new Promise(resolve => { finish = resolve })
  const load = production(traceFile, 'load', context); const old = load()
  const oldFinish = finish
  context.props.activeOrderId = 992
  const latest = load(); finish({ list: [{ id: 2 }], total: 1 }); await latest
  oldFinish({ list: [{ id: 1 }], total: 1 }); await old
  assert.equal(context.rows.value[0].id, 2)
})
const ncrFile = 'edhr-nonconformance/NonconformanceReviewPage.vue'
function entry(query, review) {
  const context = { route: { name: 'MesProFeedbackEdhrNonconformanceReview', query }, reviewTabOpen: ref(false), closeCreateDialog() {}, invalidateReviewContext() {},
    selectedReview: ref(), errorText: ref(''), reviewContextGeneration: 0, reviews: ref([]), total: ref(0), listLoading: ref(false),
    getNonconformanceReview: async () => review, parseExactIntegerJson: JSON.parse, SOURCE_TYPE_DEVIATION: 'DEVIATION',
    fillDisposeForm() {}, selectedReviewReadOnly: ref(false), activeTab: ref(''), loadReviews() {}, resolveErrorMessage: e => e.message }
  context.requireReviewIdentity = production(ncrFile, 'requireReviewIdentity', {})
  return context
}
test('deviation NCR entry validates order and membership, retains batch identity checks', async () => {
  const review = { id: '7', sourceType: 'DEVIATION', activeOrderId: '991', sourceId: '991', batchExecutionId: null, deviationIdsJson: '[3]' }
  const query = { from: 'deviation', reviewId: '7', activeOrderId: '991', deviationId: '3' }
  const valid = entry(query, review); await production(ncrFile, 'loadReviewEntry', valid)()
  assert.deepEqual(valid.selectedReview.value, review); assert.equal(valid.errorText.value, '')
  for (const bad of [{ ...query, activeOrderId: '992' }, { ...query, deviationId: '4' }, { ...query, batchExecutionId: '41' }]) {
    const context = entry(bad, review); await production(ncrFile, 'loadReviewEntry', context)()
    assert.equal(context.selectedReview.value, undefined); assert.ok(context.errorText.value)
  }
  const legacy = entry({ from: 'deviation', reviewId: '7', batchExecutionId: '41', deviationId: '3' }, { ...review, batchExecutionId: '41', sourceId: '41' })
  await production(ncrFile, 'loadReviewEntry', legacy)(); assert.equal(legacy.errorText.value, '')
})
