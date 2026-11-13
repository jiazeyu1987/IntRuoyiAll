const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const page = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const start = page.indexOf('const handleResetFixedSimulationActiveOrder =')
const end = page.indexOf('\nconst ', start + 1)
assert.ok(start >= 0 && end > start)
const handler = ts.transpileModule(page.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText
const reason = '指定测试订单配置未就绪：生效路线版本未包含唯一测试产品，请先在页面关联产品并发布新版本，再重置'
const receipt = { workOrderCode: 'FIXED-TEST', activeOrderId: '42' }
const ref = value => ({ value })

function harness({ code, refreshFails = false }) {
  const errors = [], successes = []
  let writes = 0, refreshes = 0
  const context = {
    activeOrderTestResetBlocked: ref(false), activeOrderTestResetSubmitting: ref(false),
    activeOrderTestResetReceipt: ref(undefined), activeOrderWorkOrderKeyword: ref('ORIGINAL'),
    activeOrderQuery: { pageNo: 3 }, activeOrderOptions: ref([]),
    ElMessageBox: { confirm: async () => {} },
    ElMessage: { error: value => errors.push(value), success: value => successes.push(value) },
    resolveErrorMessage: (error, fallback) => error?.message || fallback,
    resetFixedSimulationActiveOrder: async () => {
      writes++
      if (!refreshFails) throw Object.assign(new Error(reason), { code })
      return receipt
    },
    loadActiveOrders: async () => {
      refreshes++
      throw Object.assign(new Error(reason), { code })
    }
  }
  vm.createContext(context)
  vm.runInContext(handler + '\nglobalThis.run = handleResetFixedSimulationActiveOrder', context)
  return { context, errors, successes, run: context.run,
    get writes() { return writes }, get refreshes() { return refreshes } }
}

for (const code of [1040760414, '1040760414']) {
  test(`setup rejection ${typeof code} is definite and preserves the prerequisite guidance`, async () => {
    const h = harness({ code })
    await h.run()
    assert.equal(h.context.activeOrderTestResetReceipt.value.state, 'rejected')
    assert.match(h.errors[0], /重置已拒绝，本次未完成重置/)
    assert.ok(h.errors[0].includes(reason))
    assert.doesNotMatch(h.errors[0], /结果未确认|不要再次重置/)
    assert.equal(h.context.activeOrderWorkOrderKeyword.value, 'ORIGINAL')
    assert.equal(h.context.activeOrderQuery.pageNo, 3)
    assert.equal(h.context.activeOrderTestResetSubmitting.value, false)
    assert.equal(h.writes, 1)
    assert.equal(h.refreshes, 0)
    assert.equal(h.successes.length, 0)
  })
}

test('transport failure still requires checking results without an automatic retry', async () => {
  const h = harness({ code: 'ECONNRESET' })
  await h.run()
  assert.equal(h.context.activeOrderTestResetReceipt.value.state, 'unknown')
  assert.match(h.errors[0], /结果未确认.*不要再次重置/)
  assert.equal(h.writes, 1)
  assert.equal(h.context.activeOrderTestResetSubmitting.value, false)
})

test('a setup error during refresh cannot claim a completed reset was rejected', async () => {
  const h = harness({ code: 1040760414, refreshFails: true })
  await h.run()
  assert.equal(h.context.activeOrderTestResetReceipt.value.state, 'unknown')
  assert.match(h.errors[0], /已重置，但列表刷新失败/)
  assert.equal(h.writes, 1)
  assert.equal(h.refreshes, 1)
  assert.equal(h.successes.length, 0)
})
