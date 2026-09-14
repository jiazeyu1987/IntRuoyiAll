const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const traceDrawer = read('src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')
const changeTab = read('src/views/mes/pro/edhr/form-trace/FormTraceChangeTab.vue')
const batchTabs = read('src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const batchDetail = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const batchGraph = read('src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue')
const router = read('src/router/modules/remaining.ts')

assert.doesNotMatch(
  batchTabs,
  /label="历史批记录"|name="history"|edhr-batch-history/,
  'eDHR 批记录页签不能再展示或跳转独立“历史批记录”。'
)

assert.doesNotMatch(
  batchDetail,
  /label:\s*'历史同工序'|edhr-batch-history|openBatchHistoryPage/,
  '批次详情不能再提供历史同工序/历史批记录独立入口。'
)

assert.doesNotMatch(
  batchGraph,
  /history-record|历史批记录|edhr-batch-history/,
  '批记录页面关系图不能再展示“历史批记录”节点。'
)

assert.doesNotMatch(
  router,
  /path:\s*'pro\/feedback\/edhr-batch-history'|name:\s*'MesProEdhrBatchHistory'|title:\s*'历史批记录'/,
  '路由配置不能再暴露独立历史批记录页面入口。'
)

const changeDetailDialog = changeTab.match(
  /<Dialog title="电子批记录变更详情"[\s\S]*?<\/Dialog>/
)?.[0] || ''

assert.match(
  changeDetailDialog,
  /<el-tabs v-model="detailActiveTab"/,
  '表单追溯点“详情”后必须在详情弹窗内用页签组织信息。'
)

assert.match(
  changeDetailDialog,
  /<el-tab-pane label="批记录表单" name="recordForm"/,
  '表单追溯点“详情”后必须能直接看到“批记录表单”页签。'
)

assert.match(
  changeTab,
  /openSelectedChangeRecordForm/,
  '详情弹窗的“批记录表单”页签必须提供打开可视化批记录表单的动作。'
)

assert.match(
  traceDrawer,
  /import EdhrExecutionReadonlyForm from '@\/views\/mes\/pro\/edhr\/components\/EdhrExecutionReadonlyForm\.vue'/,
  '表单追溯详情必须复用批次执行同款只读可视化表单组件。'
)

assert.match(
  traceDrawer,
  /<el-tab-pane label="批记录表单" name="recordForm"[\s\S]*?<\/el-tab-pane>/,
  '表单追溯抽屉必须提供“批记录表单”页签承载可视化详情。'
)

const recordFormTab = traceDrawer.match(
  /<el-tab-pane label="批记录表单" name="recordForm"[\s\S]*?<\/el-tab-pane>/
)?.[0] || ''

assert.match(
  recordFormTab,
  /aria-label="表单追溯批记录工序"/,
  '批记录表单页签必须提供类似填写页的工序/表单导航，而不是纯文字详情。'
)

assert.match(
  recordFormTab,
  /<EdhrExecutionReadonlyForm[\s\S]*:form-view-model="selectedRecordExecution\.formViewModel"[\s\S]*:signature-records="selectedRecordExecution\.signatureRecords"/,
  '批记录表单页签必须把 review-timeline 中的快照 formViewModel 和签名记录传给只读表单。'
)

assert.match(
  traceDrawer,
  /executionSnapshotJson|sheetLayoutJson|cellValuesJson/,
  '表单追溯可视化详情必须显式依赖持久化执行快照、模板布局和单元格值，而不是当前 BATCH 配置。'
)

assert.doesNotMatch(
  traceDrawer,
  /<pre\b|JSON\.stringify\(|snapshotJson\s*\}\}/,
  '表单追溯详情不能把历史详情退化成纯 JSON 或纯文字快照。'
)

assert.doesNotMatch(
  traceDrawer,
  /getEdhrBatchExecutionPage\(|EDHR_BATCH_STATUS_ARCHIVED/,
  '表单追溯详情不能重新拉独立历史批记录列表，只能基于当前追溯上下文读取 review-timeline。'
)

console.log('PASS: eDHR form trace visual record detail static contract')
