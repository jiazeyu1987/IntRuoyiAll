const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
function loadModel(relativePath) {
  const filename = path.resolve(root, relativePath)
  const context = { exports: {} }
  context.require = name => loadModel(path.relative(root, path.resolve(path.dirname(filename), name + '.ts')))
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
const revision = loadModel('src/views/dcc/controlled-file/revision/revision-model.ts')
const version = loadModel('src/views/dcc/controlled-file/shared/version-display.ts')
const filename = path.join(root, 'src/views/dcc/controlled-file/revision/DccRevisionPanel.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const compiled = compileScript(descriptor, { id: 'revision-preparation-blocker', inlineTemplate: true })
const context = { exports: {}, require: name => {
  if (name === 'vue') return vue
  if (name === '../shared/version-display') return version
  if (name === './revision-model') return revision
  if (name === '../detail/presentation') return { formatControlledFileDateTime: String }
  throw new Error('Unexpected dependency: ' + name)
} }
vm.runInNewContext(ts.transpileModule(compiled.content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, context)

function mount(overrides = {}) {
  const nodes = [], events = [], writes = []
  const renderer = vue.createRenderer({
    createElement: type => ({ type, props: {}, children: [] }),
    createText: text => ({ text }), createComment: () => ({}),
    insert: (node, parent) => parent.children.push(node), remove() {},
    setText: (node, text) => { node.text = text },
    setElementText: (node, text) => { node.text = text },
    patchProp: (node, key, previous, value) => { node.props[key] = value },
    parentNode: () => null, nextSibling: () => null
  })
  const props = {
    baselineId: '9007199254740993', baselineVersionNo: 'A/1', idempotencyKey: 'test-click',
    facts: {}, initialIntent: 'PARTIAL',
    partialTarget: { versionNo: 'A/2', unavailableReason: 'TARGET_ALREADY_ALLOCATED' },
    replacementTarget: { versionNo: 'B/1', unavailableReason: null },
    iterations: [{ id: '9007199254740995', versionNo: 'A/1-2', canPartial: false, canReplacement: true, canPreview: false }],
    canPartial: false, canReplacement: false,
    submitRevision: async command => writes.push(command),
    onPrepareBody: (...args) => events.push(args), ...overrides
  }
  const app = renderer.createApp(context.exports.default, props)
  for (const name of ['el-descriptions', 'el-descriptions-item', 'el-alert', 'el-button', 'el-form',
    'el-form-item', 'el-radio-group', 'el-radio', 'el-select', 'el-option', 'el-input']) {
    app.component(name, { setup: (_, { attrs, slots }) => () => {
      const node = vue.h(name, attrs, slots.default?.())
      nodes.push(node)
      return node
    } })
  }
  const tree = { children: [] }
  app.mount(tree)
  return { nodes, events, writes, close: () => app.unmount() }
}

test('occupied partial target explains the actual blocker and leads to the original version without submitting', () => {
  const h = mount()
  try {
    const warning = h.nodes.find(node => node.type === 'el-alert')
    assert.match(warning.props.title, /A\/2.*已有变更记录/)
    assert.match(warning.props.title, /原申请人.*该版本检出、修正并检入后重新提交/)
    assert.doesNotMatch(warning.props.title, /当前账号没有可用/)
    const button = h.nodes.find(node => node.type === 'el-button')
    assert.equal(button.children[0].children, '前往受控浏览查看原变更版本')
    button.props.onClick()
    assert.deepEqual(h.events, [['A/2', 'PARTIAL']])
    assert.equal(h.nodes.some(node => node.type === 'el-form'), false)
    assert.equal(h.writes.length, 0)
  } finally { h.close() }
})

test('replacement uses its own target and does not report the blocked partial target', () => {
  const h = mount({ initialIntent: 'REPLACEMENT', iterations: [], replacementTarget: { versionNo: 'B/1', unavailableReason: 'TARGET_ALREADY_ALLOCATED' } })
  try {
    const title = h.nodes.find(node => node.type === 'el-alert').props.title
    assert.match(title, /B\/1.*已有变更记录/)
    assert.doesNotMatch(title, /A\/2/)
  } finally { h.close() }
})

test('missing body, body permissions and unknown server blockers remain distinct', () => {
  for (const [overrides, expected] of [
    [{ partialTarget: { versionNo: 'A/2', unavailableReason: null }, iterations: [] }, /尚无本次变更的工作正文/],
    [{ partialTarget: { versionNo: 'A/2', unavailableReason: null } }, /当前账号没有可用于本次变更/],
    [{ partialTarget: { versionNo: 'A/2', unavailableReason: 'ACTUAL_SERVER_REASON' } }, /当前不可分配：ACTUAL_SERVER_REASON/]
  ]) {
    const h = mount(overrides)
    try { assert.match(h.nodes.find(node => node.type === 'el-alert').props.title, expected) }
    finally { h.close() }
  }
})

test('available partial bodies and existing locks do not show an occupied-target preparation warning', () => {
  for (const overrides of [
    { partialTarget: { versionNo: 'A/2', unavailableReason: null }, canPartial: true,
      iterations: [{ id: '2', versionNo: 'A/1-2', canPartial: true, canReplacement: true, canPreview: false }] },
    { lockedReason: 'FORMAL_REVISION_IN_PROGRESS' }, { checkedOutBy: '99' }
  ]) {
    const h = mount(overrides)
    try {
      assert.equal(h.nodes.some(node => node.type === 'el-alert' && /已有变更记录/.test(node.props.title)), false)
    } finally { h.close() }
  }
})

const BASELINE = '2054545668044084445', ORIGINAL = '2054545668044084479', MASTER = '2054545668044079331'
const withdrawn = (patch = {}) => ({ id: ORIGINAL, versionNo: 'A/2', revisionChangeType: 'PARTIAL',
  revisionSourceControlledFileId: BASELINE, revisionAttemptNo: 1, status: 'WITHDRAWN', processInstanceId: 'actual-cancelled-round', ...patch })

test('rework selects the latest exact failed attempt, excluding old baseline drafts and other revision chains', () => {
  const versions = [withdrawn(), { id: '2054545668044084480', versionNo: 'A/1-2', status: 'WORKING' },
    withdrawn({ id: '2054545668044084481', revisionAttemptNo: 2, status: 'REJECTED' }),
    withdrawn({ id: '99', revisionSourceControlledFileId: '98', revisionAttemptNo: 3 }),
    withdrawn({ id: '97', revisionChangeType: 'REPLACEMENT', revisionAttemptNo: 4 })]
  const source = revision.requireRevisionReworkSource(BASELINE, 'A/2', 'PARTIAL', versions)
  assert.equal(source.id, '2054545668044084481')
  assert.equal(versions[0].id, ORIGINAL, 'selection must not reorder the supplied version history')
  for (const rows of [[], [withdrawn({ id: 9007199254740992 })], [withdrawn(), withdrawn({ id: '96' })],
    [withdrawn(), withdrawn({ id: '95', revisionAttemptNo: 2, status: 'PENDING_APPROVAL' })]]) {
    assert.throws(() => revision.requireRevisionReworkSource(BASELINE, 'A/2', 'PARTIAL', rows))
  }
})

const detailDescriptor = parse(fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')).descriptor
const detailAst = ts.createSourceFile('detail.ts', detailDescriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const prepareDeclaration = detailAst.statements.find(node => ts.isVariableStatement(node)
  && node.declarationList.declarations.some(declaration => declaration.name.text === 'prepareApplicationBody'))
assert.ok(prepareDeclaration)
const { buildWorkingBrowserRoute } = loadModel('src/views/dcc/controlled-file/shared/working-browser-navigation.ts')
function navigationHost(read = async () => {}) {
  const routes = [], errors = [], reads = [], baseline = { id: BASELINE, masterId: MASTER, versionHistory: [withdrawn()] }
  const state = { exports: {}, requireRevisionReworkSource: revision.requireRevisionReworkSource, buildWorkingBrowserRoute,
    ensureCurrentDetailContext: () => true, fileDetail: { value: baseline },
    openWorkingFileInBrowser: () => routes.push('current-body'),
    getControlledFile: async id => { reads.push(id); await read(state); return { ...withdrawn(), masterId: MASTER,
      hasProjectStorageMapping: true, dccProjectCodeId: '285', projectFolderId: '21', fileNumber: '999999', ...state.actualPatch } },
    router: { push: route => routes.push(route) }, message: { error: error => errors.push(error) } }
  vm.runInNewContext(ts.transpileModule(prepareDeclaration.getText(detailAst) + '\nexports.prepare=prepareApplicationBody', {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, state)
  return { state, routes, errors, reads, prepare: state.exports.prepare }
}

test('original-revision button reads its actual location and routes to the withdrawn version, preserving its exact Long identity', async () => {
  const h = navigationHost()
  await h.prepare('A/2', 'PARTIAL')
  assert.deepEqual(h.reads, [ORIGINAL])
  assert.equal(h.routes.length, 1)
  assert.equal(h.routes[0].query.workingFileId, ORIGINAL)
  assert.equal(h.routes[0].query.workingMasterId, MASTER)
  assert.equal(h.routes[0].query.browserMode, 'working')
  assert.equal(h.routes[0].query.directoryId, undefined)
  assert.equal(h.errors.length, 0)
  const normal = navigationHost()
  await normal.prepare()
  assert.deepEqual(normal.routes, ['current-body'])
  assert.equal(normal.reads.length, 0)
})

test('stale, failed, foreign or changed original-revision reads cannot navigate to another version', async () => {
  for (const alter of [state => { state.fileDetail.value = {} },
    state => { state.actualPatch = { masterId: '99' } }, state => { state.actualPatch = { status: 'ACTIVE' } },
    state => { state.actualPatch = { revisionSourceControlledFileId: '98' } },
    () => { throw new Error('Actual read failed') }]) {
    const h = navigationHost(alter)
    await h.prepare('A/2', 'PARTIAL')
    assert.equal(h.routes.length, 0)
    if (h.state.fileDetail.value.id === BASELINE) assert.equal(h.errors.length, 1)
  }
})

test('application panel relays the original target and detail listens to the repair handler', () => {
  const application = parse(fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue'), 'utf8')).descriptor
  compileScript(application, { id: 'application-repair-navigation' })
  compileScript(detailDescriptor, { id: 'detail-repair-navigation' })
  assert.match(application.template.content, /@prepare-body="\(targetVersion, intent\) => emit\('prepareBody', targetVersion, intent\)"/)
  assert.match(detailDescriptor.template.content, /@prepare-body="prepareApplicationBody"/)
})
