import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.dirname(path.dirname(fileURLToPath(import.meta.url)))
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc signature evidence export API downloads the real backend artifact strictly', () => {
  const source = readText('src/api/dcc/controlledFile/signatures.ts')

  assert.match(source, /export interface DccSignatureEvidenceExportDownload/)
  assert.match(source, /export const fetchDccSignatureEvidencePdfArtifact = async/)
  assert.match(source, /export const downloadDccSignatureEvidenceExport = async/)
  assert.match(source, /axios\.get<Blob>/)
  assert.match(source, /\/dcc\/controlled-files\/\$\{controlledFileId\}\/signature-evidence-export/)
  assert.match(source, /responseType:\s*'blob'/)
  assert.match(source, /decodeContentDispositionFileName/)
  assert.match(source, /签名证据导出响应缺少文件名/)
  assert.match(source, /assertDccSignatureEvidencePdfArtifact/)
  assert.match(source, /fileName\.toLowerCase\(\)\.endsWith\('\.pdf'\)/)
  assert.match(source, /application\/pdf/)
  assert.match(source, /downloadByData\(blob,\s*fileName,\s*'application\/pdf'\)/)
  assert.doesNotMatch(source, /artifactType\s*!==\s*'DCC_SIGNATURE_EVIDENCE_EXPORT'/)
  assert.doesNotMatch(source, /blob\.type\s*\|\|\s*'application\/json'/)
  assert.doesNotMatch(
    source,
    /signature-export-summary[\s\S]{0,240}downloadByData/,
    'export download must not be synthesized from the summary endpoint'
  )
})

test('dcc signature management page exposes a real row action and surfaces export errors', () => {
  const source = readText('src/views/dcc/controlled-file/signatures/index.vue')

  assert.match(source, /downloadDccSignatureEvidenceExport/)
  assert.match(source, /handleExportSignatureEvidence/)
  assert.match(source, /exportingControlledFileIds/)
  assert.match(source, />\s*下载证据 PDF\s*</)
  assert.match(source, /v-hasPermi="\['dcc:controlled-file:query', 'dcc:controlled-file:download'\]"/)
  assert.match(source, /签名证据导出失败，请查看错误提示后重试。/)
  assert.doesNotMatch(source, /test-only|测试专用|mock/i)
})

test('dcc signature evidence online view previews the generated PDF artifact', () => {
  const source = readText('src/views/dcc/controlled-file/signatures/index.vue')

  assert.match(source, /fetchDccSignatureEvidencePdfArtifact/)
  assert.match(source, /openSignaturePdfPreview/)
  assert.match(source, /signaturePdfPreviewDialog/)
  assert.match(source, /<iframe[\s\S]*:src="signaturePdfPreviewDialog\.objectUrl"/)
  assert.match(source, /URL\.createObjectURL\(artifact\.blob\)/)
  assert.match(source, /URL\.revokeObjectURL/)
  assert.doesNotMatch(source, /getDccElectronicSignatureEvidence/)
  assert.doesNotMatch(source, /verifyDccElectronicSignatureEvidence/)
  assert.doesNotMatch(source, />\s*重新校验证据\s*</)
  assert.doesNotMatch(source, /规范载荷/)

  const previewHandler = source.match(/const openSignaturePdfPreview[\s\S]*?const isExportingControlledFile/)
  assert.ok(previewHandler, 'missing PDF preview handler block')
  assert.doesNotMatch(previewHandler[0], /downloadDccSignatureEvidenceExport/)
})

test('unified signature governance records previews and downloads every signature record PDF inline', () => {
  const apiSource = readText('src/api/signature-governance/records.ts')
  const source = readText('src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue')

  assert.match(apiSource, /fetchSignatureGovernanceRecordPdfArtifact/)
  assert.match(apiSource, /downloadSignatureGovernanceRecordPdf/)
  assert.match(apiSource, /\/signature-governance\/signature-records\/\$\{encodeURIComponent\(String\(normalizedGlobalId\)\)\}\/pdf/)
  assert.match(source, /fetchSignatureGovernanceRecordPdfArtifact/)
  assert.match(source, /downloadSignatureGovernanceRecordPdf/)
  assert.match(source, /signatureRecordPdfPreviewDialog/)
  assert.match(source, /openRecordPdfPreview/)
  assert.match(source, /handleRecordPdfDownload/)
  assert.match(source, />\s*预览\s*</)
  assert.match(source, />\s*PDF\s*</)
  assert.match(source, /<iframe[\s\S]*:src="signatureRecordPdfPreviewDialog\.objectUrl"/)
  assert.match(source, /URL\.createObjectURL\(artifact\.blob\)/)
  assert.match(source, /URL\.revokeObjectURL/)
  assert.doesNotMatch(source, /v-if="row\.sourceCode === 'FILE'"/)
  assert.doesNotMatch(source, />\s*预览PDF\s*</)

  const previewHandler = source.match(/const openRecordPdfPreview[\s\S]*?const openRecordDetail/)
  assert.ok(previewHandler, 'missing unified record PDF preview handler block')
  assert.doesNotMatch(previewHandler[0], /downloadDccSignatureEvidenceExport/)
})

test('my signature pane stays focused on signature image management', () => {
  const apiSource = readText('src/api/signature-governance/records.ts')
  const source = readText('src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue')

  assert.match(apiSource, /getMySignatureGovernanceRecordPage/)
  assert.match(apiSource, /\/signature-governance\/my-signature-records\/page/)
  assert.match(source, /getMyDccElectronicSignatureImage/)
  assert.match(source, /uploadDccElectronicSignatureImage/)
  assert.match(source, /data-testid="dcc-my-signature-image-actions"/)
  assert.doesNotMatch(source, /getMySignatureGovernanceRecordPage/)
  assert.doesNotMatch(source, /fetchDccSignatureEvidencePdfArtifact/)
  assert.doesNotMatch(source, /mySignatureRecordList/)
  assert.doesNotMatch(source, /我的签名记录/)
  assert.doesNotMatch(source, /openMySignatureRecordPdfPreview/)
  assert.doesNotMatch(source, /getSignatureGovernanceRecordPage\(/)
})

test('dcc signature evidence shared labels cover all backend signature actions', () => {
  const source = readText('src/views/dcc/controlled-file/shared/signature-evidence.ts')

  for (const action of [
    'APPROVED',
    'REJECTED',
    'RETURNED',
    'TRANSFERRED',
    'SIGN_ADDED',
    'DISTRIBUTION_ACK',
    'DISTRIBUTION_SIGN'
  ]) {
    assert.match(source, new RegExp(`value:\\s*'${action}'`), `missing action label for ${action}`)
  }

  for (const meaning of [
    'DOC_CONTROL_REVIEW_RETURN',
    'MATRIX_REVIEW_TRANSFER',
    'MATRIX_APPROVAL_ADD_SIGN',
    'DOC_CONTROL_APPROVAL_RETURN',
    'DISTRIBUTION_ACK',
    'DISTRIBUTION_SIGN'
  ]) {
    assert.match(source, new RegExp(`value:\\s*'${meaning}'`), `missing meaning label for ${meaning}`)
  }
})
