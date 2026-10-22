const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../../src')
const component = path.join(root, 'views/mes/pro/processpool/components/ActiveOrderCorrectionHistoryPanel.vue')
const descriptor = parse(fs.readFileSync(component, 'utf8')).descriptor
function actualModule(relative, dependencies) {
  const code = ts.transpileModule(fs.readFileSync(path.join(root, relative), 'utf8'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  const exports = {}
  Function('require', 'exports', code)(name => {
    assert.ok(Object.hasOwn(dependencies, name), `unexpected dependency ${name}`)
    return dependencies[name]
  }, exports)
  return exports
}
const { createActiveOrderCorrectionHistory } = actualModule(
  'views/mes/pro/processpool/components/activeOrderCorrectionHistory.ts', { vue })
const context = (activeOrderId = '413') => ({ scope: 'BATCH', identity: { activeOrderId }, activeOrderId })
const correction = () => ({ revisionId: '1900000000000000001', eventId: '176', eventType: 'PRODUCTION_SUBMIT',
  processName: '生产工序', revisedAt: '2026-10-05T02:00:00', reason: '核对记录', signerName: '冻结本人',
  signatureId: '1900000000000000002', signedAt: '2026-10-05T01:59:59', verificationStatus: 'VALID',
  changes: [{ fieldCode: 'COUNT', fieldName: '设备次数', beforeValue: '2', afterValue: '3' }] })
const timeline = (activeOrderId = '413') => ({ activeOrderId, corrections: [correction()] })
const walk = node => [node, ...(node.children || []).flatMap(walk)]
test('actual readonly template preserves before/after and routes exact correction signature to the existing viewer', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: component, id: 'corrections' }).errors, [])
  const nodes = walk(descriptor.template.ast).filter(n => n.type === 1)
  assert.deepEqual(nodes.filter(n => n.tag === 'el-table-column').map(n => n.props.find(p => p.name === 'label').value.content),
    ['修改字段', '本次修改前', '本次修改后'])
  assert.equal(nodes.filter(n => ['el-input', 'el-select', 'el-input-number', 'el-form'].includes(n.tag)).length, 0)
  const button = nodes.find(n => n.props.some(p => p.name === 'data-active-order-correction-signature'))
  const click = button.props.find(p => p.type === 7 && p.name === 'on' && p.arg.content === 'click')
  const emitted = []
  Function('$emit', 'row', click.exp.content)((...args) => emitted.push(args), correction())
  assert.deepEqual(emitted, [['signature', '1900000000000000002']])
  const parent = parse(fs.readFileSync(path.join(root, 'views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')).descriptor
  const entries = walk(parent.template.ast).filter(n => n.type === 1 && n.tag === 'ActiveOrderCorrectionHistoryPanel')
  assert.equal(entries.length, 1)
  assert.ok(entries[0].props.some(p => p.type === 7 && p.name === 'bind' && p.arg.content === 'context' && p.exp.content === 'signatureEvidenceContext'))
  assert.ok(entries[0].props.some(p => p.type === 7 && p.name === 'on' && p.arg.content === 'signature' && p.exp.content === 'signatureEvidenceViewer.open'))
})
test('formal production and PQC history load frozen signer and changed business values without writing', async () => {
  for (const eventType of ['PRODUCTION_SUBMIT', 'PQC_INSPECTION']) {
    const source = context(), response = timeline(); response.corrections[0].eventType = eventType
    const reads = [], h = createActiveOrderCorrectionHistory(() => source, async input => { reads.push(input); return response })
    await h.open()
    assert.deepEqual(reads, [source]); assert.equal(h.state.timeline.corrections[0].signerName, '冻结本人')
    assert.equal(h.state.timeline.corrections[0].changes[0].beforeValue, '2')
    assert.equal(h.state.timeline.corrections[0].changes[0].afterValue, '3')
    assert.equal(h.state.error, ''); assert.equal(h.state.loading, false)
  }
})
test('older request cannot replace a later cycle response or its signature actions', async () => {
  let source = context(), finishA
  const h = createActiveOrderCorrectionHistory(() => source, input => input.activeOrderId === '413'
    ? new Promise(resolve => { finishA = resolve }) : Promise.resolve(timeline('414')))
  const a = h.open(); source = context('414'); await h.open(); finishA(timeline()); await a
  assert.equal(h.state.timeline.activeOrderId, '414'); assert.equal(h.state.error, '')
})
test('closing while loading invalidates response and clears sensitive association', async () => {
  let finish
  const h = createActiveOrderCorrectionHistory(() => context(), () => new Promise(resolve => { finish = resolve }))
  const pending = h.open(); h.close(); finish(timeline()); await pending
  assert.equal(h.state.timeline, undefined); assert.equal(h.state.visible, false); assert.equal(h.state.loading, false)
})
test('response integrity failures stay errors and never display an empty successful history', async () => {
  const bad = [response => { response.activeOrderId = '412' }, response => { response.corrections[0].verificationStatus = 'INVALID' },
    response => { response.corrections[0].signatureId = 1900000000000000002 }, response => { response.corrections[0].revisionId = '9223372036854775808' },
    response => { response.corrections[0].signerName = '' }, response => { response.corrections[0].changes[0].afterValue = '2' },
    response => { response.corrections.push(correction()) }, response => { response.corrections[0].changes = [] }]
  for (const breakResponse of bad) {
    const response = timeline(); breakResponse(response)
    const h = createActiveOrderCorrectionHistory(() => context(), async () => response); await h.open()
    assert.ok(h.state.error); assert.equal(h.state.timeline, undefined); assert.equal(h.state.loading, false)
  }
  const failed = createActiveOrderCorrectionHistory(() => context(), async () => { throw Error('正式证据缺失') }); await failed.open()
  assert.equal(failed.state.error, '正式证据缺失'); assert.equal(failed.state.timeline, undefined)
  const empty = createActiveOrderCorrectionHistory(() => context(), async () => ({ activeOrderId: '413', corrections: [] })); await empty.open()
  assert.equal(empty.state.error, ''); assert.equal(empty.state.timeline.corrections.length, 0)
})
test('actual component context watcher clears rows when a detail changes', async () => {
  const props = vue.reactive({ context: context() })
  const ast = ts.createSourceFile('corrections.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const dependencies = { ...vue, defineProps: () => props, defineEmits() {}, onBeforeUnmount() {},
    formatDateTimeValue: value => value, createActiveOrderCorrectionHistory, getActiveOrderCorrections: async () => timeline() }
  const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  const history = Function(...Object.keys(dependencies), `${code};return history;`)(...Object.values(dependencies))
  await history.open(); assert.ok(history.state.timeline)
  props.context = context('414'); await vue.nextTick()
  assert.equal(history.state.timeline, undefined); assert.equal(history.state.visible, false)
})
test('three scopes use only their exact existing GET identity and preserve string Snowflake IDs', async () => {
  const calls = [], api = actualModule('api/mes/pro/edhr/activeOrderCorrection.ts', { '@/config/axios': {
    default: { get: async input => { calls.push(input); return timeline() } } } })
  const id = '1900000000000000001'
  await api.getActiveOrderCorrections('TEAM', id); await api.getActiveOrderCorrections('PQC', id)
  await api.getActiveOrderCorrections('BATCH', { batchExecutionId: id })
  await api.getActiveOrderCorrections('BATCH', { activeOrderId: id })
  assert.deepEqual(calls.map(c => c.params), [{ activeOrderId: id }, { applicationId: id }, { batchExecutionId: id }, { activeOrderId: id }])
  assert.ok(calls.every(c => c.url.endsWith('/corrections')))
  for (const [scope, identity] of [['BATCH', { activeOrderId: 413, batchExecutionId: 900 }], ['BATCH', {}],
    ['TEAM', 1900000000000000001], ['TEAM', '9223372036854775808'], ['PQC', 0], ['UNKNOWN', id]]) {
    assert.throws(() => api.buildActiveOrderCorrectionRequest(scope, identity))
  }
  assert.equal(calls.length, 4)
})
