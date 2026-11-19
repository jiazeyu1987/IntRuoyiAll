const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const drain = async () => { for (let i = 0; i < 20; i++) await Promise.resolve(); await vue.nextTick() }
const businessTime = {}, dateTime = {}
new Function('exports', transpile(fs.readFileSync('src/utils/businessTime.ts', 'utf8')))(businessTime)
new Function('exports', 'require', transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/dateTimeDisplay.ts', 'utf8')))(dateTime, id => { assert.equal(id, '@/utils/businessTime'); return businessTime })

function mount(dueAt, certificate, taskOverrides = {}) {
  const descriptor = parse(fs.readFileSync('src/views/dcc/registration-certificate/components/HandlingTaskPanel.vue', 'utf8')).descriptor
  const script = compileScript(descriptor, { id: 'task-date' })
  const template = compileTemplate({ id: 'task-date', source: descriptor.template.content, compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  const calls = [], exports = {}, rendered = {}, api = {}, renewal = {}
  const data = { task: { id: '3', title: 'Own task', dueAt, responsibleUserId: '1', assignedUserId: '910524', status: 'OPEN', ...taskOverrides }, events: [], canDelegate: true, canHandle: true, certificate }
  new Function('exports', 'require', transpile(fs.readFileSync('src/api/dcc/registrationCertificate/handlingTasks.ts', 'utf8')))(api, id => {
    assert.equal(id, '@/config/axios')
    return { default: { get: async () => data, post: async (_, payload) => payload } }
  })
  new Function('exports', 'require', 'ref', 'reactive', 'watch', 'onBeforeUnmount', transpile(script.content))(exports, id => {
    if (id === 'vue') return vue
    if (id.includes('/handlingTasks')) return { ...api, getHandlingCandidates: async () => [], delegateHandlingTask: async (id, payload) => calls.push({ id, payload }) }
    if (id.endsWith('/dateTimeDisplay')) return dateTime
    if (id.endsWith('/RenewalDialog.vue')) return { default: vue.defineComponent({ props: ['modelValue', 'certificate'], setup: props => () => { renewal.visible = props.modelValue; renewal.certificate = props.certificate; return null } }) }
    if (id.endsWith('.vue')) return { default: { render: () => null } }
    throw Error('Unexpected import ' + id)
  }, vue.ref, vue.reactive, vue.watch, vue.onBeforeUnmount)
  new Function('exports', 'require', transpile(template.code))(rendered, () => vue)
  exports.default.render = rendered.render
  const node = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
  const renderer = vue.createRenderer({ createElement: node, createText: text => node('#text', text), createComment: () => node('#comment'),
    insert(c, p) { c.parent = p; p.children.push(c) }, remove(c) { c.parent.children = c.parent.children.filter(n => n !== c) },
    setText(n, text) { n.text = text }, setElementText(n, text) { n.text = text; n.children = [] }, parentNode: n => n.parent, nextSibling: () => null, patchProp: (n, k, old, value) => { n.props[k] = value } })
  const app = renderer.createApp(exports.default, { taskId: '3' }), root = node('root')
  for (const tag of ['ContentWrap', 'el-descriptions', 'el-descriptions-item', 'el-form', 'el-form-item', 'el-button', 'el-select', 'el-option', 'el-date-picker', 'el-input', 'el-alert']) {
    app.component(tag, vue.defineComponent({ inheritAttrs: false, setup: (_, ctx) => () => vue.h(tag, ctx.attrs, ctx.slots.default?.()) }))
  }
  app.component('el-table', { render: () => null })
  app.component('el-table-column', { render: () => null })
  app.directive('loading', {})
  app.mount(root)
  const host = app._instance.setupState
  const nodes = (type, n = root) => [...(n.type === type ? [n] : []), ...n.children.flatMap(c => nodes(type, c))]
  const text = (n = root) => n.text + n.children.map(text).join(' ')
  return { app, host, calls, nodes, renewal, text }
}

test('task deadline reads Beijing time while the delegation picker identifies its local zone', async () => {
  const originalZone = process.env.TZ
  process.env.TZ = 'America/New_York'
  const c = mount(1794970712963)
  try {
    await drain()
    const deadline = c.nodes('el-descriptions-item').find(n => n.props.label === '办理期限（北京时间）')
    assert.ok(deadline)
    assert.equal(c.text(deadline).trim(), '2026-11-18 10:58:32')
    c.host.delegateVisible = true; await drain()
    assert.ok(c.text().includes('按浏览器时区 America/New_York 选择'))
    assert.equal(c.host.assignment.dueAt.getTime(), 1794970712963)
  } finally {
    c.app.unmount()
    if (originalZone === undefined) delete process.env.TZ; else process.env.TZ = originalZone
  }
})

test('actual task SFC loads epoch deadline as Date and sends selected deadline as epoch', async () => {
  const initial = Date.UTC(2026, 10, 30, 14, 33, 46), next = Date.UTC(2026, 10, 20, 10)
  const c = mount(initial)
  try {
    await drain()
    assert.ok(c.host.assignment.dueAt instanceof Date)
    assert.equal(c.host.assignment.dueAt.getTime(), initial)
    c.host.delegateVisible = true; await drain()
    assert.equal(c.nodes('el-date-picker')[0].props.modelValue.getTime(), initial)
    assert.equal(c.nodes('el-date-picker')[0].props['value-format'], undefined)
    c.host.assignment.dueAt = new Date(next); c.host.assignment.reason = ' task authority '
    await c.host.delegate()
    assert.deepEqual(c.calls, [{ id: '3', payload: { assignedUserId: '910524', dueAt: next, reason: 'task authority' } }])
    assert.equal(c.host.assignment.dueAt.getTime(), initial)
  } finally { c.app.unmount() }
})

const oldCertificate = { certificateId: '8', versionId: '7', rowVersion: 2, certificateNo: 'E2E-WORD1030-DOM-EXP001', firstObtainedDate: 1693497600000, effectiveDate: 1696176000000, expiryDate: 1759248000000 }
test('actual task API and SFC pass Shanghai calendar dates to the renewal dialog regardless of browser timezone', async () => {
  const c = mount(Date.UTC(2026, 11, 1), oldCertificate, { taskKind: 'RENEWAL' })
  try {
    await drain()
    assert.equal(c.host.error, '')
    c.host.handle(); await drain()
    assert.equal(c.renewal.visible, true)
    assert.equal(c.renewal.certificate.firstObtainedDate, '2023-09-01')
    assert.equal(c.renewal.certificate.effectiveDate, '2023-10-02')
    assert.equal(c.renewal.certificate.expiryDate, '2025-10-01')
    assert.equal(c.renewal.certificate.versionId, '7')
    assert.equal(c.renewal.certificate.rowVersion, 2)
    assert.equal(oldCertificate.effectiveDate, 1696176000000)
  } finally { c.app.unmount() }
})

test('nullable first-obtained date stays absent without fabricating a date', async () => {
  const c = mount(Date.now(), { ...oldCertificate, firstObtainedDate: null }, { taskKind: 'RENEWAL' })
  try {
    await drain(); c.host.handle(); await drain()
    assert.equal(c.renewal.certificate.firstObtainedDate, undefined)
    assert.equal(c.renewal.certificate.effectiveDate, '2023-10-02')
  } finally { c.app.unmount() }
})

test('invalid or missing task certificate dates stay visible as load errors and cannot open renewal', async () => {
  for (const value of [null, undefined, NaN, Infinity, 1e20, '2023-10-02', [2023, 10, 2]]) {
    const c = mount(Date.now(), { ...oldCertificate, effectiveDate: value }, { taskKind: 'RENEWAL' })
    try {
      await drain(); c.host.handle(); await drain()
      assert.match(c.host.error, /生效日期缺失或不是有效的毫秒时间戳/)
      assert.equal(c.renewal.visible, false)
      assert.equal(c.host.detail, undefined)
      assert.equal(c.nodes('el-alert')[0].props.title, c.host.error)
    } finally { c.app.unmount() }
  }
})

test('missing deadline is explicit and cannot submit a fabricated current deadline', async () => {
  const c = mount(undefined)
  try {
    await drain(); c.host.assignment.reason = 'task authority'; await c.host.delegate()
    assert.equal(c.calls.length, 0)
    assert.equal(c.host.error, '请填写办理人、授权截止时间和说明')
  } finally { c.app.unmount() }
})
