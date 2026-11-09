const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const bodyPath = 'src/api/dcc/controlledFile/bodyReadContext.ts'
function load(file) {
  const host = { exports: {}, TextEncoder, TextDecoder, btoa, atob }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, host)
  return host.exports
}
const body = load(bodyPath)
const identity = load('src/views/bpm/processInstance/detail/dcc-business-identity.ts')
const rounds = load('src/views/dcc/controlled-file/detail/application-round-context.ts')
const progress = load('src/views/dcc/controlled-file/detail/native-approval-progress.ts')
const source = parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('detail.ts', source, ts.ScriptTarget.Latest, true)
const names = new Set(['loadApprovalDetail', 'loadData', 'toExactUserId', 'findCurrentUserTodoTask',
  'resolveDetailBodyReadContext', 'currentApprovalApplicationType'])
const code = ast.statements.filter(s => ts.isVariableStatement(s)
  && s.declarationList.declarations.some(d => names.has(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
const FILE = '2054545668044084467', BPM = 'obsolete-round', TASK = 'obsolete-supervisor-task'
const plain = value => JSON.parse(JSON.stringify(value))
function fixture({ detailAllowed = true, denied = false } = {}) {
  const calls = [], selection = { bpmRound: BPM, taskId: TASK, lockRound: true }
  const task = { id: TASK, processInstanceId: BPM, assignee: '910530', status: 1, taskDefinitionKey: 'SUPERVISOR_REVIEW' }
  const instance = { id: BPM, businessKey: 'FORM_ACTION:FCI-1-test', formVariables: {
    systemCode: 'DCC', objectType: 'CONTROLLED_FILE', actionCode: 'OBSOLETE', objectId: FILE, objectVersion: 'A/2'
  } }
  const detail = { processInstance: instance, processDefinition: { key: 'dcc-controlled-file-obsolete' }, todoTask: task }
  const file = { id: FILE, versionNo: 'A/2', status: 'ACTIVE', changeType: 'REVISION', processInstanceId: 'revision-round' }
  const env = { exports: {}, computed: vue.computed, ...body, ...identity, ...rounds, ...progress,
    detailLoadSequence: 1, controlledFileId: vue.ref(FILE), fileDetail: vue.ref(file),
    currentUserId: vue.ref('910530'), approvalProcessInstanceId: vue.ref(BPM),
    route: { fullPath: '/exact-task', query: { processInstanceId: BPM, taskId: TASK } },
    detailReadSelectionState: vue.ref({ selection }), resolveDetailReadSelection: () => selection,
    applicationRoundContextKey: vue.ref('exact-key'), isCurrentDetailLoad: () => true,
    applicationApprovalRead: vue.ref(), approvalProgressScope: vue.ref(), approvalProgressError: vue.ref(''),
    approvalProgressNotice: vue.ref(''), approvalTodoTask: vue.ref(null), approvalTaskList: vue.ref([]),
    approvalLoading: vue.ref(false), stageProgressList: vue.ref([]), viewerMode: vue.ref(false),
    isBrowserTraceabilityPage: vue.ref(false), canUploadApplicantTrainingRecord: vue.ref(false),
    checkPermi: permissions => detailAllowed || !permissions.includes('bpm:process-instance:query'),
    DCC_BPM_TASK_STATUS: { RUNNING: 1, APPROVING: 7 }, resolveReadSideErrorMessage: e => e.message,
    TaskApi: { getTaskListByProcessInstanceId: async () => [task] },
    ProcessInstanceApi: { getApprovalDetail: async () => detail }, syncStageProgress() {},
    getControlledFileApplicationRounds: async (id, wire) => {
      calls.push({ id, wire })
      if (denied) throw Error('exact task access revoked')
      assert.ok(wire, 'task rounds must never use the unscoped file permission')
      const context = body.readDccBodyReadContextRoute(id, { bodyReadContext: wire })
      assert.equal(context.applicationType, 'OBSOLETE')
      assert.equal(context.bpmRound, BPM)
      return [{ controlledFileId: FILE, applicationType: 'OBSOLETE', bpmRound: BPM, attributeRound: 1 }]
    }
  }
  vm.runInNewContext(ts.transpileModule(code + '\nexports.api={loadApprovalDetail,loadData,resolveDetailBodyReadContext,currentApprovalApplicationType}', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, env)
  return { env, detail, instance, selection, calls, task, ...env.exports.api }
}

test('actual obsolete task loader scopes rounds then preview/evidence to the real task, never file revision', async () => {
  const f = fixture()
  await f.loadApprovalDetail()
  const expected = { intent: 'APPROVAL_TASK', controlledFileId: FILE, expectedVersionNo: 'A/2',
    applicationType: 'OBSOLETE', bpmRound: BPM, taskId: TASK }
  assert.deepEqual(plain(body.readDccBodyReadContextRoute(FILE, { bodyReadContext: f.calls[0].wire })), expected)
  assert.equal(f.currentApprovalApplicationType.value, 'OBSOLETE')
  assert.equal(f.env.approvalProgressScope.value.bpmRound, BPM)
  assert.equal(f.env.approvalTodoTask.value.id, TASK)
  assert.deepEqual(plain(f.resolveDetailBodyReadContext()), expected)
  assert.equal(f.env.fileDetail.value.processInstanceId, 'revision-round')
  assert.equal(f.env.applicationApprovalRead.value.processInstanceId, BPM)
})

test('borrowed Form Center object/version/round or conflicting requested application fails before rounds', async () => {
  for (const change of ['object', 'version', 'round', 'type', 'todoRound']) {
    const f = fixture()
    if (change === 'object') f.instance.formVariables.objectId = '99'
    if (change === 'version') f.instance.formVariables.objectVersion = 'A/1'
    if (change === 'round') f.instance.id = 'other-round'
    if (change === 'type') f.selection.applicationType = 'REVISION'
    if (change === 'todoRound') f.task.processInstanceId = 'other-round'
    await assert.rejects(f.loadApprovalDetail(), undefined, change)
    assert.equal(f.calls.length, 0, change)
    assert.equal(f.env.approvalTodoTask.value, null)
    assert.equal(f.env.approvalProgressScope.value, undefined)
  }
})

test('missing BPM authority never retries task rounds without an exact application context', async () => {
  const f = fixture({ detailAllowed: false })
  await assert.rejects(f.loadApprovalDetail(), /正式正文申请身份/)
  assert.equal(f.calls.length, 0)
})

test('explicit server-checked task context remains usable without broad BPM instance query permission', async () => {
  const f = fixture({ detailAllowed: false })
  f.selection.bodyReadContext = { intent: 'APPROVAL_TASK', controlledFileId: FILE, expectedVersionNo: 'A/2',
    applicationType: 'OBSOLETE', bpmRound: BPM, taskId: TASK }
  await f.loadApprovalDetail()
  assert.equal(f.calls.length, 1)
  assert.equal(f.currentApprovalApplicationType.value, 'OBSOLETE')
})

test('round authorization failure stays failed without selecting the native revision or retrying unscoped', async () => {
  const f = fixture({ denied: true })
  await assert.rejects(f.loadApprovalDetail(), /exact task access revoked/)
  assert.equal(f.calls.length, 1)
  assert.equal(f.env.approvalProgressScope.value, undefined)
  assert.match(f.env.applicationApprovalRead.value.error, /access revoked/)
})

test('a mismatched application type returned for the same task round cannot become the approval scope', async () => {
  const f = fixture()
  f.env.getControlledFileApplicationRounds = async () => [{ controlledFileId: FILE,
    applicationType: 'REVISION', bpmRound: BPM, attributeRound: 1 }]
  await assert.rejects(f.loadApprovalDetail(), /申请类型不一致/)
  assert.equal(f.env.approvalProgressScope.value, undefined)
  assert.equal(f.env.approvalTodoTask.value, null)
})

test('navigation changed while BPM was loading cannot request rounds or activate a different task', async () => {
  const f = fixture()
  let current = true
  f.env.isCurrentDetailLoad = () => current
  f.env.ProcessInstanceApi.getApprovalDetail = async () => { current = false; return f.detail }
  await f.loadApprovalDetail()
  assert.equal(f.calls.length, 0)
  assert.equal(f.env.approvalProgressScope.value, undefined)
  assert.equal(f.env.approvalTodoTask.value, null)
})

test('metadata loading does not prematurely unlock the task evidence panel before exact BPM context is ready', async () => {
  const f = fixture(), env = f.env
  let checks = 0
  Object.assign(env, {
    isCurrentDetailLoad: () => ++checks <= 2,
    getControlledFile: async () => env.fileDetail.value,
    getFileCategoryList: async () => [], getDirectoryTree: async () => [],
    getSimpleUserList: async () => [], getSimpleDeptList: async () => [],
    showLifecycleTraceSections: vue.ref(false), workingBrowserLoadedContext: vue.ref(),
    loadActiveObsoleteAction: async () => {}, getActiveApprovalPrintTemplate: async () => null
  })
  await f.loadData(1, FILE, env.route.fullPath)
  assert.equal(env.applicationApprovalRead.value, undefined)
  const selection = rounds.resolveApplicationRoundSelection({ fileId: FILE, requestedFileId: FILE,
    nativeBpmRound: 'revision-round', requestedBpmRound: BPM, requestedTaskId: TASK,
    contextKey: 'exact-key', readContext: env.applicationApprovalRead.value })
  assert.match(selection.blockedReason, /核验/)
  assert.equal(selection.primaryBpmRound, null)
})
