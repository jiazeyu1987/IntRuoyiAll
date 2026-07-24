const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

assert(fs.existsSync(pagePath), '排产工单页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const scheduleTableStart = pageSource.indexOf(':data="scheduleOrderList"')
const scheduleTableEnd = pageSource.indexOf('</el-table>', scheduleTableStart)

assert(scheduleTableStart >= 0 && scheduleTableEnd > scheduleTableStart, '排产工单主表必须存在。')

const scheduleTable = pageSource.slice(scheduleTableStart, scheduleTableEnd)

assert(scheduleTable.includes('label="数量/进度"'), '排产工单主表必须使用数量/进度复合列。')
assert(scheduleTable.includes('schedule-order-pool__quantity-progress'), '数量/进度列必须使用复合展示容器。')
assert(scheduleTable.includes('schedule-order-pool__quantity-grid'), '数量/进度列必须有总量、完成、未完成的固定网格。')
assert(scheduleTable.includes('formatQuantity(row.totalQuantity ?? row.quantity)'), '复合列必须展示总数量。')
assert(scheduleTable.includes('formatQuantity(row.completedQuantity)'), '复合列必须展示已完成数量。')
assert(scheduleTable.includes('formatQuantity(row.uncompletedQuantity)'), '复合列必须展示未完成数量。')
assert(scheduleTable.includes('formatPercent(row.progressPercent)'), '复合列必须展示进度百分比。')
assert(scheduleTable.includes('<el-progress'), '复合列必须保留进度条。')

for (const label of ['总数量', '已完成', '未完成', '进度']) {
  assert(!scheduleTable.includes(`label="${label}"`), `排产工单主表不应继续单独显示 ${label} 列。`)
}

console.log('PASS: MES schedule order density static contract')
