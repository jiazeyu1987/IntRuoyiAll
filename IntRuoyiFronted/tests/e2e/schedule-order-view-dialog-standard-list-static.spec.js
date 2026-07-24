const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const requiredTemplateKeys = [
  'mes.pro.scheduleOrder.processRoute',
  'mes.pro.scheduleOrder.feedbackHistory',
  'mes.pro.scheduleOrder.dailyCompare',
  'mes.pro.scheduleOrder.operationLog',
  'mes.pro.scheduleOrder.operationLogDiff'
]

for (const tableKey of requiredTemplateKeys) {
  assert(
    source.includes(`table-key="${tableKey}"`),
    `排产工单查看弹框列表 ${tableKey} 必须使用 UnifiedListTemplate。`
  )
  assert(
    source.includes(`data-user-table-key="${tableKey}"`),
    `排产工单查看弹框列表 ${tableKey} 的内部表格必须声明 data-user-table-key。`
  )
}

const tableOpeningFor = (tableKey) => {
  const tableOpenings = source
    .split('<el-table')
    .slice(1)
    .map((chunk) => `<el-table${chunk.split('>', 1)[0]}>`)
  return tableOpenings.find((opening) => opening.includes(`data-user-table-key="${tableKey}"`))
}

for (const tableKey of requiredTemplateKeys) {
  const opening = tableOpeningFor(tableKey)
  assert(opening, `排产工单查看弹框列表 ${tableKey} 必须渲染内部 el-table。`)
  assert(
    opening.includes('data-user-table-column-explicit'),
    `排产工单查看弹框列表 ${tableKey} 必须声明显式列宽。`
  )
  assert(
    /@header-dragend="[^"]+"/.test(opening),
    `排产工单查看弹框列表 ${tableKey} 必须绑定列宽拖拽保存。`
  )
}

const processDialogBlock =
  source.match(/<ScheduleOrderProcessDetail[\s\S]*?<\/ScheduleOrderProcessDetail>/)?.[0] || ''

assert(
  processDialogBlock.includes('openDailyCompareDialog') &&
    processDialogBlock.includes('openOperationLogDialog'),
  '查看弹框必须保留报工对比和操作追溯入口。'
)

assert(
  !/v-for="row in operationLogList"/.test(source),
  '操作追溯记录不得继续使用卡片 v-for 列表，必须改为标准列表模板表格。'
)

console.log('PASS: schedule order view dialog lists use standard list template')
