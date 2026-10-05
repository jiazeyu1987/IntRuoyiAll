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

function harness(permissions, query = {}) {
  const allowed = new Set(permissions)
  const route = vue.reactive({ name: 'MesProFeedbackEdhrNonconformanceReview', query })
  const mounted = [], stops = [], unmounted = [], candidates = [], lists = [], exactReads = [], writes = []
  const ast = ts.createSourceFile('page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const script = ast.statements.filter(s => !ts.isImportDeclaration(s) && !ts.isExportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const context = {
    ...vue, defineOptions() {}, useRoute: () => route,
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
    createNonconformanceReview: async request => { writes.push({ create: request }); throw Error('read entry must not write') },
    disposeNonconformanceReview: async request => { writes.push({ dispose: request }); throw Error('read entry must not dispose') },
    uploadNonconformanceReviewMaterial: async () => { writes.push('upload'); throw Error('read entry must not upload') }
  }
  const compiled = ts.transpileModule(script, { compilerOptions: { module: ts.ModuleKind.None, target: ts.ScriptTarget.ES2022 } }).outputText
  const page = Function(...Object.keys(context), `${compiled}; return { createDialogVisible, reviewDialogVisible, selectedReview, selectedActiveOrderId, errorText, disposeForm, openCreateDialog, openReviewDialog, canCreateReview: typeof canCreateReview === 'undefined' ? undefined : canCreateReview, canConfigureQaAssignment };`)(...Object.values(context))
  return { page, route, candidates, lists, exactReads, writes, mount: async () => { mounted.forEach(fn => fn()); await tick() }, close: () => { unmounted.forEach(fn => fn()); stops.forEach(stop => stop()) } }
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
  assert.equal(h.page.reviewDialogVisible.value, true); assert.equal(h.page.disposeForm.signaturePassword, '')
  assert.equal(h.candidates.length, 0); assert.deepEqual(h.writes, []); h.close()
})
test('refused creation does not dismiss or replace the existing disposition object', async () => {
  const h = harness([queryPermission, disposePermission]); await h.mount()
  h.page.openReviewDialog(review); await h.page.openCreateDialog(); await tick()
  assert.equal(h.page.selectedReview.value.id, 48); assert.equal(h.page.reviewDialogVisible.value, true)
  assert.equal(h.candidates.length, 0); assert.equal(h.page.createDialogVisible.value, false)
  assert.deepEqual(h.writes, []); h.close()
})

function qaAssignmentEntry(h) {
  const find = node => {
    if (node.type === 1 && node.tag === 'QaHandoffAssignmentConfig') return node
    for (const child of node.children || []) { const result = find(child); if (result) return result }
  }
  const node = find(descriptor.template.ast)
  assert.ok(node, 'Existing NCR page must provide the formal QA assignment entry')
  const built = compileTemplate({ source: node.loc.source, filename: 'qa-assignment-entry.vue', id: 'qa-assignment-entry', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
  assert.deepEqual(built.errors, [])
  return Function('Vue', built.code)({ ...vue, resolveComponent: () => 'qa-assignment-config' })({ canConfigureQaAssignment: h.page.canConfigureQaAssignment.value }, [])
}

test('ordinary QA, creator and update-only users cannot mount the management configuration', async () => {
  for (const permissions of [[queryPermission, disposePermission], [queryPermission, createPermission], [queryPermission, 'mes:pro-edhr-work-task-rule:update']]) {
    const h = harness(permissions); await h.mount()
    assert.equal(qaAssignmentEntry(h).type, vue.Comment)
    assert.deepEqual(h.writes, []); h.close()
  }
})

test('formal rule query permission mounts QA configuration without acquiring NCR creation', async () => {
  const h = harness([queryPermission, 'mes:pro-edhr-work-task-rule:query']); await h.mount()
  assert.equal(qaAssignmentEntry(h).type, 'qa-assignment-config')
  assert.equal(createButton(h).type, vue.Comment)
  assert.equal(h.candidates.length, 0); assert.deepEqual(h.writes, []); h.close()
})
