import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts'
const detailPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue'

for (const file of [apiPath, detailPath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
const detail = read(detailPath)
const historyInterfaceStart = api.indexOf('export interface DccRegistrationCertificateHistoryItemVO')
const historyInterfaceEnd = api.indexOf('\n}', historyInterfaceStart)
assert.ok(historyInterfaceStart >= 0 && historyInterfaceEnd > historyInterfaceStart,
  'history API interface block must exist')
const historyInterface = api.slice(historyInterfaceStart, historyInterfaceEnd)

for (const field of [
  'targetVersionId',
  'versionNo',
  'approvalDate',
  'effectiveDate',
  'expiryDate',
  'categoryChanged',
  'certificateNo',
  'classification',
  'originalFileName',
  'fileStatus',
  'occurredAt'
]) {
  assert.match(
    historyInterface,
    new RegExp(`\\b${field}\\??:`),
    `history API type must expose ${field}`
  )
}
assert.match(
  historyInterface,
  /occurredAt\?:\s*string\s*\|\s*number/,
  'history event time must match the runtime string-or-epoch response contract'
)

assert.match(
  detail,
  /data-testid="registration-certificate-renewal-history"/,
  'detail page must expose a stable renewal-history section'
)
assert.match(
  detail,
  /const\s+renewalHistory\s*=\s*computed\([\s\S]*eventType\s*===\s*'RENEWAL_UPLOADED'/,
  'detail page must derive renewal records from formal renewal lifecycle events'
)
for (const label of [
  '延续记录',
  '批准日期',
  '生效日期',
  '有效期至',
  '类别是否变更',
  '变更后注册证号',
  '变更后类别',
  '延续注册证文件'
]) {
  assert.match(detail, new RegExp(label), `renewal history must display ${label}`)
}
assert.match(
  detail,
  /v-if="item\.categoryChanged"[\s\S]{0,700}变更后注册证号[\s\S]{0,700}变更后类别/,
  'changed certificate number and classification must render only for category-changed renewals'
)
assert.match(
  detail,
  /formatRegistrationCertificateDate\(item\.approvalDate\)/,
  'renewal approval date must use the shared LocalDate formatter'
)
assert.match(
  detail,
  /formatRegistrationCertificateDate\(item\.effectiveDate\)/,
  'renewal effective date must use the shared LocalDate formatter'
)
assert.match(
  detail,
  /formatRegistrationCertificateDate\(item\.expiryDate\)/,
  'renewal expiry date must use the shared LocalDate formatter'
)
assert.match(
  detail,
  /item\.originalFileName/,
  'renewal history must show the original uploaded filename'
)
assert.doesNotMatch(
  detail,
  /downloadRegistrationCertificateFile/,
  'history display must not bypass the existing file-download authorization flow'
)
assert.match(
  detail,
  /item\.fileKind\s*!==\s*'CHANGE_APPROVAL'/,
  'renewal history files must not silently expand the existing download-request candidates'
)

console.log('registration certificate renewal history detail static contract passed')
