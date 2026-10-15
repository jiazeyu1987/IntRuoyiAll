const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
function moduleFrom(source, dependencies = {}) {
  const context = { exports: {}, Error, String, JSON, Array, Object, Set,
    require: name => name === 'vue' ? vue : dependencies[name] || (name.endsWith('.vue') ? {} : (() => { throw Error(name) })()) }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const actions = moduleFrom(read('src/views/dcc/controlled-file/workflow/workflow-actions.ts'))
const renderer = () => vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }),
  createComment: () => ({}), insert: (node, parent) => parent.children.push(node), remove() {}, setText() {},
  setElementText(node, text) { node.text = text }, patchProp() {}, parentNode: () => null, nextSibling: () => null })
function mount(file, props, dependencies = {}) {
  const component = moduleFrom(compileScript(parse(read(file)).descriptor, { id: file }).content, dependencies).default
  component.render = () => vue.h('section')
  const reactive = vue.reactive(props)
  const app = renderer().createApp({ render: () => vue.h(component, reactive) })
  const host = app.mount({ children: [] })
  return { app, props: reactive, state: host.$.subTree.component.setupState }
}
function assignment() {
  const writes = [], externalLoads = []
  const panel = mount('src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue', {
    fileId: '10', processInstanceId: 'round-a', taskId: 'task-a', departmentName: 'A部门', assigned: false,
    users: [{ id: '99', name: '本人' }, { id: '100', name: '本部门成员' }],
    loadAllUsers: async () => { externalLoads.push(true); return [{ id: '200', name: 'B部门成员', departmentName: 'B部门' }] },
    save: async request => { writes.push(request); return true }
  }, { './workflow-actions': actions })
  panel.state.password = 'unit-only'; panel.state.reason = '本部门指派'
  return { ...panel, writes, externalLoads }
}
const drain = async () => { for (let i = 0; i < 12; i++) await Promise.resolve(); await vue.nextTick() }

test('actual assignment cannot load or sign a cross-department user through the former ALL selector', async () => {
  const panel = assignment()
  try {
    if (panel.state.changeCandidateScope) await panel.state.changeCandidateScope('ALL')
    panel.state.selected = '200'
    await panel.state.submit()
    assert.equal(panel.externalLoads.length, 0, '会签不能加载全员目录')
    assert.equal(panel.writes.length, 0, '跨部门账号不能进入签名保存')
    assert.match(panel.state.error, /候选|本部门/)
  } finally { panel.app.unmount() }
})

test('actual component template has one department candidate selector and no ALL option', () => {
  const descriptor = parse(read('src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue')).descriptor
  const compiled = compileScript(descriptor, { id: 'department-template', inlineTemplate: true })
  const component = moduleFrom(compiled.content, { './workflow-actions': actions }).default
  const values = [], optionLabels = [], mounted = []
  const app = renderer().createApp(component, { fileId: '10', processInstanceId: 'round-a', taskId: 'task-a',
    departmentName: 'A部门', users: [{ id: '99', name: '本人' }], assigned: false, save: async () => true,
    loadAllUsers: async () => [{ id: '200', name: 'B部门人员' }] })
  for (const name of ['ElSelect', 'ElOption', 'ElInput', 'ElButton']) app.component(name, {
    props: ['value', 'label'], setup(props, { slots }) {
      mounted.push(name)
      if (name === 'ElOption') { values.push(props.value); optionLabels.push(props.label) }
      return () => vue.h('span', slots.default?.())
    }
  })
  try {
    app.mount({ children: [] })
    assert.equal(mounted.filter(name => name === 'ElSelect').length, 1)
    assert.deepEqual(values, ['99'])
    assert.equal(optionLabels.some(label => /全员|其他部门/.test(label)), false)
  } finally { app.unmount() }
})

test('department member and self can each sign but a selection removed from current candidates cannot', async () => {
  for (const id of ['99', '100']) {
    const panel = assignment()
    try {
      panel.state.selected = id
      await panel.state.submit()
      assert.equal(panel.writes.length, 1)
      assert.equal(panel.writes[0].assigneeUserId, id)
      assert.equal(panel.writes[0].taskId, 'task-a')
    } finally { panel.app.unmount() }
  }
  const panel = assignment()
  try {
    panel.state.selected = '100'
    panel.props.users = [{ id: '99', name: '本人' }]
    await vue.nextTick()
    await panel.state.submit()
    assert.equal(panel.writes.length, 0)
  } finally { panel.app.unmount() }
})

test('actual detail save refuses an injected user outside the exact department context', async () => {
  const writes = [], peopleReads = []
  const context = { controlledFileId: '10', processInstanceId: 'round-a', taskId: 'task-a',
    obligationId: 'a', departmentId: '51', departmentName: 'A部门', assigned: false, canAssign: true,
    processDefinitionKey: 'dcc-controlled-file-upload', assigneeOptions: [{ id: '99', name: '本人' }] }
  const panel = mount('src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue',
    { fileId: '10', processInstanceId: 'round-a', taskId: 'task-a' }, {
      '@/api/dcc/controlledFile/applicationRead': { getSignoffAssignmentContext: async () => context },
      '@/api/dcc/controlledFile/workflowLifecycle': { assignWorkflowSignoff: async (...args) => { writes.push(args); return true } },
      '@/api/dcc/controlledFile/relations': {},
      '@/api/system/user': { getSimpleUserList: async () => { peopleReads.push(true); return [] } },
      '../workflow/workflow-actions': actions
    })
  try {
    await drain()
    assert.equal(panel.state.ready, true)
    await assert.rejects(panel.state.saveAssignment({ taskId: 'another-task', assigneeUserId: '99', password: 'unit-only', reason: '指派' }), /上下文/)
    assert.equal(writes.length, 0)
    await assert.rejects(panel.state.saveAssignment({ taskId: 'task-a', assigneeUserId: '200', password: 'unit-only', reason: '指派' }), /本部门|候选/)
    assert.equal(writes.length, 0)
    assert.equal(peopleReads.length, 0)
    await panel.state.saveAssignment({ taskId: 'task-a', assigneeUserId: '99', password: 'unit-only', reason: '指派本人' })
    assert.equal(writes.length, 1)
  } finally { panel.app.unmount() }
})
