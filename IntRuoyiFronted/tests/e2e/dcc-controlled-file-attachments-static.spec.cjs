const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relative) => fs.readFileSync(path.join(repoRoot, relative), 'utf8')

const workflowApi = read('src/api/dcc/controlledFile/workflow.ts')
const filePreviewApi = read('src/api/common/filePreview.ts')
const uploadPage = read('src/views/dcc/controlled-file/upload/index.vue')
const uploadSubmitter = read('src/views/dcc/controlled-file/upload/submitter.ts')
const detailPage = read('src/views/dcc/controlled-file/detail/index.vue')

assert(
  /export interface ControlledFileAttachmentVO[\s\S]*attachmentId[\s\S]*fileName[\s\S]*previewKind/.test(workflowApi),
  'workflow API must expose controlled file attachment VO'
)
assert(
  /attachments\?: ControlledFileAttachmentVO\[\]/.test(workflowApi),
  'ControlledFileVO must include attachments'
)
assert(
  /attachmentUploadTickets\?: ControlledFileAttachmentUploadTicketVO\[\]/.test(workflowApi),
  'submit request must include attachmentUploadTickets'
)
assert(
  /DCC_CONTROLLED_FILE_ATTACHMENT/.test(filePreviewApi) &&
    /buildDccControlledFileAttachmentPreviewSource/.test(filePreviewApi),
  'file preview API must support DCC controlled file attachment source'
)
assert(
  /purpose:\s*'ATTACHMENT'/.test(uploadPage) &&
    /multiple/.test(uploadPage) &&
    /data-testid="dcc-upload-attachment-files"/.test(uploadPage),
  'upload page must provide multi attachment upload control'
)
assert(
  /attachmentUploadTickets:\s*attachmentUploads\.map/.test(uploadSubmitter),
  'submitter must map uploaded attachments into attachmentUploadTickets'
)
assert(
  /data-testid="dcc-detail-attachments"/.test(detailPage) &&
    /fileDetail\?\.attachments/.test(detailPage) &&
    /openAttachmentPreview/.test(detailPage),
  'detail page must render attachment list and preview action'
)

console.log('DCC controlled file attachments static contract passed')
