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

const previewStart = detail.indexOf('class="edhr-batch-detail__preview-header"')
const previewEnd = detail.indexOf('aria-label="放行预检工作区"', previewStart)
assert(previewStart >= 0 && previewEnd > previewStart, '必须能定位批记录预览顶部栏。')
const previewTemplate = detail.slice(previewStart, previewEnd)

const contextIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-context"')
const actionsIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-actions"')
const routeIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-route-link"')
const syncIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-sync"')
const extraIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-extra"')
const versionIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-form-version"')
const carrierIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-carrier"')

assert(contextIndex >= 0, '顶部栏必须保留左侧批次/批记录上下文。')
assert(actionsIndex > contextIndex, '工艺流程和同步状态必须统一放入中间操作组。')
assert(routeIndex > actionsIndex && routeIndex < extraIndex, '工艺流程链接必须位于中间操作组内。')
assert(syncIndex > actionsIndex && syncIndex < extraIndex, '同步状态按钮必须位于中间操作组内。')
assert(extraIndex > actionsIndex, '版本号和批记录/记录本切换必须放入右侧附加组。')
assert(versionIndex > extraIndex && versionIndex < carrierIndex, '版本号必须位于右侧附加组内并在载体切换之前。')
assert(carrierIndex > extraIndex, '批记录/记录本切换必须位于右侧附加组内。')

const header = readStyleBlock('.edhr-batch-detail__preview-header')
for (const requiredStyle of [
  'display: grid',
  'grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr)',
  'align-items: center'
]) {
  assert(header.includes(requiredStyle), `顶部栏必须使用三段式网格布局：${requiredStyle}`)
}

const actions = readStyleBlock('.edhr-batch-detail__preview-actions')
for (const requiredStyle of ['display: inline-flex', 'justify-content: center', 'justify-self: center']) {
  assert(actions.includes(requiredStyle), `中间操作组必须固定居中：${requiredStyle}`)
}

const extra = readStyleBlock('.edhr-batch-detail__preview-extra')
for (const requiredStyle of ['display: inline-flex', 'justify-content: flex-end', 'justify-self: end']) {
  assert(extra.includes(requiredStyle), `右侧附加组必须靠右，不得挤压中间操作组：${requiredStyle}`)
}

assert(
  detail.includes('@media (max-width: 768px)') &&
    detail.includes('.edhr-batch-detail__preview-header {\n    grid-template-columns: 1fr;') &&
    detail.includes('.edhr-batch-detail__preview-actions {\n    justify-self: start;') &&
    detail.includes('.edhr-batch-detail__preview-extra {\n    justify-self: start;'),
  '窄屏时顶部栏必须按三段纵向换行，避免按钮重叠。'
)

console.log('PASS: eDHR preview header keeps process route and sync button aligned across modes')
