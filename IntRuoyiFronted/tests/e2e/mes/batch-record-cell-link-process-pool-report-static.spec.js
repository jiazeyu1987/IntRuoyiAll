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

const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')
const backendService = readFileSync(backendServiceFile, 'utf-8')
const feedbackPayload = readFileSync(feedbackPayloadFile, 'utf-8')

for (const token of [
  "const SOURCE_TYPE_PROCESS_POOL_REPORT = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_NAME = '报工数据'",
  '<el-option label="报工数据" :value="PROCESS_POOL_REPORT_SOURCE_REPORT_ID" />',
  'processPoolReportSourceFields.value = (data.sourceFields || []).filter',
  'const isProcessPoolReportSelected = computed(() => sourceReportId.value === PROCESS_POOL_REPORT_SOURCE_REPORT_ID)',
  'sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT',
  'filteredProcessPoolReportSourceFields.value',
  'field.routeProcessId === targetForm.value?.routeProcessId',
  'buildSourceFieldCells(filteredProcessPoolReportSourceFields.value, PROCESS_POOL_REPORT_SOURCE_REPORT_ID',
  'PROCESS_POOL_REPORT_AGGREGATION_OPTIONS',
  "type ProcessPoolSourceValueType = 'NUMBER' | 'STRING' | 'BOOLEAN'",
  'v-if="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT"',
  'v-model="aggregationStrategy"',
  'aggregationStrategy: isProcessPoolReportSource ? aggregationStrategy.value : undefined'
]) {
  assert.ok(page.includes(token), `page misses process-pool report mapping token: ${token}`)
}

assert.ok(api.includes('aggregationStrategy?: string'), 'API rule type must carry aggregationStrategy')
assert.ok(api.includes('routeProcessId?: number'), 'API form/source field types must carry routeProcessId for target-process filtering')
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

console.log('batch-record-cell-link process-pool-report static contract passed')
