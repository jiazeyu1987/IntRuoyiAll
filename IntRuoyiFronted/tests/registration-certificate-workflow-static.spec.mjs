import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'src/api/dcc/registrationCertificate/index.ts'
const workflowPath = 'src/views/dcc/registration-certificate/workflow/ActionPanel.vue'
const detailPath = 'src/views/dcc/registration-certificate/detail/index.vue'
const listPath = 'src/views/dcc/registration-certificate/index/index.vue'
const menuSqlPath = '../IntRuoyiBackend/sql/mysql/20260816_dcc_registration_certificate_menu.sql'
const backendControllerTest =
  '../IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/registrationcertificate/DccRegistrationCertificateCommandControllerTest.java'

for (const file of [apiPath, workflowPath, detailPath, listPath, menuSqlPath, backendControllerTest]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
for (const endpoint of [
  '/dcc/registration-certificates/drafts',
  '/dcc/registration-certificates/${id}/formalize',
  '/dcc/registration-certificates/${certificateId}/renewals',
  '/dcc/registration-certificates/${certificateId}/renewals/${pendingVersionId}/void',
  '/dcc/registration-certificates/${certificateId}/changes',
  '/dcc/registration-certificates/${certificateId}/changes/void',
  '/dcc/registration-certificates/${certificateId}/supporting-documents',
  '/dcc/registration-certificates/${certificateId}/supporting-documents/${supportingDocumentId}/confirm',
  '/dcc/registration-certificates/${certificateId}/supporting-documents/${supportingDocumentId}/reject',
  '/dcc/registration-certificates/access-requests',
  '/dcc/registration-certificates/files/${businessFileId}/preview-metadata'
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
  'confirmRegistrationCertificateSupportingDocument',
  'rejectRegistrationCertificateSupportingDocument',
  'submitRegistrationCertificateAccessRequest',
  'getRegistrationCertificateFilePreviewMetadata'
]) {
  assert.match(api, new RegExp(`export\\s+const\\s+${exported}\\b`), `${exported} must be exported`)
}
assert.match(api, /headers:\s*\{[\s\S]{0,80}['"]Idempotency-Key['"]/, 'write APIs must send the explicit Idempotency-Key header')
assert.doesNotMatch(api, /Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|placeholder|defaultSuccess|rawUrl|fileUrl/, 'API must not generate unstable keys, persist fake state, mock success or expose raw URLs')

const workflow = read(workflowPath)
for (const token of [
  'data-testid="registration-certificate-workflow-actions"',
  'data-testid="registration-certificate-draft-action"',
  'data-testid="registration-certificate-formalize-action"',
  'data-testid="registration-certificate-renewal-action"',
  'data-testid="registration-certificate-change-action"',
  'data-testid="registration-certificate-supporting-document-action"',
  'data-testid="registration-certificate-access-request-action"',
  'data-testid="registration-certificate-approval-result-action"',
  'idempotencyKey',
  'requireIdempotencyKey',
  'lastActionError',
  'createRegistrationCertificateDraft',
  'formalizeRegistrationCertificate',
  'uploadRegistrationCertificateRenewalCandidate',
  'submitRegistrationCertificateChange',
  'uploadRegistrationCertificateSupportingDocument',
  'submitRegistrationCertificateAccessRequest'
]) {
  assert.match(workflow, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `workflow component must contain ${token}`)
}
assert.match(workflow, /catch\s*\(\s*error\s*\)[\s\S]{0,220}lastActionError\.value/, 'workflow failures must remain visibly failed')
assert.doesNotMatch(workflow, /catch\s*\(\s*\)\s*\{\s*\}|Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|placeholder|defaultSuccess|rawUrl|fileUrl|window\.open/, 'workflow must not swallow errors, invent keys, persist fake state or expose raw URLs')

const detail = read(detailPath)
assert.match(detail, /RegistrationCertificateActionPanel/, 'detail page must mount the workflow action panel')
assert.match(detail, /data-testid="registration-certificate-detail-page"/, 'detail page anchor remains stable')

const list = read(listPath)
assert.match(list, /openCreateDraft|注册证新增|新增注册证/, 'list page must expose a maintenance entry')
assert.doesNotMatch(list, /Date\.now|Math\.random|randomUUID|localStorage|sessionStorage|mock|defaultSuccess/, 'list page must not fake workflow success')

const menuSql = read(menuSqlPath)
for (const permission of [
  'dcc:registration-certificate:renewal:upload',
  'dcc:registration-certificate:renewal:void',
  'dcc:registration-certificate:change:submit',
  'dcc:registration-certificate:void',
  'dcc:registration-certificate:supporting-document:upload',
  'dcc:registration-certificate:supporting-document:confirm'
]) {
  assert.match(menuSql, new RegExp(permission.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `menu SQL must grant ${permission}`)
}

const backendTest = read(backendControllerTest)
for (const controller of [
  'DccRegistrationCertificateRenewalController',
  'DccRegistrationCertificateChangeController',
  'DccRegistrationCertificateSupportingDocumentController'
]) {
  assert.match(backendTest, new RegExp(controller), `${controller} must be covered by backend contract tests`)
}
