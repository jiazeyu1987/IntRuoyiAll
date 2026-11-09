const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const { AxiosError } = require('axios'), { parse } = require('vue/compiler-sfc')
const read = path => fs.readFileSync(path, 'utf8')
const ast = source => ts.createSourceFile('actual.ts', source, ts.ScriptTarget.Latest, true)
const declaration = (source, name) => source.statements.find(node =>
  node.declarationList?.declarations.some(item => item.name.text === name)).getText(source)
const execute = (source, scope) => vm.runInNewContext(ts.transpileModule(source, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText, scope)

function httpFailureHandler(current = true) {
  const source = ast(read('src/config/axios/service.ts')), messages = []
  const call = source.statements.find(node => ts.isExpressionStatement(node) &&
    ts.isCallExpression(node.expression) && node.expression.expression.getText(source) === 'service.interceptors.response.use').expression
  const scope = { exports: {}, Promise, console: { log() {}, error() {} },
    assertRequestContextCurrent: () => { if (!current) throw new Error('stale context') },
    useI18n: () => ({ t: key => key }), ElMessage: { error: value => messages.push(value) } }
  execute('const reject = ' + call.arguments[1].getText(source) + '\nexports.reject=reject', scope)
  return { reject: scope.exports.reject, messages }
}

test('HTTP business denial reaches both toast and dialog while preserving Axios response and code', async () => {
  for (const msg of ['当前状态不允许上传培训记录', 'OnlyOffice conversion timeout']) {
    const error = new AxiosError('Request failed with status code 400', 'ERR_BAD_REQUEST', {}, null,
      { status: 400, data: { code: 1080000111, msg }, headers: {}, config: {} })
    const handler = httpFailureHandler()
    await assert.rejects(handler.reject(error), received => received === error && received.message === msg &&
      received.response.data.code === 1080000111 && received.response.status === 400)
    assert.deepEqual(handler.messages, [msg])
  }
})

test('transport failures stay errors; stale contexts and suppressed toasts remain guarded', async () => {
  for (const body of [undefined, '<html>proxy failure</html>', { msg: '' }, { msg: ' ' }, { msg: 4 }]) {
    const error = new AxiosError('Request failed with status code 400', 'ERR_BAD_REQUEST',
      { ignoreErrorMessage: true }, null, { status: 400, data: body })
    const handler = httpFailureHandler()
    await assert.rejects(handler.reject(error), received => received === error && received.message.includes('status code 400'))
    assert.equal(handler.messages.length, 0)
  }
  const handler = httpFailureHandler(false), error = new AxiosError('transport failure')
  assert.throws(() => handler.reject(error), /stale context/)
  assert.equal(handler.messages.length, 0)
})

test('training submission waits through the synchronous conversion and retains exact file/session/ticket', async () => {
  const source = ast(read('src/api/dcc/controlledFile/workflow.ts')), calls = []
  const scope = { exports: {}, assertRequiredString() {}, assertRequiredTicketRequest() {},
    request: { post: async options => { calls.push(options); return true } } }
  execute(declaration(source, 'uploadControlledFileTrainingRecord') + '\nexports.upload=uploadControlledFileTrainingRecord', scope)
  const data = { sessionId: 'dcc-training:exact-file:actual-round:unique', trainingRecordUploadTicket: 'formal-ticket' }
  assert.equal(await scope.exports.upload('9007199254740993', data), true)
  assert.equal(calls[0].url, '/dcc/controlled-files/9007199254740993/training-record')
  assert.equal(calls[0].data, data)
  assert.ok(calls[0].timeout > 2 * 300000, 'conversion and download each allow 300 seconds')
})

test('training dialog blocks repeated submission until the real receipt arrives and never claims success on denial', async () => {
  const source = ast(parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor.scriptSetup.content)
  let complete, calls = 0, successes = 0, reloads = 0
  const dialog = { visible: true, submitting: false, uploading: false, file: { sessionId: 'actual-session', uploadTicket: 'ticket' } }
  const scope = { exports: {}, Error, applicantTrainingRecordDialog: dialog,
    fileDetail: { value: { id: 'exact-file', processInstanceId: 'actual-round' } },
    applicantTrainingRecordUploadSessionId: { value: 'actual-session' },
    requireApplicantTrainingRecordContext: () => ({ fileId: 'exact-file', processInstanceId: 'actual-round', sessionId: 'actual-session' }),
    uploadControlledFileTrainingRecord: () => { calls++; return new Promise(resolve => { complete = resolve }) },
    resolveReadSideErrorMessage: error => error.message,
    message: { success: () => { successes++ } }, closeApplicantTrainingRecordDialog: async () => { dialog.visible = false },
    reloadAll: async () => { reloads++ } }
  execute(declaration(source, 'submitApplicantTrainingRecordDialog') + '\nexports.submit=submitApplicantTrainingRecordDialog', scope)
  const pending = scope.exports.submit()
  await scope.exports.submit()
  assert.equal(dialog.submitting, true); assert.equal(calls, 1); assert.equal(successes, 0)
  complete(true); await pending
  assert.equal(successes, 1); assert.equal(reloads, 1); assert.equal(dialog.visible, false)
  Object.assign(dialog, { visible: true, submitting: false })
  scope.uploadControlledFileTrainingRecord = async () => { throw new Error('当前状态不允许上传培训记录') }
  await scope.exports.submit()
  assert.equal(dialog.inlineError, '当前状态不允许上传培训记录')
  assert.equal(dialog.submitting, false); assert.equal(dialog.visible, true)
  assert.equal(successes, 1); assert.equal(reloads, 1)
})
