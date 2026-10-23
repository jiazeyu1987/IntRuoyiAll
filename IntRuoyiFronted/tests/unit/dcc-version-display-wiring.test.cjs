const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const sourceRoot = path.join(root, 'src/views/dcc/controlled-file')
const FILE = '9223372036854775701', SECOND = '9223372036854775702'
const read = file => fs.readFileSync(path.join(sourceRoot, file), 'utf8')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
const modules = new Map()
function production(filename) {
  filename = path.resolve(filename)
  if (modules.has(filename)) return modules.get(filename)
  const exports = {}
  new Function('exports', 'require', compile(fs.readFileSync(filename, 'utf8')))(exports, name => {
    if (name === 'vue' || name === 'dayjs') return require(name)
    if (name === '@/utils/formatTime') return { formatDateTimeValue: value => value == null ? '-' : String(value) }
    if (name.startsWith('@/')) return production(path.join(root, 'src', name.slice(2) + '.ts'))
    if (name.startsWith('.')) return production(path.resolve(path.dirname(filename), name + '.ts'))
    throw new Error('Unexpected production import ' + name)
  })
  modules.set(filename, exports)
  return exports
}
const display = production(path.join(sourceRoot, 'shared/version-display.ts'))
const noopChild = { setup: () => () => vue.h('aside') }
function component(file, dependencies = {}) {
  const { descriptor } = parse(read(file), { filename: file })
  const compiled = compileScript(descriptor, { id: file, inlineTemplate: true })
  const exports = {}
  new Function('exports', 'require', compile(compiled.content))(exports, name => {
    if (name in dependencies) return dependencies[name]
    if (name === 'vue') return vue
    if (name.endsWith('.vue')) return { __esModule: true, default: noopChild }
    return production(name.startsWith('@/') ? path.join(root, 'src', name.slice(2) + '.ts') : path.resolve(sourceRoot, path.dirname(file), name + '.ts'))
  })
  return exports.default
}
const node = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({
  createElement: type => node(type), createText: text => node('#text', text), createComment: text => node('#comment', text),
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  patchProp: (n, key, _old, value) => { n.props[key] = value },
  insert: (n, parent, anchor) => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = parent; const i = parent.children.indexOf(anchor); i < 0 ? parent.children.push(n) : parent.children.splice(i, 0, n) },
  remove: n => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
const text = n => (n.type === '#comment' ? '' : n.text) + n.children.map(text).join('')
const findAll = (n, type) => [...(n.type === type ? [n] : []), ...n.children.flatMap(child => findAll(child, type))]
function mount(c, props) {
  const target = node('root'), errors = []
  const app = renderer.createApp(c, props)
  app.config.warnHandler = () => {}
  app.config.errorHandler = error => { errors.push(error) }
  app.directive('loading', {})
  for (const tag of ['ContentWrap', 'el-dialog', 'el-select', 'el-option', 'el-radio-group', 'el-radio', 'el-input', 'el-button', 'el-alert', 'el-form', 'el-form-item', 'el-descriptions', 'el-descriptions-item', 'el-tag', 'el-collapse', 'el-collapse-item', 'el-checkbox', 'el-date-picker', 'el-empty']) {
    app.component(tag, { inheritAttrs: false, setup: (_, context) => () => vue.h(tag, context.attrs, [context.slots.default?.(), context.slots.footer?.()]) })
  }
  app.component('el-table', { props: ['data'], setup: (p, { slots }) => { vue.provide('rows', vue.toRef(p, 'data')); return () => vue.h('el-table', {}, slots.default?.()) } })
  app.component('el-table-column', { inheritAttrs: false, setup: (_, context) => {
    const rows = vue.inject('rows')
    return () => vue.h('el-table-column', context.attrs, (rows.value || []).map(row => context.slots.default ? context.slots.default({ row }) : String(context.attrs.prop?.split('.').reduce((value, key) => value?.[key], row) ?? '')))
  } })
  app.mount(target)
  return { root: target, errors, unmount: () => app.unmount() }
}
const flush = async () => { for (let i = 0; i < 10; i++) { await Promise.resolve(); await vue.nextTick() } }
const noRawDraft = mounted => { assert.deepEqual(mounted.errors, []); assert.match(text(mounted.root), /草稿/); assert.doesNotMatch(text(mounted.root), /A\/1-[12]/) }
const candidate = (id = FILE, versionNo = 'A/1-2') => ({ tenantId: '1', controlledFileId: id, masterId: '300', projectId: '20', projectName: '质量项目', projectFolderId: '2', folderName: '制度', fileName: '管理制度', fileNumber: 'F-1', versionNo, status: 'WORKING', controlled: false, pendingEffect: false, executable: false, canPreview: true })

test('actual browser presentation hides the selected work identity while keeping formal current execution', () => {
  const presentation = production(path.join(sourceRoot, 'browser/presentation.ts'))
  const version = Object.freeze({ versionNo: 'A/1-2', status: 'WORKING' })
  const summary = presentation.getBrowserVersionSummary(version, false, 'A/1')
  assert.equal(summary.versionText, '草稿')
  assert.match(summary.currentVersionSourceText, /A\/1/)
  assert.equal(presentation.getBrowserVersionSummary({ versionNo: 'A/1', status: 'ACTIVE' }, false, 'A/1').versionText, 'A/1')
  assert.equal(version.versionNo, 'A/1-2')
})

test('real storage version selector shows distinct recorded draft descriptions and preserves the second ID', async () => {
  const { descriptor } = parse(read('browser/index.vue'))
  const ast = ts.createSourceFile('browser.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declarations = ast.statements.filter(s => s.declarationList?.declarations.some(d => ['getBrowserVersionOptionLabel', 'getSelectedVersion', 'handleVersionChange'].includes(d.name.text)))
  const elements = []
  const walk = n => { if (n.type === 1 && n.tag === 'el-select' && n.loc.source.includes('handleVersionChange(row)')) elements.push(n); for (const child of n.children || []) walk(child) }
  walk(descriptor.template.ast)
  assert.equal(elements.length, 1)
  const row = vue.reactive({ selectedVersionId: FILE })
  const versions = [
    { id: FILE, versionNo: 'A/1-1', createTime: '2026-10-14 09:01:00', changeDescription: '修订术语' },
    { id: SECOND, versionNo: 'A/1-2', createTime: '2026-10-14 10:02:00', changeDescription: '修订职责' }
  ]
  const source = `<template>${elements[0].loc.source}</template><script setup lang="ts">const row=fixture.row;const getVersionOptions=()=>fixture.versions;const isValidBrowserOptionId=(id)=>typeof id==='string';const resolveInitialSelectedVersionId=()=>{throw Error('must not replace selection')};const buildCurrentVersionOption=()=>{throw Error('must not replace selection')};const getDccVersionDisplayText=fixture.display;const formatControlledFileDateTime=fixture.format;${declarations.map(s => s.getText(ast)).join('\n')}</script>`
  const compiled = compileScript(parse(source).descriptor, { id: 'browser-version-selector', inlineTemplate: true })
  const exports = {}
  new Function('exports', 'require', 'fixture', compile(compiled.content))(exports, () => vue, { row, versions, display: display.getDccVersionDisplayText, format: value => String(value) })
  const mounted = mount(exports.default)
  try {
    const options = findAll(mounted.root, 'el-option')
    assert.equal(options.length, 2)
    assert.deepEqual(options.map(n => n.props.value), [FILE, SECOND])
    assert.match(options[0].props.label, /草稿.*09:01:00.*修订术语/)
    assert.match(options[1].props.label, /草稿.*10:02:00.*修订职责/)
    assert.doesNotMatch(options.map(n => n.props.label).join(' '), /A\/1-[12]/)
    const select = findAll(mounted.root, 'el-select')[0]
    select.props['onUpdate:modelValue'](SECOND); select.props.onChange(SECOND); await flush()
    assert.equal(row.selectedVersionId, SECOND)
    assert.equal(versions[1].versionNo, 'A/1-2')
  } finally { mounted.unmount() }
})

test('mounted project folder table shows draft and formal versions and emits the exact original file', async () => {
  const source = candidate(), events = []
  const mounted = mount(component('browser/ProjectFolderFileTable.vue'), { files: [source, candidate(SECOND, 'A/1')], references: [], usage: {}, keyword: '', loading: false, canReference: false, onPreview: file => events.push(file) })
  try {
    await flush(); noRawDraft(mounted); assert.match(text(mounted.root), /A\/1/)
    findAll(mounted.root, 'el-button').find(n => text(n) === '浏览').props.onClick()
    assert.equal(events[0].controlledFileId, FILE); assert.equal(events[0].versionNo, 'A/1-2')
  } finally { mounted.unmount() }
})

test('mounted project relation header uses draft text while exact permission response matching remains enforced', async () => {
  const calls = [], source = candidate()
  const dependencies = { '@/api/dcc/controlledFile/applicationRead': { getControlledFileRelationPermissions: async id => { calls.push(id); return { ...source } } } }
  const mounted = mount(component('browser/ProjectFileRelationsDialog.vue', dependencies), { source, modelValue: true, contextKey: 'selected' })
  try { await flush(); noRawDraft(mounted); assert.deepEqual(calls, [FILE]); assert.equal(source.versionNo, 'A/1-2') }
  finally { mounted.unmount() }
})

test('mounted real revision panel selects the second identically named draft and submits only its exact ID', async () => {
  const commands = [], facts = { projectAttributes: { targetMarkets: ['NA'], licenseHolder: 'NA', actualManufacturer: 'NA', documentTransfer: 'N' }, selectedSignoffDepartmentIds: ['100'], selectedDistributionDepartmentIds: [], needTraining: false, effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null }
  const iterations = [FILE, SECOND].map((id, i) => ({ id, versionNo: `A/1-${i + 1}`, canPartial: true, canReplacement: true, canPreview: false }))
  const mounted = mount(component('revision/DccRevisionPanel.vue'), { baselineId: '300', baselineVersionNo: 'A/1', partialTarget: { versionNo: 'A/2', unavailableReason: null }, replacementTarget: { versionNo: 'B/1', unavailableReason: null }, idempotencyKey: 'g08-real-click', applicationFacts: facts, applicationReady: true, facts: { selectedIterationVersionNo: 'A/1-2' }, iterations, canPartial: true, canReplacement: true, submitRevision: async command => commands.push(command) })
  try {
    await flush(); noRawDraft(mounted)
    const options = findAll(mounted.root, 'el-option')
    assert.deepEqual(options.map(n => n.props.value), [FILE, SECOND])
    assert.doesNotMatch(options.map(n => n.props.label).join(' '), /A\/1-[12]/)
    assert.match(options[0].props.label, new RegExp(FILE)); assert.match(options[1].props.label, new RegExp(SECOND))
    const button = findAll(mounted.root, 'el-button').find(n => text(n) === '提交正式变更')
    await button.props.onClick(); await flush(); assert.equal(commands.length, 0)
    findAll(mounted.root, 'el-select')[0].props['onUpdate:modelValue'](SECOND)
    findAll(mounted.root, 'el-radio-group')[0].props['onUpdate:modelValue']('PARTIAL')
    findAll(mounted.root, 'el-input')[0].props['onUpdate:modelValue']('本次职责修改')
    await flush(); await button.props.onClick(); await flush()
    assert.equal(commands.length, 1, JSON.stringify(findAll(mounted.root, 'el-alert').map(n => n.props.title))); assert.equal(commands[0].selectedIterationId, SECOND)
    assert.equal(iterations[1].versionNo, 'A/1-2'); assert.match(text(mounted.root), /A\/1/)
  } finally { mounted.unmount() }
})

test('mounted historical file relations hide counters and preview the precise frozen version', async () => {
  const history = [], previews = [], file = candidate()
  const mounted = mount(component('relations/DccFileRelations.vue'), { mode: 'history', source: { contextKey: 'historical', controlledFileId: SECOND }, directories: [], canEdit: false, modelValue: [], loadHistory: async id => { history.push(id); return [file] }, loadCurrent: async () => { throw Error('history must not read current') }, openPreview: async row => previews.push(row) })
  try {
    await flush(); noRawDraft(mounted); assert.deepEqual(history, [SECOND])
    await findAll(mounted.root, 'el-button').find(n => text(n) === '查看').props.onClick()
    assert.equal(previews[0].controlledFileId, FILE); assert.equal(previews[0].versionNo, 'A/1-2')
  } finally { mounted.unmount() }
})

test('mounted fixed project reference versions show draft without changing the read reference pair', async () => {
  const row = { reference: { id: '101', masterId: '300', selectedControlledFileId: FILE }, selectedVersion: candidate(), sourceProjectName: '原项目', referenceProjectCount: 1 }
  const reads = []
  const mounted = mount(component('relations/DccProjectReferences.vue'), { contextKey: 'reference', projectId: '20', folderId: '2', folderName: '制度', isProjectLeader: false, selectorSource: {}, directories: [], loadReferences: async () => [row], openReferenceTrace: async selected => reads.push(selected) })
  try {
    await flush(); noRawDraft(mounted)
    await findAll(mounted.root, 'el-button').find(n => text(n) === '只读追溯').props.onClick()
    assert.equal(reads[0].reference.selectedControlledFileId, FILE); assert.equal(reads[0].selectedVersion.versionNo, 'A/1-2')
  } finally { mounted.unmount() }
})

test('actual arrangement table template hides working counters while leaving recorded relation identity intact', () => {
  const file = 'detail/DetailRelationArrangements.vue', { descriptor } = parse(read(file))
  const template = descriptor.template.content
  const fixture = { relations: [{ masterId: '300', fileName: '固定关联', versionNo: 'A/1-2' }], users: [], disabled: true }
  const script = `const relations=fixture.relations;const users=fixture.users;const disabled=fixture.disabled;const selected=()=>undefined;const toggle=()=>{};const getDccVersionDisplayText=fixture.display;`
  const generated = compileScript(parse(`<template>${template}</template><script setup>${script}</script>`).descriptor, { id: file, inlineTemplate: true })
  const exports = {}; new Function('exports', 'require', 'fixture', compile(generated.content))(exports, () => vue, { ...fixture, display: display.getDccVersionDisplayText })
  const mounted = mount(exports.default)
  try { noRawDraft(mounted); assert.equal(fixture.relations[0].versionNo, 'A/1-2') } finally { mounted.unmount() }
})

test('real detail lifecycle presentation hides draft identity in submission and history titles', () => {
  const presentation = production(path.join(sourceRoot, 'detail/presentation.ts'))
  const file = { id: FILE, versionNo: 'A/1-2', fileNumber: 'F1', submittedTime: 1791957600000, versionHistory: [{ id: SECOND, versionNo: 'A/1-1', status: 'OBSOLETE', obsoletedTime: 1791957601000 }] }
  const items = presentation.buildDetailLifecycleTimelineItems(file, { userNameMap: new Map(), deptNameMap: new Map() })
  assert.equal(items.length, 2)
  const visible = items.map(item => item.title + item.description).join(' ')
  assert.match(visible, /草稿/); assert.doesNotMatch(visible, /A\/1-[12]/)
  assert.equal(file.versionNo, 'A/1-2'); assert.equal(file.versionHistory[0].versionNo, 'A/1-1')
})

test('mounted signed stage snapshot hides the counter and its real preview handler reads the fixed ID', async () => {
  const snapshotId = '9007199254740993'
  const member = { tenantId: '1', snapshotId, relatedMasterId: '300', selectedControlledFileId: FILE, projectId: '20', fileNameSnapshot: '当时关联', fileNumberSnapshot: 'F1', versionNoSnapshot: 'A/1-2', origin: 'UPLOAD', supplemental: false, assigneeUserId: null, dueAt: null }
  const header = { id: snapshotId, tenantId: '1', sourceFileId: SECOND, sourceMasterId: '100', projectId: '20', processInstanceId: 'round-1', taskId: 'task-1', obligationId: 'dept-5', stageCode: 'MATRIX_REVIEW', nodeInstanceId: 'node-1', departmentId: '5', actorId: '7', signatureId: '9007199254740995', signedAt: '2026-10-14 12:00:01', commandKey: 'key', payloadHash: 'a'.repeat(64), expectedDigest: 'b'.repeat(64), completeDigest: 'c'.repeat(64), actionType: 'ASSIGN', reason: '本次真实意见', applicationType: 'REVISION', attributeRound: 1, formActionInstanceId: null }
  const evidence = { tenantId: '1', fileId: SECOND, masterId: '100', projectId: '20', applicationType: 'REVISION', attributeRound: 1, bpmRound: 'round-1', recorded: true, unavailableReason: null, originalUploadSelections: [], completeSelections: [], relationDigest: 'd'.repeat(64), stages: [{ snapshot: { header, members: [member] }, signatureEvidenceStatus: 'INVALID', signatureIdentityMatches: true }] }
  const calls = []
  const mounted = mount(component('detail/DetailRelationStageEvidence.vue', { '@/api/dcc/controlledFile/applicationRead': { getControlledFileRelationPermissions: async id => { calls.push(id); return { ...candidate(id), status: 'OBSOLETE', canPreview: true } } } }), { evidence, tenantId: '1', fileId: SECOND, applicationType: 'REVISION', bpmRound: 'round-1', attributeRound: 1, contextKey: 'frozen-stage' })
  try {
    await flush(); noRawDraft(mounted)
    await findAll(mounted.root, 'el-button').find(n => text(n) === '查看固定版本').props.onClick()
    await flush(); assert.deepEqual(calls, [FILE]); assert.equal(member.versionNoSnapshot, 'A/1-2')
  } finally { mounted.unmount() }
})

test('actual browser initialization uses the exact response row rather than the first same-label draft', () => {
  const { descriptor } = parse(read('browser/index.vue'))
  const ast = ts.createSourceFile('browser.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declarations = ast.statements.filter(s => s.declarationList?.declarations.some(d => ['resolveInitialSelectedVersionId', 'getSelectedVersion'].includes(d.name.text)))
  const options = [{ id: FILE, versionNo: 'A/1-1', status: 'WORKING' }, { id: SECOND, versionNo: 'A/1-2', status: 'WORKING' }]
  const exports = {}
  new Function('exports', 'getVersionOptions', 'resolveWorkingBrowserSelection', 'route', compile(declarations.map(s => s.getText(ast)).join('\n') + '\nexports.initial=resolveInitialSelectedVersionId;exports.selected=getSelectedVersion;'))(exports, () => options, () => undefined, { query: {} })
  assert.equal(exports.initial({ id: SECOND }), SECOND)
  assert.equal(exports.selected({ id: SECOND, selectedVersionId: SECOND }).id, SECOND)
  assert.throws(() => exports.selected({ id: SECOND, selectedVersionId: '999' }), /所选版本身份已失效/)
})

test('all eight authorized Vue files compile with their real script binding metadata', () => {
  for (const file of ['browser/index.vue', 'browser/ProjectFolderFileTable.vue', 'browser/ProjectFileRelationsDialog.vue', 'revision/DccRevisionPanel.vue', 'detail/DetailRelationArrangements.vue', 'detail/DetailRelationStageEvidence.vue', 'relations/DccFileRelations.vue', 'relations/DccProjectReferences.vue']) {
    const { descriptor, errors } = parse(read(file), { filename: file })
    assert.deepEqual(errors, [], file)
    const script = compileScript(descriptor, { id: file })
    const template = compileTemplate({ source: descriptor.template.content, filename: file, id: file, compilerOptions: { bindingMetadata: script.bindings } })
    assert.deepEqual(template.errors, [], file)
  }
})
