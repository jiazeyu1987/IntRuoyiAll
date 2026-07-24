const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message)
  }
}

const historyPagePath = path.join(
  root,
  'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'
)

if (!fs.existsSync(historyPagePath)) {
  throw new Error('缺少 eDHR 历史批记录页面 BatchRecordHistoryPage.vue')
}

const historyPage = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const router = read('src/router/modules/remaining.ts')

assertIncludes(historyPage, '历史批记录', '历史批记录页面必须显示明确标题')
assertIncludes(
  historyPage,
  'EDHR_BATCH_STATUS_ARCHIVED',
  '历史批记录列表必须固定使用已归档状态过滤'
)
assertIncludes(
  historyPage,
  'getEdhrBatchExecutionPage',
  '历史批记录页面必须使用 eDHR 批次分页接口'
)
assertIncludes(
  historyPage,
  'getEdhrBatchReviewTimeline',
  '历史批记录页面必须点击批次后加载 review-timeline'
)
assertIncludes(
  historyPage,
  'EdhrExecutionReadonlyForm',
  '历史批记录页面必须复用只读模板表格组件'
)
assertIncludes(
  historyPage,
  'selectedExecution.attachmentSummaries',
  '历史批记录页面必须消费 timeline 附件摘要'
)
assertIncludes(
  historyPage,
  'edhr-batch-history__attachment-section',
  '历史批记录页面必须展示附件证据区域'
)
assertIncludes(
  historyPage,
  '附件证据',
  '历史批记录页面必须展示附件证据标题'
)
assertIncludes(
  historyPage,
  'sha256',
  '历史批记录页面必须展示附件 sha256'
)
assertIncludes(
  historyPage,
  'attachmentHash',
  '历史批记录页面必须展示附件 hash'
)
assertIncludes(
  historyPage,
  'edhr-batch-history__batch-list',
  '历史批记录页面必须提供左侧历史批次列表'
)
assertIncludes(
  historyPage,
  'edhr-batch-history__process-list',
  '历史批记录页面必须提供右侧工序导航'
)
assertIncludes(
  historyPage,
  'selectedExecution',
  '历史批记录页面必须支持点击工序切换单张模板表格'
)
assertIncludes(
  historyPage,
  'signatureRecords',
  '历史批记录页面必须使用接口返回的单表电子签名记录'
)
assertIncludes(
  historyPage,
  ':signature-records="selectedExecution.signatureRecords"',
  '历史批记录页面必须把单表电子签名记录传入只读模板组件'
)

assertIncludes(
  router,
  'pro/feedback/edhr-batch-history',
  '路由必须新增历史批记录入口'
)
assertIncludes(router, 'BatchRecordHistoryPage.vue', '历史批记录路由必须指向新页面')
assertIncludes(router, "title: '历史批记录'", '历史批记录路由标题必须正确')

console.log('eDHR batch history static checks passed')
