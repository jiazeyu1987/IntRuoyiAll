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

for (const itemKey of ['length', 'appearance', 'seal', 'pressure']) {
  assert.match(
    pqcTemplate,
    new RegExp(`data-pqc-inspection-entry="${itemKey}"`),
    `PQC target layout must expose ${itemKey} inspection entry.`
  )
}

for (const choiceKey of ['appearance', 'seal']) {
  assert.match(
    pqcTemplate,
    new RegExp(`data-pqc-inspection-group="${choiceKey}"`),
    `${choiceKey} must use the three-action target layout.`
  )
}

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
  /inspectionQuantity:\s*30\s+as\s+number\s*\|\s*undefined/,
  'PQC first viewport must match the target patrol default inspection quantity of 30.'
)
assert.match(
  source,
  /scrapQuantity:\s*1\s+as\s+number\s*\|\s*undefined/,
  'PQC first viewport must match the target patrol default scrap quantity of 1.'
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
  /isPqcMode \? '返回' : '关闭'/,
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
  /length:\s*\{[\s\S]*defaultValue:\s*'32\.5'[\s\S]*step:\s*0\.1/,
  'Length piece inspection must use the target default and step.'
)
assert.match(
  source,
  /pressure:\s*\{[\s\S]*defaultValue:\s*'50'[\s\S]*step:\s*1/,
  'Pressure piece inspection must use the target default and step.'
)
assert.match(
  source,
  /getPqcPieceStateKey[\s\S]*inspectionType[\s\S]*patrolRound[\s\S]*itemKey/,
  'PQC piece values must be isolated by inspection context and item.'
)
assert.match(
  source,
  /\.frontline-pqc-piece-list\s*\{[\s\S]*grid-template-columns:\s*repeat\(5,/,
  'PQC piece dialog must use the target five-column grid.'
)
assert.match(
  source,
  /PQC 详细检验内容尚未纳入正式模板字段/,
  'Formal PQC submission must continue to fail fast until the payload contract exists.'
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
