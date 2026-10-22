const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')

const frontend = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(frontend, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'), 'utf8')
const { descriptor, errors } = parse(source)
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('batch-detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set([
  'canQueryProductionReleaseReportCandidates', 'isProductionReleaseReportNode',
  'resolveProductionReleaseReportWorkTask', 'requireProductionReleaseReportStringId',
  'requireProductionReleaseReportVersion', 'resolveTaskGateText',
  'loadProductionReleaseReportCandidates', 'loadDetail'
])
const selected = ast.statements.filter(ts.isVariableStatement).flatMap(s => [...s.declarationList.declarations])
  .filter(d => names.has(d.name.getText(ast))).map(d => 'const ' + d.getText(ast) + ';').join('\n')
const executable = ts.transpileModule(selected, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const queryPermission = 'mes:pro-edhr-work-task:query'
const batchPermission = 'mes:pro-edhr-batch-execution:query'
const batchId = '900000001252'
const reportNodes = ['INCOMING_INSPECTION_REPORT', 'STERILIZATION_REPORT', 'FINISHED_PRODUCT_INSPECTION_REPORT', 'FINISHED_PRODUCT_INSPECTION_RECORD']
const report = { id: '9000301350', nodeType: reportNodes[0] }
const candidate = { id: '2810', batchTaskId: report.id, batchExecutionId: batchId, businessScopeType: 'RELEASE_REPORT_NODE', taskType: 'FILL', status: 'TODO', nodeType: report.nodeType, version: 2 }

function harness(permissions, { tasks = [report], response = { list: [candidate] }, error } = {}) {
  const calls = [], detailCalls = [], secondary = []
  const context = {
    ...vue, userStore: vue.reactive({ permissions: new Set(permissions), roles: ['MES_PRODUCTION_TEAM_LEADER'] }),
    detail: vue.ref({ id: batchId, tasks }), productionReleaseReportWorkTasks: vue.ref([]),
    productionReleaseReportCandidateLoading: vue.ref(false), productionReleaseReportCandidateError: vue.ref(''),
    productionReleaseReportNodeTypeSet: new Set(reportNodes), EDHR_PRODUCTION_RELEASE_REPORT_NODE_TYPES: reportNodes,
    EDHR_WORK_TASK_TYPE_FILL: 'FILL', EDHR_WORK_TASK_STATUS_TODO: 'TODO',
    loading: vue.ref(false), loadError: vue.ref(''), secondaryLoadError: vue.ref(''),
    workbench: vue.ref(), reviewTimeline: vue.ref(), selectedExecutionId: vue.ref(''), selectedTaskId: vue.ref(''),
    inlineActiveOrderSubmissionDetail: vue.ref(), inlineActiveOrderSubmissionDetailLoading: vue.ref(false),
    inlineActiveOrderSubmissionDetailError: vue.ref(''),
    isStaleBatchDetailRequest: () => false, cancelDeferredBatchDetailSecondaryLoad() {}, clearTaskPreview() {},
    syncSpecialNodePendingAttachmentsFromDetail() {}, applyInitialBatchTaskSelection() {},
    loadInlineActiveOrderSubmissionDetail: async () => {},
    deferInitialBatchDetailSecondaryLoad: id => secondary.push(id), assertBatchExecutionId: () => batchId,
    resolveTaskSlotBlocker: () => '', normalizeTaskAccessReason: value => value || '',
    resolveErrorMessage: (e, defaultMessage) => e.message || defaultMessage,
    getEdhrBatchExecution: async id => { detailCalls.push(id); return { id, tasks } },
    getEdhrWorkTaskCandidateTodoPage: async req => { calls.push(req); if (error) throw error; return response }
  }
  const page = Function(...Object.keys(context), 'let batchDetailRequestSerial=0;\n' + executable + '\nreturn {loadDetail,loadProductionReleaseReportCandidates,resolveProductionReleaseReportWorkTask,resolveTaskGateText};')(...Object.values(context))
  return { page, context, calls, detailCalls, secondary }
}
const settle = async () => { await Promise.resolve(); await Promise.resolve() }

test('ordinary batch-view-only detail does not issue a candidate query or lose the main detail', async () => {
  const h = harness([batchPermission], { error: new Error('403 report candidate query denied') })
  await h.page.loadDetail(); await settle()
  assert.deepEqual(h.detailCalls, [batchId]); assert.deepEqual(h.secondary, [batchId])
  assert.equal(h.context.detail.value.id, batchId); assert.equal(h.context.loading.value, false)
  assert.equal(h.context.loadError.value, ''); assert.equal(h.calls.length, 0)
  assert.equal(h.context.productionReleaseReportCandidateLoading.value, false)
  assert.match(h.page.resolveTaskGateText(report), /没有报告候选任务查询权限/)
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report), undefined)
})

test('exact task-query authorizes one unchanged query and a matching frozen candidate', async () => {
  const h = harness([batchPermission, queryPermission]); await h.page.loadDetail(); await settle()
  assert.deepEqual(h.calls, [{ pageNo: 1, pageSize: 100, taskType: 'FILL', status: 'TODO', nodeTypes: reportNodes, batchExecutionId: batchId }])
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report).id, candidate.id)
  assert.equal(h.context.productionReleaseReportCandidateError.value, '')
})

test('the existing wildcard permission allows the same formal candidate query', async () => {
  const h = harness(['*:*:*']); await h.page.loadProductionReleaseReportCandidates(batchId)
  assert.equal(h.calls.length, 1); assert.equal(h.page.resolveProductionReleaseReportWorkTask(report).id, candidate.id)
})

test('role names and batch-query alone cannot authorize candidate access', async () => {
  for (const role of ['super_admin', 'admin', 'MES_REPORT_FILLER']) {
    const h = harness([batchPermission]); h.context.userStore.roles = [role]
    await h.page.loadProductionReleaseReportCandidates(batchId)
    assert.equal(h.calls.length, 0, role)
  }
})

test('no report nodes means no candidate query even for authorized viewers', async () => {
  const h = harness([queryPermission], { tasks: [{ id: '9000301288', nodeType: 'ROUTE_FORM' }] })
  await h.page.loadDetail(); await settle(); assert.equal(h.calls.length, 0)
})

test('permission revocation immediately disables stale candidates and reload clears them', async () => {
  const h = harness([queryPermission]); await h.page.loadProductionReleaseReportCandidates(batchId)
  h.context.userStore.permissions.delete(queryPermission)
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report), undefined)
  h.context.productionReleaseReportCandidateLoading.value = true
  await h.page.loadProductionReleaseReportCandidates(batchId)
  assert.equal(h.calls.length, 1); assert.deepEqual(h.context.productionReleaseReportWorkTasks.value, [])
  assert.equal(h.context.productionReleaseReportCandidateLoading.value, false)
  assert.match(h.page.resolveTaskGateText(report), /没有报告候选任务查询权限/)
})

test('an authorized 403 remains a visible report error while main detail remains readable', async () => {
  const h = harness([queryPermission], { error: new Error('403 candidate permission denied') })
  await h.page.loadDetail(); await settle()
  assert.equal(h.calls.length, 1); assert.equal(h.context.detail.value.id, batchId)
  assert.match(h.context.productionReleaseReportCandidateError.value, /403/)
  assert.match(h.page.resolveTaskGateText(report), /403/)
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report), undefined)
})

test('a foreign batch candidate is rejected without permitting report actions', async () => {
  const h = harness([queryPermission], { response: { list: [{ ...candidate, batchExecutionId: '900000001251' }] } })
  await h.page.loadProductionReleaseReportCandidates(batchId)
  assert.match(h.context.productionReleaseReportCandidateError.value, /所属批次/)
  assert.deepEqual(h.context.productionReleaseReportWorkTasks.value, [])
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report), undefined)
})

test('an invalid formal version remains a schema error, not a usable candidate', async () => {
  const h = harness([queryPermission], { response: { list: [{ ...candidate, version: 0 }] } })
  await h.page.loadProductionReleaseReportCandidates(batchId)
  assert.match(h.context.productionReleaseReportCandidateError.value, /权威申请版本/)
  assert.equal(h.page.resolveProductionReleaseReportWorkTask(report), undefined)
})

test('authorized successful empty candidates preserve the existing frozen-candidate empty state', async () => {
  const h = harness([queryPermission], { response: { list: [] } })
  await h.page.loadProductionReleaseReportCandidates(batchId)
  assert.equal(h.calls.length, 1); assert.equal(h.context.productionReleaseReportCandidateError.value, '')
  assert.match(h.page.resolveTaskGateText(report), /不是该报告的冻结候选/)
})

test('frontend gate matches the exact backend candidate endpoint; four report responsibilities and main hook remain', () => {
  const controller = fs.readFileSync(path.resolve(frontend, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrWorkTaskController.java'), 'utf8')
  assert.match(controller, /@GetMapping\("\/candidate-todo-page"\)\s+@PreAuthorize\("@ss\.hasPermission\('mes:pro-edhr-work-task:query'\)"\)/)
  assert.match(source, /canQueryProductionReleaseReportCandidates = computed\([\s\S]*?userStore\.permissions\.has\('mes:pro-edhr-work-task:query'\)[\s\S]*?userStore\.permissions\.has\('\*:\*:\*'\)/)
  assert.match(source, /v-if="resolveTaskGateText\(task\)"/)
  assert.match(source, /void loadProductionReleaseReportCandidates\(id, requestSerial\)/)
  const api = fs.readFileSync(path.join(frontend, 'src/api/mes/pro/edhr/workTask.ts'), 'utf8')
  for (const node of reportNodes) assert.ok(api.includes("'" + node + "'"))
})
