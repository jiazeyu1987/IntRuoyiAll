const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const assertIncludes = (content, expected, message) => {
  if (!content.includes(expected)) {
    throw new Error(message)
  }
}

const historyPage = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const api = read('src/api/mes/pro/edhr/batchExecution.ts')

assertIncludes(
  api,
  'batchEvents?: EdhrBatchExecutionReviewBatchEvent[]',
  'review-timeline API 合同必须包含批次事件。'
)
assertIncludes(api, 'closeSignatureId?: number', '批次事件合同必须暴露关闭签名证据。')
assertIncludes(api, 'rejectSignatureId?: number', '批次事件合同必须暴露质量拒收签名证据。')
assertIncludes(api, 'rejectReason?: string', '批次事件合同必须暴露质量拒收原因。')
assertIncludes(
  api,
  'taskEvents?: EdhrBatchExecutionReviewTaskEvent[]',
  'review-timeline API 合同必须包含任务事件。'
)
assertIncludes(
  api,
  'signatureRecords?: EdhrBatchExecutionReviewSignatureRecord[]',
  'review-timeline API 合同必须包含签名记录。'
)
assertIncludes(
  api,
  'approvalRecords?: EdhrBatchExecutionReviewApprovalRecord[]',
  'review-timeline API 合同必须包含审批记录。'
)
assertIncludes(
  api,
  'archiveVersions?: EdhrBatchExecutionArchiveRespVO[]',
  'review-timeline API 合同必须包含归档版本。'
)
assertIncludes(
  api,
  'flowEvents?: EdhrBatchExecutionReviewFlowEvent[]',
  'review-timeline API 合同必须包含流程干预事件。'
)

assertIncludes(
  historyPage,
  'edhr-batch-history__unified-timeline',
  '历史批记录页面必须提供统一审计时间线区域。'
)
assertIncludes(historyPage, '统一时间线', '统一审计区域标题必须使用用户可读文案。')
assertIncludes(
  historyPage,
  'unifiedTimelineItems',
  '历史批记录页面必须把多个来源合并为统一时间线。'
)
assertIncludes(historyPage, 'batchEvents', '统一时间线必须消费批次事件。')
assertIncludes(historyPage, 'taskEvents', '统一时间线必须消费任务事件。')
assertIncludes(historyPage, 'signatureRecords', '统一时间线必须消费签名记录。')
assertIncludes(historyPage, 'approvalRecords', '统一时间线必须消费审批记录。')
assertIncludes(historyPage, 'archiveVersions', '统一时间线必须消费归档版本。')
assertIncludes(historyPage, 'flowEvents', '统一时间线必须消费流程干预事件。')
assertIncludes(historyPage, 'resolveBatchEventTimelineTitle', '统一时间线必须把批次关闭和质量拒收翻译成用户可读结论。')
assertIncludes(historyPage, '关闭签名ID=', '统一时间线必须展示批次关闭签名证据。')
assertIncludes(historyPage, '质量拒收签名ID=', '统一时间线必须展示质量拒收签名证据。')
assertIncludes(historyPage, '质量拒收', '统一时间线必须显式展示批次质量拒收。')
assertIncludes(historyPage, 'resolveSkippedTaskTimelineDescription', '统一时间线必须把特殊节点跳过提升为用户可读事件。')
assertIncludes(historyPage, 'resolveFlowInterventionTimelineTitle', '统一时间线必须把流程干预转换成用户可读动作标题。')
assertIncludes(historyPage, '签核证据哈希=', '统一时间线必须展示流程干预签名证据。')
assertIncludes(historyPage, '完整性校验=', '统一时间线必须展示流程干预完整性校验结果。')
assertIncludes(historyPage, 'skipSignatureId', '统一时间线必须展示特殊节点跳过签名证据。')
assertIncludes(historyPage, 'attachmentCount=', '统一时间线必须展示特殊节点跳过附件证据摘要。')
assertIncludes(
  historyPage,
  'timelineSourceSummaryItems',
  '历史批记录页面必须展示时间线数据来源计数，便于识别缺失项。'
)
assertIncludes(historyPage, "label: '流程干预'", '历史批记录页面必须单独统计流程干预来源。')
assertIncludes(
  historyPage,
  '数据来源',
  '统一时间线必须向用户说明数据来源，而不是只堆内部状态。'
)

assertIncludes(
  detailPage,
  'openBatchHistoryPage',
  '批次详情页必须提供跳转到批次历史/统一时间线的入口。'
)
assertIncludes(detailPage, '历史/时间线', '批次详情页入口文案必须清晰指向历史和时间线。')

console.log('PASS edhr-batch-unified-timeline-static')
