const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const processWipTablePath = path.join(
  repoRoot,
  'src/views/mes/pro/scheduler-workbench/components/ProcessWipTable.vue'
)

assert(fs.existsSync(pagePath), '排产员工作台页面必须存在。')
assert(fs.existsSync(processWipTablePath), '工序在制订单拆分组件必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const processWipTableSource = fs.readFileSync(processWipTablePath, 'utf8')
const combinedSource = `${pageSource}\n${processWipTableSource}`

assert.match(
  processWipTableSource,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '工序在制订单列表必须导入标准列表模板。'
)

assert.match(
  processWipTableSource,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.schedulerWorkbench\.processWip"/,
  '工序在制订单列表必须用稳定 tableKey 接入标准列表模板。'
)

for (const token of [
  ':query-model="processWipQuickFilterParams"',
  ':filter-definitions="schedulerWorkbenchProcessWipQuickFilterDefinitions"',
  ':quick-filter-state="processWipQuickFilter.state"',
  ':selected-filter-definition="processWipQuickFilter.selectedDefinition.value"',
  ':operator-options="processWipQuickFilter.operatorOptions.value"',
  ':columns="schedulerWorkbenchProcessWipColumns"',
  ':column-saving="processWipColumnSaving"',
  ':total="processWipTotal"',
  '@update:page="processWipQuickFilterParams.pageNo = $event"',
  '@update:limit="processWipQuickFilterParams.pageSize = $event"',
  '@update:quick-filter-state="processWipQuickFilter.updateState"',
  '@quick-filter-query="processWipQuickFilter.applyQuickFilter"',
  '@column-change="saveProcessWipColumnConfig"',
  '@column-reset="resetProcessWipColumnConfig"',
  '@pagination="handleProcessWipPagination"'
]) {
  assert(combinedSource.includes(token), `标准列表模板必须绑定工序在制列表能力：${token}`)
}

assert.match(
  combinedSource,
  /#table[\s\S]*<el-table[\s\S]*data-user-table-key="mes\.pro\.schedulerWorkbench\.processWip"[\s\S]*:data="pagedProcessWipStatistics"[\s\S]*\bborder\b[\s\S]*@row-click="openProcessWipOrders"[\s\S]*@header-dragend="handleProcessWipHeaderDragend"/,
  '工序在制表格必须放入模板表格插槽，并保留行点击、border 列宽拖拽和用户列配置 key。'
)

assert.match(
  combinedSource,
  /#table[\s\S]*<el-table[\s\S]*class="scheduler-workbench__process-wip-table"[\s\S]*>/,
  '工序在制表格必须保留专属样式类，确保列表容器铺满。'
)

assert.match(
  pageSource,
  /const processWipFlexibleColumnKey = computed[\s\S]*processName[\s\S]*estimatedCompletionTime[\s\S]*todayFeedbackQuantity/,
  '工序在制列表必须保留至少一个可伸缩列，避免右侧出现空白。'
)

assert.match(
  pageSource,
  /const getProcessWipColumnLayoutWidthString = [\s\S]*processWipFlexibleColumnKey\.value[\s\S]*return undefined[\s\S]*getProcessWipColumnWidthString/,
  '工序在制列表宽度策略必须让伸缩列不绑定固定 width，同时保留其他列的列宽配置。'
)

assert.match(
  pageSource,
  /label="工序名称"[\s\S]*:width="getProcessWipColumnLayoutWidthString\('processName', 160\)"[\s\S]*:min-width="getProcessWipColumnMinWidthString\('processName', 140\)"/,
  '工序名称列必须作为优先伸缩列参与铺满列表。'
)

assert.match(
  pageSource,
  /\.scheduler-workbench__process-wip-table[\s\S]*width: 100%;[\s\S]*\.scheduler-workbench__process-wip-table :deep\(\.el-table__inner-wrapper\)[\s\S]*width: 100%;/,
  '工序在制表格及内部 Element Plus 容器必须撑满父容器。'
)

for (const behavior of [
  'handleProcessWipNightShiftChange(row, Boolean($event))',
  'handleProcessWipPlannedStartDateChange(row, $event)',
  'row.plannedStartDateMixed'
]) {
  assert(combinedSource.includes(behavior), `模板改造必须保留工序在制行内维护行为：${behavior}`)
}

for (const computedName of ['processWipTotal', 'pagedProcessWipStatistics']) {
  assert.match(pageSource, new RegExp(`const ${computedName} = computed`), `必须提供 ${computedName} 支持模板分页。`)
}

assert.doesNotMatch(
  pageSource,
  /import TableQuickFilter from '@\/components\/TableQuickFilter\/index\.vue'/,
  '接入标准列表模板后页面不应再直接导入快速过滤组件。'
)

assert.doesNotMatch(
  pageSource,
  /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index\.vue'/,
  '接入标准列表模板后页面不应再直接导入显示字段组件。'
)

assert.doesNotMatch(
  combinedSource,
  /<TableQuickFilter[\s\S]*table-key="mes\.pro\.schedulerWorkbench\.processWip"/,
  '工序在制订单列表不得继续直接渲染快速过滤控件。'
)

assert.doesNotMatch(
  combinedSource,
  /<UserTableColumnSettings[\s\S]*:columns="schedulerWorkbenchProcessWipColumns"/,
  '工序在制订单列表不得继续直接渲染显示字段控件。'
)

assert.doesNotMatch(
  pageSource,
  /scheduler-workbench__process-wip-toolbar/,
  '标准列表模板接入后必须移除旧的工序在制自建工具条。'
)

assert.doesNotMatch(pageSource, /localStorage|sessionStorage/, '工序在制订单列表不得使用本地存储兜底。')

console.log('PASS: MES scheduler workbench process WIP unified list template static contract')
