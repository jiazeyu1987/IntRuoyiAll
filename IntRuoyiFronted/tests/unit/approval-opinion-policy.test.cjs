const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const { createRequire } = require('node:module')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { computed, reactive, ref } = require('vue')
const Schema = createRequire(require.resolve('element-plus'))('async-validator').default

function declarations(file, names, runtime = {}) {
  const source = parse(fs.readFileSync(`src/views/${file}`, 'utf8')).descriptor.scriptSetup.content
  const ast = ts.createSourceFile(file + '.ts', source, ts.ScriptTarget.Latest, true)
  const selected = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.some(declaration => names.includes(declaration.name.text)))
  assert.equal(selected.length, names.length)
  const code = selected.map(statement => statement.getText(ast)).join('\n') + `\nexports = {${names.join(',')}}`
  const env = { computed, reactive, ref, ...runtime }
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  return env.exports
}

const blankReasons = ['', ' \t\n', '\u3000']

function dccActions() {
  const exports = {}, sent = []
  const source = fs.readFileSync('src/views/dcc/controlled-file/detail/approval-actions.ts', 'utf8')
  new Function('exports', 'require', ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText)(exports, name => {
    if (name.endsWith('/workflow')) {
      const transport = action => async (id, payload) => { sent.push({ action, payload }); return { controlledFileId: id } }
      return { approveControlledFileTask: transport('approve'), rejectControlledFileTask: transport('reject'),
        DccTaskActionError: Error, isControlledFileTaskPasswordInvalidError: () => false }
    }
    if (name === '../shared/lifecycle') return { getDccControlledFileStageByKey: () => undefined }
    throw Error(name)
  })
  return { ...exports, sent }
}

test('DCC sends empty approval opinions, blocks blank rejections, and still requires signing password', async () => {
  for (const reason of blankReasons) {
    const h = dccActions()
    const approved = await h.submitDccApprovalAction({ fileId: '10', taskId: 'task', action: 'approve', form: { password: 'secret', reason } })
    assert.equal(approved.success, true)
    assert.equal(h.sent[0].payload.reason, '')
    const rejected = await h.submitDccApprovalAction({ fileId: '10', taskId: 'task', action: 'reject', form: { password: 'secret', reason } })
    assert.equal(rejected.field, 'reason')
    assert.equal(h.sent.length, 1)
  }
  const h = dccActions()
  assert.ok(h.validateDccApprovalActionForm('approve', { password: ' ', reason: '' }).password)
  const result = await h.submitDccApprovalAction({ fileId: '10', taskId: 'task', action: 'reject', form: { password: 'secret', reason: ' 驳回说明 ' } })
  assert.equal(result.success, true)
  assert.equal(h.sent[0].payload.reason, '驳回说明')
})

test('BPM opinion rules ignore old node requirement and reject whitespace while preserving signature requirements', async () => {
  const rules = declarations('bpm/processInstance/detail/ProcessInstanceOperationButton.vue', ['approveReasonRule', 'rejectReasonRule'], {
    reasonRequire: ref(true), nodeTypeName: ref('审批')
  })
  assert.equal(rules.approveReasonRule.value.signPicUrl[0].required, true)
  assert.equal(rules.approveReasonRule.value.nextAssignees[0].required, true)
  for (const reason of blankReasons) {
    await new Schema({ reason: rules.approveReasonRule.value.reason || [] }).validate({ reason })
    await assert.rejects(new Schema({ reason: rules.rejectReasonRule.value.reason }).validate({ reason }))
  }
  await new Schema({ reason: rules.rejectReasonRule.value.reason }).validate({ reason: ' 拒绝说明 ' })
})

test('approval center submits empty APPROVE and blocks empty REJECT before transport', async () => {
  for (const result of ['APPROVE', 'REJECT']) for (const reason of [...blankReasons, ' 有效说明 ']) {
    const sent = [], errors = []
    const h = declarations('approval-center/index.vue', ['submitReview'], {
      reviewSubmitting: ref(false), reviewTask: ref({ moduleCode: 'BPM', sourceTaskId: 'task', availableActions: ['APPROVE', 'REJECT'] }),
      canReview: () => true, reviewForm: { result, reason, signaturePassword: 'secret' },
      ElMessage: { error: text => errors.push(text), success() {}, warning() {} },
      approvalInvalidation: { syncSession: () => ({ key: 'session' }), isCurrent: () => true },
      buildApprovalCenterRouteStateKey: () => 'route', reviewGeneration: 0, approvalTaskIdentityKey: () => 'task',
      completeApprovalWrite: async ({ write }) => write(),
      reviewApprovalTask: async payload => { sent.push(payload); return true }, resolveErrorMessage: error => error.message
    })
    await h.submitReview()
    assert.equal(sent.length, result === 'REJECT' && !reason.trim() ? 0 : 1)
    if (sent.length) assert.equal(sent[0].reason, reason.trim())
    else assert.match(errors[0], /必须填写原因/)
  }
})

test('project product review and approval both allow empty pass opinions and require reject reasons', async () => {
  for (const node of ['review', 'approve']) for (const approve of [true, false]) for (const reason of [...blankReasons, ' 有效说明 ', null]) {
    const sent = [], error = ref('')
    const transport = async (id, payload, pass) => { sent.push({ id, payload, pass }); return { id, status: 'REVIEWING' } }
    const h = declarations('dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue', ['handleProjectProductAction'], {
      projectProductRequestsLoading: ref(false), projectProductLoading: ref(false), projectProductDialogVisible: ref(true),
      canPerformProjectAction: () => true, projectProductDialogSequence: 0, projectProductActorScope: () => 'actor',
      projectProductError: error, projectProductActionReason: ref(''), window: { prompt: () => reason },
      reviewDccProjectProductRequest: transport, approveDccProjectProductRequest: transport,
      projectProductExactRequestId: ref(), projectProductRequests: ref([]), loadProjectProductRequests: async () => {},
      message: { success() {} }
    })
    await h.handleProjectProductAction({ id: '10' }, node, approve)
    assert.equal(sent.length, reason === null || (!approve && !reason.trim()) ? 0 : 1)
    if (sent.length) assert.equal(sent[0].payload.reason, reason.trim())
  }
})

test('MES unified change approval permits empty opinion and retains evidence and submission reason requirements', () => {
  const changeMode = ref('APPROVE'), changeError = ref('')
  const changeForm = { changeRequestId: 10, reason: '', approvalOpinion: '', signoffEvidenceHash: 'hash', idempotencyKey: 'key' }
  const h = declarations('mes/pro/edhr-unified-change/UnifiedChangePage.vue', ['validateActionForm'], { changeMode, changeError, changeForm })
  assert.equal(h.validateActionForm().approvalOpinion, '')
  changeForm.signoffEvidenceHash = ''
  assert.equal(h.validateActionForm(), undefined)
  assert.match(changeError.value, /签核证据/)
  changeForm.signoffEvidenceHash = 'hash'
  changeMode.value = 'SUBMIT'
  assert.equal(h.validateActionForm(), undefined)
  assert.match(changeError.value, /提交原因/)
})

for (const [file, name, mode] of [
  ['srm/supplier-access/index.vue', 'auditFormRules', 'auditAction'],
  ['srm/framework-plan/index.vue', 'auditRules', 'auditAction'],
  ['srm/procurement-plan/index.vue', 'auditRules', 'auditAction'],
  ['srm/supplier-portal/review/index.vue', 'auditRules', 'auditMode']
]) test(`${file} permits blank approval and rejects whitespace rejection`, async () => {
  const action = ref('approve')
  const h = declarations(file, [name], { [mode]: action })
  const suffixes = file.includes('supplier-access') ? ['', 'Sample', 'Trial'] : ['']
  for (const suffix of suffixes) {
    action.value = 'approve' + suffix
    for (const reason of blankReasons) await new Schema(h[name].value).validate({ auditRemark: reason })
    action.value = 'reject' + suffix
    for (const reason of blankReasons) await assert.rejects(new Schema(h[name].value).validate({ auditRemark: reason }))
    await new Schema(h[name].value).validate({ auditRemark: '有意义的原因' })
  }
})

test('payment approval permits blank explanation and its reject rule blocks whitespace', async () => {
  const h = declarations('srm/payment-execution/index.vue', ['approveRules', 'rejectRules'])
  for (const reason of blankReasons) {
    await new Schema(h.approveRules).validate({ approveRemark: reason })
    await assert.rejects(new Schema(h.rejectRules).validate({ rejectRemark: reason }))
  }
})

for (const [file, name, field] of [
  ['srm/purchase-order/my.vue', 'rejectRules', 'rejectRemark'],
  ['mall/trade/brokerage/withdraw/BrokerageWithdrawRejectForm.vue', 'formRules', 'auditReason']
]) test(`${file} reject validation blocks empty and whitespace-only reasons`, async () => {
  const h = declarations(file, [name])
  for (const reason of blankReasons) await assert.rejects(new Schema({ [field]: h[name][field] }).validate({ [field]: reason }))
  await new Schema({ [field]: h[name][field] }).validate({ [field]: '拒绝说明' })
})
