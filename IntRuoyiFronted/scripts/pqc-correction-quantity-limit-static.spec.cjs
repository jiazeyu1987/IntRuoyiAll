const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const leaderPage = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const correctionService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java'
)

const scrapInputBlock = leaderPage.match(
  /<el-form-item label="损耗数量">[\s\S]*?data-pqc-inspection-correction-scrap[\s\S]*?\/>/,
)?.[0] || ''
assert.match(
  scrapInputBlock,
  /:max="correctionForm\.pqcActualInspectionQuantity"/,
  'PQC correction scrap input must cap scrap quantity at actual inspection quantity.'
)

const rowInterface = leaderPage.match(/interface PqcInspectionCorrectionItemRow \{[\s\S]*?\n\}/)?.[0] || ''
assert.match(rowInterface, /resultType\?: string/)
assert.match(rowInterface, /standardLowerLimit\?: number \| string/)
assert.match(rowInterface, /standardUpperLimit\?: number \| string/)
assert.match(rowInterface, /standardPrecision\?: number/)

const requestBuilderStart = leaderPage.indexOf('const buildPqcCorrectionRequest = () => {')
const submitCorrectionStart = leaderPage.indexOf(
  'const submitCorrection = async () => {',
  requestBuilderStart,
)
assert.ok(requestBuilderStart >= 0, 'PQC correction request builder must exist.')
assert.ok(submitCorrectionStart > requestBuilderStart, 'PQC correction submit handler must follow request builder.')
const requestBuilder = leaderPage.slice(requestBuilderStart, submitCorrectionStart)
assert.match(
  requestBuilder,
  /scrapQuantity > actualInspectionQuantity/,
  'PQC correction request builder must reject scrap quantity above actual inspection quantity.'
)
assert.doesNotMatch(
  requestBuilder,
  /resolvePqcCorrectionFailedSampleNos|scrapQuantity < failedSampleCount/,
  'PQC correction must preserve failed samples independently of loss quantity.'
)

assert.match(
  correctionService,
  /command\.getScrapQuantity\(\) > command\.getActualInspectionQuantity\(\)/,
  'Backend PQC correction command validation must reject scrap quantity above actual inspection quantity.'
)
assert.match(
  correctionService,
  /command\.getScrapQuantity\(\) < 0/,
  'Backend PQC correction must reject negative loss quantity.'
)
assert.doesNotMatch(
  correctionService,
  /validateScrapQuantityAgainstPieceDetails|PRO_PQC_SCRAP_QUANTITY_BELOW_FAILED_SAMPLES/,
  'Backend correction must preserve failed samples independently of loss quantity.'
)
assert.match(
  correctionService,
  /resolveInspectionResult\(updatedDetails\)/,
  'Backend correction must derive quality from updated inspection details.'
)
console.log('PASS PQC correction quantity limit static contract')
