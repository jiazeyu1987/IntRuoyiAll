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
assert.match(
  requestBuilder,
  /resolvePqcCorrectionFailedSampleNos/,
  'PQC correction request builder must derive failed piece sample numbers before submit.'
)
assert.match(
  requestBuilder,
  /scrapQuantity < failedSampleCount/,
  'PQC correction request builder must reject scrap quantity below failed piece detail count.'
)

assert.match(
  correctionService,
  /command\.getScrapQuantity\(\) > command\.getActualInspectionQuantity\(\)/,
  'Backend PQC correction command validation must reject scrap quantity above actual inspection quantity.'
)
assert.match(
  correctionService,
  /validateScrapQuantityAgainstPieceDetails\(command, updatedDetails\)/,
  'Backend PQC correction service must validate scrap quantity against updated piece details before writes.'
)
assert.match(
  correctionService,
  /map\(MesPqcInspectionPieceDetailDO::getSampleNo\)[\s\S]*distinct\(\)[\s\S]*count\(\)/,
  'Backend piece detail floor must count distinct failed sample numbers, not duplicate item rows.'
)

console.log('PASS PQC correction quantity limit static contract')
