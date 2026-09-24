const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = page.indexOf(reportMarker)
assert.ok(reportStart >= 0, 'team leader report table must keep a stable report marker.')

const tableStart = page.indexOf('<el-table', reportStart)
const tableEnd = page.indexOf('</UnifiedListTemplate>', tableStart)
assert.ok(tableStart > reportStart && tableEnd > tableStart, 'team leader report template block must be locatable.')
const tableBlock = page.slice(tableStart, tableEnd)

const extractConstArray = (constName) => {
  const start = page.indexOf(`const ${constName}`)
  const arrayStart = page.indexOf('[', page.indexOf('=', start))
  const end = page.indexOf('\n]', arrayStart)
  assert.ok(start >= 0 && arrayStart > start && end > arrayStart, `${constName} must be declared.`)
  return page.slice(start, end + 1)
}

const productionDefaultColumns = extractConstArray('productionSubmissionDefaultColumns')
const pqcDefaultColumns = extractConstArray('pqcSubmissionDefaultColumns')
const allSubmissionColumnBlocks = `${productionDefaultColumns}\n${pqcDefaultColumns}`

for (const [key, label] of [
  ['auditCopyStatus', '审核副本'],
  ['submissionReviewStatus', '复核判定']
]) {
  assert.doesNotMatch(
    tableBlock,
    new RegExp(`label="${label}"|prop="${key}"|isSubmissionColumnVisible\\('${key}'\\)`),
    `red-box column ${label} must not render in the submission table.`
  )
  assert.doesNotMatch(
    allSubmissionColumnBlocks,
    new RegExp(`key:\\s*'${key}'|label:\\s*'${label}'`),
    `red-box column ${label} must not be available in submission column settings.`
  )
}

for (const [key, label, marker] of [
  ['operation', '操作', 'data-team-leader-correction-event-id'],
  ['completionQuantity', '检验数量', 'data-team-leader-completion-quantity']
]) {
  assert.match(
    allSubmissionColumnBlocks,
    new RegExp(`key:\\s*'${key}'[\\s\\S]*label:\\s*'${label}'`),
    `adjacent required column ${label} must remain in the column settings.`
  )
  assert.match(tableBlock, new RegExp(marker), `adjacent required table marker ${marker} must remain rendered.`)
}

assert.doesNotMatch(
  pqcDefaultColumns,
  /key:\s*'deviceParameterReadings'/,
  'PQC管理 regular column settings must keep device parameter details out of the default list.'
)
assert.match(
  page,
  /data-team-leader-device-parameter-readings/,
  'device parameter detail rendering support must remain available outside the regular default column set.'
)
assert.doesNotMatch(
  pqcDefaultColumns,
  /key:\s*'lossQuantity'|key:\s*'lossBreakdown'/,
  'PQC management default columns must hide the red-box loss columns.'
)

for (const requiredReviewCapability of [
  'canReviewSubmission',
  'openReview',
  'reviewTeamLeaderSubmission',
  'data-team-leader-review-event-id'
]) {
  assert.match(
    page,
    new RegExp(requiredReviewCapability),
    `review capability ${requiredReviewCapability} must remain after hiding the red-box columns.`
  )
}

console.log('PASS: team leader submission table hides review copy red-box columns')
