const assert = require('node:assert/strict')
const fs = require('node:fs'),
  path = require('node:path'),
  test = require('node:test'),
  ts = require('typescript')
const { ref } = require('vue'),
  { parse } = require('vue/compiler-sfc')
const read = (file) => fs.readFileSync(path.resolve(__dirname, '../../', file), 'utf8')
const source = read('src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const panel = read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
function executable(source) {
  const ast = ts.createSourceFile('detail.ts', source, ts.ScriptTarget.Latest, true)
  const body = ast.statements
    .filter((n) => !ts.isImportDeclaration(n))
    .map((n) => n.getText(ast).replace(/^export /, ''))
    .join('\n')
  return ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } })
    .outputText
}
const js = executable(parse(source).descriptor.scriptSetup.content)
const apiJs = executable(read('src/api/mes/pro/processpool/teamLeader.ts'))
const parsePositiveRouteQueryId = Function(
  `${executable(read('src/utils/routeQueryId.ts'))}; return parsePositiveRouteQueryId`
)()
const createApi = (get) =>
  Function('request', `${apiJs}; return getTeamLeaderActiveOrderProductionMaterialLists`)({ get })
const flush = () => new Promise((resolve) => setImmediate(resolve))
function deferred() {
  let resolve, reject
  const promise = new Promise((ok, fail) => {
    resolve = ok
    reject = fail
  })
  return { promise, resolve, reject }
}
function setup({ materials, detail } = {}) {
  const pending = deferred(),
    calls = [],
    messages = [],
    workOrderCalls = []
  const route = { params: { activeOrderId: '1009200437' }, query: {} }
  let unmount
  const deps = {
    parsePositiveRouteQueryId,
    defineOptions() {},
    onMounted() {},
    watch() {},
    ref,
    onBeforeUnmount(fn) {
      unmount = fn
    },
    useRoute: () => route,
    useRouter: () => ({ back() {} }),
    ElMessage: {
      error(msg) {
        messages.push(msg)
      }
    },
    getTeamLeaderActiveOrderDetail: async (id) =>
      detail ? detail(id) : { workOrderCode: `WO-${id}`, processes: [{}] },
    getTeamLeaderActiveOrderProductionMaterialLists: createApi((config) => {
      calls.push(config)
      return materials ? materials(config) : pending.promise
    }),
    ProWorkOrderApi: {
      getWorkOrderPage: async (params) => {
        workOrderCalls.push(params)
        return { list: [{ code: params.code }] }
      }
    }
  }
  const state = Function(
    ...Object.keys(deps),
    `${js}; return { loadDetail, loading, detail, sourceWorkOrder, productionMaterialLists, productionMaterialListLoading, productionMaterialListError, error }`
  )(...Object.values(deps))
  return { state, pending, calls, messages, workOrderCalls, route, unmount: () => unmount() }
}
for (const outcome of ['empty', 'rows', 'error'])
  test(`main detail usable before scoped material request completes: ${outcome}`, async () => {
    const { state: s, pending, calls, messages } = setup()
    await s.loadDetail()
    assert.equal(s.detail.value.workOrderCode, 'WO-1009200437')
    assert.equal(s.loading.value, false)
    assert.equal(s.productionMaterialListLoading.value, true)
    assert.equal(calls.length, 1)
    assert.equal(
      calls[0].url,
      '/mes/pro/process-pool/team-leader/active-order/production-material-lists'
    )
    assert.deepEqual(calls[0].params, { activeOrderId: '1009200437' })
    assert.equal(calls[0].ignoreErrorMessage, true)
    if (outcome === 'error') pending.reject(new Error('用料查询无权限'))
    else pending.resolve(outcome === 'rows' ? [{ id: 42 }, { id: 43 }] : [])
    await flush()
    assert.equal(s.productionMaterialListLoading.value, false)
    assert.equal(s.error.value, '')
    assert.equal(s.productionMaterialListError.value, outcome === 'error' ? '用料查询无权限' : '')
    assert.deepEqual(messages, [])
    assert.deepEqual(
      s.productionMaterialLists.value,
      outcome === 'rows' ? [{ id: 42 }, { id: 43 }] : []
    )
  })
test('material failure visible and existing retry event reloads successfully', async () => {
  let attempt = 0
  const { state: s } = setup({
    materials: async () => {
      if (++attempt === 1) throw { response: { data: { msg: '正式用料查询失败' } } }
      return [{ id: 52 }]
    }
  })
  await s.loadDetail()
  await flush()
  assert.equal(s.productionMaterialListError.value, '正式用料查询失败')
  assert.match(source, /:production-material-list-error="productionMaterialListError"/)
  const tab = panel.match(/<el-tab-pane\s+label="生产用料清单"[\s\S]*?<\/el-tab-pane>/)?.[0]
  assert.match(tab, /v-if="productionMaterialListError"[\s\S]*:title="productionMaterialListError"/)
  assert.match(tab, /@click="\$emit\('retry'\)"[\s\S]*重新加载生产用料清单/)
  assert.match(source, /@retry="loadDetail"/)
  assert.match(tab, /v-else-if="productionMaterialListDocuments.length"[\s\S]*<el-empty\s+v-else/)
  assert.match(
    tab,
    /data-active-order-production-material-list-scope[\s\S]*当前活跃订单生产用料清单 · 生产工单：\{\{ detail\.workOrderCode \}\}/
  )
  assert.match(
    tab,
    /:data-active-order-production-material-list-state="[\s\S]*productionMaterialListLoading \? 'loading' : productionMaterialListError \? 'error' : 'success'/
  )
  assert.match(tab, /data-active-order-production-material-list-error/)
  assert.match(tab, /data-active-order-production-material-list-empty/)
  await s.loadDetail()
  await flush()
  assert.equal(s.productionMaterialListError.value, '')
  assert.deepEqual(s.productionMaterialLists.value, [{ id: 52 }])
})
test('all material rows retained without generic ERP querying or frontend pagination', async () => {
  const rows = Array.from({ length: 241 }, (_, id) => ({ id, sourceBillNo: `DOC-${id % 3}` }))
  const { state: s, calls } = setup({ materials: async () => rows })
  await s.loadDetail()
  await flush()
  assert.deepEqual(s.productionMaterialLists.value, rows)
  assert.equal(calls.length, 1)
  assert.doesNotMatch(source, /ErpProductionMaterialListApi|productionOrderNo/)
})
test('source work order is display-only and request preserves long active-order ID', async () => {
  const { state: s, calls, workOrderCalls, route } = setup({ materials: async () => [] })
  route.params.activeOrderId = '9223372036854775806'
  route.query.sourceWorkOrderCode = 'SOURCE-WO'
  await s.loadDetail()
  await flush()
  assert.equal(s.sourceWorkOrder.value.code, 'SOURCE-WO')
  assert.deepEqual(workOrderCalls, [{ pageNo: 1, pageSize: 20, code: 'SOURCE-WO' }])
  assert.deepEqual(calls[0].params, { activeOrderId: '9223372036854775806' })
})
for (const outcome of ['success', 'failure']) {
  test(`old material ${outcome} cannot replace current pending or successful state`, async () => {
    const old = deferred(),
      current = deferred()
    const { state: s, route } = setup({
      materials: (c) => (c.params.activeOrderId === '1009200437' ? old.promise : current.promise)
    })
    await s.loadDetail()
    route.params.activeOrderId = '1009200438'
    await s.loadDetail()
    if (outcome === 'success') old.resolve([{ id: 'OLD' }])
    else old.reject(new Error('OLD FAILURE'))
    await flush()
    assert.equal(s.productionMaterialListLoading.value, true)
    assert.equal(s.productionMaterialListError.value, '')
    assert.deepEqual(s.productionMaterialLists.value, [])
    current.resolve([{ id: 'CURRENT' }])
    await flush()
    assert.deepEqual(s.productionMaterialLists.value, [{ id: 'CURRENT' }])
    assert.equal(s.productionMaterialListLoading.value, false)
  })
  test(`old main detail ${outcome} cannot replace current order or start material request`, async () => {
    const old = deferred()
    const {
      state: s,
      route,
      calls,
      messages
    } = setup({
      detail: (id) =>
        id === '1009200437' ? old.promise : { workOrderCode: 'CURRENT', processes: [{}] },
      materials: async () => [{ id: 'CURRENT' }]
    })
    const first = s.loadDetail()
    route.params.activeOrderId = '1009200438'
    await s.loadDetail()
    if (outcome === 'success') old.resolve({ workOrderCode: 'OLD', processes: [{}] })
    else old.reject(new Error('OLD DETAIL FAILURE'))
    await first
    await flush()
    assert.equal(s.detail.value.workOrderCode, 'CURRENT')
    assert.deepEqual(
      calls.map((c) => c.params),
      [{ activeOrderId: '1009200438' }]
    )
    assert.equal(s.error.value, '')
    assert.deepEqual(messages, [])
  })
}
test('old success cannot erase current material failure', async () => {
  const old = deferred()
  const { state: s, route } = setup({
    materials: (c) => {
      if (c.params.activeOrderId === '1009200437') return old.promise
      throw new Error('CURRENT FAILURE')
    }
  })
  await s.loadDetail()
  route.params.activeOrderId = '1009200438'
  await s.loadDetail()
  await flush()
  old.resolve([{ id: 'OLD' }])
  await flush()
  assert.equal(s.productionMaterialListError.value, 'CURRENT FAILURE')
  assert.deepEqual(s.productionMaterialLists.value, [])
})
test('old material success arriving after current success cannot replace current rows', async () => {
  const old = deferred()
  const { state: s, route } = setup({
    materials: (config) =>
      config.params.activeOrderId === '1009200437' ? old.promise : [{ id: 'CURRENT' }]
  })
  await s.loadDetail()
  route.params.activeOrderId = '1009200438'
  await s.loadDetail()
  await flush()
  old.resolve([{ id: 'OLD' }])
  await flush()
  assert.deepEqual(s.productionMaterialLists.value, [{ id: 'CURRENT' }])
  assert.equal(s.productionMaterialListError.value, '')
  assert.equal(s.productionMaterialListLoading.value, false)
})
test('old main-detail failure cannot clear current pending detail loading', async () => {
  const old = deferred(),
    current = deferred()
  const {
    state: s,
    route,
    calls,
    messages
  } = setup({
    detail: (id) => (id === '1009200437' ? old.promise : current.promise),
    materials: async () => []
  })
  const first = s.loadDetail()
  route.params.activeOrderId = '1009200438'
  const second = s.loadDetail()
  old.reject(new Error('OLD FAILURE'))
  await first
  assert.equal(s.loading.value, true)
  assert.deepEqual(calls, [])
  assert.deepEqual(messages, [])
  current.resolve({ workOrderCode: 'CURRENT', processes: [{}] })
  await second
  await flush()
  assert.equal(s.loading.value, false)
  assert.deepEqual(
    calls.map((call) => call.params),
    [{ activeOrderId: '1009200438' }]
  )
})
test('invalid order identity fails explicitly before any material query', async () => {
  const { state: s, route, calls, messages } = setup()
  route.params.activeOrderId = 'invalid-order'
  await s.loadDetail()
  assert.equal(s.error.value, '活跃订单记录ID不能为空')
  assert.equal(s.loading.value, false)
  assert.deepEqual(calls, [])
  assert.deepEqual(messages, ['活跃订单记录ID不能为空'])
})
test('malformed response is explicit failure', async () => {
  const { state: s } = setup({ materials: async () => ({ list: [], total: 0 }) })
  await s.loadDetail()
  await flush()
  assert.equal(s.productionMaterialListError.value, '生产用料清单返回格式错误')
})
test('unmount invalidates pending material responses', async () => {
  const { state: s, pending, unmount } = setup()
  await s.loadDetail()
  unmount()
  pending.resolve([{ id: 'AFTER UNMOUNT' }])
  await flush()
  assert.deepEqual(s.productionMaterialLists.value, [])
})
test('scoped API propagates formal rejection for local visible handler', async () => {
  const failure = new Error('MES denied')
  await assert.rejects(
    createApi(async () => {
      throw failure
    })('1009200437'),
    (error) => error === failure
  )
})
