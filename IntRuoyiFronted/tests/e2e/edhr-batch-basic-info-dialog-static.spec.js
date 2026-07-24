const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')

const detail = fs.readFileSync(detailPath, 'utf8')

assert(
  !detail.includes('basicInfoDialogVisible'),
  '批次详情页不得继续保留“基础信息”弹框状态。'
)
assert(
  !detail.includes('<Dialog title="基础信息"') &&
    !detail.includes('edhr-batch-detail__basic-info-dialog') &&
    !detail.includes('edhr-batch-detail__basic-info-summary'),
  '批次详情页不得继续渲染“基础信息”弹框或其专用样式。'
)
assert(
  !detail.includes("focus === 'work-order'") && !detail.includes("focus === 'route'"),
  '详情页不得继续用 work-order/route focus 触发基础信息弹框。'
)
assert(
  !detail.includes('basicInfoDialogVisible.value = true'),
  '详情页不得继续存在打开基础信息弹框的触发分支。'
)
assert(
  detail.includes("type EdhrBatchExecutionDetailFocus = 'process' | 'approval'"),
  '详情页路由 focus 类型只保留仍有独立展示行为的 process/approval。'
)
assert(
  /const applyRouteFocus = \(\) => \{[\s\S]*focus === 'process'[\s\S]*processDetailDialogVisible\.value = true[\s\S]*focus === 'approval'[\s\S]*openReleaseCheckGroup\(\)/.test(detail),
  '详情页只允许 process 打开详情弹框、approval 打开放行入口。'
)
assert(
  !detail.includes('@click="basicInfoDialogVisible = true">基础</el-button>'),
  '批次详情右侧栏不得继续显示“基础”按钮。'
)
assert(
  !detail.includes('@click="processDetailDialogVisible = true">详情</el-button>'),
  '批次详情右侧栏不得继续显示“详情”按钮。'
)
assert(
  detail.includes('<Dialog title="详情"') &&
    detail.includes('edhr-batch-detail__process-detail-dialog') &&
    detail.includes('edhr-batch-detail__process-detail-actions'),
  '当前工序控制按钮仍必须保留在“详情”弹框中。'
)
assert(
  !detail.includes('<div class="edhr-batch-detail__summary">'),
  '红框内基础信息不应继续作为详情页顶部独立大块直接展示。'
)
assert(
  !detail.includes('<aside class="edhr-batch-detail__action-panel" aria-label="当前工序控制按钮">'),
  '右侧当前工序控制按钮不应继续直接展示在主页面红框区域。'
)
assert(
  detail.includes('grid-template-columns: 240px minmax(0, 1fr) 260px;') &&
    detail.includes('edhr-batch-detail__review-rail'),
  '主页面工序复盘区应保留工序列表 + 表单 + 当前工序摘要栏。'
)

console.log('PASS: edhr batch basic info dialog removal static contract')
