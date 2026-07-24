const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)

const detail = fs.readFileSync(detailPath, 'utf8')

const reviewWorkbenchStyleStart = detail.indexOf('.edhr-batch-detail__review-workbench {')
const reviewWorkbenchStyleEnd = detail.indexOf('}', reviewWorkbenchStyleStart)
assert(
  reviewWorkbenchStyleStart >= 0 && reviewWorkbenchStyleEnd > reviewWorkbenchStyleStart,
  '必须能定位工序复盘三栏工作台样式'
)
const reviewWorkbenchStyle = detail.slice(reviewWorkbenchStyleStart, reviewWorkbenchStyleEnd)

assert(
  reviewWorkbenchStyle.includes('grid-template-columns: 240px minmax(0, 1fr) 260px;'),
  '左侧工序列表列宽必须为 240px，确保工序名称和状态完整显示，并保持中间预览弹性列与右侧 260px 摘要栏'
)

const mobileMediaStart = detail.indexOf('@media (max-width: 768px)')
assert(mobileMediaStart >= 0, '必须保留移动端响应式样式')
const mobileMedia = detail.slice(mobileMediaStart)
assert(
  mobileMedia.includes('.edhr-batch-detail__review-workbench') &&
    mobileMedia.includes('grid-template-columns: 1fr;'),
  '移动端工序复盘工作台必须继续折叠为单列布局'
)

console.log('edhr batch review rail width static contract passed')
