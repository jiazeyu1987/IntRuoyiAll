const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')

const page = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const ref = value => ({ value })
const extract = name => {
  const start = page.indexOf(`const ${name} =`)
  const end = page.indexOf('\nconst ', start + 1)
  assert.ok(start >= 0 && end > start, `missing ${name}`)
  return page.slice(start, end)
}
const receipt = { workOrderCode: 'FIXED-TEST-01', activeOrderId: '9007199254741001', workOrderId: 42 }
const createHarness = (overrides = {}) => {
  const calls = [], successes = [], errors = [], warnings = []
  const context = {
    activeOrderTestResetSubmitting: ref(false), maintenanceSubmitting: ref(false),
    activeOrderSimulationSubmittingId: ref(undefined), teamLeaderDataCleanupSubmitting: ref(false),
    activeOrderMoveSubmittingId: ref(undefined), activeOrderRebuildSubmittingId: ref(undefined),
    activeOrderVersionUpgradeSubmittingId: ref(undefined), releaseApplicationSubmittingId: ref(undefined),
    abnormalSubmitting: ref(false), activeOrderConflictSubmitting: ref(false),
    activeOrderWorkOrderKeyword: ref('OLD-ORDER'), activeOrderQuery: { pageNo: 8, pageSize: 10 },
    activeOrderOptions: ref([]),
    computed: getter => ({ get value() { return getter() } }),
    resetFixedSimulationActiveOrder: async (...args) => { calls.push(args); return receipt },
    loadActiveOrders: async () => { context.activeOrderOptions.value = [{ id: receipt.activeOrderId, workOrderCode: receipt.workOrderCode }] },
    ElMessage: { success: message => successes.push(message), error: message => errors.push(message), warning: message => warnings.push(message) },
    resolveErrorMessage: (error, fallback) => error?.message || fallback,
    ...overrides
  }
  vm.createContext(context)
  const busy = page.includes('const activeOrderTestResetBlocked =') ? extract('activeOrderTestResetBlocked') : 'const activeOrderTestResetBlocked = { value: false }'
  vm.runInContext(ts.transpileModule(busy + '\n' + extract('handleResetFixedSimulationActiveOrder') + '\nglobalThis.run = handleResetFixedSimulationActiveOrder', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { context, calls, successes, errors, warnings, run: context.run }
}

test('reset keeps the no-argument API and focuses the returned fixed order on page one', async () => {
  const h = createHarness()
  await h.run()
  assert.equal(h.calls.length, 1)
  assert.equal(h.calls[0].length, 0)
  assert.equal(h.context.activeOrderWorkOrderKeyword.value, receipt.workOrderCode)
  assert.equal(h.context.activeOrderQuery.pageNo, 1)
  assert.equal(h.context.activeOrderTestResetSubmitting.value, false)
  assert.equal(h.successes.length, 1)
  assert.match(h.successes[0], /本轮测试数据已清理/)
})

test('a repeated click while reset is pending sends only one request', async () => {
  let count = 0
  const resolvers = []
  const h = createHarness({ resetFixedSimulationActiveOrder: () => { count++; return new Promise(done => { resolvers.push(done) }) } })
  const pending = h.run()
  const repeated = h.run()
  for (const resolve of resolvers) resolve(receipt)
  await Promise.all([pending, repeated])
  assert.equal(count, 1)
})

test('a completed reset unlocks the same button for the next deliberate reset', async () => {
  const h = createHarness()
  await h.run()
  await h.run()
  assert.equal(h.calls.length, 2)
  assert.equal(h.successes.length, 2)
  assert.equal(h.context.activeOrderTestResetSubmitting.value, false)
})

test('reset cannot overlap maintenance, simulation or another active-order mutation', async () => {
  for (const [key, value] of Object.entries({ maintenanceSubmitting: true, activeOrderSimulationSubmittingId: 7, teamLeaderDataCleanupSubmitting: true, activeOrderMoveSubmittingId: 7, activeOrderRebuildSubmittingId: 7, activeOrderVersionUpgradeSubmittingId: 7, releaseApplicationSubmittingId: 7, abnormalSubmitting: true, activeOrderConflictSubmitting: true })) {
    const h = createHarness({ [key]: ref(value) })
    await h.run()
    assert.equal(h.calls.length, 0, key)
  }
})

test('request failure remains visible, preserves current filter and unlocks reset', async () => {
  const h = createHarness({ resetFixedSimulationActiveOrder: async () => { throw new Error('reset refused') } })
  await h.run()
  assert.equal(h.successes.length, 0)
  assert.match(h.errors[0], /reset refused/)
  assert.equal(h.context.activeOrderWorkOrderKeyword.value, 'OLD-ORDER')
  assert.equal(h.context.activeOrderTestResetSubmitting.value, false)
})

test('refresh failure reports that reset already succeeded and does not repeat the write', async () => {
  const h = createHarness({ loadActiveOrders: async () => { throw new Error('network unavailable') } })
  await h.run()
  assert.equal(h.calls.length, 1)
  assert.equal(h.successes.length, 0)
  assert.match(h.errors[0], /已重置.*列表刷新失败/)
  assert.match(h.errors[0], /network unavailable/)
  assert.equal(h.context.activeOrderWorkOrderKeyword.value, receipt.workOrderCode)
})

test('missing reset identity or a refreshed list without that identity is not shown as success', async () => {
  for (const result of [{ ...receipt, workOrderCode: '' }, { ...receipt, activeOrderId: undefined }]) {
    const h = createHarness({ resetFixedSimulationActiveOrder: async () => result })
    await h.run()
    assert.equal(h.successes.length, 0)
    assert.equal(h.errors.length, 1)
  }
  const h = createHarness({ loadActiveOrders: async () => {} })
  await h.run()
  assert.equal(h.successes.length, 0)
  assert.match(h.errors[0], /未找到.*活跃订单/)
})

test('visible reset and mutation controls prevent concurrent work and retain original reset semantics', () => {
  const resetButton = page.match(/<el-button\s[^>]*data-team-leader-reset-fixed-active-order[\s\S]*?<\/el-button>/)?.[0]
  assert.ok(resetButton)
  assert.match(resetButton, /:disabled="activeOrderTestResetBlocked"/)
  assert.match(resetButton, /重置指定测试订单/)
  assert.match(resetButton, /清理.*本轮.*测试/)
  for (const marker of ['data-team-leader-simulate-active-order-stage1-p1', 'data-team-leader-active-order-release-apply', 'data-team-leader-data-cleanup']) {
    const index = page.indexOf(marker)
    const start = page.lastIndexOf('<el-button', index)
    assert.match(page.slice(start, index), /activeOrderTestResetSubmitting/, marker)
  }
})

test('active-order mutation handlers cannot start while reset owns the page', async () => {
  const handlers = ['submitAddActiveOrder', 'submitActiveOrderReleaseApplication', 'submitMoveActiveOrder', 'handleRebuildActiveOrder', 'handleActiveOrderVersionUpgrade', 'submitActiveOrderVersionUpgrade', 'handleCopyLatestSimulationActiveOrder', 'handleCleanupLatestSimulationActiveOrder', 'handleSimulateStage1', 'handleGenerateStage1Forms', 'handleSimulateStage2_5', 'handlePushGeneratedPqcRelease', 'handleRecommendedActiveOrderConflictResolution', 'handleRemoveActiveOrder', 'handleTeamLeaderDataCleanup', 'openActiveOrderDialog', 'openAbnormalDialog']
  for (const name of handlers) {
    const context = { activeOrderTestResetSubmitting: ref(true) }
    vm.createContext(context)
    vm.runInContext(ts.transpileModule(extract(name) + `\nglobalThis.run = ${name}`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
    // No API, dialog or mutable state is supplied: reaching any of them is a failure.
    await context.run({ id: 42 }, 'UP')
  }
})
