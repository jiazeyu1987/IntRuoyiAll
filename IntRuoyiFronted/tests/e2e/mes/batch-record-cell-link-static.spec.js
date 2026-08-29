const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const apiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordcelllink/index.ts')
const routeFile = resolve(process.cwd(), 'src/router/modules/remaining.ts')
const templateFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordformlist/index.vue')
const executionPageFile = resolve(process.cwd(), 'src/views/mes/pro/edhr/ExecutionPage.vue')

const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')
const route = readFileSync(routeFile, 'utf-8')
const template = readFileSync(templateFile, 'utf-8')
const executionPage = readFileSync(executionPageFile, 'utf-8')

function cssBlock(selector) {
  const start = page.indexOf(selector)
  assert.notEqual(start, -1, `page misses css selector ${selector}`)
  const open = page.indexOf('{', start)
  const close = page.indexOf('\n}', open)
  assert.notEqual(open, -1, `page misses css block open for ${selector}`)
  assert.notEqual(close, -1, `page misses css block close for ${selector}`)
  return page.slice(open + 1, close)
}

for (const token of [
  'batch-record-cell-link__form-stage',
  'batch-record-cell-link__source-select',
  'batch-record-cell-link__work-order-field-panel',
  '<el-option label="生产工单" :value="PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID" />',
  'const isProductionWorkOrderSelected = computed(() => sourceReportId.value === PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID)',
  'sourceType.value = isProductionWorkOrderSelected.value',
  'handleSourceSelectionChange',
  'sourceType.value === SOURCE_TYPE_PRODUCTION_WORK_ORDER',
  'sourceFieldCode: selectedSourceCell.value.sourceFieldCode',
  'buildProductionWorkOrderFieldCells',
  'const cellMetaMap = new Map<string, BatchRecordCellLinkCellVO>()',
  'cellMetaMap.set(`${cell.rowIndex}:${cell.columnIndex}`, cell)',
  'const reportCellKey = `${formCells.reportId}:${meta?.cellKey || key}`',
  'selectedCell.rowIndex === rowIndex && selectedCell.columnIndex === columnIndex',
  'batch-record-cell-link__target-select',
  'batch-record-cell-link__source-link-count',
  'batch-record-cell-link__create-button',
  'batch-record-cell-link__detail-dialog',
  'batch-record-cell-link__pane is-source',
  'batch-record-cell-link__pane is-target',
  'grid-template-rows: 104px minmax(0, 1fr)',
  'grid-template-columns: minmax(0, 1fr) minmax(0, 1fr)',
  '暂无表单链接关系',
  '源表单链接详情',
  'aria-label="查看源表单链接详情"',
  'sourceLinkedRules',
  'sourceLinkCountText',
  'relationDetailDialogVisible',
  '@click="openRelationDetailDialog"',
  '@click="createRule"',
  'const createRule = async ()',
  'await persistRules(nextRules, `单元格链接已建立并保存，共 ${nextRules.length} 条。`)',
  'const removeRuleByIndex = async (index: number)',
  ':columns="sourceRenderableSheet.columns"',
  ':columns="targetRenderableSheet.columns"',
  "'colgroup'",
  'height: `${row.height}px`',
  'layout.cols || {}',
  'widthPercent',
  'batch-record-cell-link-sheet__row',
  'is-fillable-cell',
  "const EMPTY_FILLABLE_PLACEHOLDER = '?'",
  'normalizeRenderedCellText',
  'const text = normalizeRenderedCellText(rawText, isFillableCell)',
  'border: 1px solid #1f2937',
  '建立链接'
]) {
  assert.ok(page.includes(token), `page misses ${token}`)
}

assert.ok(!page.includes('batch-record-cell-link__source-type-select'), 'source type selector must be folded into the source selector')
assert.ok(!page.includes('handleSourceTypeChange'), 'source type change handler must not remain as a separate visible control')
assert.ok(!page.includes('batch-record-cell-link__target-tabs'), 'target tabs must be removed')
assert.ok(!page.includes('batch-record-cell-link__relation-panel'), 'inline relation cards must be removed')
assert.ok(!page.includes('batch-record-cell-link__footer'), 'bottom link footer must be removed')
assert.ok(!page.includes('batch-record-cell-link__selection'), 'current source/target cards must be removed')
assert.ok(!page.includes('batch-record-cell-link__rule-list'), 'bottom rule list must move to dialog')
assert.ok(!page.includes('已建立链接'), 'old established-link footer title must be removed')
assert.ok(!page.includes('batch-record-cell-link__save-button'), 'save rules button must be removed')
assert.ok(!page.includes('保存规则'), 'save rules label must be removed from the link workbench')

const targetSelectIndex = page.indexOf('batch-record-cell-link__target-select')
const linkCountIndex = page.indexOf('batch-record-cell-link__source-link-count')
const createButtonIndex = page.indexOf('batch-record-cell-link__create-button')
assert.ok(targetSelectIndex > 0, 'target select marker missing')
assert.ok(linkCountIndex > targetSelectIndex, 'source link count must be after target select')
assert.ok(createButtonIndex > linkCountIndex, 'create link button must be after source link count')

assert.ok(!page.includes("}, '填')"), 'fillable cells must not render the old corner 填 badge')
assert.ok(!page.includes('batch-record-cell-link-sheet__badge'), 'fillable cells must not use a corner badge')

const sheetBlock = cssBlock('.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet)')
assert.ok(sheetBlock.includes('width: 100%;'), 'sheet must fill pane width')
assert.ok(sheetBlock.includes('min-width: 0;'), 'sheet must not reserve a fixed minimum width')
assert.ok(sheetBlock.includes('max-width: 100%;'), 'sheet must stay inside pane width')
assert.ok(!sheetBlock.includes('min-width: 1080px'), 'sheet must not keep the old fixed 1080px minimum width')

const sheetCellBlock = cssBlock('.batch-record-cell-link__sheet-scroll :deep(.batch-record-cell-link-sheet__cell)')
assert.ok(sheetCellBlock.includes('min-width: 0;'), 'sheet cells must allow the table to shrink with the pane')
assert.ok(sheetCellBlock.includes('overflow-wrap: anywhere;'), 'long cell text must wrap inside adaptive columns')
assert.ok(sheetCellBlock.includes('text-align: center;'), 'question placeholder must stay centered horizontally')
assert.ok(sheetCellBlock.includes('vertical-align: middle;'), 'question placeholder must stay centered vertically')

for (const token of [
  '/mes/pro/batch-record-cell-link/workbench-context',
  '/mes/pro/batch-record-cell-link/form-cells',
  '/mes/pro/batch-record-cell-link/rules/save',
  '/mes/pro/batch-record-cell-link/prefill',
  'templateId?: number',
  'versionNo?: string'
]) {
  assert.ok(api.includes(token), `api misses ${token}`)
}

for (const token of [
  'templateId: parseNumber(route.query.templateId)',
  "versionNo: String(route.query.versionNo || '')",
  'route.query.returnTo',
  'route.query.returnLabel'
]) {
  assert.ok(page.includes(token), `cell link page misses form template route support: ${token}`)
}

assert.ok(route.includes('MesProBatchRecordCellLink'), 'route misses MesProBatchRecordCellLink')
assert.ok(
  route.includes("mes:pro-batch-record-cell-link:query"),
  'route misses mes:pro-batch-record-cell-link:query permission'
)
assert.ok(template.includes('handleCellLinks(selectedReport)'), 'template action misses cell-link entry')

for (const token of [
  'BatchRecordCellLinkApi',
  'BatchRecordCellLinkApi.getPrefill(currentExecutionId, workTaskId.value)',
  'hydrateDraftState(detail, prefillResponse?.prefills || [], prefillResponse?.conflicts || [])',
  'normalizeCellLinkPrefillDraftValue',
  'cellLinkPrefillNotice',
  '生产工单字段',
  '跨表单带入'
]) {
  assert.ok(!executionPage.includes(token), `execution page must not keep draft prefill token ${token}`)
}

assert.ok(
  executionPage.includes('hydrateDraftState(detail)'),
  'execution page must hydrate from persisted detail values only'
)

console.log('batch-record-cell-link static contract passed')
