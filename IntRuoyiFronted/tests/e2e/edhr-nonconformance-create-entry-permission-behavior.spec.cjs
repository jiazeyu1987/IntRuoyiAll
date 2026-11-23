const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'), 'utf8')
const descriptor = parse(source).descriptor
const createPermission = 'mes:pro-edhr-nonconformance-review:create'
const queryPermission = 'mes:pro-edhr-nonconformance-review:query'
const disposePermission = 'mes:pro-edhr-nonconformance-review:dispose'
const tick = async () => { await vue.nextTick(); for (let i = 0; i < 20; i++) await Promise.resolve() }
const review = { id: 48, sourceType: 'DEVIATION', sourceId: 1225, batchExecutionId: 1225, deviationIdsJson: '[91]', reviewStatus: 'pending_review' }

function harness(permissions, query = {}, apiOverrides = {}) {
  const allowed = new Set(permissions)
  const route = vue.reactive({ name: 'MesProFeedbackEdhrNonconformanceReview', query })
  const mounted = [], stops = [], unmounted = [], candidates = [], lists = [], exactReads = [], writes = [], orderReads = []
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const script = ast.statements.filter(s => !ts.isImportDeclaration(s) && !ts.isExportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const context = {
    useUserTableColumns: (_key, columns) => ({ columns: vue.ref(columns), saving: vue.ref(false), isColumnVisible: () => true, getColumnWidth: () => undefined, handleHeaderDragend() {}, saveConfig() {} }),
    useTableQuickFilter: () => ({ state: {}, selectedDefinition: vue.ref(), operatorOptions: vue.ref([]), updateState() {}, applyQuickFilter() {} }),
    useUserStore: () => ({ getUser: { id: 50 } }),
    ...vue, useApprovalCenterInvalidationStore: () => ({ syncSession: () => ({ key: "test-session" }) }), defineOptions() {}, useRoute: () => route,
    useMessage: () => ({ error() {}, success() {} }),
    onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounted.push(fn),
    watch: (...args) => { const stop = vue.watch(...args); stops.push(stop); return stop },
    hasPermission: requested => requested.some(p => allowed.has(p)),
    parsePositiveRouteQueryId: value => typeof value === 'string' && /^[1-9]\d*$/.test(value) ? value : undefined,
    parseExactIntegerJson: JSON.parse, resolveUrlPathFileName: value => value, formatEdhrDateTime: value => value,
    SOURCE_TYPE_ACTIVE_ORDER: 'ACTIVE_ORDER', SOURCE_TYPE_PQC_RELEASE: 'PQC_RELEASE', SOURCE_TYPE_PQC_SUBMISSION: 'PQC_SUBMISSION', SOURCE_TYPE_DEVIATION: 'DEVIATION',
    REVIEW_STATUS_PENDING_REVIEW: 'pending_review', DISPOSITION_CONCESSION_RELEASE: 'concession_release', DISPOSITION_REWORK: 'rework', DISPOSITION_VOID: 'void',
    getNonconformanceReviewActiveOrderList: async () => { candidates.push('GET candidates'); return [{ id: 7, workOrderId: 990274 }] },
    getNonconformanceReviewPage: async request => { lists.push(request); return { list: [review], total: 1 } },
    getNonconformanceReview: async id => { exactReads.push(id); return review },
    getNonconformanceReviewActiveOrderDetail: async id => { orderReads.push(id); return { activeOrderId: 7, workOrderCode: 'WO-7', processes: [] } },
    getNonconformanceReviewSummary: async id => ({ review: { ...review, id, activeOrderId: 7 }, qaReviewerName: 'QA', qaTaskId: '2124588436506107904', qaTaskStatus: 'TODO' }),
    createNonconformanceReview: async request => { writes.push({ create: request }); throw Error('read entry must not write') },
    disposeNonconformanceReview: async request => { writes.push({ dispose: request }); throw Error('read entry must not dispose') },
    uploadNonconformanceReviewMaterial: async () => { writes.push('upload'); throw Error('read entry must not upload') },
    ...apiOverrides
  }
  const compiled = ts.transpileModule(script, { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
  const page = Function(...Object.keys(context), `${compiled}; return { orderDetailVisible, orderDetailLoading, orderDetailError, orderDetail, openActiveOrderDetail, closeOrderDetail, retryOrderDetail, createDialogVisible, reviewTabOpen, selectedReview, selectedActiveOrderId, errorText, disposeForm, openCreateDialog, openReviewTab, canCreateReview: typeof canCreateReview === 'undefined' ? undefined : canCreateReview, entryForm, submitCreateReview, canDisposeSelectedReview };`)(...Object.values(context))
  return { page, route, candidates, lists, exactReads, writes, orderReads, mount: async () => { mounted.forEach(fn => fn()); await tick() }, close: () => { unmounted.forEach(fn => fn()); stops.forEach(stop => stop()) } }
}

function createButton(h) {
  const tree = descriptor.template.ast
  const find = node => {
    if (node.type === 1 && node.props.some(p => p.type === 6 && p.name === 'data-edhr-ncr-open-create')) return node
    for (const child of node.children || []) { const result = find(child); if (result) return result }
  }
  const button = find(tree)
  assert.ok(button, 'actual production creation button must exist for authorized creators')
  const markup = button.loc.source
  const result = compileTemplate({ source: markup, filename: 'creation-entry.vue', id: 'creation-entry', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
  assert.deepEqual(result.errors, [])
  const render = Function('Vue', result.code)({ ...vue, resolveComponent: () => 'button' })
  return render({ canCreateReview: h.page.canCreateReview?.value, openCreateDialog: h.page.openCreateDialog }, [])
}

test('query/dispose and query-only users have no rendered creation button', async () => {
  for (const permissions of [[queryPermission, disposePermission], [queryPermission], []]) {
    const h = harness(permissions); await h.mount()
    assert.equal(createButton(h).type, vue.Comment)
    assert.equal(h.candidates.length, 0); assert.deepEqual(h.writes, []); h.close()
  }
})
test('creator sees the actual button and opening loads candidates once without writes', async () => {
  const h = harness([queryPermission, createPermission]); await h.mount()
  const button = createButton(h); assert.equal(button.type, 'button')
  await button.props.onClick(); await tick()
  assert.equal(h.candidates.length, 1); assert.equal(h.page.createDialogVisible.value, true)
  assert.equal(h.page.selectedActiveOrderId.value, 7); assert.deepEqual(h.writes, []); h.close()
})
test('direct no-permission handler cannot open creation or load candidates', async () => {
  const h = harness([queryPermission, disposePermission]); await h.mount()
  await h.page.openCreateDialog(); await tick()
  assert.equal(h.candidates.length, 0); assert.equal(h.page.createDialogVisible.value, false)
  assert.match(h.page.errorText.value, /没有创建不合格评审的权限/); assert.deepEqual(h.writes, []); h.close()
})
test('unauthorized autoCreate preserves list reading but does not load creation candidates', async () => {
  const h = harness([queryPermission, disposePermission], { activeOrderId: '7', autoCreate: '1' }); await h.mount()
  assert.equal(h.lists.length, 1); assert.equal(h.candidates.length, 0)
  assert.equal(h.page.createDialogVisible.value, false); assert.equal(h.page.selectedActiveOrderId.value, undefined)
  assert.match(h.page.errorText.value, /没有创建不合格评审的权限/); assert.deepEqual(h.writes, []); h.close()
})
test('authorized existing active-order autoCreate still selects its exact candidate', async () => {
  const h = harness([queryPermission, createPermission], { activeOrderId: '7', autoCreate: '1' }); await h.mount()
  assert.equal(h.candidates.length, 1); assert.equal(h.page.selectedActiveOrderId.value, 7)
  assert.equal(h.page.createDialogVisible.value, true); assert.deepEqual(h.writes, []); h.close()
})
test('route change to unauthorized autoCreate cannot introduce creation reads', async () => {
  const h = harness([queryPermission, disposePermission]); await h.mount()
  h.route.query = { activeOrderId: '7', autoCreate: '1' }; await tick()
  assert.equal(h.lists.length, 2); assert.equal(h.candidates.length, 0)
  assert.equal(h.page.createDialogVisible.value, false); assert.deepEqual(h.writes, []); h.close()
})
test('query/dispose user retains exact formal deviation review and blank disposition password', async () => {
  const h = harness([queryPermission, disposePermission], { reviewId: '48', from: 'deviation', deviationId: '91', batchExecutionId: '1225' }); await h.mount()
  assert.deepEqual(h.exactReads, ['48']); assert.equal(h.page.selectedReview.value.id, 48)
  assert.equal(h.page.reviewTabOpen.value, true); assert.equal(h.page.disposeForm.signaturePassword, '')
  assert.equal(h.candidates.length, 0); assert.deepEqual(h.writes, []); h.close()
})
test('logged-in user without review permissions can read the list and exact review without writes', async () => {
  const list = harness([]); await list.mount()
  assert.equal(list.lists.length, 1)
  assert.equal(createButton(list).type, vue.Comment)
  assert.deepEqual(list.writes, []); list.close()
  const detail = harness([], { reviewId: '48', from: 'deviation', deviationId: '91', batchExecutionId: '1225' })
  await detail.mount()
  assert.deepEqual(detail.exactReads, ['48'])
  assert.equal(detail.page.reviewTabOpen.value, true)
  assert.equal(detail.page.canDisposeSelectedReview.value, false)
  assert.deepEqual(detail.writes, []); detail.close()
})
test('refused creation does not dismiss or replace the existing disposition object', async () => {
  const h = harness([queryPermission, disposePermission]); await h.mount()
  h.page.openReviewTab(review); await h.page.openCreateDialog(); await tick()
  assert.equal(h.page.selectedReview.value.id, 48); assert.equal(h.page.reviewTabOpen.value, true)
  assert.equal(h.candidates.length, 0); assert.equal(h.page.createDialogVisible.value, false)
  assert.deepEqual(h.writes, []); h.close()
})

test('取消路线负责人配置后，页面保留评审材料上传入口', () => {
  assert.doesNotMatch(descriptor.template.content, /QaHandoffAssignmentConfig|PqcHandoffAssignmentConfig/)
  assert.doesNotMatch(descriptor.scriptSetup.content, /canConfigureQaAssignment|canConfigurePqcAssignment/)
  assert.match(descriptor.template.content, /<NonconformanceReviewWorkspace[\s\S]*?@upload="handleMaterialUpload"/)
  const workspace = parse(fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-nonconformance/NonconformanceReviewWorkspace.vue'), 'utf8')).descriptor
  assert.match(workspace.template.content, /data-edhr-ncr-select-material/)
  assert.match(workspace.template.content, /@change="handleFilesChange"/)
  assert.deepEqual(compileTemplate({ source: workspace.template.content, filename: 'workspace.vue', id: 'workspace' }).errors, [])
  const built = compileTemplate({ source: descriptor.template.content, filename: 'nonconformance.vue', id: 'nonconformance' })
  assert.deepEqual(built.errors, [])
})


test('explicit QA selection is mandatory before a creation request is sent', async () => {
  const h=harness([queryPermission,createPermission]);await h.mount();await h.page.openCreateDialog();
  h.page.entryForm.nonconformanceReason='task owned deviation';
  await h.page.submitCreateReview();assert.deepEqual(h.writes,[]);h.close()
})
test('only the explicitly assigned QA can see the disposition controls', async () => {
  const h=harness([queryPermission,disposePermission]);await h.mount();
  h.page.openReviewTab({ ...review, qaReviewerUserId: 51 });assert.equal(h.page.canDisposeSelectedReview.value,false);
  h.page.openReviewTab({ ...review, qaReviewerUserId: 50 });assert.equal(h.page.canDisposeSelectedReview.value,true);
  h.page.openReviewTab({ ...review });assert.equal(h.page.canDisposeSelectedReview.value,false);h.close()
})

test('details button renders for every user and opens the formally linked active order', async () => {
  const find = node => {
    if (node.type === 1 && node.props.some(p => p.type === 6 && p.name === 'data-edhr-ncr-review-detail')) return node
    for (const child of node.children || []) { const found = find(child); if (found) return found }
  }
  const button = find(descriptor.template.ast)
  assert.ok(button)
  const result = compileTemplate({ source: button.loc.source, filename: 'review-detail.vue', id: 'review-detail', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
  assert.deepEqual(result.errors, [])
  const render = Function('Vue', result.code)({ ...vue, resolveComponent: () => 'button' })
  for (const permissions of [[], [queryPermission], [disposePermission], [createPermission]]) {
    const h = harness(permissions); await h.mount()
    const rendered = render({ row: review, openActiveOrderDetail: h.page.openActiveOrderDetail }, [])
    assert.equal(rendered.type, 'button')
    await rendered.props.onClick({ stopPropagation() {} })
    assert.deepEqual(h.orderReads, [review.id])
    assert.equal(h.page.orderDetailVisible.value, true)
    assert.equal(h.page.orderDetail.value.activeOrderId, 7)
    assert.equal(h.page.reviewTabOpen.value, false)
    assert.deepEqual(h.writes, []); h.close()
  }
  assert.match(descriptor.template.content, /<ActiveOrderSubmissionDetailPanel[\s\S]*?:detail="orderDetail"[\s\S]*?read-only/)
})

test('active order errors remain visible and retry reads the same review', async () => {
  let attempts = 0
  const h = harness([], {}, { getNonconformanceReviewActiveOrderDetail: async id => {
    assert.equal(id, review.id)
    if (++attempts === 1) throw Error('正式关联工单不存在')
    return { activeOrderId: 7, workOrderCode: 'WO-7', processes: [] }
  } })
  await h.mount(); await h.page.openActiveOrderDetail(review)
  assert.equal(h.page.orderDetailError.value, '正式关联工单不存在')
  assert.equal(h.page.orderDetail.value, undefined)
  h.page.retryOrderDetail(); await tick()
  assert.equal(attempts, 2)
  assert.equal(h.page.orderDetailError.value, '')
  assert.equal(h.page.orderDetail.value.activeOrderId, 7)
  assert.deepEqual(h.writes, []); h.close()
})

test('returning to the list discards an outstanding order detail response', async () => {
  let resolve
  const h = harness([], {}, { getNonconformanceReviewActiveOrderDetail: () => new Promise(done => { resolve = done }) })
  await h.mount()
  const pending = h.page.openActiveOrderDetail(review)
  h.page.closeOrderDetail()
  resolve({ activeOrderId: 7, workOrderCode: 'WO-7', processes: [] }); await pending
  assert.equal(h.page.orderDetailVisible.value, false)
  assert.equal(h.page.orderDetailLoading.value, false)
  assert.equal(h.page.orderDetail.value, undefined)
  assert.deepEqual(h.writes, []); h.close()
})
