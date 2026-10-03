const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const origin = 'http://localhost:8081'
const listPath = '/mes/pro/feedback/edhr-batch-execution'

function navigation() {
  const source = fs.readFileSync(path.join(root, 'src/utils/edhrWorkTaskNavigation.ts'), 'utf8')
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const calls = []
  const exports = {}
  Function('require', 'exports', code)(() => ({ openEdhrBatchTask: async request => { calls.push(request); throw Error('manager task must never open FILL') } }), exports)
  return { ...exports, calls }
}
const task = (overrides = {}) => ({ id: '2735', taskType: 'RELEASE_APPROVE', businessScopeType: 'RELEASE_TRANSACTION', businessScopeId: '226', batchExecutionId: '900000001225', actionUrl: '/mes/pro/feedback/edhr-batch-execution/detail?id=900000001225&releaseTransactionId=226&focus=manager-release', ...overrides })

test('formal RELEASE_APPROVE scope directs existing task to exact market-release entry', async () => {
  const h = navigation(), routes = []
  await h.navigateToEdhrWorkTask({ push: async route => { routes.push(route) } }, task(), origin)
  assert.deepEqual(routes, [{ path: listPath, query: { batchExecutionId: '900000001225', releaseTransactionId: '226', workTaskId: '2735', action: 'marketRelease' } }])
  assert.equal(h.calls.length, 0)
})

test('new producer actionURL and exact IDs are preserved without Number conversion', () => {
  const h = navigation()
  const route = h.normalizeEdhrWorkTaskRouteParts(task({ id: '9007199254740993', actionUrl: `${listPath}?batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=9007199254740993&action=marketRelease` }), origin)
  assert.equal(route.path, listPath)
  assert.equal(route.query.workTaskId, '9007199254740993')
  assert.ok(h.EDHR_WORK_TASK_NOTIFY_PATHS.has(listPath), '新正式通知入口必须加入共享精确路径白名单')
  assert.equal(h.EDHR_WORK_TASK_NOTIFY_PATHS.has(`${listPath}/forged`), false)
})

test('manager URL conflicts with formal batch/transaction/task identity fail before navigation', async () => {
  for (const query of ['batchExecutionId=wrong&releaseTransactionId=226&workTaskId=2735', 'batchExecutionId=900000001225&releaseTransactionId=wrong&workTaskId=2735', 'batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=wrong']) {
    const h = navigation(), routes = []
    await assert.rejects(h.navigateToEdhrWorkTask({ push: async route => { routes.push(route) } }, task({ actionUrl: `${listPath}?${query}&action=marketRelease` }), origin))
    assert.equal(routes.length, 0)
    assert.equal(h.calls.length, 0)
  }
})

test('manager task requires formal scope and cannot navigate an external origin', () => {
  for (const overrides of [{ businessScopeId: undefined }, { businessScopeType: 'BATCH_TASK' }, { actionUrl: 'https://external.invalid/mes/pro/feedback/edhr-batch-execution' }]) {
    assert.throws(() => navigation().normalizeEdhrWorkTaskRouteParts(task(overrides), origin))
  }
})

test('manager entry rejects duplicate or conflicting legacy id and action parameters', () => {
  for (const actionUrl of [
    '/mes/pro/feedback/edhr-batch-execution/detail?id=900000001225&id=wrong&releaseTransactionId=226&focus=manager-release',
    '/mes/pro/feedback/edhr-batch-execution/detail?id=900000001225&id=900000001225&releaseTransactionId=226&focus=manager-release',
    `${listPath}?batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=2735&action=marketRelease&action=fill`,
    `${listPath}?batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=2735&action=fill`
  ]) {
    assert.throws(() => navigation().normalizeEdhrWorkTaskRouteParts(task({ actionUrl }), origin), '经理入口不得忽略多值或冲突动作')
  }
})

test('explicit FILL or REWORK cannot be reinterpreted as manager task while typed-less notification remains valid', () => {
  const actionUrl = `${listPath}?batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=2735&action=marketRelease`
  for (const taskType of ['FILL', 'REWORK']) {
    assert.throws(() => navigation().normalizeEdhrWorkTaskRouteParts(task({ taskType, actionUrl }), origin))
  }
  const route = navigation().normalizeEdhrWorkTaskRouteParts({ actionUrl }, origin)
  assert.equal(route.path, listPath)
  assert.equal(route.query.workTaskId, '2735')
})

test('existing independent FILL form navigation contract remains intact', () => {
  const route = navigation().normalizeEdhrWorkTaskRouteParts({ id: '12', taskType: 'FILL', executionId: '34', actionUrl: '/mes/pro/feedback/edhr-execution/detail?id=34' }, origin)
  assert.equal(route.path, '/mes/pro/feedback/edhr-execution/form')
  assert.equal(route.query.workTaskId, '12')
  assert.equal(route.query.executionId, '34')
})

const tick = async () => { for (let i = 0; i < 20; i++) await Promise.resolve() }
function pageHarness(overrides = {}, query = { batchExecutionId: '900000001225', releaseTransactionId: '226', workTaskId: '2735', action: 'marketRelease' }) {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = new Set(['getList', 'normalizeRouteQueryText', 'applyRouteQueryFilters', 'openReleaseDialog', 'submitRelease', 'openMarketReleaseFromRoute', 'readMarketReleaseRouteContext', 'marketReleaseRouteGeneration', 'resetMarketReleaseDialog'])
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement) ? statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))) : ts.isExpressionStatement(statement) && (statement.getText(ast).startsWith('onMounted(') || statement.getText(ast).startsWith('onBeforeUnmount(') || (statement.getText(ast).startsWith('watch(') && /openMarketReleaseFromRoute|releaseDialogVisible/.test(statement.getText(ast)))))
  const mounted = [], unmounted = [], reads = [], writes = [], permission = new Set(['mes:pro-edhr-release:approve'])
  const route = vue.reactive({ query })
  const batch = { id: '900000001225', batchExecutionCode: 'OWNED-1225', releaseActionLocked: true, status: 15 }
  const formal = { batchExecutionId: '900000001225', releaseTransactionId: '226', releaseApprovalWorkTaskId: '2735', releaseStatus: 'PENDING_APPROVAL', version: 7, approvalSignoffEvidenceHash: 'exact-hash' }
  const context = {
    route, userStore: { permissions: permission }, hasGoldenFingerActionBypass: vue.ref(false),
    queryParams: vue.reactive({ pageNo: 1, pageSize: 10, batchExecutionCode: '', workOrderCode: '', batchCode: '' }),
    loading: vue.ref(false), loadError: vue.ref(''), list: vue.ref([]), total: vue.ref(0),
    buildQuery: () => ({ pageNo: 1, pageSize: 10 }),
    getEdhrBatchExecutionPage: async () => ({ list: [{ id: 'wrong' }], total: 1 }),
    getEdhrBatchExecution: async id => { reads.push(['batch', id]); return batch },
    getEdhrRelease: async id => { reads.push(['transaction', id]); return formal },
    getEdhrReleasePage: async () => { throw Error('deep link must use exact transaction read') },
    selectedReleaseBatch: vue.ref(), releaseContext: vue.ref(), releaseTransactionMissing: vue.ref(false), releaseError: vue.ref(''),
    releaseForm: vue.reactive({ password: '', idempotencyKey: '' }), releaseDialogVisible: vue.ref(false), releaseContextLoading: vue.ref(false), releaseLoading: vue.ref(false),
    generateUUID: () => 'route-key', message: { error() {}, success() {} }, router: { push: async () => {} },
    approveEdhrRelease: async request => { writes.push(request) },
    isVoidedBatchExecutionStatus: status => String(status) === '60',
    resolveErrorMessage: (error, defaultMessage) => error.message || defaultMessage,
    watch: vue.watch, onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn), ...overrides
  }
  const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
  const page = Function(...Object.keys(context), `${code}; return { submitRelease, openReleaseDialog };`)(...Object.values(context))
  return { page, context, reads, writes, route, mounted, batch, formal, mount: async () => { for (const fn of mounted) await fn(); await tick() }, unmount: () => unmounted.forEach(fn => fn()) }
}

test('batch-list deep link reads exact batch and transaction and opens read-only formal dialog', async () => {
  const h = pageHarness()
  await h.mount()
  assert.deepEqual(h.reads, [['batch', '900000001225'], ['transaction', '226']])
  assert.equal(h.context.selectedReleaseBatch.value.id, '900000001225')
  assert.equal(h.context.releaseContext.value.releaseApprovalWorkTaskId, '2735')
  assert.equal(h.context.releaseDialogVisible.value, true)
  assert.equal(h.writes.length, 0)
})

test('deep-link mismatches and missing permission do not enable formal approval', async () => {
  for (const change of [{ batchExecutionId: 'wrong' }, { releaseTransactionId: 'wrong' }, { releaseApprovalWorkTaskId: 'wrong' }, { releaseStatus: 'RELEASED' }]) {
    const h = pageHarness({ getEdhrRelease: async () => ({ batchExecutionId: '900000001225', releaseTransactionId: '226', releaseApprovalWorkTaskId: '2735', releaseStatus: 'PENDING_APPROVAL', ...change }) })
    await h.mount()
    assert.equal(h.context.releaseContext.value, undefined)
    assert.ok(h.context.releaseError.value || h.context.loadError.value, '正式身份冲突应显示明确错误')
    assert.equal(h.writes.length, 0)
  }
  const denied = pageHarness({ userStore: { permissions: new Set() } })
  await denied.mount()
  assert.equal(denied.context.releaseContext.value, undefined)
  assert.ok(denied.context.releaseError.value || denied.context.loadError.value)
  assert.equal(denied.reads.length, 0)
})

test('malformed manager query cannot select the first row or silently ignore identity', async () => {
  const h = pageHarness()
  h.route.query.workTaskId = ['2735', 'forged']
  await h.mount()
  assert.equal(h.reads.length, 0)
  assert.equal(h.context.releaseContext.value, undefined)
  assert.ok(h.context.releaseError.value || h.context.loadError.value)
})

const deferred = () => { let resolve, reject; const promise = new Promise((done, fail) => { resolve = done; reject = fail }); return { promise, resolve, reject } }
const otherBatch = { id: '900000001226', batchExecutionCode: 'OWNED-1226', status: 15, releaseActionLocked: true }
const otherFormal = { batchExecutionId: otherBatch.id, releaseTransactionId: '227', releaseApprovalWorkTaskId: '2736', releaseStatus: 'PENDING_APPROVAL', version: 9, approvalSignoffEvidenceHash: 'other-exact-hash' }

test('late route transaction cannot replace manually selected batch context or submit mixed identities', async () => {
  const old = deferred(), navigations = []
  const h = pageHarness({ getEdhrRelease: () => old.promise, getEdhrReleasePage: async () => ({ list: [otherFormal], total: 1 }), router: { push: async route => navigations.push(route) } })
  await h.mount()
  await h.page.openReleaseDialog(otherBatch)
  old.resolve(h.formal)
  await tick()
  assert.equal(h.context.selectedReleaseBatch.value.id, otherBatch.id)
  assert.equal(h.context.releaseContext.value.releaseTransactionId, '227', '晚到的 routeA 事务不得覆盖手动 rowB 弹窗')
  h.context.releaseForm.password = 'test-password'
  await h.page.submitRelease()
  assert.equal(h.writes.length, 1)
  assert.equal(h.writes[0].releaseTransactionId, '227')
  assert.equal(h.writes[0].workTaskId, '2736')
  assert.equal(h.writes[0].expectedVersion, 9)
  assert.equal(h.writes[0].signoffEvidenceHash, 'other-exact-hash')
  assert.match(h.writes[0].idempotencyKey, new RegExp(otherBatch.id))
  assert.equal(navigations[0].query.batchExecutionId, otherBatch.id)
})

test('cancel invalidates the pending exact transaction and clears signature material', async () => {
  const old = deferred(), h = pageHarness({ getEdhrRelease: () => old.promise })
  await h.mount()
  h.context.releaseForm.password = 'test-password'
  h.context.releaseDialogVisible.value = false
  await tick()
  old.resolve(h.formal)
  await tick()
  assert.equal(h.context.releaseContext.value, undefined)
  assert.equal(h.context.selectedReleaseBatch.value, undefined)
  assert.equal(h.context.releaseForm.password, '')
  assert.equal(h.context.releaseContextLoading.value, false)
  assert.equal(h.writes.length, 0)
})

test('unmount invalidates pending manager transaction before it can set approval context', async () => {
  const old = deferred(), h = pageHarness({ getEdhrRelease: () => old.promise })
  await h.mount()
  h.unmount()
  old.resolve(h.formal)
  await tick()
  assert.equal(h.context.releaseContext.value, undefined)
  assert.equal(h.context.selectedReleaseBatch.value, undefined)
  assert.equal(h.context.releaseForm.password, '')
  assert.equal(h.writes.length, 0)
})

test('leaving exact manager route closes and clears its previous approval dialog', async () => {
  const h = pageHarness()
  await h.mount()
  h.context.releaseForm.password = 'test-password'
  h.route.query = {}
  await tick()
  assert.equal(h.context.releaseDialogVisible.value, false)
  assert.equal(h.context.releaseContext.value, undefined)
  assert.equal(h.context.selectedReleaseBatch.value, undefined)
  assert.equal(h.context.releaseForm.password, '')
  assert.equal(h.writes.length, 0)
})

test('old approval success cannot close new manager dialog or navigate its batch', async () => {
  const old = deferred(), navigations = []
  const h = pageHarness({ approveEdhrRelease: () => old.promise, getEdhrReleasePage: async () => ({ list: [otherFormal], total: 1 }), router: { push: async route => navigations.push(route) } })
  await h.mount()
  h.context.releaseForm.password = 'old-test-password'
  const submitted = h.page.submitRelease()
  await h.page.openReleaseDialog(otherBatch)
  h.context.releaseForm.password = 'new-test-password'
  old.resolve({})
  await submitted
  assert.equal(h.context.releaseDialogVisible.value, true)
  assert.equal(h.context.releaseContext.value.releaseTransactionId, '227')
  assert.equal(h.context.releaseForm.password, 'new-test-password')
  assert.equal(navigations.length, 0)
})

test('old approval failure and finally cannot corrupt current manager submission state', async () => {
  const old = deferred(), current = deferred()
  let calls = 0
  const h = pageHarness({ approveEdhrRelease: () => ++calls === 1 ? old.promise : current.promise, getEdhrReleasePage: async () => ({ list: [otherFormal], total: 1 }) })
  await h.mount()
  h.context.releaseForm.password = 'old-test-password'
  const first = h.page.submitRelease()
  await h.page.openReleaseDialog(otherBatch)
  h.context.releaseForm.password = 'new-test-password'
  const second = h.page.submitRelease()
  old.reject(Error('old submission failed'))
  await first
  assert.equal(h.context.releaseError.value, '')
  assert.equal(h.context.releaseLoading.value, true)
  current.reject(Error('current submission failed'))
  await second
  assert.equal(h.context.releaseError.value, 'current submission failed')
  assert.equal(h.context.releaseLoading.value, false)
})

const duplicateActions = [
  'action=fill&action=marketRelease',
  'action=marketRelease&action=fill',
  'action=marketRelease&action=marketRelease',
  'action=fill&action=fill'
]
const duplicatedManagerItem = (taskType, executionId, actions) => {
  const actionUrl = `${listPath}?batchExecutionId=900000001225&releaseTransactionId=226&workTaskId=2735&${actions}${executionId ? `&executionId=${executionId}` : ''}`
  return taskType === undefined
    ? { actionUrl, ...(executionId ? { executionId } : {}) }
    : task({ taskType, actionUrl, ...(executionId ? { executionId } : {}) })
}

for (const taskType of ['FILL', 'REWORK', undefined]) {
  const label = taskType || 'typed-less notification'
  test(`${label} duplicate manager action rejects every ordering before normalization with or without executionId`, () => {
    for (const executionId of [undefined, '34']) {
      for (const actions of duplicateActions) {
        const h = navigation()
        assert.throws(() => h.normalizeEdhrWorkTaskRouteParts(duplicatedManagerItem(taskType, executionId, actions), origin), `${label}: ${actions}; executionId=${executionId || 'absent'}`)
        assert.equal(h.calls.length, 0)
      }
    }
  })

  test(`${label} duplicate action cannot be collapsed into an actual manager dialog`, async () => {
    for (const executionId of [undefined, '34']) {
      const h = navigation(), routes = [], pages = []
      let rejected = false
      try {
        await h.navigateToEdhrWorkTask({ push: async target => {
          routes.push(target)
          if (target.path === listPath) {
            const page = pageHarness({}, target.query)
            pages.push(page)
            await page.mount()
          }
        } }, duplicatedManagerItem(taskType, executionId, 'action=fill&action=marketRelease'), origin)
      } catch (error) {
        rejected = true
        assert.ok(error instanceof Error)
      }
      for (const page of pages) {
        assert.equal(page.context.releaseDialogVisible.value, false, `${label}: helper首值/末值差异不得打开真实经理签名弹窗`)
        assert.equal(page.context.releaseContext.value, undefined)
        assert.equal(page.reads.length, 0)
        assert.equal(page.writes.length, 0)
        page.unmount()
      }
      assert.equal(rejected, true, `${label}: 重复动作必须在目标导航前拒绝`)
      assert.equal(routes.length, 0)
      assert.equal(h.calls.length, 0)
    }
  })
}
