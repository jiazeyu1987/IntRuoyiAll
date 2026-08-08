const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const qaApiSource = read('src/api/mes/qc/template/index.ts')
const qaPageSource = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaSaveReqSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/qa/regulation/vo/MesQaInspectionRegulationSaveReqVO.java'
)
const qaServiceSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  apiSource,
  /export interface FrontlinePqcEquipmentOptionVO/,
  'PQC API contract must expose item-level equipment options from the regulation snapshot.'
)
assert.match(
  apiSource,
  /equipmentOptions\?: FrontlinePqcEquipmentOptionVO\[\]/,
  'PQC inspection items must carry equipment options instead of one page-level device.'
)
for (const field of [
  'equipmentRequired?: boolean',
  'standardLowerLimit?: number | string',
  'standardUpperLimit?: number | string',
  'standardUnit?: string',
  'standardPrecision?: number'
]) {
  assert.ok(apiSource.includes(field), `PQC inspection item type must include ${field}.`)
}
assert.match(
  apiSource,
  /export interface FrontlinePqcItemResultSubmitReqVO[\s\S]*itemCode: string[\s\S]*selectedEquipmentId\?: number[\s\S]*selectedEquipmentNumber\?: string[\s\S]*sampleValues: string\[\]/,
  'PQC submit contract must allow QA items without equipment to omit selected equipment fields.'
)
assert.match(
  apiSource,
  /itemResults: FrontlinePqcItemResultSubmitReqVO\[\]/,
  'PQC submit request must send itemResults as the formal item-level facts.'
)

for (const selector of [
  'data-pqc-equipment-select',
  'data-pqc-equipment-number-select',
  'data-pqc-standard-button',
  'data-pqc-method-button',
  'data-pqc-standard-dialog',
  'data-pqc-method-dialog'
]) {
  assert.ok(panelSource.includes(selector), `PQC fill page must expose ${selector}.`)
}
assert.match(
  panelSource,
  /activePqcStandardItem[\s\S]*standardLowerLimit[\s\S]*standardUpperLimit[\s\S]*standardUnit/,
  'PQC standard dialog must display regulation version standard text, lower/upper bounds, and unit.'
)
assert.match(
  panelSource,
  /activePqcMethodItem[\s\S]*inspectionMethod/,
  'PQC method dialog must display the formal regulation inspection method.'
)
assert.match(
  panelSource,
  /const pqcItemSelections = reactive<Record<PqcInspectionItemKey, PqcItemSelection>>/,
  'PQC page must keep item-level equipment selections separate from piece values.'
)
assert.match(
  panelSource,
  /buildPqcItemResultsPayload[\s\S]*selectedEquipmentId[\s\S]*selectedEquipmentNumber[\s\S]*sampleValues/,
  'PQC submit payload must still include selected equipment fields when the QA item requires equipment.'
)
assert.match(
  panelSource,
  /itemResults: buildPqcItemResultsPayload\(\)/,
  'PQC submit request must include itemResults outside rawPayload.'
)

assert.match(
  panelSource,
  /equipmentRequired:\s*item\.equipmentRequired === true,/,
  'PQC fill page must preserve the backend QA equipmentRequired flag without defaulting missing equipment metadata to required.'
)
assert.doesNotMatch(
  panelSource,
  /assertPqcSubmissionItemEquipmentSelections\(\)/,
  'PQC formal submit must not force equipment selection before opening the signature dialog.'
)
const requireSelectionBlock = blockBetween(
  panelSource,
  'const requirePqcItemSelection = (item: PqcInspectionItem) => {',
  'const getPqcExactPieceValuesForSubmit = (itemKey: PqcInspectionItemKey) => {'
)
assert.match(
  requireSelectionBlock,
  /if \(!hasSelectedEquipment\) \{[\s\S]*selectedOption: undefined/,
  'PQC items must pass equipment selection resolution when the operator leaves equipment blank.'
)
assert.match(
  requireSelectionBlock,
  /if \(!selection\.selectedEquipmentId\) \{[\s\S]*throw new Error/,
  'PQC partial equipment selection must still fail fast when selectedEquipmentId is missing.'
)
assert.match(
  requireSelectionBlock,
  /if \(!selection\.selectedEquipmentNumber\) \{[\s\S]*throw new Error/,
  'PQC partial equipment selection must still fail fast when selectedEquipmentNumber is missing.'
)
assert.match(
  requireSelectionBlock,
  /if \(!item\.equipmentOptions\.length\)/,
  'PQC selected equipment must still be checked against formal QA equipment options.'
)
assert.ok(
  panelSource.includes("item.equipmentOptions.length ? '设备可选' : '无需设备'"),
  'PQC tab requirement text must show equipment as optional instead of formal-submit required.'
)
const handleValidateBlock = blockBetween(
  panelSource,
  'const handleValidate = async () => {',
  'const closePqcSignatureDialog = () => {'
)
assert.doesNotMatch(
  handleValidateBlock,
  /assertPqcSubmissionItemEquipmentSelections\(\)/,
  'PQC equipment identity validation must not run before the signature dialog opens.'
)
assert.doesNotMatch(
  panelSource,
  /方法: \$\{item\.inspectionMethod \|\| '未配置'\}[\s\S]*标准: \$\{item\.standardText \|\| '未配置'\}/,
  'PQC item cards must use explicit standard/method actions instead of only compressing the facts into meta text.'
)

assert.match(
  qaApiSource,
  /export interface QaInspectionRegulationSaveEquipmentOptionVO[\s\S]*equipmentId: number[\s\S]*equipmentCode: string[\s\S]*equipmentName: string[\s\S]*equipmentNumber: string/,
  'QA regulation save API must carry formal item-level equipment options, not only equipmentRequired.'
)
assert.match(
  qaApiSource,
  /equipmentOptions\?: QaInspectionRegulationSaveEquipmentOptionVO\[\]/,
  'Each QA regulation item payload must include equipmentOptions from the QA inspection item.'
)
assert.match(
  qaPageSource,
  /const equipmentOptions = buildQaRegulationItemEquipmentOptions\(item\)/,
  'QA regulation page must build formal item equipment options before serializing inspection items.'
)
assert.match(
  qaPageSource,
  /equipmentOptions,\s*\r?\n\s*resultType:/,
  'QA regulation page must serialize each inspection item equipment option into the save payload.'
)
assert.doesNotMatch(
  qaPageSource,
  /equipmentRequired:\s*Boolean\(item\.inspectionTool\.trim\(\)\),/,
  'QA regulation page must not downgrade the equipment column to a boolean-only save contract.'
)
assert.match(
  qaSaveReqSource,
  /private List<EquipmentOption> equipmentOptions;/,
  'Backend QA save VO must accept item-level equipment options.'
)
assert.match(
  qaSaveReqSource,
  /public static class EquipmentOption[\s\S]*private Long equipmentId;[\s\S]*private String equipmentCode;[\s\S]*private String equipmentName;[\s\S]*private String equipmentNumber;/,
  'Backend QA save VO equipment options must include formal equipment identity and number.'
)
assert.match(
  qaServiceSource,
  /MesQaInspectionRegulationItemEquipmentMapper itemEquipmentMapper/,
  'QA regulation service must own writes to the item equipment mapper.'
)
assert.match(
  qaServiceSource,
  /itemEquipmentMapper\.deleteByVersionId\(version\.getId\(\)\)/,
  'Saving an existing QA draft must replace stale item equipment rows with the new formal options.'
)
assert.match(
  qaServiceSource,
  /itemEquipmentMapper\.insert\(toItemEquipmentDO\(version\.getId\(\), itemReqVO, equipmentOption\)\)/,
  'QA regulation service must persist each item equipment option for the published PQC snapshot.'
)
assert.match(
  qaServiceSource,
  /equipmentRequired != hasEquipmentOptions/,
  'QA regulation service must fail fast when equipmentRequired and formal equipment options disagree.'
)

console.log('PASS: PQC item equipment, standard, and method static contract')
