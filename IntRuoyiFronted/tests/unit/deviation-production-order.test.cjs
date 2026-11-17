const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function loadFunction(name, context, declarations = '') {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  const js = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${declarations}; ${js}; return ${name}`)(...Object.values(context))
}
function setup() {
  return { approvalInvalidation: { syncSession: () => ({ key: 'tenant71' }), isCurrent: () => true },
    createVisible: ref(true), orderLoading: ref(false), orderOptions: ref([]), createError: ref(''),
    getDeviationOrderOptions: async () => ({ list: [{ workOrderId: 51, workOrderCode: 'WO-51' }], total: 1 }) }
}
test('selector displays production order numbers and submits the selected production order identity', async () => {
  const calls = []
  const context = { createFormRef: ref({ validate: async () => true }), createLoading: ref(false), createError: ref(''),
    createForm: { workOrderId: 51, description: '偏差', signaturePassword: 'unit-test' },
    createDeviation: async data => calls.push(data), message: { success() {} }, createVisible: ref(true), load: async () => {} }
  await loadFunction('submitCreate', context)()
  assert.equal(calls[0].workOrderId, 51)
  assert.equal(Object.hasOwn(calls[0], 'batchExecutionId'), false)
  assert.match(source, /label="生产订单号" prop="workOrderId"/)
  assert.match(source, /:label="item.workOrderCode"/)
  assert.equal(context.createVisible.value, false)
})
test('production-order search exposes a load failure and clears stale options', async () => {
  const context = setup()
  context.orderOptions.value = [{ workOrderId: 1 }]
  context.getDeviationOrderOptions = async () => { throw new Error('order service unavailable') }
  await loadFunction('loadOrderOptions', context, 'let orderOptionsSerial = 0')('WO-51')
  assert.deepEqual(context.orderOptions.value, [])
  assert.equal(context.createError.value, 'order service unavailable')
  assert.equal(context.orderLoading.value, false)
})
test('late order search cannot overwrite newer production-order results', async () => {
  const context = setup()
  const finish = []
  context.getDeviationOrderOptions = () => new Promise(resolve => finish.push(resolve))
  const search = loadFunction('loadOrderOptions', context, 'let orderOptionsSerial = 0')
  const oldRequest = search('WO-1'), newRequest = search('WO-2')
  finish[1]({ list: [{ workOrderId: 2 }] }); await newRequest
  finish[0]({ list: [{ workOrderId: 1 }] }); await oldRequest
  assert.deepEqual(context.orderOptions.value, [{ workOrderId: 2 }])
})
test('tenant changes prevent an old order result from populating the selector', async () => {
  const context = setup()
  let finish
  context.getDeviationOrderOptions = () => new Promise(resolve => { finish = resolve })
  const pending = loadFunction('loadOrderOptions', context, 'let orderOptionsSerial = 0')()
  context.approvalInvalidation.isCurrent = () => false
  finish({ list: [{ workOrderId: 51 }] }); await pending
  assert.deepEqual(context.orderOptions.value, [])
})
