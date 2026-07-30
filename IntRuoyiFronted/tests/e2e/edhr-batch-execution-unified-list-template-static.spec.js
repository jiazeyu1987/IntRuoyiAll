const fs = require('fs')
const path = require('path')
const assert = require('assert')

const pagePath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')
const actionsMatch = source.match(/<template #actions>([\s\S]*?)<\/template>/)
assert(actionsMatch, '批次执行列表必须保留 actions 插槽。')
const actionsSource = actionsMatch[1]

function expectIncludes(snippet, message) {
  assert(source.includes(snippet), message || `Expected source to include: ${snippet}`)
}

function expectNotIncludes(snippet, message) {
  assert(!source.includes(snippet), message || `Expected source not to include: ${snippet}`)
}

const requiredTemplateContracts = [
  '<UnifiedListTemplate',
  'table-key="mes.pro.edhrBatch.execution.main"',
  ':query-model="queryParams"',
  ':filter-definitions="edhrBatchQuickFilterDefinitions"',
  ':quick-filter-state="edhrBatchQuickFilter.state"',
  ':selected-filter-definition="edhrBatchQuickFilter.selectedDefinition.value"',
  ':operator-options="edhrBatchQuickFilter.operatorOptions.value"',
  ':columns="edhrBatchExecutionColumns"',
  ':column-saving="edhrBatchExecutionColumnSaving"',
  'v-model:page="queryParams.pageNo"',
  'v-model:limit="queryParams.pageSize"',
  '@update:quick-filter-state="edhrBatchQuickFilter.updateState"',
  '@quick-filter-query="edhrBatchQuickFilter.applyQuickFilter"',
  '@column-change="saveEdhrBatchExecutionColumnConfig"',
  '@column-reset="resetEdhrBatchExecutionColumnConfig"',
  '@pagination="getList"',
  '<template #actions>',
  '<template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">'
]

for (const contract of requiredTemplateContracts) {
  expectIncludes(contract)
}

expectIncludes(
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  '批次执行列表必须直接导入标准列表模板'
)
expectIncludes(
  "import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'",
  '字段显示配置入口必须保留在截图顶部操作区。'
)
expectNotIncludes(
  "import TableQuickFilter from '@/components/TableQuickFilter/index.vue'",
  '快速过滤应由标准列表模板统一承载'
)
expectNotIncludes('<TableQuickFilter', '页面模板不应再直接渲染快速过滤组件')
expectIncludes('<UserTableColumnSettings', '页面模板必须直接渲染字段设置组件')
expectNotIncludes('<Pagination', '分页应由标准列表模板统一承载')

expectIncludes(
  '<EdhrBatchRecordTabs active-tab="execution" />',
  '批次执行列表必须接入 eDHR 批记录共享页签。'
)
expectIncludes(
  "import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'",
  '批次执行列表必须导入 eDHR 批记录共享页签组件。'
)
expectNotIncludes(
  '<template #extra-filters>',
  '批次执行列表右侧额外筛选项已按截图要求隐藏，不应再渲染 extra-filters 插槽。'
)
expectNotIncludes(
  'class="edhr-batch-page__tabs"',
  '批次执行列表不得恢复旧的本地页签样式入口，应只使用共享页签组件。'
)
expectNotIncludes(
  'label-width="88px"',
  '批次执行列表隐藏额外筛选后不应保留仅服务筛选表单的 label-width 配置。'
)

const requiredActions = [
  '@click="openCreateDialog"',
  "v-hasPermi=\"['mes:pro-edhr-batch-execution:create']\""
]

for (const action of requiredActions) {
  assert(actionsSource.includes(action), `批次执行顶部操作区缺少动作：${action}`)
}

assert(!actionsSource.includes('@click="handleQuery"'), '批次执行顶部操作区不得渲染重复查询按钮。')
assert(!actionsSource.includes('@click="resetQuery"'), '批次执行顶部操作区不得渲染重置按钮。')
assert(!actionsSource.includes('@click="openReadinessDialog"'), '批次执行顶部操作区不得渲染演练预检按钮。')

const requiredTableContracts = [
  '<el-alert',
  '<el-table',
  'data-user-table-column-explicit',
  'data-user-table-key="mes.pro.edhrBatch.execution.main"',
  '@header-dragend="handleEdhrBatchExecutionHeaderDragend"'
]

for (const contract of requiredTableContracts) {
  expectIncludes(contract)
}

const requiredColumns = [
  "'batchExecutionCode'",
  "'workOrderCode'",
  "'currentProcess'",
  "'currentFillers'",
  "'product'",
  "'route'",
  "'status'",
  "'progress'",
  "'blockedCount'",
  "'updateTime'",
  "'operation'"
]

for (const column of requiredColumns) {
  expectIncludes(column, `缺少批次执行字段配置：${column}`)
}

expectIncludes(
  'label="最后更新时间" prop="updateTime"',
  '批次执行列表最后更新时间列必须读取后端 updateTime 字段。'
)
expectIncludes(
  "{ key: 'updateTime', label: '最后更新时间', width: 180 }",
  '批次执行字段配置必须将 updateTime 展示为最后更新时间。'
)

const preservedBusinessHandlers = [
  'openCreateDialog',
  'openReadinessDialog',
  'openDetail',
  'openFlowTraceDialog',
  'openOperationHistoryDialog',
  'handleDownloadArchiveByPreview',
  'handleEdhrBatchExecutionHeaderDragend'
]

for (const handler of preservedBusinessHandlers) {
  expectIncludes(handler, `批次执行业务处理函数丢失：${handler}`)
}

expectNotIncludes('localStorage.', '批次执行列表不得自行访问 localStorage，字段持久化应由既有 hook 管理')
expectNotIncludes('sessionStorage.', '批次执行列表不得自行访问 sessionStorage')

console.log('edhr batch execution unified list template static contracts passed')
