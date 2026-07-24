const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')

const detail = fs.readFileSync(detailPath, 'utf8')

assert(
  detail.includes('aria-label="工序复盘"') &&
    !detail.includes('basicInfoDialogVisible.value = true') &&
    detail.includes('loadReviewTimeline'),
  '工序复盘区域必须保留可访问语义和刷新复盘能力，且不得继续触发基础信息弹框'
)

assert(
  !detail.includes('<div class="edhr-batch-detail__section-title">工序复盘</div>') &&
    !detail.includes('签名、审批、审计、归档、放行和变更都围绕工序或收尾节点呈现。'),
  '红框顶部不应继续显示“工序复盘”标题和冗余说明文案'
)

assert(
  !detail.includes('<div class="edhr-batch-detail__review-subtitle">表单</div>') &&
    !detail.includes('<div class="edhr-batch-detail__muted">已填写表单</div>') &&
    !detail.includes('edhr-batch-detail__review-header') &&
    !detail.includes('<el-tag type="primary" effect="plain">当前工序</el-tag>') &&
    detail.includes('aria-label="已填写批记录"'),
  '中间表单区不应继续重复显示表单标题、已填写表单说明和当前工序摘要头'
)

assert(
  !detail.includes('<div class="edhr-batch-detail__review-subtitle">当前工序控制按钮</div>') &&
    !detail.includes('工序上下文仅作用于当前选中的工序，完整明细入口仍保留到原功能页。') &&
    !detail.includes('<el-tag type="info" effect="plain">工序证据链</el-tag>') &&
    !detail.includes('edhr-batch-detail__process-evidence-context') &&
    detail.includes('selectedProcessEvidenceGroups') &&
    detail.includes('工序执行') &&
    detail.includes('审签归档') &&
    detail.includes('审计追溯') &&
    detail.includes('关联引用'),
  '右侧控制按钮区不应继续显示冗余说明和当前工序摘要，但必须保留控制按钮分组'
)

console.log('edhr redundant review copy removal static contract passed')
