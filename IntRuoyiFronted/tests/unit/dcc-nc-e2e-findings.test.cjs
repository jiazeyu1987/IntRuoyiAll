const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
const ast = ts.createSourceFile('detail.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const decl = name => ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === name)).getText(ast)
function moduleFrom(file, deps = {}) {
  const c = { exports: {}, Error, Date, require: name => deps[name] || {} }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText, c)
  return c.exports
}
test('selected obsolete department task uses exact verified list task rather than another task of same leader', () => {
  const c = { exports: {}, String, Number, DCC_BPM_TASK_STATUS: { RUNNING: 1, APPROVING: 7 }, currentUserId: vue.ref('1'), route: { query: { taskId: 'dept2' } }, approvalProcessInstanceId: vue.ref('obsolete') }
  vm.runInNewContext(ts.transpileModule(decl('toNumericUserId') + '\n' + decl('findCurrentUserTodoTask') + '\nexports.find=findCurrentUserTodoTask', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const rows = [{ id: 'dept1', processInstanceId: 'obsolete', status: 1, assigneeUser: { id: 1 } }, { id: 'dept2', processInstanceId: 'obsolete', status: 1, assigneeUser: { id: 1 } }]
  assert.equal(c.exports.find(rows).id, 'dept2')
  assert.equal(c.exports.find([{ ...rows[1], processInstanceId: 'old-upload' }]), null)
})
test('actual loader retains exact enriched obsolete task instead of generic todo missing its actor', async () => {
  const calls = []
  const actual = [{ id: 'dept1', processInstanceId: 'O', taskDefinitionKey: 'MATRIX_REVIEW', status: 1, assigneeUser: { id: 1 } },
    { id: 'dept2', processInstanceId: 'O', taskDefinitionKey: 'MATRIX_REVIEW', status: 1, assigneeUser: { id: 1 } }]
  const c = { exports: {}, String, Number, Promise, DCC_BPM_TASK_STATUS: { RUNNING: 1, APPROVING: 7 },
    currentUserId: vue.ref('1'), route: { query: { processInstanceId: 'O', taskId: 'dept2' }, fullPath: 'route-O' },
    approvalProcessInstanceId: vue.ref('O'), fileDetail: vue.ref({ id: '10', processInstanceId: 'original-upload' }),
    detailLoadSequence: 1, controlledFileId: vue.ref('10'), applicationRoundContextKey: vue.ref('context-O'),
    isCurrentDetailLoad: () => true, viewerMode: vue.ref(false), isBrowserTraceabilityPage: vue.ref(false), checkPermi: () => true,
    canUploadApplicantTrainingRecord: vue.ref(false), TaskApi: { getTaskListByProcessInstanceId: async () => actual },
    ProcessInstanceApi: { getApprovalDetail: async () => ({ todoTask: { id: 'dept1', processInstanceId: 'O', taskDefinitionKey: 'MATRIX_REVIEW' } }) },
    validateApplicationApprovalRead: (round, id, tasks) => { assert.ok(tasks.every(t => t.processInstanceId === round)); return { processInstanceId: round, taskId: id } },
    validateApplicationRoundMappings: (_id, rows) => rows, getControlledFileApplicationRounds: async () => [{ controlledFileId: '10', bpmRound: 'O', applicationType: 'OBSOLETE' }],
    resolveApprovalProgressScope: () => ({ fileId: '10', bpmRound: 'O', applicationType: 'OBSOLETE' }), syncStageProgress: () => calls.push('sync')
  }
  for (const name of ['applicationApprovalRead', 'approvalProgressScope', 'approvalProgressError', 'approvalLoading', 'approvalTaskList', 'approvalTodoTask']) c[name] = vue.ref()
  vm.runInNewContext(ts.transpileModule(decl('toNumericUserId') + '\n' + decl('findCurrentUserTodoTask') + '\n' + decl('loadApprovalDetail') + '\nexports.load=loadApprovalDetail', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  await c.exports.load()
  assert.equal(c.approvalTodoTask.value.id, 'dept2')
  assert.equal(c.approvalTodoTask.value.assigneeUser.id, 1)
  assert.equal(c.fileDetail.value.processInstanceId, 'original-upload')
  assert.equal(c.applicationApprovalRead.value.taskId, 'dept2')
  assert.deepEqual(calls, ['sync'])
})
test('ordinary training loader uses DCC file-scoped read rather than generic BPM participant API', () => {
  const load = decl('loadApprovalDetail')
  assert.ok(load.includes('getControlledFileApplicationProgress'))
  assert.ok(load.includes('canUploadApplicantTrainingRecord'))
  assert.ok(load.indexOf('getControlledFileApplicationProgress') < load.indexOf('TaskApi.getTaskListByProcessInstanceId'))
})
test('actual eligible training read publishes completed facts without ever calling generic BPM', async () => {
  const c = { exports: {}, String, Promise, route: { query: { processInstanceId: 'T' }, fullPath: 'training' },
    detailLoadSequence: 1, controlledFileId: vue.ref('10'), applicationRoundContextKey: vue.ref('training-context'),
    fileDetail: vue.ref({ id: '10', processInstanceId: 'T', canReadTrainingApplicationProgress: true }),
    isCurrentDetailLoad: () => true, viewerMode: vue.ref(false), isBrowserTraceabilityPage: vue.ref(false), checkPermi: () => false,
    canUploadApplicantTrainingRecord: vue.ref(false), TaskApi: { getTaskListByProcessInstanceId: () => { throw Error('Generic BPM forbidden') } },
    getControlledFileApplicationProgress: async () => ({ controlledFileId: '10', applicationType: 'REVISION', bpmRound: 'T', attributeRound: 1,
      tasks: [{ id: 'review', processInstanceId: 'T', taskDefinitionKey: 'MATRIX_REVIEW', status: 2 }] }),
    validateApplicationApprovalRead: (processInstanceId, taskId) => ({ processInstanceId, taskId }),
    resolveApprovalProgressScope: ({ rounds }) => ({ fileId: '10', bpmRound: rounds[0].bpmRound, applicationType: rounds[0].applicationType }),
    syncStageProgress: () => {}, resolveReadSideErrorMessage: e => e.message }
  for (const name of ['applicationApprovalRead', 'approvalProgressScope', 'approvalProgressError', 'approvalLoading', 'approvalTaskList', 'approvalTodoTask', 'stageProgressList']) c[name] = vue.ref()
  vm.runInNewContext(ts.transpileModule(decl('loadApprovalDetail') + '\nexports.load=loadApprovalDetail', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  await c.exports.load()
  assert.equal(c.approvalTodoTask.value, null)
  assert.equal(c.approvalTaskList.value[0].status, 2)
  assert.equal(c.approvalProgressScope.value.applicationType, 'REVISION')
  assert.equal(c.applicationApprovalRead.value.processInstanceId, 'T')
  c.getControlledFileApplicationProgress = async () => { throw Error('DCC exact file scope denied') }
  await assert.rejects(c.exports.load(), /exact file scope denied/)
  assert.equal(c.approvalProgressScope.value, undefined)
  assert.equal(c.stageProgressList.value.length, 0)
})
test('native training waiting responsibility remains document control even when process read is denied', () => {
  const summary = moduleFrom('src/views/dcc/controlled-file/shared/handlingSummary.ts')
  assert.match(summary.getControlledFileHandlingSummary({ status: 'PENDING_APPLICANT_TRAINING_RECORD' }).responsibilityHint, /文控/)
})
test('distribution pending means waiting receipt only when formal distribution time exists', () => {
  const detail = moduleFrom('src/views/dcc/controlled-file/detail/presentation.ts', { dayjs: require('dayjs') })
  assert.equal(detail.getDistributionStatusLabel('PENDING', { distributedTime: '2026-10-07 21:00:00' }), '已下发，待签收')
  assert.equal(detail.getDistributionStatusLabel('PENDING', {}), '待分发')
  assert.equal(detail.getDistributionStatusLabel('ACKNOWLEDGED', { distributedTime: '2026-10-07' }), '已确认')
  assert.equal(detail.getDistributionStatusLabel('future-code', {}), 'future-code')
  assert.match(fs.readFileSync('src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue', 'utf8'), /getDistributionStatusLabel\(row.status,\s*file\)/)
})
test('actual browser summary formats Java LocalDate and rejects impossible calendar dates', () => {
  const b = moduleFrom('src/views/dcc/controlled-file/browser/presentation.ts', {
    '../shared/version-display': moduleFrom('src/views/dcc/controlled-file/shared/version-display.ts'),
    '../shared/lifecycle': { getDccControlledFileStatusLabel: s => s, getDccControlledFileStatusTagType: () => 'info' },
    '@/utils/formatTime': { formatDateTimeValue: () => '-' }
  })
  assert.equal(b.getBrowserVersionSummary({ versionNo: 'A/1', status: 'ACTIVE', effectiveDate: [2026, 10, 7] }, false, 'A/1').effectiveText, '生效：2026-10-07')
  assert.equal(b.getBrowserVersionSummary({ effectiveDate: [2026, 2, 30] }, false).effectiveText, '生效：日期格式异常')
  assert.equal(b.getBrowserVersionSummary({ effectiveDate: null }, false).effectiveText, '生效：-')
})
