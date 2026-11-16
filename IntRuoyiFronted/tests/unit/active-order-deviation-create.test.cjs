const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderDeviationDialog.vue'), 'utf8')
const ast = ts.createSourceFile('dialog.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function production(name, context) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  const code = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${code}; return ${name}`)(...Object.values(context))
}
function setup() {
  const calls = []
  const context = {
    props: { activeOrderId: 991 }, loading: ref(false), submitting: ref(false), error: ref(''),
    batches: ref([]), form: { batchExecutionId: 0, categoryCodes: ['INSPECTION'], signaturePassword: 'test-only' },
    formRef: ref({ validate: async () => true }), captureContext: () => () => true,
    getActiveOrderDeviationCreationBatches: async id => { calls.push(['options', id]); return [{ batchExecutionId: 41 }] },
    createActiveOrderDeviation: async (id, data) => { calls.push(['create', id, data]); return { id: 100, deviationCode: 'PC-100', batchExecutionId: 41 } },
    message: { success: text => calls.push(['success', text]) }, emit: event => calls.push(['emit', event])
  }
  return { context, calls }
}
test('order entry resolves only selected order and auto-selects its sole formal batch', async () => {
  const { context, calls } = setup()
  await production('loadBatches', context)()
  assert.equal(context.form.batchExecutionId, 41)
  assert.deepEqual(calls, [['options', 991]])
})
test('no formal batch or failed source read cannot submit or invent a batch', async () => {
  const { context, calls } = setup()
  context.getActiveOrderDeviationCreationBatches = async () => []
  await production('loadBatches', context)()
  assert.match(context.error.value, /尚未形成正式批记录/)
  await production('submit', context)()
  assert.equal(calls.length, 0)
  context.getActiveOrderDeviationCreationBatches = async () => { throw new Error('source unavailable') }
  await production('loadBatches', context)()
  assert.equal(context.error.value, 'source unavailable')
  assert.deepEqual(context.batches.value, [])
})
test('submit binds the order and batch and clears signature credentials', async () => {
  const { context, calls } = setup()
  await production('loadBatches', context)()
  await production('submit', context)()
  assert.equal(calls[1][0], 'create')
  assert.equal(calls[1][1], 991)
  assert.equal(calls[1][2].batchExecutionId, 41)
  assert.equal(context.form.signaturePassword, '')
  assert.equal(context.submitting.value, false)
  assert.deepEqual(calls.at(-1), ['emit', 'close'])
})
test('foreign batch, failed validation and duplicate click produce no write', async () => {
  const { context, calls } = setup()
  await production('loadBatches', context)()
  context.form.batchExecutionId = 42
  await production('submit', context)()
  context.form.batchExecutionId = 41
  context.formRef.value.validate = async () => { throw new Error('required fields') }
  await production('submit', context)()
  context.submitting.value = true
  await production('submit', context)()
  assert.equal(calls.length, 1)
})
test('late source response cannot fill a closed or switched order dialog', async () => {
  const { context } = setup()
  let finish, current = true
  context.captureContext = () => () => current
  context.getActiveOrderDeviationCreationBatches = () => new Promise(resolve => { finish = resolve })
  const pending = production('loadBatches', context)()
  current = false
  finish([{ batchExecutionId: 999 }])
  await pending
  assert.equal(context.form.batchExecutionId, 0)
  assert.deepEqual(context.batches.value, [])
})
test('write errors stay visible and cannot show success', async () => {
  const { context, calls } = setup()
  await production('loadBatches', context)()
  context.createActiveOrderDeviation = async () => { throw new Error('signature rejected') }
  await production('submit', context)()
  assert.equal(context.error.value, 'signature rejected')
  assert.equal(context.form.signaturePassword, '')
  assert.equal(context.submitting.value, false)
  assert.equal(calls.some(c => c[0] === 'success'), false)
})
