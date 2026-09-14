const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8')
const reviewPagePath = path.join(root, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
const reviewPage = fs.readFileSync(reviewPagePath, 'utf8')

assert(
  /const canOpenPqcSubmissionNonconformanceReview = \(row: ProcessPoolTimelineEventVO\) =>\s*\n\s*canReviewSubmission\(row\)/.test(
    page
  ),
  'PQC管理不合格审查按钮可见性只能跟随可复核PQC提交行，不能额外依赖 batchExecutionId。'
)

assert(
  !/const canOpenPqcSubmissionNonconformanceReview = \(row: ProcessPoolTimelineEventVO\) =>\s*\n\s*canReviewSubmission\(row\) && Boolean\(row\.batchExecutionId\)/.test(
    page
  ),
  'PQC管理不合格审查按钮不能因为 row.batchExecutionId 为空被隐藏。'
)

assert(
  /const query: Record<string, string> = \{\s*sourceType: SOURCE_TYPE_PQC_SUBMISSION,\s*sourceId: String\(row\.id\)\s*\}/.test(
    page
  ),
  'PQC管理不合格审查入口必须先用 sourceType/sourceId 进入统一评审页。'
)

assert(
  !/query\.batchExecutionId\s*=|batchExecutionId:\s*String\(row\.batchExecutionId\)/.test(page),
  'PQC管理不合格审查入口必须以 sourceId 为权威来源，不能携带可能失效的 batchExecutionId。'
)

assert(
  /const canCreateEntry = computed\(\s*\(\) =>\s*Boolean\(entryBatchExecutionId\.value\) \|\|\s*Boolean\(entrySourceId\.value\)\s*\)/.test(
    reviewPage
  ),
  '统一不合格评审页必须允许 PQC_SUBMISSION 只携带 sourceId 创建评审。'
)

assert(
  !/entryForm\.sourceType === SOURCE_TYPE_PQC_RELEASE && entrySourceId\.value/.test(reviewPage),
  '统一不合格评审页不能只允许 PQC_RELEASE sourceId 入口创建评审。'
)

console.log('pqc-management-nonconformance-button-visible-static PASS')
