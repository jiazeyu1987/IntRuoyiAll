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
  const context = { exports: {}, getPqcTaskDraft: option => drafts[option.pqcTaskId] }
  vm.runInNewContext(ts.transpileModule(fragment + '\nexports.quantity = getPqcInspectionQuantityForItem;',
    { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, context)
  return { ...context.exports, task, drafts }
}

test('per-item sampling uses each frozen minimum when the separate input is removed', () => {
  const state = setup()
  assert.equal(state.quantity(state.task, 'S3'), 13)
  assert.equal(state.quantity(state.task, 'I'), 50)
  assert.equal(state.task.inspectionItems[0].plannedInspectionQuantity, 50)
  assert.equal(state.task.inspectionItems[1].plannedInspectionQuantity, 13)
  assert.equal(state.drafts[2].inspectionQuantity, 50)
})

test('existing per-item draft quantities remain readable without changing the frozen plan', () => {
  const state = setup()
  state.drafts[1].itemQuantities = { S3: 20 }
  assert.equal(state.quantity(state.task, 'S3'), 20)
  assert.equal(state.quantity(state.task, 'I'), 50)
  assert.equal(state.task.inspectionItems[1].plannedInspectionQuantity, 13)
})
