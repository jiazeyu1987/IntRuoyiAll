const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const vuePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const hookPath = path.join(frontendRoot, 'src/hooks/web/useUserTableColumns.ts')

const vueSource = fs.readFileSync(vuePath, 'utf8')
const hookSource = fs.readFileSync(hookPath, 'utf8')

const requiredSubmissionColumns = [
  ['submittedAt', 160],
  ['employeeUser', 140],
  ['process', 150],
  ['workOrder', 160],
  ['completionQuantity', 130],
  ['lossQuantity', 120],
  ['reportAllocations', 240],
  ['reportUnallocatedQuantity', 130],
  ['lossBreakdown', 210],
  ['product', 180],
  ['inspectionTask', 150],
  ['inspectionItems', 190],
  ['equipmentSnapshot', 220],
  ['selectedDevice', 220],
  ['equipmentNumber', 150],
  ['acceptanceStandard', 220],
  ['inspectionMethod', 180],
  ['inspectionJudgement', 150],
  ['parameterSnapshot', 280],
  ['deviceParameterReadings', 280],
  ['approvedBy', 140],
  ['approvedAt', 160],
]

const submissionTableMarker = 'data-user-table-column-explicit'
const submissionTableMarkerIndex = vueSource.indexOf(submissionTableMarker)
assert.ok(submissionTableMarkerIndex >= 0, 'missing standard submission table marker')
const submissionTableStart = vueSource.lastIndexOf('<el-table', submissionTableMarkerIndex)
const submissionTableEnd = vueSource.indexOf('</el-table>', submissionTableMarkerIndex)
assert.ok(submissionTableStart >= 0 && submissionTableEnd > submissionTableStart)
const submissionTableSource = vueSource.slice(
  submissionTableStart,
  submissionTableEnd + '</el-table>'.length
)

const columnBlocks = [
  ...submissionTableSource.matchAll(/<el-table-column\b[\s\S]*?<\/el-table-column>/g)
].map(
  (match) => match[0]
)

for (const [key, fallback] of requiredSubmissionColumns) {
  const blocks = columnBlocks.filter((block) => block.includes(`prop="${key}"`))
  assert.ok(blocks.length > 0, `missing el-table-column block for ${key}`)
  for (const block of blocks) {
    assert.match(
      block,
      new RegExp(`:width="getSubmissionColumnWidthString\\('${key}'\\)"`),
      `${key} must bind saved width to el-table-column width`
    )
    assert.match(
      block,
      new RegExp(
        `:min-width="getSubmissionColumnMinWidthString\\('${key}',\\s*${fallback}\\)"`
      ),
      `${key} must keep its default min-width fallback`
    )
  }
}

const tableKeyContracts = [
  ['PRODUCTION_SUBMISSION_TABLE_KEY', 'productionSubmissionDefaultColumns'],
  ['PRODUCTION_REPORT_HISTORY_TABLE_KEY', 'productionReportHistoryDefaultColumns'],
  ['PQC_SUBMISSION_TABLE_KEY', 'pqcSubmissionDefaultColumns'],
  ['PQC_FORM_HISTORY_TABLE_KEY', 'pqcFormHistoryDefaultColumns']
]

for (const [tableKey, defaultColumns] of tableKeyContracts) {
  assert.match(
    vueSource,
    new RegExp(
      `useUserTableColumns\\(\\s*${tableKey}\\s*,\\s*${defaultColumns}\\s*\\)`
    ),
    `${tableKey} must use its isolated default column pool`
  )
}

assert.match(
  hookSource,
  /width:\s*normalizeWidth\(saved\.width\)\s*\|\|\s*column\.width/,
  'useUserTableColumns must merge saved.width into the live column state'
)
assert.match(
  vueSource,
  /@header-dragend="handleSubmissionHeaderDragend"/,
  'submission table must route Element Plus header-dragend into the user column hook'
)
assert.match(
  hookSource,
  /const handleHeaderDragend = async \(newWidth:[\s\S]*?target\.width = normalizeWidth\(newWidth\) \|\| target\.width[\s\S]*?await autoSaveConfig\(\)/,
  'header dragend must update width and autosave immediately'
)

console.log('PASS: team leader submission columns bind persisted width and keep min-width defaults')
