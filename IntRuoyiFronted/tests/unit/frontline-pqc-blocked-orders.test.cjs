const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')

const blocked = { activeOrderId: 1, readBlocked: true, readBlockReason: '订单缺少检验规程来源快照' }
const healthy = { activeOrderId: 2, readBlocked: false }

function context(api = {}) {
  const source = fs.readFileSync('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts', 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText
  const module = { exports: {} }
  new Function('require', 'module', 'exports', code)(name => {
    assert.equal(name, '@/api/mes/pro/feedback')
    return { ProFeedbackApi: api }
  }, module, module.exports)
  return module.exports
}

function initializer(orders) {
  const source = fs.readFileSync('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue', 'utf8')
    .match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('panel.ts', source, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s) &&
    s.declarationList.declarations.some(d => d.name.getText(ast) === 'refreshPqcActiveOrdersAndEnsureSelection'))
  const calls = []
  const state = {}
  const code = ts.transpileModule(`let pqcActiveOrderRefreshPromise;\n${statement.getText(ast)}\nreturn refreshPqcActiveOrdersAndEnsureSelection`, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  const run = new Function('hasReturnNotification', 'isPqcMode', 'hasInitialHandoffQuery',
    'deviceState', 'loadFrontlinePqcActiveOrders', 'handleSelectActiveOrder', code)(
    { value: false }, { value: true }, { value: false }, state, async () => orders,
    async order => { calls.push(order.activeOrderId); state.selectedActiveOrder = order })
  return { run, state, calls }
}

test('entry selects the first usable order even when the newest order is blocked', async () => {
  const h = initializer([blocked, healthy])
  await h.run()
  assert.deepEqual(h.calls, [healthy.activeOrderId])
  assert.equal(h.state.lastError, undefined)
})

test('entry reports the blocking reason when every order is blocked', async () => {
  const h = initializer([blocked])
  await h.run()
  assert.deepEqual(h.calls, [])
  assert.equal(h.state.lastError, blocked.readBlockReason)
})

test('refresh clears a blocked selection and its process cache while retaining usable orders', async () => {
  const api = context({ getFrontlinePqcActiveOrders: async () => [blocked, healthy] })
  const state = api.createFrontlineDeviceEmployeeState()
  Object.assign(state, { selectedActiveOrder: blocked, selectedProcess: {}, selectedEmployee: {},
    runtimeConfig: {}, template: {}, processOptions: [{}], employeeOptions: [{}] })
  state.pqcProcessOptionsCache.set('1:10', [{}])
  state.pqcProcessOptionsCache.set('2:10', [{}])
  const result = await api.loadFrontlinePqcActiveOrders(state)
  assert.deepEqual(result, [blocked, healthy])
  assert.equal(state.selectedActiveOrder, undefined)
  assert.equal(state.selectedProcess, undefined)
  assert.equal(state.selectedEmployee, undefined)
  assert.deepEqual(state.processOptions, [])
  assert.equal(state.pqcProcessOptionsCache.has('1:10'), false)
  assert.equal(state.pqcProcessOptionsCache.has('2:10'), true)
})

test('a blocked order cannot load processes or replace the current usable selection', async () => {
  const api = context({ getPqcProcesses: () => { throw new Error('must not request blocked processes') } })
  const state = api.createFrontlineDeviceEmployeeState()
  state.selectedActiveOrder = healthy
  await assert.rejects(api.selectFrontlinePqcActiveOrder(state, blocked, 10), { message: blocked.readBlockReason })
  assert.equal(state.selectedActiveOrder, healthy)
  assert.equal(state.lastError, blocked.readBlockReason)
})

test('unexpected order-list failures still reject and retain the explicit error', async () => {
  const api = context({ getFrontlinePqcActiveOrders: async () => { throw new Error('服务不可用') } })
  const state = api.createFrontlineDeviceEmployeeState()
  await assert.rejects(api.loadFrontlinePqcActiveOrders(state), { message: '服务不可用' })
  assert.equal(state.lastError, '服务不可用')
  assert.equal(state.loadingActiveOrders, false)
})
