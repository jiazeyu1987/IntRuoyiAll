import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const listPath = 'src/views/dcc/registration-certificate/index/index.vue'
const list = read(listPath)

assert.match(
  list,
  /const CURRENT_TABLE_KEY = 'dcc\.registrationCertificate\.current\.actionsWideV2'/,
  'current registration-certificate table must use a new versioned key so old narrow widths do not override the widened layout'
)
assert.match(
  list,
  /const OLD_TABLE_KEY = 'dcc\.registrationCertificate\.old\.actionsWideV2'/,
  'old registration-certificate table must use the same widened-layout version key pattern'
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
  /\{ key: 'actions', label: '操作', width: 420, hideable: false, business: false, sortable: false \}/,
  'current registration-certificate action column must default to 420px'
)
assert.match(
  oldColumns,
  /\{ key: 'actions', label: '操作', width: 420, hideable: false, business: false, sortable: false \}/,
  'old registration-certificate action column must default to 420px'
)

assert.match(
  list,
  /:width="getCurrentColumnWidthString\('actions', 420\)"/,
  'current registration-certificate action column must render with the widened 420px fallback'
)
assert.match(
  list,
  /:width="getOldColumnWidthString\('actions', 420\)"/,
  'old registration-certificate action column must render with the widened 420px fallback'
)

const currentActionPanel = /<el-table-column[\s\S]*?v-if="isCurrentColumnVisible\('actions'\)"[\s\S]*?<\/el-table-column>/.exec(list)?.[0] ?? ''
const oldActionPanel = /<el-table-column[\s\S]*?v-if="isOldColumnVisible\('actions'\)"[\s\S]*?<\/el-table-column>/.exec(list)?.[0] ?? ''

for (const [label, panel] of [
  ['current', currentActionPanel],
  ['old', oldActionPanel]
]) {
  assert.match(
    panel,
    /<div class="registration-certificate-row-actions">/,
    `${label} registration-certificate action panel must use a dedicated row-actions container`
  )
  assert.match(
    panel,
    />\s*详情\s*</,
    `${label} registration-certificate action panel must keep the detail action visible`
  )
  assert.match(
    panel,
    />\s*项目代码\s*</,
    `${label} registration-certificate action panel must keep the project-code action visible`
  )
}

assert.match(
  list,
  /\.registration-certificate-row-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*width:\s*100%;[\s\S]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px\s+8px;/,
  'registration-certificate row actions must use a four-column grid that fills the widened operation column'
)
assert.match(
  list,
  /\.registration-certificate-row-actions :deep\(\.el-button\)\s*\{[\s\S]*margin-left:\s*0;[\s\S]*white-space:\s*nowrap;/,
  'registration-certificate row action buttons must keep Element Plus spacing from expanding the layout'
)

console.log('PASS: registration-certificate operation panel widened layout static contract')
