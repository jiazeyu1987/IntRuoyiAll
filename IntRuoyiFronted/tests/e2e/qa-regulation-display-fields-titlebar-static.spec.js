const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(projectRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const itemsStart = source.indexOf('<ContentWrap v-show="qaActiveTab === \'items\'">')
const itemsEnd = source.indexOf('<ContentWrap v-show="qaActiveTab === \'verification\'">', itemsStart)
assert.ok(itemsStart >= 0, 'QA inspection items section must exist.')
assert.ok(itemsEnd > itemsStart, 'QA inspection items section must have a stable end marker.')

const itemsSection = source.slice(itemsStart, itemsEnd)
const headerStart = itemsSection.indexOf('<template #header>')
const headerEnd = itemsSection.indexOf('</template>', headerStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'Inspection items card header must exist.')

const header = itemsSection.slice(headerStart, headerEnd)
const displayFieldsIndex = header.indexOf('<UserTableColumnSettings')
const addButtonIndex = header.indexOf('新增检验方法')

assert.match(
  source,
  /import UserTableColumnSettings from '@\/components\/UserTableColumnSettings\/index\.vue'/,
  'QA regulation page must directly import the display-field control for the card title bar.'
)
assert.match(
  header,
  /class="qa-regulation-page__card-actions"[\s\S]*<UserTableColumnSettings/,
  'The inspection items title bar must provide a dedicated action group containing display fields.'
)
assert.ok(
  displayFieldsIndex >= 0 && addButtonIndex > displayFieldsIndex,
  'Display fields must appear immediately before Add inspection method in the title bar.'
)
assert.match(header, /:columns="qaItemsColumns"/, 'Moved display fields must keep the item column state.')
assert.match(header, /:saving="qaItemsColumnSaving"/, 'Moved display fields must keep the saving state.')
assert.match(header, /:show-reset="false"/, 'Moved display fields must not add a reset-column button.')
assert.match(
  header,
  /@change="saveQaItemsColumnConfig"/,
  'Moved display fields must keep automatic column configuration persistence.'
)
assert.match(
  itemsSection,
  /table-key="mes\.qa\.regulation\.items\.processMethods"[\s\S]*:show-column-settings="false"/,
  'The inspection items list must disable the old toolbar display-field control.'
)
assert.match(
  itemsSection,
  /table-key="mes\.qa\.regulation\.items\.processMethods"[\s\S]*:show-query-form="false"/,
  'The inspection items list must remove the empty toolbar row after moving its only control.'
)
assert.match(
  source,
  /\.qa-regulation-page__card-actions\s*\{[\s\S]*display:\s*(?:inline-)?flex[\s\S]*gap:\s*12px/,
  'The title-bar buttons must stay aligned with the screenshot spacing.'
)

console.log('QA regulation display-fields title-bar static contract passed.')
