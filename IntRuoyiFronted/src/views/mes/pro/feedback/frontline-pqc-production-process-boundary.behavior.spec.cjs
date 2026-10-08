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
const helper = {}
new Function('exports', ts.transpileModule(fs.readFileSync(path.join(__dirname,
  'frontlinePqcProductionProcess.ts'), 'utf8'), {
  compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS }
}).outputText)(helper)
const item = code => ({ itemCode: code })
const task = (id, routeProcessId, processId, code, taskStatus = 'PENDING') => ({
  pqcTaskId: id, routeProcessId, processId, productionProcessName: '正式生产工序',
  qaItemCode: code, taskStatus, inspectionType: 'FIRST', inspectionRuleKey: 'FIRST',
  businessDate: '2026-10-07', shiftCode: 'FIRST', roundNo: 1,
  inspectionItems: [item(code)]
})
function harness() {
  const tasks = [task(705, 103, 203, 'I011', 'SUBMITTED'), task(706, 104, 204, 'I011'),
    task(720, 103, 203, 'I012'), task(721, 104, 204, 'I012'), task(735, 103, 203, 'I013')]
  tasks.push({ ...task(750, 103, 203, 'I012'), inspectionType: 'PATROL', inspectionRuleKey: 'PATROL_AM' })
  const process = reactive({ activeOrderId: 425, qaProcessId: 170, regulationVersionId: 70,
    pqcTaskOptions: tasks, inspectionItems: ['I011', 'I012', 'I013'].map(item),
    productionSubmitCandidates: [{ activeOrderId: 425, routeProcessId: 103, processId: 203, eventId: 500 },
      { activeOrderId: 425, routeProcessId: 104, processId: 204, eventId: 600 }] })
  const selectedPqcProductionProcess = ref({ routeProcessId: 103, processId: 203 })
  const dependencies = {
    ...helper, computed, payloadLoading: ref(false), pqcSubmitResultUncertain: ref(false),
    hasReturnNotification: ref(false), isInitialHandoffBlocked: ref(false),
    hasPqcTaskOptionSnapshot: t => t.taskStatus === 'PENDING',
    selectedPqcProductionProcess,
    selectedPqcInspectionKey: ref('I011'), selectedPqcInspectionRuleKey: ref('FIRST'),
    activePqcTaskOptionId: ref(720),
    deviceState: reactive({ selectedProcess: process, selectedActiveOrder: { activeOrderId: 425 },
      selectedEmployee: { userId: 344 }, loadingEmployees: false, loadingTemplate: false }),
    activePqcTaskOption: ref(tasks[2]),
    isFrontlinePqcProcess: p => Boolean(p && 'qaProcessId' in p),
    pqcInspectionItems: ref(process.inspectionItems.map(t => ({ key: t.itemCode }))),
    formatPqcTaskOptionLabel: () => '首检', allSwitchablePqcProcessOptions: ref([process]),
    resolveErrorMessage: e => e.message,
    PQC_INSPECTION_RULE_ORDER: ['FIRST', 'PATROL_AM'],
    PQC_INSPECTION_RULE_TYPES: { FIRST: 'FIRST', PATROL_AM: 'PATROL' },
    PQC_INSPECTION_RULE_LABELS: { FIRST: '首检', PATROL_AM: '上午巡检' }
  }
  return { process, ...dependencies, ...extract(['getPqcTaskOptions',
    'normalizePqcTaskOptionItemKey', 'pqcTaskOptionIncludesItem', 'resolvePqcProcessItemKey',
    'resolveSelectedPqcInspectionItemKey', 'getPqcTaskOptionsForInspectionItem', 'getPqcTaskOptionsByRule',
    'getPqcTaskOptionForRule', 'hasExecutablePqcTaskForRule', 'findFirstPqcInspectionRuleKey',
    'findFirstPqcProcessForInspectionRule', 'preferPqcTaskOption', 'getDefaultPqcTaskOption',
    'getSelectedPqcTaskOption', 'getPqcCurrentSubmitTaskOptions', 'pqcInspectionTypeTabs',
    'pqcProductionProcessSelection', 'isPqcSubmitBlocked'], dependencies) }
}

test('partial first inspection skips the saved item and never crosses production processes', () => {
  const h = harness()
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(t => t.pqcTaskId), [720, 735])
  assert.deepEqual(h.getPqcTaskOptions(h.process).map(t => t.pqcTaskId), [720, 735, 750])
  assert.equal(h.getDefaultPqcTaskOption(h.process).pqcTaskId, 720)
  assert.equal(h.getSelectedPqcTaskOption(h.process).pqcTaskId, 720)
})

test('unselected production has no executable task or QA tabs, but its raw production selector remains reachable', () => {
  const h = harness()
  h.selectedPqcProductionProcess.value = undefined
  assert.deepEqual(h.getPqcTaskOptions(h.process), [])
  assert.deepEqual(h.pqcInspectionTypeTabs.value, [])
  assert.equal(h.isPqcSubmitBlocked.value, true)
  assert.deepEqual(h.pqcProductionProcessSelection.value.options.map(o => o.key).sort(), ['103:203', '104:204'])
  assert.throws(() => h.getPqcCurrentSubmitTaskOptions(), /请选择正式生产工序/)
  const { descriptor } = parse(panel)
  const selectorOffset = descriptor.template.content.indexOf('data-pqc-production-process-select')
  assert.ok(selectorOffset > 0)
  assert.ok(selectorOffset < descriptor.template.content.indexOf('pqcInspectionTypeTabs'))
  const script = compileScript(descriptor, { id: 'production-process-regression' })
  const compiled = compileTemplate({ source: descriptor.template.content,
    filename: 'FrontlineFixedTemplatePanel.vue', id: 'production-process-regression',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
})

test('older submitted production steps remain selectable; future unsourced tasks and saved-only steps do not', () => {
  const h = harness()
  h.process.pqcTaskOptions.push(task(800, 105, 205, 'I011'), task(801, 106, 206, 'I011', 'SUBMITTED'))
  h.process.productionSubmitCandidates.push({ activeOrderId: 425, routeProcessId: 106, processId: 206, eventId: 700 })
  assert.deepEqual(helper.buildPqcProductionProcessOptions([h.process], 425).map(o => o.key).sort(), ['103:203', '104:204'])
  h.selectedPqcInspectionKey.value = 'I012'
  h.selectedPqcInspectionRuleKey.value = 'PATROL_AM'
  assert.equal(h.getSelectedPqcTaskOption(h.process).pqcTaskId, 750)
  h.selectedPqcInspectionRuleKey.value = 'FIRST'
  h.process.pqcTaskOptions = h.process.pqcTaskOptions.map(t => t.pqcTaskId === 720 ? { ...t, taskStatus: 'SUBMITTED' } : t)
  h.activePqcTaskOption.value = h.process.pqcTaskOptions.find(t => t.pqcTaskId === 735)
  assert.equal(h.getDefaultPqcTaskOption(h.process).pqcTaskId, 735)
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(t => t.pqcTaskId), [735])
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 103, processId: 203 })
})

test('formal pair is mandatory, display-name absence is explicit but does not block valid tasks', () => {
  const h = harness()
  h.process.pqcTaskOptions.forEach(t => { t.productionProcessName = null })
  const options = helper.buildPqcProductionProcessOptions([h.process], 425)
  assert.equal(options[0].name, '')
  assert.equal(h.isPqcSubmitBlocked.value, false)
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(t => t.pqcTaskId), [720, 735])
  h.process.pqcTaskOptions[0].routeProcessId = undefined
  assert.deepEqual(h.pqcProductionProcessSelection.value.options, [])
  assert.match(h.pqcProductionProcessSelection.value.error, /缺少正式生产工序身份/)
  assert.deepEqual(h.getPqcTaskOptions(h.process), [])
  assert.deepEqual(h.pqcInspectionTypeTabs.value, [])
  assert.equal(h.isPqcSubmitBlocked.value, true)
  assert.throws(() => h.getPqcCurrentSubmitTaskOptions(), /缺少正式生产工序身份/)
})

test('production options reject foreign-order sources and conflicting frozen names', () => {
  const h = harness()
  h.process.productionSubmitCandidates[0].activeOrderId = 426
  assert.throws(() => helper.buildPqcProductionProcessOptions([h.process], 425), /当前订单的正式生产提交来源/)
  h.process.productionSubmitCandidates[0].activeOrderId = 425
  h.process.pqcTaskOptions.find(t => t.pqcTaskId === 735).productionProcessName = '另一名称'
  assert.throws(() => helper.buildPqcProductionProcessOptions([h.process], 425), /冻结名称不一致/)
})

test('same production pair, type, day, shift and round cannot merge distinct inspection rules', () => {
  const h = harness()
  h.process.pqcTaskOptions.push({ ...task(751, 103, 203, 'I013'),
    inspectionType: 'PATROL', inspectionRuleKey: 'PATROL_PM' })
  h.activePqcTaskOption.value = h.process.pqcTaskOptions.find(t => t.pqcTaskId === 750)
  assert.deepEqual(h.getPqcCurrentSubmitTaskOptions().map(t => t.pqcTaskId), [750])
})

test('an active task from another formal production process fails before payload construction', () => {
  const h = harness()
  h.activePqcTaskOption.value = h.process.pqcTaskOptions.find(t => t.pqcTaskId === 706)
  assert.throws(() => h.getPqcCurrentSubmitTaskOptions(), /与所选生产工序不一致/)
})

function selectionHarness() {
  const h = harness()
  const errors = [], calls = []
  const deps = { ...h, payloadLoading: ref(false), pqcSubmitResultUncertain: ref(false),
    hasInitialHandoffQuery: ref(false), hasReturnNotification: ref(false), isPqcMode: ref(true),
    context: { actualEmployeeId: 344 }, employeeTemplateCode: ref('old'), payloadPreview: ref({ old: true }),
    pqcDraft: { inspectionType: 'FIRST', inspectionQuantity: 10 },
    pqcSignatureDialogVisible: ref(true), pqcSignaturePassword: ref('previous-test-draft'),
    showFrontlineError: e => errors.push(e instanceof Error ? e.message : e),
    clearAllPqcTaskDrafts: () => calls.push('clear-all-drafts'), clearPqcPieceValues: () => calls.push('clear-values'),
    findInitialProcess: processes => h.findFirstPqcProcessForInspectionRule(processes, h.selectedPqcInspectionRuleKey.value),
    handleSelectProcess: async process => { calls.push(['QA', process.qaProcessId]); h.deviceState.selectedProcess = process },
    selectFrontlinePqcActiveOrder: async (state, order) => { state.selectedActiveOrder = order; calls.push(['order', order.activeOrderId]) },
    currentLoginUserId: ref(344), closePicker: () => {}, applyActiveOrderToContext: () => {},
    FrontlinePqcStaleActiveOrderSelectionError: class extends Error {}
  }
  const extra = 'let activeOrderSelectionRequestId = 0;'
  const names = ['clearPqcTaskOptionDraft', 'clearPqcExecutionSelection', 'handleSelectPqcProductionProcess', 'handleSelectActiveOrder']
  const chosen = ast.statements.filter(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.every(d => names.includes(d.name.getText(ast))))
  const code = ts.transpileModule(extra + chosen.map(s => s.getText(ast)).join('\n') +
    `\nreturn {${names.join(',')}}`, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  return { ...deps, calls, errors, ...new Function(...Object.keys(deps), code)(...Object.values(deps)) }
}

test('visible production selection clears old active task and signature draft before scoped QA selection', async () => {
  const h = selectionHarness()
  await h.handleSelectPqcProductionProcess({ target: { value: '104:204' } })
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 104, processId: 204 })
  assert.equal(h.activePqcTaskOptionId.value, undefined)
  assert.equal(h.pqcSignaturePassword.value, '')
  assert.equal(h.pqcSignatureDialogVisible.value, false)
  assert.deepEqual(h.calls, ['clear-all-drafts', 'clear-values', ['QA', 170]])
  assert.equal(h.getDefaultPqcTaskOption(h.process).pqcTaskId, 706)
  assert.deepEqual(h.errors, [])
})

test('uncertain submit blocks production and order switching without clearing the original lock or drafts', async () => {
  const h = selectionHarness()
  h.pqcSubmitResultUncertain.value = true
  await h.handleSelectPqcProductionProcess({ target: { value: '104:204' } })
  await h.handleSelectActiveOrder({ activeOrderId: 426 })
  assert.deepEqual(h.calls, [])
  assert.equal(h.pqcSubmitResultUncertain.value, true)
  assert.equal(h.pqcSignaturePassword.value, 'previous-test-draft')
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 103, processId: 203 })
  assert.equal(h.errors.length, 2)
})

test('manual order change clears the production selection and leaves selection to the visible control', async () => {
  const h = selectionHarness()
  await h.handleSelectActiveOrder({ activeOrderId: 426 })
  assert.equal(h.selectedPqcProductionProcess.value, undefined)
  assert.equal(h.selectedPqcInspectionRuleKey.value, undefined)
  assert.equal(h.deviceState.selectedProcess, undefined)
  assert.equal(h.activePqcTaskOptionId.value, undefined)
  assert.deepEqual(h.calls, ['clear-all-drafts', 'clear-values', ['order', 426]])
  assert.deepEqual(h.errors, [])
})

test('QA process selection rebinds its scoped active task while retaining the explicit production pair', async () => {
  const h = selectionHarness()
  const deps = { ...h, selectFrontlinePqcProcess: async (state, process) => { state.selectedProcess = process },
    isFrontlineProductionProcess: process => Boolean(process && 'routeProcessId' in process),
    applyProcessToContext: () => {}, findInitialEmployee: () => ({ userId: 344 }),
    applyPqcTaskSnapshotToDraft: process => { h.activePqcTaskOptionId.value = h.getDefaultPqcTaskOption(process)?.pqcTaskId },
    handleSelectEmployee: async employee => { h.deviceState.selectedEmployee = employee } }
  delete deps.handleSelectProcess
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'handleSelectProcess'))
  const code = ts.transpileModule('let processSelectionRequestId = 0;' + statement.getText(ast) +
    '\nreturn handleSelectProcess', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  await new Function(...Object.keys(deps), code)(...Object.values(deps))(h.process)
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 103, processId: 203 })
  assert.equal(h.activePqcTaskOptionId.value, 720)
  assert.equal(h.deviceState.selectedEmployee.userId, 344)
  assert.deepEqual(h.errors, [])
})

test('foreground active-order refresh keeps a valid explicit production identity and does not pick another task', async () => {
  const h = selectionHarness()
  let reads = 0, orderSwitches = 0
  const deps = { ...h, loadFrontlinePqcActiveOrders: async () => {
    reads++
    return [{ activeOrderId: 425 }]
  }, initializeInitialHandoffSelection: async () => { throw new Error('Unexpected handoff branch') },
  handleSelectActiveOrder: async () => { orderSwitches++ } }
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'refreshPqcActiveOrdersAndEnsureSelection'))
  const code = ts.transpileModule('let pqcActiveOrderRefreshPromise;' + statement.getText(ast) +
    '\nreturn refreshPqcActiveOrdersAndEnsureSelection', {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  await new Function(...Object.keys(deps), code)(...Object.values(deps))()
  assert.equal(reads, 1)
  assert.equal(orderSwitches, 0)
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 103, processId: 203 })
  assert.equal(h.getDefaultPqcTaskOption(h.process).pqcTaskId, 720)
})

function handoffHarness(routeProcessId = 103) {
  const h = harness(), errors = [], calls = []
  h.selectedPqcProductionProcess.value = undefined
  const task = { id: 1, taskType: 'PQC_HANDOFF', activeOrderId: 425, workOrderId: 77,
    sourceType: 'PQC_INSPECTION_TASK', sourceId: 720, routeProcessId, status: 'TODO', actionUrl: '/formal' }
  const query = { handoffTaskId: '1' }, order = { activeOrderId: 425, workOrderId: 77 }
  const deps = { ...h, route: { query }, isPqcMode: ref(true), initialHandoffTask: ref(),
    initialHandoffLoading: ref(false), initialHandoffInvalid: ref(false), payloadPreview: ref(),
    employeeTemplateCode: ref(), currentLoginUserId: ref(344),
    handoffNavigationContext: async () => ({ task, current: true, processable: true }),
    resolveActiveOrderHandoffTarget: () => ({ query }),
    loadFrontlinePqcActiveOrders: async () => [order], applyActiveOrderToContext: () => {},
    selectFrontlinePqcActiveOrder: async state => { state.selectedActiveOrder = order; return [h.process] },
    clearPqcExecutionSelection: () => { h.activePqcTaskOptionId.value = undefined },
    selectFrontlinePqcProcess: async (state, process) => {
      calls.push(h.getPqcTaskOptions(process).map(t => t.pqcTaskId))
      state.selectedProcess = process
    }, applyProcessToContext: () => {},
    applyPqcTaskOptionToDraft: option => { h.activePqcTaskOptionId.value = option.pqcTaskId },
    findCurrentLoginEmployee: () => ({ userId: 344 }),
    handleSelectEmployee: async employee => { h.deviceState.selectedEmployee = employee },
    showFrontlineError: error => errors.push(error.message)
  }
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'initializeInitialHandoffSelection'))
  const code = ts.transpileModule('let initialHandoffEpoch = 0;' + statement.getText(ast) +
    '\nreturn initializeInitialHandoffSelection', { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  return { ...deps, errors, calls,
    initialize: new Function(...Object.keys(deps), code)(...Object.values(deps)) }
}

test('validated handoff finds its exact raw task then establishes the formal pair before scoped QA selection', async () => {
  const h = handoffHarness()
  await h.initialize()
  assert.equal(h.initialHandoffInvalid.value, false)
  assert.equal(h.activePqcTaskOptionId.value, 720)
  assert.deepEqual(h.selectedPqcProductionProcess.value, { routeProcessId: 103, processId: 203 })
  assert.deepEqual(h.calls, [[720, 735, 750]])
  assert.deepEqual(h.errors, [])
})

test('handoff production identity mismatch blocks selection and never substitutes another production task', async () => {
  const h = handoffHarness(104)
  await h.initialize()
  assert.equal(h.initialHandoffInvalid.value, true)
  assert.equal(h.selectedPqcProductionProcess.value, undefined)
  assert.equal(h.deviceState.selectedProcess, undefined)
  assert.deepEqual(h.calls, [])
  assert.match(h.errors[0], /与正式生产工序不一致/)
})
