const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const { ref, computed } = require('vue')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const source = fs.readFileSync(filename, 'utf8')
const start = source.indexOf('const handlePrepareFixedTestOrderData =')
const end = source.indexOf('const handleResetFixedSimulationActiveOrder =', start)
assert.ok(start >= 0 && end > start)
const handler = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

const receipt = code => ({ workOrderCode: code, documents: [
  '生产订单', '领料单明细', '补料单明细', '生产用料清单明细'
].map(name => ({ name, createdCount: 1, deletedCount: 0, skippedCount: 0, repairedCount: 0 })) })

function setup(second = async () => receipt('ORDER-2')) {
  const calls = [], messages = []
  const state = {
    activeOrderTestDataSet: ref(1), activeOrderTestDataSubmitting: ref(false),
    activeOrderTestResetSubmitting: ref(false), activeOrderTestDataReceipt: ref(),
    enabled: ref(true)
  }
  state.activeOrderTestResetBlocked = computed(() => !state.enabled.value ||
    state.activeOrderTestResetSubmitting.value || state.activeOrderTestDataSubmitting.value)
  const deps = { ...state,
    prepareFixedTestOrderData: async () => { calls.push(1); return receipt('ORDER-1') },
    prepareSecondFixedTestOrderData: async () => { calls.push(2); return second() },
    ElMessage: { success: msg => messages.push(['success', msg]), error: msg => messages.push(['error', msg]) },
    resolveErrorMessage: (error, fallback) => error.message || fallback
  }
  const prepare = Function(...Object.keys(deps), `${handler}; return handlePrepareFixedTestOrderData`)(...Object.values(deps))
  return { state, calls, messages, prepare }
}

test('the complete workbench SFC compiles and both buttons select their own data set', () => {
  const { descriptor, errors } = parse(source, { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'second-test-data' })
  const template = compileTemplate({ source: descriptor.template.content, filename,
    id: 'second-test-data', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
  for (const [attribute, set] of [['fixed', 1], ['second', 2]]) {
    const button = source.match(new RegExp(`<el-button[^>]*data-team-leader-prepare-${attribute}-test-data[^>]*>[\\s\\S]*?</el-button>`))?.[0]
    assert.ok(button)
    assert.ok(button.includes(`handlePrepareFixedTestOrderData(${set})`))
    assert.ok(button.includes('v-if="testModeEnabled"'))
    assert.ok(button.includes("mes:pro-process-pool-team-leader:maintain"))
  }
})

test('data2 calls the second endpoint and displays its actual order; original data keeps its endpoint', async () => {
  const f = setup()
  await f.prepare(2)
  assert.deepEqual(f.calls, [2])
  assert.equal(f.state.activeOrderTestDataReceipt.value.success, true)
  assert.match(f.state.activeOrderTestDataReceipt.value.message, /数据2补齐完成（生产订单：ORDER-2）/)
  await f.prepare(1)
  assert.deepEqual(f.calls, [2, 1])
  assert.match(f.state.activeOrderTestDataReceipt.value.message, /数据补齐完成（生产订单：ORDER-1）/)
})

test('in-flight data2 blocks both preparation actions and releases the mutation lock', async () => {
  let finish
  const f = setup(() => new Promise(resolve => { finish = resolve }))
  const pending = f.prepare(2)
  assert.equal(f.state.activeOrderTestDataSet.value, 2)
  assert.equal(f.state.activeOrderTestResetSubmitting.value, true)
  await f.prepare(1)
  await f.prepare(2)
  assert.deepEqual(f.calls, [2])
  finish(receipt('ORDER-2'))
  await pending
  assert.equal(f.state.activeOrderTestDataSubmitting.value, false)
  assert.equal(f.state.activeOrderTestResetSubmitting.value, false)
  f.state.enabled.value = false
  await f.prepare(2)
  assert.deepEqual(f.calls, [2])
})

test('a failure or incomplete receipt remains a visible failure without retrying the first endpoint', async () => {
  for (const respond of [async () => { throw Error('关联单据冲突') }, async () => ({ workOrderCode: 'ORDER-2', documents: [] })]) {
    const f = setup(respond)
    await f.prepare(2)
    assert.deepEqual(f.calls, [2])
    assert.equal(f.state.activeOrderTestDataReceipt.value.success, false)
    assert.equal(f.messages[0][0], 'error')
    assert.equal(f.state.activeOrderTestResetSubmitting.value, false)
    assert.equal(f.state.activeOrderTestDataSubmitting.value, false)
  }
})

test('repairs are displayed separately from created and skipped records; an invalid repair receipt fails', async () => {
  const repaired = receipt('ORDER-2')
  repaired.documents[1] = { name: '领料单明细', createdCount: 0, deletedCount: 0, skippedCount: 0, repairedCount: 30 }
  const f = setup(async () => repaired)
  await f.prepare(2)
  assert.match(f.state.activeOrderTestDataReceipt.value.message, /领料单明细新增0条、跳过已有0条、修复关联30条/)
  assert.match(f.messages[0][1], /错误用料单关联已修复/)
  const invalid = receipt('ORDER-2')
  delete invalid.documents[1].repairedCount
  const missing = setup(async () => invalid)
  await missing.prepare(2)
  assert.equal(missing.state.activeOrderTestDataReceipt.value.success, false)
})

test('the API exports use independent fixed endpoints with the existing timeout and visible failure handling', async () => {
  const api = fs.readFileSync(path.resolve(__dirname, '../../src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
  const calls = [], exports = {}
  const js = ts.transpileModule(api, { compilerOptions: { module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2022 } }).outputText
  Function('exports', 'require', js)(exports, name => {
    assert.equal(name, '@/config/axios')
    return { default: { post: async options => { calls.push(options); return receipt('ORDER') } } }
  })
  await exports.prepareFixedTestOrderData()
  await exports.prepareSecondFixedTestOrderData()
  assert.deepEqual(calls.map(row => row.url), [
    '/mes/pro/process-pool/team-leader/active-order/simulation/test-data',
    '/mes/pro/process-pool/team-leader/active-order/simulation/test-data-2'
  ])
  assert.ok(calls.every(row => row.timeout === 150000 && row.ignoreErrorMessage === true))
})
