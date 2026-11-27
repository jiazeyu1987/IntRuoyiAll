const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const read = (file) => fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/route', file), 'utf8')
const designer = read('RouteFlowGraphDesigner.vue')
const index = read('index.vue')
const extract = (source, start, end) => source.slice(source.indexOf(start), source.indexOf(end, source.indexOf(start)))
const run = (code, name, values) => {
  const context = vm.createContext({ Error, ...values })
  vm.runInContext(ts.transpileModule(code + '\nglobalThis.handler = ' + name, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.handler
}
const box = (value) => ({ value })
// Include the whole function up to its following top-level declaration.
const addStart = designer.indexOf('const handleRouteProcessAdd =')
const add = designer.slice(addStart, designer.indexOf('\nconst ', addStart + 1))
test('expected process validation rejection stops before any graph mutation; real errors propagate', async () => {
  for (const failure of [{ processId: [{ field: 'processId', message: '请选择工序' }] }, new Error('validator crashed'), { processId: [] }]) {
    let graphReads = 0
    const handler = run(add, 'handleRouteProcessAdd', {
      canMutateRouteFlow: box(true), routeProcessFormRef: box({ validate: async () => { throw failure } }),
      routeProcessForm: { processId: 1 }, routeNodes: { get value() { graphReads++; return [] } }
    })
    if (failure.processId?.length) await handler()
    else await assert.rejects(handler(), (error) => error === failure)
    assert.equal(graphReads, 0)
  }
})
const load = extract(designer, 'const loadBatchRecordReportOptions =', '\nconst ')
test('report permission blocks requests; permitted requests load and failures propagate', async () => {
  for (const allowed of [false, true]) {
    let requests = 0
    const options = box([]), loading = box(false)
    const handler = run(load, 'loadBatchRecordReportOptions', {
      canQueryBatchRecordReports: box(allowed), batchRecordReportOptionsLoading: loading,
      batchRecordReportOptions: options, normalizeNullableText: (v) => v,
      dedupeBatchRecordReportOptions: (v) => v,
      BatchRecordReportApi: { getGeneratedReportPage: async () => { requests++; return { list: [{ reportId: 'report-1' }] } } }
    })
    await handler('report')
    assert.equal(requests, allowed ? 1 : 0)
    assert.equal(options.value.length, allowed ? 1 : 0)
    assert.equal(loading.value, false)
  }
  const failure = new Error('request failed'), loading = box(false)
  const handler = run(load, 'loadBatchRecordReportOptions', {
    canQueryBatchRecordReports: box(true), batchRecordReportOptionsLoading: loading,
    normalizeNullableText: (v) => v,
    BatchRecordReportApi: { getGeneratedReportPage: async () => { throw failure } }
  })
  await assert.rejects(handler(), (error) => error === failure)
  assert.equal(loading.value, false)
})
test('successful version actions refresh versions, directory tree and list in order', async () => {
  const code = extract(index, 'const reloadRouteVersionWorkspace =', '\nconst ') + '\n' + extract(index, 'const runRouteVersionAction =', '\nconst ')
  for (const actionName of ['删除草稿', '发布', '撤回', '重开']) {
    const calls = []
    const handler = run(code, 'runRouteVersionAction', {
      routeVersionRoute: box({ id: 7 }), routeVersionActionLoadingId: box(),
      routeVersionNoticeMessage: box(''), routeVersionErrorMessage: box(''),
      loadRouteVersions: async (id) => calls.push(['versions', id]),
      routeTreePanelRef: box({ load: async () => calls.push('tree') }), getList: async () => calls.push('list'),
      message: { success() {}, error(text) { throw new Error(text) } }, resolveRouteVersionErrorMessage: (e) => e.message
    })
    await handler(8, actionName, async () => calls.push(actionName))
    assert.deepEqual(calls, [actionName, ['versions', 7], 'tree', 'list'])
  }
})
test('valid process selection adds and selects a draft node', async () => {
  const nodes = box([]), nextId = box(-1), visible = box(true), selected = box()
  const calls = []
  const handler = run(add, 'handleRouteProcessAdd', {
    canMutateRouteFlow: box(true), routeProcessFormRef: box({ validate: async () => true }),
    routeProcessForm: { processId: 12 }, routeNodes: nodes, routeProcessIdKey: String,
    processOptions: box([{ id: 12, code: 'P12', name: '工序' }]), nextDraftRouteProcessId: nextId,
    ensureSelectedProcessRouteConfigCache: async () => calls.push('cache'),
    defaultNodePosition: () => ({ x: 10, y: 20 }),
    inheritGlobalRecordBindingsForRouteProcess: (id) => calls.push(['inherit', id]),
    routeProcessDialogVisible: visible, selectedRouteProcessId: selected, selectedEdgeKey: box('old'),
    markGraphDraftChanged: () => calls.push('dirty'), syncFlowElements: () => calls.push('sync'),
    message: { warning: () => calls.push('notice'), error: (text) => { throw new Error(text) } }
  })
  await handler()
  assert.equal(nodes.value.length, 1)
  assert.equal(nodes.value[0].processId, 12)
  assert.equal(nodes.value[0].routeProcessId, -1)
  assert.equal(nextId.value, -2)
  assert.equal(selected.value, -1)
  assert.equal(visible.value, false)
  assert.deepEqual(calls, ['cache', ['inherit', -1], 'dirty', 'sync', 'notice'])
})
test('actual publish handler submits reviewed version then refreshes versions, tree and list', async () => {
  const code = extract(index, 'const reloadRouteVersionWorkspace =', '\nconst ') + '\n' + extract(index, 'const submitRouteCandidateVersion =', '\nconst ')
  const calls = [], version = { id: 8 }, loading = box()
  const handler = run(code, 'submitRouteCandidateVersion', {
    routeVersionRoute: box({ id: 7 }), routeVersionActionLoadingId: loading,
    routeVersionNoticeMessage: box(''), routeVersionErrorMessage: box(''),
    resolveLatestRouteVersionForSubmit: async (v) => { assert.equal(v, version); calls.push('latest'); return v },
    submitRouteVersionWithTreeReview: async (v) => { assert.equal(v, version); calls.push('submit'); return { lifecycleStatus: 'ACTIVE' } },
    resolveRoutePublishSuccessMessage: () => 'published',
    loadRouteVersions: async (id) => calls.push(['versions', id]),
    routeTreePanelRef: box({ load: async () => calls.push('tree') }), getList: async () => calls.push('list'),
    message: { success() {}, error(text) { throw new Error(text) } },
    isUserCancel: () => false, resolveRouteVersionErrorMessage: (e) => e.message
  })
  await handler(version)
  assert.deepEqual(calls, ['latest', 'submit', ['versions', 7], 'tree', 'list'])
  assert.equal(loading.value, undefined)
})
