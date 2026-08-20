const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const projectionSource = read('src/api/mes/pro/feedback/pqcProjection.ts')
const qaTemplateSource = read('src/api/mes/qc/template/index.ts')
const pageSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const employeeContextSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, 'missing start token: ' + startToken)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, 'missing end token after ' + startToken + ': ' + endToken)
  return source.slice(start, end)
}

const loadTypeScriptModule = (source, filename, requireModule = require) => {
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    },
    fileName: filename,
    reportDiagnostics: true
  })
  const diagnostics = compiled.diagnostics || []
  assert.equal(diagnostics.length, 0, 'TypeScript projection module must transpile without diagnostics.')
  const module = { exports: {} }
  Function('module', 'exports', 'require', compiled.outputText)(module, module.exports, requireModule)
  return module.exports
}

assert.match(
  apiSource,
  /export type FrontlinePqcInspectionRuleKey = 'FIRST' \| 'PATROL_AM' \| 'PATROL_PM' \| 'FINAL'/,
  'Frontend PQC API must preserve FIRST/PATROL_AM/PATROL_PM/FINAL as the formal rule-key union.'
)
assert.match(
  apiSource,
  /export type FrontlinePqcResultType = 'BOOLEAN' \| 'NUMERIC' \| 'TEXT'/,
  'Frontend PQC API must expose the backend resultType union without falling back to string.'
)
assert.match(
  apiSource,
  /export type FrontlinePqcTaskStatus = 'PENDING' \| 'SUBMITTED' \| 'CONFIRMED' \| 'CANCELLED'/,
  'Task options must expose only the four formal persisted task states.'
)
assert.match(
  apiSource,
  /export type FrontlinePqcTaskSummaryState =\s*\| 'NOT_CREATED'\s*\| 'PENDING'\s*\| 'SUBMITTED'\s*\| 'CONFIRMED'\s*\| 'CANCELLED'\s*\| 'MIXED'/s,
  'NOT_CREATED and MIXED must live in the task summary state contract.'
)

const ruleInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcInspectionTypeRuleVO {',
  'export interface FrontlinePqcTaskSummaryVO {'
)
for (const field of [
  'key',
  'inspectionType',
  'label',
  'roundLabel',
  'required',
  'fixedQuantity',
  'notApplicableReason',
  'taskRule',
  'releaseGate'
]) {
  assert.match(ruleInterface, new RegExp(field + '\\??:'), 'Inspection type rule must retain ' + field + '.')
}

const summaryInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcTaskSummaryVO {',
  'export interface FrontlinePqcProcessVO {'
)
for (const field of [
  'state',
  'totalCount',
  'pendingCount',
  'submittedCount',
  'confirmedCount',
  'cancelledCount'
]) {
  assert.match(summaryInterface, new RegExp(field + ':'), 'PQC task summary must retain ' + field + '.')
}

const processInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcProcessVO {',
  'export interface FrontlinePqcTaskOptionVO {'
)
for (const [field, type] of [
  ['activeOrderId', 'number'],
  ['inspectionTypeRules', 'FrontlinePqcInspectionTypeRuleVO\\[\\]'],
  ['inspectionItems', 'FrontlinePqcInspectionItemVO\\[\\]'],
  ['taskSummary', 'FrontlinePqcTaskSummaryVO'],
  ['pqcTaskOptions', 'FrontlinePqcTaskOptionVO\\[\\]'],
  ['productionSubmitCandidates', 'FrontlinePqcProductionSubmitCandidateVO\\[\\]']
]) {
  assert.match(
    processInterface,
    new RegExp(field + ':\\s*' + type),
    'PQC process projection must require ' + field + '.'
  )
}
assert.doesNotMatch(
  processInterface,
  /\b(?:pqcTaskId|inspectionType|businessDate|shiftCode|roundNo|plannedInspectionQuantity)\??:/,
  'The process DTO must not retain a flattened task snapshot beside formal pqcTaskOptions.'
)

const taskOptionInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcTaskOptionVO {',
  'export interface FrontlinePqcProductionSubmitCandidateVO {'
)
assert.match(
  apiSource,
  /export type FrontlinePqcBusinessDateResponse = string \| \[number, number, number\]/,
  'The PQC response contract must expose both the ISO date and Jackson LocalDate tuple shapes.'
)
assert.match(
  apiSource,
  /export interface FrontlinePqcTaskOptionResponseVO\s+extends Omit<FrontlinePqcTaskOptionVO, 'businessDate'>/,
  'The raw PQC task response must stay separate from the normalized page task contract.'
)
assert.match(
  apiSource,
  /request\.get<FrontlinePqcProcessResponseVO\[\]>/,
  'The PQC process request must use the raw response DTO before projection.'
)
for (const [field, type] of [
  ['inspectionRuleKey', 'FrontlinePqcInspectionRuleKey'],
  ['inspectionType', 'FrontlinePqcInspectionType'],
  ['taskStatus', 'FrontlinePqcTaskStatus'],
  ['ruleSort', 'number'],
  ['inspectionTypeRule', 'FrontlinePqcInspectionTypeRuleVO'],
  ['inspectionItems', 'FrontlinePqcInspectionItemVO\\[\\]']
]) {
  assert.match(
    taskOptionInterface,
    new RegExp(field + ':\\s*' + type),
    'PQC task options must require ' + field + '.'
  )
}

const candidateInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcProductionSubmitCandidateVO {',
  'export interface FrontlinePqcEquipmentOptionVO {'
)
for (const field of ['eventId', 'serverSubmitTime', 'activeOrderId', 'routeProcessId', 'processId']) {
  assert.match(candidateInterface, new RegExp(field + ':'), 'Production candidate must retain ' + field + '.')
}

const itemInterface = blockBetween(
  apiSource,
  'export interface FrontlinePqcInspectionItemVO {',
  'export interface FrontlineActiveOrderVO {'
)
for (const field of [
  'itemSort',
  'itemCode',
  'itemName',
  'inspectionMethod',
  'inspectionTool',
  'samplingPlanText',
  'standardText',
  'standardLowerLimit',
  'standardUpperLimit',
  'standardUnit',
  'standardPrecision',
  'equipmentRequired',
  'equipmentOptions',
  'applicableInspectionTypes',
  'firstInspectionQuantity',
  'patrolInspectionRatio',
  'critical',
  'failureRule',
  'sourceNote',
  'sourceOriginalPage',
  'sourceOriginalItem',
  'sourceOriginalExcerpt',
  'sourceOriginalMethod'
]) {
  assert.match(itemInterface, new RegExp(field + '\\??:'), 'PQC item projection must retain ' + field + '.')
}
assert.match(
  itemInterface,
  /resultType:\s*FrontlinePqcResultType/,
  'PQC item resultType must be the strict BOOLEAN/NUMERIC/TEXT union.'
)
assert.doesNotMatch(
  itemInterface,
  /acceptanceStandard|processInspectionMethod/,
  'The formal item DTO must not keep compatibility aliases outside the published-version contract.'
)
assert.doesNotMatch(
  pageSource,
  /\.acceptanceStandard|\.processInspectionMethod/,
  'The page must consume canonical standardText and inspectionMethod fields directly.'
)
assert.doesNotMatch(
  blockBetween(
    pageSource,
    'const isPqcNumericResultType =',
    'const resolvePqcNumericStep ='
  ),
  /'NUMBER'|'DECIMAL'|'MEASURE'|'MEASURED_VALUE'/,
  'The page must not accept legacy numeric result-type aliases.'
)
assert.match(
  pageSource,
  /const isPqcNumericResultType = \(resultType:\s*FrontlinePqcResultType\) =>\s*resultType === 'NUMERIC'/,
  'The page numeric branch must consume only the canonical NUMERIC result type.'
)

const helperBlock = blockBetween(
  apiSource,
  'getPqcProcesses: async (activeOrderId: number)',
  'getFrontlineEmployeeCandidates: async'
)
assert.match(
  helperBlock,
  /url:\s*\x60\/mes\/pro\/feedback\/frontline\/device-account\/pqc\/active-order\/processes\x60/,
  'getPqcProcesses must call the frozen active-order process endpoint.'
)
assert.match(
  helperBlock,
  /params:\s*{\s*activeOrderId\s*}/,
  'getPqcProcesses must send activeOrderId as the only request identity.'
)
assert.match(
  helperBlock,
  /return projectFrontlinePqcProcesses\(processes\)/,
  'getPqcProcesses must apply the stable process and task projection to the network response.'
)
assert.doesNotMatch(
  apiSource,
  /getFrontlinePqcActiveOrderProcesses|workOrderId:\s*number\s*\n\s*routeId:\s*number[\s\S]{0,220}pqc\/active-order\/processes/,
  'The frontend API must not export or retain the legacy workOrderId + routeId process helper.'
)
assert.doesNotMatch(
  apiSource + employeeContextSource,
  /getFrontlinePqcActiveOrderProcesses/,
  'The old helper identity must not remain in the API or its active-order consumer.'
)
assert.match(
  employeeContextSource,
  /buildFrontlinePqcActiveOrderProcessCacheKey[\s\S]{0,180}String\(activeOrder\.activeOrderId\)/,
  'The PQC process cache must use activeOrderId as its only order identity.'
)
const clearPqcSelectionBlock = blockBetween(
  employeeContextSource,
  'export const clearFrontlinePqcSelectionIfUnavailable =',
  'const pruneFrontlinePqcProcessCache ='
)
assert.match(
  clearPqcSelectionBlock,
  /activeOrder\.activeOrderId === selectedActiveOrder\.activeOrderId/,
  'Refreshing active orders must retain selection by activeOrderId.'
)
assert.doesNotMatch(
  clearPqcSelectionBlock,
  /workOrderId|routeId/,
  'Refreshing active orders must not collapse duplicate work-order and route rows.'
)
assert.doesNotMatch(
  pageSource,
  /getProcessPqcTaskSnapshot/,
  'The page must not synthesize a task option from process-level fields when the formal task option is absent.'
)
assert.doesNotMatch(
  pageSource,
  /withPqcTaskOption|process\.(?:pqcTaskId|inspectionType|businessDate|shiftCode|roundNo|plannedInspectionQuantity)/,
  'The page must keep selected task identity locally and must not write task-option fields into the process DTO.'
)
assert.match(
  pageSource,
  /key:\s*buildFrontlineActiveOrderPickerKey\(order\)/,
  'The actual order picker must key rows by the formal activeOrderId identity helper.'
)
assert.match(
  pageSource,
  /active:\s*isSameFrontlineActiveOrder\(order,\s*deviceState\.selectedActiveOrder\)/,
  'The actual order picker must compare active rows by the formal activeOrderId identity helper.'
)

assert.match(
  projectionSource,
  /businessDate[\s\S]*ruleSort[\s\S]*roundNo[\s\S]*pqcTaskId/,
  'The production projection must encode the frozen stable task ordering fields.'
)
assert.match(
  employeeContextSource,
  /pqcActiveOrderSelectionRequestToken:\s*number/,
  'The active-order consumer must own a request token for stale response isolation.'
)
assert.match(
  employeeContextSource,
  /const requestToken = \+\+state\.pqcActiveOrderSelectionRequestToken[\s\S]*if \(state\.pqcActiveOrderSelectionRequestToken !== requestToken\)/,
  'The active-order consumer must reject an older process response after a newer order selection starts.'
)
assert.doesNotMatch(
  projectionSource + apiSource,
  /createFrontlinePqcProjectionLoader|FRONTLINE_PQC_RULE_KEY_ORDER/,
  'Unused duplicate ordering and stale-loader abstractions must not remain in production code.'
)

assert.match(
  qaTemplateSource,
  /export type QaInspectionRegulationInspectionRuleKey = 'FIRST' \| 'PATROL_AM' \| 'PATROL_PM' \| 'FINAL'/,
  'QA regulation frontend API must expose the same formal inspection rule-key union.'
)
assert.match(
  qaTemplateSource,
  /key:\s*QaInspectionRegulationInspectionRuleKey/,
  'QA regulation inspection type rule key must be typed as the formal rule-key union.'
)
assert.match(
  qaTemplateSource,
  /export type QaInspectionRegulationResultType = 'BOOLEAN' \| 'NUMERIC' \| 'TEXT'/,
  'QA regulation frontend API must expose the formal item resultType union.'
)
assert.match(
  qaTemplateSource,
  /resultType:\s*QaInspectionRegulationResultType/,
  'QA regulation item resultType must use the formal union.'
)

const { projectFrontlinePqcProcesses } = loadTypeScriptModule(projectionSource, 'pqcProjection.ts')

const rule = (key, inspectionType) => ({
  key,
  inspectionType,
  label: key,
  roundLabel: key,
  required: true,
  taskRule: key,
  releaseGate: key
})
const task = (pqcTaskId, inspectionRuleKey, ruleSort, businessDate, taskStatus) => ({
  pqcTaskId,
  qaProcessId: 51,
  regulationVersionId: 61,
  inspectionRuleKey,
  inspectionType: inspectionRuleKey.startsWith('PATROL') ? 'PATROL' : inspectionRuleKey,
  businessDate,
  shiftCode: inspectionRuleKey,
  roundNo: 1,
  plannedInspectionQuantity: pqcTaskId,
  taskStatus,
  ruleSort,
  inspectionTypeRule: rule(
    inspectionRuleKey,
    inspectionRuleKey.startsWith('PATROL') ? 'PATROL' : inspectionRuleKey
  ),
  inspectionItems: []
})
const reverseOrderedTasks = [
  task(404, 'FINAL', 40, [2026, 8, 14], 'CANCELLED'),
  task(303, 'PATROL_PM', 30, [2026, 8, 13], 'CONFIRMED'),
  task(202, 'PATROL_AM', 20, [2026, 8, 13], 'SUBMITTED'),
  task(101, 'FIRST', 10, [2026, 8, 13], 'PENDING')
]
const processFixture = {
  activeOrderId: 1,
  qaProcessId: 51,
  qaProcessSort: 1,
  inspectionTypeRules: [],
  inspectionItems: [],
  taskSummary: {
    state: 'MIXED',
    totalCount: 4,
    pendingCount: 1,
    submittedCount: 1,
    confirmedCount: 1,
    cancelledCount: 1
  },
  pqcTaskOptions: reverseOrderedTasks,
  productionSubmitCandidates: []
}
const [projectedProcess] = projectFrontlinePqcProcesses([processFixture])
assert.deepEqual(
  projectedProcess.pqcTaskOptions.map(({ inspectionRuleKey }) => inspectionRuleKey),
  ['FIRST', 'PATROL_AM', 'PATROL_PM', 'FINAL'],
  'Reverse network order must project to FIRST, AM patrol, PM patrol, FINAL without merging AM/PM.'
)
assert.deepEqual(
  projectedProcess.pqcTaskOptions.map(({ taskStatus, plannedInspectionQuantity, pqcTaskId }) => [
    taskStatus,
    plannedInspectionQuantity,
    pqcTaskId
  ]),
  [
    ['PENDING', 101, 101],
    ['SUBMITTED', 202, 202],
    ['CONFIRMED', 303, 303],
    ['CANCELLED', 404, 404]
  ],
  'Stable projection must preserve each rule task status, quantity, and task identity.'
)
assert.deepEqual(
  projectedProcess.pqcTaskOptions.map(({ businessDate }) => businessDate),
  ['2026-08-13', '2026-08-13', '2026-08-13', '2026-08-14'],
  'Jackson LocalDate tuples must normalize to the formal YYYY-MM-DD page contract before sorting.'
)
const [stringDateProcess] = projectFrontlinePqcProcesses([{
  ...processFixture,
  pqcTaskOptions: [task(505, 'FINAL', 40, '2026-08-15', 'PENDING')]
}])
assert.equal(
  stringDateProcess.pqcTaskOptions[0].businessDate,
  '2026-08-15',
  'A valid ISO business date must preserve the normalized page contract.'
)
assert.throws(
  () =>
    projectFrontlinePqcProcesses([
      {
        ...processFixture,
        pqcTaskOptions: [task(999, 'FIRST', 10, [2026, 2, 30], 'PENDING')]
      }
    ]),
  /PQC任务业务日期无效.*pqcTaskId=999/,
  'An invalid business date must fail explicitly with the task identity.'
)

const verifyRealConsumerIdentityAndStaleIsolation = async () => {
  const pendingLoads = new Map()
  const employeeContext = loadTypeScriptModule(
    employeeContextSource,
    'frontlineDeviceEmployeeContext.ts',
    (request) => {
      assert.equal(request, '@/api/mes/pro/feedback')
      return {
        ProFeedbackApi: {
          getPqcProcesses: (activeOrderId) => new Promise((resolve) => {
            pendingLoads.set(activeOrderId, resolve)
          })
        }
      }
    }
  )
  const duplicateOrderA = {
    activeOrderId: 1,
    workOrderId: 91,
    routeId: 81,
    workOrderCode: 'ORDER-A'
  }
  const duplicateOrderB = {
    activeOrderId: 2,
    workOrderId: 91,
    routeId: 81,
    workOrderCode: 'ORDER-B'
  }
  assert.notEqual(
    employeeContext.buildFrontlineActiveOrderPickerKey(duplicateOrderA),
    employeeContext.buildFrontlineActiveOrderPickerKey(duplicateOrderB),
    'Two active orders with the same workOrderId and routeId must retain different picker keys.'
  )
  assert.equal(employeeContext.isSameFrontlineActiveOrder(duplicateOrderA, duplicateOrderB), false)
  assert.equal(employeeContext.isSameFrontlineActiveOrder(duplicateOrderB, duplicateOrderB), true)

  const state = employeeContext.createFrontlineDeviceEmployeeState()
  const staleRequest = employeeContext.selectFrontlinePqcActiveOrder(state, duplicateOrderA)
  const currentRequest = employeeContext.selectFrontlinePqcActiveOrder(state, duplicateOrderB)
  pendingLoads.get(2)([{ ...projectedProcess, activeOrderId: 2 }])
  assert.deepEqual(await currentRequest, [{ ...projectedProcess, activeOrderId: 2 }])
  pendingLoads.get(1)([{ ...processFixture, activeOrderId: 1 }])
  await assert.rejects(
    staleRequest,
    employeeContext.FrontlinePqcStaleActiveOrderSelectionError,
    'The real active-order consumer must reject an older response after a newer selection starts.'
  )
  assert.equal(state.selectedActiveOrder.activeOrderId, 2)
  assert.deepEqual(state.processOptions, [{ ...projectedProcess, activeOrderId: 2 }])
  assert.equal(state.lastError, undefined)
  assert.equal(state.loadingProcesses, false)
}

verifyRealConsumerIdentityAndStaleIsolation()
  .then(() => {
    console.log(
      'PASS: frontline PQC process contract preserves full DTOs, formal identity, stable AM/PM order, and stale isolation'
    )
  })
  .catch((error) => {
    console.error(error)
    process.exitCode = 1
  })
