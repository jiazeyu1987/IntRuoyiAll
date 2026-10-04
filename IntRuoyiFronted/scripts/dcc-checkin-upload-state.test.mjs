import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url)), 'utf8')
const extract = name => {
  const start = source.indexOf(`const ${name} =`)
  const lfEnd = source.indexOf('\n}\n', start)
  const crlfEnd = source.indexOf('\r\n}\r\n', start)
  const candidates = [lfEnd, crlfEnd].filter(value => value >= 0)
  const end = Math.min(...candidates)
  const endLength = end === crlfEnd ? 4 : 2
  assert.ok(start > 0 && end > start, name)
  return source.slice(start, end + endLength)
}
const setup = () => {
  const warnings = []; let writes = 0
  const c = {
    checkinTarget: { value: { id: 1, masterId: 20, checkedOutBy: 99, versionNo: 'A/1', remark: 'old' } }, checkinUpload: { value: undefined },
    checkinUploadContext: {value:undefined},
    checkinUploadSessionId: { value: 'session-1' }, checkinSourceState: { value: 'idle' },
    checkinUploadLoading: { value: false }, checkinDrawingPdfLoading: { value: false },
    checkinCleanupLoading: { value: false },
    checkinDrawingPdfUpload: { value: undefined }, checkinSubmitting: { value: false },
    checkinForm: { versionChangeType: 'MINOR', remark: 'changed', changeDescription: 'replace content' },
    checkoutLoadingId: { value: undefined }, checkinDialogVisible: { value: true },
    findBrowserRowForVersion: () => ({ categoryId: 10, masterId: 20 }), clearCheckinDrawingPdf: () => {},
    isCheckedOutByCurrentUser: file => file.checkedOutBy === 99, checkinDialogGeneration: 0,
    userStore: { getUser: { id: 99 } }, route: { fullPath: '/dcc/controlled-file/browser' }, browserMode: { value: 'storage' },
    buildBrowserRouteStateKey: () => 'filter', getBrowserCacheContext: () => 'tenant-user',
    list: { value: [] }, total: { value: 0 }, checkinRefreshPending: { value: undefined },
    isValidBrowserOptionId: id => !!id, validateDrawingPdfUpload: () => ({ valid: true }),
    isDrawingSourceFile: () => false, getList: async () => {}, mergeCheckinResult: () => {},
    checkinControlledFile: async () => { writes++; return { id: 2, masterId: 20, versionNo: 'A/1-1', status: 'WORKING', checkedOut: false, checkedOutBy: null } },
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

test('checkin fixes MINOR and formal revision is a separate selected-working action', async () => {
  const { c } = setup(); let request
  c.checkinForm.versionChangeType = 'MAJOR'
  c.checkinUpload.value = { uploadTicket: 'fresh-ticket', sessionId: 'scoped-session', fileName: 'updated.docx' }
  c.checkinUploadContext.value={fileId:'1',clientSessionId:'session-1',scopedSessionId:'scoped-session',ticket:'fresh-ticket'}
  c.checkinSourceState.value = 'ready'
  c.checkinControlledFile = async (_id, data) => { request = data; return { id: 2, masterId: 20, versionNo: 'A/1-1', status: 'WORKING', checkedOut: false } }
  await c.submit()
  assert.equal(request.versionChangeType, 'MINOR')
  assert.match(source, /data-testid="dcc-controlled-browser-checkin-working-only"/)
  assert.doesNotMatch(source, /dcc-controlled-browser-major-revision/)
  assert.doesNotMatch(source, /handleCreateMajorRevision/)
})

test('rejected checkins cannot reuse old content without a fresh ticket', async () => {
  for (const [versionChangeType, status] of [['MINOR', 'REJECTED'], ['MINOR', 'PENDING_APPLICANT_REWORK']]) {
    const { c, writes, warnings } = setup()
    c.checkinForm.versionChangeType = versionChangeType
    c.checkinTarget.value.status = status
    await c.submit()
    assert.equal(writes(), 0)
    assert.ok(warnings.some(message => /必须.*上传|重新上传/.test(message)))
  }
})
