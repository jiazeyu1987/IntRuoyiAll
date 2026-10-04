const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const folder = 'src/views/dcc/controlled-file/workflow/'
function moduleFrom(source, requireModule) {
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
  const context = { exports: {}, Error, require: requireModule }; vm.runInNewContext(code, context); return context.exports
}
function mountList(load) {
  const source = fs.readFileSync(folder + 'PendingWorkflowDistributionList.vue', 'utf8')
  const { descriptor, errors } = parse(source); assert.equal(errors.length, 0)
  assert.equal(compileTemplate({ source: descriptor.template.content, filename: 'PendingWorkflowDistributionList.vue', id: 'reminders' }).errors.length, 0)
  const actions = moduleFrom(fs.readFileSync(folder + 'workflow-actions.ts', 'utf8'))
  const component = moduleFrom(compileScript(descriptor, { id: 'reminders' }).content, id => {
    if (id === 'vue') return vue
    if (id === './workflow-actions') return actions
    throw new Error('Unexpected dependency: ' + id)
  }).default
  component.render = () => vue.h('section')
  const props = vue.reactive({ contextKey: 'tenant-1-user-99', canHandle: true, load }); const opened = []
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }),
    createComment: text => ({ text }), insert: (child, parent) => parent.children.push(child), remove() {},
    setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text },
    parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp({ render: () => vue.h(component, { ...props, onOpen: file => opened.push(file) }) })
  const root = app.mount({ children: [] }); return { props, app, opened, state: root.$.subTree.component.setupState }
}
const row = (id, date, stage) => ({ id, effectiveDate: date, distributionReminderStage: stage, fileNumber: 'SOP-' + id,
  title: '受控文件', versionNo: 'B/1', controlledTime: '2026-10-01 08:00', status: 'CONTROLLED_PENDING_EFFECTIVE' })
const flush = async () => { await Promise.resolve(); await vue.nextTick(); await Promise.resolve() }
function deferred() { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
test('server-ordered reminders preserve server classifications and exclude distributed items', async () => {
  const panel = mountList(async () => [row('1', '2026-10-01', 'OVERDUE'), row('2', '2026-10-10', 'UPCOMING'),
    { ...row('3', '2026-10-11', 'FUTURE'), distributedTime: '2026-10-01 09:00' }])
  try {
    await flush(); assert.equal(panel.state.rows.length, 2)
    assert.equal(panel.state.reminderLabel(panel.state.rows[0].distributionReminderStage), '逾期未下发')
    panel.state.openFile(panel.state.rows[1]); assert.equal(panel.opened[0].id, '2')
  } finally { panel.app.unmount() }
})
test('missing server reminder stage is an explicit visible error rather than using browser time', async () => {
  const file = row('1', '2026-10-10', undefined)
  const panel = mountList(async () => [file])
  try { await flush(); assert.equal(panel.state.rows.length, 0); assert.match(panel.state.error, /证据不完整/) }
  finally { panel.app.unmount() }
})
test('out-of-order or duplicate formal file identities are rejected and not silently reordered', async () => {
  const panel = mountList(async () => [row('2', '2026-10-11', 'FUTURE'), row('1', '2026-10-01', 'OVERDUE')])
  try { await flush(); assert.equal(panel.state.rows.length, 0); assert.match(panel.state.error, /未按生效日期排序/) }
  finally { panel.app.unmount() }
})
test('old tenant response never overwrites the current tenant reminder list', async () => {
  const old = deferred(); let calls = 0
  const panel = mountList(async () => ++calls === 1 ? old.promise : [row('20', '2026-10-10', 'DUE')])
  try {
    panel.props.contextKey = 'tenant-122-user-100'; await flush()
    assert.equal(panel.state.rows[0].id, '20')
    old.resolve([row('1', '2026-10-01', 'OVERDUE')]); await flush()
    assert.equal(panel.state.rows[0].id, '20')
  } finally { panel.app.unmount() }
})
test('lost permission removes old rows and cannot open a previously loaded file', async () => {
  const panel = mountList(async () => [row('1', '2026-10-10', 'DUE')])
  try {
    await flush(); const file = panel.state.rows[0]
    panel.props.canHandle = false; await flush()
    assert.equal(panel.state.rows.length, 0)
    panel.state.openFile(file); assert.equal(panel.opened.length, 0)
  } finally { panel.app.unmount() }
})
test('revoking permission during a pending load clears the loading state and ignores its late result', async () => {
  const pending = deferred()
  const panel = mountList(() => pending.promise)
  try {
    assert.equal(panel.state.loading, true)
    panel.props.canHandle = false; await flush()
    assert.equal(panel.state.loading, false)
    pending.resolve([row('1', '2026-10-10', 'DUE')]); await flush()
    assert.equal(panel.state.rows.length, 0)
  } finally { panel.app.unmount() }
})
