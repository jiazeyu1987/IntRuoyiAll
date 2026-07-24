const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')

const detail = fs.readFileSync(detailPath, 'utf8')

assert(
  detail.includes('aria-label="工序列表"') &&
    detail.includes('edhr-batch-detail__process-list') &&
    detail.includes('edhr-batch-detail__process-panel'),
  '红框左栏必须作为工序列表呈现，而不是已填写表单列表'
)

assert(
  !detail.includes('<div class="edhr-batch-detail__review-subtitle">已填写表单</div>') &&
    detail.includes('<div class="edhr-batch-detail__process-header">') &&
    !detail.includes('<div class="edhr-batch-detail__review-subtitle">工序</div>'),
  '左栏必须保留工序顶部上下文容器，且不再显示“已填写表单”或“工序”标题'
)

assert(
  detail.includes('aria-label="当前工序表单"') &&
    detail.includes('edhr-batch-detail__form-panel') &&
    detail.includes('edhr-batch-detail__form-surface') &&
    detail.indexOf('aria-label="当前工序表单"') < detail.indexOf('<EdhrExecutionReadonlyForm'),
  '蓝框中栏必须承载当前工序表单，并在只读表单前声明表单区域'
)

assert(
  detail.includes('aria-label="当前工序控制按钮"') &&
    detail.includes('edhr-batch-detail__process-detail-actions') &&
    detail.includes('edhr-batch-detail__process-evidence') &&
    detail.includes('selectedProcessEvidenceGroups'),
  '当前工序控制按钮必须集中在详情弹窗的工序证据链区域'
)

assert(
  detail.indexOf('aria-label="当前工序表单"') < detail.indexOf('aria-label="当前工序控制按钮"') &&
    detail.indexOf('aria-label="当前工序控制按钮"') < detail.indexOf('selectedProcessEvidenceGroups'),
  '当前工序控制按钮必须位于右侧独立栏，并继续复用 selectedProcessEvidenceGroups'
)

assert(
  detail.includes('grid-template-columns: 156px minmax(0, 1fr) 260px'),
  '工序复盘工作台必须保留左窄工序、中表单、右宽摘要的三栏网格'
)

assert(
  detail.includes('edhr-batch-detail__process-code') &&
    detail.includes('edhr-batch-detail__process-report'),
  '左侧工序项必须清晰区分工序编码名称与关联表单名'
)

console.log('edhr process/form/action columns static contract passed')
