const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const start = source.indexOf('const searchActiveOrderCandidates =')
const end = source.indexOf('const resolveActiveOrderCandidateByKeyword =', start)
assert.ok(start > 0 && end > start)
const script = ts.transpile(source.slice(start, end), { target: ts.ScriptTarget.ES2022 })
const ref = (value) => ({ value })

function setup({ sync, search } = {}) {
  const alerts = []
  const state = {
    activeOrderErpSyncing: ref(false), maintenanceSubmitting: ref(false),
    activeOrderErpCode: ref(' MO-001 '), activeOrderCandidateLoading: ref(true),
    activeOrderCandidateOptions: ref([{ workOrderId: 99 }]),
    activeOrderSyncedCandidates: ref([]),
    activeOrderCandidateKeyword: ref('OLD'), activeOrderCandidateError: ref(''),
    activeOrderForm: { workOrderId: 99 }, activeOrderSelectedCandidate: ref({ workOrderId: 99 }),
    activeOrderCandidateRequest: 0,
    syncTeamLeaderProductionOrder: sync || (async (code) => ({ code, syncedCount: 1 })),
    searchTeamLeaderActiveOrderCandidates: search || (async () => [{ workOrderId: 1, workOrderCode: 'MO-001' }]),
    resolveErrorMessage: (error) => error.message,
    normalizePositiveNumber: (value) => Number(value) > 0 ? Number(value) : undefined,
    ElMessage: { error: () => {} },
    ElMessageBox: { alert: async (...args) => alerts.push(args) },
    addTeamLeaderActiveOrder: () => assert.fail('同步不能加入活跃订单')
  }
  state.handleActiveOrderCandidateClear = () => {
    state.activeOrderForm.workOrderId = undefined
    state.activeOrderSelectedCandidate.value = undefined
    state.activeOrderCandidateKeyword.value = ''
    state.activeOrderCandidateError.value = ''
  }
  state.findActiveOrderCandidateById = (id) => state.activeOrderCandidateOptions.value.find((row) => row.workOrderId === id)
  vm.createContext(state)
  vm.runInContext(`${script}\nthis.run = syncActiveOrderFromErp; this.search = searchActiveOrderCandidates`, state)
  return { state, alerts }
}

test('同步成功只刷新本地候选，清除原选择并保留人工确认步骤', async () => {
  const { state, alerts } = setup()
  await state.run()
  assert.equal(state.activeOrderForm.workOrderId, undefined)
  assert.equal(state.activeOrderSelectedCandidate.value, undefined)
  assert.equal(state.activeOrderCandidateOptions.value[0].workOrderCode, 'MO-001')
  assert.match(alerts[0][0], /已同步到本地生产订单列表.*确认工艺路线/)
  assert.equal(alerts[0][2].type, 'success')
  assert.equal(state.activeOrderErpSyncing.value, false)
})

for (const message of ['ERP中未找到生产订单 MO-001，请检查订单号是否正确', 'ERP连接或请求失败，请检查ERP服务及网络连接', 'ERP配置缺失']) {
  test(`同步失败弹框保留准确原因：${message}`, async () => {
    const { state, alerts } = setup({ sync: async () => { throw new Error(message) } })
    await state.run()
    assert.ok(alerts[0][0].includes(message))
    assert.equal(alerts[0][1], 'ERP订单同步失败')
    assert.equal(state.activeOrderErpSyncing.value, false)
  })
}

test('候选刷新失败必须说明订单已同步，不能误报同步失败', async () => {
  const { state, alerts } = setup({ search: async () => { throw new Error('本地搜索服务不可用') } })
  await state.run()
  assert.match(alerts[0][0], /已同步到本地生产订单列表，但下拉候选刷新失败/)
  assert.equal(alerts[0][2].type, 'warning')
  assert.equal(state.activeOrderForm.workOrderId, undefined)
})

test('同步处理中禁止重复提交', async () => {
  let complete
  let calls = 0
  const { state } = setup({ sync: () => { calls++; return new Promise((resolve) => { complete = resolve }) } })
  const pending = state.run()
  await state.run()
  assert.equal(calls, 1)
  complete({ code: 'MO-001', syncedCount: 1 })
  await pending
})

test('同步前发出的旧搜索结果不能覆盖同步后的候选', async () => {
  let finishOldSearch
  const { state } = setup({ search: (keyword) => keyword === 'OLD'
    ? new Promise((resolve) => { finishOldSearch = resolve })
    : Promise.resolve([{ workOrderId: 1, workOrderCode: keyword }]) })
  const oldSearch = state.search('OLD')
  await state.run()
  finishOldSearch([{ workOrderId: 99, workOrderCode: 'OLD' }])
  await oldSearch
  assert.equal(state.activeOrderCandidateOptions.value[0].workOrderCode, 'MO-001')
  assert.equal(state.activeOrderForm.workOrderId, undefined)
})

test('清空搜索会结束加载状态，旧请求结束后仍保持清空', async () => {
  let finishSearch
  const { state } = setup({ search: () => new Promise((resolve) => { finishSearch = resolve }) })
  const pending = state.search('OLD')
  await state.search('')
  assert.equal(state.activeOrderCandidateLoading.value, false)
  finishSearch([{ workOrderId: 99 }])
  await pending
  assert.equal(state.activeOrderCandidateOptions.value.length, 0)
})

test('同步后首次打开空白下拉框直接展示同步候选，不自动选择', async () => {
  const { state } = setup()
  await state.run()
  await state.search('')
  assert.equal(state.activeOrderCandidateOptions.value[0].workOrderCode, 'MO-001')
  assert.equal(state.activeOrderForm.workOrderId, undefined)
  assert.equal(state.activeOrderSelectedCandidate.value, undefined)
})
