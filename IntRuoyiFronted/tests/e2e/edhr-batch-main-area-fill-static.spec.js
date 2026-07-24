const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const readStyleBlock = (selector) => {
  const start = detail.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detail.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detail.slice(start, end)
}

assert(
  detail.includes('<ContentWrap class="edhr-batch-detail__content-wrap">'),
  '批记录详情 ContentWrap 必须声明页面级 class，用于覆盖默认底部外边距并接管高度。'
)

assert(
  detail.includes("const BATCH_DETAIL_PAGE_BODY_CLASS = 'edhr-batch-detail-page'") &&
    detail.includes('document.body.classList.add(BATCH_DETAIL_PAGE_BODY_CLASS)') &&
    detail.includes('document.body.classList.remove(BATCH_DETAIL_PAGE_BODY_CLASS)') &&
    detail.includes('onActivated(activateFullHeightLayout)') &&
    detail.includes('onDeactivated(deactivateFullHeightLayout)') &&
    detail.includes('onBeforeUnmount(deactivateFullHeightLayout)'),
  '批记录详情页激活时必须折叠页脚高度，离开或卸载时必须恢复。'
)

const pageBody = readStyleBlock(':global(body.edhr-batch-detail-page)')
assert.ok(pageBody.includes('--app-footer-height: 0px'), '当前详情页必须将页脚高度折叠为 0。')

const contentWrap = readStyleBlock('.edhr-batch-detail__content-wrap')
for (const requiredStyle of [
  'height: calc(',
  '100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding)',
  'margin-bottom: 0 !important'
]) {
  assert.ok(contentWrap.includes(requiredStyle), `内容卡片必须填满页脚上方剩余空间：${requiredStyle}`)
}

const contentWrapBody = readStyleBlock('.edhr-batch-detail__content-wrap :deep(.el-card__body)')
for (const requiredStyle of ['height: 100%', 'min-height: 0', 'display: flex']) {
  assert.ok(contentWrapBody.includes(requiredStyle), `内容卡片 body 必须传递高度：${requiredStyle}`)
}

const detailRoot = readStyleBlock('.edhr-batch-detail')
for (const requiredStyle of ['flex: 1', 'min-height: 0']) {
  assert.ok(detailRoot.includes(requiredStyle), `详情根容器必须承接卡片高度：${requiredStyle}`)
}

const review = readStyleBlock('.edhr-batch-detail__review')
for (const requiredStyle of ['flex: 1', 'min-height: 0', 'display: flex', 'flex-direction: column']) {
  assert.ok(review.includes(requiredStyle), `主复盘区域必须拉满并纵向传递高度：${requiredStyle}`)
}

const workbench = readStyleBlock('.edhr-batch-detail__review-workbench')
for (const requiredStyle of ['flex: 1', 'min-height: 0', 'align-items: stretch']) {
  assert.ok(workbench.includes(requiredStyle), `三列工作台必须同步拉满：${requiredStyle}`)
}
assert.ok(!workbench.includes('align-items: start'), '三列工作台不得继续使用顶部对齐导致右侧详情提前结束。')

const reviewList = readStyleBlock('.edhr-batch-detail__review-list')
for (const requiredStyle of ['height: 100%', 'min-height: 0', 'max-height: none']) {
  assert.ok(reviewList.includes(requiredStyle), `左侧工序列表必须改为填满后内部滚动：${requiredStyle}`)
}
assert.ok(
  !reviewList.includes('max-height: calc(100vh - 250px)'),
  '左侧工序列表不得继续用固定视口扣减值限制高度。'
)

const preview = readStyleBlock('.edhr-batch-detail__review-preview')
for (const requiredStyle of ['height: 100%', 'display: flex', 'min-height: 0']) {
  assert.ok(preview.includes(requiredStyle), `中间预览区域必须填满主区域：${requiredStyle}`)
}

const rail = readStyleBlock('.edhr-batch-detail__review-rail')
for (const requiredStyle of ['height: 100%', 'min-height: 0', 'max-height: none']) {
  assert.ok(rail.includes(requiredStyle), `右侧详情栏必须填满主区域：${requiredStyle}`)
}

assert(
  /@media \(max-width: 768px\)[\s\S]*\.edhr-batch-detail__content-wrap\s*\{[\s\S]*height:\s*auto;/.test(detail) &&
    /@media \(max-width: 768px\)[\s\S]*\.edhr-batch-detail__review-list\s*\{[\s\S]*height:\s*auto;/.test(detail),
  '移动端必须恢复自然高度，避免单列布局被固定视口高度裁切。'
)

console.log('PASS: eDHR batch detail main area fills the viewport without bottom dead space.')
