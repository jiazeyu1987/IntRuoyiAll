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
    pageSource.includes('selectedExecution.value?.formViewModel || selectedEmptyTaskPreviewFormViewModel.value') &&
    !pageSource.includes('selectedExecution.value?.formViewModel || selectedTaskPreview.value?.formViewModel'),
  '主区域可用 task preview 渲染空表单壳，但不得直接读取 task preview 单元值。'
)

assert(
  pageSource.includes('getEdhrBatchTaskPreview') &&
    pageSource.includes('const selectedTaskPreview = ref<EdhrBatchExecutionTaskPreviewRespVO>()') &&
    pageSource.includes('const selectedEmptyTaskPreviewFormViewModel = computed<EdhrBatchExecutionReviewFormViewModel | undefined>(() =>') &&
    pageSource.includes('cellValuesJson: EMPTY_FORM_CELL_VALUES_JSON') &&
    pageSource.includes('signatureCellMarkers: []') &&
    !pageSource.includes(':form-view-model="selectedTaskPreview.formViewModel"'),
  '无已提交内容时必须加载正式预览模板渲染空表单，并清空草稿单元值和签名标记。'
)

assert(
  pageSource.includes(':form-view-model="selectedPreviewFormViewModel"') &&
    pageSource.includes(':signature-records="selectedPreviewSignatureRecords"') &&
    pageSource.includes('当前节点没有可预览的批记录表单') &&
    !pageSource.includes('暂无已提交批记录内容'),
  '没有已提交 execution 时，主区域必须优先显示空白表单壳；只有缺少可预览模板时才显示无法预览空态。'
)

console.log('eDHR batch admin submitted content static contract passed')
