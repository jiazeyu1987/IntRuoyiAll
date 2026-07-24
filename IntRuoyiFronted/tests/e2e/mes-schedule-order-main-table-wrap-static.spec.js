const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes("new Set(['erpWorkOrderCode', 'productCode'])"),
  '主表必须按列识别工单编码、产品编号两列的换行显示，主列表不得再显示排产编码列。'
)

assert(
  source.includes('schedule-order-pool__main-table__cell--wrap'),
  '主表必须提供专门的换行 cell class。'
)

assert(
  source.includes('schedule-order-pool__main-table-text'),
  '主表长编码列必须使用统一的文本换行 class。'
)

console.log('PASS: MES schedule order main table wrap static contract')
