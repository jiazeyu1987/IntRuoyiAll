const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

function setup() {
  const source = fs.readFileSync('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', 'utf8')
  const fragment = source.slice(source.indexOf('const getPqcPlannedQuantityForItem'), source.indexOf('const activePqcItemQuantity'))
  const task = { pqcTaskId: 1, inspectionItems: [{ itemCode: 'I', plannedInspectionQuantity: 50 },
    { itemCode: 'S3', plannedInspectionQuantity: 13 }] }
  const drafts = { 1: { inspectionQuantity: 50 }, 2: { inspectionQuantity: 50 } }
  const errors = []
  const context = { exports: {}, activePqcTaskOption: { value: task }, pqcDraft: { inspectionQuantity: 50 },
    getPqcTaskDraft: option => drafts[option.pqcTaskId], message: { error: text => errors.push(text) },
    markPqcTaskTouched() {}, resizePqcPieceValuesForCurrentTask() {} }
  vm.runInNewContext(ts.transpileModule(fragment + '\nexports.update = updatePqcItemQuantity; exports.quantity = getPqcInspectionQuantityForItem;',
    { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  return { ...context.exports, task, drafts, errors, context }
}

test('stricter per-item sampling keeps the frozen minima and uses maximum actual as task quantity', () => {
  const state = setup()
  state.update('S3', { target: { value: '20' } })
  assert.equal(state.quantity(state.task, 'S3'), 20)
  assert.equal(state.quantity(state.task, 'I'), 50)
  assert.equal(state.context.pqcDraft.inspectionQuantity, 50)
  state.update('I', { target: { value: '60' } })
  assert.equal(state.context.pqcDraft.inspectionQuantity, 60)
  assert.equal(state.task.inspectionItems[0].plannedInspectionQuantity, 50)
  assert.equal(state.task.inspectionItems[1].plannedInspectionQuantity, 13)
  assert.equal(state.drafts[2].inspectionQuantity, 50)
})

test('under-sampling and fractional quantities are rejected without changing current draft', () => {
  const state = setup()
  for (const value of ['12', '13.5', '0', '', 'NaN']) {
    const input = { value }
    state.update('S3', { target: input })
    assert.equal(input.value, '13')
    assert.equal(state.quantity(state.task, 'S3'), 13)
  }
  assert.equal(state.errors.length, 5)
})
