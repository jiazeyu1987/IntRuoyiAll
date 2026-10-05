const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const vue = require('vue')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')).descriptor
const approvalDescriptor = parse(fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')).descriptor

function actualFunctions(descriptor, names, context) {
  const ast = ts.createSourceFile('actual-page.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statements = ast.statements.filter(node => ts.isVariableStatement(node) && node.declarationList.declarations.some(declaration => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, `actual functions required: ${names.join(', ')}`)
  const code = ts.transpileModule(statements.map(node => node.getText(ast)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
  }).outputText
  return Function(...Object.keys(context), `${code}; return { ${names.join(', ')} };`)(...Object.values(context))
}

const batch = () => ({ id: '900000001228', batchExecutionCode: 'BATCH-1228', workOrderCode: 'WO-990274', status: 40 })
const routeContext = () => ({ batchExecutionId: '900000001228', releaseTransactionId: '229', workTaskId: '2741' })
const formalContext = (status = 'PENDING_APPROVAL') => ({ batchExecutionId: '900000001228', releaseTransactionId: '229', releaseApprovalWorkTaskId: '2741', releaseStatus: status, releaseCode: 'RELEASE-229', version: 3, approvedBy: '9908090347', approvedAt: '2026-10-04 12:00:00', approvalOpinion: '正式放行意见', approvalSignoffEvidenceHash: 'formal-evidence' })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }

function harness({ context = formalContext(), permissions = ['mes:pro-edhr-release:approve'], loadRelease, loadBatch } = {}) {
  const reads = [], writes = [], navigation = [], keys = []
  const state = {
    selectedReleaseBatch: vue.ref(), releaseContext: vue.ref(), releaseCompletedReadOnly: vue.ref(false),
    releaseTransactionMissing: vue.ref(false), releaseError: vue.ref(''), loadError: vue.ref(''),
    releaseForm: vue.reactive({ password: '', idempotencyKey: '' }),
    releaseDialogVisible: vue.ref(false), releaseContextLoading: vue.ref(false), releaseLoading: vue.ref(false),
    route: vue.reactive({ query: { action: 'marketRelease', ...routeContext() } }),
    userStore: { permissions: new Set(permissions) },
    isVoidedBatchExecutionStatus: status => Number(status) === 60,
    getEdhrBatchExecution: async id => { reads.push(['batch', id]); return loadBatch ? loadBatch(id) : batch() },
    getEdhrRelease: async id => { reads.push(['transaction', id]); return loadRelease ? loadRelease(id) : context },
    getEdhrReleasePage: async query => { reads.push(['page', query]); return { list: [context] } },
    approveEdhrRelease: async request => { writes.push(request) },
    generateUUID: () => { keys.push('new-key'); return 'new-key' },
    resolveErrorMessage: (error, defaultMessage) => error.message || defaultMessage,
    message: { error() {}, success() {} }, router: { push: async target => { navigation.push(target) } }
  }
  const names = ['marketReleaseRouteGeneration', 'resetMarketReleaseDialog', 'readMarketReleaseRouteContext', 'openMarketReleaseFromRoute', 'openReleaseDialog', 'submitRelease']
  const functions = actualFunctions(descriptor, names, state)
  return { ...functions, state, reads, writes, navigation, keys }
}

test('exact PENDING_APPROVAL notification retains formal approval request identity', async () => {
  const h = harness()
  await h.openMarketReleaseFromRoute()
  assert.equal(h.state.releaseError.value, '')
  assert.equal(h.state.releaseCompletedReadOnly.value, false)
  assert.equal(h.state.releaseContext.value.releaseTransactionId, '229')
  assert.equal(h.keys.length, 1)
  h.state.releaseForm.password = 'local-test-password'
  await h.submitRelease()
  assert.deepEqual(h.writes, [{ releaseTransactionId: '229', workTaskId: '2741', expectedVersion: 3, idempotencyKey: 'EDHR-MARKET-RELEASE-900000001228-new-key', signoffEvidenceHash: 'formal-evidence', password: 'local-test-password' }])
})

test('exact RELEASED notification loads completed read-only information and creates no approval key or write', async () => {
  const h = harness({ context: formalContext('RELEASED') })
  await h.openMarketReleaseFromRoute()
  assert.equal(h.state.releaseError.value, '')
  assert.equal(h.state.releaseCompletedReadOnly.value, true)
  assert.equal(h.state.releaseContext.value.approvedBy, '9908090347')
  assert.equal(h.state.releaseContext.value.approvalOpinion, '正式放行意见')
  assert.equal(h.state.releaseForm.idempotencyKey, '')
  assert.equal(h.keys.length, 0)
  h.state.releaseForm.password = 'local-test-password'
  await h.submitRelease()
  assert.equal(h.writes.length, 0)
})

for (const field of ['batchExecutionId', 'releaseTransactionId', 'releaseApprovalWorkTaskId']) {
  test(`RELEASED ${field} mismatch still refuses exact notification context`, async () => {
    const h = harness({ context: { ...formalContext('RELEASED'), [field]: 'incorrect' } })
    await h.openMarketReleaseFromRoute()
    assert.match(h.state.releaseError.value, /不一致/)
    assert.equal(h.state.releaseContext.value, undefined)
    assert.equal(h.state.releaseCompletedReadOnly.value, false)
    assert.equal(h.keys.length, 0)
    assert.equal(h.writes.length, 0)
  })
}

test('other terminal or preliminary release statuses are not treated as completed approval', async () => {
  for (const status of ['REJECTED', 'WITHDRAWN', 'PRECHECK_PASSED']) {
    const h = harness({ context: formalContext(status) })
    await h.openMarketReleaseFromRoute()
    assert.match(h.state.releaseError.value, /状态/)
    assert.equal(h.state.releaseContext.value, undefined)
    assert.equal(h.state.releaseCompletedReadOnly.value, false)
    assert.equal(h.keys.length, 0)
    assert.equal(h.writes.length, 0)
  }
})

test('no approval permission performs zero formal reads, key creation and writes', async () => {
  const h = harness({ permissions: [] })
  await h.openMarketReleaseFromRoute()
  assert.match(h.state.loadError.value, /没有上市放行权限/)
  assert.deepEqual(h.reads, [])
  assert.equal(h.state.releaseDialogVisible.value, false)
  assert.equal(h.keys.length, 0)
  assert.equal(h.writes.length, 0)
})

test('stale RELEASED response cannot replace a newly selected exact pending task', async () => {
  const old = deferred()
  const h = harness({ loadRelease: id => id === '229' ? old.promise : { ...formalContext(), batchExecutionId: '900000001229', releaseTransactionId: '230', releaseApprovalWorkTaskId: '2742' } })
  const first = h.openReleaseDialog(batch(), routeContext())
  await h.openReleaseDialog({ ...batch(), id: '900000001229' }, { batchExecutionId: '900000001229', releaseTransactionId: '230', workTaskId: '2742' })
  old.resolve(formalContext('RELEASED'))
  await first
  assert.equal(h.state.releaseContext.value.releaseTransactionId, '230')
  assert.equal(h.state.releaseCompletedReadOnly.value, false)
  assert.equal(h.state.releaseError.value, '')
  assert.equal(h.keys.length, 1)
})

test('old route batch read cannot open a dialog after route generation advances', async () => {
  const oldBatch = deferred()
  const h = harness({ loadBatch: id => id === '900000001228' ? oldBatch.promise : { ...batch(), id: '900000001229' },
    loadRelease: async () => ({ ...formalContext('RELEASED'), batchExecutionId: '900000001229', releaseTransactionId: '230', releaseApprovalWorkTaskId: '2742' }) })
  const first = h.openMarketReleaseFromRoute()
  h.state.route.query = { action: 'marketRelease', batchExecutionId: '900000001229', releaseTransactionId: '230', workTaskId: '2742' }
  await h.openMarketReleaseFromRoute()
  oldBatch.resolve(batch())
  await first
  assert.equal(h.state.selectedReleaseBatch.value.id, '900000001229')
  assert.equal(h.state.releaseContext.value.releaseTransactionId, '230')
  assert.equal(h.state.releaseCompletedReadOnly.value, true)
  assert.deepEqual(h.reads.filter(read => read[0] === 'transaction'), [['transaction', '230']])
  assert.equal(h.keys.length, 0)
  assert.equal(h.writes.length, 0)
})

test('close or unmount invalidation cannot reopen or fill completed context', async () => {
  const old = deferred()
  const h = harness({ loadRelease: () => old.promise })
  const first = h.openReleaseDialog(batch(), routeContext())
  h.state.releaseDialogVisible.value = false
  h.resetMarketReleaseDialog()
  old.resolve(formalContext('RELEASED'))
  await first
  assert.equal(h.state.releaseDialogVisible.value, false)
  assert.equal(h.state.releaseContext.value, undefined)
  assert.equal(h.state.releaseCompletedReadOnly.value, false)
  assert.equal(h.keys.length, 0)
})

const makeNode = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
const renderer = vue.createRenderer({
  createElement: makeNode, createText: text => makeNode('text', text), createComment: text => makeNode('comment', text),
  insert(node, parent, anchor) { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1); node.parent = parent; const index = anchor ? parent.children.indexOf(anchor) : -1; if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node) },
  remove(node) { if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1); node.parent = null },
  parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null,
  setText(node, text) { node.text = text }, setElementText(node, text) { node.text = text; node.children = [] }, patchProp(node, key, old, value) { node.props[key] = value }
})
const allNodes = node => [node, ...node.children.flatMap(allNodes)]
const text = node => node.type === 'comment' ? '' : node.text + node.children.map(text).join('')

test('compiled RELEASED dialog exposes exact historical detail and hides signature and approval controls', async () => {
  const h = harness({ context: formalContext('RELEASED') })
  await h.openMarketReleaseFromRoute()
  assert.equal(h.state.releaseError.value, '', 'completed exact context must be readable before rendering its actual dialog')
  const templateNodes = node => [node, ...(node.children || []).flatMap(templateNodes)]
  const dialog = templateNodes(descriptor.template.ast).find(node => node.tag === 'Dialog' && node.props.some(prop => prop.name === 'title' && prop.value?.content === '上市放行确认'))
  assert.ok(dialog)
  const history = actualFunctions(descriptor, ['openCompletedReleaseHistory'], h.state)
  const app = renderer.createApp({ render: vue.compile(dialog.loc.source), setup: () => ({ ...h.state, ...h, ...history, resolveBatchStatusLabel: () => '已放行', openPqcReleasePending() {} }) })
  app.component('Dialog', { setup: (props, { slots }) => () => vue.h('section', [slots.default?.(), slots.footer?.()]) })
  for (const name of ['ElDescriptions', 'ElDescriptionsItem', 'ElForm', 'ElFormItem']) app.component(name, { setup: (props, { attrs, slots }) => () => vue.h('div', attrs, slots.default?.()) })
  app.component('ElAlert', { props: ['title', 'description'], setup: (props, { attrs }) => () => vue.h('div', attrs, `${props.title} ${props.description || ''}`) })
  app.component('ElInput', { setup: (props, { attrs }) => () => vue.h('input', attrs) })
  app.component('ElButton', { setup: (props, { attrs, slots }) => () => vue.h('button', attrs, slots.default?.()) })
  const container = makeNode('root')
  app.mount(container)
  try {
    assert.match(text(container), /已完成上市放行/)
    assert.match(text(container), /RELEASE-229/)
    assert.match(text(container), /正式放行意见/)
    assert.equal(allNodes(container).filter(node => node.type === 'input').length, 0)
    assert.equal(allNodes(container).some(node => node.type === 'button' && /确认上市放行/.test(text(node))), false)
    const link = allNodes(container).find(node => node.props['data-edhr-batch-action'] === 'completed-release-history')
    assert.ok(link, 'completed exact context must retain the existing historical detail entry')
    await link.props.onClick()
    assert.deepEqual(h.navigation, [{ path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail', query: { batchExecutionId: '900000001228', from: '/mes/pro/feedback/edhr-batch-history' } }])
    assert.equal(h.writes.length, 0)
    assert.equal(h.keys.length, 0)
  } finally { app.unmount() }
})

test('actual approval source resolver maps EDHR_WORK_TASK explicitly without relabelling unknown sources', () => {
  const functions = actualFunctions(approvalDescriptor, ['APPROVAL_SOURCE_TASK_TYPE_LABELS', 'normalizeApprovalDisplayText', 'containsEnglishLetters', 'resolveMappedApprovalText', 'resolveSourceTaskTypeLabel'], { ENGLISH_LETTER_PATTERN: /[a-zA-Z]/, EMPTY_APPROVAL_DISPLAY: '--' })
  assert.equal(functions.resolveSourceTaskTypeLabel({ sourceTaskType: 'EDHR_WORK_TASK' }), '电子批记录工作任务')
  assert.equal(functions.resolveSourceTaskTypeLabel({ sourceTaskType: 'UNKNOWN_TASK' }), '未配置中文任务来源')
  assert.equal(functions.resolveSourceTaskTypeLabel({ sourceTaskType: 'MES_PRO_FEEDBACK' }), '生产报工复核任务')
})

test('completed history destination actually parses the exact batch query and calls the formal detail reader', async () => {
  const h = harness({ context: formalContext('RELEASED') })
  await h.openMarketReleaseFromRoute()
  const historyEntry = actualFunctions(descriptor, ['openCompletedReleaseHistory'], h.state)
  await historyEntry.openCompletedReleaseHistory()
  const destination = h.navigation[0]
  const destinationDescriptor = parse(fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'), 'utf8')).descriptor
  const detailReads = []
  const destinationState = {
    route: vue.reactive({ query: destination.query }),
    batchExecutionId: vue.computed(() => destination.query.batchExecutionId),
    detail: vue.ref(), loading: vue.ref(false), error: vue.ref(''),
    getEdhrBatchActiveOrderDetail: async query => { detailReads.push(query); return { processes: [{ processId: '123' }] } },
    resolveErrorMessage: error => error.message
  }
  const detailFunctions = actualFunctions(destinationDescriptor, ['parseBatchExecutionId', 'parseActiveOrderId', 'resolveDetailQuery', 'resolveReturnPath', 'detailSequence', 'loadDetail'], destinationState)
  assert.equal(detailFunctions.resolveReturnPath(), '/mes/pro/feedback/edhr-batch-history')
  await detailFunctions.loadDetail()
  assert.deepEqual(detailReads, [{ batchExecutionId: '900000001228' }])
  assert.equal(destinationState.error.value, '')
  assert.equal(destinationState.detail.value.processes.length, 1)
  assert.equal(h.writes.length, 0)
})
