const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes("const DEFAULT_WORK_ORDER_ADMISSION_STATUS = 'READY_TO_ADMIT'"),
  'Admission diff dialog must define READY_TO_ADMIT as the default admission status.'
)
assert(
  /admissionStatus:\s*DEFAULT_WORK_ORDER_ADMISSION_STATUS/.test(pageSource),
  'Admission diff query params must default admissionStatus to READY_TO_ADMIT instead of all.'
)
assert(
  /const resetWorkOrderAdmissionQuery = \(\) => \{[\s\S]*workOrderAdmissionQueryFormRef\.value\?\.resetFields\(\)[\s\S]*workOrderAdmissionQueryParams\.admissionStatus = DEFAULT_WORK_ORDER_ADMISSION_STATUS[\s\S]*handleWorkOrderAdmissionQuery\(\)/.test(
    pageSource
  ),
  'Admission diff reset must restore admissionStatus to READY_TO_ADMIT instead of all.'
)

console.log('PASS: MES schedule order admission default static contract')
