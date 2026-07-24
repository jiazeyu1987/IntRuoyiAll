const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')
const tableDataIndex = source.indexOf(':data="scheduleOrderList"')
assert(tableDataIndex >= 0, '排产工单主表必须绑定 scheduleOrderList。')

const tableStart = source.lastIndexOf('<el-table', tableDataIndex)
const tableEnd = source.indexOf('>', tableDataIndex)
assert(tableStart >= 0 && tableEnd > tableStart, '排产工单主表必须存在 el-table 起始标签。')

const tableOpenTag = source.slice(tableStart, tableEnd)

assert(
  tableOpenTag.includes('@header-dragend="handleScheduleOrderHeaderDragend"'),
  '排产工单主表必须监听 header-dragend 以保存拖拽后的列宽。'
)

assert(
  /\sborder(?:\s|>|$)/.test(tableOpenTag),
  '排产工单主表必须开启 Element Plus border 模式，否则列宽拖拽手柄不可用。'
)

assert(
  tableOpenTag.includes('data-user-table-key="mes.pro.scheduleOrder.main"'),
  '排产工单主表必须声明稳定 tableKey，确保列宽按账号持久化。'
)

console.log('PASS: MES schedule order main table supports column resize persistence')
