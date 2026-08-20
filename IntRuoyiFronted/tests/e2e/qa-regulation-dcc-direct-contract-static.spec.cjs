const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const compact = (source) => source.replace(/\s+/g, '')
const sliceBetween = (source, startText, endText) => {
  const start = source.indexOf(startText)
  const end = source.indexOf(endText, start)
  assert.ok(start >= 0 && end > start, startText + ' block must exist.')
  return source.slice(start, end)
}

const apiSource = read('src/api/mes/qc/template/index.ts')
const qaPageSource = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const compactApi = compact(apiSource)

const saveReqSource = sliceBetween(
  apiSource,
  'export interface QaInspectionRegulationSaveReqVO',
  'export interface QaInspectionRegulationSaveRespVO'
)
const compactSaveReq = compact(saveReqSource)

assert.ok(compactSaveReq.includes('dccProjectCodeId:number'), 'SaveReq must use DCC project code ID.')
assert.ok(
  compactSaveReq.includes('processes:QaInspectionRegulationProcessVO[]'),
  'SaveReq must submit QA-owned processes.'
)
assert.doesNotMatch(
  saveReqSource,
  /\b(productId|productMasterId|routeId|routeProcessId|processId|workOrderId)\b/,
  'SaveReq must not carry product, route, work order, or MES process identity.'
)

assert.ok(
  compactApi.includes(
    "exporttypeQaInspectionRegulationResultType='BOOLEAN'|'NUMERIC'|'TEXT'"
  ) && compactApi.includes('resultType:QaInspectionRegulationResultType'),
  'QA item resultType must be restricted to BOOLEAN/NUMERIC/TEXT at the API type boundary.'
)
assert.ok(
  compactApi.includes('exportinterfaceQaInspectionRegulationSaveRespVO{dccProjectCodeId:number'),
  'SaveResp must echo the DCC project code ID so callers can verify the saved relation.'
)
assert.ok(
  compactApi.includes('getPublishedQaRegulationVersion:async(dccProjectCodeId:number,versionId?:number)'),
  'published-version must accept DCC project code ID plus optional versionId.'
)
assert.ok(
  compactApi.includes('params:{dccProjectCodeId,...(versionId?{versionId}:{})}'),
  'published-version must query by DCC project code ID plus optional versionId.'
)
assert.ok(
  compactApi.includes('getCurrentQaRegulation:async(dccProjectCodeId:number)'),
  'current must accept DCC project code ID.'
)
assert.ok(
  compactApi.includes('params:{dccProjectCodeId}'),
  'current must be queried by DCC project code ID.'
)
assert.ok(
  compactApi.includes("dccProjectCodeIds:dccProjectCodeIds.join(',')"),
  'project-statuses must batch DCC project code IDs.'
)

const payloadSource = sliceBetween(
  qaPageSource,
  'const buildQaRegulationSavePayload',
  'const qaRegulationPublishChecks'
)
const compactPayload = compact(payloadSource)
assert.ok(
  compactPayload.includes('dccProjectCodeId:resolvePositiveId('),
  'QA page payload must resolve selected DCC ID.'
)
assert.ok(
  compactPayload.includes('inspectionTypeRules:qaInspectionTypeRules.map'),
  'QA page must submit all inspection type rules.'
)
assert.ok(
  compactPayload.includes('processes:buildQaRegulationProcesses(settings)'),
  'QA page must submit QA-owned process/item payload.'
)
assert.doesNotMatch(
  payloadSource,
  /\b(productId|productMasterId|routeId|routeProcessId|processId|workOrderId)\b/,
  'QA page payload must not submit product, route, work order, or MES process identity.'
)

const resultTypeSelect = qaPageSource.slice(
  qaPageSource.indexOf('prop="resultType"'),
  qaPageSource.indexOf('prop="sourceOriginalExcerpt"', qaPageSource.indexOf('prop="resultType"'))
)
assert.match(resultTypeSelect, /value="BOOLEAN"/)
assert.match(resultTypeSelect, /value="NUMERIC"/)
assert.match(resultTypeSelect, /value="TEXT"/)
assert.doesNotMatch(resultTypeSelect, /value="NUMBER"|value="CHOICE"/)

assert.ok(
  compact(qaPageSource).includes(
    'constqaRegulationProjectStatusByDccId=ref(newMap<number,QaInspectionRegulationProjectStatusVO>())'
  ),
  'QA page must key project statuses by DCC project code ID.'
)
assert.doesNotMatch(
  qaPageSource,
  /qaRegulationProjectStatusByProductId|resolveDccProjectCodeProductIds|QaProductRuleDraftSnapshot/,
  'QA page must not keep product-derived QA regulation status state.'
)

console.log('PASS: QA regulation DCC direct contract static checks')
