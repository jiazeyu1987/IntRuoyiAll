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
  '批次详情 API 必须提供只读任务预览请求。'
)

assert(
  pageSource.includes('selectedTaskPreview') &&
    pageSource.includes('loadTaskPreview') &&
    pageSource.includes(':form-view-model="selectedTaskPreview.formViewModel"'),
  '未开始普通表单必须加载并渲染只读预览。'
)

assert(
  pageSource.includes('taskPreviewLoading') &&
    pageSource.includes('taskPreviewError') &&
    pageSource.includes('当前节点没有可预览的批记录表单'),
  '只读预览必须具备加载、错误和无模板状态。'
)

assert(
  !pageSource.includes('当前表单尚未形成已填写内容，请在右侧工序表单中打开填写'),
  '管理员只读视图不得继续提示通过打开填写才能查看表单。'
)

console.log('eDHR batch admin preview runtime fix static contract passed')
