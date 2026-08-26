const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const apiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordcelllink/index.ts')
const backendServiceFile = resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProBatchRecordCellLinkServiceImpl.java'
)
const feedbackPayloadFile = resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesProFrontlineFeedbackPayloadReqVO.java'
)
const routeFlowDesignerFile = resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const batchRecordFormListFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordformlist/index.vue')

const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')
const backendService = readFileSync(backendServiceFile, 'utf-8')
const backendPickListSourceService = readFileSync(resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordcelllink/MesProductionPickListSourceServiceImpl.java'
), 'utf-8')
const feedbackPayload = readFileSync(feedbackPayloadFile, 'utf-8')
const routeFlowDesigner = readFileSync(routeFlowDesignerFile, 'utf-8')
const batchRecordFormList = readFileSync(batchRecordFormListFile, 'utf-8')

for (const token of [
  "const SOURCE_TYPE_PROCESS_POOL_REPORT = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_NAME = '报工数据'",
  'label="报工数据"',
  ':value="PROCESS_POOL_REPORT_SOURCE_REPORT_ID"',
  'processPoolReportSourceFields.value = (data.sourceFields || []).filter',
  'const isProcessPoolReportSelected = computed(() => sourceReportId.value === PROCESS_POOL_REPORT_SOURCE_REPORT_ID)',
  'sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT',
  'filteredProcessPoolReportSourceFields.value',
  'field.routeProcessId === targetRouteProcessId',
  'buildSourceFieldCells(filteredProcessPoolReportSourceFields.value, PROCESS_POOL_REPORT_SOURCE_REPORT_ID',
  'PROCESS_POOL_REPORT_AGGREGATION_OPTIONS',
  "type ProcessPoolSourceValueType = 'NUMBER' | 'STRING' | 'BOOLEAN'",
  'v-if="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT"',
  'v-model="aggregationStrategy"',
  'aggregationStrategy: isProcessPoolReportSource ? aggregationStrategy.value : undefined'
]) {
  assert.ok(page.includes(token), `page misses process-pool report mapping token: ${token}`)
}

for (const token of [
  "const SOURCE_TYPE_PQC_AGGREGATE_DETAIL = 'PQC_AGGREGATE_DETAIL'",
  "const PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID = 'PQC_AGGREGATE_DETAIL'",
  "const PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME = '一线PQC数据'",
  'label="一线PQC数据"',
  ':value="PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID"',
  'ProRouteProcessApi.getRouteProcessListByRoute',
  'const routeProcesses = ref',
  'const selectedPqcRouteProcessId = ref',
  'const selectedPqcRouteProcess = computed',
  'batch-record-cell-link__pqc-process-select',
  '选择工序',
  'pqcAggregateSourceFields.value = (data.sourceFields || []).filter',
  'const isPqcAggregateSelected = computed(() => sourceReportId.value === PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID)',
  'sourceType.value === SOURCE_TYPE_PQC_AGGREGATE_DETAIL',
  'filteredPqcAggregateSourceFields.value',
  'selectedPqcRouteProcessId.value',
  'selectedPqcRouteProcess?.sort',
  'selectedPqcRouteProcess?.processName',
  'buildSourceFieldCells(filteredPqcAggregateSourceFields.value, PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID',
  'routeProcessId: routeProcessIdForContext',
  'async function loadRouteProcesses()',
  'const handlePqcProcessChange = async'
]) {
  assert.ok(page.includes(token), `page misses PQC aggregate mapping token: ${token}`)
}

for (const token of [
  '填写时间',
  '复核时间',
  '填写人签名',
  '复核人签名',
  '当前工序序号',
  '工序名称'
]) {
  assert.ok(page.includes(token), `PQC aggregate source panel must surface readable process/time/signature labels: ${token}`)
}

for (const token of [
  'data-process-pool-report-source-fields',
  'data-process-pool-report-field-count',
  'const currentProcessPoolReportSourceTitle = computed',
  "`${targetForm.value?.reportName || '当前工序'}的一线生产字段`",
  'const targetRouteProcessId = activeTargetRouteProcessId.value',
  'return processPoolReportSourceFields.value.filter((field) =>',
  'buildSourceFieldCells(filteredProcessPoolReportSourceFields.value, PROCESS_POOL_REPORT_SOURCE_REPORT_ID,',
  'currentProcessPoolReportSourceTitle.value'
]) {
  assert.ok(page.includes(token), `process-pool report source panel must refresh by selected target process: ${token}`)
}

for (const token of [
  'data-pqc-aggregate-source-fields',
  'data-pqc-aggregate-field-count',
  'const currentPqcAggregateSourceTitle = computed',
  '请选择工序后查看一线PQC字段',
  'const targetRouteProcessId = selectedPqcRouteProcessId.value',
  'return pqcAggregateSourceFields.value.filter((field) => field.routeProcessId === targetRouteProcessId)',
  'currentPqcAggregateSourceTitle.value'
]) {
  assert.ok(page.includes(token), `PQC aggregate source panel must refresh by selected target process: ${token}`)
}

assert.ok(api.includes('aggregationStrategy?: string'), 'API rule type must carry aggregationStrategy')
assert.ok(api.includes('routeProcessId?: number'), 'API form/source field types must carry routeProcessId for target-process filtering')
assert.ok(api.includes('routeProcessId?: number'), 'API context/save params must carry routeProcessId for shared process inspection forms')
for (const sourceField of [
  'actualEmployeeId',
  'serverSubmitTime',
  'selectedDevice.deviceName',
  'deviceParameterReadings.',
  'deviceParameterReadings." + code + ".value',
  'equipmentParameterRules.',
  'deviceMeteringValidity.inMeteringValidityPeriod',
  'clearanceConfirmations.workplace.confirmed'
]) {
  assert.ok(backendService.includes(sourceField),
    'process-pool report catalog must expose all production element source fields: ' + sourceField)
}
assert.ok(feedbackPayload.includes('private String textValue'),
  'frontline production selected/text parameter readings must preserve textValue for process-pool mapping')
assert.ok(!page.includes('报工数据字段手工输入'), 'source fields must come from backend formal field catalog, not manual input')

for (const token of [
  'routeId: String(props.routeId)',
  'routeProcessId: String(selectedRouteProcessId.value)'
]) {
  assert.ok(routeFlowDesigner.includes(token), `route process entry must preserve formal route context: ${token}`)
}

for (const token of [
  'const cellLinkRouteId = normalizeRouteQueryText(route.query.routeId)',
  'const cellLinkRouteProcessId = normalizeRouteQueryText(route.query.routeProcessId)',
  'routeId: cellLinkRouteId || undefined',
  'routeProcessId: cellLinkRouteProcessId || undefined',
  'targetReportId: cellLinkRouteProcessId ? row.reportId : undefined'
]) {
  assert.ok(batchRecordFormList.includes(token), `batch record form entry must forward formal process context: ${token}`)
}

for (const token of [
  'const requestedTargetRouteProcessId = parseNumber(route.query.routeProcessId)',
  "const requestedTargetReportId = String(route.query.targetReportId || '')",
  ':disabled="!hasFormalRouteProcessContext"',
  'form.routeProcessId === routeProcessIdForContext',
  'form.reportId === requestedTargetReportId'
]) {
  assert.ok(page.includes(token), `cell-link page must select the requested formal process target: ${token}`)
}

for (const token of [
  "const SOURCE_TYPE_PRODUCTION_PICK_LIST = 'PRODUCTION_PICK_LIST'",
  "const PRODUCTION_PICK_LIST_SOURCE_REPORT_NAME = '领料单数据'",
  'label="领料单数据"',
  ':value="PRODUCTION_PICK_LIST_SOURCE_REPORT_ID"',
  'productionPickListSourceFields.value = (data.sourceFields || []).filter',
  'filteredProductionPickListSourceFields.value',
  'buildSourceFieldCells(filteredProductionPickListSourceFields.value, PRODUCTION_PICK_LIST_SOURCE_REPORT_ID'
]) {
  assert.ok(page.includes(token), `page misses production pick-list mapping token: ${token}`)
}

for (const token of [
  'SOURCE_TYPE_PRODUCTION_PICK_LIST',
  'material.',
  'lotNumber',
  'selectListByProductionOrderNo',
  'sourceEntryId'
]) {
  assert.ok(backendService.includes(token) || backendPickListSourceService.includes(token),
    `backend misses production pick-list source contract: ${token}`)
}

for (const token of [
  'SOURCE_TYPE_PQC_AGGREGATE_DETAIL',
  'PQC_AGGREGATE_DETAIL_SOURCE_REPORT_NAME',
  'pqcAggregateSourceFields(scope, routeId, requestedRouteProcessId)',
  'requirePqcAggregateSourceField(scope, reqVO.getRouteId()',
  'Objects.equals(targetRouteProcessId, field.routeProcessId())',
  'selectedEquipmentNumber',
  'dccProjectCode',
  'inspectedAt',
  'reviewerUserId'
]) {
  assert.ok(backendService.includes(token), `backend misses PQC aggregate source contract: ${token}`)
}

console.log('batch-record-cell-link process-pool-report static contract passed')
