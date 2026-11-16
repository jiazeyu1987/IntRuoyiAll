const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const { ref, reactive, computed, watch, nextTick } = require('vue')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
const start = source.indexOf('const productionSopFileUrl =')
const end = source.indexOf('const selectedEmployeeLabel =', start)
assert.ok(start >= 0 && end > start, 'SOP component state must exist')
const compiled = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText

const tick = async () => { await nextTick(); await new Promise(setImmediate) }
const deferred = () => {
  let resolve
  let reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}

function setup({ metadata, binary } = {}) {
  const bindings = new Map([[1, null], [2, 'https://files.example.test/cleaning.pdf']])
  const deviceState = reactive({ selectedProcess: { processId: 1, routeId: 10, routeProcessId: 101 } })
  const created = []
  const revoked = []
  const downloads = []
  class PreviewURL extends URL {
    static createObjectURL(blob) {
      const url = super.createObjectURL(blob)
      created.push({ url, type: blob.type })
      return url
    }
    static revokeObjectURL(url) { revoked.push(url); super.revokeObjectURL(url) }
  }
  const context = vm.createContext({
    ref, reactive, computed, watch, nextTick, Blob, URL: PreviewURL,
    window: { location: { origin: 'http://localhost:8081' } },
    isPqcMode: ref(false), deviceState,
    selectedActiveOrder: ref({ activeOrderId: 50 }),
    resolveErrorMessage: (error) => error.message,
    ProFeedbackApi: {
      getFrontlineProductionProcessSop: metadata || (async ({ processId }) => ({ processId, fileUrl: bindings.get(processId) }))
    },
    axios: { get: async (url) => {
      downloads.push(url)
      return binary ? binary(url) : { data: new Blob(['%PDF-1.7\nsynthetic unit fixture'], { type: 'application/octet-stream' }) }
    } }
  })
  vm.runInContext(compiled + '\n globalThis.sop = { productionSopFileUrl, productionSopLoading, productionSopLoadError, productionSopPreviewUrl, productionSopPreviewError, productionSopPreviewLoading, productionSopDialogOpen, productionSopRefreshKey, openProductionSop, closeProductionSop };', context)
  return { ...context.sop, deviceState, created, revoked, downloads, bindings }
}

test('unbound process opens an empty SOP dialog without downloading a file', async () => {
  const state = setup()
  await tick()
  await state.openProductionSop()
  assert.equal(state.productionSopFileUrl.value, '')
  assert.equal(state.productionSopLoadError.value, '')
  assert.equal(state.productionSopDialogOpen.value, true)
  assert.equal(state.downloads.length, 0)
  state.closeProductionSop()
})

test('bound PDF previews a PDF blob and releases it when closed', async () => {
  const state = setup()
  state.deviceState.selectedProcess = { processId: 2, routeId: 10, routeProcessId: 102 }
  await tick()
  await state.openProductionSop()
  assert.equal(state.downloads[0], 'https://files.example.test/cleaning.pdf')
  assert.equal(state.created[0].type, 'application/pdf')
  const url = state.productionSopPreviewUrl.value
  assert.match(url, /^blob:/)
  state.closeProductionSop()
  assert.ok(state.revoked.includes(url))
  assert.equal(state.productionSopPreviewUrl.value, '')
})

test('PDF uploaded as a system-relative file address previews without a URL parsing error', async () => {
  const fileUrl = '/admin-api/infra/file/28/get/20261028/179318186243881869/Invoice-A14LN2OC-0003.pdf'
  const state = setup({ metadata: async ({ processId }) => ({ processId, fileUrl }) })
  await tick()
  await state.openProductionSop()
  assert.equal(state.downloads[0], fileUrl)
  assert.equal(state.productionSopPreviewError.value, '')
  assert.equal(state.created[0].type, 'application/pdf')
  assert.match(state.productionSopPreviewUrl.value, /^blob:/)
  state.closeProductionSop()
})

test('switching processes discards a late SOP binding response', async () => {
  const oldRequest = deferred()
  const state = setup({ metadata: ({ processId }) => processId === 1
    ? oldRequest.promise : Promise.resolve({ processId, fileUrl: null }) })
  state.deviceState.selectedProcess = { processId: 2, routeId: 10, routeProcessId: 102 }
  await tick()
  oldRequest.resolve({ processId: 1, fileUrl: 'https://files.example.test/old.pdf' })
  await tick()
  assert.equal(state.productionSopFileUrl.value, '')
  assert.equal(state.productionSopLoading.value, false)
  assert.equal(state.productionSopDialogOpen.value, false)
})

test('closing a pending preview prevents a late download from reopening or leaking a blob', async () => {
  const download = deferred()
  const state = setup({ binary: () => download.promise })
  state.deviceState.selectedProcess = { processId: 2, routeId: 10, routeProcessId: 102 }
  await tick()
  const opening = state.openProductionSop()
  await tick()
  state.closeProductionSop()
  download.resolve({ data: new Blob(['%PDF synthetic unit fixture']) })
  await opening
  assert.equal(state.created.length, 0)
  assert.equal(state.productionSopDialogOpen.value, false)
  assert.equal(state.productionSopPreviewLoading.value, false)
})

test('file service errors stay visible instead of becoming an unbound state', async () => {
  const state = setup({ binary: async () => ({ data: new Blob([
    JSON.stringify({ msg: '文件已删除' })
  ], { type: 'application/json' }) }) })
  state.deviceState.selectedProcess = { processId: 2, routeId: 10, routeProcessId: 102 }
  await tick()
  await state.openProductionSop()
  assert.equal(state.productionSopPreviewError.value, '文件已删除')
  assert.equal(state.created.length, 0)
  assert.equal(state.productionSopFileUrl.value, 'https://files.example.test/cleaning.pdf')
  state.closeProductionSop()
})

test('retrying failed metadata loads and previews the current SOP', async () => {
  let attempts = 0
  const state = setup({ metadata: async ({ processId }) => {
    if (++attempts === 1) throw new Error('读取失败')
    return { processId, fileUrl: 'https://files.example.test/current.pdf' }
  } })
  await tick()
  assert.equal(state.productionSopLoadError.value, '读取失败')
  assert.equal(state.productionSopDialogOpen.value, true)
  state.productionSopRefreshKey.value += 1
  await tick()
  assert.equal(state.productionSopLoadError.value, '')
  assert.equal(state.productionSopFileUrl.value, 'https://files.example.test/current.pdf')
  assert.match(state.productionSopPreviewUrl.value, /^blob:/)
  state.closeProductionSop()
})
