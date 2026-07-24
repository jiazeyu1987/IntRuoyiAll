const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert(
  pageSource.includes('<el-form-item label="批次号">') &&
    pageSource.includes('v-model="queryParams.batchCode"'),
  'Batch code filter must remain available after changing the list display column.'
)

assert(
  !pageSource.includes('<el-table-column label="批次" prop="batchCode"'),
  'The list table must no longer show the blue-box column as batch code.'
)

assert(
  pageSource.includes('<el-table-column label="当前工序"') &&
    pageSource.includes('row.currentProcessName ||') &&
    !pageSource.includes('row.currentProcessCode || row.currentProcessName'),
  'The blue-box column must display current process name, not process code.'
)

assert(
  apiSource.includes('currentProcessName?: string') &&
    apiSource.includes('currentProcessCode?: string'),
  'The eDHR batch execution page response type must expose current process fields.'
)
