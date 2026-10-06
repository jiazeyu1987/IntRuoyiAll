const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const tick = async () => { await vue.nextTick(); for (let i = 0; i < 20; i++) await Promise.resolve() }
const deferred = () => { let resolve, reject; const promise = new Promise((done, fail) => { resolve = done; reject = fail }); return { promise, resolve, reject } }
const formalRow = (overrides = {}) => ({ applicationId: '224', pqcReleaseWorkTaskId: '2734', version: 3, viewStatus: 'PENDING', applicationStatus: 'PQC_RELEASE_PENDING', activeOrderId: '1009200409', approvalReady: true, ...overrides })
const formalDecision = (overrides = {}) => ({ applicationId: '224', pqcReleaseWorkTaskId: '2734', decision: 'APPROVE', status: 'MANAGER_RELEASE_PENDING', version: 4, signatureId: 29024, decidedBy: '9908090346', decidedAt: 1791232971334, batchExecutionId: '900000001232', sourceSnapshotHash: 'c'.repeat(64), ...overrides })
const formalDetail = (overrides = {}) => ({ detail: { activeOrderId: '1009200409', workOrderId: '990274', activeOrderStatus: { status: 'RELEASED' }, pqcProductionRelease: { status: 'MANAGER_RELEASE_PENDING', signature: { signatureId: 29024, role: 'PQC_RELEASE', signerName: '正式签署人', signedAt: 1791232971334 } }, ...overrides }, productionMaterialLists: [] })

function pageHarness(overrides = {}, query = { applicationId: '224', workTaskId: '2734' }) {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/production-release/PqcProductionReleasePage.vue'), 'utf8')
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = new Set(['loading', 'loadError', 'list', 'total', 'activeView', 'releaseDialogVisible', 'releaseSubmitting', 'releaseError', 'selectedRow', 'releaseResult', 'releaseOutcomeUncertain', 'releaseIdempotencyKeys', 'listRequestSequence', 'queryParams', 'releaseForm', 'resolveErrorMessage', 'getList', 'releaseDialogGeneration', 'openReleaseDialog', 'resetReleaseDialog', 'readPqcReleaseRouteContext', 'openPqcReleaseFromRoute', 'pqcReleaseRouteGeneration', 'detailVisible', 'detailLoading', 'detailError', 'detailRow', 'detailApplicationId', 'detailReadOnly', 'orderDetail', 'detailRequestSequence', 'clearOrderDetail', 'isExactPositiveId', 'isFormalSignedTime', 'requirePqcReleaseAuthority', 'openCompletedPqcReleaseDetail', 'hasActiveOrderDetail', 'openActiveOrderDetail', 'retryOrderDetail'])
  const statements = ast.statements.filter(statement => ts.isVariableStatement(statement)
    ? statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast)))
    : ts.isExpressionStatement(statement) && /^(onMounted|watch|onBeforeUnmount)\(/.test(statement.getText(ast)))
  const mounted = [], unmounted = [], reads = [], authorityReads = [], detailReads = [], writes = []
  const route = vue.reactive({ query })
  const context = {
    route, ...vue, userStore: { permissions: new Set(['mes:pro-production-release:query', 'mes:pro-production-release:pqc-approve']) },
    getPqcProductionRelease: async applicationId => { authorityReads.push(applicationId); return { applicationId, pqcReleaseWorkTaskId: route.query.workTaskId, status: 'PQC_RELEASE_PENDING' } },
    getPqcProductionReleaseOrderDetail: async applicationId => { detailReads.push(applicationId); return formalDetail() },
    getPqcProductionReleasePage: async query => { reads.push(query); return { list: [formalRow()], total: 1 } },
    approvePqcProductionRelease: async request => { writes.push(request); throw Error('route entry must never approve') },
    PQC_RELEASE_VIEW_PENDING: 'PENDING', PQC_RELEASE_VIEW_RELEASED: 'RELEASED', PQC_RELEASE_VIEW_VOIDED: 'VOIDED', PQC_RELEASE_VIEW_REWORKED: 'REWORKED', PQC_RELEASE_VIEW_CONCESSION_RELEASED: 'CONCESSION_RELEASED',
    message: { error() {}, warning() {}, success() {} },
    onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn), ...overrides
  }
  const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
  const page = Function(...Object.keys(context), `${code}; return { loading, loadError, list, total, selectedRow, releaseDialogVisible, releaseForm, getList, activeView, detailVisible, detailLoading, detailError, detailApplicationId, detailReadOnly, orderDetail, openPqcReleaseFromRoute, openActiveOrderDetail, retryOrderDetail };`)(...Object.values(context))
  return { page, route, reads, authorityReads, detailReads, writes, mount: async () => { for (const fn of mounted) await fn(); await tick() }, unmount: () => unmounted.forEach(fn => fn()) }
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

function doneHarness(overrides = {}, query = { applicationId: '224', workTaskId: '2734' }) {
  const authorityReads = [], detailReads = []
  const h = pageHarness({
    userStore: { permissions: new Set(['mes:pro-production-release:query']) },
    getPqcProductionRelease: async id => { authorityReads.push(id); return formalDecision() },
    getPqcProductionReleaseOrderDetail: async id => { detailReads.push(id); return formalDetail() },
    ...overrides
  }, query)
  return { ...h, authorityReads, detailReads }
}

test('DONE candidate opens official read-only detail using original receipt and actual current order status', async () => {
  // The official PQC detail summary is the frozen decision; the order status is current.
  const h = doneHarness()
  await h.mount()
  assert.deepEqual(h.authorityReads, ['224'])
  assert.deepEqual(h.detailReads, ['224'])
  assert.equal(h.reads.length, 0, '已办入口不得猜已放行标签或执行待放行列表查询')
  assert.equal(h.page.detailVisible.value, true)
  assert.equal(h.page.detailReadOnly.value, true)
  assert.equal(h.page.detailApplicationId.value, '224')
  assert.equal(h.page.orderDetail.value.detail.activeOrderStatus.status, 'RELEASED')
  assert.equal(h.page.orderDetail.value.detail.pqcProductionRelease.status, 'MANAGER_RELEASE_PENDING')
  assert.equal(h.page.activeView.value, 'PENDING', '原回执状态不能推导当前列表标签')
  assert.equal(h.page.releaseDialogVisible.value, false)
  assert.equal(h.page.selectedRow.value, undefined)
  assert.equal(h.page.detailError.value, '')
  assert.equal(h.writes.length, 0)
})

test('DONE candidate may read a different candidate signer without approval permission', async () => {
  const h = doneHarness({
    userStore: { permissions: new Set(['mes:pro-production-release:query']), user: { id: '9908090999' } },
    getPqcProductionRelease: async () => formalDecision({ decidedBy: '9908090346' })
  })
  await h.mount()
  assert.equal(h.page.detailVisible.value, true)
  assert.ok(h.page.orderDetail.value)
  assert.equal(h.page.releaseDialogVisible.value, false)
  assert.equal(h.writes.length, 0)
})

test('query-only permission does not authorize the original PENDING approval dialog', async () => {
  const h = pageHarness({ userStore: { permissions: new Set(['mes:pro-production-release:query']) } })
  await h.mount()
  assert.deepEqual(h.authorityReads, ['224'])
  assert.equal(h.reads.length, 1)
  assert.equal(h.page.releaseDialogVisible.value, false)
  assert.match(h.page.loadError.value, /没有生产放行权限/)
  assert.equal(h.detailReads.length, 0)
  assert.equal(h.writes.length, 0)
})

test('DONE invalid original authority fails before detail and never queries a fallback list', async () => {
  for (const invalid of [
    { applicationId: '225' }, { pqcReleaseWorkTaskId: '2735' },
    { applicationId: 9007199254740992 }, { decision: 'NONCONFORMANCE_VOID' },
    { status: 'PQC_RELEASE_PENDING' }, { status: 'PQC_RELEASE_REJECTED' },
    { signatureId: null }, { signatureId: 0 }, { decidedBy: undefined },
    { batchExecutionId: undefined }, { decidedAt: null }, { decidedAt: 'not-a-time' },
    { version: 0 }, { version: '4' }, { sourceSnapshotHash: '' }
  ]) {
    const h = doneHarness({ getPqcProductionRelease: async () => formalDecision(invalid) })
    await h.mount()
    assert.equal(h.detailReads.length, 0, JSON.stringify(invalid))
    assert.equal(h.reads.length, 0)
    assert.equal(h.page.detailVisible.value, false)
    assert.ok(h.page.loadError.value)
    assert.equal(h.writes.length, 0)
  }
})

test('query permission denial and failed formal authority stay visible without a page fallback', async () => {
  const denied = doneHarness({ userStore: { permissions: new Set(['mes:pro-production-release:pqc-approve']) } })
  await denied.mount()
  assert.equal(denied.authorityReads.length, 0)
  assert.equal(denied.detailReads.length, 0)
  assert.equal(denied.reads.length, 0)
  assert.match(denied.page.loadError.value, /查询权限/)
  const failed = doneHarness({ getPqcProductionRelease: async () => { throw Error('候选池读取拒绝') } })
  await failed.mount()
  assert.equal(failed.page.loadError.value, '候选池读取拒绝')
  assert.equal(failed.detailReads.length, 0)
  assert.equal(failed.reads.length, 0)
})

test('official DONE detail requires same formal signature and frozen summary before showing data', async () => {
  const valid = formalDetail().detail.pqcProductionRelease
  for (const detail of [
    { activeOrderId: null }, { workOrderId: null },
    { pqcProductionRelease: { ...valid, status: 'RELEASED' } },
    { pqcProductionRelease: { ...valid, signature: { ...valid.signature, signatureId: 29025 } } },
    { pqcProductionRelease: { ...valid, signature: { ...valid.signature, role: 'MARKET_RELEASE' } } },
    { pqcProductionRelease: { ...valid, signature: { ...valid.signature, signerName: '' } } },
    { pqcProductionRelease: { ...valid, signature: { ...valid.signature, signedAt: null } } }
  ]) {
    const h = doneHarness({ getPqcProductionReleaseOrderDetail: async () => formalDetail(detail) })
    await h.mount()
    assert.equal(h.page.detailVisible.value, true)
    assert.equal(h.page.detailReadOnly.value, true)
    assert.equal(h.page.orderDetail.value, undefined)
    assert.match(h.page.detailError.value, /有效电子签名/)
    assert.equal(h.reads.length, 0)
    assert.equal(h.writes.length, 0)
  }
})

test('failed official detail retains a read-only error view and retry re-authorizes the same route', async () => {
  let attempts = 0
  const h = doneHarness({ getPqcProductionReleaseOrderDetail: async () => {
    if (++attempts === 1) throw Error('正式签名核验失败')
    return formalDetail()
  } })
  await h.mount()
  assert.equal(h.page.detailError.value, '正式签名核验失败')
  assert.equal(h.page.orderDetail.value, undefined)
  assert.equal(h.page.detailReadOnly.value, true)
  h.page.retryOrderDetail()
  await tick()
  assert.deepEqual(h.authorityReads, ['224', '224'])
  assert.equal(attempts, 2)
  assert.equal(h.page.detailReadOnly.value, true)
  assert.equal(h.page.detailError.value, '')
  assert.ok(h.page.orderDetail.value)
  assert.equal(h.reads.length, 0)
  assert.equal(h.writes.length, 0)
})

test('late DONE authority success or error cannot replace a newer exact pending route', async () => {
  for (const fail of [false, true]) {
    const old = deferred()
    const h = doneHarness({
      userStore: { permissions: new Set(['mes:pro-production-release:query', 'mes:pro-production-release:pqc-approve']) },
      getPqcProductionRelease: id => id === '224' ? old.promise : Promise.resolve({ applicationId: id, pqcReleaseWorkTaskId: '2736', status: 'PQC_RELEASE_PENDING' }),
      getPqcProductionReleasePage: async () => ({ list: [formalRow({ applicationId: '225', pqcReleaseWorkTaskId: '2736' })], total: 1 })
    })
    const mounted = h.mount()
    await tick()
    h.route.query = { applicationId: '225', workTaskId: '2736' }
    await tick()
    fail ? old.reject(Error('旧权限拒绝')) : old.resolve(formalDecision())
    await mounted
    assert.equal(h.page.selectedRow.value?.applicationId, '225')
    assert.equal(h.page.releaseDialogVisible.value, true)
    assert.equal(h.page.detailVisible.value, false)
    assert.equal(h.page.loadError.value, '')
    assert.equal(h.detailReads.length, 0)
  }
})

test('late DONE detail success or error cannot overwrite a newer DONE detail', async () => {
  for (const fail of [false, true]) {
    const old = deferred()
    const current = formalDetail({ activeOrderId: '1009200410', pqcProductionRelease: { status: 'MANAGER_RELEASE_PENDING', signature: { signatureId: 29025, role: 'PQC_RELEASE', signerName: '新签署人', signedAt: 1791232971334 } } })
    const h = doneHarness({
      getPqcProductionRelease: async id => formalDecision({ applicationId: id, pqcReleaseWorkTaskId: id === '224' ? '2734' : '2736', signatureId: id === '224' ? 29024 : 29025 }),
      getPqcProductionReleaseOrderDetail: id => id === '224' ? old.promise : Promise.resolve(current)
    })
    const mounted = h.mount()
    await tick()
    assert.equal(h.page.detailLoading.value, true)
    h.route.query = { applicationId: '225', workTaskId: '2736' }
    await tick()
    fail ? old.reject(Error('旧详情失败')) : old.resolve(formalDetail())
    await mounted
    assert.equal(h.page.detailApplicationId.value, '225')
    assert.equal(h.page.orderDetail.value.detail.activeOrderId, '1009200410')
    assert.equal(h.page.detailError.value, '')
    assert.equal(h.page.detailLoading.value, false)
    assert.equal(h.page.detailReadOnly.value, true)
  }
})

test('unmount clears DONE detail and invalidates outstanding authority and detail responses', async () => {
  for (const phase of ['authority', 'detail']) {
    const old = deferred()
    const h = doneHarness(phase === 'authority'
      ? { getPqcProductionRelease: () => old.promise }
      : { getPqcProductionReleaseOrderDetail: () => old.promise })
    const mounted = h.mount()
    await tick()
    h.unmount()
    old.resolve(phase === 'authority' ? formalDecision() : formalDetail())
    await mounted
    assert.equal(h.page.detailVisible.value, false)
    assert.equal(h.page.orderDetail.value, undefined)
    assert.equal(h.page.detailApplicationId.value, undefined)
    assert.equal(h.page.detailReadOnly.value, false)
    assert.equal(h.page.detailLoading.value, false)
    assert.equal(h.page.detailError.value, '')
    assert.equal(h.page.loading.value, false)
  }
})

test('normal detail invalidates outstanding DONE authority or detail and preserves editable entry', async () => {
  for (const phase of ['authority', 'detail']) {
    const old = deferred()
    const normal = formalDetail({ activeOrderId: '1009200410', activeOrderStatus: { status: 'REPORT_UPLOAD_PENDING' } })
    const h = doneHarness({
      getPqcProductionRelease: () => phase === 'authority' ? old.promise : Promise.resolve(formalDecision()),
      getPqcProductionReleaseOrderDetail: id => id === '225' ? Promise.resolve(normal) : old.promise
    })
    const mounted = h.mount()
    await tick()
    await h.page.openActiveOrderDetail(formalRow({ applicationId: '225', activeOrderId: '1009200410' }))
    old.resolve(phase === 'authority' ? formalDecision() : formalDetail())
    await mounted
    assert.equal(h.page.detailApplicationId.value, '225')
    assert.equal(h.page.orderDetail.value.detail.activeOrderId, '1009200410')
    assert.equal(h.page.detailReadOnly.value, false)
    assert.equal(h.page.detailError.value, '')
    assert.equal(h.page.loading.value, false)
    h.page.retryOrderDetail()
    await tick()
    assert.equal(h.page.detailReadOnly.value, false)
    assert.equal(h.writes.length, 0)
  }
})

test('late normal detail cannot overwrite a newer DONE route result', async () => {
  const old = deferred()
  const h = doneHarness({ getPqcProductionReleaseOrderDetail: id => id === '225' ? old.promise : Promise.resolve(formalDetail()) }, { workOrderCode: 'NORMAL' })
  await h.mount()
  const normal = h.page.openActiveOrderDetail(formalRow({ applicationId: '225' }))
  await tick()
  h.route.query = { applicationId: '224', workTaskId: '2734' }
  await tick()
  old.resolve(formalDetail({ activeOrderId: '1009200410' }))
  await normal
  assert.equal(h.page.detailApplicationId.value, '224')
  assert.equal(h.page.orderDetail.value.detail.activeOrderId, '1009200409')
  assert.equal(h.page.detailReadOnly.value, true)
  assert.equal(h.page.detailLoading.value, false)
})
