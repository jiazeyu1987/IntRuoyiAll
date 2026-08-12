const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const apiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordcelllink/index.ts')

const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')

for (const token of [
  "const SOURCE_TYPE_PROCESS_POOL_REPORT = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'",
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_NAME = '报工数据'",
  '<el-option label="报工数据" :value="PROCESS_POOL_REPORT_SOURCE_REPORT_ID" />',
  'processPoolReportSourceFields.value = (data.sourceFields || []).filter',
  'const isProcessPoolReportSelected = computed(() => sourceReportId.value === PROCESS_POOL_REPORT_SOURCE_REPORT_ID)',
  'sourceType.value === SOURCE_TYPE_PROCESS_POOL_REPORT',
  'buildSourceFieldCells(processPoolReportSourceFields.value, PROCESS_POOL_REPORT_SOURCE_REPORT_ID',
  'PROCESS_POOL_REPORT_AGGREGATION_OPTIONS',
  'v-if="sourceType === SOURCE_TYPE_PROCESS_POOL_REPORT"',
  'v-model="aggregationStrategy"',
  'aggregationStrategy: isProcessPoolReportSource ? aggregationStrategy.value : undefined'
]) {
  assert.ok(page.includes(token), `page misses process-pool report mapping token: ${token}`)
}

assert.ok(api.includes('aggregationStrategy?: string'), 'API rule type must carry aggregationStrategy')
assert.ok(!page.includes('报工数据字段手工输入'), 'source fields must come from backend formal field catalog, not manual input')

console.log('batch-record-cell-link process-pool-report static contract passed')
