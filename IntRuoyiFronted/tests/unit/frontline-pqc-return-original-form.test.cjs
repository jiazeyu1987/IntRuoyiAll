const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const filename = path.resolve(__dirname, '../../src/api/mes/pro/feedback/pqcReturnedSubmission.ts')
const source = ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
function harness(rows, detail) {
  const calls = []
  const module = { exports: {} }
  new Function('require', 'module', 'exports', source)(() => ({
    listOwnReturns: async type => { assert.equal(type, 'PQC'); return rows },
    getOwnReturnDetail: async () => detail,
    resubmitOwnPqc: async (payload, options) => { calls.push({payload, options}); return {eventId: payload.correction.eventId, revisionId: 99} }
  }), module, module.exports)
  return {...module.exports, calls}
}
const row = {eventId: 101, activeOrderId: 1, rejectedReviewId: 31, expectedRevisionId: 2, returnTaskId: '123', rejectionReason: '数值不正确'}
const detail = {row, pqc: {actualInspectionQuantity: 2, scrapQuantity: 0, items: [{itemCode: 'A', sampleValues: ['2', '3']}]}}
const process = () => ({pqcTaskOptions: [
  {pqcTaskId: 11, submittedEventId: 101, taskStatus: 'SUBMITTED'},
  {pqcTaskId: 12, submittedEventId: 102, taskStatus: 'SUBMITTED'},
  {pqcTaskId: 13, taskStatus: 'CONFIRMED'}
], taskSummary: {totalCount: 3}})
test('only the original owner return becomes writable; other submissions stay locked', async () => {
  const h = harness([row, {...row, activeOrderId: 2, eventId: 999}], detail)
  const p = process()
  await h.restoreReturnedPqcProcesses([p], 1)
  assert.equal(p.pqcTaskOptions[0].taskStatus, 'PENDING')
  assert.deepEqual(p.pqcTaskOptions[0].returnedSubmission.pqc.items[0].sampleValues, ['2', '3'])
  assert.equal(p.pqcTaskOptions[1].taskStatus, 'SUBMITTED')
  assert.equal(p.pqcTaskOptions[2].taskStatus, 'CONFIRMED')
  assert.equal(p.taskSummary.pendingCount, 1)
})
test('stale rejection rounds and missing original tasks fail without opening another record', async () => {
  await assert.rejects(harness([row], {...detail, row: {...row, rejectedReviewId: 32}}).restoreReturnedPqcProcesses([process()], 1))
  await assert.rejects(harness([{...row, eventId: 999}], detail).restoreReturnedPqcProcesses([process()], 1))
})
test('normal submit carries changed values, current signature and original rejected version to resubmission', async () => {
  const h = harness([], detail)
  const itemResults = [{itemCode: 'A', sampleValues: ['4', '5', '6']}]
  const receipt = await h.submitReturnedPqcInspection(detail, {actualInspectionQuantity: 3, scrapQuantity: 1, itemResults, signaturePassword: 'test-pin'}, {isRequestContextCurrent: () => true})
  assert.equal(receipt.eventId, row.eventId)
  assert.equal(h.calls[0].payload.expectedRevisionId, 2)
  assert.equal(h.calls[0].payload.rejectedReviewId, 31)
  assert.deepEqual(h.calls[0].payload.correction.itemResults, itemResults)
  assert.equal(h.calls[0].payload.correction.actualInspectionQuantity, 3)
  assert.equal(h.calls[0].payload.correction.signaturePassword, 'test-pin')
  assert.equal(h.calls[0].payload.correction.eventId, 101)
  assert.equal(h.calls[0].payload.correction.changeReason, 'PQC组长退回后重新签名提交')
})

test('unchanged return resubmits original values with a fresh signature and exact rejected round', async () => {
  const h = harness([], detail)
  await h.submitReturnedPqcInspection(detail, {
    actualInspectionQuantity: detail.pqc.actualInspectionQuantity,
    scrapQuantity: detail.pqc.scrapQuantity,
    itemResults: detail.pqc.items.map(({itemCode, sampleValues}) => ({itemCode, sampleValues: [...sampleValues]})),
    signaturePassword: 'fresh-test-pin'
  })
  const submitted = h.calls[0].payload
  assert.equal(submitted.rejectedReviewId, row.rejectedReviewId)
  assert.equal(submitted.expectedRevisionId, row.expectedRevisionId)
  assert.equal(submitted.correction.eventId, row.eventId)
  assert.equal(submitted.correction.actualInspectionQuantity, 2)
  assert.equal(submitted.correction.scrapQuantity, 0)
  assert.deepEqual(submitted.correction.itemResults, [{itemCode: 'A', sampleValues: ['2', '3']}])
  assert.equal(submitted.correction.signaturePassword, 'fresh-test-pin')
})

const { parse } = require('vue/compiler-sfc')
const panel = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const script = parse(panel).descriptor.scriptSetup.content
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
function extract(names) {
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.every(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length)
  return ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n') + `\nreturn {${names.join(',')}}`, {compilerOptions: {target: ts.ScriptTarget.ES2022}}).outputText
}
test('original form hydrates every returned sample and quantity without replacing edits', () => {
  const option = {returnedSubmission: detail, plannedInspectionQuantity: 1}
  const values = {}, selections = {}
  const deps = {
    getPqcTaskDraft: () => ({}), getPqcPieceStateKeyForTask: key => key,
    pqcPieceValues: values, getPqcItemSelection: key => selections[key] ||= {}
  }
  const methods = new Function(...Object.keys(deps), extract(['createPqcTaskDraftState', 'hydrateReturnedPqcTask']))(...Object.values(deps))
  assert.deepEqual(methods.createPqcTaskDraftState(option), {inspectionQuantity: 2, scrapQuantity: 0, itemQuantities: {A: 2}})
  methods.hydrateReturnedPqcTask(option)
  assert.deepEqual(values.A, ['2', '3'])
  selections.A = { selectedEquipmentId: 24, selectedEquipmentNumber: 'FM-E-24' }
  values.A[0] = '4'
  methods.hydrateReturnedPqcTask(option)
  assert.deepEqual(values.A, ['4', '3'])
  assert.deepEqual(selections.A, { selectedEquipmentId: 24, selectedEquipmentNumber: 'FM-E-24' })
  assert.deepEqual(detail.pqc.items[0].sampleValues, ['2', '3'])
})
test('PQC uses its original editable screen while the separate return dialog remains production-only', () => {
  assert.match(panel, /FrontlineReturnCorrectionPanel v-if="!isPqcMode"/)
  assert.match(script, /submitReturnedPqcInspection\(returned, submitPayload, options\)/)
  assert.match(script, /if \(option.returnedSubmission\) hydrateReturnedPqcTask\(option\)/)
})

test('readonly and in-flight equipment changes cannot mutate selections', () => {
  const { ref } = require('vue')
  const pqcSelectedTaskReadonly = ref(true), payloadLoading = ref(false), pqcSubmitResultUncertain = ref(false)
  const h = new Function('pqcSelectedTaskReadonly', 'payloadLoading', 'pqcSubmitResultUncertain',
    extract(['updatePqcItemSelectedEquipment']))(pqcSelectedTaskReadonly, payloadLoading, pqcSubmitResultUncertain)
  assert.doesNotThrow(() => h.updatePqcItemSelectedEquipment('A', {}))
  pqcSelectedTaskReadonly.value = false
  payloadLoading.value = true
  assert.doesNotThrow(() => h.updatePqcItemSelectedEquipment('A', {}))
  payloadLoading.value = false
  pqcSubmitResultUncertain.value = true
  assert.doesNotThrow(() => h.updatePqcItemSelectedEquipment('A', {}))
})

test('returned tasks do not silently auto-select the first device', () => {
  const h = new Function(extract(['applyPqcItemEquipmentDefaults']))()
  assert.doesNotThrow(() => h.applyPqcItemEquipmentDefaults([{ key: 'A' }], { returnedSubmission: detail }))
})
