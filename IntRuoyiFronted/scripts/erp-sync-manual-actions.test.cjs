const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const front = path.resolve(__dirname, '..')
const vue = require(path.join(front, 'node_modules/vue'))
const ts = require(path.join(front, 'node_modules/typescript'))
const { parse } = require(path.join(front, 'node_modules/vue/compiler-sfc'))
const file = path.join(front, 'src/views/erp/sync/config/components/TableAutoSyncSetting.vue')
const source = parse(fs.readFileSync(file, 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
const compiled = ts.transpileModule(body, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function harness({ allowed = true, fail = false } = {}) {
  const calls = [], errors = [], successes = []
  const context = { ...vue,
    defineOptions() {}, onMounted() {}, onActivated() {}, onDeactivated() {}, onBeforeUnmount() {}, watch() {},
    useDocumentVisibility: () => vue.ref('visible'), useTimeoutPoll: () => ({ pause() {}, resume() {} }),
    useMessage: () => ({ error: x => errors.push(x) }),
    hasErpConfigCapability: () => allowed,
    ElMessage: { error: x => errors.push(x), success: x => successes.push(x), warning() {} },
    InfraJobStatusEnum: { NORMAL: 1, STOP: 2 }, ErpKingdeeConfigApi: {}, JobApi: {},
    formatDateTimeValue: x => x, toDateTimeValue: x => x,
    ErpKingdeeSyncApi: {
      runFullSync: async type => { calls.push(['FULL', type]); if (fail) throw new Error('source unavailable') },
      runIncrementalSync: async type => { calls.push(['INCREMENTAL', type]); if (fail) throw new Error('source unavailable') },
      getRunPage: async query => ({ list: query.status === 10 ? [] : [{ id: 2, syncType: query.syncType, status: 20 }], total: query.status === 10 ? 0 : 1 })
    }
  }
  const instance = Function(...Object.keys(context), compiled + '; return { handleRunSync, syncingType, syncingMode, latestRuns };')(...Object.values(context))
  return { instance, calls, errors, successes }
}
test('each mode submits its own endpoint and displays the new real run facts', async () => {
  for (const mode of ['FULL', 'INCREMENTAL']) {
    const h = harness()
    h.instance.latestRuns.value = [{ id: 1, syncType: 'SALE_ORDER', status: 20 }]
    await h.instance.handleRunSync({ syncType: 'SALE_ORDER', erpTableName: '销售订单' }, mode)
    assert.deepEqual(h.calls, [[mode, 'SALE_ORDER']])
    assert.equal(h.instance.syncingType.value, '')
    assert.equal(h.instance.syncingMode.value, mode)
    assert.equal(h.instance.latestRuns.value.find(r => r.syncType === 'SALE_ORDER').id, 2)
    assert.equal(h.errors.length, 0)
    assert.equal(h.successes.length, 1)
  }
})
test('denied operation sends no request in either mode', async () => {
  const h = harness({ allowed: false })
  for (const mode of ['FULL', 'INCREMENTAL']) await h.instance.handleRunSync({ syncType: 'SALE_ORDER' }, mode)
  assert.equal(h.calls.length, 0)
  assert.equal(h.errors.length, 2)
})
test('submission failure remains visible and releases loading in either mode', async () => {
  for (const mode of ['FULL', 'INCREMENTAL']) {
    const h = harness({ fail: true })
    await h.instance.handleRunSync({ syncType: 'SALE_ORDER' }, mode)
    assert.match(h.errors[0], /source unavailable/)
    assert.equal(h.successes.length, 0)
    assert.equal(h.instance.syncingType.value, '')
  }
})
