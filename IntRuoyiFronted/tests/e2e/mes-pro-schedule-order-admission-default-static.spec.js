const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  /const workOrderAdmissionQueryParams = reactive\(\{[\s\S]*admissionStatus:\s*undefined/.test(
    pageSource
  ),
  'Admission diff query params must not force a default admissionStatus before the user applies a condition.'
)

const admissionDefinitionsStart = pageSource.indexOf(
  'const workOrderAdmissionMultiFilterDefinitions: ListMultiFilterDefinition[] = ['
)
const admissionDefinitionsEnd = pageSource.indexOf('const replanDrawerVisible', admissionDefinitionsStart)
assert(
  admissionDefinitionsStart >= 0 && admissionDefinitionsEnd > admissionDefinitionsStart,
  'Admission diff must declare standard multi-filter definitions.'
)
const admissionDefinitions = pageSource.slice(admissionDefinitionsStart, admissionDefinitionsEnd)

for (const [key, label, queryParamKey] of [
  ['workOrderCode', '工单编码', 'workOrderCode'],
  ['productCode', '产品编号', 'productCode'],
  ['productName', '产品名称', 'productName'],
  ['productSpecification', '规格型号', 'productSpecification'],
  ['quantity', '总数量', 'quantity'],
  ['requestDate', '需求日期', 'requestDate'],
  ['admissionStatus', '入池状态', 'admissionStatus'],
  ['reasonCode', '不可排原因', 'reasonCode'],
  ['ownerRole', '建议处理', 'ownerRole']
]) {
  assert(
    new RegExp(`key:\\s*'${key}'[\\s\\S]*?label:\\s*'${label}'[\\s\\S]*?queryParamKey:\\s*'${queryParamKey}'`).test(
      admissionDefinitions
    ),
    `Admission multi-filter must map ${label} to ${queryParamKey}.`
  )
}

assert(
  /const workOrderAdmissionMultiFilter = useTableMultiFilter\([\s\S]*workOrderAdmissionMultiFilterDefinitions[\s\S]*workOrderAdmissionQueryParams[\s\S]*getWorkOrderAdmissionList/.test(
    pageSource
  ),
  'Admission diff reset/query must be owned by the standard multi-filter hook.'
)
assert(
  !/DEFAULT_WORK_ORDER_ADMISSION_STATUS|workOrderAdmissionQuickFilter|resetWorkOrderAdmissionQuery/.test(
    pageSource
  ),
  'Admission diff must not keep the old default READY_TO_ADMIT quick-filter flow.'
)

console.log('PASS: MES schedule order admission default static contract')
