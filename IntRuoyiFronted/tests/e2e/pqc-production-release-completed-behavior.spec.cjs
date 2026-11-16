const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive } = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/production-release/PqcProductionReleasePage.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['getList', 'resolveStatusLabel', 'resolveStatusTagType', 'handleTabChange', 'resetQuery', 'applyReleaseSuccess']
const selected = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
const code = ts.transpileModule(selected.map(s => s.getText(ast)).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
function harness() {
  const calls = []
  const rows = ['RELEASED', 'VOIDED', 'REWORKED', 'CONCESSION_RELEASED'].map((viewStatus, i) => ({ applicationId: String(i + 1), viewStatus }))
  const context = {
    PQC_RELEASE_VIEW_PENDING: 'PENDING', PQC_RELEASE_VIEW_COMPLETED: 'COMPLETED', PQC_RELEASE_VIEW_RELEASED: 'RELEASED',
    PQC_RELEASE_VIEW_VOIDED: 'VOIDED', PQC_RELEASE_VIEW_REWORKED: 'REWORKED', PQC_RELEASE_VIEW_CONCESSION_RELEASED: 'CONCESSION_RELEASED',
    activeView: ref('COMPLETED'), queryParams: reactive({ pageNo: 3, pageSize: 10, workOrderCode: 'WO-1', batchCode: 'B-1' }),
    loading: ref(false), loadError: ref(''), list: ref([]), total: ref(0), listRequestSequence: 0,
    approvalInvalidation: { syncSession: () => ({ key: 'session' }) }, route: { fullPath: '/pqc' },
    resolveErrorMessage: e => e.message, getPqcProductionReleasePage: async query => { calls.push(query); return { list: rows, total: 42 } },
    releaseIdempotencyKeys: new Map(), releaseForm: reactive({ signaturePassword: 'secret', udiControlDocumentNo: 'UDI' }),
    releaseOutcomeUncertain: ref(true), releaseResult: ref(), releaseError: ref(''), message: { success() {}, warning() {} }
  }
  const page = Function(...Object.keys(context), code + '; return {' + names.join(',') + '}')(...Object.values(context))
  return { ...context, ...page, calls }
}
test('merged results use one server page and distinguish all four outcomes', async () => {
  const h = harness()
  await h.getList()
  assert.equal(h.calls.length, 1)
  assert.equal(h.calls[0].viewStatus, 'COMPLETED')
  assert.equal(h.calls[0].pageNo, 3)
  assert.equal(h.calls[0].workOrderCode, 'WO-1')
  assert.equal(h.calls[0].batchCode, 'B-1')
  assert.equal(h.total.value, 42)
  assert.deepEqual(h.list.value.map(h.resolveStatusLabel), ['已放行', '已作废', '已返工', '已让步放行'])
  assert.deepEqual(h.list.value.map(h.resolveStatusTagType), ['success', 'info', 'warning', 'success'])
  assert.equal(h.resolveStatusLabel({ viewStatus: 'RELEASED', underReview: true }), '已放行')
  assert.equal(h.resolveStatusLabel({ viewStatus: 'PENDING', underReview: true }), '评审中')
})
test('switching to pending and resetting filters restart pagination', () => {
  const h = harness()
  h.activeView.value = 'PENDING'
  h.handleTabChange()
  assert.equal(h.queryParams.pageNo, 1)
  assert.equal(h.calls[0].viewStatus, 'PENDING')
  h.resetQuery()
  assert.equal(h.calls[1].workOrderCode, undefined)
  assert.equal(h.calls[1].batchCode, undefined)
  assert.equal(h.resolveStatusLabel({ viewStatus: 'PENDING', nonconformanceDisposition: 'concession_release' }), '待让步签字')
})
test('normal and concession approval both navigate to merged results', async () => {
  for (const nonconformanceDisposition of [undefined, 'concession_release']) {
    const h = harness()
    h.activeView.value = 'PENDING'
    await h.applyReleaseSuccess({ nonconformanceDisposition }, { applicationId: '1' }, { scopeKey: 'one' }, false, () => true)
    assert.equal(h.activeView.value, 'COMPLETED')
    assert.equal(h.calls[0].viewStatus, 'COMPLETED')
    assert.equal(h.queryParams.pageNo, 1)
    assert.equal(h.releaseForm.signaturePassword, '')
  }
})
test('template compiles with two tabs and the result column', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'pqc-completed' }).errors, [])
  assert.equal((descriptor.template.content.match(/<el-tab-pane/g) || []).length, 2)
  assert.ok(descriptor.template.content.includes('放行结果'))
})
