const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { ref } = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const source = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const { descriptor } = parse(source)
const ast = ts.createSourceFile('upload.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const helpers = { exports: {}, require: () => ({}) }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, helpers)
const names = ['handleFileChange', 'handleAttachmentFileChange', 'handleAttachmentRemove',
  'handleFileRemove', 'handleBeforeFileRemove', 'resetSelectedPreview', 'handleFileExceed', 'removeMainFile',
  'handleBeforeAttachmentRemove', 'findAttachmentUploadAttempt', 'nextAttachmentUploadGeneration',
  'isCurrentAttachmentUpload', 'syncAttachmentUploadLoading', 'resetAttachmentUploads']
const handlers = ast.statements.filter(s => s.declarationList?.declarations.some(d => names.includes(d.name.text)))
  .map(s => s.getText(ast)).join('\n')
const selection = (name, uid) => ({ uid, name, status: 'ready', raw: { name } })

function host() {
  let session = 0
  const c = {
    genFileId: () => ++session, exports: {}, ...helpers.exports, calls: [], errors: [], cleaned: [],
    submitLoading: ref(false), uploadSubmitted: ref(false), projectAttributesLoading: ref(false),
    isExternalReview: ref(false), sourceUploadRequestSeq: 0, uploadSessionId: 'initial',
    uploadPreviewLoading: ref(false), uploadPreviewError: ref(''), previewFileBlob: ref(null),
    previewUpload: ref(undefined), fileList: ref([]), uploadSessionBinding: ref(undefined),
    attachmentFileList: ref([]), attachmentUploads: ref([]), attachmentUploadAttempts: ref([]),
    attachmentUploadGenerations: new Map(), uploadAttachmentLoading: ref(false),
    drawingPdfFileList: ref([]), drawingPdfUpload: ref(undefined), drawingPdfUploadRequestSeq: 0,
    uploadDrawingPdfLoading: ref(false), drawingPdfUploadRef: ref({ clearFiles() {} }),
    attachmentUploadPurpose: 'ATTACHMENT', submitFieldErrors: {}, formData: { fileName: '' },
    uploadRef: ref({ clearFiles() {} }), resetSelectedPreview() {}, resetDrawingPdfUpload() {},
    cleanupCurrentUploadSession: async () => true, cleanupTemporaryUploadTicket: async () => true,
    cleanupStaleUploadResponse: async upload => c.cleaned.push(upload.uploadTicket),
    captureUploadSelectionContext: () => 'project/type', createControlledFileUploadSessionId: () => `session-${++session}`,
    buildUploadPreviewContext: id => ({ sessionId: id || c.uploadSessionId, fileName: c.formData.fileName }),
    resolveUploadPreviewErrorMessage: error => error.message,
    message: { error: value => c.errors.push(value) }
  }
  c.uploadSubmitterService = { uploadPreview: async (file, purpose, context) => {
    c.calls.push({ name: file.name, purpose, context })
    if (c.failPurpose === purpose) throw new Error('upload failed')
    return { uploadTicket: `${purpose}-${file.name}`, sessionId: context.sessionId,
      fileName: file.name, previewKind: /\.pdf$/i.test(file.name) ? 'PDF' : 'OFFICE' }
  } }
  vm.runInNewContext(ts.transpileModule(handlers + '\nexports.replace=handleFileExceed;exports.beforeRemoveSource=handleBeforeFileRemove;exports.removeSource=handleFileRemove;exports.source=handleFileChange;exports.attachment=handleAttachmentFileChange;exports.remove=handleAttachmentRemove;exports.beforeRemove=handleBeforeAttachmentRemove', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c
}

test('main file preview is separate from supplementary attachments', () => {
  const panel = descriptor.template.content.match(/<section[^>]*data-testid="dcc-upload-main-file-panel"[\s\S]*?<\/section>/)[0]
  assert.match(panel, /prop="file"/)
  assert.match(panel, /:show-file-list="false"/)
  assert.match(panel, /更换文件/)
  assert.match(panel, /@click="removeMainFile"/)
  assert.match(panel, /compact :preview-blob/)
  assert.doesNotMatch(panel, /ref="attachmentUploadRef"/)
  const attachment = descriptor.template.content.match(/<el-upload[\s\S]*?ref="attachmentUploadRef"[\s\S]*?<\/el-upload>/)[0]
  assert.doesNotMatch(attachment.slice(attachment.lastIndexOf('<el-upload')), /:disabled="[^"]*(?:!previewUpload|uploadPreviewLoading)/)
  const compiled = compileTemplate({ source: descriptor.template.content, filename: 'upload.vue', id: 'single-source' })
  assert.deepEqual(compiled.errors, [])
})

test('PDF, Word, Excel and drawing originals share the SOURCE upload path', async () => {
  for (const name of ['procedure.pdf', 'procedure.docx', 'table.xlsx', 'drawing.dwg']) {
    const c = host(), file = selection(name, 1)
    await c.exports.source(file, [file])
    assert.equal(c.calls[0].purpose, 'SOURCE')
    assert.equal(c.previewUpload.value.fileName, name)
    assert.equal(c.formData.fileName, name)
    assert.deepEqual(c.errors, [])
  }
})

test('attachments upload immediately before an original is selected', async () => {
  const c = host(), a = selection('evidence.pdf', 2), b = selection('notes.txt', 3)
  await c.exports.attachment(a, [a])
  await c.exports.attachment(b, [a, b])
  assert.equal(c.calls.length, 2)
  const original = selection('procedure.docx', 1)
  await c.exports.source(original, [original])
  assert.deepEqual(c.calls.map(call => call.purpose), ['ATTACHMENT', 'ATTACHMENT', 'SOURCE'])
  assert.deepEqual(Array.from(c.attachmentFileList.value, file => file.name), ['evidence.pdf', 'notes.txt'])
  assert.equal(c.attachmentUploadAttempts.value.every(attempt => attempt.status === 'READY'), true)
  assert.equal(c.attachmentUploads.value.length, 2)
  assert.notEqual(c.attachmentUploads.value[0].sessionId, c.previewUpload.value.sessionId)
})

test('removing an attachment does not affect later original uploads', async () => {
  const c = host(), attachment = selection('removed.pdf', 2)
  await c.exports.attachment(attachment, [attachment])
  assert.equal(await c.exports.beforeRemove(attachment), true)
  c.exports.remove(attachment, [])
  const original = selection('procedure.pdf', 1)
  await c.exports.source(original, [original])
  assert.deepEqual(c.calls.map(call => call.purpose), ['ATTACHMENT', 'SOURCE'])
  assert.equal(c.attachmentUploads.value.length, 0)
})

test('failed attachments stay failed and report the error without losing the original', async () => {
  const c = host(), attachment = selection('failed.pdf', 2)
  c.failPurpose = 'ATTACHMENT'
  await c.exports.attachment(attachment, [attachment])
  const original = selection('procedure.docx', 1)
  await c.exports.source(original, [original])
  assert.equal(c.attachmentUploadAttempts.value[0].status, 'FAILED')
  assert.equal(c.previewUpload.value.fileName, original.name)
  assert.deepEqual(c.errors, ['upload failed'])
})

test('replacing the original preserves attachments without uploading them again', async () => {
  const c = host(), attachment = selection('evidence.pdf', 2)
  await c.exports.attachment(attachment, [attachment])
  const first = selection('first.docx', 1), second = selection('second.xlsx', 3)
  await c.exports.source(first, [first])
  const formerSession = c.attachmentUploads.value[0].sessionId
  await c.exports.source(second, [second])
  assert.deepEqual(c.calls.map(call => call.purpose), ['ATTACHMENT', 'SOURCE', 'SOURCE'])
  assert.equal(c.attachmentUploads.value[0].sessionId, formerSession)
  assert.equal(c.attachmentUploads.value.length, 1)
})

test('unsupported source types fail visibly before an upload request', async () => {
  const c = host(), file = selection('program.exe', 1)
  await c.exports.source(file, [file])
  assert.equal(c.calls.length, 0)
  assert.equal(c.errors.length, 1)
  assert.match(c.errors[0], /仅支持/)
})


test('replacement button updates summary, filename and preview without clearing user fields', async () => {
  const c = host(), first = selection('first.docx', 1)
  c.formData.remark = 'keep remark'
  c.formData.fileNumber = 'SOP-001'
  await c.exports.source(first, [first])
  await c.exports.replace([{ name: 'second.xlsx', size: 2048 }])
  assert.equal(c.previewUpload.value.fileName, 'second.xlsx')
  assert.equal(c.previewFileBlob.value.name, 'second.xlsx')
  assert.equal(c.formData.fileName, 'second.xlsx')
  assert.equal(c.fileList.value[0].size, 2048)
  assert.equal(c.formData.remark, 'keep remark')
  assert.equal(c.formData.fileNumber, 'SOP-001')
})

test('invalid replacement keeps the existing main file and reports the actual validation error', async () => {
  const c = host(), first = selection('first.docx', 1)
  await c.exports.source(first, [first])
  await c.exports.replace([{ name: 'program.exe', size: 2 }])
  assert.equal(c.calls.length, 1)
  assert.equal(c.previewUpload.value.fileName, 'first.docx')
  assert.match(c.uploadPreviewError.value, /仅支持/)
})

test('removal clears only file preview state after ticket cleanup succeeds', async () => {
  const c = host(), first = selection('first.docx', 1)
  c.formData.remark = 'keep remark'
  await c.exports.source(first, [first])
  c.cleanupTemporaryUploadTicket = async () => false
  assert.equal(await c.exports.beforeRemoveSource(), false)
  assert.equal(c.previewUpload.value.fileName, first.name)
  c.cleanupTemporaryUploadTicket = async () => true
  assert.equal(await c.exports.beforeRemoveSource(), true)
  c.exports.removeSource()
  assert.equal(c.fileList.value.length, 0)
  assert.equal(c.previewUpload.value, undefined)
  assert.equal(c.previewFileBlob.value, null)
  assert.equal(c.uploadPreviewError.value, '')
  assert.equal(c.formData.remark, 'keep remark')
  assert.equal(c.formData.fileName, first.name)
})
