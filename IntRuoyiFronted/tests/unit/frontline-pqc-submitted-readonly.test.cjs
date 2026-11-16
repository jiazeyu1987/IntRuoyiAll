const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const file = path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const { descriptor } = parse(fs.readFileSync(file, 'utf8'))
const ast = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declarations = new Map(ast.statements.filter(ts.isVariableStatement).flatMap(statement =>
  statement.declarationList.declarations.map(d => [d.name.getText(ast), statement.getText(ast)])))
function load(names, deps) {
  const code = names.map(name => { assert.ok(declarations.has(name), name); return declarations.get(name) }).join('\n')
  return new Function(...Object.keys(deps), ts.transpileModule(code + `\nreturn { ${names.join(',')} }`,
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)(...Object.values(deps))
}
test('completed tasks remain browsable alongside pending tasks; cancelled and malformed tasks do not', () => {
  const { getBrowsablePqcTaskOptions } = load(['getBrowsablePqcTaskOptions'], {
    resolveFrontlinePqcTaskInvalidFields: option => option.valid ? [] : ['task identity']
  })
  const tasks = ['PENDING', 'SUBMITTED', 'CONFIRMED', 'CANCELLED'].map((taskStatus, i) => ({ pqcTaskId: i, taskStatus, valid: true }))
  tasks.push({ taskStatus: 'SUBMITTED', valid: false })
  assert.deepEqual(getBrowsablePqcTaskOptions({ pqcTaskOptions: tasks }).map(t => t.taskStatus), ['PENDING', 'SUBMITTED', 'CONFIRMED'])
})
test('card and task switching display persisted sample counts and loss, without substituting the plan or draft', () => {
  const activePqcTaskOption = ref({ taskStatus: 'SUBMITTED', plannedInspectionQuantity: 80,
    submittedInspection: { scrapQuantity: 2, items: [
      { itemCode: 'A', sampleValues: ['合格', '不合格', '合格'] },
      { itemCode: 'B', sampleValues: ['1.2'] }
    ] } })
  const activePqcTabKey = ref('A')
  const h = load(['pqcSelectedTaskReadonly', 'pqcSubmittedItem', 'pqcDisplayedScrapQuantity', 'pqcSelectedItemQuantity'], {
    computed, activePqcTaskOption, activePqcTabKey, pqcDraft: reactive({ scrapQuantity: 99 }), getPqcItemQuantity: () => 80
  })
  assert.equal(h.pqcSelectedItemQuantity.value, 3)
  assert.equal(h.pqcDisplayedScrapQuantity.value, 2)
  activePqcTabKey.value = 'B'
  assert.equal(h.pqcSelectedItemQuantity.value, 1)
  activePqcTaskOption.value = { taskStatus: 'CONFIRMED', submittedInspection: {
    scrapQuantity: 0, items: [{ itemCode: 'B', sampleValues: ['5', '6'] }] } }
  assert.equal(h.pqcSelectedItemQuantity.value, 2)
  assert.equal(h.pqcDisplayedScrapQuantity.value, 0)
  activePqcTaskOption.value = { taskStatus: 'PENDING' }
  assert.equal(h.pqcSelectedTaskReadonly.value, false)
  assert.equal(h.pqcSelectedItemQuantity.value, 80)
  assert.equal(h.pqcDisplayedScrapQuantity.value, 99)
})
test('submitted quantity changes and reset cannot mutate the draft even if handlers are invoked', () => {
  const h = load(['setPqcQuantity', 'handleResetPqc'], { pqcSelectedTaskReadonly: ref(true) })
  assert.doesNotThrow(() => h.setPqcQuantity('inspectionQuantity', 100))
  assert.doesNotThrow(() => h.setPqcQuantity('scrapQuantity', 10))
  assert.doesNotThrow(() => h.handleResetPqc())
})
test('submitted task disables formal submission', () => {
  const h = load(['isPqcSubmitBlocked'], { computed, pqcSelectedTaskReadonly: ref(true) })
  assert.equal(h.isPqcSubmitBlocked.value, true)
})
test('completed patrol rounds can be selected for viewing; cancelled rounds and navigation locks cannot', () => {
  const selector = fs.readFileSync(path.join(path.dirname(file), 'FrontlinePqcInspectionTypeSelector.vue'), 'utf8')
  const content = parse(selector).descriptor.scriptSetup.content
  const selectorAst = ts.createSourceFile('selector.ts', content, ts.ScriptTarget.Latest, true)
  const selectRound = selectorAst.statements.find(s => ts.isFunctionDeclaration(s) && s.name.text === 'selectRound')
  const props = { disabled: false, patrolRounds: [{ roundNo: 1, pendingCount: 0, cancelled: false },
    { roundNo: 2, pendingCount: 0, cancelled: true }] }
  const events = []
  const select = new Function('props', 'patrolDialog', 'emit', ts.transpileModule(selectRound.getText(selectorAst) + '\nreturn selectRound',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText)(props, ref({ close() {} }), (...args) => events.push(args))
  select(1)
  select(2)
  props.disabled = true
  select(1)
  assert.deepEqual(events, [['select-round', 1]])
})
test('PQC panel script and template compile', () => {
  const script = compileScript(descriptor, { id: 'pqc-submitted' })
  const result = compileTemplate({ id: 'pqc-submitted', source: descriptor.template.content,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [])
})

test('submitted result cards derive bulk status from formal samples, including mixed and empty results', () => {
  const pqcSubmittedItem = ref({ sampleValues: ['合格', '合格'] })
  const h = load(['isPqcSubmittedChoiceActive'], { pqcSubmittedItem })
  assert.equal(h.isPqcSubmittedChoiceActive('合格'), true)
  assert.equal(h.isPqcSubmittedChoiceActive('不合格'), false)
  pqcSubmittedItem.value.sampleValues = ['合格', '不合格']
  assert.equal(h.isPqcSubmittedChoiceActive('合格'), false)
  assert.equal(h.isPqcSubmittedChoiceActive('不合格'), false)
  pqcSubmittedItem.value.sampleValues = ['不合格']
  assert.equal(h.isPqcSubmittedChoiceActive('不合格'), true)
  pqcSubmittedItem.value.sampleValues = []
  assert.equal(h.isPqcSubmittedChoiceActive('合格'), false)
})

test('submitted piece viewer copies formal samples and never saves or bulk-modifies them', () => {
  const samples = ['合格', '不合格']
  const activePqcInspectionKey = ref()
  const pqcPieceDraftValues = ref([])
  const pqcSelectedTaskReadonly = ref(true)
  const activePqcTaskOption = ref({ submittedInspection: { items: [{ itemCode: 'A', sampleValues: samples }] } })
  const errors = []
  const h = load(['openPqcPieceInspection', 'closePqcPieceInspection', 'applyPqcBulkChoice',
    'updatePqcPieceChoice', 'activePqcItemQuantity'], {
    computed, pqcSelectedTaskReadonly, activePqcTaskOption, activePqcInspectionKey,
    pqcPieceDraftValues, showFrontlineError: error => errors.push(error)
  })
  h.openPqcPieceInspection('A')
  assert.equal(activePqcInspectionKey.value, 'A')
  assert.deepEqual(pqcPieceDraftValues.value, samples)
  assert.notEqual(pqcPieceDraftValues.value, samples)
  assert.equal(h.activePqcItemQuantity.value, 2)
  h.updatePqcPieceChoice(0, false)
  h.applyPqcBulkChoice('A', '合格')
  assert.deepEqual(pqcPieceDraftValues.value, samples)
  h.closePqcPieceInspection(true)
  assert.equal(activePqcInspectionKey.value, undefined)
  assert.deepEqual(samples, ['合格', '不合格'])
  h.openPqcPieceInspection('missing')
  assert.equal(errors.length, 1)
  assert.equal(activePqcInspectionKey.value, undefined)
})
