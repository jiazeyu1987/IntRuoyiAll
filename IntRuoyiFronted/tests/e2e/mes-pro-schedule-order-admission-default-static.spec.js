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
  /key:\s*'workOrderCode'[\s\S]*queryParamKey:\s*'workOrderCode'/.test(pageSource),
  'Admission quick filter must map workOrderCode to the explicit backend query parameter.'
)
assert(
  /key:\s*'productCode'[\s\S]*queryParamKey:\s*'productCode'/.test(pageSource),
  'Admission quick filter must map productCode to the explicit backend query parameter.'
)
assert(
  /key:\s*'admissionStatus'[\s\S]*type:\s*'select'[\s\S]*queryParamKey:\s*'admissionStatus'/.test(
    pageSource
  ),
  'Admission quick filter must map admissionStatus to the explicit backend query parameter.'
)
assert(
  /const resetWorkOrderAdmissionQuery = \(\) => \{[\s\S]*workOrderAdmissionQuickFilter\.updateState\([\s\S]*workOrderAdmissionQueryParams\.admissionStatus = DEFAULT_WORK_ORDER_ADMISSION_STATUS[\s\S]*delete workOrderAdmissionQueryParams\.quickFilter[\s\S]*handleWorkOrderAdmissionQuery\(\)/.test(
    pageSource
  ),
  'Admission diff reset must clear quick-filter UI state and restore READY_TO_ADMIT.'
)

console.log('PASS: MES schedule order admission default static contract')
