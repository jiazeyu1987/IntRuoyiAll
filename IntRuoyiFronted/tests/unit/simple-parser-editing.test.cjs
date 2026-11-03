const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const read = file => fs.readFileSync(`src/views/form-center/${file}`, 'utf8')
function load(source, imports = {}, globals = {}) {
  const context = { exports: {}, ...globals, require: name => {
    if (Object.hasOwn(imports, name)) return imports[name]
    throw new Error(`Unexpected import: ${name}`)
  } }
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
const editing = load(read('parser/useParserEditing.ts'), { vue })
const pqc = load(read('parser/simplePqc.ts'))
const commonValidation = load(read('simple-common-parser/draft.ts'))
const production = load(read('parser/simpleRecognition.ts'), { pizzip: require('pizzip') })
const fixture = () => pqc.makeDraft({ regulationName: '检验规程', regulationCode: 'PQC-1', versionNo: 'A/1',
  processes: [1, 2].map(n => ({ processCode: `P${n}`, processName: `工序${n}`, sort: n,
    items: [{ itemCode: `I${n}`, itemSort: 1, itemName: '外观', standardText: '无异物',
      inspectionMethod: '观察', inspectionTool: '目测', samplingPlanText: '首件1件',
      resultType: 'BOOLEAN', applicableInspectionTypes: ['FIRST'], firstInspectionQuantity: 1 }] }))
}, 42)

function page(kind) {
  const saved = new Map(), confirms = [], errors = []
  const globals = {
    crypto: require('node:crypto').webcrypto,
    window: { addEventListener() {}, removeEventListener() {} },
    useMessage: () => ({ confirm: async text => { confirms.push(text) }, success() {}, error: text => errors.push(text) }),
    localStorage: { setItem: (key, value) => saved.set(key, value), getItem: key => saved.get(key) },
    document: { querySelector() {} }
  }
  const imports = {
    vue, 'vue-router': { useRouter: () => ({ push() {} }), onBeforeRouteLeave() {} },
    'element-plus': { ElMessageBox: { confirm: async text => { confirms.push(text) } } }, vuedraggable: {},
    '../parser/useParserEditing': editing, './useParserEditing': editing,
    '../parser/simplePqc': pqc, './simpleRecognition': production, './draft': commonValidation,
    '@/api/mes/pro/batchrecordreport': {}, '@/api/mes/pro/process': {}, '@/api/mes/pro/craft': {}, '@/api/mes/md/item': {},
    '@/api/mes/pro/batchrecordreport/simpleParser': { SimpleParserApi: { devices: async () => [] } },
    '@/api/mes/pro/route': { getRouteProjectOptions: async () => [] },
    '@/api/mes/qc/template': { QcTemplateApi: { getCurrentQaRegulation: async () => null, listQaRegulationVersions: async () => [] } },
    '@/api/mes/qc/template/simpleCommonParser': {},
    '@/api/mes/dv/machinery': { DvMachineryApi: { getMachineryPage: async () => ({ list: [], total: 0 }) } },
    '@/api/dcc/controlledFile/projectCodes': { getProjectCodePage: async () => ({ list: [] }),
      getProjectCode: async id => ({ id, projectCode: 'P42', projectName: '项目', status: 'ENABLE' }) },
    '@/store/modules/user': { useUserStore: () => ({ getUser: { id: 1 } }) },
    '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => undefined },
    '../parser/simplePqcWord': {}, './simpleWord': {}, '@/assets/templates/simple-parser-export.docx?url': {}
  }
  for (const prefix of ['../parser/', './']) for (const name of ['SimpleParserHeader', 'SimpleParameterDialog',
    'SimpleTrialPreview', 'SimplePqcTrialPreview', 'ParserProcessToolbar', 'ParserTrialNavigation', 'ParserIssueList', 'ParserRegulationResult'])
    imports[`${prefix}components/${name}.vue`] = {}
  const filename = kind === 'production' ? 'parser/SimpleParserPage.vue' : `simple-${kind}-parser/index.vue`
  const descriptor = parse(read(filename), { filename }).descriptor
  const script = compileScript(descriptor, { id: kind })
  const component = load(script.content, imports, globals).default
  return { component, descriptor, script, imports, globals, saved, confirms, errors }
}
function initialize(state, kind) {
  state.projectId.value = 42
  state.projects.value = [{ id: 42, projectCode: 'P42', projectName: '项目' }]
  if (kind === 'production') {
    state.recognition.value = { schemaVersion: 3, product: {}, processes: [] }
    state.routeName.value = '工艺流程'
    state.processes.value = [1, 2].map(n => ({ id: `P${n}`, value: { name: `工序${n}`, equipmentGroups: [] } }))
    state.selectedId.value = 'P1'
  } else {
    const draft = fixture()
    if (kind === 'common') draft.ownerModule = 'MES_QA_COMMON'
    state.draft.value = draft
  }
  state.resetEditing(true, '2026-11-02 10:00:00')
}

for (const kind of ['production', 'pqc', 'common']) {
  test(`${kind}: navigation stays clean; edits, reorder and deletion can be undone through an empty list`, async t => {
    const ctx = page(kind)
    const state = mountState(ctx, t)
    initialize(state, kind)
    await vue.nextTick()
    const list = () => kind === 'production' ? state.processes.value : state.draft.value.processes
    const select = n => state.selectProcess(kind === 'production' ? `P${n + 1}` : n)
    select(1)
    await vue.nextTick()
    assert.equal(state.dirty.value, false)
    assert.equal(state.history.value.length, 0)
    state.moveProcess(-1)
    await vue.nextTick()
    assert.equal(kind === 'production' ? list()[0].id : list()[0].processCode, 'P2')
    assert.equal(state.dirty.value, true)
    state.undo()
    await vue.nextTick()
    assert.equal(kind === 'production' ? list()[1].id : list()[1].processCode, 'P2')
    assert.equal(state.dirty.value, false)
    state.reorderProcesses([...list()].reverse())
    await vue.nextTick()
    assert.equal(state.history.value.length, 1)
    state.undo()
    await vue.nextTick()
    assert.equal(state.dirty.value, false)
    if (kind === 'production') state.routeName.value = '修改流程'
    else state.item.value.standardText = '修改标准'
    await vue.nextTick()
    state.undo()
    await vue.nextTick()
    assert.equal(state.dirty.value, false)
    await state.removeProcess()
    await vue.nextTick()
    await state.removeProcess()
    await vue.nextTick()
    assert.equal(list().length, 0)
    state.undo()
    await vue.nextTick()
    assert.equal(list().length, 1)
    state.undo()
    await vue.nextTick()
    assert.equal(list().length, 2)
    assert.equal(state.dirty.value, false)
    assert.equal(state.savedAt.value, '2026-11-02 10:00:00')
  })
  test(`${kind}: restoring a saved draft asks only when edits would be lost`, async t => {
    const ctx = page(kind), state = mountState(ctx, t)
    initialize(state, kind)
    await vue.nextTick()
    const save = kind === 'production' ? state.saveDraft : state.saveLocal
    const restore = kind === 'production' ? state.restoreDraft : state.restoreLocal
    save()
    assert.equal(ctx.errors.length, 0)
    await restore()
    await vue.nextTick()
    assert.equal(ctx.confirms.length, 0)
    if (kind === 'production') state.routeName.value += '修改'
    else state.draft.value.regulationName += '修改'
    await vue.nextTick()
    await restore()
    await vue.nextTick()
    assert.equal(ctx.confirms.length, 1)
    assert.equal(ctx.errors.length, 0)
    assert.equal(state.error.value, '')
    assert.equal(state.dirty.value, false)
    assert.equal(state.history.value.length, 0)
  })
}

test('inspection issues point to exact missing metadata, item field and empty process actions', () => {
  for (const validate of [pqc.validateDraft, commonValidation.validateCommonDraft]) {
    const draft = fixture()
    draft.regulationCode = ''
    draft.processes[1].items[0].inspectionMethod = ''
    const issues = validate(draft)
    assert.equal(issues.find(x => x.target === 'regulationCode').field, 'metadata')
    const item = issues.find(x => x.target === 'inspectionMethod')
    assert.equal(item.processIndex, 1)
    assert.equal(item.itemIndex, 0)
    draft.processes[1].items = []
    assert.ok(validate(draft).some(x => x.target === 'addItem' && x.processIndex === 1))
    draft.processes = []
    assert.ok(validate(draft).some(x => x.target === 'addProcess'))
  }
})

test('production issues retain material, equipment and parameter field coordinates', () => {
  const issues = production.reviewIssues([{ value: { name: '工序', inputs: [{ name: '' }], equipmentGroups: [{
    selectionMode: 'SINGLE', equipmentOptions: [{ code: '', name: '' }],
    parameters: [{ name: '压力', referenceValue: '1-3', ui: { control: 'number', min: 1, max: 3, step: 1, defaultValue: 5 } }]
  }] } }])
  assert.ok(issues.some(x => x.target === 'inputs-0'))
  assert.ok(issues.some(x => x.target === 'equipment-0-0'))
  const parameter = issues.find(x => x.target === 'defaultValue')
  assert.equal(parameter.process, 0)
  assert.equal(parameter.group, 0)
  assert.equal(parameter.parameter, 0)
})

function renderer() {
  const remove = node => { const i = node.parent?.children.indexOf(node); if (i >= 0) node.parent.children.splice(i, 1) }
  return vue.createRenderer({
    createElement: type => ({ type, children: [], style: {} }), createText: text => ({ text }), createComment: () => ({}),
    patchProp() {}, setText() {}, setElementText() {}, parentNode: node => node.parent,
    nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1], remove,
    insert: (node, parent, anchor) => { remove(node); node.parent = parent; const i = parent.children.indexOf(anchor); if (i < 0) parent.children.push(node); else parent.children.splice(i, 0, node) }
  })
}

for (const kind of ['production', 'pqc', 'common']) test(`${kind}: actual trial template preserves entries across process/mode switches without editing the draft`, async () => {
  const ctx = page(kind), trialStates = []
  const trialFile = `parser/components/${kind === 'production' ? 'SimpleTrialPreview' : 'SimplePqcTrialPreview'}.vue`
  const trialDescriptor = parse(read(trialFile), { filename: trialFile }).descriptor
  const trial = load(compileScript(trialDescriptor, { id: `${kind}-trial` }).content, {
    vue, '../simpleRecognition': production,
    '@/store/modules/user': { useUserStore: () => ({ getUser: {} }) }
  }).default
  const setupTrial = trial.setup
  trial.setup = (props, context) => { const state = setupTrial(props, context); trialStates.push(state); return state }
  trial.render = () => vue.h('span')
  ctx.imports[kind === 'production' ? './components/SimpleTrialPreview.vue' : '../parser/components/SimplePqcTrialPreview.vue'] = { default: trial }
  const component = load(ctx.script.content, ctx.imports, ctx.globals).default
  const trialSource = kind === 'production'
    ? ctx.descriptor.template.content.match(/<div v-show="mode === 'trial'">[\s\S]*?<\/div>/)[0]
    : ctx.descriptor.template.content.match(/<SimplePqcTrialPreview\b[\s\S]*?\/>/)[0]
  const compiled = compileTemplate({ source: trialSource, filename: kind, id: kind, compilerOptions: { bindingMetadata: ctx.script.bindings } })
  assert.deepEqual(compiled.errors, [])
  component.render = load(compiled.code, { vue }).render
  let state
  const setupPage = component.setup
  component.setup = (props, context) => { state = setupPage(props, context); initialize(state, kind); return state }
  const app = renderer().createApp(component)
  app.directive('loading', {})
  app.mount({ children: [] })
  try {
    await vue.nextTick()
    const before = JSON.stringify(kind === 'production' ? state.processes.value : state.draft.value)
    if (kind === 'production') { state.mode.value = 'trial'; trialStates[0].quantity.value = 7 }
    else { state.viewMode.value = 'trial'; trialStates[0].bulk('合格') }
    state.selectProcess(kind === 'production' ? 'P2' : 1)
    await vue.nextTick()
    if (kind === 'production') trialStates[1].quantity.value = 9
    else trialStates[0].bulk('不合格')
    state.selectProcess(kind === 'production' ? 'P1' : 0)
    await vue.nextTick()
    const mode = kind === 'production' ? state.mode : state.viewMode
    mode.value = 'edit'; await vue.nextTick(); mode.value = 'trial'; await vue.nextTick()
    if (kind === 'production') {
      assert.equal(trialStates.length, 2)
      assert.equal(trialStates[0].quantity.value, 7)
      assert.equal(trialStates[1].quantity.value, 9)
    } else {
      assert.equal(trialStates.length, 1)
      assert.match(trialStates[0].resultSummary.value, /合格 1 件/)
    }
    assert.equal(JSON.stringify(kind === 'production' ? state.processes.value : state.draft.value), before)
    assert.equal(state.dirty.value, false)
  } finally { app.unmount() }
})

function mountState(ctx, t) {
  let state
  const original = ctx.component.setup
  ctx.component.setup = (props, context) => { state = original(props, context); return state }
  ctx.component.render = () => vue.h('div')
  const app = renderer().createApp(ctx.component)
  app.mount({ children: [] })
  t.after(() => app.unmount())
  return state
}

for (const kind of ['pqc', 'common']) test(`${kind}: reimporting a published Word creates an unused version without losing inspection content`, async t => {
  const ctx = page(kind)
  const snapshot = fixture()
  if (kind === 'common') snapshot.ownerModule = 'MES_QA_COMMON'
  snapshot.expectedDraftRevision = 7
  const content = JSON.stringify(snapshot.processes)
  ctx.imports['../parser/simplePqcWord'].readPqcWord = () => ({ draft: snapshot, confirmedUnbound: [] })
  const api = ctx.imports['@/api/mes/qc/template'].QcTemplateApi
  api.listQaRegulationVersions = async () => [{ versionNo: 'A/1' }, { versionNo: 'A/2' }]
  api.listCommonRegulationPublishedVersions = async () => [
    { commonDccProjectCodeId: 42, versionNo: 'A/1' },
    { commonDccProjectCodeId: 42, versionNo: 'A/2' },
    { commonDccProjectCodeId: 99, versionNo: 'A/3' }
  ]
  const state = mountState(ctx, t)
  state.projectId.value = 42
  await state.importWord({ raw: { arrayBuffer: async () => new ArrayBuffer(0) } })
  assert.equal(ctx.errors.length, 0)
  assert.equal(state.draft.value.versionNo, 'A/3')
  assert.equal(state.draft.value.expectedDraftRevision, 0)
  assert.equal(state.draft.value.dccProjectCodeId, 42)
  assert.equal(JSON.stringify(state.draft.value.processes), content)
  assert.equal(state.published.value, false)
  assert.equal(state.dirty.value, true)
})

for (const kind of ['production', 'pqc', 'common']) test(`${kind}: publication locks repeated submissions and closes only on success`, async t => {
  const ctx = page(kind)
  let calls = 0, complete
  const response = new Promise(resolve => { complete = resolve })
  const publish = () => { calls++; return response }
  if (kind === 'production') ctx.imports['@/api/mes/pro/batchrecordreport/simpleParser'].SimpleParserApi.publish = publish
  else if (kind === 'pqc') ctx.imports['@/api/mes/qc/template'].QcTemplateApi.publishSimplePqc = publish
  else ctx.imports['@/api/mes/qc/template/simpleCommonParser'].SimpleCommonParserApi = { publish }
  const state = mountState(ctx, t)
  initialize(state, kind)
  if (kind !== 'production') state.draft.value.finalInspectionNotApplicableReason = '本规程无需末检'
  await vue.nextTick()
  const visible = kind === 'production' ? state.publishVisible : state.previewVisible
  visible.value = true
  if (kind === 'production') {
    state.publishPreview.value = { blockers: [] }
    state.pendingRequest.value = { requestId: 'R1', previewToken: 'T1' }
  }
  const first = state.publish()
  assert.equal(state.busy.value, true)
  assert.match(state.busyText.value, /正在发布/)
  await state.publish()
  assert.equal(calls, 1)
  assert.equal(visible.value, true)
  const result = { versionNo: 'A/1', routeId: 9, publishedVersionId: 99,
    regulationCode: 'SERVER-RESULT', dccProjectCodeId: 42, processes: [] }
  complete(result)
  await first
  assert.equal(state.busy.value, false)
  assert.equal(visible.value, false)
  assert.ok(state.published.value)
  assert.equal(ctx.errors.length, 0)
  assert.equal(state.error.value, '')
  if (kind === 'common') {
    assert.equal(state.publishedResult.value.publishedVersionId, 99)
    assert.equal(state.publishedResult.value.regulationCode, 'SERVER-RESULT')
    state.viewResult()
    assert.equal(state.resultVisible.value, true)
    const saved = JSON.parse([...ctx.saved.values()][0])
    assert.equal(saved.publishedResult.publishedVersionId, 99)
    assert.equal(saved.publishedResult.regulationCode, 'SERVER-RESULT')
  }
})
