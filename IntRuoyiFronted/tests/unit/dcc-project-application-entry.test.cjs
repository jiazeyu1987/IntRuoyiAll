const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = file => fs.readFileSync(file, 'utf8')
const page = read('src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue')
const requestId = '9007199254740993'
const origin = 'http://localhost:48087'
const locationQuery = id => ({ requestId: id, requestOpen: 'records', from: 'notification' })
const row = id => ({ id, projectName: '原项目', projectCode: 'P01', productName: '真实产品', status: 'REJECTED', applicantUserId: '7', rejectReason: '修正原资料', resubmittedRequestId: '9007199254740995' })
function moduleAt(path, host = {}) {
  const context = { exports: {}, Error, String, Number, BigInt, Object, Array, URL, Set, JSON, Promise, console, ...host }
  if (path === 'src/utils/notifyMessageNavigation.ts') {
    const resolve = context.require
    context.require = id => id === './dccOfflineTrainingRecord' ? moduleAt('src/utils/dccOfflineTrainingRecord.ts') : resolve(id)
  }
  vm.runInNewContext(ts.transpileModule(read(path), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
function declarations(source, names) {
  const ast = ts.createSourceFile('actual.ts', source, ts.ScriptTarget.Latest, true)
  const picked = names.map(name => ast.statements.find(st => ts.isVariableStatement(st) && st.declarationList.declarations.some(d => ts.isIdentifier(d.name) && d.name.text === name)))
  picked.forEach((st, i) => assert.ok(st, 'actual source declaration: ' + names[i]))
  return picked.map(st => st.getText(ast)).join('\n')
}
function routeHost(get = async id => row(id)) {
  const reviewers = moduleAt('src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts')
  const calls = [], state = {
    exports: {}, Error, String, JSON, Array, Object,
    route: { path: '/mdm/product-catalog', query: locationQuery(requestId) },
    projectProductRequestsLoading: vue.ref(false), projectProductLoading: vue.ref(false),
    projectProductDialogVisible: vue.ref(false), projectProductMode: vue.ref('create'),
    projectProductRequests: vue.ref([]), projectProductError: vue.ref(''),
    projectProductExactRequestId: vue.ref(), projectProductExpandedRequestIds: vue.ref([]),
    reviewerConfiguration: vue.ref(), projectProductRequestReadSequence: 0, projectProductDialogSequence: 0,
    resetProjectProductForm() {},
    dccProjectProductRequestIdentity: value => { assert.equal(typeof value, 'string'); return value },
    getDccProjectProductRequest: async id => { calls.push(id); return get(id) },
    getDccProjectProductRequests: async () => { calls.push('pending'); return [] },
    canReviewProjectProduct: reviewers.canReviewProjectProduct,
    userStore: { getUser: { id: '7' } }, window: { prompt: () => '实际办理意见' },
    message: { success() {}, warning() {} }, getList: async () => {}
  }
  const source = parse(page).descriptor.scriptSetup.content
  vm.runInNewContext(ts.transpileModule(declarations(source, ['loadProjectProductRequests', 'openProjectProductDialog', 'consumeProjectProductRequestRoute', 'closeProjectProductRequestEntry', 'handleProjectProductAction']) + '\nexports.consume=consumeProjectProductRequestRoute;exports.refresh=loadProjectProductRequests;exports.close=closeProjectProductRequestEntry;exports.handle=handleProjectProductAction;', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
  return { state, calls, ...state.exports }
}
test('formal exact request GET preserves original Long ID and refuses response identity drift', async () => {
  const calls = [], api = moduleAt('src/api/dcc/controlledFile/projectProductRequests.ts', { require: () => ({ default: { get: async query => { calls.push(query); return row(requestId) } } }) })
  assert.equal(typeof api.getDccProjectProductRequest, 'function')
  const result = await api.getDccProjectProductRequest(requestId)
  assert.equal(result.id, requestId)
  assert.equal(calls[0].url, '/dcc/project-product-requests/' + requestId)
  const wrong = moduleAt('src/api/dcc/controlledFile/projectProductRequests.ts', { require: () => ({ default: { get: async () => row('9007199254740995') } }) })
  await assert.rejects(() => wrong.getDccProjectProductRequest(requestId), /身份|申请/)
})
test('actual route handler opens exact original records including rejected successor facts without pending', async () => {
  const h = routeHost()
  await h.consume()
  assert.deepEqual(h.calls, [requestId])
  assert.equal(h.state.projectProductMode.value, 'records')
  assert.equal(h.state.projectProductDialogVisible.value, true)
  assert.equal(h.state.projectProductRequests.value[0].rejectReason, '修正原资料')
  assert.equal(h.state.projectProductRequests.value[0].resubmittedRequestId, '9007199254740995')
  assert.equal(h.state.projectProductExpandedRequestIds.value[0], requestId)
  await h.refresh()
  assert.deepEqual(h.calls, [requestId, requestId])
})
test('exact request error remains visible and never falls back to pending records', async () => {
  const h = routeHost(async () => { throw new Error('原申请无读取权限') })
  await h.consume()
  assert.deepEqual(h.calls, [requestId])
  assert.equal(h.state.projectProductError.value, '原申请无读取权限')
  assert.equal(h.state.projectProductRequests.value.length, 0)
})
test('late request cannot replace the newly selected original or reopen a closed dialog', async () => {
  let release
  const h = routeHost(id => id === requestId ? new Promise(resolve => { release = resolve }) : Promise.resolve({ ...row(id), status: 'COMPLETED' }))
  const first = h.consume()
  h.state.route.query = locationQuery('9007199254740995')
  await h.consume()
  release(row(requestId)); await first
  assert.equal(h.state.projectProductRequests.value[0].id, '9007199254740995')
  h.state.route.query = locationQuery(requestId)
  const last = h.consume(); h.state.projectProductDialogVisible.value = false; h.close(); release(row(requestId)); await last
  assert.equal(h.state.projectProductDialogVisible.value, false)
  assert.equal(h.state.projectProductRequests.value.length, 0)
})
test('actual original review and approval handlers retain exact ID through existing opinion wrappers and reread', async () => {
  const writes = [], api = moduleAt('src/api/dcc/controlledFile/projectProductRequests.ts', {
    require: () => ({ default: { post: async query => { writes.push(query); return row(requestId) } } })
  })
  for (const node of ['review', 'approve']) {
    const actualRow = { ...row(requestId), configuredReviewerUserId: '7', status: node === 'review' ? 'PENDING_REVIEW' : 'PENDING_APPROVAL' }
    const h = routeHost(async () => actualRow)
    h.state.reviewDccProjectProductRequest = api.reviewDccProjectProductRequest
    h.state.approveDccProjectProductRequest = api.approveDccProjectProductRequest
    await h.consume(); await h.handle(h.state.projectProductRequests.value[0], node, true)
    assert.equal(writes[writes.length - 1].url, `/dcc/project-product-requests/${requestId}/${node}/approve`)
    assert.equal(writes[writes.length - 1].data.reason, '实际办理意见')
    assert.deepEqual(h.calls, [requestId, requestId])
  }
})
test('actual notification helper navigates only the dedicated exact same-origin project request contract', async () => {
  const api = moduleAt('src/utils/notifyMessageNavigation.ts', { window: { location: { origin } }, require: () => ({ EDHR_WORK_TASK_NOTIFY_PATHS: new Set() }) })
  const params = { notifyTargetType: 'DCC_PROJECT_PRODUCT_REQUEST', notifyTargetId: requestId, actionUrl: '/mdm/product-catalog?' + new URLSearchParams(locationQuery(requestId)) }
  const target = api.getNotifyMessageTarget({ templateParams: params })
  assert.ok(target)
  assert.equal(target.label, '查看项目及产品申请')
  const calls = []
  await api.navigateToNotifyMessageTarget({ push: async value => calls.push(value) }, target)
  assert.equal(calls[0].path, '/mdm/product-catalog')
  assert.equal(calls[0].query.requestId, requestId)
  for (const actionUrl of ['https://foreign.example' + params.actionUrl, params.actionUrl + '&taskId=4', params.actionUrl + '&requestId=9', params.actionUrl + '#fake', params.actionUrl.replace(requestId, '9007199254740995')]) {
    assert.equal(api.getNotifyMessageTarget({ templateParams: { ...params, actionUrl } }), null)
  }
})
test('actual approval-center native source labels and module navigation keep project query out of file handling', () => {
  const source = parse(read('src/views/approval-center/index.vue')).descriptor.scriptSetup.content
  const training = moduleAt('src/utils/dccOfflineTrainingRecord.ts')
  const context = { exports: {}, String, queryParams: { viewType: 'TODO' }, DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX: '/dcc/controlled-file/detail/', DCC_APPROVAL_HANDLING_MODE: 'approval', ...training }
  vm.runInNewContext(ts.transpileModule(declarations(source, ['APPROVAL_SOURCE_TASK_TYPE_LABELS', 'isDccModuleHandlingAction', 'resolveDccApprovalDetailLocation']) + '\nexports.labels=APPROVAL_SOURCE_TASK_TYPE_LABELS;exports.resolve=resolveDccApprovalDetailLocation;', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal(context.exports.labels.DCC_PROJECT_PRODUCT_REVIEW, '项目产品审核')
  assert.equal(context.exports.labels.DCC_PROJECT_PRODUCT_APPROVAL, '项目产品批准')
  const result = context.exports.resolve({ moduleCode: 'DCC', availableActions: ['PROCESS_IN_MODULE'] }, '/mdm/product-catalog', locationQuery(requestId))
  assert.equal(result.query.requestId, requestId)
  assert.equal(Object.hasOwn(result.query, 'taskId'), false)
  assert.equal(Object.hasOwn(result.query, 'handling'), false)
})
test('actual message detail template renders the dedicated project-request button and calls its real handler', async () => {
  const { descriptor } = parse(read('src/views/system/notify/my/MyNotifyMessageDetail.vue'))
  const find = node => {
    if (node.type === 1 && node.tag === 'el-button' && node.children.some(n => n.type === 2 && n.content.includes('查看项目及产品申请'))) return node
    for (const child of node.children || []) { const found = find(child); if (found) return found }
  }
  const node = find(descriptor.template.ast); assert.ok(node, 'dedicated real message action')
  const targets = moduleAt('src/utils/notifyMessageNavigation.ts', { window: { location: { origin } }, require: () => ({ EDHR_WORK_TASK_NOTIFY_PATHS: new Set() }) })
  const target = targets.getNotifyMessageTarget({ templateParams: { notifyTargetType: 'DCC_PROJECT_PRODUCT_REQUEST', notifyTargetId: requestId, actionUrl: '/mdm/product-catalog?' + new URLSearchParams(locationQuery(requestId)) } })
  const calls = [], handlerContext = { exports: {}, dccProjectProductNavigation: vue.ref(target), router: { push: async value => calls.push(value) }, navigateToNotifyMessageTarget: targets.navigateToNotifyMessageTarget, resetDialogState: () => calls.push('closed'), nextTick: vue.nextTick }
  vm.runInNewContext(ts.transpileModule(declarations(descriptor.scriptSetup.content, ['navigateToDccProjectProduct']) + '\nexports.handler=navigateToDccProjectProduct', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, handlerContext)
  const host = { dccProjectProductNavigation: target, navigateToDccProjectProduct: handlerContext.exports.handler }
  const compiled = compileScript(parse('<template>' + node.loc.source + '</template><script setup>const dccProjectProductNavigation=__host.dccProjectProductNavigation;const navigateToDccProjectProduct=__host.navigateToDccProjectProduct;</script>').descriptor, { id: 'dcc-request-notice', inlineTemplate: true })
  const c = { exports: {}, require: () => vue, __host: host }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, c)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: () => ({}), insert: (n, p) => p.children.push(n), remove() {}, setText: (n, t) => { n.text = t }, setElementText: (n, t) => { n.text = t }, patchProp: (n, k, _old, v) => { n.props[k] = v }, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(c.exports.default); app.component('el-button', { setup: (_p, ctx) => () => vue.h('button', ctx.attrs, ctx.slots.default?.()) })
  const root = { children: [] }; app.mount(root)
  try {
    await root.children[0].props.onClick()
    assert.equal(calls[0], 'closed')
    assert.equal(calls[1].path, '/mdm/product-catalog')
    assert.equal(calls[1].query.requestId, requestId)
  } finally { app.unmount() }
})
