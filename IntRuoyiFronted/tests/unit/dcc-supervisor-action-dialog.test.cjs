const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue', 'utf8')
const descriptor = parse(source).descriptor
const script = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const names = new Set(['actionDialogTitle', 'actionDialogSubmitFlowText', 'currentSignerLabel'])
const code = script.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.has(d.name.getText(script)))).map(s => s.getText(script)).join('\n')
function model(mode = 'approve') {
  const env = {
    computed: fn => ({ get value() { return fn() } }),
    canShowSignoffAssignment: { value: false },
    actionDialog: { mode }, approvalTodoTask: { value: { taskDefinitionKey: 'SUPERVISOR_REVIEW' } },
    userStore: { getUser: { nickname: '审核人', username: 'reviewer' } },
    displayStageProgressList: { value: [{ stageCode: 'SUPERVISOR_REVIEW', stageName: '直属主管审核' }, { stageCode: 'MATRIX_REVIEW_ASSIGN', stageName: '部门负责人签名指派' }] },
    exports: {}
  }
  vm.runInNewContext(ts.transpileModule(code + '\nexports.api = { actionDialogTitle, actionDialogSubmitFlowText, currentSignerLabel }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return { ...env, ...env.exports.api }
}
test('supervisor approve and reject identify the action in the title and explain rejection consequences', () => {
  const approve = model(), reject = model('reject')
  assert.equal(approve.actionDialogTitle.value, '直属主管审核通过')
  assert.equal(reject.actionDialogTitle.value, '直属主管审核驳回')
  assert.equal(reject.actionDialogSubmitFlowText.value, '驳回后由申请人修改并重新提交。')
})
test('next step follows actual verified round stages without printing entire process or inventing a node', () => {
  const m = model()
  assert.equal(m.actionDialogSubmitFlowText.value, '下一节点：部门负责人签名指派')
  m.displayStageProgressList.value[1].stageName = '本轮实际下一环节'
  assert.equal(m.actionDialogSubmitFlowText.value, '下一节点：本轮实际下一环节')
  m.displayStageProgressList.value = []
  assert.match(m.actionDialogSubmitFlowText.value, /未能确认下一流程节点/)
})
test('signer shows name then existing account name', () => {
  const m = model()
  assert.equal(m.currentSignerLabel.value, '审核人')
  m.userStore.getUser.nickname = ''
  assert.equal(m.currentSignerLabel.value, 'reviewer')
})

test('department leader rejection identifies the node and uses the shared rejection consequences', () => {
  const m = model('reject')
  m.approvalTodoTask.value.taskDefinitionKey = 'MATRIX_REVIEW'
  m.canShowSignoffAssignment.value = true
  assert.equal(m.actionDialogTitle.value, '部门负责人驳回')
  assert.equal(m.actionDialogSubmitFlowText.value, '驳回后由申请人修改并重新提交。')
})
test('both modes share ordered fields, masked password and busy controls; Vue template compiles', () => {
  const dialog = descriptor.template.content.split('v-model="actionDialog.visible"')[1].split('</el-dialog>')[0]
  const labels = ['icon="ep:document"', '{{ fileDetail?.fileName }}', '<dt>文件编号</dt>', '<dt>版本</dt>', '<dt>签名人</dt>', '<strong>提交后流转</strong>', 'v-model="actionDialog.form.reason"', 'v-model="actionDialog.form.password"', '<template #footer>']
  for (let i = 1; i < labels.length; i++) assert.ok(dialog.indexOf(labels[i]) > dialog.indexOf(labels[i - 1]), labels[i])
  assert.doesNotMatch(dialog, /签名含义|签名动作|任务ID|电子签名审计证据|证据前置|文件类型/)
  assert.doesNotMatch(dialog, /签署说明/)
  assert.match(source, /\.action-signature-file__name\s*\{[^}]*overflow-wrap: anywhere/s)
  assert.match(dialog, /label="电子签名" required/)
  assert.match(source, /\.action-signature-confirm--approve\s*\{[^}]*--el-button-bg-color: #008f7a/s)
  assert.match(dialog, /action-signature-flow--reject/)
  for (const text of ['审批意见（选填）', '驳回原因（必填）', '请输入审批意见', '请填写具体的驳回原因，便于申请人修改', '请输入所选签名方式的六位 PIN 码或密码', 'type="password"', '确认批准并签名', '确认驳回并签名', '提交中', "'danger' : 'primary'"]) assert.ok(dialog.includes(text), text)
  assert.match(dialog, /:disabled="actionDialog.submitting"/)
  for (const prop of ['close-on-click-modal', 'close-on-press-escape', 'show-close']) assert.ok(dialog.includes(`:${prop}="!actionDialog.submitting"`))
  const result = compileTemplate({ source: descriptor.template.content, filename: 'detail/index.vue', id: 'issue14' })
  assert.deepEqual(result.errors, [])
})


test('actual countersign title and next step follow remaining verified participants', () => {
  const m = model()
  m.approvalTodoTask.value.taskDefinitionKey = 'MATRIX_REVIEW'
  m.displayStageProgressList.value = [
    { stageCode: 'MATRIX_REVIEW_ASSIGN', stageName: '部门负责人签名指派' },
    { stageCode: 'MATRIX_REVIEW', stageName: '实际会签人会签', totalCount: 3, approvedCount: 1 },
    { stageCode: 'MATRIX_APPROVAL', stageName: '批准' }
  ]
  assert.equal(m.actionDialogTitle.value, '会签通过')
  assert.match(m.actionDialogSubmitFlowText.value, /等待其他会签人/)
  m.displayStageProgressList.value[1].approvedCount = 2
  assert.equal(m.actionDialogSubmitFlowText.value, '下一节点：批准')
  m.displayStageProgressList.value = []
  assert.match(m.actionDialogSubmitFlowText.value, /尚未核验/)
  m.actionDialog.mode = 'reject'
  assert.equal(m.actionDialogTitle.value, '会签驳回')
  assert.match(m.actionDialogSubmitFlowText.value, /由申请人修改并重新提交/)
})

test('all shared signature dialogs place confirmation before rightmost cancel', () => {
  const dialog = descriptor.template.content.split('v-model="actionDialog.visible"')[1].split('</el-dialog>')[0]
  const footer = dialog.split('<template #footer>')[1]
  assert.ok(footer.indexOf('@click="submitActionDialog"') < footer.indexOf('@click="closeActionDialog"'))
  assert.match(dialog, /label-position="left"/)
  assert.match(dialog, /label="电子签名" required/)
})


test('confirmed signature feedback uses actual response status without promising activation', () => {
  const env = { getDccControlledFileStatusLabel: status => ({
    PENDING_MATRIX_REVIEW: '待会签审核', CONTROLLED_PENDING_EFFECTIVE: '受控（待生效）',
    REJECTED: '已驳回'
  })[status] }
  vm.runInNewContext(ts.transpileModule(script.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(script) === 'buildActionSuccessMessage')).getText(script) + '\nexports = buildActionSuccessMessage', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, env)
  assert.equal(env.exports('approve', { nextStatus: 'PENDING_MATRIX_REVIEW' }), '本次审批已通过并签名；当前状态：待会签审核')
  assert.equal(env.exports('approve', { nextStatus: 'CONTROLLED_PENDING_EFFECTIVE' }), '本次审批已通过并签名；当前状态：受控（待生效）')
  assert.equal(env.exports('reject', { nextStatus: 'REJECTED' }), '本次任务已驳回并签名；当前状态：已驳回')
})

test('each signing dialog places opinion before masked required signature and confirmation before cancel', () => {
  for (const [name, opinion, submit, close] of [
    ['actionDialog', 'reason', 'submitActionDialog', 'closeActionDialog'],
    ['taskActionDialog', 'reason', 'submitTaskActionDialog', 'closeTaskActionDialog'],
    ['electronicReceiptDialog', 'comment', 'submitElectronicReceiptDialog', 'closeElectronicReceiptDialog']
  ]) {
    const start = source.indexOf(`v-model="${name}.visible"`)
    const dialog = source.slice(start, source.indexOf('</el-dialog>', start))
    assert.ok(dialog.indexOf(`v-model="${name}.form.${opinion}"`) < dialog.indexOf(`v-model="${name}.form.password"`))
    assert.match(dialog, /label="电子签名" required/)
    assert.match(dialog, /type="password"/)
    assert.ok(dialog.indexOf(`@click="${submit}"`) < dialog.indexOf(`@click="${close}"`))
    assert.ok(dialog.includes(`:disabled="${name}.submitting"`))
  }
  assert.match(source, /\.dcc-signature-dialog \.el-dialog__footer\s*{[^}]*justify-content: space-between/s)
})


test('signing footer buttons are siblings so cancel is always visible outside countdown content', () => {
  const ast = require('vue/compiler-sfc').compileTemplate({ source: descriptor.template.content, filename: 'detail.vue', id: 'detail' }).ast
  let count = 0
  function visit(node) {
    if (node.tag === 'el-dialog' && node.props?.some(p => p.name === 'class' && p.value?.content === 'dcc-signature-dialog')) {
      const footer = node.children.find(n => n.tag === 'template' && n.props.some(p => p.name === 'slot' && p.arg?.content === 'footer'))
      const buttons = footer.children.filter(n => n.tag === 'el-button')
      assert.equal(buttons.length, 2)
      assert.match(buttons[1].loc.source, />取消<\/el-button>/)
      count++
    }
    for (const child of node.children || []) visit(child)
    for (const branch of node.branches || []) visit(branch)
  }
  visit(ast)
  assert.equal(count, 3)
})
