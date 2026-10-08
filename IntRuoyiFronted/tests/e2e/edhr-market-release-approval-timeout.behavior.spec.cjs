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
const command = () => ({
  releaseTransactionId: '236', workTaskId: '2760', expectedVersion: 7,
  idempotencyKey: 'task-owned-approval-key', signoffEvidenceHash: 'task-owned-signoff-hash',
  password: 'test-only-signature-input', approvalOpinion: 'task-owned approval'
})
const receipt = { releaseTransactionId: '236', releaseStatus: 'RELEASED', approvedBy: '9908090347' }
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
  // The transport is virtual; Axios config merging and both production TS wrappers are real.
  service.interceptors.response.use(response => response.data)
  const request = load('src/config/axios/index.ts', { './service': { service }, './config': { config } }).default
  const api = load('src/api/mes/pro/edhr/release.ts', { '@/config/axios': request })
  return { api, calls, lastFailure: () => lastFailure }
}
function observe(promise) {
  let state = 'pending'
  const outcome = promise.then(value => { state = 'resolved'; return { value } }, error => { state = 'rejected'; return { error } })
  return { outcome, state: () => state }
}

test('market approval waits for a 35s formal response with its exact signing identity', async t => {
  const h = harness(t), input = command(), pending = observe(h.api.approveEdhrRelease(input))
  await flush()
  assert.equal(h.calls.length, 1)
  const call = h.calls[0]
  assert.equal(call.url, '/mes/pro/edhr-release/approve')
  assert.equal(call.method, 'post')
  assert.deepEqual(JSON.parse(call.data), input)
  assert.equal(call.headers.get('Content-Type'), 'application/json')
  t.mock.timers.tick(30000); await flush()
  assert.equal(pending.state(), 'pending', 'a 35s approval must survive the global 30s boundary')
  t.mock.timers.tick(4999); await flush(); assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  assert.deepEqual((await pending.outcome).value, receipt)
  assert.equal(call.timeout, 180000)
  assert.equal(h.calls.length, 1)
})

test('market approval rejects at 180s with the original timeout error and never retries', async t => {
  const h = harness(t, 180001), pending = observe(h.api.approveEdhrRelease(command()))
  await flush(); t.mock.timers.tick(179999); await flush()
  assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  const result = await pending.outcome
  assert.equal(result.error, h.lastFailure())
  assert.equal(result.error.code, 'ECONNABORTED')
  assert.equal(result.error.config.timeout, 180000)
  t.mock.timers.tick(1000); await flush(); assert.equal(h.calls.length, 1)
})

test('market approval preserves a service failure without fabricating a release receipt', async t => {
  const failure = new Error('formal approval refused'), h = harness(t, 1, failure)
  const pending = observe(h.api.approveEdhrRelease(command()))
  await flush(); t.mock.timers.tick(1); await flush()
  const result = await pending.outcome
  assert.equal(result.error, failure)
  assert.equal(Object.hasOwn(result, 'value'), false)
  assert.equal(h.calls.length, 1)
})

test('all adjacent release reads and commands retain the global 30s timeout', async t => {
  const h = harness(t)
  const pending = [
    observe(h.api.getEdhrReleasePage({ pageNo: 1, pageSize: 20 })),
    observe(h.api.getEdhrRelease('236')),
    observe(h.api.precheckEdhrRelease({ releaseTransactionId: '236' })),
    observe(h.api.submitEdhrRelease(command())),
    observe(h.api.rejectEdhrRelease({ releaseTransactionId: '236', idempotencyKey: 'task-owned-reject', rejectReason: 'task-owned reason' })),
    observe(h.api.withdrawEdhrRelease({ releaseTransactionId: '236', idempotencyKey: 'task-owned-withdraw', withdrawReason: 'task-owned reason' })),
    observe(h.api.getEdhrReleaseCheckItemPage({ releaseTransactionId: '236', pageNo: 1, pageSize: 20 })),
    observe(h.api.getEdhrReleaseEventPage({ releaseTransactionId: '236', pageNo: 1, pageSize: 20 }))
  ]
  await flush()
  assert.equal(config.request_timeout, 30000)
  assert.equal(h.calls.length, pending.length)
  assert.ok(h.calls.every(call => call.timeout === 30000))
  t.mock.timers.tick(29999); await flush()
  assert.ok(pending.every(result => result.state() === 'pending'))
  t.mock.timers.tick(1); await flush()
  for (const result of pending) assert.equal((await result.outcome).error.code, 'ECONNABORTED')
  t.mock.timers.tick(1000); await flush(); assert.equal(h.calls.length, pending.length)
})
