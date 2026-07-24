const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const apiPath = path.join(repoRoot, 'src', 'api', 'mes', 'pro', 'edhr', 'batchExecution.ts')
const servicePath = path.join(
  repoRoot,
  '..',
  'ruoyi-vue-pro',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'service',
  'pro',
  'batchrecord',
  'MesProEdhrBatchExecutionServiceImpl.java'
)
const taskVoPath = path.join(
  repoRoot,
  '..',
  'ruoyi-vue-pro',
  'yudao-module-mes',
  'src',
  'main',
  'java',
  'cn',
  'iocoder',
  'yudao',
  'module',
  'mes',
  'controller',
  'admin',
  'pro',
  'batchrecord',
  'vo',
  'EdhrBatchExecutionTaskRespVO.java'
)

const detail = fs.readFileSync(detailPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const service = fs.readFileSync(servicePath, 'utf8')
const taskVo = fs.readFileSync(taskVoPath, 'utf8')

const previewStart = detail.indexOf('class="edhr-batch-detail__preview-header"')
assert.notEqual(previewStart, -1, '批次详情页必须保留顶部预览 header。')
const previewEnd = detail.indexOf('class="edhr-batch-detail__preview-carrier"', previewStart)
assert.ok(previewEnd > previewStart, '必须能定位顶部预览 header 的版本显示区域。')
const previewHeader = detail.slice(previewStart, previewEnd)

assert.ok(
  taskVo.includes('private String batchRecordVersionNo;'),
  '后端任务响应 VO 必须返回当前表单版本号 batchRecordVersionNo。'
)
assert.ok(
  service.includes('buildBatchRecordVersionNoMap(tasks)') &&
    service.includes('.setBatchRecordVersionNo(batchRecordVersionNoMap.get(task.getBatchRecordVersionId()))'),
  '批次详情任务响应必须按 batchRecordVersionId 映射真实版本号，不得让前端用 ID 或固定值代替版本号。'
)
assert.ok(
  api.includes('batchRecordVersionNo?: string'),
  '前端 EdhrBatchExecutionTaskRespVO 类型必须声明 batchRecordVersionNo。'
)
assert.ok(
  detail.includes('const currentFormVersionNo = computed') &&
    detail.includes('selectedTaskForEvidence.value?.batchRecordVersionNo'),
  '批次详情页必须从当前选中表单任务读取 batchRecordVersionNo。'
)
assert.ok(
  previewHeader.includes('class="edhr-batch-detail__preview-form-version"') &&
    previewHeader.includes('{{ currentFormVersionNo }}') &&
    previewHeader.includes('aria-label="当前表单版本号"'),
  '顶部红框位置必须显示当前表单版本号，并提供可访问标签。'
)
assert.ok(
  !detail.includes('batchRecordVersionId ||') &&
    !detail.includes('|| selectedTaskForEvidence.value?.batchRecordVersionId'),
  '版本号显示不得 fallback 到 batchRecordVersionId。'
)

console.log('PASS: current form version number is shown in the batch detail preview header.')
