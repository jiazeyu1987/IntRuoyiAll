const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../../../../..')
const viewSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/batchrecordreport/index.ts'),
  'utf8'
)

assert.match(
  viewSource,
  /data-fill-config-action="formalize"/,
  'batch record cell rules dialog must expose a formalize action button.'
)
assert.match(
  viewSource,
  /正式化可映射格子/,
  'batch record cell rules dialog must show the explicit formalization label.'
)
assert.match(
  viewSource,
  /formalizeDetectedCells/,
  'formalization button must call a dedicated handler.'
)
assert.match(
  apiSource,
  /formalizeCellRules\s*:\s*async\s*\(reportId: string\)/,
  'batch record report API must expose a formalizeCellRules helper.'
)
assert.match(
  apiSource,
  /\/mes\/pro\/batch-record-report\/cell-rules\/formalize/,
  'batch record report API must call the formalization endpoint.'
)

console.log('PASS: batch record cell rules formalization frontend static contract')
