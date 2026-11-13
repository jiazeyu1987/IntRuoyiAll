const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { test } = require('node:test')
const ts = require('typescript')
const { ref } = require('vue')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const panel = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')).descriptor
const ast = ts.createSourceFile('panel.ts', panel.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['hasPqcEquipmentOptions', 'isPqcCustomEquipmentSelection', 'returnToPqcEquipmentOptions',
  'updatePqcItemSelectedEquipment', 'assertPqcItemEquipmentSelection', 'buildPqcItemResultsPayload']
const statements = ast.statements.filter(statement =>
  ts.isFunctionDeclaration(statement) ? names.includes(statement.name.text) :
    ts.isVariableStatement(statement) && statement.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(statements.length, names.length)
const code = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') + `\nreturn {${names.join(',')}}`,
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness() {
  const selection = { selectedEquipmentId: undefined, selectedEquipmentNumber: '原手填设备' }
  const item = { key: 'A', label: '尺寸', equipmentOptions: [
    { equipmentId: 24, equipmentNumber: 'FM-E-24' },
    { equipmentId: 25, equipmentNumber: 'FM-E-25' }
  ] }
  const returned = { row: { activeOrderId: 1, eventId: 101, rejectedReviewId: 31, expectedRevisionId: 2 },
    pqc: { actualInspectionQuantity: 2, scrapQuantity: 0, items: [
      { itemCode: 'A', selectedEquipmentNumber: '原手填设备', sampleValues: ['2', '3'] }
    ] } }
  const task = { returnedSubmission: returned, inspectionItems: [item] }
  let touched = 0
  const deps = { pqcSelectedTaskReadonly: ref(false), payloadLoading: ref(false), pqcSubmitResultUncertain: ref(false),
    activePqcTaskOption: ref(task), pqcTaskInspectionItemMap: ref({ A: item }),
    FRONTLINE_OTHER_OPTION_VALUE: '__OTHER__', getPqcItemSelection: () => selection,
    markPqcTaskTouched: () => { touched++ }, mapPqcInspectionItem: value => value,
    getPqcExactPieceValuesForTask: () => ['2', '3'] }
  return { ...deps, task, returned, selection, item, touched: () => touched,
    ...new Function(...Object.keys(deps), code)(...Object.values(deps)) }
}

function findElement(node, marker) {
  if (node.type === 1 && node.props.some(prop => prop.type === 6 && prop.name === marker)) return node
  for (const child of node.children || []) {
    const found = findElement(child, marker)
    if (found) return found
  }
}
const button = findElement(panel.template.ast, 'data-pqc-custom-equipment-return')
assert.ok(button)
const disabled = button.props.find(prop => prop.type === 7 && prop.name === 'bind' && prop.arg.content === 'disabled').exp.content
const buttonDisabled = new Function('pqcSelectedTaskReadonly', 'payloadLoading', 'pqcSubmitResultUncertain',
  'activePqcTaskOption', `return ${disabled}`)

test('owner returned custom equipment can return to configured options and sign the changed equipment', async () => {
  const h = harness()
  assert.equal(buttonDisabled(false, false, false, h.task), false)
  assert.equal(h.isPqcCustomEquipmentSelection('A'), true)
  h.returnToPqcEquipmentOptions('A')
  assert.equal(h.isPqcCustomEquipmentSelection('A'), false)
  assert.deepEqual(h.selection, { selectedEquipmentId: undefined, selectedEquipmentNumber: undefined })
  h.updatePqcItemSelectedEquipment('A', { target: { value: '25' } })
  const itemResults = h.buildPqcItemResultsPayload(h.task)
  assert.deepEqual(itemResults, [{ itemCode: 'A', selectedEquipmentId: 25,
    selectedEquipmentNumber: 'FM-E-25', sampleValues: ['2', '3'] }])

  const source = ts.transpileModule(fs.readFileSync(path.join(root, 'src/api/mes/pro/feedback/pqcReturnedSubmission.ts'), 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  let request
  const module = { exports: {} }
  new Function('require', 'module', 'exports', source)(() => ({ resubmitOwnPqc: async payload => { request = payload } }), module, module.exports)
  await module.exports.submitReturnedPqcInspection(h.returned, {
    actualInspectionQuantity: 2, scrapQuantity: 0, itemResults, signaturePassword: 'fresh-test-pin'
  })
  assert.equal(request.activeOrderId, 1)
  assert.equal(request.rejectedReviewId, 31)
  assert.equal(request.expectedRevisionId, 2)
  assert.equal(request.correction.eventId, 101)
  assert.equal(request.correction.signaturePassword, 'fresh-test-pin')
  assert.deepEqual(request.correction.itemResults, itemResults)
  assert.equal(h.returned.pqc.items[0].selectedEquipmentNumber, '原手填设备')
})

for (const state of ['pqcSelectedTaskReadonly', 'payloadLoading', 'pqcSubmitResultUncertain']) {
  test(`${state} prevents returning to options and changing selected equipment`, () => {
    const h = harness()
    h[state].value = true
    assert.equal(buttonDisabled(h.pqcSelectedTaskReadonly.value, h.payloadLoading.value,
      h.pqcSubmitResultUncertain.value, h.task), true)
    h.returnToPqcEquipmentOptions('A')
    h.updatePqcItemSelectedEquipment('A', { target: { value: '25' } })
    assert.deepEqual(h.selection, { selectedEquipmentId: undefined, selectedEquipmentNumber: '原手填设备' })
    assert.equal(h.touched(), 0)
  })
}

test('an equipment outside the current project options cannot replace the original equipment', () => {
  const h = harness()
  assert.throws(() => h.updatePqcItemSelectedEquipment('A', { target: { value: '999' } }), /不存在于当前 QA 版本/)
  assert.deepEqual(h.selection, { selectedEquipmentId: undefined, selectedEquipmentNumber: '原手填设备' })
  h.selection.selectedEquipmentId = 25
  h.selection.selectedEquipmentNumber = 'not-configured'
  assert.throws(() => h.buildPqcItemResultsPayload(h.task), /设备编号不属于所选检验设备/)
})
