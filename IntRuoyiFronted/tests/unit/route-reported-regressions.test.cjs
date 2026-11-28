const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

function loadFunctions(file, names, context) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/route', file), 'utf8')
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile(file, descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declarations = ast.statements.filter(ts.isVariableStatement)
    .flatMap(statement => [...statement.declarationList.declarations])
    .filter(declaration => names.includes(declaration.name.getText(ast)))
  assert.equal(declarations.length, names.length)
  const code = declarations.map(declaration => `let ${declaration.getText(ast)};`).join('\n')
  const executable = ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), executable + `\nreturn {${names.join(',')}}`)(...Object.values(context))
}

test('batch record links carry the target query permission and keep formal report identities', () => {
  const reports = [{ batchRecordReportId: '900000000000000001', name: '批记录甲' },
    { batchRecordReportId: '900000000000000002', name: '批记录乙' }]
  const { buildBatchRecordFormLinks } = loadFunctions('RouteFlowGraphDesigner.vue', ['buildBatchRecordFormLinks'], {
    getSelectedBatchRecordForms: () => reports,
    isLegacyBatchRecordConfigured: () => true,
    getLegacyBatchRecordDisplayName: row => row.name,
    openLegacyBatchRecordTargetLink() {}
  })
  assert.deepEqual(buildBatchRecordFormLinks().map(({ key, label, permission }) => ({ key, label, permission })),
    reports.map(row => ({ key: `batch-record-report-${row.batchRecordReportId}`, label: row.name,
      permission: 'mes:pro-batch-record-template:query' })))
})

test('batch record navigation checks current permission before saving return state or routing', async () => {
  let permission = false
  const calls = []
  const { openLegacyBatchRecordTargetLink } = loadFunctions('RouteFlowGraphDesigner.vue', ['openLegacyBatchRecordTargetLink'], {
    canQueryBatchRecordReports: { get value() { return permission } },
    normalizeNullableText: value => value,
    persistRouteFlowReturnState: async () => calls.push('persist'),
    requireLegacyBatchRecordSlotType: () => 'MAIN', props: { routeId: '100' },
    selectedRouteProcessId: { value: '200' }, router: { push: async target => calls.push(target) }
  })
  const report = { batchRecordReportId: '900000000000000001' }
  await openLegacyBatchRecordTargetLink(report)
  assert.deepEqual(calls, [])
  permission = true
  await openLegacyBatchRecordTargetLink(report)
  assert.deepEqual(calls, ['persist', { path: '/mes/pro/batch-record-form-list',
    query: { reportId: report.batchRecordReportId, formSlotType: 'MAIN', routeId: '100', routeProcessId: '200' } }])
})

test('empty process validation is handled, while unexpected validation failures still surface', async () => {
  const expected = { processId: [{ field: 'processId', message: '请选择工序' }] }
  for (const failure of [expected, new Error('校验服务异常'), { processId: [{ field: 'processId', message: '其他错误' }] }]) {
    const { handleRouteProcessAdd } = loadFunctions('RouteFlowGraphDesigner.vue', ['handleRouteProcessAdd'], {
      canMutateRouteFlow: { value: true }, routeProcessFormRef: { value: { validate: async () => { throw failure } } }
    })
    if (failure === expected) await assert.doesNotReject(handleRouteProcessAdd)
    else await assert.rejects(handleRouteProcessAdd, error => error === failure)
  }
})

function treeLoader() {
  const pending = [], rows = { value: [] }, loading = { value: false }, error = { value: '' }
  const functions = loadFunctions('RouteTreePanel.vue', ['treeLoadSequence', 'load'], {
    rows, loading, error, resolveRouteOperationErrorMessage: failure => failure.message,
    RouteTreeApi: { list: () => new Promise((resolve, reject) => pending.push({ resolve, reject })) }
  })
  return { ...functions, pending, rows, loading, error }
}

test('an earlier tree result cannot overwrite a refreshed version summary', async () => {
  const h = treeLoader(), oldRequest = h.load(), refresh = h.load()
  const published = [{ id: 1, activeRouteVersionNo: 'V2', pendingStatus: null }]
  h.pending[1].resolve(published); await refresh
  h.pending[0].resolve([{ id: 1, activeRouteVersionNo: 'V1', pendingStatus: 'DRAFT' }]); await oldRequest
  assert.deepEqual(h.rows.value, published)
  assert.equal(h.loading.value, false)
})

test('an obsolete tree failure cannot clear current loading or replace current errors', async () => {
  const h = treeLoader(), oldRequest = h.load(), refresh = h.load()
  h.pending[0].reject(new Error('旧请求失败')); await oldRequest
  assert.equal(h.loading.value, true)
  assert.equal(h.error.value, '')
  h.pending[1].reject(new Error('当前请求失败')); await refresh
  assert.equal(h.loading.value, false)
  assert.equal(h.error.value, '当前请求失败')
})

test('version workspace refresh includes both the tree and ordinary list', async () => {
  const calls = []
  const { reloadRouteVersionWorkspace } = loadFunctions('index.vue', ['reloadRouteVersionWorkspace'], {
    routeVersionRoute: { value: { id: 1 } }, loadRouteVersions: async id => calls.push(['versions', id]),
    routeTreePanelRef: { value: { load: async () => calls.push('tree') } }, getList: async () => calls.push('list')
  })
  await reloadRouteVersionWorkspace()
  assert.deepEqual(calls, [['versions', 1], 'tree', 'list'])
})
