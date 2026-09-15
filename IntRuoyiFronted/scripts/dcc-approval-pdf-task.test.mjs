import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import vm from 'node:vm'
import test from 'node:test'
const source = readFileSync(new URL('../src/views/dcc/controlled-file/detail/index.vue', import.meta.url), 'utf8')
const start = source.indexOf("const handleStampedPdfChange:")
const handler = source.slice(start, source.indexOf('\nconst ', start + 10))
const setup = () => {
  const c = {
    fileDetail: { value: { id: 900, categoryId: 10 } }, approvalTodoTask: { value: { id: 'task-4' } },
    fourthNodeUploadSessionId: { value: 'session-1' }, stampedPdfFileList: { value: [] },
    fourthNodeUpload: { stampedPdf: undefined, stampedLoading: false },
    actionDialog: { fieldErrors: {}, inlineError: '' }, stampedPdfUploadRef: { value: { clearFiles() {} } },
    isPdfUploadFile: () => true, clearActionDialogFieldError() {}, resolveReadSideErrorMessage: e => e.message,
    buildDetailUploadPreviewContext: sessionId => ({ categoryId: 10, sessionId })
  }
  vm.createContext(c)
  vm.runInContext(ts.transpileModule(`${handler};globalThis.change=handleStampedPdfChange`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c
}
test('approval PDF upload carries exact task/file scope and its own purpose', async () => {
  const c = setup(); let call
  c.uploadControlledFilePreview = async (...args) => { call = args; return { uploadTicket: 'UT-APPROVAL' } }
  await c.change({ raw: {}, name: 'approval.pdf' }, [])
  assert.equal(call[1], 'APPROVAL_PDF')
  assert.equal(call[2].controlledFileId, 900)
  assert.equal(call[2].taskId, 'task-4')
  assert.equal(call[2].sessionId, 'dcc-approval:900:6:task-4:session-1')
})
test('a task switch while uploading cannot bind the old approval ticket', async () => {
  const c = setup(); let finish
  c.uploadControlledFilePreview = () => new Promise(resolve => { finish = resolve })
  const running = c.change({ raw: {}, name: 'approval.pdf' }, [])
  c.approvalTodoTask.value = { id: 'task-5' }
  finish({ uploadTicket: 'UT-OLD' }); await running
  assert.equal(c.fourthNodeUpload.stampedPdf, undefined)
})
