const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')
const panel = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')
const source = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
const names = ['getSelectedPqcTaskOption', 'activePqcTaskOption',
  'selectPqcInspectionRule', 'selectPqcInspectionTab',
  'applyPqcTaskOptionToSelectedProcess', 'switchPqcCurrentLoginEmployeeForActiveTask',
  'isPqcSubmitBlocked', 'assertPqcFormalSubmissionReady']
const code = ts.transpileModule(ast.statements.filter((s) => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every((d) => names.includes(d.name.getText(ast))))
  .map((s) => s.getText(ast)).join('\n') + `\nreturn {${names.join(',')}}`,
{ compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
function harness({ missingEmployee = false, currentTask = 1, available = true,
  boundEmployee = true, targetProcess } = {}) {
  const calls = [], errors = []
  const option = { pqcTaskId: 2, regulationVersionId: 202, qaProcessId: 303, inspectionRuleKey: 'PATROL_AM' }
  const previousOption = { pqcTaskId: 1, inspectionRuleKey: 'FIRST' }
  const selectedTaskId = ref(currentTask)
  const selectedRuleKey = ref(currentTask === 2 ? 'PATROL_AM' : 'FIRST')
  const employee = { id: 77 }
  const deviceState = reactive({ selectedProcess: { id: 10 }, selectedEmployee: boundEmployee ? { id: 66 } : undefined,
    template: {}, loadingTemplate: false, loadingEmployees: false })
  let finishEmployee, finishProcess
  const employeePending = new Promise((resolve) => { finishEmployee = resolve })
  const processPending = new Promise((resolve) => { finishProcess = resolve })
  const dependencies = {
    hasReturnNotification: ref(false), isInitialHandoffBlocked: ref(false),
    selectedPqcProductionProcess: ref({ routeProcessId: 1, processId: 2 }),
    pqcProductionProcessSelection: ref({ options: [], error: '' }),
    computed, deviceState, context: { actualEmployeeId: 66 }, employeeTemplateCode: ref('old'),
    payloadPreview: ref({}), payloadLoading: ref(false), pqcSubmitResultUncertain: ref(false),
    isPqcMode: ref(true), activePqcTaskOptionId: selectedTaskId,
    activePqcTabKey: ref('item-A'), selectedPqcInspectionKey: ref('item-A'),
    selectedPqcInspectionRuleKey: selectedRuleKey, pqcDraft: { inspectionType: 'FIRST' },
    allSwitchablePqcProcessOptions: ref(targetProcess ? [targetProcess] : []),
    PQC_INSPECTION_RULE_LABELS: { PATROL_AM: '上午巡检' },
    isFrontlinePqcProcess: (p) => Boolean(p), showFrontlineError: (e) => errors.push(e),
    persistCurrentPqcTaskDraft: () => calls.push('persist'),
    applyPqcTaskOptionToDraft: (value) => {
      calls.push(['apply', value]); selectedTaskId.value = value.pqcTaskId
      selectedRuleKey.value = value.inspectionRuleKey
    },
    resolveSelectedPqcInspectionItemKey: () => 'item-A',
    getDefaultPqcTaskOption: () => available ? previousOption : undefined,
    getPqcTaskOptionForRule: (_, ruleKey) => available ? ruleKey === 'FIRST' ? previousOption : option : undefined,
    getPqcTaskOptionsForInspectionItem: () => available ? [option] : [],
    getPqcTaskOptions: () => available ? [previousOption, option] : [],
    pqcTaskOptionIncludesItem: () => true, preferPqcTaskOption: (options) => options[0],
    findFirstPqcProcessForInspectionRule: () => targetProcess,
    clearPqcTaskOptionDraft: () => { calls.push('clear'); selectedTaskId.value = undefined },
    pqcTaskAvailabilityIssue: ref(undefined),
    findCurrentLoginEmployee: () => missingEmployee ? undefined : employee,
    handleSelectEmployee: async (selected) => {
      calls.push(['employee', selected, selectedTaskId.value])
      deviceState.loadingTemplate = true
      await employeePending
      deviceState.selectedEmployee = selected
      deviceState.template = { templateNo: 'PQC' }
      dependencies.context.actualEmployeeId = selected.id
      deviceState.loadingTemplate = false
    },
    handleSelectProcess: async (process) => { calls.push(['process', process]); await processPending }
  }
  return { ...dependencies, calls, errors, option, employee, finishEmployee, finishProcess,
    ...new Function(...Object.keys(dependencies), code)(...Object.values(dependencies)) }
}
for (const [handler, value] of [['selectPqcInspectionRule', 'PATROL_AM'],
  ['selectPqcInspectionTab', 'item-B']]) {
  test(`${handler} clears old identity, rebinds current employee to new task and waits`, async () => {
    const h = harness()
    let completed = false
    const pending = h[handler](value).then(() => { completed = true })
    assert.equal(h.deviceState.selectedEmployee, undefined)
    assert.equal(h.context.actualEmployeeId, undefined)
    assert.equal(h.deviceState.template, undefined)
    assert.equal(h.payloadPreview.value, undefined)
    assert.deepEqual(h.calls.filter(Array.isArray), [['apply', h.option], ['employee', h.employee, h.option.pqcTaskId]])
    assert.equal(h.activePqcTaskOptionId.value, h.option.pqcTaskId)
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

for (const [handler, value] of [['selectPqcInspectionRule', 'PATROL_AM'],
  ['selectPqcInspectionTab', 'item-B']]) {
  test(`${handler} rebinds an unbound employee even when the formal task is unchanged`, async () => {
    const h = harness({ currentTask: 2, boundEmployee: false })
    const pending = h[handler](value)
    assert.equal(h.isPqcSubmitBlocked.value, true)
    assert.deepEqual(h.calls.filter(Array.isArray), [['apply', h.option], ['employee', h.employee, 2]])
    h.finishEmployee(); await pending
    assert.equal(h.deviceState.selectedEmployee.id, h.employee.id)
    assert.equal(h.isPqcSubmitBlocked.value, false)
  })
}

test('formal submit is blocked when the selected employee is missing', () => {
  const h = harness({ currentTask: 2, boundEmployee: false })
  assert.equal(h.isPqcSubmitBlocked.value, true)
  assert.throws(h.assertPqcFormalSubmissionReady, /请先完成PQC人员和任务切换/)
})

test('a computed task inferred from another rule cannot authorize an unbound formal task ID', () => {
  const h = harness()
  h.selectedPqcInspectionRuleKey.value = 'PATROL_AM'
  assert.equal(h.activePqcTaskOption.value.pqcTaskId, 2)
  assert.equal(h.activePqcTaskOptionId.value, 1)
  assert.equal(h.isPqcSubmitBlocked.value, true)
  assert.throws(h.assertPqcFormalSubmissionReady, /请先完成PQC人员和任务切换/)
})

test('missing executable task remains blocked even with a previous employee binding', () => {
  const h = harness({ available: false })
  assert.equal(h.isPqcSubmitBlocked.value, true)
  assert.throws(h.assertPqcFormalSubmissionReady, /请先完成PQC人员和任务切换/)
})

test('the ordinary scoped task initializer assigns the formal ID before employee binding', () => {
  const option = { pqcTaskId: 616, inspectionRuleKey: 'PATROL_PM', inspectionType: 'PATROL',
    roundNo: 2, plannedInspectionQuantity: 1, inspectionItems: [{ itemCode: 'I005' }] }
  const dependencies = {
    activePqcTaskOptionId: ref(undefined), selectedPqcInspectionRuleKey: ref('PATROL_PM'),
    selectedPqcInspectionKey: ref('I005'), pqcDraft: {}, pqcSignatureDialogVisible: ref(true),
    pqcSignaturePassword: ref('previous'), pqcSubmitResultUncertain: ref(false),
    getPqcTaskDraft: () => ({}), isFrontlinePqcProcess: () => true,
    resolveSelectedPqcInspectionItemKey: () => 'I005',
    getPqcTaskOptionForRule: () => option, getDefaultPqcTaskOption: () => option,
    resolvePqcInspectionType: type => { assert.equal(type, 'PATROL') },
    clearPqcTaskOptionDraft: () => { throw new Error('valid formal task must initialize') },
    normalizePqcTaskOptionItemKey: () => 'I005', mapPqcInspectionItem: item => item,
    applyPqcItemEquipmentDefaults: () => {}
  }
  const initializerNames = ['applyPqcTaskOptionToDraft', 'applyPqcTaskSnapshotToDraft']
  const initializerCode = ts.transpileModule(ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.every(d => initializerNames.includes(d.name.getText(ast))))
    .map(s => s.getText(ast)).join('\n') + '\nreturn applyPqcTaskSnapshotToDraft',
  { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  new Function(...Object.keys(dependencies), initializerCode)(...Object.values(dependencies))({ qaProcessId: 4 })
  assert.equal(dependencies.activePqcTaskOptionId.value, 616)
  assert.equal(dependencies.selectedPqcInspectionRuleKey.value, 'PATROL_PM')
  assert.equal(dependencies.pqcDraft.inspectionQuantity, 1)
  assert.equal(dependencies.pqcSignatureDialogVisible.value, false)
  assert.equal(dependencies.pqcSignaturePassword.value, '')
})
test('missing current login employee is explicit and never substitutes old employee', async () => {
  const h = harness({ missingEmployee: true }); await h.selectPqcInspectionRule('PATROL_AM')
  assert.equal(h.deviceState.selectedEmployee, undefined)
  assert.equal(h.isPqcSubmitBlocked.value, true)
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
