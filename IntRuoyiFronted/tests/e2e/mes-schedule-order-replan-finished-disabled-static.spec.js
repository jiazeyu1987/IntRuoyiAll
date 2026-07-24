const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), '排产工单页面必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes('const SCHEDULE_ORDER_STATUS_CANCELED = 4'),
  '前端必须显式识别已取消排产工单状态。'
)
assert.ok(
  source.includes('const isScheduleOrderReplanable = (row: MesProScheduleOrderVO) =>'),
  '前端必须集中定义排产工单是否可重排。'
)
assert.ok(
  source.includes('row.status !== SCHEDULE_ORDER_STATUS_FINISHED') &&
    source.includes('row.status !== SCHEDULE_ORDER_STATUS_CANCELED'),
  '完成和取消排产工单必须不可重排。'
)
assert.ok(
  source.includes('return isScheduleOrderReplanable(row)'),
  '表格选择框必须复用可重排判断。'
)
assert.ok(
  source.includes('rows.filter((item) => isScheduleOrderReplanable(item))'),
  '选择变化时必须过滤掉完成、取消、冻结等不可重排工单。'
)

console.log('PASS: MES schedule order replan finished disabled static contract')
