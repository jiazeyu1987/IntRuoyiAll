import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import test from 'node:test'
import ts from 'typescript'

const source = readFileSync(new URL('../src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue', import.meta.url), 'utf8')
const start = source.indexOf('let batchListReadGeneration = 0')
const end = source.indexOf('const edhrBatchQuickFilter', start)
assert.ok(start >= 0 && end > start)
const code = ts.transpileModule(source.slice(start, end), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function harness() {
  const pending = []
  let timer
  const context = {
    loading: { value: false }, loadError: { value: '' }, list: { value: [] }, total: { value: 0 },
    releaseLoading: { value: false }, selectedReleaseBatch: { value: null }, releaseDialogVisible: { value: false },
    releaseForm: { password: '' }, message: { warning() {} }, document: { hidden: false },
    route: { fullPath: '/batch' }, approvalInvalidation: { syncSession: () => ({ key: 'actor-1' }) },
    buildQuery: () => ({ pageNo: 1 }), isVisibleBatchExecutionRow: () => true,
    resolveErrorMessage: error => error.message,
    getEdhrBatchExecutionPage: () => new Promise((resolve, reject) => pending.push({ resolve, reject })),
    setInterval(callback, ms) { assert.equal(ms, 5000); timer = callback; return 1 }, clearInterval() {},
    onMounted() {}, onActivated() {}, onDeactivated() {}, onBeforeUnmount() {}
  }
  vm.createContext(context)
  vm.runInContext(`${code}\nthis.api = { loadBatchList, startHandoverRefresh, stopHandoverRefresh }`, context)
  return { context, pending, api: context.api, tick: () => timer() }
}

test('quiet polling never overlaps a slow request and resumes after completion', async () => {
  const h = harness()
  h.api.startHandoverRefresh()
  const first = h.tick()
  assert.equal(h.context.loading.value, false)
  await h.tick()
  await h.tick()
  assert.equal(h.pending.length, 1)
  h.pending[0].resolve({ list: [{ id: 1 }], total: 1 })
  await first
  const next = h.tick()
  assert.equal(h.pending.length, 2)
  h.pending[1].resolve({ list: [], total: 0 })
  await next
})

test('hidden and deactivated pages do not poll; old activation cannot replace data', async () => {
  const h = harness()
  h.api.startHandoverRefresh()
  h.context.document.hidden = true
  await h.tick()
  assert.equal(h.pending.length, 0)
  h.context.document.hidden = false
  const pending = h.tick()
  h.api.stopHandoverRefresh()
  await h.tick()
  h.api.startHandoverRefresh()
  h.pending[0].resolve({ list: [{ id: 1 }], total: 1 })
  await pending
  assert.equal(h.context.list.value.length, 0)
})

test('revoked selection closes promptly after the next successful poll', async () => {
  const h = harness()
  h.context.selectedReleaseBatch.value = { id: 1 }
  h.context.releaseDialogVisible.value = true
  h.context.releaseForm.password = 'test-only'
  h.api.startHandoverRefresh()
  const pending = h.tick()
  h.pending[0].resolve({ list: [{ id: 1, marketReleaseAccess: { writable: false } }], total: 1 })
  await pending
  assert.equal(h.context.releaseDialogVisible.value, false)
  assert.equal(h.context.releaseForm.password, '')
})

test('failed quiet read clears stale rows and releases the in-flight guard', async () => {
  const h = harness()
  h.context.list.value = [{ id: 1 }]
  h.api.startHandoverRefresh()
  const pending = h.tick()
  h.pending[0].reject(new Error('permission read failed'))
  await pending
  assert.equal(h.context.loadError.value, 'permission read failed')
  assert.equal(h.context.list.value.length, 0)
  const next = h.tick()
  assert.equal(h.pending.length, 2)
  h.pending[1].resolve({ list: [], total: 0 })
  await next
})
