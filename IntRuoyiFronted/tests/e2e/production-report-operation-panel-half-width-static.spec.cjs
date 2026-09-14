const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const page = fs.readFileSync(pagePath, 'utf8')

const extractConstArray = (constName) => {
  const start = page.indexOf(`const ${constName}`)
  const arrayStart = page.indexOf('[', start)
  const end = page.indexOf('\n]', arrayStart)
  assert.ok(start >= 0 && arrayStart > start && end > arrayStart, `${constName} must be declared`)
  return page.slice(start, end + 2)
}

const productionColumns = extractConstArray('productionSubmissionDefaultColumns')
const productionHistoryColumns = extractConstArray('productionReportHistoryDefaultColumns')
const pqcColumns = extractConstArray('pqcSubmissionDefaultColumns')
const pqcHistoryColumns = extractConstArray('pqcFormHistoryDefaultColumns')

assert.match(
  page,
  /const PRODUCTION_SUBMISSION_TABLE_KEY = `\$\{SUBMISSION_TABLE_KEY\}\.production\.operation-half-width-v1`/,
  'production report columns must use a new stable key so old 270px preferences do not override the new default'
)
assert.match(
  productionColumns,
  /key:\s*'operation',\s*label:\s*'操作',\s*width:\s*135/,
  'production report operation column must be exactly half of the former 270px width'
)
assert.match(
  productionHistoryColumns,
  /key:\s*'operation',\s*label:\s*'操作',\s*width:\s*110/,
  'production report history operation width must remain unchanged'
)
assert.match(
  pqcColumns,
  /key:\s*'operation',\s*label:\s*'操作',\s*width:\s*270/,
  'PQC management operation width must remain unchanged'
)
assert.match(
  pqcHistoryColumns,
  /key:\s*'operation',\s*label:\s*'操作',\s*width:\s*110/,
  'PQC history operation width must remain unchanged'
)

const operationColumnStart = page.indexOf("v-if=\"isSubmissionColumnVisible('operation')\"")
const operationColumnEnd = page.indexOf('</el-table-column>', operationColumnStart)
assert.ok(operationColumnStart >= 0 && operationColumnEnd > operationColumnStart)
const operationColumn = page.slice(operationColumnStart, operationColumnEnd + '</el-table-column>'.length)

assert.match(operationColumn, /:width="getSubmissionColumnWidthString\('operation'\)"/)
assert.match(operationColumn, /:align="showProductionReportModule \? 'center' : undefined"/)
assert.match(operationColumn, /:header-align="showProductionReportModule \? 'center' : undefined"/)
assert.match(
  operationColumn,
  /<div\s+:class="\{ 'team-leader-workbench__submission-actions': showProductionReportModule \}"\s*>[\s\S]*<\/div>/,
  'operation buttons must be grouped in an explicit centered container'
)
assert.match(
  operationColumn,
  /data-team-leader-correction-event-id="String\(row\.id\)"[\s\S]*@click="openCorrection\(row\)"[\s\S]*>\s*修改\s*<\/el-button>/,
  'the existing correction action must remain intact'
)
assert.match(
  operationColumn,
  /data-production-report-allocation-event-id="String\(row\.id\)"[\s\S]*@click="openAllocation\(row\)"[\s\S]*>\s*分配\s*<\/el-button>/,
  'the existing allocation action must remain intact'
)
assert.match(
  page,
  /\.team-leader-workbench__submission-actions\s*\{[\s\S]*?display:\s*flex;[\s\S]*?align-items:\s*center;[\s\S]*?justify-content:\s*center;[\s\S]*?gap:\s*12px;[\s\S]*?white-space:\s*nowrap;[\s\S]*?\}/,
  'operation actions must use a stable centered single-row layout'
)
assert.match(
  page,
  /\.team-leader-workbench__submission-actions\s+:deep\(\.el-button \+ \.el-button\)\s*\{[\s\S]*?margin-left:\s*0;[\s\S]*?\}/,
  'Element Plus adjacent button margin must not offset the centered group'
)

console.log('PASS: production report operation panel is half width and centered')
