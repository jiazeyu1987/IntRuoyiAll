const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { ref, computed } = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const filename = 'src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'
const source = fs.readFileSync(path.resolve(__dirname, '../..', filename), 'utf8')
const descriptor = parse(source).descriptor
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const js = ts.transpileModule(ast.statements.filter(n => !ts.isImportDeclaration(n))
  .map(n => n.getText(ast)).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const flush = () => new Promise(resolve => setImmediate(resolve))
function deferred() {
  let resolve, reject
  const promise = new Promise((ok, fail) => { resolve = ok; reject = fail })
  return { promise, resolve, reject }
}
function setup(materials) {
  const calls = [], route = { query: { activeOrderId: '9223372036854775806' } }
  let unmount
  const deps = {
    ref, computed, defineOptions() {}, onMounted() {}, watch() {},
    onBeforeUnmount(fn) { unmount = fn }, useRoute: () => route, useRouter: () => ({}),
    canReverseTrace: () => false,
    getEdhrBatchActiveOrderDetail: async () => ({ processes: [{}] }),
    getEdhrBatchActiveOrderProductionMaterialLists: query => { calls.push(query); return materials(query) }
  }
  const state = Function(...Object.keys(deps), `${js}; return {
    loadDetail, detail, error, loading, productionMaterialLists,
    productionMaterialListLoading, productionMaterialListError
  }`)(...Object.values(deps))
  return { state, calls, route, unmount: () => unmount() }
}
test('page compiles and passes material data, loading and failures to the panel', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'test' }).errors, [])
  for (const [prop, state] of Object.entries({
    'production-material-lists': 'productionMaterialLists',
    'production-material-list-loading': 'productionMaterialListLoading',
    'production-material-list-error': 'productionMaterialListError'
  })) assert.ok(source.includes(`:${prop}="${state}"`))
})
for (const identity of ['activeOrderId', 'batchExecutionId']) {
  test(`loads formal materials with unchanged ${identity}, without blocking the detail`, async () => {
    const pending = deferred()
    const { state: s, calls, route } = setup(() => pending.promise)
    route.query = { [identity]: '9223372036854775806' }
    await s.loadDetail()
    assert.ok(s.detail.value)
    assert.equal(s.loading.value, false)
    assert.equal(s.productionMaterialListLoading.value, true)
    assert.deepEqual(calls, [{ [identity]: '9223372036854775806' }])
    const rows = Array.from({ length: 241 }, (_, id) => ({ id, sourceBillNo: 'FORMAL-1' }))
    pending.resolve(rows)
    await flush()
    assert.deepEqual(s.productionMaterialLists.value, rows)
    assert.equal(s.productionMaterialListLoading.value, false)
  })
}
test('failed query is visible and retry clears the error and restores rows', async () => {
  let attempt = 0
  const { state: s } = setup(async () => {
    if (++attempt === 1) throw new Error('无查询权限')
    return [{ id: 1 }]
  })
  await s.loadDetail(); await flush()
  assert.match(s.productionMaterialListError.value, /无查询权限/)
  assert.equal(s.error.value, '')
  await s.loadDetail(); await flush()
  assert.equal(s.productionMaterialListError.value, '')
  assert.deepEqual(s.productionMaterialLists.value, [{ id: 1 }])
})
for (const outcome of ['success', 'failure']) {
  test(`old ${outcome} cannot overwrite another order's materials`, async () => {
    const old = deferred()
    const { state: s, route } = setup(q => q.activeOrderId === '2' ? Promise.resolve([{ id: 2 }]) : old.promise)
    await s.loadDetail()
    route.query.activeOrderId = '2'
    await s.loadDetail(); await flush()
    if (outcome === 'success') old.resolve([{ id: 1 }])
    else old.reject(new Error('old error'))
    await flush()
    assert.deepEqual(s.productionMaterialLists.value, [{ id: 2 }])
    assert.equal(s.productionMaterialListError.value, '')
  })
}
test('malformed response is reported rather than displayed as empty', async () => {
  const { state: s } = setup(async () => ({ list: [] }))
  await s.loadDetail(); await flush()
  assert.match(s.productionMaterialListError.value, /返回格式错误/)
})
test('unmount invalidates pending material response', async () => {
  const pending = deferred()
  const { state: s, unmount } = setup(() => pending.promise)
  await s.loadDetail(); unmount()
  pending.resolve([{ id: 1 }]); await flush()
  assert.deepEqual(s.productionMaterialLists.value, [])
})
