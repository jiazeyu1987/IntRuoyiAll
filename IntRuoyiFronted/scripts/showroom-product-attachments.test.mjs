import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const apiSource = readText('src/api/showroom-admin/index.ts')
const workspaceSource = readText('src/views/showroom-admin/index.vue')
const productContractsSource = readText('src/views/showroom-admin/product/contracts.ts')

test('BDD: showroom product attachment API -> Given product base dialog uploads an attachment, When frontend calls backend, Then it posts FormData to the dedicated endpoint and keeps the returned fileId contract', () => {
  assert.match(apiSource, /export interface ShowroomProductAttachment/)
  assert.match(apiSource, /export interface ShowroomProductAttachmentUploadRespVO/)
  assert.match(apiSource, /uploadProductAttachment:\s*async/)
  assert.match(apiSource, /url:\s*'\/showroom\/product\/attachment\/upload'/)
  assert.match(apiSource, /const response = await request\.upload/)
  assert.match(apiSource, /return response\.data as ShowroomProductAttachmentUploadRespVO/)
  assert.match(apiSource, /attachments\?:\s*ShowroomProductAttachment\[\]/)
})

test('BDD: base dialog attachment section -> Given product base dialog is editable, When attachments render, Then it exposes upload type, upload button and attachment table actions', () => {
  assert.match(workspaceSource, /附件资料/)
  assert.match(workspaceSource, /productAttachmentUploadType/)
  assert.match(workspaceSource, /handleProductAttachmentUpload/)
  assert.match(workspaceSource, /productForm\.attachments/)
  assert.match(workspaceSource, /handleMoveProductAttachment/)
  assert.match(workspaceSource, /handleRemoveProductAttachment/)
})

test('BDD: save and publish payload include attachments -> Given attachments are edited, When saving or publishing, Then payload contains sorted attachments and direct publish preserves detail attachments', () => {
  assert.match(workspaceSource, /buildProductAttachmentPayload\(\)/)
  assert.match(workspaceSource, /attachments:\s*buildProductAttachmentPayload\(\)/)
  assert.match(workspaceSource, /attachments:\s*productDetail\.attachments/)
  assert.match(productContractsSource, /attachments:\s*ShowroomProductAttachment\[\]/)
  assert.match(productContractsSource, /expectProductAttachments/)
})

test('BDD: attachment file name opens the uploaded file -> Given an image, video or text attachment exists, When the basic info dialog renders it, Then the displayed file name links to the official file URL', () => {
  assert.match(apiSource, /export interface ShowroomProductAttachment[\s\S]*url\?:\s*string/)
  assert.match(apiSource, /export interface ShowroomProductAttachmentUploadRespVO[\s\S]*url:\s*string/)
  assert.match(productContractsSource, /export interface ShowroomProductAttachment[\s\S]*url\?:\s*string/)
  assert.match(productContractsSource, /expectProductAttachments[\s\S]*url:\s*expectNullableString/)
  assert.match(workspaceSource, /resolveProductAttachmentFileUrl/)
  assert.match(workspaceSource, /showroom-admin-product-dialog__attachment-link/)
  assert.match(workspaceSource, /window\.open\(url,\s*'_blank',\s*'noopener'\)/)
})

test('BDD: attachment payload normalization does not call trim on undefined optional fields -> Given attachment records from upload or detail APIs omit optional text fields, When saving the product, Then payload construction normalizes them before trimming', () => {
  assert.match(workspaceSource, /normalizeProductAttachmentForPayload/)
  assert.doesNotMatch(workspaceSource, /attachment\.originalName\.trim\(\)/)
  assert.doesNotMatch(workspaceSource, /attachment\.mimeType\.trim\(\)/)
})

test('BDD: read-only product attachments -> Given the backend marks product detail not editable, When dialog opens, Then attachment upload and row actions are disabled', () => {
  assert.match(workspaceSource, /const productDialogEditable = ref\(true\)/)
  assert.match(workspaceSource, /const canEditProductDialog = computed/)
  assert.match(workspaceSource, /productDialogEditable\.value = Boolean\(revision\.editable\)/)
  assert.match(workspaceSource, /:disabled="!canEditProductDialog/)
  assert.match(workspaceSource, /v-if="canEditProductDialog"/)
})
