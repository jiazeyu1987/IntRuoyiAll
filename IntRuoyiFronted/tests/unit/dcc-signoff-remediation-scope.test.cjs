const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = file => fs.readFileSync(file, 'utf8')
const moduleFrom = (source, dependencies = {}) => {
  const c = { exports: {}, Error, JSON, String, Number, BigInt, Array, Object, Set, console,
    require: id => id === 'vue' ? vue : dependencies[id] || (id.endsWith('.vue') ? {} : (() => { throw new Error(id) })()) }
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c.exports
}
const actions = moduleFrom(read('src/views/dcc/controlled-file/workflow/workflow-actions.ts'))
function mount(file, props, dependencies) {
  const compiled = compileScript(parse(read(file)).descriptor, { id: file })
  const component = moduleFrom(compiled.content, dependencies).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: () => ({}), insert: (n, p) => p.children.push(n), remove() {}, setText() {}, setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const reactive = vue.reactive(props), app = renderer.createApp({ render: () => vue.h(component, reactive) }), host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, props: reactive, app }
}
const drain = async () => { for (let i = 0; i < 12; i++) await Promise.resolve(); await vue.nextTick() }
function assignment(processDefinitionKey, fail = false) {
  const reads = [], writes = [], context = { controlledFileId: '9007199254740993', processInstanceId: 'bpm-round', taskId: 'task-id', obligationId: 'obligation', departmentId: '100', departmentName: '质量部', assigned: false, canAssign: true, assigneeOptions: [{ id: '7', name: '会签甲' }], processDefinitionKey }
  const api = moduleFrom(read('src/api/dcc/controlledFile/workflowLifecycle.ts'), { '@/config/axios': { default: { post: async value => { writes.push(value); return true } } } })
  const panel = mount('src/views/dcc/controlled-file/detail/DetailSignoffAssignment.vue', { fileId: context.controlledFileId, processInstanceId: context.processInstanceId, taskId: context.taskId }, {
    '@/api/dcc/controlledFile/applicationRead': { getSignoffAssignmentContext: async () => context },
    '@/api/dcc/controlledFile/workflowLifecycle': api,
    '@/api/dcc/controlledFile/relations': {
      listHistoricalRelations: async () => { reads.push('history'); return [{ masterId: '8', fileName: '真实关联.docx', versionNo: 'A/1' }] },
      listRelationArrangements: async () => { reads.push('arrangements'); if (fail) throw new Error('整改安排真实读取拒绝'); return [] }
    },
    '@/api/system/user': { getSimpleUserList: async () => { reads.push('people'); return [{ id: '7', nickname: '会签甲' }] } },
    '../workflow/workflow-actions': actions
  })
  return { panel, reads, writes, context }
}
test('actual NEW and obsolete signed assignment do not read inapplicable remediation or send a fake empty field', async () => {
  for (const key of ['dcc-controlled-file-upload', 'dcc-controlled-file-obsolete']) {
    const h = assignment(key, true)
    let signing
    try {
      await drain()
      assert.deepEqual(h.reads, [])
      assert.equal(h.panel.state.ready, true)
      signing = mount('src/views/dcc/controlled-file/workflow/SignoffAssignmentPanel.vue', { fileId: h.context.controlledFileId, processInstanceId: 'bpm-round', taskId: 'task-id', departmentName: '质量部', users: h.context.assigneeOptions, assigned: false, arrangementsReady: h.panel.state.ready, save: h.panel.state.saveAssignment }, { './workflow-actions': actions })
      signing.state.selected = '7'; signing.state.password = 'unit-only'; signing.state.reason = '真实指派意见'
      await signing.state.submit()
      assert.equal(h.writes.length, 1)
      assert.equal(h.writes[0].url, '/dcc/controlled-file/workflow-lifecycle/9007199254740993/assign-signoff')
      assert.equal(Object.hasOwn(h.writes[0].data, 'relationArrangements'), false)
      assert.equal(h.writes[0].data.assigneeUserId, '7')
    } finally { signing?.app.unmount(); h.panel.app.unmount() }
  }
})
test('actual revision keeps authoritative remediation reads and validated arrangements in its signed command', async () => {
  const h = assignment('dcc-controlled-file-revision')
  try {
    await drain()
    assert.deepEqual(h.reads, ['history', 'people', 'arrangements'])
    h.panel.state.arrangementsRef = { validate: () => [{ relatedMasterId: '8', assigneeUserId: '7', dueAt: '2026-12-01 12:00:00' }] }
    await h.panel.state.saveAssignment({ taskId: 'task-id', assigneeUserId: '7', password: 'unit-only', reason: '升版指派' })
    assert.equal(h.writes[0].data.relationArrangements[0].relatedMasterId, '8')
  } finally { h.panel.app.unmount() }
})
test('actual revision remediation read failure remains visible and blocks every assignment write', async () => {
  const h = assignment('dcc-controlled-file-revision', true)
  try {
    await drain()
    assert.equal(h.panel.state.ready, false)
    assert.equal(h.panel.state.error, '整改安排真实读取拒绝')
    await assert.rejects(h.panel.state.saveAssignment({ taskId: 'task-id', assigneeUserId: '7', password: 'unit-only', reason: '意见' }), /上下文/)
    assert.equal(h.writes.length, 0)
  } finally { h.panel.app.unmount() }
})
test('actual missing or unknown current process scope refuses rather than guessing nonrevision', async () => {
  for (const key of [undefined, 'unknown-process']) {
    const h = assignment(key)
    try { await drain(); assert.equal(h.panel.state.ready, false); assert.match(h.panel.state.error, /流程|类型|适用/); assert.equal(h.writes.length, 0) }
    finally { h.panel.app.unmount() }
  }
})
test('actual signoff context wrapper requires the current formal task process definition', async () => {
  let response = { controlledFileId: '9007199254740993', processInstanceId: 'bpm-round', taskId: 'task-id', obligationId: 'obligation', departmentId: '100', assigned: false, canAssign: true, assigneeOptions: [] }
  const api = moduleFrom(read('src/api/dcc/controlledFile/applicationRead.ts'), {
    '@/config/axios': { default: { get: async () => response } },
    '@/views/dcc/controlled-file/project-attributes/state': { validateAttributes: v => v }
  })
  await assert.rejects(api.getSignoffAssignmentContext('9007199254740993', 'task-id'), /流程|资格|响应/)
  for (const key of ['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete']) {
    response = { ...response, processDefinitionKey: key }
    assert.equal((await api.getSignoffAssignmentContext('9007199254740993', 'task-id')).processDefinitionKey, key)
  }
})
