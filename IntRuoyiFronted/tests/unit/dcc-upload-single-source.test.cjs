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
  'handleFileRemove',
  'handleBeforeAttachmentRemove', 'findAttachmentUploadAttempt', 'nextAttachmentUploadGeneration',
  'isCurrentAttachmentUpload', 'syncAttachmentUploadLoading', 'resetAttachmentUploads']
const handlers = ast.statements.filter(s => s.declarationList?.declarations.some(d => names.includes(d.name.text)))
  .map(s => s.getText(ast)).join('\n')
const selection = (name, uid) => ({ uid, name, status: 'ready', raw: { name } })

function host() {
  let session = 0
  const c = {
    exports: {}, ...helpers.exports, calls: [], errors: [], cleaned: [],
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
  vm.runInNewContext(ts.transpileModule(handlers + '\nexports.removeSource=handleFileRemove;exports.source=handleFileChange;exports.attachment=handleAttachmentFileChange;exports.remove=handleAttachmentRemove;exports.beforeRemove=handleBeforeAttachmentRemove', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c
}

test('upload section renders only original and attachments', () => {
  const section = descriptor.template.content.match(/<section[^>]*data-testid="dcc-upload-section-attachment"[\s\S]*?<\/section>/)[0]
  assert.deepEqual([...section.matchAll(/<el-form-item label="([^"]+)"/g)].map(m => m[1]), ['文件上传', '附件'])
  assert.doesNotMatch(section, /可编辑版本|普通附件|必须上传不可编辑/)
  assert.doesNotMatch(section, /上传文件原件，在线浏览为只读模式|源文件为 DWG|可一次选择多个附件/)
  assert.doesNotMatch(section, /图纸文件|DRAWING_PDF/)
  const attachment = section.slice(section.indexOf('label="附件"'))
  assert.doesNotMatch(attachment, /:disabled="[^"]*(?:!previewUpload|uploadPreviewLoading)/)
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
