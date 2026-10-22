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
const row = (overrides = {}) => ({ id: 48, sourceType: 'DEVIATION', sourceId: 1225, batchExecutionId: 1225, deviationIdsJson: '[91,92]', reviewStatus: 'pending_review', ...overrides })
const entry = (overrides = {}) => ({ reviewId: '48', from: 'deviation', deviationId: '91', batchExecutionId: '1225', ...overrides })
const handoffEntry = (overrides = {}) => ({ activeOrderId: '7', reviewId: '48', handoffTaskId: '101', roundId: '48', handoffType: 'QA_REVIEW', ...overrides })
const handoffContext = (query = handoffEntry(), overrides = {}) => ({
  current: true, processable: true,
  task: { id: query.handoffTaskId, activeOrderId: query.activeOrderId, sourceId: query.reviewId,
    roundId: query.roundId, taskType: query.handoffType, status: 'TODO', reason: '待评审',
    actionUrl: '/mes/pro/feedback/edhr-nonconformance-review?' + new URLSearchParams(Object.entries(query).filter(([key]) => key !== 'handoffReadOnly')).toString() },
  ...overrides
})
function compile(script) {
  return ts.transpileModule(script, { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
}
function pageHarness(query = entry(), overrides = {}) {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
  const ast = ts.createSourceFile('page.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s) && !ts.isExportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const route = vue.reactive({ name: 'MesProFeedbackEdhrNonconformanceReview', query })
  const mounted = [], unmounted = [], exactReads = [], listReads = [], writes = [], messages = [], stops = []
  const context = {
    ...vue, defineOptions() {}, useRoute: () => route,
    watch: (...args) => { const stop = vue.watch(...args); stops.push(stop); return stop },
    onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn),
    useMessage: () => ({ error: msg => messages.push(msg), success() {} }),
    hasPermission: () => true,
    parsePositiveRouteQueryId: value => typeof value === 'string' && /^[1-9]\d*$/.test(value) ? value : undefined,
    parseExactIntegerJson: (() => {
      const exports = {}
      const code = ts.transpileModule(fs.readFileSync(path.join(root, 'src/utils/exactIntegerJson.ts'), 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
      Function('exports', code)(exports)
      return exports.parseExactIntegerJson
    })(), resolveUrlPathFileName: value => value, formatEdhrDateTime: value => value,
    SOURCE_TYPE_ACTIVE_ORDER: 'ACTIVE_ORDER', SOURCE_TYPE_PQC_RELEASE: 'PQC_RELEASE', SOURCE_TYPE_PQC_SUBMISSION: 'PQC_SUBMISSION', SOURCE_TYPE_DEVIATION: 'DEVIATION',
    REVIEW_STATUS_PENDING_REVIEW: 'pending_review', DISPOSITION_CONCESSION_RELEASE: 'concession_release', DISPOSITION_REWORK: 'rework', DISPOSITION_VOID: 'void',
    getNonconformanceReview: async id => { exactReads.push(id); return row({ id: Number(id) }) },
    handoffNavigationContext: async () => handoffContext(),
    getNonconformanceReviewPage: async query => { listReads.push(query); return { list: [row({ id: 99, sourceType: 'ACTIVE_ORDER' })], total: 1 } },
    getNonconformanceReviewActiveOrderList: async () => [{ id: 7 }],
    createNonconformanceReview: async request => { writes.push(request); throw Error('entry must not create') },
    disposeNonconformanceReview: async request => { writes.push(request); throw Error('entry must not dispose') },
    uploadNonconformanceReviewMaterial: async () => { throw Error('entry must not upload') },
    ...overrides
  }
  const page = Function(...Object.keys(context), `${compile(body)}; return { errorText, selectedReview, selectedReviewReadOnly, reviewDialogVisible, createDialogVisible, selectedActiveOrderId, disposeForm, reviews, openReviewDialog, openCreateDialog, resolveSourceTypeLabel, handleDispose, handleMaterialUpload };`)(...Object.values(context))
  const unmount = () => { unmounted.forEach(fn => fn()); stops.forEach(stop => stop()) }
  return { page, route, exactReads, listReads, writes, messages, unmount, mount: async () => { mounted.forEach(fn => fn()); await tick() } }
}
test('deviation link includes exact review and formal source identities', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
  const ast = ts.createSourceFile('detail.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'openReview'))
  const pushed = []
  const detail = vue.ref({ id: 91, batchExecutionId: 1225, nonconformanceReviewId: 48 })
  Function('detail', 'router', `${compile(declaration.getText(ast))}; openReview();`)(detail, { push: value => pushed.push(value) })
  assert.deepEqual(pushed[0].query, entry())
})
test('deviation exact entry reads formal review outside first list page, without business writes', async () => {
  const h = pageHarness(); await h.mount()
  assert.deepEqual(h.exactReads, ['48'])
  assert.equal(h.page.selectedReview.value?.id, 48)
  assert.equal(h.page.reviewDialogVisible.value, true)
  assert.equal(h.page.createDialogVisible.value, false)
  assert.equal(h.page.disposeForm.signaturePassword, '')
  assert.deepEqual(h.writes, []); h.unmount()
})
test('DEVIATION has a business label and existing source labels remain unchanged', async () => {
  const h = pageHarness({}); await h.mount()
  assert.equal(h.page.resolveSourceTypeLabel('DEVIATION'), '偏差')
  assert.equal(h.page.resolveSourceTypeLabel('ACTIVE_ORDER'), '活跃订单')
  assert.equal(h.page.resolveSourceTypeLabel('PQC_RELEASE'), 'PQC生产放行')
  assert.equal(h.page.resolveSourceTypeLabel('PQC_SUBMISSION'), 'PQC提交记录')
  assert.equal(h.page.resolveSourceTypeLabel('unrecognized'), '未知来源'); h.unmount()
})
test('malformed, missing or duplicate source queries fail before reads or writes', async () => {
  for (const query of [entry({ reviewId: ['48', '49'] }), entry({ reviewId: '0' }), entry({ reviewId: '48.1' }), entry({ deviationId: undefined }), entry({ batchExecutionId: undefined }), entry({ deviationId: ['91'] }), entry({ batchExecutionId: ['1225'] }), entry({ from: ['deviation'] }), entry({ autoCreate: '1', activeOrderId: '7' }), entry({ reviewId: undefined }), { from: 'deviation' }]) {
    const h = pageHarness(query); await h.mount()
    assert.ok(h.page.errorText.value, JSON.stringify(query))
    assert.equal(h.exactReads.length, 0); assert.equal(h.listReads.length, 0)
    assert.equal(h.page.reviewDialogVisible.value, false); assert.equal(h.page.createDialogVisible.value, false)
    assert.deepEqual(h.writes, []); h.unmount()
  }
})
test('existing query permission is required, with no silent list fallback', async () => {
  const h = pageHarness(entry(), { hasPermission: () => false }); await h.mount()
  assert.equal(h.exactReads.length, 0); assert.equal(h.listReads.length, 0)
  assert.ok(h.page.errorText.value); assert.equal(h.page.reviewDialogVisible.value, false); h.unmount()
})
test('wrong review, source, batch, sourceId or deviation membership is refused', async () => {
  for (const bad of [row({ id: 49 }), row({ sourceType: 'ACTIVE_ORDER' }), row({ batchExecutionId: 1226 }), row({ sourceId: 1226 }), row({ deviationIdsJson: '[93]' }), row({ deviationIdsJson: undefined }), row({ deviationIdsJson: '{' }), row({ deviationIdsJson: '{}' }), row({ deviationIdsJson: '[91.1]' }), row({ id: Number.MAX_SAFE_INTEGER + 1 }), undefined]) {
    const h = pageHarness(entry(), { getNonconformanceReview: async () => bad }); await h.mount()
    assert.ok(h.page.errorText.value); assert.equal(h.page.selectedReview.value, undefined)
    assert.equal(h.page.reviewDialogVisible.value, false); assert.deepEqual(h.writes, []); h.unmount()
  }
})
test('large string identities remain exact in route and formal source membership', async () => {
  const identity = { reviewId: '9007199254740993', deviationId: '9007199254740995', batchExecutionId: '9007199254740997' }
  const reads = []
  const h = pageHarness(entry(identity), { getNonconformanceReview: async id => { reads.push(id); return row({ id, sourceId: identity.batchExecutionId, batchExecutionId: identity.batchExecutionId, deviationIdsJson: '[9007199254740995]' }) } })
  await h.mount(); assert.deepEqual(reads, [identity.reviewId]); assert.equal(h.page.selectedReview.value?.id, identity.reviewId)
  assert.equal(h.page.reviewDialogVisible.value, true); h.unmount()
})
test('formal read errors are visible and do not open or create another object', async () => {
  const h = pageHarness(entry(), { getNonconformanceReview: async () => { throw { response: { data: { msg: '无权访问关联评审' } } } } })
  await h.mount(); assert.match(h.page.errorText.value, /无权访问关联评审/)
  assert.equal(h.page.reviewDialogVisible.value, false); assert.equal(h.page.selectedReview.value, undefined); assert.deepEqual(h.writes, []); h.unmount()
})
test('late old route result cannot replace a newer exact review', async () => {
  const old = deferred(), reads = []
  const h = pageHarness(entry(), { getNonconformanceReview: async id => { reads.push(id); return id === '48' ? old.promise : row({ id: 49 }) } })
  await h.mount(); h.route.query = entry({ reviewId: '49' }); await tick()
  old.resolve(row()); await tick()
  assert.deepEqual(reads, ['48', '49']); assert.equal(h.page.selectedReview.value?.id, 49)
  assert.equal(h.page.reviewDialogVisible.value, true); h.unmount()
})
test('late response and error after route departure or unmount cannot reopen a dialog', async () => {
  for (const departure of ['query', 'name', 'unmount']) for (const failed of [false, true]) {
    const old = deferred()
    const h = pageHarness(entry(), { getNonconformanceReview: () => old.promise }); await h.mount()
    if (departure === 'query') h.route.query = {}
    else if (departure === 'name') h.route.name = 'OtherPage'
    else h.unmount()
    await tick(); if (failed) old.reject(Error('old route failure')); else old.resolve(row()); await tick()
    assert.equal(h.page.reviewDialogVisible.value, false); assert.equal(h.page.selectedReview.value, undefined)
    assert.equal(h.page.errorText.value, '')
    h.unmount()
  }
})
test('late list projection cannot replace the formal exact detail, closing returns to normal list', async () => {
  const list = deferred()
  const h = pageHarness(entry(), { getNonconformanceReviewPage: () => list.promise }); await h.mount()
  assert.equal(h.page.selectedReview.value?.sourceType, 'DEVIATION')
  list.resolve({ list: [row({ sourceType: 'ACTIVE_ORDER', deviationIdsJson: undefined })], total: 1 }); await tick()
  assert.equal(h.page.selectedReview.value?.sourceType, 'DEVIATION')
  assert.equal(h.page.reviews.value.length, 1)
  h.page.reviewDialogVisible.value = false; await tick()
  assert.equal(h.page.reviewDialogVisible.value, false); assert.equal(h.page.reviews.value.length, 1); h.unmount()
})
test('malformed formal materials do not expose a writable selected object', async () => {
  const h = pageHarness(entry(), { getNonconformanceReview: async () => row({ reviewMaterialsJson: '{' }) }); await h.mount()
  assert.match(h.page.errorText.value, /材料清单格式无效/)
  assert.equal(h.page.selectedReview.value, undefined); assert.equal(h.page.reviewDialogVisible.value, false); h.unmount()
})
test('manual review or create action invalidates an outstanding route read', async () => {
  for (const action of ['review', 'create']) {
    const old = deferred()
    const h = pageHarness(entry(), { getNonconformanceReview: () => old.promise }); await h.mount()
    if (action === 'review') h.page.openReviewDialog(row({ id: 77, sourceType: 'ACTIVE_ORDER' }))
    else await h.page.openCreateDialog()
    old.resolve(row()); await tick()
    assert.equal(h.page.selectedReview.value?.id, action === 'review' ? 77 : undefined)
    assert.equal(h.page.reviewDialogVisible.value, action === 'review')
    assert.equal(h.page.createDialogVisible.value, action === 'create'); h.unmount()
  }
})
test('normal list and existing active-order autoCreate paths remain independent', async () => {
  const ordinary = pageHarness({}); await ordinary.mount()
  assert.equal(ordinary.listReads.length, 1); assert.equal(ordinary.exactReads.length, 0)
  assert.equal(ordinary.page.reviewDialogVisible.value, false); ordinary.unmount()
  const create = pageHarness({ activeOrderId: '7', autoCreate: '1' }); await create.mount()
  assert.equal(create.page.createDialogVisible.value, true); assert.equal(create.page.selectedActiveOrderId.value, 7)
  assert.equal(create.exactReads.length, 0); assert.deepEqual(create.writes, []); create.unmount()
})

test('formal QA handoff opens its exact review and active cycle without starting a new review', async () => {
  const h = pageHarness(handoffEntry(), { getNonconformanceReview: async id => row({ id, activeOrderId: 7 }) })
  await h.mount()
  assert.equal(h.page.selectedReview.value.id, '48')
  assert.equal(h.page.reviewDialogVisible.value, true)
  assert.equal(h.page.selectedReviewReadOnly.value, false)
  assert.equal(h.page.createDialogVisible.value, false)
  assert.deepEqual(h.writes, []); h.unmount()
})

test('malformed or mixed handoff entries cannot fall back to lists or create another review', async () => {
  for (const query of [handoffEntry({ handoffTaskId: ['101'] }), handoffEntry({ roundId: undefined }),
    handoffEntry({ activeOrderId: undefined }), handoffEntry({ handoffType: 'PQC_REVIEW' }),
    handoffEntry({ handoffReadOnly: '0' }), handoffEntry({ from: 'deviation' }),
    handoffEntry({ autoCreate: '1' }), { handoffTaskId: '101' }]) {
    const h = pageHarness(query); await h.mount()
    assert.ok(h.page.errorText.value, JSON.stringify(query))
    assert.equal(h.listReads.length, 0); assert.equal(h.exactReads.length, 0)
    assert.equal(h.page.reviewDialogVisible.value, false); assert.equal(h.page.createDialogVisible.value, false)
    assert.deepEqual(h.writes, []); h.unmount()
  }
})

test('handoff task, frozen link, current cycle and actual review must all agree', async () => {
  const good = handoffContext()
  for (const context of [undefined, { ...good, current: false },
    { ...good, task: { ...good.task, status: 'CANCELED' } },
    { ...good, task: { ...good.task, id: 102 } },
    { ...good, task: { ...good.task, activeOrderId: 8 } },
    { ...good, task: { ...good.task, sourceId: 49 } },
    { ...good, task: { ...good.task, roundId: 49 } },
    { ...good, task: { ...good.task, actionUrl: good.task.actionUrl + '&reviewId=48' } },
    { ...good, task: { ...good.task, actionUrl: 'https://example.org' + good.task.actionUrl } }]) {
    const h = pageHarness(handoffEntry(), { handoffNavigationContext: async () => context }); await h.mount()
    assert.ok(h.page.errorText.value); assert.equal(h.page.reviewDialogVisible.value, false)
    assert.equal(h.exactReads.length, 0); assert.deepEqual(h.writes, []); h.unmount()
  }
  for (const review of [row({ activeOrderId: 8 }), row({ activeOrderId: 7, reviewStatus: 'closed' })]) {
    const h = pageHarness(handoffEntry(), { getNonconformanceReview: async () => review }); await h.mount()
    assert.ok(h.page.errorText.value); assert.equal(h.page.selectedReview.value, undefined); h.unmount()
  }
})

test('void result is read-only even outside the active cycle and handlers cannot dispose or upload', async () => {
  const query = handoffEntry({ handoffType: 'QA_DECISION_HANDOFF', handoffReadOnly: '1' })
  const context = handoffContext(query, { current: false, processable: false })
  context.task.status = 'DONE'; context.task.reason = 'void：本任务作废原因'
  const h = pageHarness(query, { handoffNavigationContext: async () => context,
    getNonconformanceReview: async () => row({ activeOrderId: 7, reviewStatus: 'closed', disposition: 'void' }) })
  await h.mount()
  assert.equal(h.page.reviewDialogVisible.value, true); assert.equal(h.page.selectedReviewReadOnly.value, true)
  await h.page.handleDispose('void')
  await h.page.handleMaterialUpload({ target: { files: [{ name: 'forbidden.txt' }], value: 'file' } })
  assert.deepEqual(h.writes, []); assert.match(h.messages.at(-1), /只允许查看/); h.unmount()
})

test('late handoff context cannot open an old review after changing rounds', async () => {
  const old = deferred(), reads = []
  const h = pageHarness(handoffEntry(), {
    handoffNavigationContext: async id => id === '101' ? old.promise : handoffContext(handoffEntry({ handoffTaskId: '102', roundId: '49', reviewId: '49' })),
    getNonconformanceReview: async id => { reads.push(id); return row({ id, activeOrderId: 7 }) }
  })
  await h.mount(); h.route.query = handoffEntry({ handoffTaskId: '102', roundId: '49', reviewId: '49' }); await tick()
  old.resolve(handoffContext()); await tick()
  assert.deepEqual(reads, ['49']); assert.equal(h.page.selectedReview.value.id, '49'); h.unmount()
})
