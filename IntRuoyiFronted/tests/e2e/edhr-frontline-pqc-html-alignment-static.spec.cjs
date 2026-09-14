const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(viewPath, 'utf8').replace(/\r\n/g, '\n')

const pqcStart = source.indexOf('v-if="isPqcMode"')
const pqcEnd = source.indexOf('\n    <div\n      v-else', pqcStart)
assert.ok(pqcStart >= 0 && pqcEnd > pqcStart, 'PQC template block must exist.')
const pqcTemplate = source.slice(pqcStart, pqcEnd)

assert.match(
  pqcTemplate,
  /v-for="item in pqcInspectionItems"[\s\S]*data-pqc-inspection-tab/,
  'PQC target layout must render formal QA/PQC inspection entries dynamically.'
)
assert.match(
  pqcTemplate,
  /:data-pqc-inspection-entry="activePqcTabItem\.key"/,
  'PQC target layout must expose a stable entry selector for the active formal inspection item.'
)
assert.match(
  pqcTemplate,
  /frontline-pqc-choice-actions[\s\S]*applyPqcBulkChoice\(activePqcTabItem\.key/,
  'PQC choice actions must use the active formal item key for grouped actions.'
)
assert.match(
  pqcTemplate,
  /formatPqcInspectionItemTabLabel\(item\)/,
  'PQC target layout must display the formal inspection item name through the tab label helper.'
)

for (const label of ['全部合格', '全部不良', '逐件选择']) {
  assert.match(pqcTemplate, new RegExp(label), `PQC target layout must include ${label}.`)
}
assert.match(
  source,
  /getPqcProgressText[\s\S]*`已填 \$\{getPqcCompletedCount\(itemKey\)\}\/\$\{pqcInspectionQuantity\.value\}`/,
  'PQC inspection entries must render the target completed quantity text.'
)
assert.match(
  source,
  /applyPqcTaskOptionToDraft[\s\S]*pqcDraft\.inspectionQuantity = storedDraft\.inspectionQuantity \?\? option\.plannedInspectionQuantity/,
  'PQC inspection quantity must default from the formal PQC task snapshot while preserving an operator draft.'
)
assert.match(
  source,
  /applyPqcTaskOptionToDraft[\s\S]*pqcDraft\.scrapQuantity = undefined/,
  'PQC scrap quantity must start empty instead of using a hard-coded default.'
)
assert.match(
  pqcTemplate,
  /activePqcInspectionItem[\s\S]*pqcInspectionQuantity[\s\S]*件/,
  'PQC piece dialog title must continue to use the current inspection quantity.'
)
assert.match(
  source,
  /activePicker === 'process' \? '选工序' : '选择员工'/,
  'PQC process picker title must match the target text 选工序.'
)
assert.match(
  source,
  /class="frontline-picker__close picker-close"[\s\S]*返回/,
  'PQC picker close action must match the target text 返回.'
)

assert.match(
  pqcTemplate,
  /data-pqc-piece-modal/,
  'PQC page must render the target piece inspection dialog.'
)
assert.match(
  pqcTemplate,
  /data-pqc-piece-list/,
  'PQC piece dialog must expose the grid list.'
)
assert.match(
  pqcTemplate,
  /class="frontline-pqc-reset-button"/,
  'PQC footer must expose the target reset action.'
)
assert.match(
  pqcTemplate,
  /@click="handleResetPqc"/,
  'PQC reset action must clear the current local inspection context.'
)

assert.match(
  source,
  /const pqcInspectionItems = computed<PqcInspectionItem\[\]>[\s\S]*deviceState\.selectedProcess\.inspectionItems\.map\(mapPqcInspectionItem\)/,
  'PQC method tab definitions must come from the selected process formal QA/PQC item snapshot.'
)
assert.match(
  source,
  /const pqcTaskInspectionItems = computed<PqcInspectionItem\[\]>\(\(\) =>\s*\(activePqcTaskOption\.value\?\.inspectionItems \|\| \[\]\)\.map\(mapPqcInspectionItem\)/,
  'PQC submission item definitions must still come from the active formal PQC task snapshot.'
)
assert.match(
  source,
  /key: item\.itemCode[\s\S]*itemName: normalizePqcInspectionItemName\(item\.itemName\)[\s\S]*label: normalizePqcInspectionItemName\(item\.itemName\) \|\| '未配置检验项目名称'[\s\S]*type: isPqcNumericResultType\(item\.resultType\) \? 'number' : 'choice'/,
  'PQC item key, formal item name, display label, and value type must use the formal QA/PQC snapshot fields.'
)
assert.match(
  source,
  /inspectionMethod: item\.inspectionMethod \|\| ''[\s\S]*standardText: item\.standardText \|\| ''[\s\S]*acceptanceStandard: item\.acceptanceStandard \|\| item\.standardText \|\| ''[\s\S]*processInspectionMethod: item\.processInspectionMethod \|\| item\.inspectionMethod \|\| ''[\s\S]*resultType: item\.resultType/,
  'PQC item metadata must preserve legacy method/standard and the QA process method/standard aliases.'
)
assert.match(
  source,
  /hasPqcTaskSnapshot[\s\S]*getSelectedPqcTaskOption\(process\)/,
  'PQC mode must fail fast when the formal inspection item snapshot is missing.'
)
assert.match(
  source,
  /getPqcPieceStateKey[\s\S]*pqcTaskId[\s\S]*inspectionType[\s\S]*roundNo[\s\S]*itemKey/,
  'PQC piece values must be isolated by inspection context and item.'
)
assert.match(
  source,
  /\.frontline-pqc-piece-list\s*\{[\s\S]*grid-template-columns:\s*repeat\(5,/,
  'PQC piece dialog must use the target five-column grid.'
)
assert.match(
  source,
  /submitFrontlinePqcInspection[\s\S]*buildPqcInspectionSubmitPayload/,
  'Formal PQC submission must use the dedicated PQC inspection payload contract.'
)
assert.doesNotMatch(
  source,
  /PQC 详细检验内容尚未纳入正式模板字段/,
  'PQC mode must not keep the obsolete placeholder fail-fast once the formal payload contract exists.'
)
assert.doesNotMatch(
  source,
  /defaultValue:\s*'32\.5'|defaultValue:\s*'50'|itemKey of \['length', 'appearance', 'seal', 'pressure'\]/,
  'PQC mode must not restore old hard-coded inspection items or pseudo defaults.'
)

for (const oldBinding of [
  'v-model="pqcDraft.lengthCm"',
  'v-model="pqcDraft.appearanceQualified"',
  'v-model="pqcDraft.sealQualified"',
  'v-model="pqcDraft.pressureMpa"'
]) {
  assert.doesNotMatch(
    pqcTemplate,
    new RegExp(oldBinding.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `PQC target layout must remove old batch-level binding ${oldBinding}.`
  )
}

console.log('PASS: eDHR frontline PQC HTML alignment static contract')
