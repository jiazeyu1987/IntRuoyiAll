const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const source = fs.readFileSync(path.join(__dirname, '../src/views/mes/pro/edhr-batch/components/BatchIntegrityCheck.vue'), 'utf8')
function component(checkIntegrityChunk) {
  const script = parse(source).descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
  const js = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return new Function('ref', 'computed', 'onBeforeUnmount', 'checkIntegrityChunk', js + ';return {start,cancel,checked,errors,status,running,failure};')(vue.ref, vue.computed, () => {}, checkIntegrityChunk)
}
test('does not run before explicit start; propagates server cursor and reports errors', async () => {
  const calls = []
  const app = component(async request => {
    calls.push(request)
    return calls.length === 1
      ? { checked: 128, errors: [{ batchId: 4, message: 'SOURCE_MISSING' }], nextId: 150, upperId: 200, status: 'RUNNING' }
      : { checked: 20, errors: [], nextId: 200, upperId: 200, status: 'COMPLETED' }
  })
  assert.equal(calls.length, 0)
  await app.start()
  assert.deepEqual(calls[1], { afterId: 150, upperId: 200 })
  assert.equal(app.checked.value, 148)
  assert.equal(app.errors.value.length, 1)
  assert.equal(app.status.value, 'COMPLETED')
})
test('cancellation prevents the next request and preserves completed chunk results', async () => {
  let resolve
  let count = 0
  const app = component(() => { count++; return new Promise(done => { resolve = done }) })
  const work = app.start()
  app.cancel()
  resolve({ checked: 128, errors: [], nextId: 128, upperId: 400, status: 'RUNNING' })
  await work
  assert.equal(count, 1)
  assert.equal(app.status.value, 'CANCELLED')
  assert.equal(app.checked.value, 128)
  assert.equal(app.running.value, false)
})
test('service failure is visible and cannot report completed', async () => {
  const app = component(async () => { throw new Error('database offline') })
  await app.start()
  assert.equal(app.status.value, 'FAILED')
  assert.equal(app.failure.value, 'database offline')
  assert.equal(app.running.value, false)
})
