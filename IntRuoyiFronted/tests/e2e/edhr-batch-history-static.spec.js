const fs = require('fs')
const path = require('path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message)
  }
}

const traceDrawer = read('src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')
const batchTabs = read('src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const router = read('src/router/modules/remaining.ts')

assert.match(batchTabs, /label="历史追溯"\s+name="history"/, '批记录页签必须保留历史追溯入口。')
assert.match(router, /edhr-batch-history|MesProEdhrBatchHistory/, '路由必须保留历史追溯页面入口。')
assertIncludes(
  traceDrawer,
  '批记录表单',
  '表单追溯详情必须承载历史批记录可视化表单页签'
)
assertIncludes(
  traceDrawer,
  'getEdhrBatchReviewTimeline',
  '表单追溯详情必须使用 eDHR 批次复盘时间线'
)
assertIncludes(
  traceDrawer,
  'ActiveOrderSubmissionDetailPanel',
  '表单追溯详情必须复用活跃订单详情面板'
)
assertIncludes(
  traceDrawer,
  'getEdhrBatchActiveOrderDetail',
  '表单追溯详情必须按批次执行正式来源读取详情单据'
)
assertIncludes(
  traceDrawer,
  'selectedRecordExecution.attachmentSummaries',
  '表单追溯详情必须消费 timeline 附件摘要'
)
assertIncludes(
  traceDrawer,
  'edhr-form-trace-batch-trace__attachment-section',
  '表单追溯详情必须展示附件证据区域'
)
assertIncludes(
  traceDrawer,
  '附件证据',
  '表单追溯详情必须展示附件证据标题'
)
assertIncludes(
  traceDrawer,
  'sha256',
  '表单追溯详情必须展示附件 sha256'
)
assertIncludes(
  traceDrawer,
  'attachmentHash',
  '表单追溯详情必须展示附件 hash'
)
assertIncludes(
  traceDrawer,
  'edhr-form-trace-batch-trace__record-process-list',
  '表单追溯详情必须提供批记录工序导航'
)
assertIncludes(
  traceDrawer,
  'selectedRecordExecution',
  '表单追溯详情必须支持点击工序切换单张模板表格'
)
assertIncludes(
  traceDrawer,
  'signatureRecords',
  '表单追溯详情必须使用接口返回的单表电子签名记录'
)

console.log('eDHR batch record history moved into form trace static checks passed')
