import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url)), 'utf8')
const extract = name => {
  const start = source.indexOf(`const ${name} =`)
  const end = source.indexOf('\n}\n', start) < 0
    ? source.indexOf('\r\n}\r\n', start) + 3 : source.indexOf('\n}\n', start) + 2
  assert.ok(start > 0 && end > start, name)
  return source.slice(start, end)
}
const setup = () => {
  const warnings = []; let writes = 0
  const c = {
    checkinTarget: { value: { id: 1, remark: 'old' } }, checkinUpload: { value: undefined },
    checkinUploadSessionId: { value: 'session-1' }, checkinSourceState: { value: 'idle' },
    checkinUploadLoading: { value: false }, checkinDrawingPdfLoading: { value: false },
    checkinCleanupLoading: { value: false },
    checkinDrawingPdfUpload: { value: undefined }, checkinSubmitting: { value: false },
    checkinForm: { versionChangeType: 'MINOR', remark: 'changed', changeDescription: 'replace content' },
    checkoutLoadingId: { value: undefined }, checkinDialogVisible: { value: true },
    findBrowserRowForVersion: () => ({ categoryId: 10 }), clearCheckinDrawingPdf: () => {},
    isValidBrowserOptionId: id => !!id, validateDrawingPdfUpload: () => ({ valid: true }),
    isDrawingSourceFile: () => false, getList: async () => {}, mergeCheckinResult: () => {},
    checkinControlledFile: async () => { writes++; return { id: 2, versionNo: 'A.2' } },
    message: { error: x => warnings.push(x), warning: x => warnings.push(x), success: () => {} },
    resolveBrowserErrorMessage: e => e.message
  }
  vm.createContext(c)
  const compiled = ts.transpileModule(`let checkinSourceRequestSequence=0; ${extract('uploadCheckinSource')}; ${extract('submitCheckin')}; globalThis.upload=uploadCheckinSource; globalThis.submit=submitCheckin`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  vm.runInContext(compiled, c)
  return { c, warnings, writes: () => writes }
}
test('late source upload cannot attach to a new checkin session', async () => {
  const { c } = setup()
  let resolve; const p = new Promise(yes => { resolve = yes })
  c.uploadControlledFilePreview = () => p
  let success = 0
  const pending = c.upload({ file: { uid: 1 }, onSuccess: () => success++, onError: () => {} })
  c.checkinTarget.value = { id: 2 }; c.checkinUploadSessionId.value = 'session-2'
  resolve({ uploadTicket: 'old-ticket', fileName: 'old.docx' }); await pending
  assert.equal(c.checkinUpload.value, undefined)
  assert.equal(success, 0)
})
test('failed selected upload never turns into metadata-only checkin', async () => {
  const { c, writes, warnings } = setup()
  c.uploadControlledFilePreview = async () => { throw new Error('upload failed') }
  await c.upload({ file: { uid: 1 }, onSuccess: () => {}, onError: () => {} })
  await c.submit()
  assert.equal(writes(), 0)
  assert.equal(c.checkinDialogVisible.value, true)
  assert.ok(warnings.length > 0)
})

test('checkin sends the selected major or minor version type and has no standalone major action', async () => {
  const { c } = setup(); let request
  c.checkinForm.versionChangeType = 'MAJOR'
  c.checkinUpload.value = { uploadTicket: 'fresh-ticket', fileName: 'updated.docx' }
  c.checkinSourceState.value = 'ready'
  c.checkinControlledFile = async (_id, data) => { request = data; return { id: 2, versionNo: 'B/1' } }
  await c.submit()
  assert.equal(request.versionChangeType, 'MAJOR')
  assert.match(source, /data-testid="dcc-controlled-browser-checkin-version-type"/)
  assert.doesNotMatch(source, /dcc-controlled-browser-major-revision/)
  assert.doesNotMatch(source, /handleCreateMajorRevision/)
})

test('major and rejected checkins cannot reuse old content without a fresh ticket', async () => {
  for (const [versionChangeType, status] of [['MAJOR', 'ACTIVE'], ['MINOR', 'REJECTED'], ['MINOR', 'PENDING_APPLICANT_REWORK']]) {
    const { c, writes, warnings } = setup()
    c.checkinForm.versionChangeType = versionChangeType
    c.checkinTarget.value.status = status
    await c.submit()
    assert.equal(writes(), 0)
    assert.ok(warnings.some(message => /必须.*上传|重新上传/.test(message)))
  }
})
