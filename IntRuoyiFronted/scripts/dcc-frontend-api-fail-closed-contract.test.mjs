import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')
const uploadSubmitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')
const uploadPageSource = readText('src/views/dcc/controlled-file/upload/index.vue')
const previewPageSource = readText('src/views/dcc/controlled-file/view/index.vue')
const detailPageSource = readText('src/views/dcc/controlled-file/detail/index.vue')
const approvalActionsSource = readText('src/views/dcc/controlled-file/detail/approval-actions.ts')
const externalReviewPageSource = readText('src/views/dcc/controlled-file/external-review/index.vue')
const minePageSource = readText('src/views/dcc/controlled-file/mine/index.vue')
const auditApiSource = readText('src/api/dcc/controlledFile/audits.ts')
const auditPageSource = readText('src/views/dcc/controlled-file/audit/index.vue')
const remainingRouteSource = readText('src/router/modules/remaining.ts')

const extractInterfaceBody = (source, interfaceName) => {
  const match = source.match(new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `${interfaceName} must be exported`)
  return match[1]
}

test('BDD: upload ticket contract -> Given DCC upload succeeds, When frontend API exposes the response, Then only controlled upload ticket fields are available', () => {
  const uploadBody = extractInterfaceBody(workflowSource, 'ControlledFileUploadRespVO')

  assert.match(uploadBody, /uploadTicket:\s*string/)
  assert.match(uploadBody, /sessionId:\s*string/)
  assert.match(uploadBody, /requestId:\s*string/)
  assert.match(uploadBody, /watermarkTraceCode\?:\s*string\s*\|\s*null/)
  assert.doesNotMatch(uploadBody, /\bfileId\b/)
  assert.doesNotMatch(uploadBody, /\bonlyofficeDocumentUrl\b/)
})

test('BDD: submit ticket contract -> Given a DCC submit payload is built, When frontend API types it, Then it cannot depend on stored file identifiers', () => {
  const submitBody = extractInterfaceBody(workflowSource, 'ControlledFileSubmitReqVO')

  assert.match(submitBody, /sessionId:\s*string/)
  assert.match(submitBody, /originalUploadTicket:\s*string/)
  assert.match(submitBody, /sourceUploadTicket\?:\s*string/)
  assert.match(submitBody, /drawingPdfUploadTicket\?:\s*string/)
  assert.doesNotMatch(submitBody, /\buploadTicket\b/)
  assert.doesNotMatch(submitBody, /\boriginalFileId\b/)
  assert.doesNotMatch(submitBody, /\bsourceFileId\b/)
  assert.doesNotMatch(submitBody, /\bdrawingPdfFileId\b/)
})

test('BDD: preview metadata contract -> Given preview metadata is loaded, When required evidence is missing, Then API parsing fails closed before a UI can render', () => {
  const metadataBody = extractInterfaceBody(workflowSource, 'ControlledFilePreviewMetadataVO')

  for (const field of [
    'viewerToken',
    'viewerTokenId',
    'viewerTokenNonce',
    'accessEventCode',
    'watermarkTraceCode'
  ]) {
    assert.match(metadataBody, new RegExp(`${field}:\\s*string`), `${field} is required`)
  }
  assert.match(metadataBody, /watermark:\s*ControlledPreviewWatermark/)
  assert.match(metadataBody, /onlyofficeDocumentUrl\?:\s*string/)
  assert.match(workflowSource, /parseControlledFilePreviewMetadata/)
  assert.match(workflowSource, /assertRequiredString\(payload,\s*'viewerToken'/)
  assert.match(workflowSource, /assertRequiredObject\(payload,\s*'watermark'/)
  assert.match(workflowSource, /onlyofficeDocumentUrl:\s*readOptionalString\(payload,\s*'onlyofficeDocumentUrl'\)/)
})

test('BDD: openable PDF download contract -> Given a DCC download response arrives, When required PDF audit evidence is missing, Then frontend API throws instead of producing a generic blob download', () => {
  for (const header of [
    'X-DCC-Download-Request-Id',
    'X-DCC-Access-Event-Code',
    'X-DCC-Plain-SHA256'
  ]) {
    assert.match(workflowSource, new RegExp(header), `${header} must be validated`)
  }

  assert.doesNotMatch(workflowSource, /X-DCC-Encryption-Policy-Version/)
  assert.doesNotMatch(workflowSource, /X-DCC-Artifact-Id/)
  assert.doesNotMatch(workflowSource, /X-DCC-Cipher-SHA256/)
  assert.match(workflowSource, /DccControlledFileContractError/)
  assert.match(workflowSource, /assertControlledFileDownloadHeaders/)
  assert.doesNotMatch(workflowSource, /resolveControlledFileDownloadFallbackName/)
  assert.doesNotMatch(workflowSource, /fallbackFileName/)
  assert.match(workflowSource, /throw new DccControlledFileContractError\(`DCC download response missing required header/)
})

test('BDD: download request id binding -> Given a controlled download starts, When frontend calls backend T10, Then it sends a generated request id and rejects mismatched evidence', () => {
  assert.match(workflowSource, /const createControlledFileDownloadRequestId = \(\): string =>/)
  assert.match(workflowSource, /crypto\.getRandomValues\(bytes\)/)
  assert.match(
    workflowSource,
    /throw new DccControlledFileContractError\('DCC download request id requires crypto\.getRandomValues\(\)'/
  )
  assert.match(workflowSource, /const downloadRequestId = createControlledFileDownloadRequestId\(\)/)
  assert.match(workflowSource, /params:\s*\{[\s\S]*downloadRequestId/)
  assert.match(workflowSource, /assertControlledFileDownloadHeaders\(response,\s*downloadRequestId\)/)
  assert.match(workflowSource, /if \(evidence\.downloadRequestId !== expectedDownloadRequestId\)/)
  assert.match(workflowSource, /DCC download response request id mismatch/)
})

test('BDD: page submit integration -> Given DCC pages build business requests, When upload tickets are available, Then pages submit tickets instead of storage identifiers', () => {
  for (const [name, source] of [
    ['upload submitter', uploadSubmitterSource],
    ['detail page', detailPageSource],
    ['approval actions', approvalActionsSource],
    ['external review page', externalReviewPageSource]
  ]) {
    assert.doesNotMatch(source, /\boriginalFileId\b/, `${name} must not submit originalFileId`)
    assert.doesNotMatch(source, /\bsourceFileId\b/, `${name} must not submit sourceFileId`)
    assert.doesNotMatch(source, /\bdrawingPdfFileId\b/, `${name} must not submit drawingPdfFileId`)
    assert.doesNotMatch(source, /\bstampedPdfFileId\b/, `${name} must not submit stampedPdfFileId`)
    assert.doesNotMatch(source, /\btrainingRecordFileId\b/, `${name} must not submit trainingRecordFileId`)
    assert.doesNotMatch(source, /\boutputFileId\b/, `${name} must not submit outputFileId`)
  }
  assert.match(uploadSubmitterSource, /\bsessionId:\s*previewFile\.sessionId/)
  assert.match(uploadSubmitterSource, /\boriginalUploadTicket:\s*previewFile\.uploadTicket/)
  assert.match(uploadSubmitterSource, /\bsourceUploadTicket:\s*previewFile\.uploadTicket/)
  assert.match(uploadSubmitterSource, /\bdrawingPdfUploadTicket:\s*drawingPdfUpload\?\.uploadTicket/)
  assert.match(approvalActionsSource, /\bstampedPdfUploadTicket:\s*form\.stampedPdfUploadTicket/)
  assert.match(detailPageSource, /\btrainingRecordUploadTicket:\s*applicantTrainingRecordDialog\.file\.uploadTicket/)
  assert.match(detailPageSource, /\boutputUploadTicket:\s*externalReviewAction\.outputFile\?\.uploadTicket/)
  assert.match(externalReviewPageSource, /\bsessionId:\s*previewUpload\.value\.sessionId/)
  assert.match(externalReviewPageSource, /\boriginalUploadTicket:\s*previewUpload\.value\.uploadTicket/)
  assert.match(externalReviewPageSource, /\bsourceUploadTicket:\s*previewUpload\.value\.uploadTicket/)
  assert.match(externalReviewPageSource, /\bdrawingPdfUploadTicket:\s*drawingPdfUpload\.value\?\.uploadTicket/)
})

test('BDD: upload preview request binding -> Given a user uploads a preview file, When frontend calls the preview API, Then it sends categoryId and a stable sessionId before a ticket can be created', () => {
  assert.match(workflowSource, /export interface ControlledFileUploadPreviewContext/)
  assert.match(workflowSource, /categoryId:\s*number/)
  assert.match(workflowSource, /sessionId:\s*string/)
  assert.match(workflowSource, /formData\.append\('categoryId',\s*String\(context\.categoryId\)\)/)
  assert.match(workflowSource, /formData\.append\('sessionId',\s*context\.sessionId\)/)
  assert.match(uploadSubmitterSource, /uploadPreview\(file:\s*File,\s*purpose:\s*UploadPreviewPurpose,\s*context:/)
  assert.match(uploadPageSource, /const uploadSessionId = createControlledFileUploadSessionId\(\)/)
  assert.match(externalReviewPageSource, /const uploadSessionId = createControlledFileUploadSessionId\(\)/)
})

test('BDD: upload temporary lifecycle contract -> Given a preview upload is discarded, When frontend cleans the upload session, Then the response is parsed without exposing storage capability fields', () => {
  const statusBody = extractInterfaceBody(workflowSource, 'ControlledFileUploadTemporaryStatusRespVO')

  assert.match(statusBody, /requestId:\s*string/)
  assert.match(statusBody, /temporaryFileCount:\s*number/)
  assert.match(statusBody, /bindable:\s*boolean/)
  assert.match(statusBody, /cleanedCount\?:\s*number/)
  assert.match(workflowSource, /parseControlledFileUploadTemporaryStatusResp/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/upload-temporary\/status/)
  assert.match(workflowSource, /\/dcc\/controlled-files\/upload-temporary\/session-cleanup/)
  assert.match(workflowSource, /headers:\s*requestId\s*\?\s*\{\s*\[DCC_REQUEST_ID_HEADER\]:\s*requestId\s*\}\s*:\s*undefined/)
  assert.match(workflowSource, /assertNoForbiddenDccFileCapabilityFields\(payload,\s*'DCC upload temporary status'\)/)
  for (const source of [uploadPageSource, externalReviewPageSource, detailPageSource]) {
    assert.match(source, /cleanupControlledFileUploadSession/)
    assert.match(source, /\.requestId/)
    assert.match(source, /onBeforeRouteLeave/)
  }
})

test('BDD: page preview and download integration -> Given DCC pages render previews or downloads, When metadata is missing or download starts, Then pages use only controlled metadata URLs and no fallback file names', () => {
  assert.doesNotMatch(uploadPageSource, /\bonlyofficeDocumentUrl\b/)
  assert.doesNotMatch(previewPageSource, /\bprops\.onlyofficeDocumentUrl\b/)
  assert.match(previewPageSource, /\bmetadata\.onlyofficeDocumentUrl\b/)
  assert.doesNotMatch(detailPageSource, /triggerControlledFileDownload\([^,\n]+,\s*[^)]/)
  assert.doesNotMatch(minePageSource, /triggerControlledFileDownload\([^,\n]+,\s*[^)]/)
})

test('BDD: audit page integration -> Given an auditor searches DCC traces, When the page calls backend audit API, Then it exposes trace filters and no storage fields', () => {
  assert.match(auditApiSource, /\/dcc\/controlled-file-audits\/page/)
  for (const field of [
    'accessEventCode',
    'watermarkTraceCode',
    'controlledFileId',
    'userId',
    'actionType',
    'result',
    'failureCode',
    'occurredAt'
  ]) {
    assert.match(auditApiSource, new RegExp(`\\b${field}\\??:`), `${field} must exist in audit API contract`)
    assert.match(auditPageSource, new RegExp(`queryParams\\.${field}`), `${field} must be wired on audit page`)
  }
  for (const forbidden of ['storageFileId', 'sourceFileId', 'originalFileId', 'publishedFileId', 'filePath', 'fileUrl', 'cipherFileRef']) {
    assert.doesNotMatch(auditApiSource, new RegExp(`\\b${forbidden}\\b`))
    assert.doesNotMatch(auditPageSource, new RegExp(`\\b${forbidden}\\b`))
  }
  assert.match(remainingRouteSource, /controlled-file\/audit/)
  assert.match(remainingRouteSource, /dcc:controlled-file:audit:query/)
})
