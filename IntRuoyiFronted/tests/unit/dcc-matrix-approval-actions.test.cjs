const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript'), vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = path.join(__dirname, '../../src/views/dcc/controlled-file/detail/index.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function state() {
  const env = { computed: vue.computed, approvalLoading: vue.ref(false), checkPermi: p => p[0] === 'dcc:controlled-file:approve',
    isApprovalUploadHandlingPage: vue.ref(true), isExternalReviewProcess: vue.ref(false),
    isWorkingBrowserDetailCurrent: vue.ref(true), approvalProgressError: vue.ref(''),
    applicationRoundSelection: vue.ref({ blockedReason: '' }), currentUserId: vue.ref(8),
    approvalProcessInstanceId: vue.ref('round-1'),
    approvalTodoTask: vue.ref({ id: 'task-1', taskDefinitionKey: 'MATRIX_APPROVAL', assigneeUserId: 8, processInstanceId: 'round-1' }),
    displayStageProgressList: vue.ref([{ stageCode: 'MATRIX_REVIEW', completionText: '9/9' },
      { stageCode: 'MATRIX_APPROVAL', completionText: '2/3', isCurrent: true }]) }
  const names = ['isMatrixApprovalPage', 'matrixApprovalStage', 'matrixApprovalBlockedReason']
  const code = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
  vm.runInNewContext(ts.transpileModule(code + '\nglobalThis.state = { isMatrixApprovalPage, matrixApprovalStage, matrixApprovalBlockedReason }',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return env
}
test('approval progress uses its own stage; changed counts and task completion remain reactive', () => {
  const h = state()
  assert.equal(h.state.isMatrixApprovalPage.value, true)
  assert.equal(h.state.matrixApprovalStage.value.completionText, '2/3')
  h.displayStageProgressList.value[1].completionText = '3/3'
  assert.equal(h.state.matrixApprovalStage.value.completionText, '3/3')
  h.approvalTodoTask.value = null
  assert.equal(h.state.isMatrixApprovalPage.value, false)
  h.displayStageProgressList.value = [h.displayStageProgressList.value[0]]
  assert.equal(h.state.matrixApprovalStage.value, undefined)
})
test('approval rejects missing permission, stale identity, wrong assignee and process, or failed progress', () => {
  const h = state(), reason = h.state.matrixApprovalBlockedReason
  assert.equal(reason.value, '')
  h.currentUserId.value = 9
  assert.match(reason.value, /没有分配/)
  h.currentUserId.value = 8
  h.approvalProcessInstanceId.value = 'round-2'
  assert.match(reason.value, /没有分配/)
  h.approvalProcessInstanceId.value = 'round-1'
  h.isWorkingBrowserDetailCurrent.value = false
  assert.match(reason.value, /上下文/)
  h.isWorkingBrowserDetailCurrent.value = true
  h.approvalProgressError.value = '进度读取失败'
  assert.equal(reason.value, '进度读取失败')
  h.checkPermi = () => false
  h.approvalLoading.value = true
  assert.match(reason.value, /正在读取/)
  h.approvalLoading.value = false
  assert.match(reason.value, /缺少批准权限/)
})
test('page compiles and matrix approval reuses the fixed bar with real handlers; task card has no duplicate actions', () => {
  const compiled = compileScript(descriptor, { id: 'matrix-approval' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'matrix-approval',
    compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  const html = descriptor.template.content
  assert.match(html, /<SupervisorReviewActions v-if="isMatrixApprovalPage"[\s\S]*?approve-text="批准通过"[\s\S]*?:submitting="actionDialog.submitting \|\| actionDialog.visible"[\s\S]*?@approve="openActionDialog\('approve'\)" @reject="openActionDialog\('reject'\)"/)
  assert.match(html, /!isSupervisorReviewPage && !isActualSignoffReviewPage && !isMatrixApprovalPage/)
  assert.match(html, /已完成 {{ matrixApprovalStage\?\.completionText/)
})
