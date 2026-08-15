const assert = require('node:assert/strict')
const fs = require('node:fs')
const Module = require('node:module')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const typescript = require(path.join(root, 'node_modules/typescript'))
const sourcePath = path.join(root, 'src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const source = fs.readFileSync(sourcePath, 'utf8')
const compiled = typescript.transpileModule(source, {
  compilerOptions: {
    module: typescript.ModuleKind.CommonJS,
    target: typescript.ScriptTarget.ES2020
  },
  fileName: sourcePath
}).outputText

let processRequest
let processRequestCount = 0
const testModule = new Module(sourcePath, module)
testModule.filename = sourcePath
testModule.paths = Module._nodeModulePaths(path.dirname(sourcePath))
const originalLoad = Module._load
Module._load = function load(request, parent, isMain) {
  if (request === '@/api/mes/pro/feedback') {
    return {
      ProFeedbackApi: {
        getFrontlineDeviceAccountProcesses: async () => {
          processRequestCount += 1
          return await processRequest()
        }
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
  FrontlineProductionStaleActiveOrderSelectionError,
  createFrontlineDeviceEmployeeState,
  selectFrontlineProductionActiveOrder
} = testModule.exports

const routeOneProcess = {
  routeId: 101,
  routeProcessId: 1001,
  processId: 11,
  processName: '球囊粗洗',
  sort: 1
}
const routeTwoProcess = {
  routeId: 202,
  routeProcessId: 2001,
  processId: 21,
  processName: '按压式组装',
  sort: 1
}
const balloonOrder = {
  activeOrderId: 401,
  workOrderId: 501,
  workOrderCode: 'WO-BALLOON',
  productId: 1,
  productName: '球囊扩张压力泵',
  quantity: 100,
  routeId: 101
}
const pushOrder = {
  activeOrderId: 402,
  workOrderId: 502,
  workOrderCode: 'WO-PUSH',
  productId: 2,
  productName: '按压式压力泵',
  quantity: 80,
  routeId: 202
}

const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

const run = async () => {
  const state = createFrontlineDeviceEmployeeState()
  state.productionProcessOptions = [routeOneProcess]
  state.processOptions = [routeOneProcess]
  state.selectedActiveOrder = balloonOrder
  state.selectedProcess = routeOneProcess
  state.selectedEmployee = { userId: 99, nickname: '旧员工' }
  state.runtimeConfig = { devices: [] }
  state.template = { templateNo: 'OLD' }

  processRequest = async () => [routeOneProcess, routeTwoProcess]
  const pushProcesses = await selectFrontlineProductionActiveOrder(state, pushOrder)
  assert.equal(processRequestCount, 1, 'switching an order must issue a new process request')
  assert.deepEqual(pushProcesses, [routeTwoProcess])
  assert.deepEqual(state.productionProcessOptions, [routeOneProcess, routeTwoProcess])
  assert.equal(state.selectedActiveOrder, pushOrder)
  assert.equal(state.selectedProcess, undefined)
  assert.equal(state.selectedEmployee, undefined)
  assert.equal(state.runtimeConfig, undefined)
  assert.equal(state.template, undefined)

  processRequest = async () => {
    throw new Error('工序读取失败')
  }
  await assert.rejects(
    selectFrontlineProductionActiveOrder(state, balloonOrder),
    /工序读取失败/
  )
  assert.equal(state.selectedActiveOrder, balloonOrder)
  assert.deepEqual(state.productionProcessOptions, [])
  assert.deepEqual(state.processOptions, [])
  assert.match(state.lastError, /工序读取失败/)

  const firstRequest = deferred()
  const secondRequest = deferred()
  let requestIndex = 0
  processRequest = async () => {
    requestIndex += 1
    return await (requestIndex === 1 ? firstRequest.promise : secondRequest.promise)
  }
  const firstSwitch = selectFrontlineProductionActiveOrder(state, balloonOrder)
  const secondSwitch = selectFrontlineProductionActiveOrder(state, pushOrder)
  secondRequest.resolve([routeOneProcess, routeTwoProcess])
  assert.deepEqual(await secondSwitch, [routeTwoProcess])
  firstRequest.resolve([routeOneProcess])
  await assert.rejects(
    firstSwitch,
    (error) => error instanceof FrontlineProductionStaleActiveOrderSelectionError
  )
  assert.equal(state.selectedActiveOrder, pushOrder)
  assert.deepEqual(state.processOptions, [routeTwoProcess])

  console.log('PASS: production order refresh replaces stale processes and rejects late responses')
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
