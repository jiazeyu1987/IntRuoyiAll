const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/scheduler-workbench/index.vue')
const processWipPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue'
)
const pageSource = fs.readFileSync(pagePath, 'utf8')
const processWipSource = fs.readFileSync(processWipPath, 'utf8')

assert(
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.schedulerWorkbench\.processWip"/.test(
    processWipSource
  ),
  '排产员工作台工序列表必须继续通过 ProcessWipTable 使用标准列表模板。'
)

const requiredTemplateKeys = [
  'mes.pro.schedulerWorkbench.routeActiveOrders',
  'mes.pro.schedulerWorkbench.replan.orders',
  'mes.pro.schedulerWorkbench.replan.workOrders',
  'mes.pro.schedulerWorkbench.replan.processes',
  'mes.pro.schedulerWorkbench.replan.protectedTasks',
  'mes.pro.schedulerWorkbench.replan.materials',
  'mes.pro.schedulerWorkbench.replan.materialContributions',
  'mes.pro.schedulerWorkbench.replan.issues'
]

for (const tableKey of requiredTemplateKeys) {
  assert(
    pageSource.includes(`table-key="${tableKey}"`),
    `排产员工作台列表 ${tableKey} 必须使用 UnifiedListTemplate。`
  )
  assert(
    pageSource.includes(`data-user-table-key="${tableKey}"`),
    `排产员工作台列表 ${tableKey} 的内部表格必须声明 data-user-table-key。`
  )
}

const tableOpeningFor = (tableKey) => {
  const tableOpenings = pageSource
    .split('<el-table')
    .slice(1)
    .map((chunk) => `<el-table${chunk.split('>', 1)[0]}>`)
  return tableOpenings.find((opening) => opening.includes(`data-user-table-key="${tableKey}"`))
}

for (const tableKey of ['mes.pro.schedulerWorkbench.processWip', ...requiredTemplateKeys]) {
  const block = tableOpeningFor(tableKey)
  assert(block, `排产员工作台列表 ${tableKey} 必须渲染内部 el-table。`)
  assert(
    block.includes('data-user-table-column-explicit'),
    `排产员工作台列表 ${tableKey} 必须声明显式列宽。`
  )
  assert(
    /@header-dragend="[^"]+"/.test(block),
    `排产员工作台列表 ${tableKey} 必须绑定列宽拖拽保存。`
  )
}

const routeActivePane =
  pageSource.match(
    /<el-tab-pane label="工艺路线在制订单"[\s\S]*?<el-tab-pane label="排产逻辑"/
  )?.[0] || ''

assert(
  routeActivePane && !routeActivePane.includes('scheduler-workbench__route-active-list'),
  '工艺路线在制订单不得继续使用卡片列表，必须改为标准列表模板表格。'
)

console.log('PASS: scheduler workbench all lists use standard list template')
