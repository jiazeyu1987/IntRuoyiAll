const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const vue = require('vue')
const root = path.resolve(__dirname, '../..')
const source = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('designer.ts', source, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const declarations = new Map()
for (const statement of ast.statements) {
  if (ts.isFunctionDeclaration(statement) && statement.body) {
    declarations.set(statement.name.text, statement.getText(ast))
  }
  if (!ts.isVariableStatement(statement)) continue
  for (const declaration of statement.declarationList.declarations) {
    if (ts.isIdentifier(declaration.name)) declarations.set(declaration.name.text, `const ${declaration.getText(ast)};`)
  }
}
const compile = code => ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const identity = { exports: {} }
vm.runInNewContext(compile(fs.readFileSync(path.join(root, 'src/views/mes/pro/route/routeProcessIdentity.ts'), 'utf8')), identity)
const A = '2116486795629948963', B = '2116486795629948964'
const evaluateDeclarations = (names, context) => {
  vm.createContext(context)
  vm.runInContext(compile(names.map(name => declarations.get(name)).join('\n') + `\nthis.result = { ${names.join(', ')} };`), context)
  return context.result
}

const createProductionConfigHarness = () => {
  const existing = { routeProcessId: A, processId: B, overagePercent: 2,
    lossReasons: [{ code: 'LOSS' }], deviceSelectionGroups: [{ deviceIds: [B] }],
    parameterRules: [{ parameterCode: 'KEEP', deviceId: B, lowerLimit: 3 }] }
  const context = { ...identity.exports,
    selectedProcessRouteConfigCache: { value: { productionProcessConfigSnapshot: {
      routeVersionId: A, routeSnapshotSha256: 'original-hash', schemaVersion: 1,
      productionProcessConfigs: [existing]
    } } },
    routeNodes: { value: [{ routeProcessId: A, processId: B, processName: 'existing' }] },
    productionProcessConfigJsonDrafts: {}, productionProcessConfigJsonBaselines: {},
    isActiveRouteNode: () => true, resolveRouteVersionIdForSave: () => A,
    resolveErrorMessage: error => error.message
  }
  const harness = evaluateDeclarations(['findProductionProcessConfig', 'normalizeProductionProcessConfig',
    'buildDefaultProductionProcessConfig', 'ensureProductionProcessConfigJsonDraft',
    'parseProductionProcessConfigDraft', 'hasRouteProcessStructureChanges', 'buildFlowProductionProcessConfig'], context)
  return { context, harness, existing }
}

const createOverageEditor = (modelValue, disabled = false) => {
  const filename = path.join(root, 'src/views/mes/pro/route/components/RouteOverageEditor.vue')
  const descriptor = parse(fs.readFileSync(filename, 'utf8'), { filename }).descriptor
  const script = compileScript(descriptor, { id: 'overage-test' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'overage-test',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const module = { exports: {} }
  vm.runInNewContext(compile(script.content), { ...vue, module, exports: module.exports,
    require: () => vue, Error })
  const props = vue.reactive({ modelValue, disabled })
  const emitted = []
  const state = vue.proxyRefs(module.exports.default.setup(props, {
    expose() {}, emit: (...args) => emitted.push(args)
  }))
  return { state, props, emitted }
}

test('atomic flow production payload preserves exact identities, hash and untouched configuration', () => {
  const { harness, existing } = createProductionConfigHarness()
  const payload = harness.buildFlowProductionProcessConfig()
  assert.equal(payload.routeVersionId, A)
  assert.equal(payload.expectedRouteSnapshotSha256, 'original-hash')
  assert.equal(payload.schemaVersion, 1)
  assert.equal(JSON.stringify(payload.productionProcessConfigs), JSON.stringify([existing]))
  assert.equal(harness.hasRouteProcessStructureChanges(), false)
})

test('new process visual overage field supplies first atomic save without editing JSON', () => {
  const { context, harness, existing } = createProductionConfigHarness()
  context.routeNodes.value.push({ routeProcessId: -1, processId: B, processName: 'new' })
  assert.equal(harness.hasRouteProcessStructureChanges(), true)
  assert.throws(() => harness.buildFlowProductionProcessConfig(), /new.*允许超量比例/)
  const draft = JSON.parse(context.productionProcessConfigJsonDrafts['-1'])
  assert.equal(draft.overagePercent, null)
  const editor = createOverageEditor(context.productionProcessConfigJsonDrafts['-1'])
  editor.state.updateOverage(0)
  assert.equal(editor.emitted[0][0], 'update:modelValue')
  context.productionProcessConfigJsonDrafts['-1'] = editor.emitted[0][1]
  const configs = harness.buildFlowProductionProcessConfig().productionProcessConfigs
  assert.equal(configs.length, 2)
  assert.equal(JSON.stringify(configs[0]), JSON.stringify(existing))
  assert.equal(configs[1].routeProcessId, -1)
  assert.equal(configs[1].processId, B)
  assert.equal(configs[1].overagePercent, 0)
  assert.equal(configs[1].deviceSelectionGroups.length, 0)
})

test('visual overage edit preserves equipment, parameters and unknown production configuration fields', () => {
  const original = { routeProcessId: A, processId: B, overagePercent: 2,
    deviceSelectionGroups: [{ deviceGroupKey: 'A', deviceIds: [B] }],
    parameterRules: [{ parameterCode: 'KEEP' }], lossReasons: [{ id: 7 }], customField: 'keep' }
  const editor = createOverageEditor(JSON.stringify(original))
  editor.state.updateOverage(12.5)
  assert.deepEqual(JSON.parse(editor.emitted[0][1]), { ...original, overagePercent: 12.5 })
  editor.state.updateOverage(undefined)
  assert.equal(JSON.parse(editor.emitted[1][1]).overagePercent, null)
})

test('visual overage editor reports malformed production configuration and blocks read-only changes', () => {
  for (const json of ['{', 'null', '[]']) {
    const editor = createOverageEditor(json)
    assert.match(editor.state.parsed.error, /生产配置格式无效/)
    editor.state.updateOverage(0)
    assert.equal(editor.emitted.length, 0)
  }
  const editor = createOverageEditor('{"overagePercent":2}', true)
  editor.state.updateOverage(0)
  assert.equal(editor.emitted.length, 0)
})

test('deleted processes are removed from the atomic configuration without moving another process values', () => {
  const { context, harness } = createProductionConfigHarness()
  context.routeNodes.value = [{ routeProcessId: -1, processId: B, processName: 'new' }]
  context.productionProcessConfigJsonDrafts['-1'] = JSON.stringify({ routeProcessId: -1, processId: B,
    overagePercent: 5, lossReasons: [], deviceSelectionGroups: [], parameterRules: [] })
  assert.equal(harness.hasRouteProcessStructureChanges(), true)
  const configs = harness.buildFlowProductionProcessConfig().productionProcessConfigs
  assert.equal(configs.length, 1)
  assert.equal(configs[0].routeProcessId, -1)
  assert.equal(configs[0].overagePercent, 5)
})

test('atomic configuration refuses missing or wrong candidate identity and hash', () => {
  const { context, harness } = createProductionConfigHarness()
  const snapshot = context.selectedProcessRouteConfigCache.value.productionProcessConfigSnapshot
  snapshot.routeVersionId = B
  assert.throws(() => harness.buildFlowProductionProcessConfig(), /快照标识尚未加载/)
  snapshot.routeVersionId = A
  snapshot.routeSnapshotSha256 = ''
  assert.throws(() => harness.buildFlowProductionProcessConfig(), /快照标识尚未加载/)
})

test('production configuration parser failures and structural edits are persistently reported before writes', async () => {
  for (const structural of [false, true]) {
    const context = { operationError: { value: '' }, productionConfigSavedMessage: { value: '' },
      selectedNode: { value: { routeProcessId: A } }, selectedProcessRouteConfigCache: { value: {} },
      hasRouteProcessStructureChanges: () => structural, requireCandidateRouteVersionId: () => A,
      parseProductionProcessConfigDraft: () => { throw new Error('invalid production JSON') },
      resolveErrorMessage: error => error.message, message: { error() {} },
      ProRouteApi: { saveRouteProductionProcessConfig() { assert.fail('must not write') } }
    }
    const harness = evaluateDeclarations(['saveSelectedProductionProcessConfig'], context)
    await harness.saveSelectedProductionProcessConfig()
    assert.match(context.operationError.value, structural ? /顶部保存/ : /invalid production JSON/)
    assert.equal(context.productionConfigSavedMessage.value, '')
  }
})

test('route edit page preserves the exact route-process query before passing it to the designer', () => {
  const filename = path.join(root, 'src/views/mes/pro/route/RouteEditPage.vue')
  const content = parse(fs.readFileSync(filename, 'utf8')).descriptor.scriptSetup.content
  const pageAst = ts.createSourceFile('page.ts', content, ts.ScriptTarget.Latest, true)
  const statement = pageAst.statements.find(statement => ts.isVariableStatement(statement) && statement.declarationList.declarations.some(declaration => declaration.name.getText(pageAst) === 'targetRouteProcessId'))
  const context = { route: { query: { routeProcessId: '2121453180886011905' } }, computed: fn => ({ get value() { return fn() } }) }
  vm.createContext(context)
  vm.runInContext(compile(fs.readFileSync(path.join(root, 'src/utils/routeQueryId.ts'), 'utf8')).replace(/exports\./g, 'this.'), Object.assign(context, { exports: {} }))
  vm.runInContext(compile(statement.getText(pageAst)) + '\nthis.target = targetRouteProcessId;', context)
  assert.equal(context.target.value, '2121453180886011905')
})

const createAddProcessHarness = loadConfig => {
  const context = {
    ...identity.exports,
    canMutateRouteFlow: { value: true }, routeProcessFormRef: { value: { validate: async () => true } },
    routeProcessForm: { processId: 15 }, processOptions: { value: [{ id: 15, name: 'test-process' }] },
    routeNodes: { value: [{ routeProcessId: A, sort: 1 }] }, nextDraftRouteProcessId: { value: -1 },
    ensureSelectedProcessRouteConfigCache: loadConfig, defaultNodePosition: () => ({ x: 0, y: 0 }),
    inheritGlobalRecordBindingsForRouteProcess: () => context.events.push('inherit'),
    routeProcessDialogVisible: { value: true }, selectedRouteProcessId: { value: A }, selectedEdgeKey: { value: '' },
    markGraphDraftChanged: () => context.events.push('dirty'), syncFlowElements: () => context.events.push('sync'),
    message: { error: text => context.events.push(text), warning: () => {} }, resolveErrorMessage: error => error.message,
    events: []
  }
  return { context, ...evaluateDeclarations(['handleRouteProcessAdd'], context) }
}

test('adding a selected process preserves colliding long IDs, codes and names in either option order', async () => {
  assert.equal(Number(A), Number(B))
  for (const selected of [A, B]) {
    const harness = createAddProcessHarness(async () => ({}))
    harness.context.processOptions.value = [
      { id: A, code: 'IDML-GX-0001', name: '粗洗工序' },
      { id: B, code: 'IDML-GX-0015', name: '大包装工序' }
    ]
    harness.context.routeProcessForm.processId = selected
    await harness.handleRouteProcessAdd()
    const expected = harness.context.processOptions.value.find(option => option.id === selected)
    const actual = harness.context.routeNodes.value.at(-1)
    assert.equal(actual.processId, expected.id)
    assert.equal(actual.processCode, expected.code)
    assert.equal(actual.processName, expected.name)
  }
})

test('unsafe numeric selected IDs fail without adding or consuming a draft ID', async () => {
  const harness = createAddProcessHarness(async () => ({}))
  harness.context.routeProcessForm.processId = Number(A)
  await harness.handleRouteProcessAdd()
  assert.equal(harness.context.routeNodes.value.length, 1)
  assert.equal(harness.context.nextDraftRouteProcessId.value, -1)
  assert.match(harness.context.events[0], /完整整数精度/)
})

test('available process options exclude only the exact used identity', () => {
  const context = { ...identity.exports, computed: getter => ({ get value() { return getter() } }),
    routeNodes: { value: [{ processId: A }] },
    processOptions: { value: [{ id: A }, { id: B }] } }
  const harness = evaluateDeclarations(['availableProcessOptions'], context)
  assert.equal(harness.availableProcessOptions.value.length, 1)
  assert.equal(harness.availableProcessOptions.value[0].id, B)
})

test('adding a local process waits for route configuration before modifying the graph', async () => {
  let finishLoading
  const pending = new Promise(resolve => { finishLoading = resolve })
  const harness = createAddProcessHarness(() => pending)
  const request = harness.handleRouteProcessAdd()
  await Promise.resolve()
  assert.equal(harness.context.routeNodes.value.length, 1)
  assert.equal(harness.context.nextDraftRouteProcessId.value, -1)
  finishLoading({})
  await request
  assert.equal(harness.context.routeNodes.value.length, 2)
  assert.equal(harness.context.selectedRouteProcessId.value, -1)
  assert.deepEqual(harness.context.events, ['inherit', 'dirty', 'sync'])
})

test('failed route configuration loading leaves the graph and draft sequence unchanged', async () => {
  const harness = createAddProcessHarness(async () => { throw new Error('test configuration failure') })
  await harness.handleRouteProcessAdd()
  assert.equal(harness.context.routeNodes.value.length, 1)
  assert.equal(harness.context.nextDraftRouteProcessId.value, -1)
  assert.equal(harness.context.routeProcessDialogVisible.value, true)
  assert.deepEqual(harness.context.events, ['test configuration failure'])
})

test('saved local process mappings retain exact snowflake IDs and reject rounded numbers', () => {
  const context = { ...identity.exports }
  const harness = evaluateDeclarations(['isDraftRouteProcessId', 'normalizePersistedRouteProcessIdMap'], context)
  const mapping = harness.normalizePersistedRouteProcessIdMap({ '-1': A, '-2': B })
  assert.equal(mapping.get('-1'), A)
  assert.equal(mapping.get('-2'), B)
  assert.throws(() => harness.normalizePersistedRouteProcessIdMap({ '-1': Number(A) }), /完整整数精度/)
})

test('saved local IDs remap selection, edges and attribute drafts without changing adjacent IDs', () => {
  const context = { ...identity.exports, isBoundaryNodeId: id => id === 'process-start' || id === 'process-end',
    selectedProcessAttributeDrafts: { '-1': { routeProcessId: -1, marker: 'new' }, [B]: { routeProcessId: B, marker: 'existing' } },
    selectedProcessAttributeBaselines: { '-1': 'old', [B]: 'existing' }
  }
  const harness = evaluateDeclarations(['remapPersistedRouteProcessId', 'remapRouteProcessEdgeKey', 'remapSelectedProcessAttributeDrafts'], context)
  const mapping = new Map([['-1', A]])
  assert.equal(harness.remapPersistedRouteProcessId(-1, mapping), A)
  assert.equal(harness.remapPersistedRouteProcessId(B, mapping), B)
  assert.equal(harness.remapRouteProcessEdgeKey(`-1->${B}`, mapping), `${A}->${B}`)
  assert.equal(harness.remapRouteProcessEdgeKey('process-start->-1', mapping), `process-start->${A}`)
  harness.remapSelectedProcessAttributeDrafts(mapping)
  assert.equal(context.selectedProcessAttributeDrafts[A].routeProcessId, A)
  assert.equal(context.selectedProcessAttributeDrafts[B].marker, 'existing')
  assert.equal(context.selectedProcessAttributeDrafts['-1'], undefined)
})

test('loaded process identity and deletion membership keep adjacent long IDs separate', () => {
  const context = { ...identity.exports, routeNodes: { value: [{ routeProcessId: A }, { routeProcessId: B }] },
    loadedRouteProcessIds: { value: new Set() }, pendingDeletedRouteProcessIds: { value: new Set([A]) }
  }
  const harness = evaluateDeclarations(['resetLoadedRouteProcessIds', 'isLoadedRouteProcessId', 'isActiveRouteProcessId'], context)
  harness.resetLoadedRouteProcessIds()
  assert.equal(harness.isLoadedRouteProcessId(A), true)
  assert.equal(harness.isLoadedRouteProcessId(B), true)
  assert.equal(harness.isLoadedRouteProcessId(-1), false)
  assert.equal(harness.isActiveRouteProcessId(A), false)
  assert.equal(harness.isActiveRouteProcessId(B), true)
})

test('relation selections and canvas deletion retain exact existing and temporary IDs', () => {
  const removed = []
  const context = { ...identity.exports, canMutateRouteFlow: { value: true },
    isBoundaryNodeId: id => id === 'process-start', removeRouteProcessesFromDraft: ids => removed.push(...ids)
  }
  const harness = evaluateDeclarations(['normalizeRouteProcessIdList', 'handleNodesChange'], context)
  assert.deepEqual(Array.from(harness.normalizeRouteProcessIdList([A, B, -1, '-1'])), [A, B, '-1'])
  harness.handleNodesChange([{ type: 'remove', id: A }, { type: 'remove', id: '-1' }, { type: 'remove', id: 'process-start' }])
  assert.deepEqual(removed, [A, '-1'])
})
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
