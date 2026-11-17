const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { ref } = require('vue')
const base = path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/components')
const source = fs.readFileSync(path.join(base, 'DeviationReviewerSelect.vue'), 'utf8')
function method(sfc, name, context) {
  const ast = ts.createSourceFile('component.ts', parse(sfc).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  const js = ts.transpileModule(declaration.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${js}; return ${name}`)(...Object.values(context))
}
function candidateContext() {
  return { props: { modelValue: 201, node: 'VERIFIER' }, candidates: ref([]), loaded: ref(false), loading: ref(false), error: ref(''), generation: 0,
    session: { syncSession: () => ({ key: 'tenant71' }), isCurrent: key => key === 'tenant71' },
    getDeviationReviewerCandidates: async node => { assert.equal(node, 'VERIFIER'); return [{ id: 201, username: 'verifier', nickname: '核验人' }] } }
}
test('candidate loading requests the actual node and only uses the authoritative pool', async () => {
  const context = candidateContext()
  await method(source, 'load', context)()
  assert.equal(context.candidates.value[0].id, 201)
  assert.equal(context.loaded.value, true)
  assert.match(source, /:user-options="candidates"/)
  assert.doesNotMatch(source, /getSelectionPage|getSimpleUserList|getUserList/)
})
test('empty and failed candidate pools remain distinct with visible errors', async () => {
  const context = candidateContext()
  context.getDeviationReviewerCandidates = async () => []
  await method(source, 'load', context)()
  assert.deepEqual(context.candidates.value, [])
  assert.equal(context.loaded.value, true)
  context.getDeviationReviewerCandidates = async () => { throw new Error('资格服务不可用') }
  await method(source, 'load', context)()
  assert.equal(context.loaded.value, false)
  assert.equal(context.error.value, '资格服务不可用')
  assert.deepEqual(context.candidates.value, [])
})
test('late candidates from a different responsibility do not replace current candidates', async () => {
  const context = candidateContext(); let complete
  context.getDeviationReviewerCandidates = () => new Promise(resolve => { complete = resolve })
  const pending = method(source, 'load', context)()
  context.props.node = 'QA'
  complete([{ id: 201 }]); await pending
  assert.deepEqual(context.candidates.value, [])
})
test('late candidates from a different tenant are discarded', async () => {
  const context = candidateContext(); let complete
  context.getDeviationReviewerCandidates = () => new Promise(resolve => { complete = resolve })
  const pending = method(source, 'load', context)()
  context.session.isCurrent = () => false
  complete([{ id: 201 }]); await pending
  assert.deepEqual(context.candidates.value, [])
})
const returnSource = fs.readFileSync(path.join(base, 'DeviationReviewReturn.vue'), 'utf8')
function returnContext() {
  const sent = [], events = []
  const context = { props: { deviationId: 901, contentVersion: 3, contentHash: 'hash3', nodes: [{ value: 'QA', label: 'QA' }] },
    submitting: ref(false), visible: ref(true), error: ref(''), form: { node: 'QA', reason: '  证据不足  ' },
    guard: { capture: () => () => true }, message: { success: text => events.push(text) }, emit: event => events.push(event),
    returnDeviationToInvestigation: async payload => { sent.push(payload) } }
  return { context, sent, events }
}
test('return sends the reviewed version, exact responsibility and mandatory reason', async () => {
  const { context, sent, events } = returnContext()
  await method(returnSource, 'submit', context)()
  assert.deepEqual(sent[0], { deviationId: 901, expectedContentVersion: 3, expectedContentHash: 'hash3', node: 'QA', reason: '证据不足' })
  assert.equal(context.visible.value, false)
  assert.ok(events.includes('returned'))
})
test('return cannot submit without a reason or current responsibility', async () => {
  const { context, sent } = returnContext()
  context.form.reason = '  '
  await method(returnSource, 'submit', context)()
  assert.equal(context.error.value, '请填写退回原因')
  context.form.reason = '证据不足'; context.props.nodes = []
  await method(returnSource, 'submit', context)()
  assert.equal(sent.length, 0)
})
test('failed return preserves the dialog and displays the server failure', async () => {
  const { context, events } = returnContext()
  context.returnDeviationToInvestigation = async () => { throw new Error('版本已变化') }
  await method(returnSource, 'submit', context)()
  assert.equal(context.error.value, '版本已变化')
  assert.equal(context.visible.value, true)
  assert.equal(context.submitting.value, false)
  assert.deepEqual(events, [])
})
test('both responsibility components compile as Vue SFCs', () => {
  for (const [name, code] of [['Candidate', source], ['Return', returnSource]]) {
    const { descriptor, errors } = parse(code)
    assert.deepEqual(errors, [])
    const script = compileScript(descriptor, { id: name })
    const result = compileTemplate({ id: name, source: descriptor.template.content, filename: name + '.vue', compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(result.errors, [])
  }
})
