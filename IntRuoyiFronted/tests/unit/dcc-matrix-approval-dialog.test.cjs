const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const source = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
const script = ts.createSourceFile('detail.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['actionDialogTitle', 'actionDialogSubmitFlowText', 'buildActionSuccessMessage'])
const code = script.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.has(d.name.getText(script)))).map(s => s.getText(script)).join('\n')
function model() {
  const env = {
    computed: fn => ({ get value() { return fn() } }),
    actionDialog: { mode: 'approve' }, approvalTodoTask: { value: { taskDefinitionKey: 'MATRIX_APPROVAL' } },
    canShowSignoffAssignment: { value: false }, displayStageProgressList: { value: [] },
    getDccControlledFileStatusLabel: status => `实际状态:${status}`, exports: {}
  }
  vm.runInNewContext(ts.transpileModule(code + '\nexports.api = { actionDialogTitle, actionDialogSubmitFlowText, buildActionSuccessMessage }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return { ...env, ...env.exports.api }
}

test('matrix titles identify approval and rejection, without changing other shared titles', () => {
  const m = model()
  assert.equal(m.actionDialogTitle.value, '会签批准通过')
  m.actionDialog.mode = 'reject'
  assert.equal(m.actionDialogTitle.value, '会签批准驳回')
  assert.match(m.actionDialogSubmitFlowText.value, /本轮审批结束.*保留正文、意见和签名记录.*重新提交.*新一轮审批/)
  m.approvalTodoTask.value.taskDefinitionKey = 'SUPERVISOR_REVIEW'
  assert.equal(m.actionDialogTitle.value, '直属主管审核驳回')
  assert.equal(m.actionDialogSubmitFlowText.value, '驳回后由申请人修改并重新提交。')
})

test('before signature submission neither participant counts nor route snapshots promise a next node', () => {
  const m = model()
  for (const approvedCount of [0, 1, 2]) {
    m.displayStageProgressList.value = [
      { stageCode: 'MATRIX_APPROVAL', totalCount: 2, approvedCount },
      { stageCode: 'DOC_CONTROL_REVIEW', stageName: '文控审核' }
    ]
    assert.match(m.actionDialogSubmitFlowText.value, /本轮批准规则及实际进度/)
    assert.doesNotMatch(m.actionDialogSubmitFlowText.value, /下一节点/)
  }
})

test('signature response determines waiting, actual document control entry, and preserved rejected round', () => {
  const { buildActionSuccessMessage: result } = model()
  assert.match(result('approve', { nextStatus: 'PENDING_MATRIX_APPROVAL' }, 'MATRIX_APPROVAL'), /等待其他批准人/)
  assert.match(result('approve', { nextStatus: 'PENDING_DOC_CONTROL_REVIEW' }, 'MATRIX_APPROVAL'), /下一节点：文控审核/)
  assert.match(result('reject', { nextStatus: 'REJECTED' }, 'MATRIX_APPROVAL'), /本轮审批结束.*保留正文、意见和签名记录.*新一轮审批/)
  for (const status of ['OFFLINE_TRAINING', 'PENDING_DOC_CONTROL_APPROVAL', 'ACTIVE']) {
    const text = result('approve', { nextStatus: status }, 'MATRIX_APPROVAL')
    assert.ok(text.includes(`实际状态:${status}`))
    assert.doesNotMatch(text, /下一节点：文控审核|等待其他批准人/)
  }
  assert.doesNotMatch(result('approve', { nextStatus: 'PENDING_DOC_CONTROL_REVIEW' }, 'MATRIX_REVIEW'), /下一节点/)
})

test('result uses captured task identity and shared dialog keeps actual file facts and masked signature order', () => {
  assert.match(source, /taskDefinitionKey: approvalTodoTask.value\?\.taskDefinitionKey/)
  assert.match(source, /buildActionSuccessMessage\(request.action, result.response, request.taskDefinitionKey\)/)
  const dialog = source.split('v-model="actionDialog.visible"')[1].split('</el-dialog>')[0]
  for (const fact of ['ep:document', 'fileDetail?.fileName', 'fileDetail?.fileNumber', 'fileDetail?.versionNo', 'currentSignerLabel', '请输入当前登录密码', 'type="password"']) assert.ok(dialog.includes(fact), fact)
  assert.ok(dialog.indexOf('v-model="actionDialog.form.reason"') < dialog.indexOf('v-model="actionDialog.form.password"'))
  assert.ok(dialog.indexOf('确认批准并签名') < dialog.lastIndexOf('取消'))
  assert.match(source, /if \(actionDialog.submitting \|\| !actionDialog.visible\) return/)
})

test('shared signature transport rejects blank inputs and reports wrong password without success', async () => {
  // Unit boundary only; this does not stand in for a real signed browser approval.
  const actions = {}, calls = []
  class SignatureError extends Error {}
  const passwordError = new SignatureError('当前密码错误')
  const transport = async (id, payload) => { calls.push(payload); throw passwordError }
  new Function('exports', 'require', ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/detail/approval-actions.ts', 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText)(actions, name => {
    if (name.endsWith('/workflow')) return {
      approveControlledFileTask: transport, rejectControlledFileTask: transport,
      DccTaskActionError: SignatureError, isControlledFileTaskPasswordInvalidError: error => error === passwordError
    }
    if (name === '../shared/lifecycle') return { getDccControlledFileStageByKey: () => undefined }
    throw Error(name)
  })
  const submit = (action, password, reason) => actions.submitDccApprovalAction({ fileId: 'unit-file', taskId: 'unit-task', action, form: { password, reason } })
  assert.equal((await submit('approve', '   ', '')).field, 'password')
  assert.equal((await submit('reject', 'unit-password', ' \n ')).field, 'reason')
  assert.equal(calls.length, 0)
  const failed = await submit('approve', ' incorrect password ', '')
  assert.equal(failed.success, false)
  assert.equal(failed.field, 'password')
  assert.match(failed.inlineError, /当前密码错误/)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].password, ' incorrect password ')
  assert.equal(calls[0].reason, '')
})
