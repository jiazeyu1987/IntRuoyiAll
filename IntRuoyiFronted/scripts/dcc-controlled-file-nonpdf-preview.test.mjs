import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('dcc workflow api exposes preview metadata and nas transfer contracts', () => {
  const source = readText('src/api/dcc/controlledFile/workflow.ts')

  assert.match(source, /ControlledFilePreviewKind/)
  assert.match(source, /ControlledFilePreviewMetadataVO/)
  assert.match(source, /ControlledFileNasTransferReqVO/)
  assert.match(source, /ControlledFileNasTransferRespVO/)
  assert.match(source, /getControlledFilePreviewMetadata/)
  assert.match(source, /transferNasDirectories/)
  assert.match(source, /previewKind/)
  assert.match(source, /onlyofficeBaseUrl/)
  assert.match(source, /onlyofficeDocumentUrl/)
  assert.match(source, /previewUnavailableReason/)
  assert.match(source, /\/dcc\/controlled-files\/\$\{id\}\/preview-metadata/)
  assert.match(source, /\/dcc\/controlled-files\/nas-transfer/)
})

test('dcc upload page supports non-pdf selection and preview branching', () => {
  const uploadSource = readText('src/views/dcc/controlled-file/upload/index.vue')
  const submitterSource = readText('src/views/dcc/controlled-file/upload/submitter.ts')
  const viewerSource = readText('src/views/dcc/controlled-file/view/index.vue')

  assert.match(uploadSource, /previewKind/)
  assert.match(uploadSource, /onlyofficeBaseUrl/)
  assert.match(uploadSource, /受控文件/)
  assert.doesNotMatch(uploadSource, /accept="\.pdf,application\/pdf"/)
  assert.doesNotMatch(submitterSource, /仅支持上传 PDF 文件/)
  assert.doesNotMatch(submitterSource, /validatePdfSelection/)
  assert.match(submitterSource, /validateControlledFileSelection/)
  assert.match(viewerSource, /OnlyOfficeReadOnlyViewer/)
  assert.match(viewerSource, /PDF/)
  assert.match(viewerSource, /IMAGE/)
  assert.match(viewerSource, /TEXT/)
  assert.match(viewerSource, /OFFICE/)
  assert.match(viewerSource, /DOWNLOAD_ONLY/)
  assert.match(viewerSource, /getControlledFilePreviewMetadata/)
})
