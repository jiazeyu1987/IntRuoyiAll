const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.match(
  workflowApi,
  /createControlledFilePrintRecord/,
  'workflow API must create controlled print records through the backend'
)
assert.match(
  workflowApi,
  /getControlledFilePrintRecords/,
  'workflow API must list controlled print records for traceability'
)
assert.match(
  workflowApi,
  /getControlledFilePrintHtml/,
  'workflow API must fetch generated controlled print HTML'
)
assert.match(
  workflowApi,
  /\/dcc\/controlled-files\/\$\{id\}\/controlled-print/,
  'workflow API must use the controlled-print endpoint, not approval-print/process-print'
)

const browserActionColumn = extractBetween(
  browserPage,
  'v-if="isDccBrowserColumnVisible(\'operation\')"',
  '</el-table-column>',
  'browser operation column'
)
assert.match(browserActionColumn, />\s*受控打印\s*</, 'browser row actions must expose 受控打印')
assert.match(
  browserActionColumn,
  /dcc:controlled-file:print/,
  'browser controlled print entry must be gated by dcc:controlled-file:print'
)
assert.match(browserPage, /openControlledPrintDialog/, 'browser page must open a controlled print dialog')

assert.match(detailPage, />\s*受控打印\s*</, 'detail page must expose 受控打印')
assert.match(detailPage, /data-testid="dcc-controlled-print-dialog"/, 'detail page must render controlled print dialog')
assert.match(detailPage, /打印用途/, 'controlled print dialog must require print purpose')
assert.match(detailPage, /份数/, 'controlled print dialog must require copies')
assert.match(detailPage, /接收部门/, 'controlled print dialog must require receiving department')
assert.match(detailPage, /使用位置/, 'controlled print dialog must require use location')
assert.match(detailPage, /data-testid="dcc-controlled-print-records"/, 'detail page must show controlled print records')
assert.match(detailPage, /打印编号/, 'print output/records must show print number')
assert.match(detailPage, /打印人/, 'print output/records must show print user')
assert.match(detailPage, /打印时间/, 'print output/records must show print time')
assert.doesNotMatch(
  detailPage,
  /openApprovalPrintWindow[\s\S]{0,200}受控打印/,
  '受控打印 must not be an alias for 流程打印'
)

console.log('PASS: DCC controlled print static contract')
