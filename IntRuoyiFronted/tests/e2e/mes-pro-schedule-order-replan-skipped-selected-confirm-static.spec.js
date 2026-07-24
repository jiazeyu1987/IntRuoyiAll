const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), 'Schedule order page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  source.includes("import { ElMessageBox } from 'element-plus'"),
  'Replan skipped selected confirmation must use an explicit Element Plus confirm dialog.'
)
assert.ok(
  source.includes('const buildSkippedSelectedReplanRows = (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'),
  'Replan apply must derive selected schedule orders that produced no task rows.'
)
assert.ok(
  source.includes('const confirmSkippedSelectedReplanRows = async (preview: ProTaskAutoScheduleReplanPreviewRespVO) => {'),
  'Replan apply must confirm skipped selected orders before continuing.'
)
assert.ok(
  source.includes('await confirmSkippedSelectedReplanRows(freshPreview)'),
  'Replan apply must run skipped selected confirmation after fresh preview and before apply.'
)
assert.ok(
  source.includes('以下选中的排产工单本次不会参与排产'),
  'Confirmation must clearly tell the user that selected schedule orders will not participate.'
)
assert.ok(
  source.includes('是否继续应用其余可排工单？'),
  'Confirmation must ask whether to continue applying the remaining schedulable orders.'
)
assert.ok(
  source.includes('issue.workOrderId === row.workOrderId'),
  'Skipped reason matching must connect preview issues back to the selected work order.'
)
assert.ok(
  source.includes('row.erpWorkOrderCode || row.code'),
  'Confirmation must show the business work order code or schedule order code.'
)

console.log('PASS: MES schedule order replan skipped selected confirmation static contract')
