const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const page = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')

function harness(name, history, rejected = false, releaseApplicationCount = 0) {
  const start = page.indexOf(`const ${name} =`)
  assert.ok(start >= 0)
  const end = page.indexOf('\nconst ', start + 1)
  const source = ts.transpileModule(page.slice(start, end) + `\nglobalThis.run = ${name}`, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  const calls = { rebuild: 0, simulate: 0, confirm: 0, refresh: 0, success: [], errors: [], payloads: [] }
  const context = {
    activeOrderRebuildSubmittingId: { value: undefined },
    activeOrderSimulationSubmittingId: { value: undefined },
    activeOrderConflictSubmitting: { value: false },
    activeOrderConflictSelectedOrder: { value: { id: 81 } },
    activeOrderConflictDrawerVisible: { value: true }, activeOrderConflictDetail: { value: {} },
    requirePositiveNumber: n => n,
    previewTeamLeaderActiveOrderRebuild: async () => ({ hasHistoricalRuntimeData: history, releaseApplicationCount }),
    rebuildTeamLeaderActiveOrder: async (payload) => {
      calls.payloads.push(payload)
      calls.rebuild++
      if (rejected) throw new Error('已有正式证据，禁止重建')
      return { rebuiltProcessSnapshotCount: 2, rebuiltPqcTaskCount: 4 }
    },
    simulateTeamLeaderActiveOrderCompletion: async () => { calls.simulate++; return {} },
    formatActiveOrderProgressPercent: () => '100%',
    loadActiveOrders: async () => { calls.refresh++ },
    ElMessageBox: { confirm: async () => { calls.confirm++ } },
    ElMessage: { success: m => calls.success.push(m), error: m => calls.errors.push(m) },
    resolveErrorMessage: (e, fallback) => e.message || fallback
  }
  vm.createContext(context)
  vm.runInContext(source, context)
  return { context, calls }
}

for (const name of ['handleRebuildActiveOrder', 'handleRecommendedActiveOrderConflictResolution']) {
  test(`${name}: existing evidence blocks all writes and clears loading`, async () => {
    const { context, calls } = harness(name, true)
    await context.run({ id: 81 })
    assert.equal(calls.rebuild, 0)
    assert.equal(calls.simulate, 0)
    assert.equal(calls.confirm, 0)
    assert.equal(calls.success.length, 0)
    assert.match(calls.errors.join(' '), /证据.*禁止重建/)
    assert.equal(context.activeOrderRebuildSubmittingId.value, undefined)
    assert.equal(context.activeOrderConflictSubmitting.value, false)
  })
  test(`${name}: empty preview still permits rebuild`, async () => {
    const { context, calls } = harness(name, false)
    await context.run({ id: 81 })
    assert.equal(calls.rebuild, 1)
    assert.equal(calls.payloads[0].confirmDeleteHistoricalRuntimeData, false)
    assert.equal(calls.refresh, 1)
    assert.equal(calls.errors.length, 0)
  })
  test(`${name}: evidence created after preview is shown as server rejection`, async () => {
    const { context, calls } = harness(name, false, true)
    await context.run({ id: 81 })
    assert.equal(calls.rebuild, 1)
    assert.equal(calls.simulate, 0)
    assert.equal(calls.success.length, 0)
    assert.match(calls.errors.join(' '), /已有正式证据，禁止重建/)
  })
  test(`${name}: release evidence alone blocks rebuild and simulation`, async () => {
    const { context, calls } = harness(name, false, false, 1)
    await context.run({ id: 81 })
    assert.equal(calls.rebuild, 0)
    assert.equal(calls.simulate, 0)
    assert.equal(calls.confirm, 0)
    assert.equal(calls.success.length, 0)
    assert.match(calls.errors.join(' '), /禁止重建/)
  })
}
