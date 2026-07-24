const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

assert.ok(
  detail.includes('grid-template-columns: 156px minmax(0, 1fr) 260px;'),
  '工序复盘主网格必须互换左右宽度：左侧 156px，中间自适应，右侧 260px。'
)
assert.ok(
  !detail.includes('grid-template-columns: 260px minmax(0, 1fr) 156px;'),
  '工序复盘主网格不得继续使用左 260px / 右 156px。'
)
assert.ok(detail.includes('edhr-batch-detail__process-panel'), '左侧工序列表栏必须保留。')
assert.ok(detail.includes('edhr-batch-detail__review-rail'), '右侧当前工序摘要栏必须保留。')

console.log('PASS: EDHR review workbench swaps left and right rail widths.')
