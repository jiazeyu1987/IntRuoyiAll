const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript'), vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = require('node:path').resolve(__dirname, '../..')
const read = file => fs.readFileSync(root + '/src/' + file, 'utf8')
function api() {
  const c = { exports: {}, Error, BigInt, Set, Date, JSON, require: id => {
    if (id === '@/config/axios') return { default: {} }
    if (id.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error(id)
  } }
  vm.runInNewContext(ts.transpileModule(read('api/dcc/controlledFile/applicationRead.ts'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c.exports
}
const context = () => ({ controlledFileId: '9223372036854775000', taskId: 'confirm-task', processInstanceId: 'R', attributeRound: 2,
  processDefinitionId: 'dcc-controlled-file-upload:6:exact', definitionDigest: 'a'.repeat(64), versionNo: 'A/1',
  effectiveMode: 'DELAYED', stageCode: 'DELAYED_EFFECTIVE_CONFIRM', canConfirm: true, timeZone: 'Asia/Shanghai', serverTime: '2026-10-14 12:30:00.123' })
const result = req => ({ controlledFileId: context().controlledFileId, taskId: req.taskId, processInstanceId: req.processInstanceId,
  attributeRound: req.attributeRound, selectedEffectiveTime: req.selectedEffectiveTime, signatureId: '700', evidenceHash: 'b'.repeat(64),
  signedAt: '2026-10-14 12:30:00.123456789', status: 'CONFIRMED' })
test('second precision input is compared against untruncated Shanghai signature time', () => {
  const a = api(); assert.equal(typeof a.buildDelayedEffectiveConfirmationCommand, 'function')
  assert.throws(() => a.buildDelayedEffectiveConfirmationCommand(context(), '2026-10-14 12:30:00', 'p', '真实原因', 'K'))
  const req = a.buildDelayedEffectiveConfirmationCommand(context(), '2026-10-14 12:30:01', 'p', '真实原因', 'K')
  assert.equal(req.selectedEffectiveTime, '2026-10-14 12:30:01'); assert.equal(req.expectedProcessDefinitionId, context().processDefinitionId)
  assert.equal(a.validateDelayedEffectiveConfirmationResult(context(), req, result(req)).signedAt, '2026-10-14 12:30:00.123456789')
  assert.throws(() => a.validateDelayedEffectiveConfirmationResult(context(), req, { ...result(req), signedAt: '2026-10-14 12:30:01.000000001' }))
})
test('empty invalid or offset selection, password, reason and denied task never form a command', () => {
  const a = api()
  for (const value of ['', '2026-02-29 10:00:00', '2026-10-14', '2026-10-14T12:31:00Z', '2026-10-14 12:31:00.1'])
    assert.throws(() => a.buildDelayedEffectiveConfirmationCommand(context(), value, 'p', '原因', 'K'))
  for (const c of [{ ...context(), canConfirm: false }, { ...context(), timeZone: 'UTC' }, { ...context(), stageCode: 'DOC_CONTROL_REVIEW' }])
    assert.throws(() => a.buildDelayedEffectiveConfirmationCommand(c, '2026-10-15 00:00:00', 'p', '原因', 'K'))
  for (const [p, r] of [['', '原因'], ['p', '  ']]) assert.throws(() => a.buildDelayedEffectiveConfirmationCommand(context(), '2026-10-15 00:00:00', p, r, 'K'))
})
test('foreign task, round, signature or changed selected time cannot claim confirmed', () => {
  const a = api(), req = a.buildDelayedEffectiveConfirmationCommand(context(), '2026-10-15 00:00:00', 'p', '原因', 'K')
  for (const patch of [{ taskId: 'other' }, { processInstanceId: 'other' }, { attributeRound: 3 }, { signatureId: null },
    { selectedEffectiveTime: '2026-10-16 00:00:00' }, { evidenceHash: '' }, { status: 'APPROVED' }])
    assert.throws(() => a.validateDelayedEffectiveConfirmationResult(context(), req, { ...result(req), ...patch }))
})
function mount(save) {
  const filename = 'views/dcc/controlled-file/workflow/DelayedEffectiveConfirmationPanel.vue'
  const descriptor = parse(read(filename)).descriptor
  assert.equal(compileTemplate({ id: 'confirm', source: descriptor.template.content, filename }).errors.length, 0)
  const compiled = compileScript(descriptor, { id: 'confirm' })
  const c = { exports: {}, Error, JSON, require: id => id === 'vue' ? vue : id.endsWith('applicationRead') ? api() : { generateUUID: () => 'K' } }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const panel = c.exports.default; panel.render = () => vue.h('section')
  const props = vue.reactive({ context: context(), save }), events = []
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [] }), createText: text => ({ text }), createComment: text => ({ text }),
    insert: (c, p) => p.children.push(c), remove() {}, setText() {}, setElementText() {}, parentNode: () => null, nextSibling: () => null, patchProp() {} })
  const app = renderer.createApp({ render: () => vue.h(panel, { ...props, onSaved: value => events.push(value) }) }), tree = app.mount({ children: [] })
  return { state: tree.$.subTree.component.setupState, props, events, app }
}
test('actual Vue handler sends exact future choice and signature inputs only once after confirmed result', async () => {
  const requests = [], p = mount(async req => { requests.push(req); return result(req) })
  try { p.state.selectedEffectiveTime = '2026-10-15 00:00:00'; p.state.password = 'credential'; p.state.reason = '衔接关系已核对'
    await p.state.confirmEffective(); assert.equal(requests.length, 1); assert.equal(requests[0].processInstanceId, 'R'); assert.equal(requests[0].attributeRound, 2)
    assert.equal(p.events.length, 1); assert.equal(p.state.password, ''); await p.state.confirmEffective(); assert.equal(requests.length, 1)
  } finally { p.app.unmount() }
})
test('actual Vue errors remain visible with selected input retained and no saved event', async () => {
  const p = mount(async () => { throw new Error('真实签名授权拒绝') })
  try { p.state.selectedEffectiveTime = '2026-10-15 00:00:00'; p.state.password = 'credential'; p.state.reason = '真实意见'
    await p.state.confirmEffective(); assert.equal(p.state.error, '真实签名授权拒绝'); assert.equal(p.state.selectedEffectiveTime, '2026-10-15 00:00:00'); assert.equal(p.events.length, 0)
  } finally { p.app.unmount() }
})
test('late result after exact task changes cannot confirm the new task', async () => {
  let complete, sent; const p = mount(req => { sent = req; return new Promise(resolve => { complete = resolve }) })
  try { p.state.selectedEffectiveTime = '2026-10-15 00:00:00'; p.state.password = 'credential'; p.state.reason = '真实意见'
    const pending = p.state.confirmEffective(); p.props.context = { ...context(), taskId: 'new' }; await vue.nextTick()
    assert.equal(p.state.password, ''); assert.equal(p.state.selectedEffectiveTime, '')
    complete(result(sent)); await pending; assert.equal(p.events.length, 0); assert.equal(p.state.saved, false)
  } finally { p.app.unmount() }
})
