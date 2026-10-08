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
const flush = async () => { for (let i = 0; i < 16; i++) await Promise.resolve() }
const command = () => ({ activeOrderId: '1009200426', expectedVersion: 1, simulationRunId: 'STAGE2_5-123456789' })
const receipt = {
  simulationRunId: 'STAGE2_5-123456789', batchExecutionId: '900000009999', completionReceiptId: '9988',
  detailPath: '/mes/pro/edhr/batch-execution/detail?id=900000009999',
  batchExecutionSnapshot: { activeOrderId: '1009200426', workOrderId: '990274' }, blockers: []
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
  // Time is virtual. Real Axios merging and the actual request/API wrappers execute.
  service.interceptors.response.use(response => response.data)
  const request = load('src/config/axios/index.ts', { './service': { service }, './config': { config } }).default
  const api = load('src/api/mes/pro/processpool/teamLeader.ts', { '@/config/axios': request })
  return { api, calls, lastFailure: () => lastFailure }
}

function observe(promise) {
  let state = 'pending'
  const outcome = promise.then(value => { state = 'resolved'; return { value } }, error => { state = 'rejected'; return { error } })
  return { outcome, state: () => state }
}

test('P2 backfill survives 30s and preserves the clicked cycle, version, run and formal receipt', async t => {
  const h = harness(t), input = command(), original = structuredClone(input)
  const pending = observe(h.api.simulateStage2_5BackfillBatchExecution(input))
  await flush()
  assert.equal(h.calls.length, 1)
  const call = h.calls[0]
  assert.equal(call.url, '/mes/pro/process-pool/team-leader/active-order/simulation/stage2-5')
  assert.equal(call.method, 'post')
  assert.equal(call.ignoreErrorMessage, true)
  assert.deepEqual(JSON.parse(call.data), original)
  assert.deepEqual(input, original)
  t.mock.timers.tick(30000); await flush()
  assert.equal(pending.state(), 'pending', 'a 35s P2 backfill must survive the default 30s boundary')
  t.mock.timers.tick(5000); await flush()
  assert.deepEqual((await pending.outcome).value, receipt)
  assert.equal(call.timeout, 180000)
  assert.equal(h.calls.length, 1)
})

test('P2 rejects at 180s with the original Axios error and never repeats the write', async t => {
  const h = harness(t, 180001), pending = observe(h.api.simulateStage2_5BackfillBatchExecution(command()))
  await flush(); t.mock.timers.tick(179999); await flush()
  assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  const result = await pending.outcome
  assert.equal(result.error, h.lastFailure())
  assert.equal(result.error.code, 'ECONNABORTED')
  assert.equal(result.error.config.timeout, 180000)
  t.mock.timers.tick(1000); await flush()
  assert.equal(h.calls.length, 1)
})

test('P2 propagates an explicit rejection without changing its command or synthesizing success', async t => {
  const failure = new axios.AxiosError('formal backfill refused', 'ERR_BAD_REQUEST', undefined, undefined, { status: 409 })
  const h = harness(t, 1, failure), input = command()
  const pending = observe(h.api.simulateStage2_5BackfillBatchExecution(input))
  await flush(); t.mock.timers.tick(1); await flush()
  assert.equal((await pending.outcome).error, failure)
  assert.equal(h.calls.length, 1)
  assert.deepEqual(JSON.parse(h.calls[0].data), input)
})

test('active-order reads retain the global 30s timeout and P1 retains its existing budget', async t => {
  const h = harness(t)
  const read = observe(h.api.getTeamLeaderActiveOrderList())
  const p1 = observe(h.api.simulateStage1ActiveOrderCompletion({ activeOrderId: '1009200426' }))
  await flush()
  assert.equal(config.request_timeout, 30000)
  assert.deepEqual(h.calls.map(call => call.timeout), [30000, 120000])
  assert.deepEqual(h.calls.map(call => call.method), ['get', 'post'])
  t.mock.timers.tick(30000); await flush()
  assert.equal((await read.outcome).error.code, 'ECONNABORTED')
  t.mock.timers.tick(5000); await flush()
  assert.deepEqual((await p1.outcome).value, receipt)
  assert.equal(h.calls.length, 2)
})
