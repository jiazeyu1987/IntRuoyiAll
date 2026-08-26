const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = source.indexOf(reportMarker)
assert.ok(reportStart >= 0, 'submission workbench must keep report marker.')
const tableStart = source.indexOf('<el-table', reportStart)
const tableEnd = source.indexOf('</el-table>', tableStart)
assert.ok(tableStart > reportStart && tableEnd > tableStart, 'submission table block must be locatable.')
const tableBlock = source.slice(tableStart, tableEnd)
const extractConstArray = (constName) => {
  const start = source.indexOf(`const ${constName}`)
  const arrayStart = source.indexOf('[', source.indexOf('=', start))
  const end = source.indexOf('\n]', arrayStart)
  assert.ok(start >= 0 && arrayStart > start && end > arrayStart, `${constName} block must be locatable.`)
  return source.slice(start, end + 1)
}
const productionDefaultColumnsBlock = extractConstArray('productionSubmissionDefaultColumns')
const pqcDefaultColumnsBlock = extractConstArray('pqcSubmissionDefaultColumns')
const defaultColumnsBlock = `${productionDefaultColumnsBlock}\n${pqcDefaultColumnsBlock}`

for (const removedLabel of ['一线PQC表单', '审核副本', '过程检验汇集', '复核判定']) {
  assert.doesNotMatch(
    tableBlock,
    new RegExp(`label="${removedLabel}"`),
    `PQC leader list must not render removed workflow/audit column ${removedLabel}.`
  )
}

for (const removedKey of [
  'pqcFillFormSnapshot',
  'auditCopyStatus',
  'processInspectionAggregation',
  'submissionReviewStatus'
]) {
  assert.doesNotMatch(
    defaultColumnsBlock,
    new RegExp(`\\{ key: '${removedKey}'`),
    `submission default columns must not include removed key ${removedKey}.`
  )
}

for (const [key, label, marker] of [
  ['workOrder', '生产工单', 'data-pqc-leader-work-order'],
  ['inspectionItems', '检验项', 'data-pqc-leader-inspection-items'],
  ['equipmentNumber', '设备编号', 'data-pqc-leader-equipment-number'],
  ['acceptanceStandard', '接收标准', 'data-pqc-leader-acceptance-standard'],
  ['inspectionMethod', '检验方法', 'data-pqc-leader-inspection-method'],
  ['inspectionJudgement', '检验判定', 'data-pqc-leader-inspection-judgement']
]) {
  assert.match(
    pqcDefaultColumnsBlock,
    new RegExp(`\\{ key: '${key}', label: '${label}'`),
    `submission default columns must include structured PQC key ${key}.`
  )
  assert.match(
    tableBlock,
    new RegExp(marker),
    `PQC leader list must expose structured marker ${marker}.`
  )
}
assert.doesNotMatch(
  defaultColumnsBlock,
  /key:\s*'pieceSampleValues'|label:\s*'逐件\/样本值'/,
  'sample values must stay detail-only and must not be available as a list column.'
)
assert.doesNotMatch(
  tableBlock,
  /data-pqc-leader-piece-sample-values|label="逐件\/样本值"/,
  'PQC leader list must not expose the noisy sample-values column.'
)

for (const keptKey of [
  'completionQuantity',
  'lossQuantity',
  'lossBreakdown',
  'product',
  'inspectionTask',
  'equipmentSnapshot',
  'parameterSnapshot'
]) {
  assert.match(
    defaultColumnsBlock,
    new RegExp(`\\{ key: '${keptKey}'`),
    `submission default columns must keep structured key ${keptKey}.`
  )
}

for (const resolver of [
  'resolvePqcInspectionItemItems',
  'resolvePqcEquipmentNumberItems',
  'resolvePqcAcceptanceStandardItems',
  'resolvePqcInspectionMethodItems',
  'resolvePqcInspectionJudgementItems',
  'resolvePqcPieceSampleItems'
]) {
  assert.match(source, new RegExp(`const ${resolver}\\s*=`), `missing structured PQC resolver ${resolver}.`)
}

const parameterResolverStart = source.indexOf('const resolvePqcParameterItems')
const parameterResolverEnd = source.indexOf('const resolveSubmissionParameterItems', parameterResolverStart)
assert.ok(
  parameterResolverStart >= 0 && parameterResolverEnd > parameterResolverStart,
  'PQC parameter resolver must be locatable.'
)
const parameterResolverBlock = source.slice(parameterResolverStart, parameterResolverEnd)
assert.match(
  parameterResolverBlock,
  /formatPqcSnapshotStandard\(detail\)/,
  'PQC parameter detail column must summarize frozen standards and limits per inspection item.'
)
assert.match(
  parameterResolverBlock,
  /selectedEquipmentName|selectedEquipmentCode/,
  'PQC parameter detail column must keep equipment context without relying on the sample-value column.'
)
assert.match(
  parameterResolverBlock,
  /inspectionMethod/,
  'PQC parameter detail column must keep the configured inspection method context.'
)
assert.match(
  parameterResolverBlock,
  /judgement|itemResult|resultType/,
  'PQC parameter detail column must keep item judgement context.'
)
assert.doesNotMatch(
  parameterResolverBlock,
  /sampleIndex|detail\.sampleValues|#\$\{sampleIndex \+ 1\}/,
  'PQC parameter detail column must not duplicate the 30-piece sample values.'
)

assert.match(
  source,
  /readSubmissionPayloadValue\(rootPayload, \['inspectionQuantity', 'actualInspectionQuantity'\]\)/,
  'PQC list must read inspection quantity using the same frontline form field names.'
)
assert.match(
  source,
  /readSubmissionPayloadValue\(rootPayload, \['scrapQuantity', 'lossQuantity', 'SCRAP_QUANTITY'\]\)/,
  'PQC list must read scrap/loss quantity using the same frontline form field names.'
)
assert.match(
  source,
  /label:\s*'不良\/损耗'/,
  'PQC loss breakdown must use a fixed business label without reading defect descriptions.'
)
assert.doesNotMatch(
  source,
  /defectDescription|nonconformanceDescription/,
  'PQC leader list must not read or render removed defect descriptions.'
)
assert.match(
  source,
  /team-leader-workbench__parameter-value[\s\S]*is-out-of-range/,
  'out-of-range sample values must remain red in the leader list.'
)

console.log('PASS: PQC leader structured submission columns static contract')
