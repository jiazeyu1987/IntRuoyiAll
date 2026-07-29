const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pageSource = fs.readFileSync(
  path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts'),
  'utf8'
)

assert(
  apiSource.includes('export const getEdhrBatchTaskPreview') &&
    apiSource.includes("url: `${BATCH_EXECUTION_BASE_URL}/task/preview`"),
  '批次详情 API 可保留只读任务预览请求，但主区域不得再使用它冒充已提交内容。'
)

assert(
  pageSource.includes('const SUBMITTED_EXECUTION_REVIEW_STATUSES = new Set([2, 3, 4])') &&
    pageSource.includes('const submittedExecutionReviews = computed(() => executionReviews.value.filter(isSubmittedExecutionReview))'),
  '批记录管理员主区域必须显式识别已提交、已批准、填写完成的 execution review。'
)

assert(
  pageSource.includes('return submittedExecutionReviews.value.find') &&
    !pageSource.includes('return executionReviews.value.find((execution) => String(execution.executionId) === selectedExecutionId.value)'),
  'selectedExecution 必须只从已提交 execution review 中选择，不能读取草稿 execution。'
)

assert(
  pageSource.includes('const selectedPreviewFormViewModel = computed<EdhrBatchExecutionReviewFormViewModel | undefined>(') &&
    pageSource.includes('() => selectedExecution.value?.formViewModel') &&
    !pageSource.includes('selectedExecution.value?.formViewModel || selectedTaskPreview.value?.formViewModel'),
  '主区域辅助预览也只能读取已提交 execution 的 formViewModel，不能读取 task preview。'
)

assert(
  !pageSource.includes('getEdhrBatchTaskPreview') &&
    !pageSource.includes('selectedTaskPreview') &&
    !pageSource.includes('taskPreviewLoading') &&
    !pageSource.includes('taskPreviewError') &&
    !pageSource.includes(':form-view-model="selectedTaskPreview.formViewModel"'),
  '批次详情主区域不得调用 task preview、渲染空模板或草稿预览。'
)

assert(
  pageSource.includes('暂无已提交批记录内容'),
  '没有已提交 execution 时，主区域必须明确提示暂无已提交内容。'
)

console.log('eDHR batch admin submitted content static contract passed')
