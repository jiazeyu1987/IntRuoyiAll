const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), 'Schedule order page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  !source.includes("import { ElMessageBox } from 'element-plus'"),
  'Replan skipped selected handling must not depend on a blocking Element Plus confirm dialog.'
)
assert.ok(
  source.includes('const buildSkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'),
  'Replan apply must derive selected schedule orders that produced no task rows.'
)
assert.ok(
  source.includes('const notifySkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'),
  'Replan apply must notify skipped selected orders without blocking the apply flow.'
)
assert.ok(
  source.includes('notifySkippedSelectedReplanRows(freshPreview)'),
  'Replan apply must notify skipped selected rows after fresh preview and before apply.'
)
assert.ok(
  source.includes('以下工单本次被阻断'),
  'Notification must clearly tell the user which selected schedule orders are blocked.'
)
assert.ok(
  !source.includes('是否继续应用其余可排工单？'),
  'Skipped selected rows must not ask for a second confirmation before applying the schedulable remainder.'
)
assert.ok(
  source.includes('issue.workOrderId === row.workOrderId'),
  'Skipped reason matching must connect preview issues back to the selected work order.'
)
assert.ok(
  source.includes('row.erpWorkOrderCode || row.code'),
  'Notification must show the business work order code or schedule order code.'
)

const skippedRowTypeStart = source.indexOf('type SkippedSelectedReplanRow = {')
assert.ok(skippedRowTypeStart >= 0, 'Skipped selected row type must be explicit.')
const skippedRowTypeEnd = source.indexOf('\nconst escapeHtml', skippedRowTypeStart)
assert.ok(skippedRowTypeEnd > skippedRowTypeStart, 'Skipped selected row type block must be bounded.')
const skippedRowTypeBlock = source.slice(skippedRowTypeStart, skippedRowTypeEnd)
assert.ok(
  !skippedRowTypeBlock.includes('productCode') && !skippedRowTypeBlock.includes('productName'),
  'Skipped selected notification rows must only carry the work order and blocked reason, not product details.'
)

const buildRowsStart = source.indexOf(
  'const buildSkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'
)
assert.ok(buildRowsStart >= 0, 'Skipped selected row builder must exist.')
const buildRowsEnd = source.indexOf('\nconst notifySkippedSelectedReplanRows', buildRowsStart)
assert.ok(buildRowsEnd > buildRowsStart, 'Skipped selected row builder block must be bounded.')
const buildRowsBlock = source.slice(buildRowsStart, buildRowsEnd)
assert.ok(
  !buildRowsBlock.includes('productCode: row.productCode') &&
    !buildRowsBlock.includes('productName: row.productName'),
  'Skipped selected row builder must not collect product details for the notification.'
)

const notifyStart = source.indexOf(
  'const notifySkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'
)
assert.ok(notifyStart >= 0, 'Skipped selected notification helper must exist.')
const notifyEnd = source.indexOf('\nconst canApplyReplan', notifyStart)
assert.ok(notifyEnd > notifyStart, 'Skipped selected notification helper block must be bounded.')
const notifyBlock = source.slice(notifyStart, notifyEnd)
assert.ok(
  /escapeHtml\(row\.code\)/.test(notifyBlock) && /escapeHtml\(\s*row\.reason\s*\)/.test(notifyBlock),
  'Skipped selected notification must render the work order code and blocked reason.'
)
assert.ok(
  !notifyBlock.includes('row.productCode') && !notifyBlock.includes('row.productName'),
  'Skipped selected notification must not render product code, product name, or other detail fields.'
)

console.log('PASS: MES schedule order replan skipped selected non-blocking notification static contract')
