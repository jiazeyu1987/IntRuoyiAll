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

for (const token of [
  'useUserTableColumns',
  'useTableQuickFilter',
  'mes.pro.schedulerWorkbench.processWip',
  'schedulerWorkbenchProcessWipColumns',
  'schedulerWorkbenchProcessWipQuickFilterDefinitions',
  'filteredProcessWipStatistics',
  'applyProcessWipQuickFilter'
]) {
  assert(pageSource.includes(token), `工序在制列表必须接入排产工单同款控件能力：${token}`)
}
assert(
  processWipTableSource.includes("import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'"),
  '工序在制拆分组件必须导入标准列表模板。'
)

assert(
  /<ProcessWipTable[\s\S]*@quick-filter-query="processWipQuickFilter\.applyQuickFilter"/.test(
    pageSource
  ) &&
    /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.schedulerWorkbench\.processWip"[\s\S]*@quick-filter-query="emit\('quickFilterQuery'\)"/.test(
      processWipTableSource
    ),
  '工序列表 Tab 必须通过标准列表模板渲染快速过滤能力并绑定查询。'
)

assert(
  /<ProcessWipTable[\s\S]*:columns="schedulerWorkbenchProcessWipColumns"[\s\S]*@column-change="saveProcessWipColumnConfig"[\s\S]*@column-reset="resetProcessWipColumnConfig"/.test(
    pageSource
  ) &&
    /<UnifiedListTemplate[\s\S]*:columns="columns"[\s\S]*@column-change="emit\('columnChange', \$event\)"[\s\S]*@column-reset="emit\('columnReset'\)"/.test(
      processWipTableSource
    ),
  '工序列表 Tab 必须通过标准列表模板渲染显示字段自动保存和重置能力。'
)

assert(
  /<ProcessWipTable[\s\S]*<template #table>[\s\S]*?<el-table/.test(
    pageSource
  ),
  '工序列表列设置和过滤控件必须由标准列表模板承载，表格必须放入模板表格插槽。'
)

assert(
  /<ProcessWipTable[\s\S]*:total="processWipTotal"[\s\S]*@update:page="processWipQuickFilterParams\.pageNo = \$event"[\s\S]*@update:limit="processWipQuickFilterParams\.pageSize = \$event"/.test(
    pageSource
  ),
  '工序列表必须接入标准列表模板分页状态。'
)

assert.doesNotMatch(
  pageSource,
  /@save="saveProcessWipColumnConfig"/,
  '工序列表显示字段不得继续依赖手动保存按钮。'
)

const processWipPaneMatch = pageSource.match(
  /<el-tab-pane[\s\S]*label="工序列表"[\s\S]*name="process-list"[\s\S]*?<\/el-tab-pane>/
)
assert.ok(processWipPaneMatch, '工序列表 Tab 必须存在。')
const processWipPaneSource = processWipPaneMatch[0]

assert.match(
  processWipPaneSource,
  /<template #actions>[\s\S]*<el-button[\s\S]*type="primary"[\s\S]*@click="openSchedulerSettingsDialog"[\s\S]*>\s*排产设置\s*<\/el-button>[\s\S]*<\/template>/,
  '排产设置按钮必须放入工序列表标准模板工具栏 actions 插槽。'
)

assert.doesNotMatch(
  pageSource,
  /scheduler-workbench__settings-entry-panel[\s\S]*openSchedulerSettingsDialog|scheduler-workbench__settings-entry-panel[\s\S]*>\s*排产设置\s*</,
  '顶部排产说明区不得承载排产设置按钮。'
)

assert.doesNotMatch(
  pageSource,
  /scheduler-workbench__process-wip-toolbar/,
  '工序列表接入标准列表模板后不得保留自建工具条样式。'
)

assert(
  /<el-table[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="mes\.pro\.schedulerWorkbench\.processWip"[\s\S]*:data="pagedProcessWipStatistics"[\s\S]*\bborder\b[\s\S]*@header-dragend="handleProcessWipHeaderDragend"/.test(
    pageSource
  ),
  '工序列表表格必须声明用户列配置 key、使用过滤后数据，并开启 border 后接入列宽拖拽。'
)

for (const key of [
  'processCode',
  'processName',
  'wipOrderCount',
  'shiftCapacityTotal',
  'shiftStatus',
  'unfinishedDemandQuantity',
  'estimatedCompletionTime',
  'todayFeedbackQuantity'
]) {
  assert(
    new RegExp(`v-if="isProcessWipColumnVisible\\('${key}'\\)"`).test(pageSource),
    `工序列表列必须支持显示字段控制：${key}`
  )
  assert(
    new RegExp(`getProcessWipColumnLayoutWidthString\\('${key}'`).test(pageSource),
    `工序列表列必须使用铺满布局列宽策略：${key}`
  )
}

assert(
  /const getProcessWipColumnLayoutWidthString = [\s\S]*getProcessWipColumnWidthString\(key, fallback\)/.test(
    pageSource
  ),
  '工序列表铺满布局策略必须继续复用已保存列宽。'
)

for (const field of ['processCode', 'processName', 'shiftStatus', 'estimatedCompletionTime']) {
  assert(
    new RegExp(`key: '${field}'`).test(pageSource),
    `工序列表快速过滤必须支持字段：${field}`
  )
}

assert.doesNotMatch(pageSource, /localStorage|sessionStorage/, '工序列表列配置不得使用本地存储兜底。')
assert.doesNotMatch(pageSource, /scheduler-workbench__process-wip-item/, '工序在制订单不得恢复卡片项。')
assert.match(pageSource, /const activeWipTab = ref\('process-list'\)/, '默认 Tab 必须仍为工序列表。')

console.log('PASS: MES scheduler workbench process WIP controls static contract')
