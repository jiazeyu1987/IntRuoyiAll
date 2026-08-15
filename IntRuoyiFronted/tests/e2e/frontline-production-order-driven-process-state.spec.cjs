const assert = require('node:assert/strict')
const fs = require('node:fs')
const Module = require('node:module')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const typescript = require(path.join(root, 'node_modules/typescript'))
const sourcePath = path.join(
  root,
  'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts'
)
const source = fs.readFileSync(sourcePath, 'utf8')
const compiled = typescript.transpileModule(source, {
  compilerOptions: {
    module: typescript.ModuleKind.CommonJS,
    target: typescript.ScriptTarget.ES2020
  },
  fileName: sourcePath
}).outputText

let processResponse = []
const testModule = new Module(sourcePath, module)
testModule.filename = sourcePath
testModule.paths = Module._nodeModulePaths(path.dirname(sourcePath))
const originalLoad = Module._load
Module._load = function load(request, parent, isMain) {
  if (request === '@/api/mes/pro/feedback') {
    return {
      ProFeedbackApi: {
        getFrontlineDeviceAccountProcesses: async () => processResponse
      }
    }
  }
  return originalLoad.call(this, request, parent, isMain)
}
try {
  testModule._compile(compiled, sourcePath)
} finally {
  Module._load = originalLoad
}

const {
  createFrontlineDeviceEmployeeState,
  selectFrontlineProductionActiveOrder
} = testModule.exports

const routeOneFirst = {
  routeId: 101,
  routeProcessId: 1001,
  processId: 11,
  processName: '球囊粗洗',
  sort: 1
}
const routeOneSecond = {
  routeId: 101,
  routeProcessId: 1002,
  processId: 12,
  processName: '球囊精洗',
  sort: 2
}
const routeTwoFirst = {
  routeId: 202,
  routeProcessId: 2001,
  processId: 21,
  processName: '按压式组装',
  sort: 1
}
const routeTwoSecond = {
  routeId: 202,
  routeProcessId: 2002,
  processId: 22,
  processName: '按压式检测',
  sort: 2
}
const balloonOrder = {
  workOrderId: 501,
  workOrderCode: 'WO-BALLOON',
  productId: 1,
  productName: '球囊扩张压力泵',
  quantity: 100,
  routeId: 101
}
const pushOrder = {
  workOrderId: 502,
  workOrderCode: 'WO-PUSH',
  productId: 2,
  productName: '按压式压力泵',
  quantity: 80,
  routeId: 202
}

const run = async () => {
  const state = createFrontlineDeviceEmployeeState()
  processResponse = [routeOneFirst, routeOneSecond, routeTwoFirst, routeTwoSecond]

  const balloonProcesses = await selectFrontlineProductionActiveOrder(state, balloonOrder)
  assert.deepEqual(balloonProcesses, [routeOneFirst, routeOneSecond])
  assert.equal(state.selectedActiveOrder, balloonOrder)
  assert.equal(state.selectedProcess, undefined)

  state.selectedProcess = routeOneSecond
  state.selectedEmployee = { userId: 99, nickname: '旧员工' }
  state.runtimeConfig = { devices: [] }
  state.template = { templateNo: 'OLD', routeProcessId: 1002, processId: 12, actualEmployeeId: 99 }

  const processTokenBeforeSwitch = state.processSelectionRequestToken
  const employeeTokenBeforeSwitch = state.employeeSwitchRequestToken
  const pushProcesses = await selectFrontlineProductionActiveOrder(state, pushOrder)
  assert.deepEqual(pushProcesses, [routeTwoFirst, routeTwoSecond])
  assert.ok(pushProcesses.every((process) => process.routeId === pushOrder.routeId))
  assert.equal(state.selectedActiveOrder, pushOrder)
  assert.equal(state.selectedProcess, undefined)
  assert.equal(state.selectedEmployee, undefined)
  assert.equal(state.runtimeConfig, undefined)
  assert.equal(state.template, undefined)
  assert.equal(state.processSelectionRequestToken, processTokenBeforeSwitch + 1)
  assert.equal(state.employeeSwitchRequestToken, employeeTokenBeforeSwitch + 1)

  await assert.rejects(
    selectFrontlineProductionActiveOrder(state, {
      ...pushOrder,
      workOrderId: 503,
      workOrderCode: 'WO-MISSING-ROUTE-PROCESSES',
      routeId: 303
    }),
    /正式工艺路线没有可用工序/
  )
  assert.deepEqual(state.processOptions, [])
  assert.match(state.lastError, /正式工艺路线没有可用工序/)

  console.log('PASS: production order state transition isolates refreshed route processes and clears stale context')
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
