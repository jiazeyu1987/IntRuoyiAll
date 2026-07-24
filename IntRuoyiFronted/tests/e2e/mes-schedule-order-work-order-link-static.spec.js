const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const workOrderPath = path.join(root, 'src/views/mes/pro/workorder/index.vue')

assert(fs.existsSync(scheduleOrderPath), '排产工单页面必须存在。')
assert(fs.existsSync(workOrderPath), '生产工单页面必须存在。')

const scheduleOrderSource = fs.readFileSync(scheduleOrderPath, 'utf8')
const workOrderSource = fs.readFileSync(workOrderPath, 'utf8')

assert(
  workOrderSource.includes('queryParams.code = typeof route.query.code') &&
    workOrderSource.includes('route.query.code'),
  '生产工单页必须继续支持从路由 query.code 初始化筛选条件。'
)

const selectionColumn = scheduleOrderSource.indexOf('type="selection"')
const scheduleOrderCodeColumn = scheduleOrderSource.indexOf('label="排产工单号"', selectionColumn)
const sourceWorkOrderColumn = scheduleOrderSource.indexOf('label="来源生产工单号"', selectionColumn)
const productCodeColumn = scheduleOrderSource.indexOf('label="产品编号"', selectionColumn)

assert(
  selectionColumn >= 0 &&
    scheduleOrderCodeColumn > selectionColumn &&
    sourceWorkOrderColumn > scheduleOrderCodeColumn &&
    productCodeColumn > sourceWorkOrderColumn,
  '排产工单主表必须先展示排产工单号，再展示来源生产工单号，避免误解为生产工单可直接重排。'
)

const scheduleOrderCodeColumnEnd = scheduleOrderSource.indexOf('</el-table-column>', scheduleOrderCodeColumn)
assert(scheduleOrderCodeColumnEnd > scheduleOrderCodeColumn, '排产工单号列必须是完整 el-table-column。')
const scheduleOrderCodeColumnSource = scheduleOrderSource.slice(
  scheduleOrderCodeColumn,
  scheduleOrderCodeColumnEnd
)

assert(
  scheduleOrderCodeColumnSource.includes('prop="code"'),
  '排产工单号主列必须绑定排产工单 code 字段。'
)
assert(
  scheduleOrderCodeColumnSource.includes('{{ row.code ||'),
  '排产工单号主列必须显示 row.code。'
)

const sourceWorkOrderColumnEnd = scheduleOrderSource.indexOf('</el-table-column>', sourceWorkOrderColumn)
assert(sourceWorkOrderColumnEnd > sourceWorkOrderColumn, '来源生产工单号列必须是完整 el-table-column。')
const sourceWorkOrderColumnSource = scheduleOrderSource.slice(
  sourceWorkOrderColumn,
  sourceWorkOrderColumnEnd
)

assert(
  sourceWorkOrderColumnSource.includes('prop="erpWorkOrderCode"'),
  '来源生产工单号列必须绑定 erpWorkOrderCode 字段。'
)
assert(
  sourceWorkOrderColumnSource.includes('v-if="row.erpWorkOrderCode"'),
  '来源生产工单号列必须仅在 erpWorkOrderCode 存在时显示可点击入口。'
)
assert(
  sourceWorkOrderColumnSource.includes('@click="openWorkOrder(row)"'),
  '来源生产工单号列必须点击调用 openWorkOrder(row)。'
)
assert(
  sourceWorkOrderColumnSource.includes('{{ row.erpWorkOrderCode }}'),
  '来源生产工单号列必须显示 erpWorkOrderCode。'
)
assert(
  sourceWorkOrderColumnSource.includes('v-else') && sourceWorkOrderColumnSource.includes('--'),
  '来源生产工单号缺失时必须显示 --。'
)

const openWorkOrderStart = scheduleOrderSource.indexOf('const openWorkOrder = (row: MesProScheduleOrderVO)')
const openWorkOrderEnd = scheduleOrderSource.indexOf('\n}', openWorkOrderStart)
assert(
  openWorkOrderStart >= 0 && openWorkOrderEnd > openWorkOrderStart,
  '排产工单页必须定义 openWorkOrder(row) 跳转函数。'
)
const openWorkOrderSource = scheduleOrderSource.slice(openWorkOrderStart, openWorkOrderEnd)

assert(
  openWorkOrderSource.includes("path: '/mes/pro/work-order'"),
  'openWorkOrder 必须跳转到生产工单列表路径。'
)
assert(
  openWorkOrderSource.includes('query: { code: row.erpWorkOrderCode }'),
  'openWorkOrder 必须通过 query.code 传递 erpWorkOrderCode。'
)
assert(
  !openWorkOrderSource.includes('row.erpWorkOrderCode || row.code') &&
    !openWorkOrderSource.includes('row.code || row.erpWorkOrderCode'),
  'openWorkOrder 不得用排产工单号兜底生产工单号。'
)

console.log('PASS: schedule order work order link static contract')
