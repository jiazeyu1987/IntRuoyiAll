import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (path) => readFileSync(join(root, path), 'utf8')

const paths = {
  detail: 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue',
  api: 'IntRuoyiFronted/src/api/dcc/registrationCertificate/index.ts',
  fileController:
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/file/DccRegistrationCertificateFilePreviewController.java',
  grantStatusVo:
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/file/vo/DccRegistrationCertificateFileDownloadGrantStatusRespVO.java',
  accessPolicy:
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/accesspolicy/DccRegistrationCertificateAccessPolicyService.java'
}

for (const path of Object.values(paths)) {
  assert.equal(existsSync(join(root, path)), true, `${path} must exist`)
}

const detail = read(paths.detail)
const api = read(paths.api)
const fileController = read(paths.fileController)
const grantStatusVo = read(paths.grantStatusVo)
const accessPolicy = read(paths.accessPolicy)

assert.match(
  detail,
  /submitRegistrationCertificateAccessRequest/,
  'detail page must submit a formal access request directly from the attachment button'
)
assert.match(
  detail,
  /const message = useMessage\(\)/,
  'detail page must use the app toast mechanism for request feedback'
)
assert.match(
  detail,
  /message\.success\('已申请下载'\)/,
  'successful request must show the exact toast 已申请下载'
)
assert.match(
  detail,
  /const requestPendingFileIds = ref<Set<string>>\(new Set\(\)\)/,
  'detail page must track file ids whose current user download request is pending'
)
assert.match(
  detail,
  /const requestingDownloadFileId = ref\(''\)/,
  'detail page must track the attachment currently submitting a download request'
)
assert.match(
  detail,
  /const isDownloadRequestPending = \(businessFileId: number \| string\) =>[\s\S]*requestPendingFileIds\.value\.has\(String\(businessFileId\)\)/,
  'detail page must expose a pending-state resolver by business file id'
)
assert.match(
  detail,
  /isDownloadRequestPending\(detail\.registrationFileId\)[\s\S]*申请中/,
  'main registration file request button must become 申请中 when pending'
)
assert.match(
  detail,
  /isDownloadRequestPending\(item\.businessFileId\)[\s\S]*申请中/,
  'history attachment request buttons must become 申请中 when pending'
)
assert.match(
  detail,
  /submitRegistrationCertificateAccessRequest\(\s*\{[\s\S]*requestType:\s*'DOWNLOAD_FILE'[\s\S]*businessFileIds:\s*\[businessFileId\]/,
  'download request click must submit DOWNLOAD_FILE for the clicked formal business file id'
)
assert.doesNotMatch(
  detail,
  /if \(!detail\.value\.projectCodeId\)[\s\S]{0,180}缺少项目代码/,
  'download request must not be blocked when the registration certificate has no project code'
)
assert.match(
  detail,
  /requestPendingFileIds\.value = new Set\(\[\.\.\.requestPendingFileIds\.value,\s*String\(businessFileId\)\]\)/,
  'successful request must immediately mark the clicked file as pending'
)
assert.doesNotMatch(
  detail,
  /const openDownloadRequest = async[\s\S]{0,800}mode:\s*'access-request'/,
  'attachment request click must not switch the normal detail page into access-request mode'
)
assert.doesNotMatch(
  detail,
  /const openDownloadRequest = async[\s\S]{0,800}scrollIntoView/,
  'attachment request click must not scroll to the workflow action panel'
)

assert.match(
  api,
  /pendingRequestId\?: number \| string/,
  'download grant status API type must expose pendingRequestId'
)
assert.match(
  grantStatusVo,
  /private Long pendingRequestId;/,
  'backend grant status response must carry a pending request id'
)
assert.match(
  fileController,
  /findPendingDownloadRequestId\(/,
  'download grant status endpoint must populate current-user pending download request ids'
)
assert.match(
  accessPolicy,
  /public Long findPendingDownloadRequestId[\s\S]*r\.requester_user_id = \?[\s\S]*r\.request_type = 'DOWNLOAD_FILE'[\s\S]*r\.status IN \('SUBMITTED', 'BPM_BOUND'\)[\s\S]*rf\.business_file_id = \?/,
  'pending lookup must be scoped to the current tenant, current user, DOWNLOAD_FILE requests, active pending statuses, and target business file'
)

console.log('registration certificate inline download-request UX static contract passed')
