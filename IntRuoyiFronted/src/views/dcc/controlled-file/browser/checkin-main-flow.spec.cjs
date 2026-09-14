const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.join(__dirname, 'index.vue'), 'utf8')
const start = source.indexOf('const submitCheckin = async () => {')
const end = source.indexOf('\nconst mergeCheckinResult', start)
assert.ok(start >= 0 && end > start, '检入提交处理器必须存在')
const handler = ts.transpileModule(source.slice(start, end) + '\nglobalThis.submitCheckin = submitCheckin',
  { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const helperSource = fs.readFileSync(path.join(__dirname, '../upload/submitter.ts'), 'utf8')
const helperExports = {}
vm.runInNewContext(ts.transpileModule(helperSource, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText, { exports: helperExports, module: { exports: helperExports } })

async function submit({ sourceFile, drawingPdf, remark = '原备注' }) {
  const requests = []
  const warnings = []
  const errors = []
  const context = {
    checkinTarget: { value: { id: 900, remark: '原备注' } },
    checkinUpload: { value: sourceFile },
    checkinDrawingPdfUpload: { value: drawingPdf },
    checkinForm: { changeDescription: '修正主流程文档', remark },
    checkinSubmitting: { value: false },
    checkinUploadLoading: { value: false },
    checkinDrawingPdfLoading: { value: false },
    checkoutLoadingId: { value: undefined },
    checkinUploadSessionId: { value: 'checkin-session' },
    checkinDialogVisible: { value: true },
    isValidBrowserOptionId: id => Number(id) > 0,
    validateDrawingPdfUpload: helperExports.validateDrawingPdfUpload,
    isDrawingSourceFile: helperExports.isDrawingSourceFile,
    message: { warning: value => warnings.push(value), error: value => errors.push(value), success() {} },
    checkinControlledFile: async (id, payload) => {
      requests.push({ id, payload: JSON.parse(JSON.stringify(payload)) })
      return { id: 901, versionNo: 'A/2' }
    },
    getList: async () => {},
    mergeCheckinResult() {},
    resolveBrowserErrorMessage: error => String(error)
  }
  vm.runInNewContext(handler, context)
  await context.submitCheckin()
  assert.deepEqual(errors, [])
  return { requests, warnings }
}

test('修改图纸但未提供配套PDF时不提交', async () => {
  const result = await submit({ sourceFile: { fileName: '修改后.dwg', uploadTicket: 'SOURCE-TICKET' } })
  assert.equal(result.requests.length, 0)
  assert.ok(result.warnings.some(value => value.includes('PDF')))
})

test('图纸和配套PDF以同一会话提交', async () => {
  const result = await submit({
    sourceFile: { fileName: '修改后.sldprt', uploadTicket: 'SOURCE-TICKET' },
    drawingPdf: { fileName: '修改后.pdf', uploadTicket: 'PDF-TICKET' }
  })
  assert.deepEqual(result.requests, [{
    id: 900,
    payload: {
      uploadTicket: 'SOURCE-TICKET', drawingPdfUploadTicket: 'PDF-TICKET',
      sessionId: 'checkin-session', changeDescription: '修正主流程文档', remark: '原备注'
    }
  }])
})

test('普通文档检入不要求配套PDF', async () => {
  const result = await submit({ sourceFile: { fileName: '修改后.docx', uploadTicket: 'SOURCE-TICKET' } })
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].payload.uploadTicket, 'SOURCE-TICKET')
  assert.equal(result.requests[0].payload.drawingPdfUploadTicket, undefined)
})

test('仅备注变化仍可检入', async () => {
  const result = await submit({ remark: '修正后的备注' })
  assert.equal(result.requests.length, 1)
  assert.equal(result.requests[0].payload.uploadTicket, undefined)
  assert.equal(result.requests[0].payload.remark, '修正后的备注')
})

test('旧图纸PDF上传的迟到结果不得清除新源件的PDF', async () => {
  const uploadStart = source.indexOf('const uploadCheckinDrawingPdf = async')
  const uploadEnd = source.indexOf('\nconst submitCheckin', uploadStart)
  assert.ok(uploadStart >= 0 && uploadEnd > uploadStart)
  const uploadHandler = ts.transpileModule(source.slice(uploadStart, uploadEnd) +
    '\nglobalThis.uploadCheckinDrawingPdf = uploadCheckinDrawingPdf', {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  let resolveUpload
  const clearStart = source.indexOf('const clearCheckinDrawingPdf = () => {')
  const clearEnd = source.indexOf('\nconst clearCheckinUpload', clearStart)
  assert.ok(clearStart >= 0 && clearEnd > clearStart)
  const clearHandler = ts.transpileModule(source.slice(clearStart, clearEnd) +
    '\nglobalThis.clearCheckinDrawingPdf = clearCheckinDrawingPdf', {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }).outputText
  const pending = new Promise(resolve => { resolveUpload = resolve })
  const nextPdf = { uploadTicket: 'NEXT-PDF', fileName: 'next.pdf' }
  const context = {
    checkinTarget: { value: { id: 900 } },
    findBrowserRowForVersion: () => ({ categoryId: 10 }),
    checkinUpload: { value: { uploadTicket: 'OLD-SOURCE', fileName: 'old.dwg' } },
    checkinUploadSessionId: { value: 'session' },
    checkinDrawingPdfLoading: { value: false },
    checkinDrawingPdfUpload: { value: undefined },
    checkinDrawingPdfFileList: { value: [] },
    checkinDrawingPdfRequestSequence: 0,
    isDrawingSourceFile: helperExports.isDrawingSourceFile,
    uploadControlledFilePreview: () => pending,
    message: { warning() {}, error() {} },
    resolveBrowserErrorMessage: error => String(error)
  }
  vm.runInNewContext(uploadHandler, context)
  vm.runInNewContext(clearHandler, context)
  const finished = context.uploadCheckinDrawingPdf({ file: {}, onError() {}, onSuccess() {} })
  context.checkinUpload.value = { uploadTicket: 'NEXT-SOURCE', fileName: 'next.dwg' }
  context.clearCheckinDrawingPdf()
  context.checkinDrawingPdfUpload.value = nextPdf
  context.checkinDrawingPdfLoading.value = true
  resolveUpload({ uploadTicket: 'OLD-PDF', fileName: 'old.pdf' })
  await finished
  assert.equal(context.checkinDrawingPdfUpload.value, nextPdf)
  assert.equal(context.checkinDrawingPdfLoading.value, true)
})
