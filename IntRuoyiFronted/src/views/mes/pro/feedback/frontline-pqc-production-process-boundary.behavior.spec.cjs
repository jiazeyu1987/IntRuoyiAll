const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const panel = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
function extract(names, dependencies) {
  const selected = new Set(names)
  const statements = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.every(d => selected.has(d.name.getText(ast))))
  assert.equal(statements.length, names.length, 'extract the actual production functions')
  const executable = ts.transpileModule(statements.map(s => s.getText(ast)).join('\n') +
    `\nreturn { ${names.join(',')} }`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  return new Function(...Object.keys(dependencies), executable)(...Object.values(dependencies))
}
const item = (code, quantity) => ({ itemCode: code, plannedInspectionQuantity: quantity })
const task = (id, rule) => ({
  pqcTaskId: id, routeProcessId: null, processId: null, qaItemCode: '', taskStatus: 'PENDING',
  inspectionType: rule === 'FIRST' ? 'FIRST' : 'PATROL', inspectionRuleKey: rule,
  businessDate: '2026-10-14', shiftCode: rule, roundNo: 1,
  plannedInspectionQuantity: 5, inspectionItems: [item('A', 2), item('B', 5)]
})
function harness() {
  const tasks = [task(1, 'FIRST'), task(2, 'PATROL_AM'), task(3, 'PATROL_PM')]
  const process = { activeOrderId: 425, qaProcessId: 170, regulationVersionId: 70,
    pqcTaskOptions: tasks, inspectionItems: tasks[0].inspectionItems, productionSubmitCandidates: [] }
  const dependencies = {
    hasPqcTaskOptionSnapshot: option => option.taskStatus === 'PENDING',
    deviceState: { selectedProcess: process }, activePqcTaskOption: ref(tasks[0]),
    isFrontlinePqcProcess: value => Boolean(value?.qaProcessId),
    pqcInspectionItems: ref([{ key: 'A' }, { key: 'B' }]),
    formatPqcTaskOptionLabel: () => '首检',
    pqcTaskOptionIncludesItem: (option, key) => option.inspectionItems.some(item => item.itemCode === key)
  }
  return { process, ...dependencies, ...extract(['getPqcTaskOptions',
    'getPqcCurrentSubmitTaskOptions', 'getPqcInspectionQuantityForItem'], dependencies) }
}

test('QA stages are executable before any production submission or production selection', () => {
  const h = harness()
  assert.deepEqual(h.getPqcTaskOptions(h.process).map(task => task.pqcTaskId), [1, 2, 3])
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(task => task.pqcTaskId), [1])
  h.activePqcTaskOption.value = h.process.pqcTaskOptions[1]
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(task => task.pqcTaskId), [2])
})

test('multiple inspection items submit as one stage task with independent sample counts', () => {
  const h = harness(), option = h.process.pqcTaskOptions[0]
  assert.equal(h.getPqcCurrentSubmitTaskOptions().length, 1)
  assert.equal(h.getPqcInspectionQuantityForItem(option, 'A'), 2)
  assert.equal(h.getPqcInspectionQuantityForItem(option, 'B'), 5)
  assert.throws(() => h.getPqcInspectionQuantityForItem(option, 'missing'), /冻结抽样数量/)
})

test('submitted stages disappear from executable options without hiding other stages', () => {
  const h = harness()
  h.process.pqcTaskOptions[0].taskStatus = 'SUBMITTED'
  assert.deepEqual(h.getPqcTaskOptions(h.process).map(task => task.pqcTaskId), [2, 3])
})

test('real Vue template compiles and offers QA selection without a production-process selector', () => {
  assert.doesNotMatch(panel, /data-pqc-production-process-select|selectedPqcProductionProcess/)
  const { descriptor } = parse(panel)
  const script = compileScript(descriptor, { id: 'order-qa-stage-regression' })
  const compiled = compileTemplate({ source: descriptor.template.content,
    filename: 'FrontlineFixedTemplatePanel.vue', id: 'order-qa-stage-regression',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
})
