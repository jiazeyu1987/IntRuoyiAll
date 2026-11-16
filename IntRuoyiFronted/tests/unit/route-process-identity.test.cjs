const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const source = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('designer.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const declarations = new Map()
for (const statement of ast.statements) {
  if (!ts.isVariableStatement(statement)) continue
  for (const declaration of statement.declarationList.declarations) {
    if (ts.isIdentifier(declaration.name)) declarations.set(declaration.name.text, `const ${declaration.getText(ast)};`)
  }
}
const compile = code => ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const identity = { exports: {} }
vm.runInNewContext(compile(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/routeProcessIdentity.ts'), 'utf8')), identity)
const A = '2116486795629948963', B = '2116486795629948964'
const names = ['selectedNode', 'mergeRouteProcessRowWithNode', 'candidateAwareRouteProcessRows', 'selectedRouteProcess',
  'selectedPredecessorRouteProcessIds', 'selectedSuccessorRouteProcessIds', 'routeProcessRelationOptions',
  'findProductionProcessConfig', 'formatPublishedProductionProcessConfig', 'findRouteProcessConfig', 'findNode',
  'readRouteFlowLastSelection', 'persistRouteFlowLastSelection', 'selectRouteProcessNode', 'restoreRouteFlowSelection',
  'handleNodeClick', 'handleNodeDragStop', 'isSelectedProcessDetailRequestCurrent', 'loadSelectedRouteProcessDeviceParameterConfig']
function setup(ids = [A, B]) {
  const cache = new Map(), requests = []
  const ref = value => ({ value })
  const context = { ...identity.exports,
    computed: fn => ({ get value() { return fn() } }),
    props: { routeVersionEditContext: { routeVersionId: '2116486795650920457' } },
    routeNodes: ref(ids.map((id, i) => ({ routeProcessId: id, processName: `node-${i}`, processId: 11 + i }))),
    routeProcessRows: ref(ids.map((id, i) => ({ id, remark: `row-${i}`, processId: 11 + i }))),
    routeEdges: ref([{ sourceRouteProcessId: ids[0], targetRouteProcessId: ids[1] }]),
    sortedActiveRouteNodes: ref(ids.map(id => ({ routeProcessId: id }))),
    canMutateRouteFlow: ref(true), isBoundaryNodeId: id => id === 'start' || id === 'end',
    syncFlowElements: () => requests.push('syncFlowElements'),
    selectedRouteProcessId: ref(null), selectedBoundaryType: ref(null), selectedBoundaryDetailFieldKey: ref(), selectedEdgeKey: ref(''),
    selectedProcessDetailFieldKey: ref('deviceParameters'), selectedProcessDetailFieldKeys: ref(['deviceParameters']),
    selectedProcessRouteConfigCache: ref({ productionProcessConfigSnapshot: { productionProcessConfigs: ids.map((id, i) => ({ routeProcessId: id, parameterRules: [{ name: `parameter-${i}` }] })) } }),
    routeProcessDeviceParameterRequestId: 0, routeProcessDeviceParameterLoading: ref(false), routeProcessDeviceParameterConfig: ref(),
    selectedProcessDetailRequestId: 3, processDetailInterestReady: ref(false),
    buildRouteFlowLastSelectionCacheKey: () => 'selection',
    normalizeProcessDetailFieldKey: key => key, PROCESS_DETAIL_FIELD_KEY_SET: new Set(['deviceParameters']),
    wsCache: { get: key => cache.get(key), set: (key, value) => cache.set(key, value) },
    removeRouteFlowLastSelection: () => cache.delete('selection'),
    requireRouteProcessDeviceParameterVersionId: () => '2116486795650920457',
    ProRouteFlowConfigApi: { getRouteProcessDeviceParameterConfig: async (version, id) => { requests.push({ version, id }); return { routeProcessId: id } } },
    PROCESS_START_NODE_ID: 'start', PROCESS_END_NODE_ID: 'end',
    message: { error(message) { throw new Error(message) } }, resolveErrorMessage: error => error.message,
    exports: {}
  }
  vm.runInNewContext(compile(names.map(name => { assert.ok(declarations.has(name), name); return declarations.get(name) }).join('\n') + `\nObject.assign(exports, { ${names.join(',')} });`), context)
  return { ...context, ...context.exports, cache, requests }
}

test('routeId prop accepts exact string IDs at the Vue runtime boundary', () => {
  const filename = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
  const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
  const compiled = compileScript(descriptor, {
    id: 'route-id-test',
    fs: { fileExists: fs.existsSync, readFile: file => fs.readFileSync(file, 'utf8') }
  }).content
  assert.match(compiled, /routeId: \{ type: \[Number, String\], required: true \}/)
})

test('node clicks keep adjacent snowflake IDs distinct across selected rows, relations and parameters', async () => {
  assert.equal(Number(A), Number(B))
  const h = setup()
  assert.equal(h.candidateAwareRouteProcessRows.value.length, 2)
  for (const [index, id] of [A, B].entries()) {
    h.handleNodeClick({ node: { id } })
    assert.equal(h.selectedRouteProcessId.value, id)
    assert.equal(h.selectedNode.value.processName, `node-${index}`)
    assert.equal(h.selectedRouteProcess.value.remark, `row-${index}`)
    assert.equal(h.findRouteProcessConfig([{ routeProcessId: A, text: 'first' }, { routeProcessId: B, text: 'second' }], id).text, index ? 'second' : 'first')
    assert.equal(JSON.parse(h.formatPublishedProductionProcessConfig()).parameterRules[0].name, `parameter-${index}`)
    assert.equal(h.routeProcessRelationOptions.value.length, 1)
    assert.equal(h.routeProcessRelationOptions.value[0].routeProcessId, index ? A : B)
    await h.loadSelectedRouteProcessDeviceParameterConfig()
    assert.equal(h.requests.at(-1).id, id)
    assert.equal(h.routeProcessDeviceParameterConfig.value.routeProcessId, id)
  }
  assert.deepEqual(Array.from(h.selectedPredecessorRouteProcessIds.value), [A])
  assert.deepEqual(Array.from(h.selectedSuccessorRouteProcessIds.value), [])
  assert.equal(h.isSelectedProcessDetailRequestCurrent(3, A), false)
  assert.equal(h.isSelectedProcessDetailRequestCurrent(3, B), true)
})

test('selection persistence and restore preserve exact long IDs and detail field', () => {
  const h = setup()
  h.handleNodeClick({ node: { id: B } })
  const selection = h.readRouteFlowLastSelection()
  assert.equal(selection.routeProcessId, B)
  assert.equal(selection.detailFieldKey, 'deviceParameters')
  h.selectedRouteProcessId.value = null
  assert.equal(h.restoreRouteFlowSelection(selection, { source: 'cache' }), true)
  assert.equal(h.selectedNode.value.routeProcessId, B)
})

test('dragging adjacent long ID nodes changes only the exact node coordinates', () => {
  const h = setup()
  const [first, second] = h.routeNodes.value
  h.handleNodeDragStop({ node: { id: B, position: { x: 123.7, y: -4 } } })
  assert.equal(second.routeProcessId, B)
  assert.equal(second.x, 124)
  assert.equal(second.y, 0)
  assert.equal(first.x, undefined)
  assert.equal(first.y, undefined)
  h.handleNodeDragStop({ node: { id: A, position: { x: 77, y: 88.2 } } })
  assert.equal(first.routeProcessId, A)
  assert.equal(first.x, 77)
  assert.equal(first.y, 88)
  assert.equal(second.x, 124)
  assert.equal(second.y, 0)
  assert.deepEqual(h.requests, ['syncFlowElements', 'syncFlowElements'])
  h.canMutateRouteFlow.value = false
  h.handleNodeDragStop({ node: { id: B, position: { x: 999, y: 999 } } })
  assert.equal(second.x, 124)
})

test('safe legacy numbers and negative local IDs remain selectable; unsafe numbers fail explicitly', () => {
  const h = setup([42, -1])
  h.handleNodeClick({ node: { id: '42' } })
  assert.equal(h.selectedNode.value.routeProcessId, 42)
  h.handleNodeClick({ node: { id: '-1' } })
  assert.equal(h.selectedNode.value.routeProcessId, -1)
  assert.equal(h.readRouteFlowLastSelection().routeProcessId, '-1')
  assert.equal(identity.exports.requireRouteProcessId(-2), -2)
  assert.throws(() => h.selectRouteProcessNode(Number(A)), /完整整数精度/)
  h.cache.set('selection', { routeProcessId: Number(A) })
  assert.throws(() => h.readRouteFlowLastSelection(), /完整整数精度/)
})
