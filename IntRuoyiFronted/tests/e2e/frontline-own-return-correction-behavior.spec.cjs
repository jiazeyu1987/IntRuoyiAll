const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineReturnCorrectionPanel.vue')
const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
const row = { returnTaskId: '1900000000000000001', eventId: 176, activeOrderId: 413, rejectedReviewId: 91, expectedRevisionId: 0,
  workOrderCode: 'OWN-TEST', formName: '参数录入', rejectionReason: '参数需核对', rejectedAt: '2026-10-05' }
const production = { outputQuantity: 10, materialDetails: [], lossDetails: [],
  deviceParameterReadings: [{ deviceId: 663, parameterCode: 'COUNT', value: 2 }] }
function harness(mode = 'production', permissions = ['mes:pro-feedback:query', 'mes:pro-feedback:create']) {
  const ast = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const reads = [], writes = [], emits = [], allowed = new Set(permissions)
  let detail = { row, production }
  let save = async request => { writes.push(request); return { eventId: 176, revisionId: 701,
    changes: [{ fieldName: '次数', beforeValue: '2', afterValue: '3' }] } }
  const context = { ...vue, defineProps: () => ({ mode }), defineEmits: () => (...args) => emits.push(args),
    defineExpose() {}, checkPermi: requested => requested.some(p => allowed.has(p)),
    listOwnReturns: async type => { reads.push(['list', type]); return [row] },
    getOwnReturnDetail: async (request, type) => { reads.push(['detail', request, type]); return detail },
    resubmitOwnProduction: request => save(request), resubmitOwnPqc: request => save(request) }
  const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None } }).outputText
  const panel = Function(...Object.keys(context), `${code};return {open,openTask,choose,resubmit,clearSensitive,visible,rows,draft,reason,password,errorText,result,busy};`)(...Object.values(context))
  return { panel, reads, writes, emits, setDetail: value => { detail = value }, setSave: fn => { save = fn } }
}
test('actual dialog template compiles and both frontline screens offer the return entry', () => {
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: 'own-return' })
  assert.deepEqual(compiled.errors, [])
  const parent = parse(fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')).descriptor
  const walk = node => [node, ...(node.children || []).flatMap(walk)]
  const buttons = walk(parent.template.ast).filter(n => n.type === 1 && n.props.some(p => p.type === 6 && p.name === 'data-frontline-own-return-entry'))
  assert.equal(buttons.length, 2)
  for (const button of buttons) assert.ok(button.props.some(p => p.type === 7 && p.name === 'hasPermi'))
})
test('both production header actions retain readable 18px text after the actual stage scaling', () => {
  const parent = parse(fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')).descriptor
  const ast = ts.createSourceFile('parent.ts', parent.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations])
    .find(d => d.name.getText(ast) === 'productionStageStyle')
  assert.ok(declaration)
  const scale = vue.ref(1)
  const actualStyle = Function('computed', 'productionViewportScale', 'PRODUCTION_CANVAS_WIDTH', 'PRODUCTION_CANVAS_HEIGHT',
    `return ${declaration.initializer.getText(ast)}`)(vue.computed, scale, 1920, 1080)
  assert.match(parent.styles[0].content, /\.frontline-production-stage \.frontline-return-actions > button\s*\{\s*font-size:\s*var\(--frontline-production-return-action-font-size\)/)
  for (const value of [1, 0.5, 0.7]) {
    scale.value = value
    assert.ok(Math.abs(parseFloat(actualStyle.value['--frontline-production-return-action-font-size']) * value - 18) < 0.001)
  }
})
test('unauthorized handler cannot load return tasks or submit a correction', async () => {
  const h = harness('production', []); await h.panel.open(); await h.panel.resubmit()
  assert.deepEqual(h.reads, []); assert.deepEqual(h.writes, []); assert.equal(h.panel.visible.value, false)
  assert.match(h.panel.errorText.value, /权限/)
})
test('own production changes an actual parameter, retaining quantity, original event and exact rejected round', async () => {
  const h = harness(); await h.panel.open(); await h.panel.choose(row)
  h.panel.draft.value.production.deviceParameterReadings[0].value = 3
  h.panel.reason.value = '核对设备记录'; h.panel.password.value = 'test-only-signature'
  await h.panel.resubmit()
  assert.equal(h.writes.length, 1)
  assert.deepEqual(h.writes[0], { activeOrderId: 413, rejectedReviewId: 91, expectedRevisionId: 0,
    correction: { eventId: 176, outputQuantity: 10, materialDetails: [], lossDetails: [],
      deviceParameterReadings: [{ deviceId: 663, parameterCode: 'COUNT', value: 3 }],
      changeReason: '核对设备记录', signaturePassword: 'test-only-signature' } })
  assert.equal(h.panel.password.value, ''); assert.equal(h.panel.result.value.revisionId, 701)
  assert.equal(h.emits[0][0], 'corrected'); assert.equal(h.writes[0].correction.actorUserId, undefined)
})
test('unchanged form is blocked even with a new reason and password', async () => {
  const h = harness(); await h.panel.choose(row)
  h.panel.reason.value = '只改理由'; h.panel.password.value = 'test-only-signature'; await h.panel.resubmit()
  assert.equal(h.writes.length, 0); assert.match(h.panel.errorText.value, /业务值/)
})
test('stale detail cannot replace the selected cycle and submit another round', async () => {
  const h = harness(); h.setDetail({ row: { ...row, activeOrderId: 412 }, production })
  await h.panel.choose(row); assert.equal(h.panel.draft.value, undefined); assert.match(h.panel.errorText.value, /轮次/)
})
test('PQC sends legal measurement changes with frozen sample quantity and equipment', async () => {
  const h = harness('pqc'); h.setDetail({ row, pqc: { actualInspectionQuantity: 2, scrapQuantity: 0,
    items: [{ itemCode: 'QA-1', itemName: '尺寸', resultType: 'NUMERIC', lowerLimit: 1, upperLimit: 5,
      selectedEquipmentId: 663, selectedEquipmentNumber: 'EQ-1', sampleValues: ['2', '2'] }] } })
  await h.panel.choose(row); h.panel.draft.value.pqc.items[0].sampleValues[0] = '3'
  h.panel.reason.value = '核对量测'; h.panel.password.value = 'test-only-signature'; await h.panel.resubmit()
  assert.equal(h.writes[0].correction.actualInspectionQuantity, 2)
  assert.deepEqual(h.writes[0].correction.itemResults, [{ itemCode: 'QA-1', selectedEquipmentId: 663,
    selectedEquipmentNumber: 'EQ-1', sampleValues: ['3', '2'] }])
  assert.equal(h.writes[0].correction.afterPayload, undefined)
})
test('server rejection preserves the form, shows the failure and clears signature password', async () => {
  const h = harness(); await h.panel.choose(row); h.panel.draft.value.production.deviceParameterReadings[0].value = 3
  h.panel.reason.value = '核对记录'; h.panel.password.value = 'test-only-signature'
  h.setSave(async () => { throw Error('已非当前退回轮次') }); await h.panel.resubmit()
  assert.match(h.panel.errorText.value, /已非当前退回轮次/); assert.equal(h.panel.password.value, '')
  assert.equal(h.panel.result.value, undefined); assert.equal(h.panel.draft.value.production.deviceParameterReadings[0].value, 3)
})
test('pending correction rejects duplicate clicks and keeps the original request immutable', async () => {
  const h = harness(); await h.panel.choose(row); h.panel.draft.value.production.deviceParameterReadings[0].value = 3
  h.panel.reason.value = '核对记录'; h.panel.password.value = 'test-only-signature'
  let resolve; h.setSave(request => { h.writes.push(request); return new Promise(r => { resolve = r }) })
  const pending = h.panel.resubmit(); await h.panel.resubmit(); assert.equal(h.writes.length, 1)
  h.panel.draft.value.production.deviceParameterReadings[0].value = 4
  assert.equal(h.writes[0].correction.deviceParameterReadings[0].value, 3)
  resolve({ eventId: 176, revisionId: 701, changes: [{ fieldName: '次数', beforeValue: '2', afterValue: '3' }] })
  await pending; assert.equal(h.panel.busy.value, false); assert.equal(h.panel.password.value, '')
})
test('all dialog close paths clear the signature password through the actual closed handler', () => {
  const h = harness(); h.panel.password.value = 'test-only-signature'
  const dialog = descriptor.template.ast.children.find(n => n.type === 1 && n.tag === 'el-dialog')
  assert.ok(dialog.props.some(p => p.type === 7 && p.name === 'on' && p.arg.content === 'closed' && p.exp.content === 'clearSensitive'))
  h.panel.clearSensitive(); assert.equal(h.panel.password.value, '')
})
const returnQuery = { returnTaskId: row.returnTaskId, handoffTaskId: row.returnTaskId,
  activeOrderId: '413', eventId: '176', rejectedReviewId: '91', roundId: '91', handoffType: 'PRODUCTION_RETURN' }
test('notification opens only the exact own pending task without rounding a Snowflake ID', async () => {
  const h = harness(); await h.panel.openTask(returnQuery)
  assert.equal(h.panel.draft.value.row.returnTaskId, '1900000000000000001')
  assert.deepEqual(h.reads.map(r => r[0]), ['list', 'detail']); assert.equal(h.writes.length, 0)
})
test('old, foreign or mismatched notification context cannot guess an event detail', async () => {
  for (const query of [
    { ...returnQuery, returnTaskId: '1900000000000000002', handoffTaskId: '1900000000000000002' },
    { ...returnQuery, activeOrderId: '412' },
    { ...returnQuery, rejectedReviewId: '90', roundId: '90' },
    { ...returnQuery, eventId: '177' },
    { ...returnQuery, handoffTaskId: '1900000000000000002' },
    { ...returnQuery, handoffType: 'PQC_RETURN' },
    { ...returnQuery, returnTaskId: ['1900000000000000001', '1900000000000000002'] },
    { ...returnQuery, returnTaskId: Number(row.returnTaskId) }
  ]) {
    const h = harness(); await h.panel.openTask(query)
    assert.equal(h.panel.draft.value, undefined); assert.match(h.panel.errorText.value, /失效|无效/)
    assert.deepEqual(h.reads.map(r => r[0]), ['list']); assert.equal(h.writes.length, 0)
  }
})
test('a replaced pending task cannot overwrite the selected original task during detail read', async () => {
  const h = harness(); h.setDetail({ row: { ...row, returnTaskId: '1900000000000000002' }, production })
  await h.panel.openTask(returnQuery)
  assert.equal(h.panel.draft.value, undefined); assert.match(h.panel.errorText.value, /轮次/)
  assert.equal(h.writes.length, 0)
})
