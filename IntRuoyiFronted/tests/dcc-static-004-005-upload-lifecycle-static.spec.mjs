import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const page = readFileSync(join(root, 'src/views/dcc/controlled-file/upload/index.vue'), 'utf8')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  const end = source.indexOf(endToken, start + startToken.length)
  assert.ok(start >= 0 && end > start, `无法提取 ${startToken} 到 ${endToken}`)
  return source.slice(start, end)
}

const attachmentChange = extractBetween(
  page,
  'const handleAttachmentFileChange',
  'const handleBeforeAttachmentRemove'
)
const attachmentRemove = extractBetween(
  page,
  'const handleBeforeAttachmentRemove',
  'const handleAttachmentRemove'
)
const sourceChange = extractBetween(page, 'const handleFileChange', 'const handleBeforeFileRemove')
const drawingChange = extractBetween(page, 'const handleDrawingPdfChange', 'const handleBeforeDrawingPdfRemove')
const drawingRemove = extractBetween(page, 'const handleBeforeDrawingPdfRemove', 'const handleDrawingPdfRemove')
const submitForm = extractBetween(page, 'const submitForm = async () =>', 'watch(')

assert.match(page, /attachmentUploadAttempts/, 'attachments must track every selected upload attempt')
assert.match(page, /status:\s*'UPLOADING'/, 'attachment attempts must enter an uploading state before awaiting the request')
assert.match(page, /attempt\.status\s*=\s*'FAILED'/, 'failed attachments must remain explicit instead of being silently removed')
assert.match(
  submitForm,
  /hasUnreadyAttachmentUploads|resolveReadyAttachmentUploads|attachmentUploadAttempts\.value[\s\S]*status/,
  'submit must inspect the complete attachment attempt state before sending the business request'
)
assert.match(
  submitForm,
  /if \([^\n]*(?:UPLOADING|FAILED|unready|Unready)[^\n]*\)[\s\S]*return/,
  'submit must block while an attachment is pending or failed'
)
assert.match(
  submitForm,
  /const attachmentSnapshot|attachmentUploadsSnapshot|readyAttachmentUploads/,
  'submit must pass a frozen ready attachment snapshot to the submitter'
)

assert.match(
  attachmentChange,
  /isCurrentAttachmentUpload|attachmentUploadGeneration|attachmentUploadRequest/,
  'attachment response handling must verify the current request identity'
)
assert.match(
  attachmentChange,
  /cleanupStaleUploadResponse|cleanupControlledFileUploadTicket/,
  'late attachment responses must clean their own temporary ticket'
)
assert.match(
  attachmentRemove,
  /invalidateAttachmentUpload|bumpAttachmentUpload|attachmentUploadGeneration|nextAttachmentUploadGeneration/,
  'removing an attachment must invalidate an in-flight response before awaiting cleanup'
)

assert.match(
  sourceChange,
  /isCurrentSourceUpload|previewUploadRequestSeq|sourceUploadRequestSeq/,
  'source response handling must verify the current selected file/request identity'
)
assert.match(
  sourceChange,
  /cleanupStaleUploadResponse|cleanupControlledFileUploadTicket/,
  'late source responses must clean their own temporary ticket'
)

assert.match(
  drawingChange,
  /drawingPdfUploadRequestSeq[\s\S]*cleanupStaleUploadResponse[\s\S]*drawingPdfUploadRequestSeq/,
  'drawing PDF response handling must protect the current request and clean late tickets'
)
assert.match(
  drawingRemove,
  /cleanupTemporaryUploadTicket[\s\S]*drawingPdfUploadRequestSeq\+\+/,
  'removing a drawing PDF must invalidate its in-flight response after cleanup succeeds'
)

console.log('PASS: DCC-STATIC-004-005 upload lifecycle contract')
