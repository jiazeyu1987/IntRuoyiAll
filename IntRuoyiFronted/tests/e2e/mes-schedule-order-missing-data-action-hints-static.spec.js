const fs = require('fs')
const path = require('path')
const assert = require('assert')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  'MISSING_MATERIAL_LIST_HINT',
  'MISSING_CURRENT_PROCESS_HINT',
  '未查询到生产用料清单',
  '当前列表未解析出可显示的未完成工序',
  '仍可调整优先级、设置承诺交期和冻结/解冻',
  '入池与手动重排以正式排产检查结果为准',
  '该展示值不作为统一禁用判据',
  'schedule-order-pool__missing-value-hint',
  'schedule-order-pool__missing-value-popper',
  'ep:question-filled'
]) {
  assert(pageSource.includes(token), `Schedule order missing-data hint must contain ${token}.`)
}

assert(
  /<el-tooltip[\s\S]*?:content="MISSING_MATERIAL_LIST_HINT"[\s\S]*?<span[\s\S]*?schedule-order-pool__material-missing[\s\S]*?tabindex="0"[\s\S]*?:aria-label="MISSING_MATERIAL_LIST_HINT"[\s\S]*?>[\s\S]*?缺失[\s\S]*?ep:question-filled[\s\S]*?<\/span>[\s\S]*?<\/el-tooltip>/.test(
    pageSource
  ),
  'Missing production material list must expose a focusable icon tooltip without replacing the visible status.'
)

assert(
  /<el-tooltip[\s\S]*?:content="MISSING_CURRENT_PROCESS_HINT"[\s\S]*?<span[\s\S]*?schedule-order-pool__current-process-missing[\s\S]*?tabindex="0"[\s\S]*?:aria-label="MISSING_CURRENT_PROCESS_HINT"[\s\S]*?>[\s\S]*?-[\s\S]*?ep:question-filled[\s\S]*?<\/span>[\s\S]*?<\/el-tooltip>/.test(
    pageSource
  ),
  'Missing current process must expose a focusable icon tooltip without replacing the visible placeholder.'
)

const replanableMatch = pageSource.match(
  /const isScheduleOrderReplanable = \(row: MesProScheduleOrderVO\) => \{([\s\S]*?)\n\}/
)
assert(replanableMatch, 'Schedule order page must define the manual-replan row gate.')
assert(
  !/productionMaterialListCount|currentProcessId/.test(replanableMatch[1]),
  'Manual replan eligibility must not use missing material-list or current-process display fields.'
)

const admissionSelectableMatch = pageSource.match(
  /const isAdmissionRowSelectable = \(row: MesProScheduleOrderAdmissionDiffRowVO\) => \{([\s\S]*?)\n\}/
)
assert(admissionSelectableMatch, 'Schedule order page must define the admission row gate.')
assert(
  !/productionMaterialListCount|currentProcessId/.test(admissionSelectableMatch[1]),
  'Admission eligibility must remain based on the formal admission result only.'
)

assert(
  /:global\(\.schedule-order-pool__missing-value-popper\)[\s\S]*?max-width:\s*360px[\s\S]*?white-space:\s*normal/.test(
    pageSource
  ),
  'Long missing-data explanations must wrap in a constrained tooltip instead of spanning the table.'
)

console.log('PASS: schedule-order missing-data operation hints static contract')
