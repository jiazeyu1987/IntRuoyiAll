const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const page = fs.readFileSync(path.resolve(__dirname, '../../src/views/dcc/controlled-file/external-review/index.vue'), 'utf8')
const script = parse(page).descriptor.scriptSetup.content
const source = ts.createSourceFile('external-review.ts', script, ts.ScriptTarget.Latest, true)

function loadHandler(name, context) {
  context = {
    submitLoading: { value: false },
    uploadPreviewLoading: { value: false },
    uploadDrawingPdfLoading: { value: false },
    ...context
  }
  let initializer
  function visit(node) {
    if (ts.isVariableDeclaration(node) && node.name.getText(source) === name) initializer = node.initializer
    ts.forEachChild(node, visit)
  }
  visit(source)
  assert.ok(initializer, `Missing handler ${name}`)
  const js = ts.transpileModule(`globalThis.handler = ${initializer.getText(source)}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(js, context)
  return context.handler
}

test('PDF deletion cleans its own ticket even when a source remains selected', async () => {
  const oldPdf = { sessionId: 'dcc-external:pdf-session', uploadTicket: 'PDF-OLD' }
  const calls = []
  const handler = loadHandler('handleBeforeDrawingPdfRemove', {
    previewUpload: { value: { uploadTicket: 'SOURCE' } },
    drawingPdfUpload: { value: oldPdf },
    cleanupTemporaryUploadTicket: async (...args) => { calls.push(args); return false },
    cleanupCurrentUploadSession: async () => { throw new Error('Must not clean the source ticket') }
  })
  assert.equal(await handler(), false, 'Cleanup failure must prevent deletion')
  assert.equal(calls.length, 1)
  assert.equal(calls[0][0], oldPdf)
})

test('PDF replacement stops before upload when old-ticket cleanup fails', async () => {
  const oldPdf = { sessionId: 'dcc-external:pdf-session', uploadTicket: 'PDF-OLD' }
  let uploadCalls = 0
  let cleanupCalls = 0
  const state = { value: oldPdf }
  const handler = loadHandler('handleDrawingPdfChange', {
    validateSingleUploadFileSelection: () => ({ valid: true }),
    drawingPdfUpload: state,
    drawingPdfUploadRef: { value: { clearFiles() {} } },
    drawingPdfFileList: { value: [] },
    uploadDrawingPdfLoading: { value: false },
    cleanupTemporaryUploadTicket: async (upload) => { assert.equal(upload, oldPdf); cleanupCalls++; return false },
    uploadControlledFilePreview: async () => { uploadCalls++; return { uploadTicket: 'PDF-NEW' } },
    buildUploadPreviewContext: () => ({}),
    message: { error: (message) => { throw new Error(message) } }
  })
  const file = { name: 'drawing.pdf', raw: { type: 'application/pdf' } }
  await handler(file, [file])
  assert.equal(cleanupCalls, 1)
  assert.equal(uploadCalls, 0)
  assert.equal(state.value, oldPdf)
})

test('single-ticket cleanup sends the returned session and preserves failures', async () => {
  const upload = { sessionId: 'dcc-external:server-session', uploadTicket: 'PDF-OLD', requestId: 'request-1' }
  const calls = []
  const errors = []
  let rejectCleanup = false
  const handler = loadHandler('cleanupTemporaryUploadTicket', {
    uploadSubmitted: { value: false },
    cleanupControlledFileUploadTicket: async (...args) => {
      calls.push(args)
      if (rejectCleanup) throw new Error('cleanup unavailable')
    },
    resolveUploadErrorMessage: (error) => error.message,
    message: { error: (message) => errors.push(message) }
  })
  assert.equal(await handler(upload), true)
  assert.deepEqual(calls[0], [upload.sessionId, upload.uploadTicket, upload.requestId])
  rejectCleanup = true
  assert.equal(await handler(upload), false)
  assert.deepEqual(errors, ['cleanup unavailable'])
})

test('invalid PDF replacement cannot discard the old ticket when cleanup fails', async () => {
  const oldPdf = { sessionId: 'dcc-external:old-session', uploadTicket: 'PDF-OLD' }
  const state = { value: oldPdf }
  let cleanupCalls = 0
  let clearCalls = 0
  const handler = loadHandler('handleDrawingPdfChange', {
    validateSingleUploadFileSelection: () => ({ valid: true }),
    drawingPdfUpload: state,
    drawingPdfUploadRef: { value: { clearFiles() { clearCalls++ } } },
    resetDrawingPdfUpload: () => { state.value = undefined },
    cleanupTemporaryUploadTicket: async (upload) => { assert.equal(upload, oldPdf); cleanupCalls++; return false },
    message: { error() {} }
  })
  const file = { name: 'invalid.txt', raw: { type: 'text/plain' } }
  await handler(file, [file])
  assert.equal(cleanupCalls, 1)
  assert.equal(state.value, oldPdf)
  assert.equal(clearCalls, 0)
})

test('replacement drops the cleaned ticket while awaiting the new PDF', async () => {
  const state = { value: { uploadTicket: 'PDF-OLD' } }
  let finishUpload
  const pendingUpload = new Promise((resolve) => { finishUpload = resolve })
  const handler = loadHandler('handleDrawingPdfChange', {
    validateSingleUploadFileSelection: () => ({ valid: true }),
    drawingPdfUpload: state,
    drawingPdfFileList: { value: [] },
    cleanupTemporaryUploadTicket: async () => true,
    uploadControlledFilePreview: () => pendingUpload,
    buildUploadPreviewContext: () => ({})
  })
  const file = { name: 'new.pdf', raw: { type: 'application/pdf' } }
  const running = handler(file, [file])
  await Promise.resolve()
  const duringUpload = state.value
  finishUpload({ uploadTicket: 'PDF-NEW' })
  await running
  assert.equal(duringUpload, undefined)
  assert.equal(state.value.uploadTicket, 'PDF-NEW')
})

test('submit waits until all source and PDF upload operations finish', async () => {
  for (const loadingField of ['uploadPreviewLoading', 'uploadDrawingPdfLoading']) {
    let validations = 0
    const handler = loadHandler('submitForm', {
      [loadingField]: { value: true },
      formRef: { value: { validate: async () => { validations++; return false } } },
      message: { warning() {} }
    })
    await handler()
    assert.equal(validations, 0, loadingField)
  }
})
