const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')
const mainTableStart = source.indexOf('<el-table')
const paginationStart = source.indexOf('<Pagination', mainTableStart)
assert(mainTableStart >= 0 && paginationStart > mainTableStart, '排产工单主列表表格模板必须存在。')

const mainTableSource = source.slice(mainTableStart, paginationStart)

assert(
  !mainTableSource.includes('label="排产编码"'),
  '排产工单主列表不得显示排产编码列，避免首列占用排产员视野。'
)

assert(
  !mainTableSource.includes('label="工单编码"'),
  '排产工单主列表不得显示工单编码/工单编号列，避免左侧仍暴露工单编号。'
)

assert(
  mainTableSource.includes('label="产品编号"'),
  '排产工单主列表首个业务数据列必须是产品编号。'
)

assert(
  mainTableSource.includes(':row-class-name="getScheduleOrderRowClassName"'),
  '排产工单主列表必须提供冻结行 class，确保冻结效果在整行层面可见。'
)

assert(
  mainTableSource.includes('schedule-order-pool__freeze-badge'),
  '冻结状态必须使用专门的醒目冻结徽标 class。'
)

assert(
  mainTableSource.includes('ep:lock') && mainTableSource.includes('schedule-order-pool__freeze-icon'),
  '冻结状态必须带锁图标，提升冻结识别度。'
)

assert(
  source.includes('schedule-order-pool__row--frozen'),
  '冻结行必须定义醒目行样式。'
)

assert(
  source.includes('schedule-order-pool__freeze-badge') && source.includes('schedule-order-pool__freeze-badge--active'),
  '冻结徽标必须区分已冻结和未冻结视觉权重。'
)

console.log('PASS: MES schedule order freeze visibility static contract')
