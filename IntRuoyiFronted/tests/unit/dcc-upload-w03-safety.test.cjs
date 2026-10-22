const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const page = parse(fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/upload/index.vue'), 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('upload.ts', page, ts.ScriptTarget.Latest, true)
const declaration = name => {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === name))
  assert.ok(statement, `real page declaration ${name}`)
  return statement.getText(ast)
}
const evaluate = (code, state = {}) => {
  const c = { exports: {}, Error, Date, JSON, Promise, ...state }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return c
}
const helpers = evaluate(fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/upload/submitter.ts'), 'utf8'), {
  require: id => id.endsWith('approval-matrix') ? require('./dcc-upload-test-loader.cjs').loadUploadMatrix() : id.endsWith('signoff-departments') ? require('./dcc-upload-test-loader.cjs').loadUploadDepartments() : ({ validateAttributes: value => value })
}).exports
const ref = value => ({ value })
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }

test('date mode rejects past and non-calendar dates and accepts today or future', () => {
  assert.equal(typeof helpers.validateUploadEffectiveDate, 'function')
  for (const value of ['', '2026-10-10', '2026-02-29', '2026-13-01', '2026-1-01', '2026-10-11T00:00:00']) {
    assert.equal(helpers.validateUploadEffectiveDate(value, '2026-10-11').valid, false, value)
  }
  for (const value of ['2026-10-11', '2026-12-31', '2028-02-29']) {
    assert.equal(helpers.validateUploadEffectiveDate(value, '2026-10-11').valid, true, value)
  }
  assert.equal(helpers.validateUploadEffectiveDate('2026-10-11', '2026-10-12').valid, false, 'confirmation crosses midnight')
})

const draft = { processType: 'CONTROLLED_FILE', changeType: 'NEW', categoryId: 2, projectFolderId: '5', projectFolderChangeReason: '登记', fileName: 'a.pdf', fileNumber: 'SOP-1', versionNo: 'A/1', effectiveMode: 'PRESET_DATE', effectiveDate: '2099-10-11', selectedDistributionDepartmentIds: ['8'], relatedControlledFileIds: [], projectAttributes: {} }
const ticket = { sessionId: 'S1', uploadTicket: 'UT1', fileName: 'a.pdf' }
draft.approvalMatrixSelection = require('./dcc-upload-test-loader.cjs').matrixSelection()
test('source preview requires a real matching response and never guesses its kind', () => {
  assert.equal(typeof helpers.validateUploadPreviewResponse, 'function')
  for (const bad of [ticket, { ...ticket, previewKind: 'UNKNOWN' }, { ...ticket, previewKind: 'PDF', fileName: 'other.pdf' }])
    assert.throws(() => helpers.validateUploadPreviewResponse(bad, 'a.pdf', true))
  assert.doesNotThrow(() => helpers.validateUploadPreviewResponse({ ...ticket, previewKind: 'OFFICE', previewUnavailableReason: '正式预览不可用' }, 'a.pdf', true))
  assert.doesNotThrow(() => helpers.validateUploadPreviewResponse(ticket, 'a.pdf', false))
})
test('submit payload refuses missing, expired or conflicting upload identities before submit', async () => {
  for (const bad of [{ ...ticket, uploadTicket: '' }, { ...ticket, sessionId: '' }, { ...ticket, fileName: '' }, { ...ticket, expireTime: 1 }]) {
    assert.throws(() => helpers.buildSubmitPayload(draft, bad), /上传|票据|过期/)
  }
  assert.throws(() => helpers.buildSubmitPayload(draft, ticket, { ...ticket, uploadTicket: 'UT2', sessionId: 'OTHER' }), /会话/)
  assert.throws(() => helpers.buildSubmitPayload(draft, ticket, undefined, [{ ...ticket, sessionId: 'ATT' }]), /重复/)
  let calls = 0
  const service = helpers.createUploadSubmitterService({ submit: async () => { calls++ } })
  await assert.rejects(service.submit(draft, { ...ticket, uploadTicket: '' }))
  assert.equal(calls, 0)
  const payload = helpers.buildSubmitPayload(draft, ticket, { ...ticket, uploadTicket: 'UT2', fileName: 'drawing.pdf' }, [{ ...ticket, sessionId: 'ATT', uploadTicket: 'UT3', fileName: 'evidence.pdf' }])
  assert.equal(payload.idempotencyKey, 'S1')
  assert.equal(payload.attachmentUploadTickets[0].sessionId, 'ATT')
})

function typeState() {
  const calls = []
  const c = { formData: { fileTypeTaxonomyId: 'NEW', fileNumber: 'KEEP', selectedSignoffDepartmentIds: ['8'] },
    fileTypeTaxonomyPathMap: ref(new Map([['NEW',{approvalSourceMode:'PUBLIC_MATRIX'}],['NEWER',{approvalSourceMode:'PUBLIC_MATRIX'}]])),
    acceptedFileTypeTaxonomyId: ref('OLD'), submitLoading: ref(false), projectAttributesLoading: ref(false), fileTypeChangeRequestSequence: 0,
    hasUploadApplicationInput: () => true, message: { confirm: async () => { calls.push('confirm'); throw 'cancel' }, error: value => calls.push(value) },
    cleanupCurrentUploadSession: async () => { calls.push('cleanup'); return true },
    resetUploadNameContext: () => calls.push('reset-name'), resetCategorySelectionForFileTypeTaxonomyChange: () => calls.push('reset-type'),
    fileTypeCategoryRequestSequence: 0, formRef: ref({ validateField: async () => {} }), syncAutoCategoryFromSelectedFileTypeTaxonomy: async () => calls.push('load') }
  return { c: evaluate(declaration('handleFileTypeTaxonomyChange') + '\nexports.run=handleFileTypeTaxonomyChange', c), calls }
}
test('cancelled type change preserves type, tickets and manually entered fields without cleanup', async () => {
  const { c, calls } = typeState()
  await c.exports.run()
  assert.deepEqual(calls, ['confirm'])
  assert.equal(c.formData.fileTypeTaxonomyId, 'OLD')
  assert.equal(c.formData.fileNumber, 'KEEP')
  assert.deepEqual(c.formData.selectedSignoffDepartmentIds, ['8'])
})
test('older type confirmation cannot accept a newer unconfirmed selection', async () => {
  const { c, calls } = typeState()
  const wait = deferred()
  c.message.confirm = async () => wait.promise
  const pending = c.exports.run()
  c.formData.fileTypeTaxonomyId = 'NEWER'
  wait.resolve()
  await pending
  assert.equal(c.acceptedFileTypeTaxonomyId.value, 'OLD')
  assert.deepEqual(calls, [])
})
test('cleanup uses the captured old tickets across asynchronous context changes', async () => {
  const wait = deferred(), cleaned = []
  const c = evaluate(declaration('cleanupCurrentUploadSession') + '\nexports.run=cleanupCurrentUploadSession', {
    uploadSubmitted: ref(false), hasTemporaryUploadState: () => true, resolveCurrentUploadCleanupRequestId: () => 'REQ',
    previewUpload: ref({ ...ticket, requestId: 'REQ' }), drawingPdfUpload: ref(undefined), attachmentUploads: ref([{ sessionId: 'OLD-ATT', uploadTicket: 'OLD-T' }]),
    cleanupControlledFileUploadSession: async () => wait.promise,
    cleanupControlledFileUploadTicket: async (session, ticket) => { cleaned.push([session, ticket]); return { cleanedCount: 1 } },
    message: { success() {}, error() {} }, resolveUploadErrorMessage: error => error.message
  })
  const pending = c.exports.run()
  c.attachmentUploads.value = [{ sessionId: 'NEW-ATT', uploadTicket: 'NEW-T' }]
  wait.resolve({ cleanedCount: 1 })
  assert.equal(await pending, true)
  assert.deepEqual(cleaned, [['OLD-ATT', 'OLD-T']])
})
test('stale body upload does not proceed after its asynchronous cleanup finishes', async () => {
  const wait = deferred(), uploaded = []
  const c = evaluate(declaration('handleFileChange') + '\nexports.run=handleFileChange', {
    sourceUploadRequestSeq: 0, validateSingleUploadFileSelection: () => ({ valid: true }),
    cleanupCurrentUploadSession: () => wait.promise, uploadRef: ref({ clearFiles() {} }),
    resetSelectedPreview() {}, resetDrawingPdfUpload() {}, resetAttachmentUploads() {},
    clearSubmitFieldErrors() {}, submitFieldErrors: {}, uploadPreviewError: ref(''), fileList: ref([]), previewUpload: ref(), previewFileBlob: ref(), uploadPreviewLoading: ref(false),
    formData: { categoryId: 2, fileName: 'old.pdf' }, isExternalReview: ref(false), projectAttributesLoading: ref(false), uploadSubmitted: ref(false), uploadSessionId: 'OLD',
    buildUploadPreviewContext: () => ({ categoryId: 2, sessionId: 'OLD' }), captureUploadSelectionContext: () => 'CONTEXT',
    createControlledFileUploadSessionId: () => 'NEW', uploadSessionBinding: ref(), submitLoading: ref(false),
    uploadSubmitterService: { uploadPreview: async () => { uploaded.push('upload'); return ticket } },
    message: { error() {}, warning() {} }, cleanupStaleUploadResponse: async () => {}, resolveUploadPreviewErrorMessage: error => error.message
  })
  const pending = c.exports.run({ uid: 1, name: 'a.pdf', raw: { name: 'a.pdf' } }, [{ uid: 1, name: 'a.pdf' }])
  c.sourceUploadRequestSeq++
  wait.resolve(true)
  await pending
  assert.deepEqual(uploaded, [])
  assert.equal(c.formData.fileName, 'old.pdf')
})
test('resetting attachments releases busy attempts while invalidating old generations', () => {
  const c = evaluate(declaration('resetAttachmentUploads') + '\nexports.run=resetAttachmentUploads', {
    attachmentUploadAttempts: ref([{ uid: 1, status: 'UPLOADING' }]), attachmentUploads: ref([ticket]), attachmentFileList: ref([1]),
    nextAttachmentUploadGeneration() {}, syncAttachmentUploadLoading() {}
  })
  c.exports.run()
  assert.equal(c.attachmentUploadAttempts.value.length, 0)
})

test('confirmation enumerates the frozen drawing, attachments and remark actually submitted', () => {
  const facts = { projectName: '甲', folderName: '目录', fileTypeName: 'SOP', relatedFiles: [], signoffDepartments: [{ id: '8', name: '质量' }], distributionDepartments: [{ id: '8', name: '质量' }], approvers: [{ id: '9', name: '批准' }], approvalRule: '全部批准', compiler:{id:'71',name:'编制甲',deptId:'8',deptName:'质量'},supervisor:{id:'72',name:'主管乙',deptId:'10',deptName:'研发'}, drawingPdf: { fileName: 'drawing.pdf' }, attachments: [{ fileName: 'evidence.pdf' }] }
  const completeDraft = { ...draft, remark: '备注甲', projectAttributes: { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' } }
  const summary = helpers.buildUploadConfirmationSummary(completeDraft, ticket, facts).map(row => `${row.label}:${row.value}`).join('\n')
  for (const expected of ['drawing.pdf', 'evidence.pdf', '备注甲']) assert.ok(summary.includes(expected), expected)
})
