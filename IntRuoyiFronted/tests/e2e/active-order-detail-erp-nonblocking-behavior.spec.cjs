const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'), 'utf8')
const { descriptor } = parse(source)
const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(node => !ts.isImportDeclaration(node))
  .map(node => node.getText(ast)).join('\n')
const js = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

const apiSource = fs.readFileSync(path.resolve(__dirname,
  '../../src/api/erp/production/material-list/index.ts'), 'utf8')
const apiJs = ts.transpileModule(apiSource.replace(/^import .*$/gm, '').replace(/^export /gm, ''), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const createApi = get => Function('request', `${apiJs}; return ErpProductionMaterialListApi`)({ get })

function setup(getPage) {
  let resolve, reject
  const pending = new Promise((ok, fail) => { resolve = ok; reject = fail })
  const calls = []
  const deps = {
    defineOptions() {}, onMounted() {}, watch() {},
    ref: value => ({ value }),
    useRoute: () => ({ params: { activeOrderId: 1009200057 }, query: {} }),
    useRouter: () => ({ back() {} }),
    ElMessage: { error(message) { throw new Error(message) } },
    getTeamLeaderActiveOrderDetail: async () => ({ workOrderCode: 'WO-TEST', processes: [{}] }),
    ErpProductionMaterialListApi: createApi(config => {
      calls.push(config)
      return getPage ? getPage(config) : pending
    })
  }
  const state = Function(...Object.keys(deps), `${js}; return {
    loadDetail, loading, detail, productionMaterialLists, productionMaterialListLoading,
    get productionMaterialListError() { return typeof productionMaterialListError === 'undefined' ? '' : productionMaterialListError.value }, error
  }`)(...Object.values(deps))
  return { state, resolve, reject, calls }
}

const flush = () => new Promise(resolve => setImmediate(resolve))

for (const outcome of ['empty', 'rows', 'error']) {
  test(`detail is usable before ERP returns: ${outcome}`, async () => {
    const { state, resolve, reject, calls } = setup()
    const loading = state.loadDetail()
    await flush()
    try {
      assert.equal(state.detail.value.workOrderCode, 'WO-TEST')
      assert.equal(state.loading.value, false, 'detail must not wait for optional ERP query')
      assert.equal(state.productionMaterialListLoading.value, true)
      assert.equal(calls.length, 1)
      assert.deepEqual(calls[0].params, { pageNo: 1, pageSize: 100, productionOrderNo: 'WO-TEST' })
      assert.equal(calls[0].ignoreErrorMessage, true, 'optional ERP request must suppress global notifications')
    } finally {
      if (outcome === 'error') reject(new Error('ERP unavailable'))
      else resolve({ list: outcome === 'rows' ? [{ id: 42 }] : [], total: outcome === 'rows' ? 1 : 0 })
      await loading
      await flush()
    }
    assert.equal(state.productionMaterialListLoading.value, false)
    assert.equal(state.loading.value, false)
    assert.equal(state.error.value, '')
    assert.equal(state.productionMaterialListError, '')
    assert.doesNotMatch(source, /:production-material-list-error=/)
    assert.deepEqual(state.productionMaterialLists.value, outcome === 'rows' ? [{ id: 42 }] : [])
  })
}

test('a failed subsequent page does not expose a partial material document', async () => {
  const { state, calls } = setup(async config => {
    if (config.params.pageNo === 1) return { list: [{ id: 42 }], total: 2 }
    throw new Error('ERP second page unavailable')
  })
  await state.loadDetail()
  await flush()
  assert.equal(calls.length, 2)
  assert.ok(calls.every(config => config.ignoreErrorMessage === true))
  assert.deepEqual(state.productionMaterialLists.value, [])
  assert.equal(state.loading.value, false)
  assert.equal(state.productionMaterialListLoading.value, false)
  assert.equal(state.error.value, '')
})

test('other ERP callers retain default error notifications and rejection', async () => {
  const failure = new Error('ERP unavailable')
  let captured
  const api = createApi(async config => { captured = config; throw failure })
  await assert.rejects(api.getPage({ pageNo: 1 }), error => error === failure)
  assert.notEqual(captured.ignoreErrorMessage, true)
})
