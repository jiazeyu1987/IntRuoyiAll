const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/dcc/controlled-file/training/task/index.vue'), 'utf8')
const start = source.indexOf('const startSessionIfNeeded =')
const end = source.indexOf('const stopSession =', start)
assert.ok(start >= 0 && end > start)
assert.match(source, /import \{ createSecureUuid \} from '@\/utils\/uuid'/)
const code = ts.transpileModule(source.slice(start, end) + '\nexports.start = startSessionIfNeeded', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto, options = {}) {
  const calls = [], stops = [], timers = []
  const sessionActive = { value: false }
  const context = {
    exports: {}, ...loadUuid(crypto), startRequestSequence: 0,
    activeClientSessionId: '', mounted: true,
    progressId: { value: '123' }, task: { value: {} }, sessionActive,
    isSessionCountable: () => !options.hidden,
    startTrainingViewSession: async (id, payload) => {
      calls.push({ id, payload })
      if (options.failStart) throw new Error('start failed')
      return { accumulatedViewSeconds: 5 }
    },
    stopTrainingViewSessionKeepalive: async (id, payload) => stops.push({ id, payload }),
    window: { setInterval: (callback, delay) => { timers.push({ callback, delay }); return 1 } }
  }
  vm.runInNewContext(code, context)
  return { context, start: context.exports.start, calls, stops, timers, sessionActive }
}

const httpCrypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }
const uuidV4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

test('HTTP training starts with UUID v4 and does not restart an active session', async () => {
  const h = setup(httpCrypto)
  await h.start()
  assert.equal(h.calls.length, 1)
  assert.match(h.calls[0].payload.clientSessionId, uuidV4)
  assert.equal(h.context.activeClientSessionId, h.calls[0].payload.clientSessionId)
  assert.equal(h.sessionActive.value, true)
  assert.equal(h.timers[0].delay, 5000)
  await h.start()
  assert.equal(h.calls.length, 1)
})

test('missing random source prevents requests and leaves training inactive', async () => {
  const h = setup({})
  await assert.rejects(h.start(), /不支持安全随机数/)
  assert.equal(h.calls.length, 0)
  assert.equal(h.context.activeClientSessionId, '')
  assert.equal(h.sessionActive.value, false)
  assert.equal(h.timers.length, 0)
})

test('failed HTTP start can retry with a fresh session without starting a timer prematurely', async () => {
  const options = { failStart: true }
  const h = setup(httpCrypto, options)
  await assert.rejects(h.start(), /start failed/)
  assert.equal(h.sessionActive.value, false)
  assert.equal(h.timers.length, 0)
  options.failStart = false
  await h.start()
  assert.equal(h.calls.length, 2)
  assert.notEqual(h.calls[0].payload.clientSessionId, h.calls[1].payload.clientSessionId)
  assert.equal(h.sessionActive.value, true)
})
