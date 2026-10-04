const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return { promise, resolve } }
const flush = async () => { for (let n = 0; n < 25; n++) await Promise.resolve() }
function parent({ confirm = async () => true, readyGate, saveGate, failGate, cleanupGate, ownerRequired = true } = {}) {
  const raw = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
  const page = ts.createSourceFile('detail.ts', parse(raw).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = new Set(['submitActionDialog', 'refreshTaskActionReadiness', 'openActionDialog', 'closeActionDialog', 'readActionDialogRequest', 'actionDialogRequestIsCurrent'])
  const code = page.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.has(d.name.getText(page)))).map(s => s.getText(page)).join('\n')
  const sent = [], success = [], feedback = [], closed = [], env = { exports: {}, Error, JSON, String, Promise, structuredClone,
    actionDialogGeneration: 1, taskActionReadinessRequestSeq: 0,
    actionDialog: vue.reactive({ visible: true, submitting: false, mode: 'approve', inlineError: '', fieldErrors: {}, form: { password: 'unit-only-A', reason: 'A意见', fileOwnerUserId: '9007199254740993' } }),
    approvalTodoTask: vue.ref({ id: 'task-A', taskDefinitionKey: 'MATRIX_APPROVAL' }), fileDetail: vue.ref({ id: '10', fileNumber: 'F', versionNo: 'A/2' }), controlledFileId: vue.ref('10'), currentUserId: vue.ref('6'),
    isExternalReviewProcess: vue.ref(false), shouldCollectFourthNodeFiles: vue.ref(false), isFourthNodeApprovalTask: vue.ref(false), isSignoffTask: vue.ref(false), signoffReadyForReview: vue.ref(true),
    taskActionReadiness: vue.reactive({ ready: false, blockers: [], error: '', loading: false }), fourthNodeUpload: { selectedDistributionScopes: [] }, externalReviewAction: {},
    requiresFileOwnerSelection: vue.ref(ownerRequired), route: vue.reactive({ fullPath: 'route-A' }), approvalProcessInstanceId: vue.ref('bpm-A'),
    validateExternalReviewConclusion: () => true, applyTaskActionReadinessFieldErrors() {}, resetFourthNodeUploads() {}, cleanupFourthNodeUploadSessions: async () => { if (cleanupGate) await cleanupGate.promise; return true }, loadDocControlDirectoryTree: async () => {},
    getControlledFileTaskActionReadiness: async () => { if (readyGate) await readyGate.promise; return { ready: true, blockers: [], finalApproval: false } },
    message: { confirm, success: text => success.push(text), error: text => feedback.push(text), warning: text => feedback.push(text), info: text => feedback.push(text) },
    reloadAll: async () => closed.push('reload'), buildActionSuccessMessage: () => '原批准已签名成功', resolveReadSideErrorMessage: e => e.message,
    resolveDccApprovalSignatureErrorMessage: e => e.message, isControlledFileTaskPasswordInvalidError: () => false, DCC_APPROVAL_WRONG_PASSWORD_MESSAGE: 'wrong password'
  }
  env.resetTaskActionReadiness = () => { env.taskActionReadinessRequestSeq++; env.taskActionReadiness.ready = false }
  env.fileOwnerApprovalContext = { get value() { return JSON.stringify([env.route.fullPath, env.controlledFileId.value, env.approvalTodoTask.value.id, env.approvalProcessInstanceId.value, env.actionDialog.mode]) } }
  env.actionDialogContextKey = { get value() { return JSON.stringify([env.route.fullPath, env.controlledFileId.value, String(env.fileDetail.value.id), env.approvalTodoTask.value.id, env.approvalProcessInstanceId.value, env.actionDialog.mode, String(env.currentUserId.value)]) } }
  const model = {}, transport = async (id, payload) => { sent.push({ id, payload }); if (saveGate) await saveGate.promise; if (failGate) { await failGate.promise; throw Error('original signature rejected') } return { controlledFileId: id } }
  new Function('exports', 'require', ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/detail/approval-actions.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(model, id => {
    if (id.endsWith('/workflow')) return { approveControlledFileTask: transport, rejectControlledFileTask: transport, DccTaskActionError: Error, isControlledFileTaskPasswordInvalidError: () => false }
    if (id === '../shared/lifecycle') return { getDccControlledFileStageByKey: () => undefined }
    throw Error(id)
  })
  Object.assign(env, model)
  if (fs.existsSync('src/views/dcc/controlled-file/detail/approval-dialog-request.ts')) {
    new Function('exports', ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/detail/approval-dialog-request.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText)(env)
  }
  vm.runInNewContext(ts.transpileModule(code + '\nexports.api={submitActionDialog,openActionDialog,closeActionDialog,refreshTaskActionReadiness}', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  const change = async ({ sameTask = false, busy = false } = {}) => {
    await env.exports.api.closeActionDialog(true)
    if (!sameTask) { env.route.fullPath = 'route-B'; env.approvalProcessInstanceId.value = 'bpm-B'; env.approvalTodoTask.value = { id: 'task-B', taskDefinitionKey: 'MATRIX_APPROVAL' } }
    env.exports.api.openActionDialog('approve')
    env.actionDialog.form.password = 'unit-only-B'; env.actionDialog.form.reason = 'B意见'; env.actionDialog.form.fileOwnerUserId = '7'
    env.taskActionReadiness.ready = true; env.actionDialog.submitting = busy
  }
  return { env, sent, success, feedback, closed, change, ...env.exports.api }
}
test('original click cannot submit another task after readiness returns, even when the new dialog is ready', async () => {
  const gate = deferred(), p = parent({ readyGate: gate }), old = p.submitActionDialog()
  await p.change(); gate.resolve(); await old
  assert.equal(p.sent.length, 0); assert.equal(p.env.actionDialog.form.fileOwnerUserId, '7')
})
test('close and reopen same file/task during readiness invalidates ABA click without releasing new busy', async () => {
  const gate = deferred(), p = parent({ readyGate: gate }), old = p.submitActionDialog()
  await p.change({ sameTask: true, busy: true }); gate.resolve(); await old
  assert.equal(p.sent.length, 0); assert.equal(p.env.actionDialog.submitting, true)
})
test('ABA with identical file task and form still rejects old click by dialog generation and protects new busy', async () => {
  const gate = deferred(), p = parent({ readyGate: gate }), originalForm = { ...p.env.actionDialog.form }, old = p.submitActionDialog()
  await p.change({ sameTask: true, busy: true }); Object.assign(p.env.actionDialog.form, originalForm)
  gate.resolve(); await old; assert.equal(p.sent.length, 0); assert.equal(p.env.actionDialog.submitting, true)
})
test('late original signature success retains new dialog, owner and busy and gives original-record success guidance', async () => {
  const gate = deferred(), p = parent({ saveGate: gate }), old = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  await p.change({ busy: true }); const reloads = p.closed.length; gate.resolve(); await old
  assert.equal(p.env.actionDialog.visible, true); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.form.fileOwnerUserId, '7'); assert.equal(p.closed.length, reloads)
  assert.ok([...p.success, ...p.feedback].some(text => text.includes('原记录') && text.includes('10')))
})
test('late original signature error cannot overwrite a reopened same-task dialog or unlock its in-flight request', async () => {
  const gate = deferred(), p = parent({ failGate: gate }), old = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  await p.change({ sameTask: true, busy: true }); p.env.actionDialog.inlineError = 'B独立错误'; gate.resolve(); await old
  assert.equal(p.env.actionDialog.inlineError, 'B独立错误'); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.visible, true)
})
test('confirmation cancel retains input with zero writes; form edits before or during confirmation reject the clicked snapshot', async () => {
  const cancel = parent({ confirm: async () => { throw 'cancel' } }); await cancel.submitActionDialog(); assert.equal(cancel.sent.length, 0); assert.equal(cancel.env.actionDialog.form.fileOwnerUserId, '9007199254740993')
  const ready = deferred(), edit = parent({ readyGate: ready }), before = edit.submitActionDialog(); edit.env.actionDialog.form.reason = 'changed'; ready.resolve(); await before; assert.equal(edit.sent.length, 0)
  const confirm = deferred(), during = parent({ confirm: () => confirm.promise }), pending = during.submitActionDialog(); await flush(); during.env.actionDialog.form.fileOwnerUserId = '8'; confirm.resolve(true); await pending; assert.equal(during.sent.length, 0)
})
test('stable confirmation submits frozen exact owner/file/task and entered password once; irrelevant node has no owner field', async () => {
  for (const ownerRequired of [true, false]) { const p = parent({ ownerRequired }); await p.submitActionDialog(); assert.equal(p.sent.length, 1); assert.equal(p.sent[0].id, '10'); assert.equal(p.sent[0].payload.taskId, 'task-A'); assert.equal(p.sent[0].payload.password, 'unit-only-A'); assert.equal(p.sent[0].payload.fileOwnerUserId, ownerRequired ? '9007199254740993' : undefined) }
})
test('late cleanup from a cancel cannot clear a reopened identical task dialog', async () => {
  const gate = deferred(), p = parent({ cleanupGate: gate }), closing = p.closeActionDialog()
  p.openActionDialog('approve'); p.env.actionDialog.form.fileOwnerUserId = '8'; p.env.actionDialog.form.reason = 'new input'; p.env.actionDialog.submitting = true
  gate.resolve(); assert.equal(await closing, false)
  assert.equal(p.env.actionDialog.visible, true); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.form.fileOwnerUserId, '8')
})
test('accepted signature followed by detail refresh failure stays successful and tells original-record verification instead of retry', async () => {
  const p = parent(); p.env.reloadAll = async () => { throw Error('readback failed') }
  await p.submitActionDialog()
  assert.equal(p.sent.length, 1); assert.equal(p.env.actionDialog.visible, false)
  assert.ok(p.feedback.some(text => text.includes('签名已提交') && text.includes('刷新失败') && text.includes('原记录')))
  assert.equal(p.env.actionDialog.inlineError, '')
})
test('frozen upload scope and owner payload are not replaced by subsequent form mutation while transport is pending', async () => {
  const gate = deferred(), p = parent({ saveGate: gate })
  p.env.fourthNodeUpload.selectedDistributionScopes = [{ departmentId: 20, distributionMedium: 'PAPER' }]
  const signing = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  p.env.fourthNodeUpload.selectedDistributionScopes[0].departmentId = 99
  p.env.actionDialog.form.fileOwnerUserId = '7'
  assert.equal(p.sent[0].payload.selectedDistributionScopes[0].departmentId, 20)
  assert.equal(p.sent[0].payload.fileOwnerUserId, '9007199254740993')
  gate.resolve(); await signing
})
