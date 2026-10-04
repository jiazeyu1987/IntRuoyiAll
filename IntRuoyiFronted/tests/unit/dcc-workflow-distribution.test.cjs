const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const folder = 'src/views/dcc/controlled-file/workflow/'

function moduleFrom(source, requireModule) {
  const result = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } })
  const context = { exports: {}, Error, require: requireModule }
  vm.runInNewContext(result.outputText, context)
  return context.exports
}
function mountDistribution({ save = async () => true, loadRecipients = async () => [{ id: '99', name: '接收人' }] } = {}) {
  const source = fs.readFileSync(folder + 'WorkflowDistributionPanel.vue', 'utf8')
  const { descriptor, errors } = parse(source)
  assert.equal(errors.length, 0)
  assert.equal(compileTemplate({ source: descriptor.template.content, filename: 'WorkflowDistributionPanel.vue', id: 'distribution' }).errors.length, 0)
  const actions = moduleFrom(fs.readFileSync(folder + 'workflow-actions.ts', 'utf8'))
  const panel = moduleFrom(compileScript(descriptor, { id: 'distribution' }).content, id => {
    if (id === 'vue') return vue
    if (id === './workflow-actions') return actions
    throw new Error('Unexpected component dependency: ' + id)
  }).default
  panel.render = () => vue.h('section')
  const props = vue.reactive({ fileId: '42', processInstanceId: 'round-42', versionNo: 'B/1',
    facts: { effectiveDate: '2026-10-10', controlledTime: '2026-10-01 08:00', status: 'CONTROLLED_PENDING_EFFECTIVE' },
    canDistribute: true, departments: [{ id: '51', name: '质量部' }, { id: '52', name: '生产部' }], save, loadRecipients })
  const saved = []
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }),
    createComment: text => ({ text }), insert: (child, parent) => parent.children.push(child), remove() {},
    setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text },
    parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp({ render: () => vue.h(panel, { ...props, onSaved: context => saved.push(context) }) })
  const root = app.mount({ children: [] })
  return { props, state: root.$.subTree.component.setupState, saved, app }
}
async function selectElectronic(panel) {
  panel.state.addScope()
  panel.state.rows[0].departmentId = '51'
  panel.state.rows[0].distributionMedium = 'PUBLIC_FOLDER'
  await panel.state.loadRowRecipients(panel.state.rows[0])
  panel.state.rows[0].recipientUserIds = ['99']
}
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

test('future file distribution requires explicit confirmation and retains preset date', async () => {
  const calls = []
  const panel = mountDistribution({ save: async (file, scopes) => { calls.push({ file, scopes }); return true } })
  try {
    await selectElectronic(panel)
    panel.state.prepareConfirmation()
    assert.equal(panel.state.confirmationVisible, true)
    assert.equal(calls.length, 0)
    panel.state.cancelConfirmation()
    assert.equal(panel.state.rows[0].recipientUserIds[0], '99')
    panel.state.prepareConfirmation()
    await panel.state.confirmDistribution()
    assert.equal(calls.length, 1)
    assert.equal(calls[0].file, '42')
    assert.equal(calls[0].scopes[0].departmentId, '51')
    assert.equal(calls[0].scopes[0].recipientUserIds[0], '99')
    assert.equal(panel.saved.length, 1)
    assert.equal(panel.state.completed, true)
    assert.equal(panel.props.facts.effectiveDate, '2026-10-10')
  } finally { panel.app.unmount() }
})
test('real save failure remains visible, preserves selection and never emits completion', async () => {
  const panel = mountDistribution({ save: async () => { throw new Error('下发保存失败') } })
  try {
    await selectElectronic(panel); panel.state.prepareConfirmation()
    await panel.state.confirmDistribution()
    assert.equal(panel.state.completed, false)
    assert.match(panel.state.error, /下发保存失败/)
    assert.equal(panel.saved.length, 0)
    assert.equal(panel.state.rows[0].recipientUserIds[0], '99')
  } finally { panel.app.unmount() }
})
test('changing selection after confirmation requires another confirmation before any write', async () => {
  let calls = 0
  const panel = mountDistribution({ save: async () => { calls++ } })
  try {
    await selectElectronic(panel); panel.state.prepareConfirmation()
    panel.state.rows[0].distributionMedium = 'PAPER'
    await panel.state.confirmDistribution()
    assert.equal(calls, 0)
    assert.equal(panel.saved.length, 0)
    assert.match(panel.state.error, /重新确认/)
  } finally { panel.app.unmount() }
})
test('old file response and repeated click never complete or overwrite a new file', async () => {
  const pending = deferred(); let calls = 0
  const panel = mountDistribution({ save: async () => { calls++; await pending.promise } })
  try {
    await selectElectronic(panel); panel.state.prepareConfirmation()
    const first = panel.state.confirmDistribution()
    await panel.state.confirmDistribution()
    assert.equal(calls, 1)
    panel.props.fileId = '43'; await vue.nextTick()
    pending.resolve(); await first
    assert.equal(panel.saved.length, 0)
    assert.equal(panel.state.completed, false)
    assert.equal(panel.state.rows.length, 0)
  } finally { panel.app.unmount() }
})
test('unavailable recipient, duplicate department and pending directory request block confirmation', async () => {
  const pending = deferred()
  const panel = mountDistribution({ loadRecipients: () => pending.promise })
  try {
    panel.state.addScope(); panel.state.rows[0].departmentId = '51'
    const loading = panel.state.loadRowRecipients(panel.state.rows[0])
    panel.state.prepareConfirmation(); assert.equal(panel.state.confirmationVisible, false)
    pending.resolve([{ id: '99', name: '接收人' }]); await loading
    panel.state.rows[0].recipientUserIds = ['100']
    panel.state.prepareConfirmation(); assert.equal(panel.state.confirmationVisible, false)
    panel.state.rows[0].recipientUserIds = ['99']
    panel.state.addScope(); panel.state.rows[1].departmentId = '51'
    panel.state.rows[1].distributionMedium = 'PAPER'
    panel.state.prepareConfirmation(); assert.equal(panel.state.confirmationVisible, false)
  } finally { panel.app.unmount() }
})
test('directory response for previous department does not overwrite the current selection', async () => {
  const old = deferred()
  const panel = mountDistribution({ loadRecipients: async (_file, dept) => dept === '51'
    ? old.promise : [{ id: '100', name: '生产接收人' }] })
  try {
    panel.state.addScope(); const row = panel.state.rows[0]; row.departmentId = '51'
    const first = panel.state.loadRowRecipients(row)
    row.departmentId = '52'; await panel.state.loadRowRecipients(row)
    row.recipientUserIds = ['100']
    old.resolve([{ id: '99', name: '旧部门人员' }]); await first
    assert.equal(row.options[0].id, '100')
    assert.equal(row.recipientUserIds[0], '100')
  } finally { panel.app.unmount() }
})
test('lost permission or obsolete status prevents preparing or confirming a write', async () => {
  let calls = 0
  const panel = mountDistribution({ save: async () => { calls++ } })
  try {
    await selectElectronic(panel); panel.state.prepareConfirmation()
    panel.props.canDistribute = false; await vue.nextTick()
    await panel.state.confirmDistribution(); assert.equal(calls, 0)
    panel.props.canDistribute = true; panel.props.facts.status = 'OBSOLETE'; await vue.nextTick()
    panel.state.prepareConfirmation(); assert.equal(panel.state.confirmationVisible, false)
  } finally { panel.app.unmount() }
})

test('API false response is not treated as a persisted distribution success', async () => {
  const panel = mountDistribution({ save: async () => false })
  try {
    await selectElectronic(panel); panel.state.prepareConfirmation()
    await panel.state.confirmDistribution()
    assert.equal(panel.state.completed, false)
    assert.equal(panel.saved.length, 0)
    assert.match(panel.state.error, /未确认|失败/)
  } finally { panel.app.unmount() }
})
