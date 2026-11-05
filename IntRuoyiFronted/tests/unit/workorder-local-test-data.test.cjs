const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const source = fs.readFileSync('src/views/mes/pro/workorder/index.vue', 'utf8')
const descriptor = parse(source).descriptor
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['localTestProducts', 'localTestPreflight', 'localTestResult', 'localTestCheckedPayload',
  'localTestDialog', 'localTestProduct', 'localTestInputOrders', 'buildLocalTestPayload',
  'localTestCanPrepare', 'assertLocalTestAccess', 'localTestError', 'openLocalTestData',
  'preflightLocalTestData', 'prepareLocalTestData']
const code = names.map(name => ast.statements.find(s => ts.isVariableStatement(s)
  && s.declarationList.declarations.some(d => d.name.getText(ast) === name)).getText(ast)).join('\n')
const invalidation = ast.statements.find(s => ts.isExpressionStatement(s)
  && s.getText(ast).startsWith('watch(() => [localTestDialog.runId')).getText(ast)
const product = { requestId: '9007199254740993', projectId: '9007199254740994',
  catalogId: '9007199254740995', relationId: '9007199254740996', mesProductId: '9007199254740997',
  productCode: 'PRODUCT-A', productName: '正式产品', projectCode: 'PROJECT-A' }
function host(options = {}) {
  const calls = { writes: [], reads: 0, success: [], confirms: [], refresh: 0 }
  const context = { ...vue, exports: {}, Error, JSON, Number, Boolean, String,
    testModeEnabled: vue.ref(options.testMode !== false), checkPermi: () => options.permission !== false,
    ProWorkOrderApi: {
      getLocalTestApprovedProducts: async () => { calls.reads++; if (options.readError) throw options.readError; return [product] },
      preflightLocalTestData: async data => { calls.writes.push(['preflight', data]); return options.preflight || { ready: true, checks: [{ code: 'APPROVED_PRODUCT', ready: true, message: '已完成申请', path: '/mdm/product-catalog' }], missing: [], orders: [] } },
      prepareLocalTestData: async data => { calls.writes.push(['prepare', data]); if (options.prepareError) throw options.prepareError; return { source: data.source, runId: data.runId,
        orders: data.orders.map(row => ({ slot: row.slot, manifestId: '700', workOrderId: '800', workOrderCode: 'TEST', status: 'RUNNING', reused: true })) } }
    }, message: { confirm: async text => { calls.confirms.push(text); if (options.confirm) await options.confirm(context) }, success: text => calls.success.push(text) },
    getList: async () => { calls.refresh++ } }
  vm.runInNewContext(ts.transpileModule(code + '\n' + invalidation + '\nexports={localTestProducts,localTestPreflight,localTestResult,localTestCheckedPayload,localTestDialog,buildLocalTestPayload,localTestCanPrepare,openLocalTestData,preflightLocalTestData,prepareLocalTestData}',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  Object.assign(context, context.exports)
  return { c: context, calls }
}
async function fill(h) {
  await h.c.openLocalTestData()
  h.c.localTestDialog.requestId = product.requestId
  h.c.localTestDialog.runId = 'RUN-20261105'
  h.c.localTestDialog.orders.forEach(row => { row.batchCode = 'BATCH-' + row.slot })
}

test('page compiles and exposes only the new local test entry under mode and existing create permission', () => {
  const compiled = compileScript(descriptor, { id: 'workorder' })
  const template = compileTemplate({ source: descriptor.template.content, filename: 'index.vue', id: 'workorder', compilerOptions: { bindingMetadata: compiled.bindings } })
  assert.deepEqual(template.errors, [])
  const button = source.match(/<el-button[^>]*data-local-test-data-open[\s\S]*?<\/el-button>/)[0]
  assert.match(button, /v-if="testModeEnabled"/)
  assert.match(button, /mes:pro-work-order:create/)
  assert.doesNotMatch(button, /v-show|create-erp|openAiE2eDialog/)
  assert.match(source, /v-model="row.quantity"\s+:min="0\.01"\s+:precision="2"\s+:max="999999999999\.99"/)
  assert.match(source, /v-model="row.batchCode"\s+:maxlength="64"/)
})

test('preflight and prepare carry exact formal IDs and fixed A/B inputs, preserving reused result status', async () => {
  const h = host(); await fill(h)
  h.c.localTestDialog.orders[1].includeReplenishment = true
  await h.c.preflightLocalTestData(); await h.c.prepareLocalTestData()
  assert.equal(h.calls.writes.length, 2)
  const payload = h.calls.writes[1][1]
  for (const key of ['requestId', 'projectId', 'catalogId', 'relationId', 'mesProductId']) assert.equal(payload[key], product[key])
  assert.equal(payload.source, 'LOCAL_TEST_RUN')
  assert.deepEqual(Array.from(payload.orders, row => row.slot), ['A', 'B'])
  assert.equal(payload.orders[1].includeReplenishment, true)
  assert.equal(h.c.localTestResult.value.orders[0].status, 'RUNNING')
  assert.equal(h.c.localTestResult.value.orders[0].reused, true)
  assert.match(h.calls.confirms[0], /不计 ERP 同步，不自动办理业务/)
})

test('missing dependencies, unpreflighted input and changed parameters cannot prepare', async () => {
  const h = host({ preflight: { ready: false, missing: [{ code: 'BOM', message: '缺少 BOM', path: '/mes/md/bom' }], orders: [] } })
  await fill(h); await assert.rejects(h.c.prepareLocalTestData, /必须先/)
  await h.c.preflightLocalTestData(); await assert.rejects(h.c.prepareLocalTestData, /必须先/)
  assert.equal(h.calls.writes.filter(row => row[0] === 'prepare').length, 0)
  const ready = host(); await fill(ready); await ready.c.preflightLocalTestData()
  ready.c.localTestDialog.orders[0].quantity = 99
  assert.equal(ready.c.localTestPreflight.value, undefined)
  await assert.rejects(ready.c.prepareLocalTestData, /必须先/)
})

test('empty inputs never call preflight; new RUN is explicit and same RUN sends no reset action', async () => {
  const h = host(); await h.c.openLocalTestData()
  await assert.rejects(h.c.preflightLocalTestData, /正式申请/)
  h.c.localTestDialog.requestId = product.requestId
  await assert.rejects(h.c.preflightLocalTestData, /本轮 RUN/)
  h.c.localTestDialog.runId = 'RUN-ONE'
  await assert.rejects(h.c.preflightLocalTestData, /生产批号/)
  assert.equal(h.calls.writes.length, 0)
  h.c.localTestDialog.orders.forEach(row => { row.batchCode = 'BATCH-' + row.slot })
  await h.c.preflightLocalTestData(); await h.c.prepareLocalTestData()
  assert.equal(h.c.localTestResult.value.orders[0].workOrderId, '800')
  assert.equal(h.c.localTestResult.value.orders[0].status, 'RUNNING')
  await h.c.preflightLocalTestData(); await h.c.prepareLocalTestData()
  const same = h.calls.writes.filter(row => row[0] === 'prepare').map(row => JSON.parse(JSON.stringify(row[1])))
  assert.deepEqual(same[0], same[1])
  assert.equal('reset' in same[1], false)
  h.c.localTestDialog.runId = 'RUN-TWO'
  assert.equal(h.c.localTestResult.value, undefined)
  assert.equal(h.c.localTestPreflight.value, undefined)
  await h.c.preflightLocalTestData(); await h.c.prepareLocalTestData()
  assert.equal(h.calls.writes.at(-1)[1].runId, 'RUN-TWO')
  assert.equal(h.calls.writes.at(-1)[1].requestId, product.requestId)
})

test('test mode and permission are checked before reads; service errors stay visible and never succeed', async () => {
  for (const options of [{ testMode: false }, { permission: false }]) {
    const h = host(options); await assert.rejects(h.c.openLocalTestData, /仅测试模式/); assert.equal(h.calls.reads, 0)
  }
  const failed = host({ prepareError: new Error('RUN_PARAMETERS_CONFLICT') }); await fill(failed)
  await failed.c.preflightLocalTestData(); await assert.rejects(failed.c.prepareLocalTestData, /RUN_PARAMETERS_CONFLICT/)
  assert.equal(failed.c.localTestDialog.error, 'RUN_PARAMETERS_CONFLICT')
  assert.equal(failed.calls.success.length, 0)
  assert.equal(failed.c.localTestDialog.busy, false)
})

test('confirmation cannot submit a changed input or a newly disabled test mode', async () => {
  for (const confirm of [c => { c.localTestDialog.runId = 'OTHER-RUN' }, c => { c.testModeEnabled.value = false }]) {
    const h = host({ confirm }); await fill(h); await h.c.preflightLocalTestData()
    await assert.rejects(h.c.prepareLocalTestData, /输入或测试模式已变化/)
    assert.equal(h.calls.writes.filter(row => row[0] === 'prepare').length, 0)
  }
})

test('API methods use only the independent local data endpoints and retain lossless IDs', async () => {
  const calls = []
  const context = { exports: {}, require: name => {
    assert.equal(name, '@/config/axios')
    return { default: { get: async q => { calls.push(q); return [product] },
      post: async q => { calls.push(q); return q.data } } }
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/mes/pro/workorder/index.ts', 'utf8'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText, context)
  const api = context.exports.ProWorkOrderApi
  await api.getLocalTestApprovedProducts()
  const payload = { source: 'LOCAL_TEST_RUN', runId: 'RUN', requestId: product.requestId,
    projectId: product.projectId, catalogId: product.catalogId, relationId: product.relationId,
    mesProductId: product.mesProductId, orders: [] }
  await api.preflightLocalTestData(payload); await api.prepareLocalTestData(payload)
  assert.deepEqual(calls.map(q => q.url), ['/mes/pro/work-order/local-test-data/approved-products',
    '/mes/pro/work-order/local-test-data/preflight', '/mes/pro/work-order/local-test-data/prepare'])
  assert.equal(calls[2].data.requestId, '9007199254740993')
  assert.equal(calls[2].data, payload)
})

test('normal confirmation cancellation ends without a write or cancel error; other failures remain errors', async () => {
  for (const reason of ['cancel', 'close']) {
    const h = host({ confirm: async () => { throw reason } }); await fill(h); await h.c.preflightLocalTestData()
    await h.c.prepareLocalTestData()
    assert.equal(h.calls.writes.filter(row => row[0] === 'prepare').length, 0)
    assert.equal(h.c.localTestDialog.error, '')
    assert.equal(h.c.localTestDialog.busy, false)
    assert.equal(h.calls.success.length, 0)
  }
  const failed = host({ confirm: async () => { throw new Error('dialog failed') } }); await fill(failed)
  await failed.c.preflightLocalTestData(); await assert.rejects(failed.c.prepareLocalTestData, /dialog failed/)
  assert.equal(failed.c.localTestDialog.error, 'dialog failed')
})
