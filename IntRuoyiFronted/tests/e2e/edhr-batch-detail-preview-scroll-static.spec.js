const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
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

const readStyleBlock = (selector, source = detail) => {
  const start = source.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = source.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return source.slice(start, end)
}

const previewStart = detail.indexOf(
  'class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"'
)
const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail"', previewStart)
assert.ok(previewStart >= 0 && railStart > previewStart, '必须存在中栏表单区域')

const previewTemplate = detail.slice(previewStart, railStart)
const headerIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-header"')
const scrollBodyIndex = previewTemplate.indexOf('class="edhr-batch-detail__review-card"')
assert.ok(headerIndex >= 0, '中栏必须保留顶部上下文和载体切换栏')
assert.ok(scrollBodyIndex > headerIndex, '纵向滚动容器必须位于顶部切换栏之后，保证顶部固定')

const readonlyForms = [...previewTemplate.matchAll(/<EdhrExecutionReadonlyForm[\s\S]*?\/>/g)].map(
  (match) => match[0]
)
assert.equal(readonlyForms.length, 1, '已执行表单和空表单预览应共用同一个只读渲染分支')
assert.ok(
  readonlyForms[0].includes('v-else-if="selectedPreviewFormViewModel"') &&
    readonlyForms[0].includes(':form-view-model="selectedPreviewFormViewModel"') &&
    readonlyForms[0].includes(':signature-records="selectedPreviewSignatureRecords"'),
  '统一只读表单必须由 submitted 内容或清空后的 task preview formViewModel 驱动'
)
assert.match(readonlyForms[0], /\sfit-to-viewport(?:\s|\/>)/, '统一只读表单必须按中栏宽度等比缩放')

const reviewCard = readStyleBlock('.edhr-batch-detail__review-card')
for (const requiredStyle of [
  'flex: 1',
  'min-height: 0',
  'max-width: 100%',
  'overflow-x: hidden',
  'overflow-y: auto',
  'overscroll-behavior: contain',
  'scrollbar-gutter: stable'
]) {
  assert.ok(reviewCard.includes(requiredStyle), `表单正文滚动容器缺少：${requiredStyle}`)
}

const formSurface = readStyleBlock('.edhr-batch-detail__form-surface')
for (const requiredStyle of [
  'flex: 0 0 auto',
  'height: auto',
  'width: 100%',
  'max-width: 100%',
  'min-width: 0'
]) {
  assert.ok(formSurface.includes(requiredStyle), `表单内容面缺少横向自适应约束：${requiredStyle}`)
}
assert.ok(!formSurface.includes('height: 100%'), '表单内容面必须使用自然高度以参与正文纵向滚动')

const mobileStart = detail.indexOf('@media (max-width: 768px)')
assert.notEqual(mobileStart, -1, '必须保留窄屏响应式样式')
const mobileStyles = detail.slice(mobileStart)
const mobileReviewCard = readStyleBlock('.edhr-batch-detail__review-card', mobileStyles)
for (const requiredStyle of [
  'height: auto',
  'max-height: none',
  'overflow: visible',
  'scrollbar-gutter: auto'
]) {
  assert.ok(mobileReviewCard.includes(requiredStyle), `窄屏必须恢复自然页面布局：${requiredStyle}`)
}

console.log('PASS: eDHR batch detail preview scrolls vertically and fits width without horizontal overflow.')
