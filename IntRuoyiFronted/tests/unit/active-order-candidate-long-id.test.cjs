const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const idModule = { exports: {} }
vm.runInNewContext(transpile(fs.readFileSync('src/utils/routeQueryId.ts', 'utf8')), idModule)
const source = parse(fs.readFileSync('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('workbench.ts', source, ts.ScriptTarget.Latest, true)
const names = ['findActiveOrderCandidateById', 'findActiveOrderCandidateByCode', 'handleActiveOrderCandidateClear', 'handleActiveOrderCandidateChange', 'requireSelectedActiveOrderCandidateWorkOrderId']
const body = names.map(name => {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  return statement.getText(ast)
}).join('\n')
function setup() {
  const candidates = ['2118683660918222848', '2118683660918222849'].map((workOrderId, i) => ({ workOrderId, workOrderCode: 'WO-' + i, eligible: true }))
  const h = { parsePositiveRouteQueryId: idModule.exports.parsePositiveRouteQueryId,
    activeOrderCandidateOptions: { value: candidates }, activeOrderForm: {},
    activeOrderSelectedCandidate: {}, activeOrderCandidateKeyword: { value: '' }, activeOrderCandidateError: { value: '' },
    resolveActiveOrderCandidateByKeyword: async () => candidates.find(c => c.workOrderCode === h.activeOrderCandidateKeyword.value) }
  vm.runInNewContext(transpile(body) + '\nglobalThis.select = handleActiveOrderCandidateChange; globalThis.requireId = requireSelectedActiveOrderCandidateWorkOrderId;', h)
  return h
}
test('adjacent long candidate IDs remain distinct and are transmitted exactly', async () => {
  const h = setup()
  for (const candidate of h.activeOrderCandidateOptions.value) {
    h.select(candidate.workOrderId)
    assert.equal(h.activeOrderSelectedCandidate.value, candidate)
    const id = await h.requireId()
    assert.equal(id, candidate.workOrderId)
    assert.equal(JSON.stringify({ workOrderId: id }), `{"workOrderId":"${candidate.workOrderId}"}`)
  }
})
test('small numeric candidates still select through the same path', async () => {
  const h = setup()
  h.activeOrderCandidateOptions.value = [{ workOrderId: 12, workOrderCode: 'WO-12', eligible: true }]
  h.select(12)
  assert.equal(await h.requireId(), '12')
})
test('unknown or invalid selection clears the previous candidate', async () => {
  const h = setup()
  for (const invalid of ['0', '-1', 'abc', '2e18', '2118683660918222800']) {
    h.select('2118683660918222848')
    h.select(invalid)
    assert.equal(h.activeOrderSelectedCandidate.value, undefined)
    assert.equal(h.activeOrderForm.workOrderId, undefined)
  }
})
test('an ineligible candidate cannot be submitted', async () => {
  const h = setup()
  h.activeOrderCandidateOptions.value[0].eligible = false
  h.activeOrderCandidateOptions.value[0].ineligibleReason = '订单已关闭'
  h.select('2118683660918222848')
  await assert.rejects(h.requireId(), /订单已关闭/)
})
