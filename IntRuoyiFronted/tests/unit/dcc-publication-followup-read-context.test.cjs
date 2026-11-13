const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const read = file => fs.readFileSync(file, 'utf8')
const flush = async () => { for (let i = 0; i < 12; i++) await Promise.resolve(); await vue.nextTick() }
function load(source, dependencies = {}) {
  const host = { exports: {}, ...vue, TextEncoder, TextDecoder, btoa, atob,
    require: id => { if (!(id in dependencies)) throw Error(id); return dependencies[id] } }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, host)
  return host.exports
}
const body = load(read('src/api/dcc/controlledFile/bodyReadContext.ts'))
function mount(selection, request = async () => null, renderTemplate = false) {
  const file = 'src/views/dcc/controlled-file/detail/PublicationFollowupPanel.vue'
  const script = compileScript(parse(read(file)).descriptor, { id: file })
  const calls = []
  const component = load(script.content, {
    vue,
    '@/api/dcc/controlledFile/publicationFollowup': { getPublicationFollowupByFile: (...args) => {
      calls.push(args); return request(...args)
    } },
    '@/api/dcc/controlledFile/bodyReadContext': body,
    '../shared/publicationFollowupPresentation': { batchStatusLabel: () => '已完成' },
    '../shared/PublicationFollowupTimeline.vue': {}
  }).default
  component.render = renderTemplate ? load(compileTemplate({
    source: parse(read(file)).descriptor.template.content, filename: file, id: file,
    compilerOptions: { bindingMetadata: script.bindings }
  }).code, { vue }).render : () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: tag => ({ tag, children: [] }),
    createText: text => ({ text }), createComment: text => ({ text }),
    insert(c, p) { c.parent = p; p.children.push(c) },
    remove(c) { if (c.parent) c.parent.children = c.parent.children.filter(child => child !== c) },
    setText() {}, setElementText(el, text) { el.text = text }, parentNode: c => c.parent,
    nextSibling: () => null, patchProp() {} })
  const props = vue.reactive({ controlledFileId: '9007199254740993', readSelection: selection })
  const app = renderer.createApp({ render: () => vue.h(component, props) })
  for (const name of ['ContentWrap', 'el-alert', 'el-button', 'el-tag', 'el-table', 'el-table-column']) {
    app.component(name, { setup: (_, { slots }) => () => vue.h(name, slots.default?.()) })
  }
  app.directive('loading', {})
  const host = app.mount({ children: [] })
  return { props, calls, state: host.$.subTree.component.setupState, app,
    tree: () => host.$.subTree.component.subTree }
}
test('obsolete todo forwards exact round and task without losing Long file IDs', async () => {
  const p = mount({ bpmRound: 'obsolete-round', taskId: 'approval-task', applicationType: 'OBSOLETE', lockRound: true })
  try {
    await flush()
    assert.equal(p.calls[0][0], '9007199254740993')
    assert.deepEqual(JSON.parse(JSON.stringify(p.calls[0][1])), {
      processInstanceId: 'obsolete-round', taskId: 'approval-task', applicationType: 'OBSOLETE'
    })
  } finally { p.app.unmount() }
})
test('encoded context remains exact and denied reads never retry with daily permission', async () => {
  const context = { intent: 'APPROVAL_TASK', controlledFileId: '9007199254740993',
    expectedVersionNo: 'A/1', applicationType: 'OBSOLETE', bpmRound: 'obsolete-round', taskId: 'approval-task' }
  const p = mount({ bodyReadContext: context, lockRound: true }, async () => { throw Error('该任务无权访问') })
  try {
    await flush()
    assert.equal(p.calls.length, 1)
    assert.equal(p.calls[0][1].bodyReadContext, body.serializeDccBodyReadContext(context.controlledFileId, context))
    assert.deepEqual(Object.keys(p.calls[0][1]), ['bodyReadContext'])
    assert.equal(p.state.followupLoadError, '该任务无权访问')
    assert.equal(p.state.followup, null)
  } finally { p.app.unmount() }
})
test('switching task on the same file discards a late response from the previous task', async () => {
  let resolve
  const pending = new Promise(r => { resolve = r })
  const p = mount({ bpmRound: 'old', taskId: 'old-task', lockRound: true },
    (_, params) => params.processInstanceId === 'old' ? pending : Promise.resolve({ publishedControlledFileId: '9007199254740993', versionNo: 'new' }))
  try {
    await flush()
    p.props.readSelection = { bpmRound: 'new', taskId: 'new-task', lockRound: true }
    await flush()
    resolve({ versionNo: 'old' })
    await flush()
    assert.equal(p.calls.length, 2)
    assert.equal(p.state.followup.versionNo, 'new')
    assert.equal(p.state.followupLoading, false)
  } finally { p.app.unmount() }
})

test('no publication record renders no card, heading or empty illustration', async () => {
  const p = mount(undefined, async () => null, true)
  try {
    await flush()
    assert.equal(p.tree().type, vue.Comment)
  } finally { p.app.unmount() }
})

test('a real publication record displays the card and hides empty subsections', async () => {
  const p = mount(undefined, async () => ({ publishedControlledFileId: '9007199254740993',
    fileNumber: 'DOC-13', versionNo: 'A/1', status: 'COMPLETED',
    visibilityRules: [], notificationDeliveries: [], impactTasks: [], timeline: [] }), true)
  try {
    await flush()
    assert.notEqual(p.tree().type, vue.Comment)
    const sections = []
    const visit = vnode => {
      if (vnode.type === 'section') sections.push(vnode)
      if (vnode.component) visit(vnode.component.subTree)
      if (Array.isArray(vnode.children)) vnode.children.forEach(visit)
    }
    visit(p.tree())
    assert.equal(sections.length, 1)
  } finally { p.app.unmount() }
})

test('wrong version is rejected and load errors remain visible with retry', async () => {
  const p = mount(undefined, async () => ({ publishedControlledFileId: 'another-version' }), true)
  try {
    await flush()
    assert.equal(p.state.followup, null)
    assert.match(p.state.followupLoadError, /当前查看版本不一致/)
    assert.notEqual(p.tree().type, vue.Comment)
  } finally { p.app.unmount() }
})

test('loading reserves no empty card and changing the selected task clears the previous record', async () => {
  let complete
  const pending = new Promise(resolve => { complete = resolve })
  const p = mount({ bpmRound: 'first', taskId: 'first-task', lockRound: true },
    (_, params) => params.processInstanceId === 'first' ? Promise.resolve({
      publishedControlledFileId: '9007199254740993', fileNumber: 'DOC-13', versionNo: 'A/1',
      status: 'COMPLETED', visibilityRules: [], notificationDeliveries: [], impactTasks: [], timeline: []
    }) : pending, true)
  try {
    assert.equal(p.tree().type, vue.Comment)
    await flush()
    assert.notEqual(p.tree().type, vue.Comment)
    p.props.readSelection = { bpmRound: 'second', taskId: 'second-task', lockRound: true }
    await flush()
    assert.equal(p.tree().type, vue.Comment)
    complete(null)
    await flush()
    assert.equal(p.tree().type, vue.Comment)
  } finally { p.app.unmount() }
})
