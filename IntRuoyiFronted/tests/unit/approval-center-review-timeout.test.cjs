const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const filename = path.join(__dirname, '../../src/views/approval-center/index.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
compileScript(descriptor, { id: 'approval-center-review-timeout' })
const ast = ts.createSourceFile('approval.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['submitReview', 'resetReviewRequestContext', 'resolveErrorMessage'])
const source = ast.statements.filter(statement => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
  .map(statement => statement.getText(ast)).join('\n')
const state = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(__dirname,
  '../../src/store/modules/approvalCenterInvalidationState.ts'), 'utf8'),
{ compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, state)

function handler({ write = async () => true, refresh = async () => true } = {}) {
  const calls = { writes: 0, refreshes: 0, successes: [], warnings: [], errors: [], publishes: 0 }
  let currentRoute = 'route-29'
  const env = {
    Error, String, Object, ...state.exports,
    reviewGeneration: 0, reviewSubmitting: vue.ref(false), reviewDialogVisible: vue.ref(true),
    reviewTask: vue.ref({ moduleCode: 'BPM', sourceTaskType: 'BPM_TASK_TODO', sourceTaskId: 'task-29',
      processInstanceId: 'process-29', availableActions: ['APPROVE', 'REJECT'] }),
    reviewForm: vue.reactive({ result: 'APPROVE', reason: '', signaturePassword: 'unit-only', passwordFocused: true }),
    reviewPreview: vue.ref(undefined), reviewPreviewError: vue.ref(''), reviewIdempotencyKey: vue.ref('key-29'),
    loadError: vue.ref(''), canReview: () => true,
    buildApprovalCenterRouteStateKey: () => currentRoute,
    approvalInvalidation: { syncSession: () => ({ key: 'session' }), isCurrent: () => true,
      captureEdhrSuccess: () => ({}), publishEdhrSuccess: () => { calls.publishes++ } },
    reviewApprovalTask: async () => { calls.writes++; return write(env) },
    getList: async badge => { assert.equal(badge, true); calls.refreshes++; return refresh(env) },
    ElMessage: { error: message => calls.errors.push(message), warning: message => calls.warnings.push(message),
      success: message => calls.successes.push(message) }
  }
  vm.runInNewContext(ts.transpileModule(source + '\nglobalThis.submit = submitReview; globalThis.reset = resetReviewRequestContext',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  vue.watch(env.reviewDialogVisible, visible => { if (!visible) env.reset() }, { flush: 'sync' })
  return { env, calls, submit: env.submit, changeRoute: () => { currentRoute = 'another-route' } }
}

for (const code of ['ECONNABORTED', 'ETIMEDOUT']) {
  test(`${code} closes signature and reads results without retrying or claiming success`, async () => {
    const h = handler({ write: async () => { throw Object.assign(new Error('timeout of 30000ms exceeded'), { code }) } })
    await h.submit()
    assert.equal(h.calls.writes, 1)
    assert.equal(h.calls.refreshes, 1)
    assert.deepEqual(h.calls.successes, [])
    assert.equal(h.calls.publishes, 0)
    assert.deepEqual(h.calls.warnings, ['办理结果待确认，请刷新核对审批结果后再操作'])
    assert.deepEqual(h.calls.errors, [])
    assert.equal(h.env.reviewDialogVisible.value, false)
    assert.equal(h.env.reviewForm.signaturePassword, '')
    assert.equal(h.env.reviewForm.passwordFocused, false)
    assert.equal(h.env.reviewSubmitting.value, false)
  })
}

test('a failed list read remains explicitly visible and never becomes successful approval', async () => {
  const h = handler({ write: async () => { throw { code: 'ECONNABORTED' } },
    refresh: async env => { env.loadError.value = '查询权限已变化'; return false } })
  await h.submit()
  assert.deepEqual(h.calls.errors, ['审批列表刷新失败：查询权限已变化'])
  assert.deepEqual(h.calls.successes, [])
  assert.equal(h.calls.writes, 1)
})

test('an unexpected list exception is displayed after uncertain approval', async () => {
  const h = handler({ write: async () => { throw { code: 'ETIMEDOUT' } },
    refresh: async () => { throw new Error('连接中断') } })
  await h.submit()
  assert.deepEqual(h.calls.errors, ['审批列表刷新失败：连接中断'])
  assert.equal(h.env.reviewDialogVisible.value, false)
  assert.equal(h.calls.writes, 1)
})

test('business denial keeps its specific error and current signature dialog', async () => {
  const h = handler({ write: async () => { throw Object.assign(new Error('没有注册证审批权限'), { code: 403 }) } })
  await h.submit()
  assert.deepEqual(h.calls.errors, ['没有注册证审批权限'])
  assert.deepEqual(h.calls.warnings, [])
  assert.equal(h.env.reviewDialogVisible.value, true)
  assert.equal(h.calls.refreshes, 0)
  assert.equal(h.env.reviewSubmitting.value, false)
})

test('confirmed review still reports success and refreshes once', async () => {
  const h = handler()
  await h.submit()
  assert.deepEqual(h.calls.successes, ['审核已通过'])
  assert.deepEqual(h.calls.warnings, [])
  assert.equal(h.calls.writes, 1)
  assert.equal(h.calls.refreshes, 1)
  assert.equal(h.env.reviewDialogVisible.value, false)
})

test('a late timeout cannot close or refresh another route', async () => {
  let reject
  const h = handler({ write: () => new Promise((_, fail) => { reject = fail }) })
  const pending = h.submit()
  h.changeRoute()
  reject({ code: 'ECONNABORTED' })
  await pending
  assert.equal(h.calls.refreshes, 0)
  assert.equal(h.env.reviewDialogVisible.value, true)
  assert.deepEqual(h.calls.warnings, [])
  assert.deepEqual(h.calls.errors, [])
})

test('a refresh failure after navigation does not report against the new page', async () => {
  let failRead
  const h = handler({ write: async () => { throw { code: 'ECONNABORTED' } },
    refresh: () => new Promise((_, reject) => { failRead = reject }) })
  const pending = h.submit()
  while (!failRead) await Promise.resolve()
  h.changeRoute()
  failRead(new Error('旧页面查询失败'))
  await pending
  assert.deepEqual(h.calls.errors, [])
  assert.equal(h.calls.writes, 1)
})
