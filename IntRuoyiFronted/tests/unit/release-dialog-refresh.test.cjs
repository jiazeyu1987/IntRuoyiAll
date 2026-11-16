const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref } = require('vue')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
function production(name, context) {
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === name))
  assert.ok(statement, name)
  const code = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(context), `${code}; return ${name}`)(...Object.values(context))
}
function setup() {
  const calls = []
  const context = {
    document: { hidden: false }, handoverRefreshInFlight: false, batchListRequestsInFlight: 0,
    releaseLoading: ref(false), marketReleaseRouteGeneration: 1,
    approvalInvalidation: { syncSession: () => ({ key: 'tenant:user' }), isCurrent: key => key === 'tenant:user' },
    route: { fullPath: '/release' }, selectedReleaseBatch: ref({ id: '800' }),
    releaseDialogVisible: ref(true), releaseCompletedReadOnly: ref(false), releaseAccessError: ref(''),
    releaseForm: { reason: '已核对正式资料', password: '12' },
    loadBatchList: async () => { calls.push('list'); return true },
    getMarketReleaseAccess: async params => { calls.push(params); return { visible: true, writable: true } },
    resolveErrorMessage: (error, message) => error.message || message
  }
  return { context, calls, refresh: () => production('refreshHandover', context) }
}

test('editing a release checks the exact batch and never closes the dialog because of pagination', async () => {
  const { context, calls, refresh } = setup()
  await refresh()(() => true)
  assert.deepEqual(calls, [{ batchExecutionId: '800' }])
  assert.equal(context.releaseDialogVisible.value, true)
  assert.equal(context.releaseForm.password, '12')
  assert.equal(context.releaseForm.reason, '已核对正式资料')
})

test('authorization request failure preserves the draft, blocks submit, and recovers only after a successful check', async () => {
  const { context, refresh } = setup()
  let fail = true
  context.getMarketReleaseAccess = async () => { if (fail) throw new Error('网络连接失败'); return { visible: true, writable: true } }
  const run = refresh()
  await run(() => true)
  assert.equal(context.releaseAccessError.value, '网络连接失败')
  assert.equal(context.releaseDialogVisible.value, true)
  assert.equal(context.releaseForm.reason, '已核对正式资料')
  await production('submitRelease', { ...context, releaseContextLoading: ref(false) })()
  fail = false
  await run(() => true)
  assert.equal(context.releaseAccessError.value, '')
})

test('actual revocation clears credentials but preserves the open draft', async () => {
  const { context, refresh } = setup()
  context.getMarketReleaseAccess = async () => ({ visible: true, writable: false })
  await refresh()(() => true)
  assert.match(context.releaseAccessError.value, /不可办理/)
  assert.equal(context.releaseForm.password, '')
  assert.equal(context.releaseForm.reason, '已核对正式资料')
  assert.equal(context.releaseDialogVisible.value, true)
})

test('overlapping refreshes and late responses cannot alter a different selected batch', async () => {
  const { context, refresh } = setup()
  let finish, calls = 0
  context.getMarketReleaseAccess = () => { calls++; return new Promise(resolve => { finish = resolve }) }
  const run = refresh()
  const pending = run(() => true)
  await run(() => true)
  assert.equal(calls, 1)
  context.selectedReleaseBatch.value = { id: '801' }
  finish({ visible: false, writable: false })
  await pending
  assert.equal(context.releaseAccessError.value, '')
  assert.equal(context.releaseForm.password, '12')
})

test('malformed access response blocks signing instead of implying authorization', async () => {
  const { context, refresh } = setup()
  context.getMarketReleaseAccess = async () => ({})
  await refresh()(() => true)
  assert.match(context.releaseAccessError.value, /无效结果/)
})

test('background list failure is reported without an unhandled rejection', async () => {
  const context = {
    batchListRequestsInFlight: 0, batchListReadGeneration: 0, buildQuery: () => ({}),
    approvalInvalidation: { syncSession: () => ({ key: 'tenant:user' }) }, route: { fullPath: '/release' },
    loading: ref(false), loadError: ref(''), list: ref([{ id: 800 }]), total: ref(1),
    getEdhrBatchExecutionPage: async () => { throw new Error('列表连接失败') },
    isVisibleBatchExecutionRow: () => true, resolveErrorMessage: e => e.message
  }
  assert.equal(await production('loadBatchList', context)(() => true, true), false)
  assert.equal(context.loadError.value, '列表连接失败')
})
