const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const versionDisplay = {}
new Function('exports', ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/shared/version-display.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText)(versionDisplay)
const deferred = () => { let resolve; const promise = new Promise(r => resolve = r); return { promise, resolve } }
const flush = async () => { for (let n = 0; n < 25; n++) await Promise.resolve() }
function parent({ confirm = async () => true, readyGate, saveGate, failGate, cleanupGate, viewer } = {}) {
  const raw = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
  const page = ts.createSourceFile('detail.ts', parse(raw).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const names = new Set(['submitActionDialog', 'refreshTaskActionReadiness', 'openActionDialog', 'closeActionDialog', 'readActionDialogRequest', 'actionDialogRequestIsCurrent', 'returnToCompletedTaskInbox'])
  const code = page.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.has(d.name.getText(page)))).map(s => s.getText(page)).join('\n')
  const sent = [], success = [], feedback = [], closed = [], navigated = [], env = { exports: {}, Error, JSON, String, Promise, structuredClone, ...versionDisplay,
    actionDialogGeneration: 1, taskActionReadinessRequestSeq: 0, taskListRevision: 0, approvalBodyViewerRef: { value: viewer },
    detailLoadSequence: 1, isCurrentDetailLoad: () => true, ensureCurrentDetailContext: () => true, isDelayedEffectiveTask: vue.ref(false),
    useApprovalTodoBadgeStoreWithOut: () => ({ invalidateTaskLists: () => { env.taskListRevision++ } }),
    actionDialog: vue.reactive({ visible: true, submitting: false, mode: 'approve', inlineError: '', fieldErrors: {}, form: { password: 'unit-only-A', reason: 'A意见' } }),
    approvalTodoTask: vue.ref({ id: 'task-A', taskDefinitionKey: 'MATRIX_APPROVAL' }), fileDetail: vue.ref({ id: '10', fileNumber: 'F', versionNo: 'A/2' }), controlledFileId: vue.ref('10'), currentUserId: vue.ref('6'),
    isExternalReviewProcess: vue.ref(false), shouldCollectFourthNodeFiles: vue.ref(false), isFourthNodeApprovalTask: vue.ref(false), isSignoffTask: vue.ref(false), signoffReadyForReview: vue.ref(true),
    taskActionReadiness: vue.reactive({ ready: false, blockers: [], error: '', loading: false }), fourthNodeUpload: { selectedDistributionScopes: [] }, externalReviewAction: {},
    route: vue.reactive({ fullPath: 'route-A', query: {} }), approvalProcessInstanceId: vue.ref('bpm-A'), router: { push: async target => navigated.push(target) },
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
    env.actionDialog.form.password = 'unit-only-B'; env.actionDialog.form.reason = 'B意见'
    env.taskActionReadiness.ready = true; env.actionDialog.submitting = busy
  }
  return { env, sent, success, feedback, closed, navigated, change, ...env.exports.api }
}
test('signature waits for late reading start and its confirmed stop',async()=>{
  const template=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.template.content
  assert.match(template.split('data-testid="dcc-approval-upload-file-preview"')[1].split('</el-col>')[0],/ref="approvalBodyViewerRef"/,'signature must control the approval-pane viewer')
  const module = {}
  new Function('exports', ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/view/reading-lifecycle.ts','utf8'), {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(module)
  const start=deferred(), stop=deferred(), events=[]
  const lifecycle=module.createPreviewReadingLifecycle({api:{start:async()=>{events.push('start');await start.promise;return {sessionId:'old',heartbeatIntervalSeconds:10}},stop:async()=>{events.push('stop');await stop.promise},heartbeat:async()=>{}},visible:()=>true,newClientId:()=> 'client',schedule:()=>()=>{},onError:error=>{throw error}})
  const starting=lifecycle.resume()
  const p=parent({viewer:{pauseReading:()=>lifecycle.pause(),resumeReading:()=>lifecycle.resume()}})
  const signing=p.submitActionDialog();await flush()
  assert.equal(p.sent.length,0,'signature must wait while the old reading start is pending')
  start.resolve();await flush();assert.deepEqual(events,['start','stop']);assert.equal(p.sent.length,0,'signature must wait for stop acknowledgement')
  stop.resolve();await starting;await signing;assert.equal(p.sent.length,1)
})
test('completed approval-center task returns to inbox without rereading expired task body',async()=>{
  const p=parent({});p.env.route.query={handling:'approval',from:'approval-center'};let reloads=0;p.env.reloadAll=async()=>{reloads++;throw Error('expired task body')};await p.submitActionDialog();assert.equal(p.sent.length,1);assert.equal(reloads,0);assert.equal(p.env.taskListRevision,1);assert.equal(p.navigated[0].path,'/approval-center/todo')
})
test('explicit signature failure resumes current reading but stop failure never submits',async()=>{
  let resumed=0,paused=0
  const failure=deferred(),p=parent({failGate:failure,viewer:{pauseReading:async()=>{paused++},resumeReading:async()=>{resumed++}}})
  const signing=p.submitActionDialog();await flush();failure.resolve();await signing
  assert.equal(paused,1);assert.equal(p.sent.length,1);assert.equal(resumed,1)
  const q=parent({viewer:{pauseReading:async()=>{throw Error('reading stop unconfirmed')},resumeReading:async()=>{resumed++}}})
  await q.submitActionDialog();assert.equal(q.sent.length,0);assert.equal(resumed,1);assert.match(q.env.actionDialog.inlineError,/reading stop unconfirmed/)
})
test('compiler is read only and approval never offers a compiler picker',()=>{
  const template=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.template.content
  assert.doesNotMatch(template,/<ApprovalFileOwnerPicker\b/)
  assert.match(template,/label="文件编制人"[^\n]+fileOwnerNicknameSnapshot/)
  const p=parent();p.env.fileDetail.value.requesterId='9007199254740901';p.openActionDialog('approve')
  assert.equal(p.env.actionDialog.form.password,'');assert.equal(p.env.actionDialog.form.reason,'')
})
test('original click cannot submit another task after readiness returns, even when the new dialog is ready', async () => {
  const gate = deferred(), p = parent({ readyGate: gate }), old = p.submitActionDialog()
  await p.change(); gate.resolve(); await old
  assert.equal(p.sent.length, 0); assert.equal(p.env.actionDialog.form.reason, 'B意见')
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
test('late original signature success retains new dialog input and busy and gives original-record success guidance', async () => {
  const gate = deferred(), p = parent({ saveGate: gate }), old = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  await p.change({ busy: true }); const reloads = p.closed.length; gate.resolve(); await old
  assert.equal(p.env.actionDialog.visible, true); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.form.reason, 'B意见'); assert.equal(p.closed.length, reloads)
  assert.ok([...p.success, ...p.feedback].some(text => text.includes('原记录') && text.includes('10')))
})
test('late original signature error cannot overwrite a reopened same-task dialog or unlock its in-flight request', async () => {
  const gate = deferred(), p = parent({ failGate: gate }), old = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  await p.change({ sameTask: true, busy: true }); p.env.actionDialog.inlineError = 'B独立错误'; gate.resolve(); await old
  assert.equal(p.env.actionDialog.inlineError, 'B独立错误'); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.visible, true)
})
test('password and reason edits during readiness reject the clicked snapshot and retain current input', async () => {
  for (const [field,value] of [['password','unit-only-edited'],['reason','changed opinion']]) {
    const ready=deferred(),p=parent({readyGate:ready}),pending=p.submitActionDialog()
    p.env.actionDialog.form[field]=value;ready.resolve();await pending
    assert.equal(p.sent.length,0);assert.equal(p.env.actionDialog.form[field],value)
    assert.match(p.env.actionDialog.inlineError,/已变化/)
  }
})
test('stable signature submits exact file task password and reason once without an owner payload', async () => {
  const p=parent();await p.submitActionDialog();assert.equal(p.sent.length,1);assert.equal(p.sent[0].id,'10');assert.equal(p.sent[0].payload.taskId,'task-A');assert.equal(p.sent[0].payload.password,'unit-only-A');assert.equal(p.sent[0].payload.reason,'A意见');assert.equal(Object.hasOwn(p.sent[0].payload,'fileOwnerUserId'),false)
})
test('late cleanup from a cancel cannot clear a reopened identical task dialog', async () => {
  const gate = deferred(), p = parent({ cleanupGate: gate }), closing = p.closeActionDialog()
  p.openActionDialog('approve'); p.env.actionDialog.form.reason = 'new input'; p.env.actionDialog.submitting = true
  gate.resolve(); assert.equal(await closing, false)
  assert.equal(p.env.actionDialog.visible, true); assert.equal(p.env.actionDialog.submitting, true); assert.equal(p.env.actionDialog.form.reason, 'new input')
})
test('accepted signature followed by detail refresh failure stays successful and tells original-record verification instead of retry', async () => {
  const p = parent(); p.env.reloadAll = async () => { throw Error('readback failed') }
  await p.submitActionDialog()
  assert.equal(p.sent.length, 1); assert.equal(p.env.actionDialog.visible, false)
  assert.ok(p.feedback.some(text => text.includes('签名已提交') && text.includes('刷新失败') && text.includes('原记录')))
  assert.equal(p.env.actionDialog.inlineError, '')
})
test('frozen upload scope password and reason are not replaced while transport is pending', async () => {
  const gate = deferred(), p = parent({ saveGate: gate })
  p.env.fourthNodeUpload.selectedDistributionScopes = [{ departmentId: 20, distributionMedium: 'PAPER' }]
  const signing = p.submitActionDialog(); await flush(); assert.equal(p.sent.length, 1)
  p.env.fourthNodeUpload.selectedDistributionScopes[0].departmentId = 99
  p.env.actionDialog.form.password='unit-only-new';p.env.actionDialog.form.reason='new opinion'
  assert.equal(p.sent[0].payload.selectedDistributionScopes[0].departmentId, 20)
  assert.equal(p.sent[0].payload.password,'unit-only-A');assert.equal(p.sent[0].payload.reason,'A意见');assert.equal(Object.hasOwn(p.sent[0].payload,'fileOwnerUserId'),false)
  gate.resolve(); await signing
})

test('approval uses its signature dialog without an owner confirmation and preserves exact version and task', async () => {
  const prompts = [], p = parent({ confirm: async value => { prompts.push(value); return true } })
  p.env.fileDetail.value.versionNo = 'A/1-2'
  await p.submitActionDialog()
  assert.equal(prompts.length,0)
  assert.equal(p.sent.length, 1); assert.equal(p.sent[0].id, '10'); assert.equal(p.sent[0].payload.taskId, 'task-A')
  assert.equal(p.env.fileDetail.value.versionNo, 'A/1-2')
})

test('reject validates the required reason before readiness and transport', async () => {
  const p=parent(); p.env.actionDialog.mode='reject'; p.env.actionDialog.form.reason='  '
  let checks=0; p.env.getControlledFileTaskActionReadiness=async()=>{checks++;throw Error('approve-only readiness')}
  await p.submitActionDialog()
  assert.equal(checks,0); assert.equal(p.sent.length,0); assert.equal(p.env.actionDialog.fieldErrors.reason,'请输入驳回原因')
})
test('department assignment rejection skips approval-only readiness and still sends signed reject', async () => {
  const p=parent(); p.env.actionDialog.mode='reject'; p.env.isSignoffTask.value=true; p.env.signoffReadyForReview.value=false
  let checks=0; p.env.getControlledFileTaskActionReadiness=async()=>{checks++;throw Error('approve-only readiness')}
  await p.submitActionDialog()
  assert.equal(checks,0); assert.equal(p.sent.length,1); assert.equal(p.sent[0].payload.reason,'A意见'); assert.equal(p.sent[0].payload.password,'unit-only-A')
})
test('reopening a signature dialog clears the credential and locks autofill until focus', async () => {
  const p=parent(); p.env.actionDialog.passwordFocused=true; p.openActionDialog('reject')
  assert.equal(p.env.actionDialog.form.password,''); assert.equal(p.env.actionDialog.passwordFocused,false)
  p.env.actionDialog.passwordFocused=true; await p.closeActionDialog(true)
  assert.equal(p.env.actionDialog.passwordFocused,false)
})
