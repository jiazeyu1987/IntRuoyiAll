import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (path) => readFileSync(join(root, path), 'utf8')

const paths = {
  detail: 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue',
  actionPanel: 'IntRuoyiFronted/src/views/dcc/registration-certificate/workflow/ActionPanel.vue',
  commonPreview: 'IntRuoyiFronted/src/api/common/filePreview.ts',
  api: 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts'
}

for (const path of Object.values(paths)) {
  assert.equal(existsSync(join(root, path)), true, `${path} must exist`)
}

const detail = read(paths.detail)
const actionPanel = read(paths.actionPanel)
const commonPreview = read(paths.commonPreview)
const api = read(paths.api)

assert.match(
  commonPreview,
  /type:\s*'DCC_REGISTRATION_CERTIFICATE'[\s\S]*businessFileId:\s*number\s*\|\s*string/,
  'unified online preview must expose a registration-certificate business-file source'
)
assert.match(
  commonPreview,
  /buildDccRegistrationCertificatePreviewSource/,
  'unified online preview must build a registration-certificate source'
)
assert.match(
  commonPreview,
  /registration-certificates\/files\/\$\{source\.businessFileId\}\/preview-metadata/,
  'registration-certificate preview metadata must use the business-file endpoint'
)
assert.match(
  commonPreview,
  /registration-certificates\/files\/\$\{source\.businessFileId\}\/preview/,
  'registration-certificate preview binary must use the business-file endpoint'
)
assert.match(
  commonPreview,
  /DCC_REQUEST_ID_HEADER[\s\S]*DCC-REG-CERT-PREVIEW-META-/,
  'registration-certificate preview metadata must carry a formal audit request id'
)
assert.match(
  commonPreview,
  /DCC_REQUEST_ID_HEADER[\s\S]*DCC-REG-CERT-PREVIEW-BINARY-/,
  'registration-certificate preview binary must carry a formal audit request id'
)

for (const anchor of [
  'registration-certificate-detail-attachment-preview',
  'registration-certificate-detail-attachment-download',
  'registration-certificate-detail-attachment-request-download',
  'registration-certificate-renewal-attachment-preview',
  'registration-certificate-renewal-attachment-download'
]) {
  assert.match(detail, new RegExp(`data-testid=["']${anchor}["']`), `detail must expose ${anchor}`)
}
for (const label of ['在线查看', '下载', '申请下载']) {
  assert.match(detail, new RegExp(`>\\s*${label}\\s*<`), `detail must expose ${label}`)
}
assert.match(detail, /ProtectedPdfViewer/, 'detail must reuse the protected controlled-file viewer')
assert.match(
  detail,
  /buildDccRegistrationCertificatePreviewSource\(/,
  'detail must preview by formal registration-certificate business file id'
)
assert.match(
  detail,
  /downloadRegistrationCertificateFile\([\s\S]*downloadByData/,
  'detail download must use the authorized registration-certificate download API and server filename'
)
assert.match(
  detail,
  /const canDirectDownload = computed\(\(\) => checkRole\(\['dcc_registration_certificate_approver'\]\)\)/,
  'detail must derive direct-download visibility from the formal registration manager role'
)
assert.equal(
  (detail.match(/v-if="canDirectDownload"/g) || []).length,
  2,
  'registration and renewal attachment areas must each expose one direct-download branch'
)
assert.equal(
  (detail.match(/v-else\n\s+v-hasPermi="\['dcc:registration-certificate:access-request:create'\]"/g) || []).length,
  2,
  'registration and renewal attachment areas must each expose one request-download branch'
)
assert.match(
  detail,
  /mode:\s*'access-request'[\s\S]*downloadFileId:/,
  'detail must keep a formal download-request entry for files without a grant'
)
assert.match(
  detail,
  /注册证访问授权范围不合法[\s\S]*当前附件尚未获得下载授权，请先申请下载/,
  'missing download grant must render a business-readable Chinese message'
)
assert.match(
  detail,
  /<Dialog[\s\S]*<ProtectedPdfViewer[\s\S]*:preview-source="selectedPreviewSource"/,
  'online view must open the controlled preview in a dialog'
)
assert.match(
  detail,
  /<el-descriptions\s+:column="detailDescriptionColumns"/,
  'detail descriptions must switch to a responsive column count'
)
assert.match(
  detail,
  /detailDescriptionColumns[\s\S]*viewportWidth\.value\s*<=\s*720\s*\?\s*1\s*:\s*2/,
  'narrow detail view must use one description column'
)
assert.match(
  detail,
  /<Dialog[\s\S]{0,260}width="min\(1120px, 94vw\)"/,
  'online preview dialog must stay inside narrow desktop and mobile viewports'
)
assert.doesNotMatch(detail, /window\.open\(|infraFileId|\/infra\/file\//,
  'detail must not bypass the business-file preview and download guards')

assert.match(api, /downloadRegistrationCertificateFile/, 'authorized download API must remain available')
assert.match(
  actionPanel,
  /initial-access-request-type|initialAccessRequestType/,
  'access panel must support opening directly in download-request mode'
)
assert.match(
  actionPanel,
  /initial-download-business-file-id|initialDownloadBusinessFileId/,
  'access panel must preselect the requested business file'
)
assert.match(
  actionPanel,
  /options\.find\([\s\S]*String\(option\.businessFileId\)\s*===\s*current[\s\S]*selectedDownloadBusinessFileId\.value\s*=\s*selected\.businessFileId/,
  'download request preselection must normalize route string ids to the formal option id type'
)

console.log('registration certificate attachment preview/download static contract passed')
