import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const listPath = 'src/views/dcc/registration-certificate/index/index.vue'
const list = read(listPath)
const detail = read('src/views/dcc/registration-certificate/detail/index.vue')
const api = read('src/api/dcc/registrationCertificate/index.ts')

assert.match(
  list,
  /const CURRENT_TABLE_KEY = 'dcc\.registrationCertificate\.current\.actionsDoubleWidthV1'/,
  'current registration-certificate table must use a new versioned key so old compact widths do not override the doubled operation column'
)
assert.match(
  list,
  /const OLD_TABLE_KEY = 'dcc\.registrationCertificate\.old\.actionsDoubleWidthV1'/,
  'old registration-certificate table must use a new versioned key so old compact widths do not override the doubled operation column'
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
  /\{ key: 'actions', label: '操作', width: 280, hideable: false, business: false, sortable: false \}/,
  'current registration-certificate action column must default to double the previous 140px width'
)
assert.match(
  oldColumns,
  /\{ key: 'actions', label: '操作', width: 420, hideable: false, business: false, sortable: false \}/,
  'old registration-certificate action column must default to double the previous 210px width'
)

assert.match(
  list,
  /:width="getCurrentColumnWidthString\('actions', 280\)"/,
  'current registration-certificate action column must render with the doubled 280px fallback'
)
assert.match(
  list,
  /:width="getOldColumnWidthString\('actions', 420\)"/,
  'old registration-certificate action column must render with the doubled 420px fallback'
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
  3,
  'current registration-certificate action panel must render exactly three buttons'
)
assert.match(
  currentActionPanel,
  /<el-button link type="primary" @click="openDetail\(row\.certificateId\)">\s*详细\s*<\/el-button>/,
  'current registration-certificate action panel must show the detail action as 详细 and keep the handler'
)
assert.match(
  currentActionPanel,
  /v-hasPermi="\['dcc:registration-certificate:change:submit'\]"[\s\S]*@click="openChange\(row\)"[\s\S]*>\s*变更\s*</,
  'current registration-certificate action panel must show the change action as 变更 and keep the change approval upload permission and handler'
)
assert.match(
  list,
  /const openChange = \(row: DccRegistrationCertificatePageItemVO\) => \{[\s\S]*selectedChangeCertificate\.value = row[\s\S]*showChangeDialog\.value = true/,
  'current registration-certificate change button must open the row-level change dialog'
)
assert.match(
  currentActionPanel,
  /v-hasPermi="\['dcc:registration-certificate:renewal:upload'\]"[\s\S]*@click="openRenewalDialog\(row\)"[\s\S]*>\s*延续\s*</,
  'current registration-certificate action panel must keep the renewal permission and handler'
)
assert.match(
  currentActionPanel,
  />\s*详细\s*<[\s\S]*>\s*延续\s*<[\s\S]*>\s*变更\s*</,
  'current registration-certificate action panel must order buttons as 详细、延续、变更'
)
assert.doesNotMatch(
  currentActionPanel,
  />\s*(产品|项目代码|申请查看)\s*</,
  'current registration-certificate action panel must remove non-detail and non-renewal actions'
)
assert.match(
  oldActionPanel,
  /<div class="registration-certificate-row-actions registration-certificate-row-actions--compact registration-certificate-row-actions--old-manager-view">/,
  'old registration-certificate action panel must use the manager-view row-actions container'
)
assert.equal(
  (oldActionPanel.match(/<el-button\b/g) ?? []).length,
  3,
  'old registration-certificate action panel must render exactly three buttons'
)
assert.match(
  oldActionPanel,
  /<el-button link type="primary" @click="openOldDetail\(row\.certificateId,\s*row\.versionId\)">\s*详情\s*<\/el-button>/,
  'old registration-certificate action panel must keep the detail action and handler'
)
assert.match(
  oldActionPanel,
  /<el-button\s+v-hasRole="\['dcc_registration_certificate_approver'\]"\s+link\s+type="success"\s+@click="openOldDirectView\(row\.certificateId,\s*row\.versionId\)">\s*查看\s*<\/el-button>/,
  'old registration-certificate action panel must show direct old-certificate view only for the registration-manager role'
)
assert.match(
  oldActionPanel,
  /<el-button link type="warning" @click="openOldAccessRequest\(row\.certificateId\)">\s*申请查看\s*<\/el-button>/,
  'old registration-certificate action panel must keep the old-certificate access request action and handler'
)
assert.doesNotMatch(
  oldActionPanel,
  />\s*(产品|项目代码)\s*</,
  'old registration-certificate action panel must remove the red-box product and project-code actions'
)

assert.match(
  list,
  /\.registration-certificate-row-actions\s*\{[\s\S]*display:\s*grid;[\s\S]*width:\s*100%;[\s\S]*grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px\s+8px;/,
  'existing registration-certificate row actions must keep the four-column grid for non-compact panels'
)
assert.match(
  list,
  /\.registration-certificate-row-actions--compact\s*\{[\s\S]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px;/,
  'current registration-certificate compact row actions must use a three-column grid so 详细、延续、变更 stay on one row'
)
assert.match(
  list,
  /\.registration-certificate-row-actions--old-manager-view\s*\{[\s\S]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\);[\s\S]*gap:\s*4px;/,
  'old registration-certificate manager-view row actions must use a three-column grid inside the compact operation column'
)
assert.match(
  list,
  /\.registration-certificate-row-actions :deep\(\.el-button\)\s*\{[\s\S]*margin-left:\s*0;[\s\S]*white-space:\s*nowrap;/,
  'registration-certificate row action buttons must keep Element Plus spacing from expanding the layout'
)

assert.doesNotMatch(
  detail,
  /route\.query\.mode === 'change'/,
  'registration-certificate detail page must not keep the list-level change mode'
)
assert.match(
  detail,
  /data-testid="registration-certificate-change-history"[\s\S]*变更履历/,
  'registration-certificate detail page must show change history'
)
assert.match(
  api,
  /changeId\?: number \| string[\s\S]*changeStatus\?: string[\s\S]*submittedByName\?: string[\s\S]*reviewedByName\?: string/,
  'registration-certificate history API type must expose change audit fields used by detail history'
)

console.log('PASS: registration-certificate operation panel compact layout static contract')
