const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewPath = path.join(root, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const source = fs.readFileSync(viewPath, 'utf8').replace(/\r\n/g, '\n')

const columnsStart = source.indexOf('const qaItemsDefaultColumns')
const columnsEnd = source.indexOf('const qaChecksDefaultColumns', columnsStart)
assert.ok(columnsStart >= 0 && columnsEnd > columnsStart, 'QA item default column block must exist.')
const columnsBlock = source.slice(columnsStart, columnsEnd)

assert.match(
  columnsBlock,
  /\{ key: 'applicableTypes', label: '适用检验类型', minWidth: 210 \}/,
  'Applicable inspection types must be visible in the default QA item table layout.'
)
assert.doesNotMatch(
  columnsBlock,
  /key: 'applicableTypes'[^\n]*visible:\s*false/,
  'Applicable inspection types must not remain hidden by default.'
)

const nextTableKey = 'mes.qa.regulation.items.processMethods.v2'
assert.match(
  source,
  new RegExp(`table-key="${nextTableKey.replaceAll('.', '\\.')}"`),
  'UnifiedListTemplate must use the upgraded QA item table key.'
)
assert.match(
  source,
  new RegExp(`data-user-table-key="${nextTableKey.replaceAll('.', '\\.')}"`),
  'The rendered Element Plus table must use the upgraded QA item table key.'
)
assert.match(
  source,
  new RegExp(`useUserTableColumns\\('${nextTableKey.replaceAll('.', '\\.')}', qaItemsDefaultColumns\\)`),
  'Saved user column settings must use the same upgraded QA item table key.'
)

assert.match(
  source,
  /v-if="isQaItemsColumnVisible\('applicableTypes'\)"[\s\S]*label="适用检验类型"[\s\S]*data-qa-regulation-applicable-types[\s\S]*resolveQaItemApplicableTypes\(row\)/,
  'The visible applicable inspection type column must render the formal derived values.'
)
assert.doesNotMatch(
  source,
  /v-model="row\.applicableTypes"/,
  'The derived applicable inspection types must not remain independently editable.'
)

console.log('PASS: QA regulation applicable inspection types are visible by default')
