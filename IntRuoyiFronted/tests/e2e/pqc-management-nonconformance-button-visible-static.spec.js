const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8')
const reviewPagePath = path.join(root, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
const reviewPage = fs.readFileSync(reviewPagePath, 'utf8')
const entry = page.match(
  /const openPqcSubmissionNonconformanceReview =[\s\S]*?(?=const openProductionReject =)/
)
assert(entry, 'PQC管理必须提供不合格审查入口。')
const listQuery = reviewPage.match(/const buildReviewQuery =[\s\S]*?(?=const loadReviews =)/)
assert(listQuery, '统一不合格评审页必须提供列表查询。')

assert(
  /const canOpenPqcSubmissionNonconformanceReview = \(row: ProcessPoolTimelineEventVO\) =>\s*\n\s*canReviewSubmission\(row\) && Boolean\(row\.activeOrderId\)/.test(
    page
  ),
  'PQC管理不合格审查按钮可见性必须同时确认正式活跃订单来源。'
)

assert(
  !/const canOpenPqcSubmissionNonconformanceReview = \(row: ProcessPoolTimelineEventVO\) =>\s*\n\s*canReviewSubmission\(row\) && Boolean\(row\.batchExecutionId\)/.test(
    page
  ),
  'PQC管理不合格审查按钮不能因为 row.batchExecutionId 为空被隐藏。'
)

assert(
  /const query: Record<string, string> = \{\s*activeOrderId: String\(row\.activeOrderId\),\s*autoCreate: '1'\s*\}/.test(
    entry[0]
  ),
  'PQC管理不合格审查入口必须用活跃订单身份自动打开新建弹框。'
)

assert(
  !/\b(sourceId|sourceType|batchExecutionId)\b/.test(entry[0]),
  'PQC管理不合格审查入口必须以 activeOrderId 为正式来源，不能携带旧来源或批次筛选。'
)

assert(
  /entryActiveOrderId/.test(
    reviewPage
  ),
  '统一不合格评审页必须读取活跃订单入口身份。'
)

assert(
  /route\.query\.activeOrderId/.test(reviewPage) &&
    /:disabled="Boolean\(entryActiveOrderId\)"/.test(reviewPage) &&
    /activeOrderCandidates\.value\s*=\s*entryActiveOrderId\.value\s*\?[\s\S]*?candidates\.filter\(\(order\)\s*=>\s*order\.id\s*===\s*entryActiveOrderId\.value\)/.test(reviewPage),
  'PQC入口必须把路由活跃订单身份解析为锁定值，并只保留对应候选。'
)

assert(
  /:data-entry-active-order-id="entryActiveOrderId\s*\|\|\s*''"/.test(reviewPage) &&
    /:data-selected-active-order-id="selectedActiveOrderId\s*\|\|\s*''"/.test(reviewPage),
  '新建弹框必须暴露已解析及已选活跃订单身份供真实页面合同核对。'
)

assert(
  !/\b(sourceId|sourceType|batchExecutionId|activeOrderId|entrySourceId|entrySourceType|entryBatchExecutionId|entryActiveOrderId)\b/.test(listQuery[0]),
  '全部和进行中列表不能继承入口的来源、批次或活跃订单隐藏筛选。'
)

console.log('pqc-management-nonconformance-button-visible-static PASS')
