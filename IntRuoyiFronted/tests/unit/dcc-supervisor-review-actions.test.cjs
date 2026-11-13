const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.join(__dirname, '../../src/views/dcc/controlled-file/detail')
const filename = path.join(root, 'SupervisorReviewActions.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const compiled = compileScript(descriptor, { id: 'supervisor-actions' })
assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'supervisor-actions',
  compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
const source = descriptor.scriptSetup.content.replace(/^import .*$/gm, '')

function actions(request, blockedReason = '') {
  const props = vue.reactive({ fileId: '101', taskId: 'task-1', taskName: '主管审核',
    contextKey: 'user-1-round-1', blockedReason, submitting: false })
  const scope = vue.effectScope()
  const env = { ...vue, props, defineProps: () => props, defineEmits: () => {},
    onMounted: () => {}, onBeforeUnmount: () => {}, getControlledFileTaskActionReadiness: request,
    resolveReadSideErrorMessage: e => e.message }
  scope.run(() => vm.runInNewContext(ts.transpileModule(source +
    '\nglobalThis.state = { readiness, loading, error, canApprove, canReject, loadReadiness }',
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env))
  return { props, state: env.state, stop: () => scope.stop() }
}
const settle = async () => { await Promise.resolve(); await vue.nextTick() }

test('permission or task denial prevents condition reads and both operations', async () => {
  let calls = 0
  const h = actions(async () => { calls++; return { ready: true, blockers: [] } }, '没有分配给您的主管审核任务')
  await settle()
  assert.equal(calls, 0)
  assert.equal(h.state.canApprove.value, false)
  assert.equal(h.state.canReject.value, false)
  h.stop()
})

test('server blockers disable approval while rejection remains available; submit disables both', async () => {
  const h = actions(async () => ({ ready: false, blockers: [{ message: '请完成前置审核' }] }))
  await settle()
  assert.equal(h.state.canApprove.value, false)
  assert.equal(h.state.canReject.value, true)
  h.props.submitting = true
  assert.equal(h.state.canReject.value, false)
  h.stop()
})

test('failed condition reads remain visible, and an explicit successful recheck enables approval', async () => {
  let failed = true
  const h = actions(async () => { if (failed) throw new Error('任务已变化'); return { ready: true, blockers: [] } })
  await settle()
  assert.match(h.state.error.value, /任务已变化/)
  assert.equal(h.state.canApprove.value, false)
  failed = false
  await h.state.loadReadiness()
  assert.equal(h.state.canApprove.value, true)
  h.stop()
})

test('an old task response cannot enable approval after the task or account changes', async () => {
  const pending = []
  const h = actions(() => new Promise(resolve => pending.push(resolve)))
  h.props.contextKey = 'user-2-round-2'
  h.props.taskId = 'task-2'
  await vue.nextTick()
  pending[0]({ ready: true, blockers: [] })
  await settle()
  assert.equal(h.state.canApprove.value, false)
  pending[1]({ ready: false, blockers: [{ message: '新任务尚未就绪' }] })
  await settle()
  assert.equal(h.state.canApprove.value, false)
  h.stop()
})

test('page denies approval for missing permission, stale detail, wrong task and round errors', () => {
  const page = parse(fs.readFileSync(path.join(root, 'index.vue'), 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile('page.ts', page, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s)
    && s.declarationList.declarations.some(d => d.name.getText(ast) === 'supervisorReviewBlockedReason'))
  const env = { computed: vue.computed, approvalLoading: vue.ref(false), checkPermi: () => true,
    isWorkingBrowserDetailCurrent: vue.ref(true), approvalProgressError: vue.ref(''),
    applicationRoundSelection: vue.ref({ blockedReason: '' }),
    approvalTodoTask: vue.ref({ taskDefinitionKey: 'SUPERVISOR_REVIEW' }) }
  vm.runInNewContext(ts.transpileModule(statement.getText(ast) + '\nglobalThis.reason = supervisorReviewBlockedReason',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  assert.equal(env.reason.value, '')
  env.isWorkingBrowserDetailCurrent.value = false
  assert.match(env.reason.value, /刷新/)
  env.isWorkingBrowserDetailCurrent.value = true
  env.approvalTodoTask.value = null
  assert.match(env.reason.value, /没有分配/)
  env.applicationRoundSelection.value.blockedReason = '申请轮次不一致'
  assert.match(env.reason.value, /申请轮次不一致/)
})

test('supervisor has a current task section and keeps its actions in the fixed bar', () => {
  const page = parse(fs.readFileSync(path.join(root, 'index.vue'), 'utf8')).descriptor
  assert.match(page.template.content, /<section ref="reviewTaskRef" class="review-section">[\s\S]*?data-testid="dcc-approval-upload-action-panel"/)
  assert.match(page.template.content, /<div v-if="!canShowSignoffAssignment && !isSupervisorReviewPage && !isActualSignoffReviewPage && !isMatrixApprovalPage"[^>]*class="mt-14px flex flex-wrap gap-8px"/)
  assert.match(page.template.content, /<SupervisorReviewActions v-if="isSupervisorReviewPage"/)
  const overview = page.template.content.indexOf('dcc-approval-upload-view')
  const body = page.template.content.indexOf('dcc-approval-upload-file-preview')
  const application = page.template.content.indexOf('<DetailApplicationHistory', body)
  const relations = page.template.content.indexOf('<DetailRelationsPanel')
  assert.ok(overview < body && body < application && application < relations)
})

test('verified countersigner has the approval label while assignment verification stays explicit', () => {
  const page = parse(fs.readFileSync(path.join(root, 'index.vue'), 'utf8')).descriptor
  const ast = ts.createSourceFile('page.ts', page.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s)
    && s.declarationList.declarations.some(d => d.name.getText(ast) === 'currentApprovalTaskName'))
  const env = { computed: vue.computed, approvalTodoTask: vue.ref({ id: 't1' }),
    isSignoffTask: vue.ref(true), currentApprovalApplicationType: vue.ref('REVISION'),
    signoffAssignmentState: vue.ref({ fileId: '101', taskId: 't1', assigned: true }),
    fileDetail: vue.ref({ id: '101' }), resolveDccStageDisplayName: () => '直属主管审核' }
  vm.runInNewContext(ts.transpileModule(statement.getText(ast) + '\nglobalThis.name = currentApprovalTaskName',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  assert.equal(env.name.value, '会签批准')
  env.signoffAssignmentState.value.assigned = false
  assert.equal(env.name.value, '部门负责人签名指派')
  env.signoffAssignmentState.value.taskId = 'stale-task'
  assert.match(env.name.value, /正在核验指派状态/)
  env.isSignoffTask.value = false
  assert.equal(env.name.value, '直属主管审核')
  assert.match(page.template.content, /已完成 \{\{ actualSignoffStage\?\.completionText/)
  assert.match(page.template.content, /<SupervisorReviewActions v-if="isActualSignoffReviewPage"[^>]*approve-text="批准通过"/)
})


test('actual countersign requires verified assignment and current permitted task before enabling fixed operations', () => {
  const descriptor = parse(fs.readFileSync(path.join(root, 'index.vue'), 'utf8')).descriptor
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['isActualSignoffReviewPage', 'signoffReviewBlockedReason']
  const declarations = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  const permitted = vue.ref(true)
  const env = { computed: vue.computed, isApprovalUploadHandlingPage: vue.ref(true), isSignoffTask: vue.ref(true),
    signoffReadyForReview: vue.ref(true), approvalLoading: vue.ref(false), checkPermi: () => permitted.value,
    isWorkingBrowserDetailCurrent: vue.ref(true), approvalProgressError: vue.ref(''), applicationRoundSelection: vue.ref({ blockedReason: '' }) }
  vm.runInNewContext(ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') +
    '\nglobalThis.active = isActualSignoffReviewPage; globalThis.reason = signoffReviewBlockedReason', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  assert.equal(env.active.value, true)
  assert.equal(env.reason.value, '')
  env.signoffReadyForReview.value = false
  assert.equal(env.active.value, false)
  assert.match(env.reason.value, /指派状态尚未核验/)
  env.signoffReadyForReview.value = true
  permitted.value = false
  assert.match(env.reason.value, /缺少会签权限/)
  permitted.value = true; env.isWorkingBrowserDetailCurrent.value = false
  assert.match(env.reason.value, /上下文已变化/)
  env.isWorkingBrowserDetailCurrent.value = true; env.applicationRoundSelection.value.blockedReason = '轮次错误'
  assert.equal(env.reason.value, '轮次错误')
  assert.match(descriptor.template.content, /<SupervisorReviewActions v-if="isActualSignoffReviewPage"[\s\S]*?:blocked-reason="signoffReviewBlockedReason"[\s\S]*?@approve="openActionDialog\('approve'\)" @reject="openActionDialog\('reject'\)"/)
})
