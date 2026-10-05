const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const root = path.resolve(__dirname, '../..')
const filename = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
const ast = ts.createSourceFile('leader.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = new Set(['parseProductionEventQuery', 'requireProductionHandoffDetail', 'handoffNavigationEpoch', 'navigateToLeaderHandoff'])
const statements = ast.statements.filter(statement => ts.isVariableStatement(statement)
  && statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
for (const name of ['parseProductionEventQuery', 'handoffNavigationEpoch', 'navigateToLeaderHandoff']) {
  assert.equal(statements.flatMap(statement => [...statement.declarationList.declarations])
    .filter(declaration => declaration.name.getText(ast) === name).length, 1)
}
const code = ts.transpileModule(statements.map(statement => statement.getText(ast)).join('\n'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.None }
}).outputText
const navigationCode = ts.transpileModule(fs.readFileSync(path.join(root, 'src/utils/activeOrderHandoffNavigation.ts'), 'utf8'), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const navigation = { exports: {} }
Function('exports', 'module', 'require', 'window', navigationCode)(navigation.exports, navigation,
  () => ({}), { location: { origin: 'http://localhost:8081' } })

// Identity fields transcribed from the retained natural 341 GET for event288016/cycle414.
const taskId = '2106841125730213888'
function task(leader = 'PRODUCTION', options = {}) {
  const result = { id: taskId, activeOrderId: 1009200414, workOrderId: 990274,
    routeProcessId: 9908090670, taskType: `${leader}_REVIEW`, sourceType: 'PROCESS_POOL_EVENT',
    sourceId: 288016, roundId: 288016, status: 'TODO', ...options }
  result.actionUrl = `/mes/pro/process-pool/${leader === 'PRODUCTION' ? 'production' : 'pqc'}-leader`
    + `?activeOrderId=${result.activeOrderId}&eventId=${result.sourceId}&handoffTaskId=${result.id}&roundId=${result.roundId}&handoffType=${result.taskType}`
  return result
}
function payload(options = {}) {
  return { activeOrderId: 1009200414, workOrderId: 990274, routeId: 980091,
    routeProcessId: 9908090670, processId: 922985,
    activeOrderProcess: { activeOrderId: 1009200414, routeProcessId: 9908090670,
      processId: 922985, activeOrderProcessSnapshotId: 1009206270 }, ...options }
}
function allocation(options = {}) {
  return { allocationId: 5793, activeOrderId: 1009200414, workOrderId: 990274,
    allocatedQuantity: 10, overageQuantity: 0, needsAdjustment: false, released: false, editable: true, ...options }
}
function production(options = {}) {
  return { id: 288016, activeOrderId: null, workOrderId: 990274, routeId: 980091,
    routeProcessId: 9908090670, processId: 922985, submissionReviewStatus: null,
    originalPayloadJson: JSON.stringify(payload()), reportAllocations: [allocation()], ...options }
}
const query = value => Object.fromEntries(new URL(value.actionUrl, 'http://localhost:8081').searchParams)
function harness(leader = 'PRODUCTION', original = task(leader), selected = production()) {
  const route = vue.reactive({ query: query(original) })
  const detail = vue.ref({ id: 999 }), detailVisible = vue.ref(true), errors = [], calls = []
  let context = { task: original, current: true, processable: true }
  let response = selected
  const values = { route, detail, detailVisible, handoffActiveOrderAnchor: vue.ref(),
    handoffNavigationContext: async id => { calls.push(['context', id]); return typeof context === 'function' ? context(id) : context },
    resolveActiveOrderHandoffTarget: navigation.exports.resolveActiveOrderHandoffTarget,
    resolveCurrentLeaderType: () => leader,
    getTeamLeaderSubmissionDetail: async (id, type) => { calls.push(['detail', id, type]); return typeof response === 'function' ? response(id, type) : response },
    pqcDetailQuery: { pageNo: 5 }, activePqcModuleTab: vue.ref(), activeProductionModuleTab: vue.ref(),
    loadError: vue.ref(), resolveErrorMessage: error => error.message,
    ElMessage: { error: error => errors.push(error) }, isProductionLeader: vue.ref(leader === 'PRODUCTION'),
    loadActiveOrders: async () => { calls.push(['orders']) }, activeOrderOptions: vue.ref([]), activeOrderQuery: { pageNo: 1 } }
  const page = Function(...Object.keys(values), code + ';return {navigateToLeaderHandoff};')(...Object.values(values))
  return { ...values, page, errors, calls, setContext: value => { context = value }, setResponse: value => { response = value } }
}
function rejected(h) {
  assert.equal(h.detail.value, undefined)
  assert.equal(h.detailVisible.value, false)
  assert.equal(h.errors.length, 1)
}

test('真实生产组长父SFC模板可编译，测试执行真实交接导航方法', () => {
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'production-handoff' }).errors, [])
})
test('原自然生产DTO顶层周期NULL，准确载荷和allocation5793仍打开event288016复核', async () => {
  const h = harness(); await h.page.navigateToLeaderHandoff()
  assert.deepEqual(h.errors, []); assert.equal(h.detail.value.id, 288016)
  assert.equal(h.detailVisible.value, true); assert.equal(h.activeProductionModuleTab.value, 'report')
  assert.deepEqual(h.calls.filter(call => call[0] === 'detail'), [['detail', 288016, 'PRODUCTION']])
})
test('原REJECTED已退休分配，DONE严格只读仍能查看原拒绝结果', async () => {
  const original = task('PRODUCTION', { status: 'DONE' })
  const h = harness('PRODUCTION', original, production({ submissionReviewStatus: 'REJECTED', reportAllocations: [] }))
  h.route.query.handoffReadOnly = '1'; h.setContext({ task: original, current: true, processable: false })
  await h.page.navigateToLeaderHandoff(); assert.deepEqual(h.errors, [])
  assert.equal(h.detail.value.submissionReviewStatus, 'REJECTED'); assert.equal(h.activeProductionModuleTab.value, 'reportHistory')
})
test('本人补正新revision round待FIFO，PENDING零CURRENT分配仍打开原event再审', async () => {
  const original = task('PRODUCTION', { roundId: 287170 })
  const h = harness('PRODUCTION', original, production({ submissionReviewStatus: 'PENDING', reportAllocations: [] }))
  await h.page.navigateToLeaderHandoff(); assert.deepEqual(h.errors, [])
  assert.equal(h.detail.value.id, 288016); assert.equal(h.detailVisible.value, true)
})
test('首次待复核缺原周期分配不能借其他订单allocation通过', async () => {
  for (const reportAllocations of [[], [allocation({ allocationId: 6000, activeOrderId: 1009200415, workOrderId: 990275 })]]) {
    const h = harness('PRODUCTION', task(), production({ reportAllocations }))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
})
test('共享生产池合法跨单分配保留，原周期关联不是数组第一项', async () => {
  const h = harness('PRODUCTION', task(), production({ reportAllocations: [
    allocation({ allocationId: 6000, activeOrderId: 1009200415, workOrderId: 990275 }), allocation()] }))
  await h.page.navigateToLeaderHandoff(); assert.deepEqual(h.errors, []); assert.equal(h.detail.value.id, 288016)
})
test('DTO显式错误cycle不能用正确payload或allocation掩盖', async () => {
  const h = harness('PRODUCTION', task(), production({ activeOrderId: 1009200415 }))
  await h.page.navigateToLeaderHandoff(); rejected(h)
})
test('DTO工单工序或冻结任务工序错误不能被正确载荷替代', async () => {
  for (const change of [{ workOrderId: 990275 }, { routeProcessId: 9908090671 }, { processId: 922986 }, { routeId: 980092 }]) {
    const h = harness('PRODUCTION', task(), production(change))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
  const original = task('PRODUCTION', { routeProcessId: 9908090671 })
  const h = harness('PRODUCTION', original); await h.page.navigateToLeaderHandoff(); rejected(h)
})
test('生产载荷root和activeOrderProcess必须同时精确绑定原cycle工序工单', async () => {
  const changes = [{ activeOrderId: 1009200415 }, { workOrderId: 990275 }, { routeProcessId: 9908090671 },
    { processId: 922986 }, { routeId: 980092 }, { activeOrderProcess: { ...payload().activeOrderProcess, activeOrderId: 1009200415 } },
    { activeOrderProcess: { ...payload().activeOrderProcess, routeProcessId: 9908090671 } },
    { activeOrderProcess: { ...payload().activeOrderProcess, processId: 922986 } }, { activeOrderProcess: undefined },
    { activeOrderId: undefined }, { workOrderId: undefined }]
  for (const change of changes) {
    const h = harness('PRODUCTION', task(), production({ activeOrderId: 1009200414, originalPayloadJson: JSON.stringify(payload(change)) }))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
})
test('畸形或缺失生产正式payload显式失败且不打开旧抽屉', async () => {
  for (const originalPayloadJson of [undefined, '', '{bad', 'null', '[]', '"wrong"']) {
    const h = harness('PRODUCTION', task(), production({ activeOrderId: 1009200414, originalPayloadJson }))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
})
test('有CURRENT分配时精确行身份、原工单和唯一原周期关联都必须有效', async () => {
  for (const reportAllocations of [undefined, [allocation({ workOrderId: 990275 })],
    [allocation(), allocation()], [allocation(), allocation({ allocationId: 6000 })],
    [allocation({ allocationId: 0 })], [allocation(), allocation({ allocationId: 6000, activeOrderId: null })]]) {
    const h = harness('PRODUCTION', task(), production({ activeOrderId: 1009200414, reportAllocations }))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
})
test('DTO事件或任务source/round不一致必须拒绝，不能只验证URL', async () => {
  for (const change of [{ sourceType: 'PQC_INSPECTION_TASK' }, { sourceId: 288017 }, { roundId: 287170 }]) {
    const h = harness(); h.setContext({ task: { ...task(), ...change }, current: true, processable: true })
    await h.page.navigateToLeaderHandoff(); rejected(h)
    assert.equal(h.calls.filter(call => call[0] === 'detail').length, 0)
  }
  const h = harness('PRODUCTION', task(), production({ id: 288017 })); await h.page.navigateToLeaderHandoff(); rejected(h)
})
test('补正零分配不是泛用例外，首次PENDING和新round APPROVED缺分配均拒绝', async () => {
  for (const [original, selected] of [[task(), production({ submissionReviewStatus: 'PENDING', reportAllocations: [] })],
    [task('PRODUCTION', { roundId: 287170 }), production({ submissionReviewStatus: 'APPROVED', reportAllocations: [] })]]) {
    const h = harness('PRODUCTION', original, selected); await h.page.navigateToLeaderHandoff(); rejected(h)
  }
})
test('篡改原cycle、event或round的页面查询在详情读取前失败', async () => {
  for (const change of [{ activeOrderId: '1009200415' }, { eventId: '288017' }, { roundId: '287170' }, { workOrderId: '990274' }]) {
    const h = harness(); Object.assign(h.route.query, change); await h.page.navigateToLeaderHandoff(); rejected(h)
    assert.equal(h.calls.filter(call => call[0] === 'detail').length, 0)
  }
})
test('旧周期或CANCELED任务一律在详情读取前明确失效', async () => {
  for (const context of [{ task: task(), current: false, processable: false },
    { task: task('PRODUCTION', { status: 'CANCELED' }), current: true, processable: false }]) {
    const h = harness(); h.route.query.handoffReadOnly = '1'; h.setContext(context)
    await h.page.navigateToLeaderHandoff(); rejected(h); assert.match(h.errors[0], /旧周期/)
    assert.equal(h.calls.filter(call => call[0] === 'detail').length, 0)
  }
})
test('PQC保持顶层周期严格合同，生产payload和分配不能补齐NULL或错误PQC周期', async () => {
  for (const activeOrderId of [null, 1009200415]) {
    const h = harness('PQC', task('PQC'), production({ activeOrderId }))
    await h.page.navigateToLeaderHandoff(); rejected(h)
  }
  const h = harness('PQC', task('PQC'), { id: 288016, activeOrderId: 1009200414 })
  await h.page.navigateToLeaderHandoff(); assert.deepEqual(h.errors, []); assert.equal(h.activePqcModuleTab.value, 'detail')
})
test('已完成任务没有严格readonly参数不能继续办理', async () => {
  const original = task('PRODUCTION', { status: 'DONE' }), h = harness('PRODUCTION', original)
  h.setContext({ task: original, current: true, processable: false }); await h.page.navigateToLeaderHandoff(); rejected(h)
  assert.equal(h.calls.filter(call => call[0] === 'detail').length, 0)
})
test('A详情迟到不能覆盖已经打开的B原事件', async () => {
  const h = harness(); let resolveA
  const pending = new Promise(resolve => { resolveA = resolve })
  const next = task('PRODUCTION', { id: '2106841125730213889', sourceId: 288017, roundId: 288017 })
  h.setResponse(id => id === 288016 ? pending : production({ id: 288017 }))
  const first = h.page.navigateToLeaderHandoff(); await new Promise(resolve => setImmediate(resolve))
  h.route.query = query(next); h.setContext({ task: next, current: true, processable: true })
  await h.page.navigateToLeaderHandoff(); resolveA(production()); await first
  assert.deepEqual(h.errors, []); assert.equal(h.detail.value.id, 288017); assert.equal(h.detailVisible.value, true)
})
test('A任务上下文迟到不能继续读原详情或覆盖已经打开的B', async () => {
  const h = harness(); let resolveA
  const pending = new Promise(resolve => { resolveA = resolve })
  const next = task('PRODUCTION', { id: '2106841125730213889', sourceId: 288017, roundId: 288017 })
  h.setContext(id => id === taskId ? pending : { task: next, current: true, processable: true })
  h.setResponse(() => production({ id: 288017 }))
  const first = h.page.navigateToLeaderHandoff(); h.route.query = query(next)
  await h.page.navigateToLeaderHandoff(); resolveA({ task: task(), current: true, processable: true }); await first
  assert.deepEqual(h.errors, []); assert.equal(h.detail.value.id, 288017)
  assert.deepEqual(h.calls.filter(call => call[0] === 'detail'), [['detail', 288017, 'PRODUCTION']])
})
test('正式详情读取失败可见，不自动打开泛用工单或沿用旧详情', async () => {
  const h = harness(); h.setResponse(() => { throw new Error('正式详情读取失败') })
  await h.page.navigateToLeaderHandoff(); rejected(h); assert.equal(h.errors[0], '正式详情读取失败')
  assert.equal(h.calls.filter(call => call[0] === 'orders').length, 0)
})
