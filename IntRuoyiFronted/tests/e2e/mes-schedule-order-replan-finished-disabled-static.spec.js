const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
assert(fs.existsSync(pagePath), '排产工单页面必须存在。')
assert(fs.existsSync(apiPath), '排产工单 API 类型必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.ok(
  source.includes('const SCHEDULE_ORDER_STATUS_CANCELED = 4'),
  '前端必须显式识别已取消排产工单状态。'
)
assert.ok(
  source.includes('const isScheduleOrderReplanable = (row: MesProScheduleOrderVO) =>'),
  '前端必须集中定义排产工单是否可重排。'
)
const replanableMatch = source.match(
  /const isScheduleOrderReplanable = \(row: MesProScheduleOrderVO\) => \{([\s\S]*?)\n\}/
)
assert.ok(replanableMatch, '前端必须存在可独立核对的排产工单重排资格函数。')
const replanableSource = replanableMatch[1]
assert.ok(
  replanableSource.includes('Number(row.status)') &&
    replanableSource.includes('Number(row.sourceWorkOrderStatus)') &&
    replanableSource.includes('status !== SCHEDULE_ORDER_STATUS_FINISHED') &&
    replanableSource.includes('status !== SCHEDULE_ORDER_STATUS_CANCELED') &&
    replanableSource.includes('sourceWorkOrderStatus !== MesProWorkOrderStatusEnum.FINISHED') &&
    replanableSource.includes('sourceWorkOrderStatus !== MesProWorkOrderStatusEnum.CANCELED'),
  '完成和取消排产工单、完成和取消来源生产工单都必须不可重排。'
)
assert.ok(
  apiSource.includes('sourceWorkOrderStatus?: number'),
  '排产工单主列表 API 类型必须承载来源生产工单状态。'
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
