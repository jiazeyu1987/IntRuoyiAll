const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const processPoolApi = readUtf8('src/api/mes/pro/processpool/index.ts')

const extractConstArray = (constName) => {
  const start = page.indexOf(`const ${constName}`)
  const arrayStart = page.indexOf('[', page.indexOf('=', start))
  const end = page.indexOf('\n]', arrayStart)
  assert.ok(start >= 0 && arrayStart > start && end > arrayStart, `team-leader ${constName} must be declared`)
  return page.slice(start, end + 1)
}

const productionDefaultColumns = extractConstArray('productionSubmissionDefaultColumns')
const pqcDefaultColumns = extractConstArray('pqcSubmissionDefaultColumns')
const submissionColumnConfigBlockStart = page.indexOf('const productionSubmissionColumnControl')
const submissionColumnConfigBlockEnd = page.indexOf('const showLeaderTypeTabs', submissionColumnConfigBlockStart)
assert.ok(
  submissionColumnConfigBlockStart >= 0 && submissionColumnConfigBlockEnd > submissionColumnConfigBlockStart,
  'team-leader submission column controls must be declared before module visibility'
)
const submissionColumnConfigBlock = page.slice(
  submissionColumnConfigBlockStart,
  submissionColumnConfigBlockEnd
)

assert.doesNotMatch(productionDefaultColumns, /key:\s*'workOrderCode'/, 'production report default columns must not expose the deprecated workOrderCode column key')
assert.doesNotMatch(productionDefaultColumns, /key:\s*'pqcResult'|label:\s*'PQC'/, 'production report default columns must remove the red-box PQC column')
assert.doesNotMatch(productionDefaultColumns, /key:\s*'submissionContent'|label:\s*'提交内容'/, 'production report default columns must remove the red-box 提交内容 column')
for (const pqcOnlyColumn of [
  'product',
  'inspectionTask',
  'inspectionItems',
  'inspectionJudgement'
]) {
  assert.doesNotMatch(
    productionDefaultColumns,
    new RegExp(`key:\\s*'${pqcOnlyColumn}'`),
    `production leader report columns must not expose PQC-only ${pqcOnlyColumn}`
  )
}
assert.doesNotMatch(
  productionDefaultColumns,
  /PQC提交内容|data-pqc-leader-submission-content|过程检验汇集|检验类型\/轮次/,
  'production leader report column settings must not show PQC submission content labels'
)

assert.doesNotMatch(
  productionDefaultColumns,
  /key:\s*'completionQuantity'|key:\s*'lossQuantity'|key:\s*'reportUnallocatedQuantity'|key:\s*'submissionMaterialSummary'|key:\s*'submissionDeviceSummary'|key:\s*'submissionParameterSummary'|key:\s*'lossBreakdown'|key:\s*'selectedDevice'|key:\s*'deviceParameterReadings'/,
  'production report default columns must hide duplicate detail fields from the main row'
)
assert.match(pqcDefaultColumns, /key:\s*'completionQuantity'[\s\S]*label:\s*'检验数量'/, 'PQC report table must keep inspection quantity')
assert.match(pqcDefaultColumns, /key:\s*'lossQuantity'[\s\S]*label:\s*'损耗数量'/, 'PQC report table must keep loss quantity')

for (const pqcOnlyColumn of [
  'product',
  'inspectionTask',
  'inspectionItems',
  'inspectionJudgement'
]) {
  assert.match(
    pqcDefaultColumns,
    new RegExp(`key:\\s*'${pqcOnlyColumn}'`),
    `PQC leader report columns must keep PQC-only ${pqcOnlyColumn}`
  )
}
assert.match(
  submissionColumnConfigBlock,
  /PRODUCTION_SUBMISSION_TABLE_KEY[\s\S]*PQC_SUBMISSION_TABLE_KEY[\s\S]*activeSubmissionColumnControl/,
  'production and PQC leader report columns must use separate table keys and active column control'
)
assert.match(
  submissionColumnConfigBlock,
  /activeLeaderTab\.value\s*===\s*'PQC'[\s\S]*pqcSubmissionColumnControl[\s\S]*productionSubmissionColumnControl/,
  'active leader type must select the scoped column control and preserve history-specific variants'
)

assert.match(page, /type="expand"[\s\S]*data-team-leader-submission-expand-detail/, 'production report table needs an expanded row for complete multi-material and multi-device facts')
assert.match(page, /activeLeaderTab !== 'PRODUCTION'[\s\S]*isSubmissionColumnVisible\('completionQuantity'\)/, 'completion quantity column must be kept out of production leader main rows')
assert.match(page, /activeLeaderTab !== 'PRODUCTION'[\s\S]*isSubmissionColumnVisible\('lossQuantity'\)/, 'loss quantity column must be kept out of production leader main rows')
assert.doesNotMatch(page, /isProductionLeader && isSubmissionColumnVisible\('submissionMaterialSummary'\)/, 'production leader main row must not show duplicate material summary')
assert.doesNotMatch(page, /isProductionLeader && isSubmissionColumnVisible\('submissionDeviceSummary'\)/, 'production leader main row must not show duplicate device summary')
assert.doesNotMatch(page, /isProductionLeader && isSubmissionColumnVisible\('submissionParameterSummary'\)/, 'production leader main row must not show duplicate parameter summary')
assert.match(page, /data-team-leader-submission-material-detail[\s\S]*resolveSubmissionMaterialDetailRows\(row\)/, 'expanded detail row must render full material detail rows')
assert.match(page, /data-team-leader-submission-material-card[\s\S]*v-for="material in resolveSubmissionMaterialDetailRows\(row\)"/, 'expanded detail row must render one material block per submitted material')
assert.match(page, /:data="material\.devices"[\s\S]*data-team-leader-submission-material-device-row/, 'expanded material block must render one row per device that belongs to the material')
assert.doesNotMatch(page, /data-team-leader-submission-device-detail[\s\S]*resolveSubmissionDeviceDetailGroups\(row\)/, 'expanded detail row must not render devices in a separate cross-material device section')
assert.match(page, /interface SubmissionMaterialDetailRow[\s\S]*devices: SubmissionMaterialDeviceRow\[\]/, 'material detail rows must carry their own device rows')
assert.match(page, /interface SubmissionMaterialDeviceRow[\s\S]*deviceNameText: string[\s\S]*deviceCodeText: string[\s\S]*parameterText: string/, 'device rows must expose device name, device code and inline parameter text')
assert.match(page, /<el-table-column label="设备名称" prop="deviceNameText"[\s\S]*<el-table-column label="设备编号" prop="deviceCodeText"[\s\S]*<el-table-column label="设备参数" prop="parameterText"/, 'expanded device row must show name, code and parameter text on one row')
assert.match(page, /formatSubmissionDeviceParameterText[\s\S]*\.join\('；'\)/, 'device parameter text must be separated by semicolons')
assert.match(page, /data-team-leader-submission-device-parameter-detail/, 'expanded device parameter detail needs a stable marker')
assert.match(page, /activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible\('lossBreakdown'\)/, 'legacy loss breakdown column must be kept out of production leader main rows')
assert.match(page, /activeLeaderTab !== 'PRODUCTION' && isSubmissionColumnVisible\('selectedDevice'\)/, 'legacy selected device column must be kept out of production leader main rows')
assert.match(page, /activeLeaderTab !== 'PRODUCTION'[\s\S]*isSubmissionColumnVisible\('deviceParameterReadings'\)/, 'legacy device parameter column must be kept out of production leader main rows')

assert.match(processPoolApi, /lossDetails\??:\s*ProcessPoolTimelineLossDetailVO\[\]/, 'timeline API VO must expose structured lossDetails')
assert.match(processPoolApi, /selectedDevice\??:\s*ProcessPoolTimelineSelectedDeviceVO/, 'timeline API VO must expose selectedDevice')
assert.match(processPoolApi, /deviceParameterReadings\??:\s*ProcessPoolTimelineDeviceParameterReadingVO\[\]/, 'timeline API VO must expose deviceParameterReadings')

console.log('PASS: production report table columns expose structured submission payload')
