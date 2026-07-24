const fs = require('fs')
const path = require('path')
const assert = require('assert/strict')

const scheduleOrderViewPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/scheduleorder/index.vue'
)

const source = fs.readFileSync(scheduleOrderViewPath, 'utf8')

const mainTableStart = source.indexOf('<el-table')
const mainListEnd = source.indexOf('</ScheduleOrderMainList>', mainTableStart)
assert(mainTableStart >= 0 && mainListEnd > mainTableStart, '排产工单主列表表格模板必须存在。')

assert(
  source.includes("{ key: 'code', label: '排产工单号'") &&
    source.includes("{ key: 'erpWorkOrderCode', label: '来源生产工单号'") &&
    source.includes('TableQuickFilter') &&
    !source.slice(0, mainTableStart).includes('placeholder="请输入工单编码"'),
  '排产工单查询区必须以排产工单号为主口径，并把生产工单号标为来源字段。'
)

console.log('PASS mes-schedule-order-workorder-link-static')
