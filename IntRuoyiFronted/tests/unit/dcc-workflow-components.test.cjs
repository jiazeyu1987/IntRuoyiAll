const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

function compileModule(source, requireModule) {
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020 } }).outputText
  const context = { exports: {}, require: requireModule, Error }
  vm.runInNewContext(code, context)
  return context.exports
}
const actions = compileModule(fs.readFileSync('src/views/dcc/controlled-file/workflow/workflow-actions.ts', 'utf8'))
function component(name) {
  const { descriptor } = parse(fs.readFileSync(`src/views/dcc/controlled-file/workflow/${name}.vue`, 'utf8'))
  const compiled = compileScript(descriptor, { id: name })
  const exported = compileModule(compiled.content, id => {
    if (id === 'vue') return vue
    if (id === './workflow-actions') return actions
    throw new Error(`Unexpected component dependency: ${id}`)
  }).default
  exported.render = () => vue.h('section')
  return exported
}
function mountAssignment(save) {
  let saved = 0
  const props = vue.reactive({ fileId: '10', processInstanceId: 'round-1', taskId: 'task-old', departmentName: '质量部', users: [{ id: '99', name: '本人' }],
    assigned: false, save })
  const renderer = vue.createRenderer({
    createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
    insert: (child, parent) => parent.children.push(child), remove() {}, setText(node, text) { node.text = text },
    setElementText(node, text) { node.text = text }, parentNode: () => null, nextSibling: () => null, patchProp() {}
  })
  const panel = component('SignoffAssignmentPanel')
  const app = renderer.createApp({ render: () => vue.h(panel, { ...props, onSaved: () => { saved++ } }) })
  const root = app.mount({ children: [] })
  const state = root.$.subTree.component.setupState
  state.selected = '99'; state.password = 'test-credential'; state.reason = '指派本人'
  return { props, state, app, saved: () => saved }
}
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('signed assignment carries a frozen copy of the selected D remediation arrangements', async () => {
  const original=[{relatedMasterId:'30',assigneeUserId:'200',dueAt:'2026-11-01 12:00:00'}]
  let sent
  const panel=mountAssignment(async request=>{ sent=request; return true })
  panel.props.relationArrangements=original
  await vue.nextTick()
  await panel.state.submit()
  assert.deepEqual(JSON.parse(JSON.stringify(sent.relationArrangements)),original)
  original[0].assigneeUserId='201'
  assert.equal(sent.relationArrangements[0].assigneeUserId,'200')
  panel.app.unmount()
})

test('arrangement selection that is still loading cannot be silently replaced with an empty signed scope', async () => {
  let calls=0
  const panel=mountAssignment(async()=>{calls++;return true})
  panel.props.arrangementsReady=false
  await vue.nextTick()
  await panel.state.submit()
  assert.equal(calls,0)
  assert.match(panel.state.error,/整改安排/)
  panel.app.unmount()
})

test('resolved assignment data=false never emits a saved signing fact', async () => {
  const panel=mountAssignment(async()=>false)
  await panel.state.submit()
  assert.equal(panel.saved(),0)
  assert.match(panel.state.error,/指派/)
  panel.app.unmount()
})

test('actual Vue setup ignores old saved response after task context changes and clears signing inputs', async () => {
  const pending = deferred()
  const panel = mountAssignment(() => pending.promise)
  try {
    const submission = panel.state.submit()
    assert.equal(panel.state.busy, true)
    panel.props.taskId = 'task-new'
    await vue.nextTick()
    assert.equal(panel.state.password, '', 'signature credentials must not remain attached to another task')
    panel.state.password = 'new-task-credential'; panel.state.reason = '新任务意见'
    pending.resolve()
    await submission
    assert.equal(panel.saved(), 0, 'old success must never announce saved for the new task')
    assert.equal(panel.state.password, 'new-task-credential')
    assert.equal(panel.state.reason, '新任务意见')
  } finally { panel.app.unmount() }
})

test('actual Vue setup ignores old task error without overwriting current task feedback', async () => {
  const pending = deferred()
  const panel = mountAssignment(() => pending.promise)
  try {
    const submission = panel.state.submit()
    panel.props.taskId = 'task-new'; await vue.nextTick()
    panel.state.error = '新任务当前提示'
    pending.reject(new Error('旧任务签名错误'))
    await submission
    assert.equal(panel.state.error, '新任务当前提示')
    assert.equal(panel.saved(), 0)
  } finally { panel.app.unmount() }
})

test('unmounted assignment component never emits a late saved result', async () => {
  const pending = deferred()
  const panel = mountAssignment(() => pending.promise)
  const submission = panel.state.submit()
  panel.app.unmount()
  pending.resolve()
  await submission
  assert.equal(panel.saved(), 0)
})

test('assignment component refuses incomplete file and workflow round context', async () => {
  let saves = 0
  const panel = mountAssignment(async () => { saves++ })
  try {
    panel.props.processInstanceId = ''; await vue.nextTick()
    panel.state.selected = '99'; panel.state.password = 'test-credential'; panel.state.reason = '指派'
    await panel.state.submit()
    assert.equal(saves, 0)
    assert.match(panel.state.error, /任务上下文缺失/)
  } finally { panel.app.unmount() }
})
