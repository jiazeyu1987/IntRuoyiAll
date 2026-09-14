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
  'PQC API contract must expose item-level equipment options from the tenant item equipment config.'
)
assert.match(
  apiSource,
  /equipmentOptions\?: FrontlinePqcEquipmentOptionVO\[\]/,
  'PQC inspection items must carry tenant equipment options instead of one page-level device.'
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
  /itemResults\?: FrontlinePqcItemResultSubmitReqVO\[\]/,
  'PQC submit request must allow itemResults as the formal item-level facts.'
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
const equipmentControlsBlock = blockBetween(
  panelSource,
  '<div class="pqc-utility-strip"',
  'data-pqc-standard-button'
)
assert.match(
  panelSource,
  /const hasPqcEquipmentOptions = \(item: PqcInspectionItem\) =>\s*item\.equipmentOptions\.length > 0/,
  'PQC fill page must use formal item equipment options to decide whether equipment cards render.'
)
assert.match(
  equipmentControlsBlock,
  /<label\s+v-if="hasPqcEquipmentOptions\(activePqcTabItem\)"[\s\S]*data-pqc-equipment-card/,
  'PQC equipment card must render only when the active inspection item has formal equipment.'
)
assert.match(
  equipmentControlsBlock,
  /<label\s+v-if="hasPqcEquipmentOptions\(activePqcTabItem\)"[\s\S]*data-pqc-equipment-number-card/,
  'PQC equipment number card must render only when the active inspection item has formal equipment.'
)
assert.doesNotMatch(
  panelSource,
  /无需检验设备|无需设备编号/,
  'PQC fill page must not display no-equipment placeholder cards for inspection items without equipment.'
)
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
  'PQC fill page must preserve the backend tenant equipmentRequired flag without defaulting missing equipment metadata to required.'
)
const equipmentSelectionBlock = blockBetween(
  panelSource,
  'function assertPqcItemEquipmentSelection(item: PqcInspectionItem) {',
  'const getPqcExactPieceValuesForSubmit = (itemKey: PqcInspectionItemKey) => {'
)
assert.match(
  equipmentSelectionBlock,
  /if \(!hasPqcEquipmentOptions\(item\)\) \{[\s\S]*selectedOption: undefined/,
  'PQC items without tenant equipment config must submit without equipment selection.'
)
assert.match(
  equipmentSelectionBlock,
  /if \(selection\.selectedEquipmentId \|\| selection\.selectedEquipmentNumber\) \{[\s\S]*throw new Error/,
  'PQC items without tenant equipment config must reject stale equipment selections.'
)
assert.match(
  equipmentSelectionBlock,
  /if \(!selection\.selectedEquipmentId\) \{[\s\S]*throw new Error/,
  'PQC items with tenant equipment config must fail fast when selectedEquipmentId is missing.'
)
assert.match(
  equipmentSelectionBlock,
  /if \(!selection\.selectedEquipmentNumber\) \{[\s\S]*throw new Error/,
  'PQC items with tenant equipment config must fail fast when selectedEquipmentNumber is missing.'
)
assert.match(
  equipmentSelectionBlock,
  /item\.equipmentOptions\.find\(\(option\) =>[\s\S]*option\.equipmentId === selection\.selectedEquipmentId[\s\S]*option\.equipmentNumber === selection\.selectedEquipmentNumber/,
  'PQC selected equipment must be checked against the current tenant item equipment config.'
)
const handleValidateBlock = blockBetween(
  panelSource,
  'const handleValidate = async () => {',
  'const closePqcSignatureDialog = () => {'
)
assert.match(
  handleValidateBlock,
  /assertPqcCurrentProcessAllMethodSubmissionReady\(\)/,
  'PQC equipment identity validation must run before the signature dialog opens through the formal submit readiness check.'
)
assert.doesNotMatch(
  panelSource,
  /方法: \$\{item\.inspectionMethod \|\| '未配置'\}[\s\S]*标准: \$\{item\.standardText \|\| '未配置'\}/,
  'PQC item cards must use explicit standard/method actions instead of only compressing the facts into meta text.'
)

assert.doesNotMatch(
  qaApiSource,
  /QaInspectionRegulationSaveEquipmentOptionVO|QaInspectionRegulationEquipmentOptionVO|equipmentOptions\\?:\\s*QaInspectionRegulationSaveEquipmentOptionVO\\[\\]|equipmentRequired\\?:\\s*boolean/,
  'QA regulation API must not expose item-level equipment config; PQC equipment config lives in the tenant-level PQC leader tab.'
)
assert.doesNotMatch(
  qaPageSource,
  /data-qa-regulation-equipment-option|data-qa-regulation-equipment-option-add|buildQaRegulationEquipmentOptions|getQaRegulationItemEquipmentOptions|addQaRegulationEquipmentOption|equipmentOptions|equipmentRequired:/,
  'QA regulation page must not render or serialize inspection equipment config.'
)
assert.doesNotMatch(
  qaSaveReqSource,
  /equipmentRequired|EquipmentOption|equipmentOptions/,
  'Backend QA save VO must not accept item-level equipment config.'
)
assert.doesNotMatch(
  qaServiceSource,
  /for \(MesQaInspectionRegulationSaveReqVO\.EquipmentOption option :|toItemEquipmentDO|equipmentRequired != hasEquipmentOptions|buildEquipmentResponses/,
  'QA regulation service must not persist or validate QA-version-bound equipment options.'
)
assert.doesNotMatch(
  qaServiceSource,
  /itemEquipmentMapper|MesQaInspectionRegulationItemEquipmentMapper/,
  'QA regulation service must not depend on the legacy QA-version equipment mapper; tenant PQC item equipment config is the only equipment source.'
)

console.log('PASS: PQC item equipment, standard, and method static contract')
