import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts'
const listPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/index/index.vue'
const detailPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue'
const renewalDialogPath =
  'IntRuoyiFronted/src/views/dcc/registration-certificate/renewal/RenewalDialog.vue'
const statePath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/shared/state.ts'
const controllerPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/query/DccRegistrationCertificateQueryController.java'
const mapperPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/dal/mysql/DccRegistrationCertificateQueryMapper.java'

for (const file of [
  apiPath,
  listPath,
  detailPath,
  renewalDialogPath,
  statePath,
  controllerPath,
  mapperPath
]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(
  api,
  /getRegistrationCertificateDetail\s*=\s*async\s*\(\s*id:\s*number\s*\|\s*string,\s*versionId\?:\s*number\s*\|\s*string\s*\)/,
  'detail API must accept an optional versionId for old-certificate detail reads'
)
assert.match(
  api,
  /params:\s*versionId\s*\?\s*\{\s*versionId\s*\}\s*:\s*undefined/,
  'detail API must send versionId only when the old index supplied a concrete version'
)
assert.match(
  api,
  /export\s+type\s+DccRegistrationCertificateLocalDateValue\s*=\s*string\s*\|\s*\[number,\s*number,\s*number\]/,
  'registration certificate API must declare the Java LocalDate array response contract'
)

const list = read(listPath)
assert.match(
  list,
  /@click="openOldDetail\(row\.certificateId,\s*row\.versionId\)"/,
  'old index detail button must pass the row versionId'
)
assert.match(
  list,
  /const\s+openOldDetail\s*=\s*\(\s*certificateId:\s*number\s*\|\s*string,\s*versionId:\s*number\s*\|\s*string\s*\)/,
  'old index route helper must require a versionId'
)
assert.match(
  list,
  /query:\s*\{[\s\S]{0,120}mode:\s*'old-detail'[\s\S]{0,120}versionId:\s*String\(versionId\)[\s\S]{0,80}\}/,
  'old index route helper must carry mode=old-detail and versionId in the route query'
)
assert.match(
  list,
  /'已被替换，原有效期至 '\s*\+\s*formatRegistrationCertificateDate\(row\.expiryDate\)/,
  'old index must distinguish replacement from the original certificate expiry date'
)
assert.match(
  list,
  /formatRegistrationCertificateDate\(row\.approvalDate\)/,
  'current index must format approval LocalDate arrays before rendering'
)
assert.match(
  list,
  /formatRegistrationCertificateDate\(row\.effectiveDate\)/,
  'current index must format effective LocalDate arrays before rendering'
)

const detail = read(detailPath)
assert.match(
  detail,
  /const\s+detailVersionId\s*=\s*computed\(\(\)\s*=>\s*parsePositiveRouteQueryId\(route\.query\.versionId\)\)/,
  'detail page must parse versionId from route query with the formal long-id parser'
)
assert.match(
  detail,
  /const\s+versionId\s*=\s*detailVersionId\.value[\s\S]*const\s+resolvedCertificateId\s*=\s*await\s+resolveCertificateIdForDetail\(\)[\s\S]*getRegistrationCertificateDetail\(resolvedCertificateId,\s*versionId\)/,
  'detail page must pass the optional versionId into the detail API after resolving the formal certificate id'
)
assert.match(
  detail,
  /v-if="detail\.status === 'OLD'" title="已被替换的旧版本，已停止使用；原有效期至仅为证载日期，以下证载状态为历史记录"/,
  'old detail must distinguish replacement from the original certificate expiry date'
)
assert.match(
  detail,
  /:label="detail\.status === 'OLD' \? '原有效期至' : '有效期至'">\{\{\s*formatRegistrationCertificateDate\(detail\.expiryDate\)\s*\}\}/,
  'detail date fields must not render Java LocalDate arrays with comma-separated displayText'
)

const renewalDialog = read(renewalDialogPath)
assert.match(
  renewalDialog,
  /formatRegistrationCertificateDate\(certificate\.effectiveDate\)/,
  'renewal dialog must format current effective LocalDate arrays'
)
assert.match(
  renewalDialog,
  /formatRegistrationCertificateDate\(certificate\.expiryDate\)/,
  'renewal dialog must format current expiry LocalDate arrays'
)

const state = read(statePath)
assert.match(
  state,
  /export\s+const\s+formatRegistrationCertificateDate\s*=/,
  'shared state must export a dedicated registration certificate date formatter'
)
const dateFormatterStart = state.indexOf('export const formatRegistrationCertificateDate')
const dateFormatterEnd = state.indexOf('\n}', dateFormatterStart)
assert.ok(dateFormatterStart >= 0 && dateFormatterEnd > dateFormatterStart,
  'registration certificate date formatter block must exist')
const dateFormatter = state.slice(dateFormatterStart, dateFormatterEnd)
assert.match(
  dateFormatter,
  /Array\.isArray\(value\)[\s\S]*padStart\(2,\s*'0'\)/,
  'date formatter must normalize Java LocalDate arrays to zero-padded YYYY-MM-DD text'
)

const controller = read(controllerPath)
assert.match(
  controller,
  /@RequestParam\(value\s*=\s*"versionId",\s*required\s*=\s*false\)\s*@Positive\s*Long\s+versionId/,
  'detail controller must accept an optional positive versionId query parameter'
)
assert.match(
  controller,
  /queryService\.getDetail\([\s\S]{0,160}id,\s*versionId,\s*DccRequestAuditContext\.from/,
  'detail controller must pass the requested versionId into the query service'
)

const mapper = read(mapperPath)
assert.match(
  mapper,
  /@Param\("versionId"\)\s*Long\s+versionId/,
  'query mapper must expose versionId as a formal SQL parameter'
)
assert.match(
  mapper.replaceAll('\\"', '"'),
  /<when test="versionId != null">[\s\S]{0,80}AND v\.id = #\{versionId\}/,
  'detail mapper must constrain the selected row by versionId when provided'
)

console.log('registration certificate old detail version static contract passed')
