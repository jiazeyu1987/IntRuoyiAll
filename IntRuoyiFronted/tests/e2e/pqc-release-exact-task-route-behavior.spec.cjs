const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const tick = async () => { await vue.nextTick(); for (let i = 0; i < 20; i++) await Promise.resolve() }
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done }); return { promise, resolve } }
const formalRow = (overrides = {}) => ({ applicationId: '224', pqcReleaseWorkTaskId: '2734', version: 3, viewStatus: 'PENDING', applicationStatus: 'PQC_RELEASE_PENDING', activeOrderId: '1009200409', approvalReady: true, ...overrides })

function pageHarness(overrides = {}, query = { applicationId: '224', workTaskId: '2734' }) {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = new Set(['loading', 'loadError', 'list', 'total', 'activeView', 'releaseDialogVisible', 'releaseSubmitting', 'releaseError', 'selectedRow', 'releaseResult', 'releaseOutcomeUncertain', 'releaseIdempotencyKeys', 'listRequestSequence', 'queryParams', 'releaseForm', 'resolveErrorMessage', 'getList', 'releaseDialogGeneration', 'openReleaseDialog', 'resetReleaseDialog', 'readPqcReleaseRouteContext', 'openPqcReleaseFromRoute', 'pqcReleaseRouteGeneration'])
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement)
    ? statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast)))
    : ts.isExpressionStatement(statement) && /^(onMounted|watch|onBeforeUnmount)\(/.test(statement.getText(ast)))
  const mounted = [], unmounted = [], reads = [], writes = []
  const route = vue.reactive({ query })
  const context = {
    route, ...vue, userStore: { permissions: new Set(['mes:pro-production-release:pqc-approve']) },
    getPqcProductionReleasePage: async query => { reads.push(query); return { list: [formalRow()], total: 1 } },
    approvePqcProductionRelease: async request => { writes.push(request); throw Error('route entry must never approve') },
    PQC_RELEASE_VIEW_PENDING: 'PENDING', PQC_RELEASE_VIEW_RELEASED: 'RELEASED', PQC_RELEASE_VIEW_VOIDED: 'VOIDED', PQC_RELEASE_VIEW_REWORKED: 'REWORKED', PQC_RELEASE_VIEW_CONCESSION_RELEASED: 'CONCESSION_RELEASED',
    message: { error() {}, warning() {}, success() {} },
    onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn), ...overrides
  }
  const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
  const page = Function(...Object.keys(context), `${code}; return { loading, loadError, list, total, selectedRow, releaseDialogVisible, releaseForm, getList };`)(...Object.values(context))
  return { page, route, reads, writes, mount: async () => { for (const fn of mounted) await fn(); await tick() }, unmount: () => unmounted.forEach(fn => fn()) }
}

test('PQC exact task route queries both formal IDs before opening original read-only dialog', async () => {
  const h = pageHarness()
  await h.mount()
  assert.equal(h.reads.length, 1)
  assert.equal(h.reads[0].applicationId, '224')
  assert.equal(h.reads[0].pqcReleaseWorkTaskId, '2734')
  assert.equal(h.page.selectedRow.value?.applicationId, '224')
  assert.equal(h.page.selectedRow.value?.pqcReleaseWorkTaskId, '2734')
  assert.equal(h.page.releaseDialogVisible.value, true)
  assert.equal(h.page.releaseForm.signaturePassword, '')
  assert.equal(h.writes.length, 0)
})

test('PQC exact query preserves large string IDs without Number conversion', async () => {
  const row = formalRow({ applicationId: '9007199254740993', pqcReleaseWorkTaskId: '9007199254740995' })
  const reads = []
  const h = pageHarness({ getPqcProductionReleasePage: async query => { reads.push(query); return { list: [row], total: 1 } } })
  h.route.query = { applicationId: row.applicationId, workTaskId: row.pqcReleaseWorkTaskId }
  await h.mount()
  assert.equal(reads[0].applicationId, row.applicationId)
  assert.equal(reads[0].pqcReleaseWorkTaskId, row.pqcReleaseWorkTaskId)
  assert.equal(h.page.selectedRow.value?.applicationId, row.applicationId)
})

test('incomplete or multivalued PQC route fails visibly before any read or write', async () => {
  for (const query of [{ applicationId: '224' }, { workTaskId: '2734' }, { applicationId: ['224', '225'], workTaskId: '2734' }, { applicationId: '224', workTaskId: ['2734', '2735'] }, { applicationId: '0', workTaskId: '2734' }]) {
    const h = pageHarness()
    h.route.query = query
    await h.mount()
    assert.equal(h.reads.length, 0, '非法精确入口不得退回普通列表查询')
    assert.equal(h.page.releaseDialogVisible.value, false)
    assert.ok(h.page.loadError.value, '入口合同错误须在当前页面可见')
    assert.equal(h.writes.length, 0)
  }
})

test('PQC query does not select other applications, wrong tasks, duplicate or missing rows', async () => {
  for (const rows of [[formalRow({ applicationId: '225' })], [formalRow({ pqcReleaseWorkTaskId: '2735' })], [formalRow(), formalRow()], []]) {
    const h = pageHarness({ getPqcProductionReleasePage: async () => ({ list: rows, total: rows.length }) })
    await h.mount()
    assert.equal(h.page.selectedRow.value, undefined)
    assert.equal(h.page.releaseDialogVisible.value, false)
    assert.ok(h.page.loadError.value, '正式查询身份冲突或缺失不得当作成功入口')
    assert.equal(h.writes.length, 0)
  }
})

test('PQC automatic entry retains permission and explicit approval blocker guards', async () => {
  for (const row of [formalRow({ underReview: true }), formalRow({ approvalReady: false }), formalRow({ viewStatus: 'RELEASED', applicationStatus: 'REPORT_UPLOAD_PENDING' })]) {
    const h = pageHarness({ getPqcProductionReleasePage: async () => ({ list: [row], total: 1 }) })
    await h.mount()
    assert.equal(h.page.releaseDialogVisible.value, false)
    assert.ok(h.page.loadError.value)
  }
  const denied = pageHarness({ userStore: { permissions: new Set() } })
  await denied.mount()
  assert.equal(denied.page.releaseDialogVisible.value, false)
  assert.ok(denied.page.loadError.value)
})

test('PQC fast page omitted approvalReady keeps existing entry behavior', async () => {
  const row = formalRow()
  delete row.approvalReady
  const h = pageHarness({ getPqcProductionReleasePage: async () => ({ list: [row], total: 1 }) })
  await h.mount()
  assert.equal(h.page.releaseDialogVisible.value, true)
  assert.equal(h.page.selectedRow.value?.applicationId, '224')
})

test('ordinary work-order list route remains independent and does not auto-select a row', async () => {
  const h = pageHarness({}, { workOrderCode: 'FORMAL-WO' })
  await h.mount()
  assert.equal(h.reads[0].workOrderCode, 'FORMAL-WO')
  assert.equal(h.reads[0].applicationId, undefined)
  assert.equal(h.page.releaseDialogVisible.value, false)
})

test('late old PQC route response cannot replace the new exact task dialog', async () => {
  const old = deferred(), reads = []
  const h = pageHarness({ getPqcProductionReleasePage: async query => { reads.push(query); return reads.length === 1 ? old.promise : { list: [formalRow({ applicationId: '225', pqcReleaseWorkTaskId: '2736' })], total: 1 } } })
  const mounted = h.mount()
  await tick()
  h.route.query = { applicationId: '225', workTaskId: '2736' }
  await tick()
  old.resolve({ list: [formalRow()], total: 1 })
  await mounted
  await tick()
  assert.equal(h.page.selectedRow.value?.applicationId, '225')
  assert.equal(h.page.selectedRow.value?.pqcReleaseWorkTaskId, '2736')
  assert.equal(h.page.releaseDialogVisible.value, true)
  assert.equal(h.writes.length, 0)
})

test('late PQC route response after unmount cannot open a dialog', async () => {
  const response = deferred()
  const h = pageHarness({ getPqcProductionReleasePage: () => response.promise })
  const mounted = h.mount()
  await tick()
  h.unmount()
  response.resolve({ list: [formalRow()], total: 1 })
  await mounted
  assert.equal(h.page.releaseDialogVisible.value, false)
  assert.equal(h.page.selectedRow.value, undefined)
})

test('formal PQC notification path is in the shared exact-path allowlist', () => {
  const source = fs.readFileSync(path.join(root, 'src/utils/edhrWorkTaskNavigation.ts'), 'utf8')
  const exports = {}
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  Function('require', 'exports', code)(() => ({ openEdhrBatchTask: async () => { throw Error('PQC must not open FILL') } }), exports)
  assert.equal(exports.EDHR_WORK_TASK_NOTIFY_PATHS.has('/mes/production-release/pqc'), true)
  assert.equal(exports.EDHR_WORK_TASK_NOTIFY_PATHS.has('/mes/production-release/pqc/forged'), false)
})
