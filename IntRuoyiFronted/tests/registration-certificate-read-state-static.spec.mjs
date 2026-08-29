import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'src/api/dcc/registrationCertificate/index.ts'
const statePath = 'src/views/dcc/registration-certificate/shared/state.ts'
const indexPath = 'src/views/dcc/registration-certificate/index/index.vue'
const detailPath = 'src/views/dcc/registration-certificate/detail/index.vue'
const historyPath = 'src/views/dcc/registration-certificate/history/index.vue'

for (const file of [apiPath, statePath, indexPath, detailPath, historyPath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(
  api,
  /type\s+DccRegistrationCertificateStatus\s*=/,
  'API exposes explicit server status union'
)
for (const status of ['DRAFT', 'PENDING_EFFECTIVE', 'CURRENT', 'OLD', 'VOIDED']) {
  assert.match(api, new RegExp(`['"]${status}['"]`), `API keeps server status ${status}`)
}
for (const endpoint of [
  '/dcc/registration-certificates/page',
  '/dcc/registration-certificates/old-index/page',
  '/dcc/registration-certificates/${id}',
  '/dcc/registration-certificates/${id}/history'
]) {
  assert.match(
    api,
    new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `API endpoint ${endpoint} is frozen`
  )
}
for (const exported of [
  'getRegistrationCertificatePage',
  'getRegistrationCertificateOldIndexPage',
  'getRegistrationCertificateDetail',
  'getRegistrationCertificateHistory'
]) {
  assert.match(api, new RegExp(`export\\s+const\\s+${exported}\\b`), `${exported} must be exported`)
}
assert.doesNotMatch(
  api,
  /localStorage|sessionStorage|new\s+Date|Date\.now/,
  'API wrapper must not persist or calculate state locally'
)

const state = read(statePath)
assert.match(
  state,
  /REGISTRATION_CERTIFICATE_STATUS_META/,
  'shared state metadata must be centralized'
)
for (const [status, label] of [
  ['DRAFT', '草稿'],
  ['PENDING_EFFECTIVE', '待生效'],
  ['CURRENT', '当前有效'],
  ['OLD', '旧证'],
  ['VOIDED', '已作废']
]) {
  assert.match(
    state,
    new RegExp(`${status}[\\s\\S]{0,120}${label}`),
    `${status} label must be explicit`
  )
}
assert.match(state, /formatMissingMarker/, 'missing-marker formatter must be explicit')
assert.doesNotMatch(
  state,
  /new\s+Date|Date\.now|effectiveDate\s*[<>]=?|expiryDate\s*[<>]=?/,
  'state metadata must not calculate status from dates'
)

const index = read(indexPath)
assert.match(
  index,
  /import\s+UnifiedListTemplate\s+from\s+['\"]@\/components\/UnifiedListTemplate\/index\.vue['\"]/,
  'registration-certificate lists must use the project UnifiedListTemplate'
)
assert.equal(
  (index.match(/<UnifiedListTemplate\b/g) || []).length,
  2,
  'current and old certificate tabs must each render a UnifiedListTemplate'
)
for (const tableKey of [
  'dcc.registrationCertificate.current.actionsWideV2',
  'dcc.registrationCertificate.old.actionsWideV2'
]) {
  assert.match(
    index,
    new RegExp(`const\\s+(CURRENT|OLD)_TABLE_KEY\\s*=\\s*'${tableKey}'`),
    `${tableKey} must have its own standard list identity`
  )
}
assert.match(index, /:table-key="CURRENT_TABLE_KEY"/, 'current list must bind the standard list identity')
assert.match(index, /:table-key="OLD_TABLE_KEY"/, 'old list must bind the standard list identity')
for (const token of [
  'getRegistrationCertificatePage',
  'getRegistrationCertificateOldIndexPage',
  'formatRegistrationCertificateStatus',
  'formatMissingMarker',
  'data-testid="registration-certificate-read-page"',
  'data-testid="registration-certificate-old-index"',
  'data-testid="registration-certificate-tabs"',
  'el-tab-pane',
  'activeTab',
  '注册证',
  '老证',
  'hasProjectCode',
  'hasRegistrationFile'
]) {
  assert.match(
    index,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `index page must contain ${token}`
  )
}
assert.doesNotMatch(
  index,
  /router\.replace|router\.push\(\{\s*query|localStorage|sessionStorage|new\s+Date|Date\.now/,
  'index page must not persist filters or compute server state'
)

const detail = read(detailPath)
for (const token of [
  'getRegistrationCertificateDetail',
  'getRegistrationCertificateHistory',
  'formatRegistrationCertificateStatus',
  'formatEntrustedEnterpriseNames',
  'data-testid="registration-certificate-detail-page"',
  'hasRegistrationFile'
]) {
  assert.match(
    detail,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `detail page must contain ${token}`
  )
}
assert.doesNotMatch(
  detail,
  /<pre\s+class="detail-json">[\s\S]*entrustedEnterprisesJson[\s\S]*<\/pre>/,
  'detail page must not render raw entrustedEnterprisesJson JSON'
)
assert.match(
  detail,
  /<RegistrationCertificateActionPanel/,
  'detail page must expose the workflow action panel for access requests'
)
assert.match(
  detail,
  /:read-only="viewMode !== 'current'"/,
  'detail page must lock non-current certificate maintenance actions'
)
assert.match(
  detail,
  /:initial-action="viewMode === 'current' \? 'draft' : 'access'"/,
  'old/detail access modes must open the access request action first'
)
assert.match(
  detail,
  /:downloadable-files="downloadableFiles"/,
  'detail page must pass formal downloadable file options to the action panel'
)
assert.doesNotMatch(
  detail,
  /<el-descriptions-item\s+label="备注"/,
  'remark must move out of the registration certificate descriptions table'
)
assert.match(
  detail,
  /<\/el-descriptions>[\s\S]{0,240}<el-card\s+class="detail-card"\s+shadow="never"[\s\S]{0,120}<template\s+#header>备注<\/template>[\s\S]{0,220}displayText\(detail\.remark\)[\s\S]{0,240}<template\s+#header>受托生产企业<\/template>/,
  'green-box remark area must render detail.remark above entrusted enterprises'
)
for (const token of [
  '型号规格',
  '结构组成',
  '适用范围',
  '技术要求',
  '住所',
  '生产地址'
]) {
  assert.match(
    detail,
    new RegExp(`<el-descriptions-item\\s+label="${token}"`),
    `detail page must show the yellow-box field ${token}`
  )
}
assert.doesNotMatch(
  detail,
  /new\s+Date|Date\.now|localStorage|sessionStorage/,
  'detail page must not compute or persist state'
)

const history = read(historyPath)
for (const token of [
  'getRegistrationCertificateHistory',
  'beforeValueJson',
  'afterValueJson',
  'data-testid="registration-certificate-history-page"'
]) {
  assert.match(
    history,
    new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `history page must contain ${token}`
  )
}
assert.doesNotMatch(
  history,
  /mock|placeholder|defaultSuccess|localStorage|sessionStorage/,
  'history page must not mock or persist evidence'
)
