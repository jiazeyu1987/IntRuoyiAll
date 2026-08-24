import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'src/api/dcc/registrationCertificate/index.ts'
const axiosIndexPath = 'src/config/axios/index.ts'
const axiosServicePath = 'src/config/axios/service.ts'
const workflowPath = 'src/views/dcc/registration-certificate/workflow/ActionPanel.vue'
const detailPath = 'src/views/dcc/registration-certificate/detail/index.vue'
const listPath = 'src/views/dcc/registration-certificate/index/index.vue'
const realFlowPath = 'tests/e2e/registration-certificate-real-flow.spec.js'
const menuSqlPath = '../IntRuoyiBackend/sql/mysql/20260816_dcc_registration_certificate_menu.sql'
const directEffectiveMigrationPath = '../IntRuoyiBackend/sql/mysql/20260824_dcc_registration_certificate_supporting_document_direct_effective.sql'
const backendControllerTest =
  '../IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/registrationcertificate/DccRegistrationCertificateCommandControllerTest.java'

for (const file of [apiPath, axiosIndexPath, axiosServicePath, workflowPath, detailPath, listPath, realFlowPath, menuSqlPath, directEffectiveMigrationPath, backendControllerTest]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
assert.match(api, /registrationFileId\??:\s*number\s*\|\s*string/, 'detail API must expose the formal registration business-file id')
assert.match(api, /DccRegistrationCertificateChangeApplyReqVO[\s\S]{0,260}businessFileId\??:\s*number\s*\|\s*string/, 'change API must carry the formal change-approval business-file id')
assert.match(api, /resolvedAttemptKey\s*=\s*attemptKey\?\.trim\(\)\s*\|\|\s*`DCC-REG-CERT-DOWNLOAD-\$\{generateUUID\(\)\}`/, 'download API must generate an attempt key when the caller omits it')
assert.doesNotMatch(api, /请填写稳定下载尝试键/, 'download API must not ask a user to type an internal attempt key')
for (const endpoint of [
  '/dcc/registration-certificates/drafts',
  '/dcc/registration-certificates/${id}/formalize',
  '/dcc/registration-certificates/${certificateId}/renewals',
  '/dcc/registration-certificates/${certificateId}/renewals/${pendingVersionId}/void',
  '/dcc/registration-certificates/${certificateId}/changes',
  '/dcc/registration-certificates/${certificateId}/changes/void',
  '/dcc/registration-certificates/${certificateId}/supporting-documents',
  '/dcc/registration-certificates/access-requests',
  '/dcc/registration-certificates/access-requests/${requestId}',
  '/dcc/registration-certificates/access-requests/${requestId}/withdraw',
  '/dcc/registration-certificates/grants/${grantId}/revoke',
  '/dcc/registration-certificates/files/${businessFileId}/preview-metadata',
  '/dcc/registration-certificates/files/${businessFileId}/download'
]) {
  assert.match(api, new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API endpoint ${endpoint} is frozen`)
}
for (const exported of [
  'createRegistrationCertificateDraft',
  'updateRegistrationCertificateDraft',
  'deleteRegistrationCertificateDraft',
  'formalizeRegistrationCertificate',
  'uploadRegistrationCertificateRenewalCandidate',
  'voidRegistrationCertificateRenewalCandidate',
  'submitRegistrationCertificateChange',
  'voidRegistrationCertificate',
  'uploadRegistrationCertificateSupportingDocument',
  'submitRegistrationCertificateAccessRequest',
  'getRegistrationCertificateAccessRequestStatus',
  'withdrawRegistrationCertificateAccessRequest',
  'revokeRegistrationCertificateGrant',
  'downloadRegistrationCertificateFile',
  'getRegistrationCertificateFilePreviewMetadata'
]) {
  assert.match(api, new RegExp(`export\\s+const\\s+${exported}\\b`), `${exported} must be exported`)
}
assert.match(api, /headers:\s*\{[\s\S]{0,80}['"]Idempotency-Key['"]/, 'write APIs must send the explicit Idempotency-Key header')
assert.match(api, /downloadOriginal[\s\S]{0,320}response\.headers\?\.\['content-disposition'\]/, 'registration certificate download must read the server Content-Disposition header from the original response')
assert.doesNotMatch(api, /Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|placeholder|defaultSuccess|rawUrl|fileUrl/, 'API must not generate unstable keys, persist fake state, mock success or expose raw URLs')

const axiosIndex = read(axiosIndexPath)
const axiosService = read(axiosServicePath)
assert.match(
  axiosIndex,
  /downloadOriginal:[\s\S]{0,360}returnOriginalResponse:\s*true/,
  'downloadOriginal must opt in to preserving response headers for audited file downloads'
)
assert.match(
  axiosService,
  /returnOriginalResponse\?:\s*boolean/,
  'axios custom config must type the explicit original-response contract'
)
assert.match(
  axiosService,
  /responseType === 'blob'[\s\S]{0,420}returnOriginalResponse[\s\S]{0,120}return response/,
  'blob response interceptor must preserve headers when downloadOriginal explicitly requests the original response'
)

const workflow = read(workflowPath)
for (const token of [
  'data-testid="registration-certificate-workflow-actions"',
  'data-testid="registration-certificate-draft-action"',
  'data-testid="registration-certificate-draft-form"',
  'data-testid="registration-certificate-formalize-action"',
  'data-testid="registration-certificate-renewal-action"',
  'data-testid="registration-certificate-change-action"',
  'data-testid="registration-certificate-supporting-document-action"',
  'data-testid="registration-certificate-supporting-document-form"',
  'data-testid="registration-certificate-access-request-action"',
  'data-testid="registration-certificate-approval-result-action"',
  'operationKeys',
  'IdempotencyOperation',
  'getOrCreateIdempotencyKey',
  'generateUUID',
  'lastActionError',
  'createRegistrationCertificateDraft',
  'formalizeRegistrationCertificate',
  'uploadRegistrationCertificateRenewalCandidate',
  'submitRegistrationCertificateChange',
  'uploadRegistrationCertificateSupportingDocument',
  'changeTypeOptions',
  'renewalForm',
  'supportingForm',
  'submitRegistrationCertificateAccessRequest',
  'getRegistrationCertificateAccessRequestStatus',
  'withdrawRegistrationCertificateAccessRequest',
  'revokeRegistrationCertificateGrant',
  'downloadRegistrationCertificateFile'
]) {
  assert.match(workflow, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `workflow component must contain ${token}`)
}
assert.doesNotMatch(
  workflow,
  /<el-input[^>]+data-field="idempotencyKey"/,
  'idempotency keys must be generated by the workflow instead of entered by the user'
)
assert.match(
  workflow,
  /const getOrCreateIdempotencyKey\s*=\s*\(/,
  'workflow must lazily create one key per pending operation'
)
assert.match(
  workflow,
  /const resetIdempotencyKey\s*=\s*\(/,
  'workflow must reset a key only after a successful operation'
)
for (const token of [
  'accessRequestType',
  'VIEW_OLD_CERTIFICATE',
  'DOWNLOAD_FILE',
  'purpose: \'页面提交的注册证文件下载申请\''
]) {
  assert.match(workflow, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `workflow download request must contain ${token}`)
}
assert.doesNotMatch(workflow, /data-field="accessProjectCodeId"/, 'project code IDs must not be typed into the workflow')
assert.doesNotMatch(workflow, /data-field="accessRequestId"/, 'request IDs must be carried from the server response')
assert.match(workflow, /props\.projectCodeId/, 'workflow must resolve the project code from the current detail')
assert.doesNotMatch(workflow, /projectCodeId:\s*require|businessFileIds:\s*\[/, 'access requests must not ask the user or page to assemble internal IDs')
assert.match(workflow, /activeSupportingDocumentId/, 'supporting document IDs must be carried from upload results')
assert.match(workflow, /上传后直接生效/, 'supporting document upload must disclose immediate effectiveness')
assert.doesNotMatch(workflow, /handleConfirmSupportingDocument|handleRejectSupportingDocument|confirmRegistrationCertificateSupportingDocument|rejectRegistrationCertificateSupportingDocument/, 'supporting document flow must not expose confirmation or rejection actions')
assert.match(workflow, /pendingVersionId/, 'renewal candidate IDs must be carried from upload results')
assert.match(workflow, /requirePendingVersionId/, 'renewal void must use the generated candidate ID only')
assert.doesNotMatch(workflow, /pendingVersionId\.value\s*\|\|\s*requireVersionId/, 'renewal void must not guess the current version as a candidate')
assert.match(workflow, /catch\s*\(\s*error\s*\)[\s\S]{0,220}lastActionError\.value/, 'workflow failures must remain visibly failed')
assert.doesNotMatch(workflow, /catch\s*\(\s*\)\s*\{\s*\}|Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|placeholder|defaultSuccess|rawUrl|fileUrl|window\.open/, 'workflow must not swallow errors, invent keys, persist fake state or expose raw URLs')

const detail = read(detailPath)
assert.match(detail, /RegistrationCertificateActionPanel/, 'detail page must mount the workflow action panel')
assert.match(detail, /data-testid="registration-certificate-detail-page"/, 'detail page anchor remains stable')
assert.match(
  detail,
  /:business-file-id="detail\.registrationFileId"/,
  'detail page must pass the server-resolved formal business-file id into the workflow'
)
assert.match(detail, /:row-version="detail\.rowVersion"/, 'detail page must pass the server row version')
assert.match(detail, /:snapshot-revision="detail\.snapshotRevision"/, 'detail page must pass the server snapshot revision')
assert.match(detail, /:project-code-id="detail\.projectCodeId"/, 'detail page must pass the server project code')

const realFlow = read(realFlowPath)
for (const legacyInput of ['访问申请幂等键', '下载项目代码 ID', '访问申请 ID', '下载尝试键']) {
  assert.doesNotMatch(realFlow, new RegExp(legacyInput), `real E2E must not ask users to type ${legacyInput}`)
}
assert.match(realFlow, /DCC-REG-CERT-ACCESS_SUBMIT-/, 'real E2E must verify the generated access idempotency key')
assert.match(realFlow, /DCC-REG-CERT-DOWNLOAD_GRANT-/, 'real E2E must verify the generated download idempotency key')

const list = read(listPath)
assert.match(list, /openCreateDraft|注册证新增|新增注册证/, 'list page must expose a maintenance entry')
assert.doesNotMatch(list, /Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|defaultSuccess/, 'list page must not fake workflow success')

const menuSql = read(menuSqlPath)
for (const permission of [
  'dcc:registration-certificate:renewal:upload',
  'dcc:registration-certificate:renewal:void',
  'dcc:registration-certificate:change:submit',
  'dcc:registration-certificate:void',
  'dcc:registration-certificate:supporting-document:upload'
]) {
  assert.match(menuSql, new RegExp(permission.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `menu SQL must grant ${permission}`)
}
const directEffectiveMigration = read(directEffectiveMigrationPath)
assert.match(
  directEffectiveMigration,
  /DELETE\s+FROM\s+`?system_menu`?[\s\S]{0,240}dcc:registration-certificate:supporting-document:confirm/,
  'direct-effective migration must remove the obsolete confirmation permission without rewriting historical menu SQL'
)
assert.match(
  directEffectiveMigration,
  /SUPPORTING_DOCUMENT_EFFECTIVE/,
  'direct-effective migration must register the effective lifecycle event'
)

const backendTest = read(backendControllerTest)
for (const controller of [
  'DccRegistrationCertificateRenewalController',
  'DccRegistrationCertificateChangeController',
  'DccRegistrationCertificateSupportingDocumentController'
]) {
  assert.match(backendTest, new RegExp(controller), `${controller} must be covered by backend contract tests`)
}
