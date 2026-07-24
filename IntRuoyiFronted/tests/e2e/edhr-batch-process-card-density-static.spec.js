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

const reviewList = readStyleBlock('.edhr-batch-detail__review-list')
assert.ok(reviewList.includes('gap: 6px'), '工序列表卡片间距必须收紧到 6px。')

const pendingList = readStyleBlock('.edhr-batch-detail__pending-task-list')
assert.ok(pendingList.includes('gap: 6px'), '待处理工序分组间距必须收紧到 6px。')
assert.ok(pendingList.includes('margin-bottom: 6px'), '待处理工序与已填写工序之间仅保留 6px 间距。')

const pendingItem = readStyleBlock('.edhr-batch-detail__pending-task-item')
for (const requiredStyle of [
  'height: var(--edhr-process-item-height)',
  'min-height: var(--edhr-process-item-height)',
  'padding: 6px 8px',
  'display: flex',
  'align-items: center',
  'box-sizing: border-box'
]) {
  assert.ok(pendingItem.includes(requiredStyle), `待处理工序卡片必须使用紧凑规格：${requiredStyle}`)
}
assert.ok(!pendingItem.includes('min-height: 72px'), '待处理工序卡片不得继续使用 72px 最小高度。')

const reviewItem = readStyleBlock('.edhr-batch-detail__review-item')
for (const requiredStyle of [
  'grid-template-columns: minmax(0, 1fr) auto',
  'gap: 4px 8px',
  'padding: 7px 8px',
  'height: var(--edhr-process-item-height)',
  'min-height: var(--edhr-process-item-height)',
  'box-sizing: border-box'
]) {
  assert.ok(reviewItem.includes(requiredStyle), `已填写工序卡片必须使用紧凑双列布局：${requiredStyle}`)
}

assert.ok(
  detail.includes('.edhr-batch-detail__review-item > .el-tag {'),
  '已填写工序状态标签必须有紧凑网格定位样式。'
)

console.log('PASS: EDHR batch process cards use compact high-density layout.')
