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
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-policy-dialog"/,
  'controlled print dialog must expose the current direct-print/no-approval policy'
)
assert.match(detailPage, /打印用途/, 'controlled print dialog must require print purpose')
assert.match(detailPage, /份数/, 'controlled print dialog must require copies')
assert.match(detailPage, /接收部门/, 'controlled print dialog must require receiving department')
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-receiving-department-select"[\s\S]{0,300}<el-select[\s\S]{0,300}filterable/,
  'receiving department must be a structured selectable field'
)
assert.match(detailPage, /使用位置/, 'controlled print dialog must require use location')
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-use-location-select"[\s\S]{0,300}<el-select[\s\S]{0,300}allow-create/,
  'use location must support common selections and controlled free-entry'
)
assert.match(detailPage, /data-testid="dcc-controlled-print-records"/, 'detail page must show controlled print records')
const shouldLoadControlledPrintRecordsBlock = extractBetween(
  detailPage,
  'const shouldLoadControlledPrintRecords = () =>',
  'const loadControlledPrintRecords = async () =>',
  'controlled print records load gate'
)
assert.match(
  shouldLoadControlledPrintRecordsBlock,
  /!viewerMode\.value/,
  'viewer preview mode must not request controlled print records because that records section is not rendered'
)
const loadControlledPrintRecordsCatch = extractBetween(
  detailPage,
  '  } catch (error) {\n    controlledPrintRecords.value = []',
  '  } finally {\n    controlledPrintRecordsLoading.value = false',
  'controlled print records local error handling'
)
assert.doesNotMatch(
  loadControlledPrintRecordsCatch,
  /throw error/,
  'controlled print records auxiliary load errors must stay visible in the records section instead of failing the whole detail/preview page'
)
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-permission-hint"/,
  'detail page must show a read-only no-print-permission hint when the print action is hidden'
)
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-policy-hint"/,
  'print records section must explain whether the current file prints directly or requires approval'
)
assert.match(
  detailPage,
  /data-testid="dcc-controlled-print-result-dialog"/,
  'submitting controlled print must show a durable success result dialog'
)
assert.match(
  detailPage,
  />\s*查看打印记录\s*</,
  'success result dialog must provide a direct entry to the created print record'
)
assert.match(detailPage, /打印编号/, 'print output/records must show print number')
assert.match(detailPage, /打印人/, 'print output/records must show print user')
assert.match(detailPage, /打印时间/, 'print output/records must show print time')
assert.match(detailPage, /副本编号/, 'print output/records must show per-copy copy numbers')
assert.match(
  detailPage,
  /controlled-print-record-row--latest/,
  'latest print record must have a dedicated highlight row class'
)
assert.match(
  detailPage,
  /data-controlled-print-record-id/,
  'print number cell must expose a stable record id for auto-scroll/highlight'
)
assert.doesNotMatch(
  detailPage,
  /openApprovalPrintWindow[\s\S]{0,200}受控打印/,
  '受控打印 must not be an alias for 流程打印'
)

console.log('PASS: DCC controlled print static contract')
