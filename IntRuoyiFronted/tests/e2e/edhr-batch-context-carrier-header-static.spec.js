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

const processListStart = detail.indexOf('<nav class="edhr-batch-detail__process-panel')
const processListEnd = detail.indexOf('</nav>', processListStart)
const processListTemplate = detail.slice(processListStart, processListEnd)
assert.ok(
  !processListTemplate.includes('edhr-batch-detail__process-context'),
  '左侧工序列表不得继续显示批记录上下文标题。'
)

const previewStart = detail.indexOf(
  'class="edhr-batch-detail__form-panel edhr-batch-detail__review-preview"'
)
const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail"', previewStart)
const previewTemplate = detail.slice(previewStart, railStart)
for (const marker of [
  'class="edhr-batch-detail__preview-header"',
  'class="edhr-batch-detail__preview-context"',
  'aria-label="当前批记录上下文"',
  '{{ detail?.workOrderCode || \'--\' }}',
  '{{ resolveCurrentBatchRecordNo() }}',
  'class="edhr-batch-detail__preview-carrier"'
]) {
  assert.ok(previewTemplate.includes(marker), `主区域顶部必须包含迁移后的内容：${marker}`)
}
assert.ok(
  !previewTemplate.includes('edhr-batch-detail__preview-carrier-label') &&
    !previewTemplate.includes('>填写载体</'),
  '顶部信息卡片不得显示“填写载体”文字标签。'
)

for (const marker of [
  "currentProcessFillCarrier === 'FORM'",
  "currentProcessFillCarrier === 'RECORDBOOK'",
  "selectFillCarrier('FORM')",
  "selectFillCarrier('RECORDBOOK')"
]) {
  assert.ok(previewTemplate.includes(marker), `填写载体必须只更新当前选择：${marker}`)
}
assert.ok(
  !previewTemplate.includes('openPendingTaskByFillCarrier('),
  '点击批记录或记录本时不得直接打开填写页或改变主视图。'
)

const railTemplate = detail.slice(railStart, detail.indexOf('</aside>', railStart))
assert.ok(!railTemplate.includes('>填写载体</'), '右侧详情栏不得重复显示填写载体。')
assert.ok(
  !railTemplate.includes('edhr-batch-detail__rail-carrier'),
  '右侧详情栏不得保留填写载体按钮组。'
)
const processFormActionClass = 'class="edhr-batch-detail__rail-process-form-action"'
const processFormActionClassStart = railTemplate.indexOf(processFormActionClass)
assert.notEqual(processFormActionClassStart, -1, '右侧表单卡片必须存在打开填写按钮。')
const processFormActionStart = railTemplate.lastIndexOf('<button', processFormActionClassStart)
const processFormActionEnd = railTemplate.indexOf('</button>', processFormActionClassStart)
assert.ok(
  processFormActionStart >= 0 && processFormActionEnd > processFormActionStart,
  '必须能定位右侧表单卡片的打开填写按钮。'
)
const processFormActionTemplate = railTemplate.slice(processFormActionStart, processFormActionEnd)
assert.ok(
  processFormActionTemplate.includes('@click.stop="handleSelectedPendingTaskAction(task)"'),
  '表单卡片“打开填写”必须使用当前载体并进入统一任务填写流程。'
)
assert.ok(
  railTemplate.includes('handleSelectedPendingTaskAction(selectedTaskForEvidence)'),
  '右侧打开填写按钮必须统一使用当前选中的填写载体。'
)

const selectCarrierStart = detail.indexOf('const selectFillCarrier =')
const selectCarrierEnd = detail.indexOf('\n}', selectCarrierStart)
assert.ok(selectCarrierStart >= 0 && selectCarrierEnd > selectCarrierStart, '必须实现填写载体本地选择方法。')
const selectCarrierFunction = detail.slice(selectCarrierStart, selectCarrierEnd)
for (const forbiddenMarker of ['router.push', 'openPendingTaskByFillCarrier', 'selectProcessTask', 'selectedTaskId', 'selectedExecutionId']) {
  assert.ok(
    !selectCarrierFunction.includes(forbiddenMarker),
    `切换填写载体不得改变主视图或导航：${forbiddenMarker}`
  )
}
assert.ok(
  selectCarrierFunction.includes('selectedFillCarrier.value = fillCarrier'),
  '切换填写载体必须保存为当前批次页面统一选择。'
)
assert.ok(
  !detail.includes('fillCarrierSelectionByTask'),
  '填写载体不得再按工序分别保存，否则切换工序会恢复不同模式。'
)

const currentCarrierStart = detail.indexOf('const currentProcessFillCarrier =')
const currentCarrierEnd = detail.indexOf('\n})', currentCarrierStart)
assert.ok(currentCarrierStart >= 0 && currentCarrierEnd > currentCarrierStart, '必须能定位当前填写载体计算。')
const currentCarrierFunction = detail.slice(currentCarrierStart, currentCarrierEnd)
assert.ok(
  currentCarrierFunction.includes('selectedFillCarrier.value'),
  '所有工序必须优先使用当前批次页面统一选择的填写载体。'
)

const selectProcessStart = detail.indexOf('const selectProcessTask =')
const selectProcessEnd = detail.indexOf('\n}', selectProcessStart)
assert.ok(selectProcessStart >= 0 && selectProcessEnd > selectProcessStart, '必须能定位工序选择方法。')
const selectProcessFunction = detail.slice(selectProcessStart, selectProcessEnd)
assert.ok(
  !selectProcessFunction.includes('selectedFillCarrier'),
  '切换工序不得重置当前批次页面的填写载体。'
)

assert.ok(
  detail.includes('selectedFillCarrier.value = undefined') &&
    detail.includes('() => route.query.id'),
  '只有切换到另一批次执行时才重置统一填写载体。'
)

const openSelectedStart = detail.indexOf('const handleSelectedPendingTaskAction =')
const openSelectedEnd = detail.indexOf('\n}', openSelectedStart)
assert.ok(openSelectedStart >= 0 && openSelectedEnd > openSelectedStart, '必须实现统一打开填写方法。')
const openSelectedFunction = detail.slice(openSelectedStart, openSelectedEnd)
assert.ok(
  openSelectedFunction.includes('currentProcessFillCarrier.value') &&
    openSelectedFunction.includes('openPendingTaskByFillCarrier(row, fillCarrier)'),
  '点击打开填写时必须使用当前选中的载体进入同一任务。'
)

const handleOpenTaskStart = detail.indexOf('const handleOpenTask =')
const handleOpenTaskEnd = detail.indexOf('\n}', handleOpenTaskStart)
assert.ok(handleOpenTaskStart >= 0 && handleOpenTaskEnd > handleOpenTaskStart, '必须实现工序任务打开方法。')
const handleOpenTaskFunction = detail.slice(handleOpenTaskStart, handleOpenTaskEnd)
for (const marker of [
  'openEdhrBatchTask({',
  "path: '/mes/pro/feedback/edhr-execution/form'",
  'id: String(opened.executionId)',
  'batchTaskId: String(row.id)',
  "returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'"
]) {
  assert.ok(handleOpenTaskFunction.includes(marker), `打开填写必须直达主生产表填写页：${marker}`)
}

const preview = readStyleBlock('.edhr-batch-detail__review-preview')
for (const requiredStyle of ['flex-direction: column', 'gap: 8px']) {
  assert.ok(preview.includes(requiredStyle), `主区域必须为紧凑纵向布局：${requiredStyle}`)
}
assert.ok(!preview.includes('background:'), '主视图本身不得随填写载体切换背景。')

const headerTemplateStart = previewTemplate.indexOf('class="edhr-batch-detail__preview-header"')
const headerTemplateEnd = previewTemplate.indexOf('>', headerTemplateStart)
const headerOpeningTag = previewTemplate.slice(headerTemplateStart, headerTemplateEnd)
for (const marker of [
  "'is-batch-record': currentProcessFillCarrier === 'FORM'",
  "'is-recordbook': currentProcessFillCarrier === 'RECORDBOOK'"
]) {
  assert.ok(headerOpeningTag.includes(marker), `背景状态必须绑定顶部信息卡片：${marker}`)
}

const batchRecordPreview = readStyleBlock('.edhr-batch-detail__preview-header.is-batch-record')
assert.ok(
  batchRecordPreview.includes('background: #f2f7ff'),
  '批记录模式的顶部信息卡片必须使用淡蓝色背景。'
)

const recordbookPreview = readStyleBlock('.edhr-batch-detail__preview-header.is-recordbook')
assert.ok(
  recordbookPreview.includes('background: #fff8e6'),
  '记录本模式的顶部信息卡片必须使用淡黄色背景。'
)

const header = readStyleBlock('.edhr-batch-detail__preview-header')
for (const requiredStyle of [
  'display: flex',
  'align-items: center',
  'justify-content: space-between',
  'min-height: 42px'
]) {
  assert.ok(header.includes(requiredStyle), `主区域顶部栏必须紧凑对齐：${requiredStyle}`)
}

const context = readStyleBlock('.edhr-batch-detail__preview-context')
for (const requiredStyle of ['display: flex', 'min-width: 0', 'overflow: hidden']) {
  assert.ok(context.includes(requiredStyle), `批记录上下文必须保持单行紧凑：${requiredStyle}`)
}

assert(
  detail.includes('@media (max-width: 768px)') &&
    detail.includes('.edhr-batch-detail__preview-header {\n    align-items: flex-start;') &&
    detail.includes('flex-direction: column;'),
  '窄屏时主区域顶部栏必须允许纵向换行，避免内容重叠。'
)

console.log('PASS: eDHR batch context and fill carrier are placed in the compact main-preview header.')
