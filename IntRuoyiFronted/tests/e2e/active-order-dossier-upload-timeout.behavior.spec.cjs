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
const load = (relative, dependencies) => {
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
  vm.runInNewContext(compiled, { module, exports: module.exports, require: requireDependency, FormData, File }, { filename })
  return module.exports
}
const { config } = load('src/config/axios/config.ts', {})
const flush = async () => { for (let i = 0; i < 16; i++) await Promise.resolve() }
const command = () => ({ activeOrderId: '1009200422', applicationId: '234', categoryKey: 'INCOMING_INSPECTION_FILE', file: new File(['task-owned dossier'], 'incoming.txt', { type: 'text/plain' }) })
function harness(t, delay = 35000, envelope = { code: 0, data: { fileId: '40', fileName: 'incoming.txt' } }, rejectWith) {
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
          options.onUploadProgress?.({ loaded: 18, total: 18 })
          resolve({ data: envelope, status: 200, statusText: 'OK', headers: {}, config: options })
        }, delay)
      })
    }
  })
  // Mock only the service response boundary; execute the real request/upload and API wrappers.
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

test('dossier upload waits for a 35s formal response and retains multipart identity/progress', async t => {
  const h = harness(t), input = command(), progress = [], onProgress = event => progress.push(event)
  const pending = observe(h.api.uploadActiveOrderDossierFile(input, onProgress))
  await flush()
  assert.equal(h.calls.length, 1)
  const call = h.calls[0]
  assert.equal(call.url, '/mes/pro/process-pool/team-leader/active-order/dossier-files/upload')
  assert.equal(call.method, 'post')
  assert.equal(call.data.get('activeOrderId'), input.activeOrderId)
  assert.equal(call.data.get('applicationId'), input.applicationId)
  assert.equal(call.data.get('categoryKey'), input.categoryKey)
  assert.equal(call.data.get('file'), input.file)
  assert.equal(call.onUploadProgress, onProgress)
  t.mock.timers.tick(30000); await flush()
  assert.equal(pending.state(), 'pending', 'a 35s save must survive the global 30s boundary')
  t.mock.timers.tick(4999); await flush(); assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  assert.deepEqual((await pending.outcome).value, { fileId: '40', fileName: 'incoming.txt' })
  assert.deepEqual(progress, [{ loaded: 18, total: 18 }])
  assert.equal(call.timeout, 180000)
  assert.equal(h.calls.length, 1)
})

test('dossier upload rejects over its 180s budget with the same transport error and no retry', async t => {
  const h = harness(t, 180001), pending = observe(h.api.uploadActiveOrderDossierFile(command()))
  await flush(); t.mock.timers.tick(179999); await flush()
  assert.equal(pending.state(), 'pending')
  t.mock.timers.tick(1); await flush()
  const result = await pending.outcome
  assert.equal(result.error, h.lastFailure())
  assert.equal(result.error.code, 'ECONNABORTED')
  assert.equal(result.error.config.timeout, 180000)
  t.mock.timers.tick(1000); await flush(); assert.equal(h.calls.length, 1)
})

test('dossier upload keeps missing data explicit and preserves application omission semantics', async t => {
  const h = harness(t, 1, { code: 0 }), input = command()
  delete input.applicationId
  const pending = observe(h.api.uploadActiveOrderDossierFile(input))
  await flush(); t.mock.timers.tick(1); await flush()
  assert.match((await pending.outcome).error.message, /资料文件上传响应缺少 data，不能确认文件已入账/)
  assert.equal(h.calls[0].data.has('applicationId'), false)
  assert.equal(h.calls[0].data.get('activeOrderId'), input.activeOrderId)
  assert.equal(h.calls.length, 1)
})

test('dossier upload passes service rejection through unchanged without a synthetic receipt', async t => {
  const failure = new Error('formal upload refused'), h = harness(t, 1, undefined, failure)
  const pending = observe(h.api.uploadActiveOrderDossierFile(command()))
  await flush(); t.mock.timers.tick(1); await flush()
  assert.equal((await pending.outcome).error, failure)
  assert.equal(h.calls.length, 1)
})

test('dossier reads/deletes retain the global 30s budget and completion keeps its 180s override', async t => {
  const h = harness(t), read = observe(h.api.getActiveOrderDossierFiles({ activeOrderId: '1009200422', applicationId: '234' }))
  const deletion = observe(h.api.deleteActiveOrderDossierFile({ activeOrderId: '1009200422', attachmentId: '40' }))
  const completion = observe(h.api.applyTeamLeaderActiveOrderRelease({ activeOrderId: '1009200422' }))
  await flush()
  assert.equal(config.request_timeout, 30000)
  assert.deepEqual(h.calls.map(call => call.timeout), [30000, 30000, 180000])
  t.mock.timers.tick(30000); await flush()
  assert.equal((await read.outcome).error.code, 'ECONNABORTED')
  assert.equal((await deletion.outcome).error.code, 'ECONNABORTED')
  assert.equal(completion.state(), 'pending')
  t.mock.timers.tick(5000); await flush()
  assert.deepEqual((await completion.outcome).value, { fileId: '40', fileName: 'incoming.txt' })
  assert.equal(h.calls.length, 3)
})
