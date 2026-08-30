import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const listPath = 'src/views/dcc/registration-certificate/index/index.vue'
const list = read(listPath)

assert.match(
  list,
  /const CURRENT_TABLE_KEY = 'dcc\.registrationCertificate\.current\.actionsCompactV3'/,
  'current registration-certificate table must use a new versioned key so old widened widths do not override the compact operation column'
)
assert.match(
  list,
  /const OLD_TABLE_KEY = 'dcc\.registrationCertificate\.old\.actionsWideV2'/,
  'old registration-certificate table key must stay unchanged because the compact two-action requirement only applies to the current certificate list'
)
assert.match(
  list,
  /:table-key="CURRENT_TABLE_KEY"/,
  'current registration-certificate list must bind the widened table key as a reactive prop'
)
assert.match(
  list,
  /:data-user-table-key="CURRENT_TABLE_KEY"/,
  'current registration-certificate table DOM anchor must use the widened table key'
)
assert.match(
  list,
  /:table-key="OLD_TABLE_KEY"/,
  'old registration-certificate list must bind the widened table key into the template'
)
assert.match(
  list,
  /:data-user-table-key="OLD_TABLE_KEY"/,
  'old registration-certificate table DOM anchor must use the widened table key'
)

const currentColumns = /const currentColumnDefinitions: UserTableColumnDefinition\[\] = \[([\s\S]*?)\n\]\n\nconst oldColumnDefinitions/.exec(list)?.[1] ?? ''
const oldColumns = /const oldColumnDefinitions: UserTableColumnDefinition\[\] = \[([\s\S]*?)\n\]\n\nconst \{/.exec(list)?.[1] ?? ''

assert.match(
  currentColumns,
  /\{ key: 'actions', label: '操作', width: 140, hideable: false, business: false, sortable: false \}/,
  'current registration-certificate action column must default to one third of the previous 420px width'
)
assert.match(
  oldColumns,
  /\{ key: 'actions', label: '操作', width: 420, hideable: false, business: false, sortable: false \}/,
  'old registration-certificate action column must keep its existing 420px default'
)

assert.match(
  list,
  /:width="getCurrentColumnWidthString\('actions', 140\)"/,
  'current registration-certificate action column must render with the compact 140px fallback'
)
assert.match(
  list,
  /:width="getOldColumnWidthString\('actions', 420\)"/,
  'old registration-certificate action column must keep the existing 420px fallback'
)

const extractActionPanel = (source, visibilityToken) => {
  const markerIndex = source.indexOf(visibilityToken)
  assert.notEqual(markerIndex, -1, `${visibilityToken} must exist`)
  const startIndex = source.lastIndexOf('<el-table-column', markerIndex)
  assert.notEqual(startIndex, -1, `${visibilityToken} action column must have an el-table-column start`)
  const endIndex = source.indexOf('</el-table-column>', markerIndex)
  assert.notEqual(endIndex, -1, `${visibilityToken} action column must have an el-table-column end`)
  return source.slice(startIndex, endIndex + '</el-table-column>'.length)
}

const currentActionPanel = extractActionPanel(list, `v-if="isCurrentColumnVisible('actions')"`)
const oldActionPanel = extractActionPanel(list, `v-if="isOldColumnVisible('actions')"`)

assert.match(
  currentActionPanel,
  /<div class="registration-certificate-row-actions registration-certificate-row-actions--compact">/,
  'current registration-certificate action panel must use the compact row-actions container'
)
assert.equal(
  (currentActionPanel.match(/<el-button\b/g) ?? []).length,
  2,
  'current registration-certificate action panel must render exactly two buttons'
)
assert.match(
  currentActionPanel,
  /<el-button link type="primary" @click="openDetail\(row\.certificateId\)">\s*详情\s*<\/el-button>/,
  'current registration-certificate action panel must keep the detail action and handler'
)
assert.match(
  currentActionPanel,
  /v-hasPermi="\['dcc:registration-certificate:renewal:upload'\]"[\s\S]*@click="openRenewalDialog\(row\)"[\s\S]*>\s*延续\s*</,
  'current registration-certificate action panel must keep the renewal permission and handler'
)
assert.doesNotMatch(
  currentActionPanel,
  />\s*(产品|项目代码|申请查看)\s*</,
  'current registration-certificate action panel must remove non-detail and non-renewal actions'
)
assert.match(
  oldActionPanel,
  /<div class="registration-certificate-row-actions">/,
  'old registration-certificate action panel must keep its existing row-actions container'
)

assert.match(
  list,
  /\.registration-certificate-row-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*width:\s*100%;[\s\S]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px\s+8px;/,
  'existing registration-certificate row actions must keep the four-column grid for non-compact panels'
)
assert.match(
  list,
  /\.registration-certificate-row-actions--compact\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px;/,
  'current registration-certificate compact row actions must use a two-column grid'
)
assert.match(
  list,
  /\.registration-certificate-row-actions :deep\(\.el-button\)\s*\{[\s\S]*margin-left:\s*0;[\s\S]*white-space:\s*nowrap;/,
  'registration-certificate row action buttons must keep Element Plus spacing from expanding the layout'
)

console.log('PASS: registration-certificate operation panel compact layout static contract')
