const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const drain = async () => { for (let i = 0; i < 30; i++) await Promise.resolve(); await vue.nextTick() }
const defaults = () => ({
  id: 1, enabled: true, dailyRunTime: '09:00', timezone: 'Asia/Shanghai',
  thresholdDaysJson: '[30,8,2,1]', rowVersion: 1,
  thresholdRecipientUserIds: { T_30: [], T_8: [], T_2: [], T_1: [] }
})
function mount(path, api) {
  const descriptor = parse(fs.readFileSync('src/views/dcc/registration-certificate/config/' + path, 'utf8')).descriptor
  const script = compileScript(descriptor, { id: path })
  assert.deepEqual(compileTemplate({ id: path, source: descriptor.template.content,
    compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  const exports = {}, messages = []
  const globals = { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, onMounted: vue.onMounted,
    useMessage: () => ({ error: text => messages.push(text), success: text => messages.push(text) }) }
  const js = ts.transpileModule(script.content, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText
  new Function('exports', 'require', ...Object.keys(globals), js)(exports, id => {
    if (id === 'vue') return vue
    if (id === '@/api/dcc/registrationCertificate/reminderConfig') return api
    if (id === '@/utils/businessConfigAccess') return { hasExplicitConfigPermission: () => true }
    if (id === '@/api/system/user') return { getSimpleUserList: async () => [] }
    if (id.endsWith('.vue')) return { default: {} }
    throw Error('unexpected import ' + id)
  }, ...Object.values(globals))
  exports.default.render = () => vue.h('section')
  const renderer = vue.createRenderer({ createElement: () => ({ children: [] }), createComment: () => ({ children: [] }),
    createText: () => ({ children: [] }), insert: (c, p) => p.children.push(c), remove() {}, setText() {},
    setElementText() {}, patchProp() {}, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp({ render: () => vue.h(exports.default, { canUpdate: true }) })
  const host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, app, messages }
}

test('actual schedule accepts the explicit default DTO empty lists as unconfigured without writing anything', async () => {
  let writes = 0
  const c = mount('components/ReminderScheduleSetting.vue', {
    getRegistrationCertificateReminderConfig: async () => defaults(),
    updateRegistrationCertificateReminderConfig: async () => { writes++ }
  })
  try {
    await drain()
    assert.equal(c.state.loaded, true)
    assert.equal(c.state.loadError, '')
    assert.equal(c.state.recipientsUnconfigured, true)
    assert.deepEqual(c.state.form.thresholdRecipientUserIds, defaults().thresholdRecipientUserIds)
    assert.equal(writes, 0)
  } finally { c.app.unmount() }
})

test('actual settings dialog allows explicitly choosing recipients after default initialization and saves only those choices', async () => {
  const writes = []
  const c = mount('ReminderConfigDialog.vue', {
    getRegistrationCertificateReminderConfig: async () => defaults(),
    updateRegistrationCertificateReminderConfig: async data => { writes.push(data); return { rowVersion: 2 } }
  })
  try {
    await c.state.open()
    assert.equal(c.state.loaded, true)
    assert.equal(c.state.recipientsUnconfigured, true)
    for (const key of ['T_30', 'T_8', 'T_2', 'T_1', 'CHANGE_CONFIRMATION']) {
      assert.deepEqual(c.state.formData.thresholdRecipientUserIds[key], [])
      c.state.formData.thresholdRecipientUserIds[key] = [101]
    }
    c.state.formRef = { validate: async () => true }
    await c.state.submit()
    assert.equal(writes.length, 1)
    assert.equal(writes[0].expectedRowVersion, 1)
    assert.deepEqual(writes[0].thresholdRecipientUserIds,
      { T_30: [101], T_8: [101], T_2: [101], T_1: [101], CHANGE_CONFIRMATION: [101] })
    assert.equal(c.state.dialogVisible, false)
  } finally { c.app.unmount() }
})

test('both actual configuration UIs reject partial and nonarray recipient shapes instead of treating them as defaults', async () => {
  for (const invalid of [{ T_30: [] }, { T_30: true, T_8: [], T_2: [], T_1: [] }]) {
    let writes = 0
    const api = { getRegistrationCertificateReminderConfig: async () => ({ ...defaults(), thresholdRecipientUserIds: invalid }),
      updateRegistrationCertificateReminderConfig: async () => { writes++ } }
    const schedule = mount('components/ReminderScheduleSetting.vue', api)
    try {
      await drain()
      assert.equal(schedule.state.loaded, false)
      assert.match(schedule.state.loadError, /收件人必须为数组/)
    } finally { schedule.app.unmount() }
    const dialog = mount('ReminderConfigDialog.vue', api)
    try {
      await assert.rejects(dialog.state.open(), /收件人必须为数组/)
      assert.equal(dialog.state.loaded, false)
      assert.equal(dialog.state.dialogVisible, false)
      assert.equal(writes, 0)
    } finally { dialog.app.unmount() }
  }
})
