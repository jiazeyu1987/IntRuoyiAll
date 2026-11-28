import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (path) => readFileSync(join(root, path), 'utf8')

const paths = {
  detail: 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue',
  actionPanel: 'IntRuoyiFronted/src/views/dcc/registration-certificate/workflow/ActionPanel.vue',
  commonPreview: 'IntRuoyiFronted/src/api/common/filePreview.ts',
  api: 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts',
  fileController: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/file/DccRegistrationCertificateFilePreviewController.java',
  fileDelivery: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/file/DccRegistrationCertificateFileDeliveryServiceImpl.java',
  accessPolicy: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/accesspolicy/DccRegistrationCertificateAccessPolicyService.java'
}

for (const path of Object.values(paths)) {
  assert.equal(existsSync(join(root, path)), true, `${path} must exist`)
}

const detail = read(paths.detail)
const actionPanel = read(paths.actionPanel)
const commonPreview = read(paths.commonPreview)
const api = read(paths.api)
const fileController = read(paths.fileController)
const accessPolicy = read(paths.accessPolicy)
const fileDelivery = read(paths.fileDelivery)

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
for (const label of ['在线查看', '下载']) {
  assert.match(detail, new RegExp(`>\\s*${label}\\s*<`), `detail must expose ${label}`)
}
assert.match(detail, /申请中[\s\S]*申请下载/, 'detail must expose request-download and pending labels')
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
  /const result = await downloadRegistrationCertificateFile\(businessFileId\)[\s\S]*const savedFileName = result\.fileName[\s\S]*downloadByData\(result\.blob, savedFileName, result\.blob\.type \|\| 'application\/octet-stream'\)/,
  'detail must save the authorized download using the server filename, including server-owned expired markers'
)
assert.match(
  api,
  /const disposition = response\.headers\?\.\['content-disposition'\][\s\S]*if \(!disposition\)\s*\{\s*throw new Error\('下载响应缺少服务端文件名，已拒绝保存文件。'\)/,
  'download must reject a response without a server filename header'
)
assert.match(
  api,
  /const fileName = encodedName \? decodeURIComponent\(encodedName\) : plainName[\s\S]*if \(!fileName \|\| !fileName\.trim\(\)\)\s*\{\s*throw new Error\('下载响应文件名无效，已拒绝保存文件。'\)[\s\S]*return \{ blob: response\.data, fileName: fileName\.trim\(\) \}/,
  'download must validate the decoded server filename and return it with the response blob'
)
assert.match(
  detail,
  /getRegistrationCertificateFileDownloadGrantStatuses/,
  'detail must load current-user download authorization status from the backend'
)
assert.match(
  detail,
  /const canDirectDownload = \(businessFileId: number \| string\) =>[\s\S]*downloadAuthorizedFileIds\.value\.has\(String\(businessFileId\)\)/,
  'detail must show direct download only for file ids authorized by the backend'
)
assert.equal(
  (detail.match(/v-if="canDirectDownload\(/g) || []).length,
  3,
  'registration, renewal, and change attachment areas must each expose one direct-download branch'
)
assert.equal(
  (detail.match(/v-else\r?\n\s+v-hasPermi="\['dcc:registration-certificate:access-request:create'\]"/g) || []).length,
  3,
  'registration, renewal, and change attachment areas must each expose one request-download branch'
)
assert.doesNotMatch(
  detail,
  /const canDirectDownload = computed\(\(\) => checkRole\(\['dcc_registration_certificate_approver'\]\)\)/,
  'detail must not rely only on a fixed role to decide whether an approved applicant can download'
)
assert.match(
  detail,
  /submitRegistrationCertificateAccessRequest\(\s*\{[\s\S]*requestType:\s*'DOWNLOAD_FILE'[\s\S]*businessFileIds:\s*\[businessFileId\]/,
  'detail must submit a formal download request for files without a grant'
)
assert.doesNotMatch(
  detail,
  /const openDownloadRequest = async[\s\S]{0,800}mode:\s*'access-request'/,
  'detail attachment request must not switch into access-request workflow mode'
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
  api,
  /getRegistrationCertificateFileDownloadGrantStatuses[\s\S]*\/dcc\/registration-certificates\/files\/download-grants[\s\S]*businessFileIds\.map\(\(id\) => String\(id\)\)\.join\(','\)/,
  'frontend must call the current-user download grant status endpoint with explicit business-file ids'
)
assert.match(api, /pendingRequestId\?: number \| string/, 'download grant status type must expose pendingRequestId')
assert.match(
  fileController,
  /@GetMapping\("\/download-grants"\)[\s\S]*listDownloadGrants[\s\S]*accessPolicyService\.canDownloadFile/,
  'backend must expose a read-only current-user download authorization status endpoint'
)
assert.match(
  accessPolicy,
  /public boolean canDownloadFile[\s\S]*isExpired\(tenantId, version, at\) && !isRegistrationDepartment\(tenantId, actorId\)\) return false[\s\S]*requireDownloadGrant\(tenantId, actorId, businessFileId, at\)[\s\S]*consumptionMapper\.countSuccess\(tenantId, grant\.getId\(\), businessFileId\) == 0/,
  'backend authorization status must enforce expired-file department restrictions, a valid grant, and no previous successful consumption'
)
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



assert.match(
  detail,
  /const statuses = await getRegistrationCertificateFileDownloadGrantStatuses\(businessFileIds\)[\s\S]*downloadAuthorizedFileIds\.value = new Set\([\s\S]*\.filter\(\(status\) => status\.canDownload\)[\s\S]*\.map\(\(status\) => String\(status\.businessFileId\)\)/,
  'frontend must populate authorized file ids only from backend canDownload results'
)
assert.match(
  fileDelivery,
  /public DccRegistrationCertificateFileDownloadResult download\([\s\S]*accessPolicyService\.assertDownloadAllowed\(tenantId, userId, businessFileId, now\)[\s\S]*accessPolicyService\.requireDownloadGrant\(tenantId, userId, businessFileId, now\)[\s\S]*buildFileName\(/,
  'actual file delivery must enforce download authorization and a grant before returning file content'
)
console.log('registration certificate attachment preview/download static contract passed')
