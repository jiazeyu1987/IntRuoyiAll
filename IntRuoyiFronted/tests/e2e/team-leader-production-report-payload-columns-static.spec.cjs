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

assert.doesNotMatch(productionDefaultColumns, /key:\s*'workOrderCode'|label:\s*'生产工单'/, 'production report default columns must remove the red-box 生产工单 column')
assert.doesNotMatch(productionDefaultColumns, /key:\s*'pqcResult'|label:\s*'PQC'/, 'production report default columns must remove the red-box PQC column')
assert.doesNotMatch(productionDefaultColumns, /key:\s*'submissionContent'|label:\s*'提交内容'/, 'production report default columns must remove the red-box 提交内容 column')
for (const pqcOnlyColumn of [
  'product',
  'inspectionTask',
  'pqcSubmissionContent',
  'processInspectionAggregation'
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

assert.match(productionDefaultColumns, /key:\s*'completionQuantity'[\s\S]*label:\s*'完成数量'/, 'report table must expose completion quantity')
assert.match(productionDefaultColumns, /key:\s*'lossQuantity'[\s\S]*label:\s*'损耗数量'/, 'report table must expose loss quantity')
assert.match(productionDefaultColumns, /key:\s*'lossBreakdown'[\s\S]*label:\s*'损耗明细'/, 'report table must expose loss reason quantity details')
assert.match(productionDefaultColumns, /key:\s*'selectedDevice'[\s\S]*label:\s*'选用设备'/, 'report table must expose the selected device as a first-class column')
assert.match(productionDefaultColumns, /key:\s*'deviceParameterReadings'[\s\S]*label:\s*'设备参数'/, 'report table must expose selected device parameter readings')

for (const pqcOnlyColumn of [
  'product',
  'inspectionTask',
  'pqcSubmissionContent',
  'processInspectionAggregation'
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
  /activeLeaderTab\.value\s*===\s*'PQC'\s*\?\s*pqcSubmissionColumnControl\s*:\s*productionSubmissionColumnControl/,
  'active leader type must select the scoped column control'
)

assert.match(page, /data-team-leader-loss-breakdown/, 'loss breakdown column needs a stable marker')
assert.match(page, /data-team-leader-selected-device/, 'selected device column needs a stable marker')
assert.match(page, /data-team-leader-device-parameter-readings/, 'device parameter readings column needs a stable marker')

assert.match(processPoolApi, /lossDetails\??:\s*ProcessPoolTimelineLossDetailVO\[\]/, 'timeline API VO must expose structured lossDetails')
assert.match(processPoolApi, /selectedDevice\??:\s*ProcessPoolTimelineSelectedDeviceVO/, 'timeline API VO must expose selectedDevice')
assert.match(processPoolApi, /deviceParameterReadings\??:\s*ProcessPoolTimelineDeviceParameterReadingVO\[\]/, 'timeline API VO must expose deviceParameterReadings')

console.log('PASS: production report table columns expose structured submission payload')
