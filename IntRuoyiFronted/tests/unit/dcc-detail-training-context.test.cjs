const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const moduleContext = { exports: {}, Error }
vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/workflow/workflow-actions.ts', 'utf8'),
  { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, moduleContext)
const trainingSession = moduleContext.exports.trainingUploadSession
function handlers(upload = async () => ({ uploadTicket: 'ticket', requestId: 'request' }), bind = async () => true) {
  const { descriptor } = parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8'))
  const source = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = new Set(['requireApplicantTrainingRecordContext', 'handleApplicantTrainingRecordChange', 'submitApplicantTrainingRecordDialog'])
  const declarations = source.statements.filter(statement => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(source))))
  const calls = { uploads: [], binds: [], cleaned: [], successes: [] }
  const env = {
    fileDetail: vue.ref({ id: 10, categoryId: 5, processInstanceId: 'round-1', status: 'PENDING_APPLICANT_TRAINING_RECORD' }),
    controlledFileId: vue.ref(10), canUploadApplicantTrainingRecord: vue.ref(true),
    applicantTrainingRecordUploadSequence: 0,
    applicantTrainingRecordUploadSessionId: vue.ref('dcc-upload-original'),
    applicantTrainingRecordFileList: vue.ref([]),
    applicantTrainingRecordDialog: vue.reactive({ visible: true, uploading: false, submitting: false, inlineError: '', fieldErrors: {}, file: undefined }),
    trainingUploadSession: trainingSession,
    generateUUID: () => '12345678-1234-1234-1234-123456789012',
    clearApplicantTrainingRecordFieldError: name => { delete env.applicantTrainingRecordDialog.fieldErrors[name] },
    buildDetailUploadPreviewContext: sessionId => ({ categoryId: env.fileDetail.value.categoryId, sessionId }),
    uploadControlledFilePreview: async (...args) => { calls.uploads.push(args); return upload(...args) },
    uploadControlledFileTrainingRecord: async (...args) => { calls.binds.push(args); return bind(...args) },
    cleanupControlledFileUploadTicket: async (...args) => { calls.cleaned.push(args) },
    resolveReadSideErrorMessage: error => String(error),
    message: { success: text => calls.successes.push(text), error() {} },
    closeApplicantTrainingRecordDialog: async () => { env.applicantTrainingRecordDialog.visible = false },
    reloadAll: async () => {}, Error
  }
  const code = declarations.map(statement => statement.getText(source)).join('\n')
    + '\nglobalThis.handlers={ upload:handleApplicantTrainingRecordChange, submit:submitApplicantTrainingRecordDialog };'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText, env)
  return { env, calls, ...env.handlers }
}
const selected = { raw: { name: '培训.pdf' } }
const ticket = sessionId => ({ sessionId, uploadTicket: 'ticket', requestId: 'request', fileName: '培训.pdf' })

test('detail training preupload sends the exact file and a real BPM-bound session', async () => {
  const state = handlers(async (_file, _purpose, context) => ticket(context.sessionId))
  await state.upload(selected, [selected])
  assert.equal(state.calls.uploads.length, 1)
  const context = state.calls.uploads[0][2]
  assert.equal(context.controlledFileId, 10)
  assert.match(context.sessionId, /^dcc-training:10:7:round-1:/)
  assert.equal(state.env.applicantTrainingRecordDialog.file.sessionId, context.sessionId)
})
test('a previous BPM upload response is cleaned and cannot become the new round ticket', async () => {
  let complete
  const state = handlers((_file, _purpose, context) => new Promise(resolve => { complete = () => resolve(ticket(context.sessionId)) }))
  const pending = state.upload(selected, [selected])
  state.env.fileDetail.value.processInstanceId = 'round-2'
  complete()
  await pending
  assert.equal(state.env.applicantTrainingRecordDialog.file, undefined)
  assert.equal(state.calls.cleaned.length, 1)
})
test('training without a real workflow identity cannot create a ticket', async () => {
  const state = handlers()
  state.env.fileDetail.value.processInstanceId = ''
  await state.upload(selected, [selected])
  assert.equal(state.calls.uploads.length, 0)
  assert.match(state.env.applicantTrainingRecordDialog.inlineError, /流程|轮次/)
})
test('a saved ticket from another round is rejected before binding', async () => {
  const state = handlers()
  const sessionId = trainingSession(10, 'round-1', 'nonce')
  state.env.applicantTrainingRecordUploadSessionId.value = sessionId
  state.env.applicantTrainingRecordDialog.file = ticket(sessionId)
  state.env.fileDetail.value.processInstanceId = 'round-2'
  await state.submit()
  assert.equal(state.calls.binds.length, 0)
  assert.equal(state.calls.successes.length, 0)
})
test('a false binding response cannot report progression to document control review', async () => {
  const state = handlers(undefined, async () => false)
  const sessionId = trainingSession(10, 'round-1', 'nonce')
  state.env.applicantTrainingRecordUploadSessionId.value = sessionId
  state.env.applicantTrainingRecordDialog.file = ticket(sessionId)
  await state.submit()
  assert.equal(state.calls.binds.length, 1)
  assert.equal(state.calls.successes.length, 0)
  assert.equal(state.env.applicantTrainingRecordDialog.visible, true)
  assert.ok(state.env.applicantTrainingRecordDialog.inlineError)
})
test('a confirmed training binding reports the next actual review stage', async () => {
  const state = handlers()
  const sessionId = trainingSession(10, 'round-1', 'nonce')
  state.env.applicantTrainingRecordUploadSessionId.value = sessionId
  state.env.applicantTrainingRecordDialog.file = ticket(sessionId)
  await state.submit()
  assert.equal(state.calls.successes.length, 1)
  assert.match(state.calls.successes[0], /文控审核/)
})
test('repeated training confirmation binds once and an old response cannot complete a new round', async () => {
  let complete
  const state = handlers(undefined, () => new Promise(resolve => { complete = resolve }))
  const sessionId = trainingSession(10, 'round-1', 'nonce')
  state.env.applicantTrainingRecordUploadSessionId.value = sessionId
  state.env.applicantTrainingRecordDialog.file = ticket(sessionId)
  const pending = state.submit()
  await state.submit()
  assert.equal(state.calls.binds.length, 1)
  state.env.fileDetail.value.processInstanceId = 'round-2'
  state.env.applicantTrainingRecordDialog.submitting = false
  complete(true)
  await pending
  assert.equal(state.calls.successes.length, 0)
  assert.equal(state.env.applicantTrainingRecordDialog.visible, true)
})
