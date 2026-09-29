const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const panel = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const names = ['selectPqcInspectionRule', 'selectPqcInspectionTab', 'selectPqcInspectionTaskOption',
  'applyPqcTaskOptionToSelectedProcess', 'switchPqcCurrentLoginEmployeeForActiveTask', 'isPqcSubmitBlocked']
const code = ts.transpileModule(ast.statements.filter((s) => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every((d) => names.includes(d.name.getText(ast))))
  .map((s) => s.getText(ast)).join('\n') + `\nreturn {${names.join(',')}}`,
{ compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
function harness({ missingEmployee = false, currentTask = 1, available = true, targetProcess } = {}) {
  const calls = [], errors = []
  const option = { pqcTaskId: 2, regulationVersionId: 202, qaProcessId: 303, inspectionRuleKey: 'PATROL_AM' }
  const active = ref({ pqcTaskId: currentTask })
  const employee = { id: 77 }
  const deviceState = reactive({ selectedProcess: { id: 10 }, selectedEmployee: { id: 66 },
    template: {}, loadingTemplate: false, loadingEmployees: false })
  let finishEmployee, finishProcess
  const employeePending = new Promise((resolve) => { finishEmployee = resolve })
  const processPending = new Promise((resolve) => { finishProcess = resolve })
  const dependencies = {
    computed, deviceState, context: { actualEmployeeId: 66 }, employeeTemplateCode: ref('old'),
    payloadPreview: ref({}), payloadLoading: ref(false), pqcSubmitResultUncertain: ref(false),
    isPqcMode: ref(true), activePqcTaskOption: active, activePqcTaskOptionId: computed(() => active.value?.pqcTaskId),
    activePqcTabKey: ref('item-A'), selectedPqcInspectionKey: ref('item-A'),
    selectedPqcInspectionRuleKey: ref('FIRST'), pqcDraft: { inspectionType: 'FIRST' },
    allSwitchablePqcProcessOptions: ref(targetProcess ? [targetProcess] : []),
    PQC_INSPECTION_RULE_LABELS: { PATROL_AM: '上午巡检' },
    isFrontlinePqcProcess: (p) => Boolean(p), showFrontlineError: (e) => errors.push(e),
    persistCurrentPqcTaskDraft: () => calls.push('persist'),
    applyPqcTaskOptionToDraft: (value) => { calls.push(['apply', value]); active.value = value },
    getPqcTaskOptionForRule: () => available ? option : undefined,
    getPqcTaskOptionsForInspectionItem: () => available ? [option] : [],
    getPqcTaskOptions: () => available ? [option] : [],
    pqcTaskOptionIncludesItem: () => true, preferPqcTaskOption: (options) => options[0],
    findFirstPqcProcessForInspectionRule: () => targetProcess,
    clearPqcTaskOptionDraft: () => { calls.push('clear'); active.value = undefined },
    findCurrentLoginEmployee: () => missingEmployee ? undefined : employee,
    handleSelectEmployee: async (selected) => {
      calls.push(['employee', selected, active.value])
      deviceState.loadingTemplate = true
      await employeePending
      deviceState.loadingTemplate = false
    },
    handleSelectProcess: async (process) => { calls.push(['process', process]); await processPending }
  }
  return { ...dependencies, calls, errors, option, employee, finishEmployee, finishProcess,
    ...new Function(...Object.keys(dependencies), code)(...Object.values(dependencies)) }
}
for (const [handler, value] of [['selectPqcInspectionRule', 'PATROL_AM'],
  ['selectPqcInspectionTab', 'item-B'], ['selectPqcInspectionTaskOption', 2]]) {
  test(`${handler} clears old identity, rebinds current employee to new task and waits`, async () => {
    const h = harness()
    let completed = false
    const pending = h[handler](value).then(() => { completed = true })
    assert.equal(h.deviceState.selectedEmployee, undefined)
    assert.equal(h.context.actualEmployeeId, undefined)
    assert.equal(h.deviceState.template, undefined)
    assert.equal(h.payloadPreview.value, undefined)
    assert.deepEqual(h.calls.filter(Array.isArray), [['apply', h.option], ['employee', h.employee, h.option]])
    assert.equal(h.isPqcSubmitBlocked.value, true)
    await Promise.resolve()
    assert.equal(completed, false)
    h.finishEmployee(); await pending
    assert.equal(h.isPqcSubmitBlocked.value, false)
    assert.deepEqual(h.errors, [])
  })
  test(`${handler} does not repeat employee switch for same task`, async () => {
    const h = harness({ currentTask: 2 }); await h[handler](value)
    assert.deepEqual(h.calls.filter(Array.isArray), [])
  })
}
test('missing current login employee is explicit and never substitutes old employee', async () => {
  const h = harness({ missingEmployee: true }); await h.selectPqcInspectionRule('PATROL_AM')
  assert.equal(h.deviceState.selectedEmployee, undefined)
  assert.equal(h.calls.some((call) => Array.isArray(call) && call[0] === 'employee'), false)
  assert.match(h.errors[0], /当前登录账号未返回PQC人员候选/)
})
test('rule absent on current process delegates to formal target process and awaits it', async () => {
  const process = { id: 20 }
  const h = harness({ available: false, targetProcess: process })
  let completed = false
  const pending = h.selectPqcInspectionRule('PATROL_AM').then(() => { completed = true })
  assert.deepEqual(h.calls.filter(Array.isArray), [['process', process]])
  await Promise.resolve(); assert.equal(completed, false)
  h.finishProcess(); await pending
  assert.deepEqual(h.errors, [])
})
test('no executable rule clears task and reports absence', async () => {
  const h = harness({ available: false }); await h.selectPqcInspectionRule('PATROL_AM')
  assert.equal(h.activePqcTaskOption.value, undefined)
  assert.match(h.errors[0], /没有可执行的上午巡检工序/)
  assert.deepEqual(h.calls.filter(Array.isArray), [])
})
