const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const panel = fs.readFileSync(`${__dirname}/FrontlineFixedTemplatePanel.vue`, 'utf8').replace(/\r\n/g, '\n')
const { descriptor } = parse(panel)
const ast = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = ['normalizePqcQuantity', 'setPqcQuantity', 'updatePqcQuantity', 'adjustPqcQuantity',
  'assertPqcInspectionWithinOrderQuantity',
  'getPqcInspectionQuantityForTask', 'getPqcInspectionQuantityForItem',
  'getPqcItemQuantity', 'pqcSelectedItemQuantity', 'pqcSelectedItemMinimum',
  'getPqcPlannedQuantityForItem', 'resizePqcPieceValuesForCurrentTask', 'assertPqcSignatureAndQuantityReady']
const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every(d => names.includes(d.name.getText(ast))))
assert.equal(statements.length, names.length)
const executable = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
  `\nreturn { ${names.join(',')} }`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness() {
  const task = { plannedInspectionQuantity: 2,
    inspectionItems: [{ itemCode: 'A', plannedInspectionQuantity: 1 }, { itemCode: 'B', plannedInspectionQuantity: 2 }] }
  const pqcDraft = reactive({ inspectionQuantity: 2, scrapQuantity: 0 })
  const stored = reactive({})
  const errors = []
  const dependencies = {
    computed,
    pqcSelectedTaskReadonly: ref(false), pqcSubmittedItem: ref(undefined),
    pqcOrderTotalQuantity: ref(10), resolveErrorMessage: error => error.message,
    pqcDraft, activePqcTaskOption: ref(task), activePqcTabKey: ref('A'),
    pqcInspectionQuantity: computed(() => pqcDraft.inspectionQuantity),
    pqcSignaturePassword: ref(''), pqcPieceValues: { A: ['合格'], B: ['合格', '不合格'] },
    markPqcTaskTouched: () => {}, showFrontlineError: text => errors.push(text),
    persistCurrentPqcTaskDraft: () => Object.assign(stored, pqcDraft), getPqcTaskDraft: () => stored,
    mapPqcInspectionItem: item => ({ key: item.itemCode }), getPqcPieceStateKeyForTask: key => key
  }
  Object.assign(stored, pqcDraft)
  return { ...dependencies, task, errors, stored,
    ...new Function(...Object.keys(dependencies), executable)(...Object.values(dependencies)) }
}

test('quantity follows the selected item, edits stay isolated and task quantity remains the maximum', () => {
  const h = harness()
  assert.equal(h.pqcSelectedItemQuantity.value, 1)
  h.activePqcTabKey.value = 'B'
  assert.equal(h.pqcSelectedItemQuantity.value, 2)
  h.adjustPqcQuantity('inspectionQuantity', 1)
  assert.equal(h.pqcDraft.inspectionQuantity, 3)
  assert.equal(h.getPqcInspectionQuantityForTask(h.task), 3)
  assert.equal(h.getPqcInspectionQuantityForItem(h.task, 'A'), 1)
  assert.equal(h.getPqcInspectionQuantityForItem(h.task, 'B'), 3)
  assert.deepEqual(h.pqcPieceValues.B, ['合格', '不合格'])
  h.pqcPieceValues.B.push('合格')
  h.adjustPqcQuantity('inspectionQuantity', -1)
  assert.equal(h.pqcDraft.inspectionQuantity, 2)
  assert.deepEqual(h.pqcPieceValues.B, ['合格', '不合格'])
  h.adjustPqcQuantity('inspectionQuantity', -1)
  assert.equal(h.pqcDraft.inspectionQuantity, 2)
  assert.match(h.errors.at(-1), /规程/)
  h.activePqcTabKey.value = 'A'
  assert.equal(h.pqcSelectedItemQuantity.value, 1)
  assert.equal(h.pqcSelectedItemMinimum.value, 1)
  h.adjustPqcQuantity('inspectionQuantity', 1)
  assert.equal(h.pqcSelectedItemQuantity.value, 2)
  h.activePqcTabKey.value = 'B'
  assert.equal(h.pqcSelectedItemQuantity.value, 2)
})

test('5000-piece patrol displays 80 for appearance and 20 for no-jump, retaining independent edits', () => {
  const h = harness()
  h.pqcOrderTotalQuantity.value = 5000
  h.task.inspectionItems[0].plannedInspectionQuantity = 80
  h.task.inspectionItems[1].plannedInspectionQuantity = 20
  h.pqcDraft.inspectionQuantity = h.stored.inspectionQuantity = h.task.plannedInspectionQuantity = 80
  assert.equal(h.pqcSelectedItemQuantity.value, 80)
  h.activePqcTabKey.value = 'B'
  assert.equal(h.pqcSelectedItemQuantity.value, 20)
  assert.equal(h.pqcSelectedItemMinimum.value, 20)
  h.adjustPqcQuantity('inspectionQuantity', 1)
  assert.equal(h.pqcSelectedItemQuantity.value, 21)
  assert.equal(h.getPqcInspectionQuantityForTask(h.task), 80)
  h.activePqcTabKey.value = 'A'
  assert.equal(h.pqcSelectedItemQuantity.value, 80)
  h.activePqcTabKey.value = 'B'
  assert.equal(h.pqcSelectedItemQuantity.value, 21)
  h.updatePqcQuantity('inspectionQuantity', { target: { value: '20' } })
  const input = { value: '19' }
  h.updatePqcQuantity('inspectionQuantity', { target: input })
  assert.equal(input.value, '20')
  assert.equal(h.getPqcInspectionQuantityForItem(h.task, 'A'), 80)
})

test('loss cannot exceed inspection via buttons, direct input or submission; equality is allowed', () => {
  const h = harness()
  h.adjustPqcQuantity('scrapQuantity', 1)
  h.adjustPqcQuantity('scrapQuantity', 1)
  assert.equal(h.pqcDraft.scrapQuantity, 2)
  assert.doesNotThrow(() => h.assertPqcSignatureAndQuantityReady())
  h.adjustPqcQuantity('scrapQuantity', 1)
  assert.equal(h.pqcDraft.scrapQuantity, 2)
  const input = { value: '9' }
  h.updatePqcQuantity('scrapQuantity', { target: input })
  assert.equal(input.value, '2')
  assert.equal(h.stored.scrapQuantity, 2)
  h.updatePqcQuantity('inspectionQuantity', { target: { value: '4' } })
  h.updatePqcQuantity('scrapQuantity', { target: { value: '4' } })
  h.adjustPqcQuantity('inspectionQuantity', -1)
  assert.equal(h.pqcDraft.inspectionQuantity, 4)
  h.pqcDraft.scrapQuantity = 5
  assert.throws(() => h.assertPqcSignatureAndQuantityReady(), /损耗数量不能大于检验数量/)
  h.pqcDraft.scrapQuantity = 0
  h.adjustPqcQuantity('scrapQuantity', -1)
  assert.equal(h.pqcDraft.scrapQuantity, 0)
})

test('real template compiles and inspection controls are editable with loss maximum bound', () => {
  const script = compileScript(descriptor, { id: 'pqc-quantity-regression' })
  const compiled = compileTemplate({ source: descriptor.template.content,
    filename: 'FrontlineFixedTemplatePanel.vue', id: 'pqc-quantity-regression',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
  const block = panel.slice(panel.indexOf('<label for="frontlinePqcInspectionQuantity">'),
    panel.indexOf('<div\n            class="frontline-inline-error-slot"'))
  assert.doesNotMatch(block, /\sdisabled\s|\sreadonly\s/)
  assert.match(block, /:max="pqcInspectionQuantity"/)
  assert.match(block, /:max="pqcOrderTotalQuantity"/)
  assert.match(block, /:value="pqcSelectedItemQuantity \|\| ''"/)
  assert.match(block, /:min="pqcSelectedItemMinimum"/)
  assert.match(block, /:disabled="pqcSelectedTaskReadonly \|\| !activePqcTabKey \|\| pqcOrderTotalQuantity == null \|\| pqcSelectedItemQuantity >= pqcOrderTotalQuantity"/)
  assert.match(block, /@click="adjustPqcQuantity\('inspectionQuantity', 1\)"/)
  assert.match(block, /@click="adjustPqcQuantity\('inspectionQuantity', -1\)"/)
})

test('inspection accepts the order total, rejects excess from buttons, input and existing drafts', () => {
  const h = harness()
  h.updatePqcQuantity('inspectionQuantity', { target: { value: '10' } })
  assert.equal(h.stored.inspectionQuantity, 10)
  assert.doesNotThrow(() => h.assertPqcSignatureAndQuantityReady())
  h.adjustPqcQuantity('inspectionQuantity', 1)
  assert.equal(h.pqcDraft.inspectionQuantity, 10)
  const input = { value: '14' }
  h.updatePqcQuantity('inspectionQuantity', { target: input })
  assert.equal(input.value, '10')
  assert.equal(h.stored.inspectionQuantity, 10)
  assert.equal(h.getPqcInspectionQuantityForItem(h.task, 'A'), 10)
  assert.match(h.errors.at(-1), /不能大于总数量/)
  h.pqcDraft.inspectionQuantity = 14
  assert.throws(() => h.assertPqcSignatureAndQuantityReady(), /不能大于总数量/)
  h.pqcOrderTotalQuantity.value = 20
  assert.doesNotThrow(() => h.assertPqcSignatureAndQuantityReady())
  h.pqcOrderTotalQuantity.value = undefined
  assert.throws(() => h.assertPqcSignatureAndQuantityReady(), /缺少有效的订单总数量/)
})
