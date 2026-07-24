import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const source = fs.readFileSync(detailPath, 'utf8')

const previewStart = source.indexOf('class="edhr-batch-detail__preview-header"')
const previewEnd = source.indexOf('<div v-if="isReleaseProcessSelected"', previewStart)
assert(previewStart >= 0 && previewEnd > previewStart, '必须能定位批记录预览顶部区域。')

const previewTemplate = source.slice(previewStart, previewEnd)
const contextIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-context"')
const routeLinkIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-route-link"')
const carrierIndex = previewTemplate.indexOf('class="edhr-batch-detail__preview-carrier"')

assert(contextIndex >= 0, '批记录预览顶部必须保留左侧批记录上下文。')
assert(routeLinkIndex > contextIndex, '工艺流程链接必须位于批记录上下文之后。')
assert(carrierIndex > routeLinkIndex, '工艺流程链接必须位于批记录/记录本切换之前。')
assert.match(previewTemplate, /:disabled="!batchProcessRouteId"/)
assert.match(previewTemplate, /:title="batchProcessRouteTitle"/)
assert.match(previewTemplate, /@click\.stop="openBatchProcessRoute"/)
assert.match(previewTemplate, /\{\{\s*batchProcessRouteLabel\s*\}\}/)

assert.match(
  source,
  /const batchProcessRouteId = computed\(\(\) => \{[\s\S]*?Number\(detail\.value\?\.routeId\)[\s\S]*?Number\.isFinite/
)
assert.match(
  source,
  /const batchProcessRouteLabel = computed\([\s\S]*?routeName[\s\S]*?routeCode[\s\S]*?'未关联工艺流程'/
)

const openFunction = source.match(
  /const openBatchProcessRoute = async \(\) => \{[\s\S]*?\r?\n\}\r?\n/
)?.[0]
assert(openFunction, '必须实现工艺流程跳转方法。')
for (const marker of [
  "name: 'MesProRouteEdit'",
  'id: String(routeId)',
  "tab: 'flow'",
  '当前批次未关联有效工艺流程。'
]) {
  assert(openFunction.includes(marker), `工艺流程跳转必须包含：${marker}`)
}

const routeLinkStyle = source.match(
  /\.edhr-batch-detail__preview-route-link\s*\{([\s\S]*?)\n\}/
)?.[1]
assert(routeLinkStyle, '必须提供工艺流程链接样式。')
for (const marker of ['display: inline-flex', 'color: #1677ff', 'text-overflow: ellipsis']) {
  assert(routeLinkStyle.includes(marker), `工艺流程链接必须保持紧凑可读：${marker}`)
}

console.log('PASS: eDHR batch preview header exposes the linked process route.')
