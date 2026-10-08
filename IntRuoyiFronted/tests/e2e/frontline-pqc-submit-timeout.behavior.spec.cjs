const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { createRequire } = require('node:module')

const frontend = path.resolve(__dirname, '../..')
const localRequire = createRequire(path.join(frontend, 'package.json'))
const { transformSync } = createRequire(localRequire.resolve('vite'))('esbuild')
const axios = localRequire('axios')

function load(relative, dependencies) {
  const filename = path.join(frontend, relative)
  const compiled = transformSync(fs.readFileSync(filename, 'utf8'), {
    loader: 'ts', format: 'cjs', target: 'es2022',
    define: { 'import.meta.env': JSON.stringify({ VITE_BASE_URL: '', VITE_API_URL: '/admin-api' }) }
  }).code
  const module = { exports: {} }
  const requireDependency = name => {
    assert.ok(Object.hasOwn(dependencies, name), 'Unexpected runtime dependency: ' + name)
    return dependencies[name]
  }
  vm.runInNewContext(compiled, { module, exports: module.exports, require: requireDependency }, { filename })
  return module.exports
}

const { config } = load('src/config/axios/config.ts', {})
const projection = load('src/api/mes/pro/feedback/pqcProjection.ts', {})
const flush = async () => { for (let i = 0; i < 16; i++) await Promise.resolve() }
const command = () => ({
  activeOrderId: 1009200425, pqcTaskId: 1009640705,
  regulationVersionId: 70, qaProcessId: 170,
  actualEmployeeId: 9908090344, workOrderId: 990274, routeId: 980091,
  inspectionType: 'FIRST', businessDate: '2026-10-07', shiftCode: 'DAY', roundNo: 1,
  actualInspectionQuantity: 10, scrapQuantity: 0,
  signaturePassword: 'test-only-signature-input',
  itemResults: [{ itemCode: 'PQC-IDI-001-I011', qualified: true }],
  rawPayload: { pqcPieceValues: { appearance: ['PASS', 'PASS'] }, inspectionResult: 'SUCCESS' },
  clientSubmitTime: '2026-10-07 04:00:00'
})
const receipt = {
  pqcTaskId: 1009640705, pqcEventId: 296363, sourceRevision: 296363,
  payloadHash: 'formal-server-canonical-hash', pqcRecordId: 290656,
  signatureId: 31686, inspectionResult: 'SUCCESS', serverSubmitTime: 1791317825000
}

function harness(t, delay = 35000, rejectWith) {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  const calls = []
  let lastFailure
  const service = axios.create({
    timeout: config.request_timeout,
    adapter: options => {
      calls.push(options)
      return new Promise((resolve, reject) => {
        let responseTimer
        const timeoutTimer = setTimeout(() => {
          clearTimeout(responseTimer)
          lastFailure = new axios.AxiosError('timeout of ' + options.timeout + 'ms exceeded', 'ECONNABORTED', options)
          reject(lastFailure)
        }, options.timeout)
        responseTimer = setTimeout(() => {
          clearTimeout(timeoutTimer)
          if (rejectWith) { lastFailure = rejectWith; reject(rejectWith); return }
          resolve({ data: { code: 0, data: receipt }, status: 200, statusText: 'OK', headers: {}, config: options })
        }, delay)
      })
    }
  })
  // Only transport time is virtual; Axios merging and both production TS wrappers execute.
  service.interceptors.response.use(response => response.data)
  const request = load('src/config/axios/index.ts', { './service': { service }, './config': { config } }).default
  const api = load('src/api/mes/pro/feedback/index.ts', { '@/config/axios': request, './pqcProjection': projection }).ProFeedbackApi
  return { api, calls, lastFailure: () => lastFailure }
}

function observe(promise) {
  let state = 'pending'
  const outcome = promise.then(value => { state = 'resolved'; return { value } }, error => { state = 'rejected'; return { error } })
  return { outcome, state: () => state }
}

test('PQC formal submission survives 30s and preserves its exact signed payload and receipt', async t => {
  const h = harness(t), input = command(), original = JSON.parse(JSON.stringify(input))
  const pending = observe(h.api.submitFrontlinePqcInspection(input))
  await flush()
  assert.equal(h.calls.length, 1)
  const call = h.calls[0]
  assert.equal(call.url, '/mes/pro/feedback/frontline/device-account/pqc/submit')
  assert.equal(call.method, 'post')
  assert.equal(call.ignoreErrorMessage, true)
  assert.deepEqual(JSON.parse(call.data), original)
  assert.deepEqual(input, original)
  t.mock.timers.tick(30000); await flush()
  assert.equal(pending.state(), 'pending', 'a 35s formal submission must survive the default 30s boundary')
  t.mock.timers.tick(4999); await flush(); assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  assert.deepEqual((await pending.outcome).value, receipt)
  assert.equal(call.timeout, 180000)
  assert.equal(h.calls.length, 1)
})

test('PQC formal submission rejects at 180s with the original Axios error and never retries', async t => {
  const h = harness(t, 180001), pending = observe(h.api.submitFrontlinePqcInspection(command()))
  await flush(); t.mock.timers.tick(179999); await flush()
  assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  const result = await pending.outcome
  assert.equal(result.error, h.lastFailure())
  assert.equal(result.error.code, 'ECONNABORTED')
  assert.equal(result.error.config.timeout, 180000)
  t.mock.timers.tick(1000); await flush(); assert.equal(h.calls.length, 1)
})

test('PQC formal submission preserves an explicit rejection without a receipt read or synthetic success', async t => {
  const failure = new axios.AxiosError('formal submit refused', 'ERR_BAD_REQUEST', undefined, undefined, { status: 409 })
  const h = harness(t, 1, failure), input = command(), pending = observe(h.api.submitFrontlinePqcInspection(input))
  await flush(); t.mock.timers.tick(1); await flush()
  assert.equal((await pending.outcome).error, failure)
  assert.equal(h.calls.length, 1)
  assert.deepEqual(JSON.parse(h.calls[0].data), input)
})

test('PQC receipt reads and employee switches retain the default 30s budget', async t => {
  const h = harness(t)
  const read = observe(h.api.getFrontlinePqcSubmitReceipt({ pqcTaskId: 1009640705 }))
  const change = observe(h.api.switchFrontlinePqcActualEmployee({ activeOrderId: 1009200425, actualEmployeeId: 9908090344 }))
  await flush()
  assert.equal(config.request_timeout, 30000)
  assert.deepEqual(h.calls.map(call => call.timeout), [30000, 30000])
  assert.deepEqual(h.calls.map(call => call.method), ['get', 'post'])
  t.mock.timers.tick(30000); await flush()
  assert.equal((await read.outcome).error.code, 'ECONNABORTED')
  assert.equal((await change.outcome).error.code, 'ECONNABORTED')
  assert.equal(h.calls.length, 2)
})
