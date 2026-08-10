const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const apiSource = read('src/api/mes/pro/feedback/index.ts')
const backendSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/' +
  'cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)

const pendingContextBlock = blockBetween(
  backendSource,
  'private List<MesFrontlinePqcTaskContext> resolvePendingPqcTaskContexts',
  'private Map<PqcTaskProcessKey, List<MesFrontlineProductionSubmitCandidate>>'
)
assert.match(
  pendingContextBlock,
  /toProcessListInspectionItem\(item,/,
  'Process list task snapshots must use the list-only inspection-item mapper.'
)

const processListMapperBlock = blockBetween(
  backendSource,
  'private static MesFrontlinePqcInspectionItem toProcessListInspectionItem',
  'private static MesFrontlinePqcInspectionItem toInspectionItem'
)
assert.doesNotMatch(
  processListMapperBlock,
  /inspectionItem\.(inspectionTool|samplingPlanText)/,
  'Process-list mapping must not require QA display fields.'
)

const strictMapperBlock = blockBetween(
  backendSource,
  'private static MesFrontlinePqcInspectionItem toInspectionItem',
  'private static MesFrontlinePqcInspectionItem.EquipmentOption toEquipmentOption'
)
assert.match(strictMapperBlock, /StrUtil\.isBlank\(item\.getInspectionTool\(\)\)/)
assert.match(strictMapperBlock, /inspectionItem\.inspectionTool/)
assert.match(strictMapperBlock, /StrUtil\.isBlank\(item\.getSamplingPlanText\(\)\)/)
assert.match(strictMapperBlock, /inspectionItem\.samplingPlanText/)

assert.match(apiSource, /inspectionTool:\s*string\s*\|\s*null/)
assert.match(apiSource, /samplingPlanText:\s*string\s*\|\s*null/)

const detailOpenBlock = blockBetween(
  panelSource,
  'const openPqcMethodDialog',
  'const closePqcStandardDialog'
)
assert.match(detailOpenBlock, /requirePqcInspectionDisplayFields\(item\)/)
assert.match(detailOpenBlock, /message\.error\(resolveErrorMessage\(error\)\)/)

const validateBlock = blockBetween(
  panelSource,
  'const handleValidate',
  'const closePqcSignatureDialog'
)
assert.match(validateBlock, /assertPqcInspectionDisplayFieldsReady\(\)/)

const submitPayloadBlock = blockBetween(
  panelSource,
  'const buildPqcInspectionSubmitPayload',
  'const formatLocalDateTime'
)
assert.match(submitPayloadBlock, /assertPqcInspectionDisplayFieldsReady\(\)/)

console.log('frontline PQC process-list display-field boundary static contract passed')
