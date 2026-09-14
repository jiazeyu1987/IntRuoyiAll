import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url)), 'utf8')
const body = source.match(/const getList = async \(\) => \{([\s\S]*?)\n\}/)?.[1]
assert.ok(body)
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const setup = () => {
  const requests = []
  const context = {
    isCurrentDirectorySearch: { value: true }, selectedDirectoryId: { value: 1 },
    list: { value: [] }, total: { value: 0 }, loading: { value: false },
    browserListErrorMessage: { value: '' }, route: { path: '/dcc/browser' },
    page: 1, tenant: 1,
    buildBrowserRouteStateKey: () => String(context.page),
    markBrowserListLoadedForState: () => {}, clearBrowserLoadedListState: () => {},
    buildBrowserRequestParams: () => ({ directoryId: context.selectedDirectoryId.value, pageNo: context.page }),
    getBrowserCacheContext: () => ({ tenantId: context.tenant, userId: 1 }),
    getControlledFileBrowserPage: () => { const d = deferred(); requests.push(d); return d.promise },
    resolveInitialSelectedVersionId: item => item.id,
    resolveControlledFileReadErrorMessage: error => error.message,
    console: { warn: () => {} }
  }
  vm.createContext(context)
  vm.runInContext(`let listRequestSequence = 0; globalThis.getList = async () => {${body}}`, context)
  return { context, requests }
}
const result = id => ({ list: [{ id, canPreview: id === 'B' }], total: id === 'B' ? 2 : 1 })

for (const staleFails of [false, true]) {
  test(`old directory ${staleFails ? 'failure' : 'success'} cannot overwrite B`, async () => {
    const { context: c, requests } = setup()
    const a = c.getList()
    c.selectedDirectoryId.value = 2
    const b = c.getList()
    requests[1].resolve(result('B')); await b
    if (staleFails) requests[0].reject(new Error('A failed'))
    else requests[0].resolve(result('A'))
    await a
    assert.equal(c.list.value[0].id, 'B')
    assert.equal(c.list.value[0].canPreview, true)
    assert.equal(c.total.value, 2)
    assert.equal(c.browserListErrorMessage.value, '')
    assert.equal(c.loading.value, false)
  })
}
test('old response cannot clear newer loading state', async () => {
  const { context: c, requests } = setup()
  const a = c.getList(); c.page = 2; const b = c.getList()
  requests[0].resolve(result('A')); await a
  assert.equal(c.loading.value, true)
  requests[1].resolve(result('B')); await b
})
test('clearing directory invalidates an outstanding response', async () => {
  const { context: c, requests } = setup()
  const a = c.getList(); c.selectedDirectoryId.value = undefined; await c.getList()
  requests[0].resolve(result('A')); await a
  assert.equal(c.list.value.length, 0)
  assert.equal(c.total.value, 0)
  assert.equal(c.loading.value, false)
})
test('tenant switch invalidates outstanding permission projection', async () => {
  const { context: c, requests } = setup()
  const a = c.getList(); c.tenant = 2
  requests[0].resolve(result('A')); await a
  assert.equal(c.list.value.length, 0)
})
test('current failure remains visible and rejects, without stale rows', async () => {
  const { context: c, requests } = setup()
  c.list.value = result('old').list
  const a = c.getList(); requests[0].reject(new Error('current failed'))
  await assert.rejects(a, /current failed/)
  assert.equal(c.list.value.length, 0)
  assert.equal(c.browserListErrorMessage.value, 'current failed')
  assert.equal(c.loading.value, false)
})

test('late directory metadata cannot replace the newly selected directory or cache', async () => {
  const directoryBody = source.match(/const resolveSelectedDirectory = async \(\) => \{([\s\S]*?)\n\}/)?.[1]?.replace('directoryId!', 'directoryId')
  assert.ok(directoryBody)
  const d = deferred()
  const cached = []
  const c = {
    selectedDirectoryId: { value: 1 }, selectedDirectory: { value: undefined },
    directoryNodeById: { value: new Map() }, directories: { value: [] },
    findDirectoryNode: () => undefined, getDirectory: () => d.promise,
    getBrowserCacheContext: () => ({ tenantId: 1 }),
    toDirectoryNode: x => x, cacheDirectoryNodes: x => cached.push(...x)
  }
  const request = vm.runInNewContext(`(async () => {${directoryBody}})()`, c)
  c.selectedDirectoryId.value = 2; c.selectedDirectory.value = { id: 2 }
  d.resolve({ id: 1 }); await request
  assert.equal(c.selectedDirectory.value.id, 2)
  assert.equal(cached.length, 0)
})

test('old tenant directory tree cannot replace new tenant cache', async () => {
  const directoryBody = source.match(/const loadDirectories = async \(\) => \{([\s\S]*?)\n\}/)?.[1]
  assert.ok(directoryBody)
  const d = deferred()
  const c = {
    tenant: 1, directoryLoading: { value: false }, directories: { value: [] },
    restoreBrowserMetadataCache: () => {}, openRememberedDirectoryInTree: async () => false,
    getBrowserCacheContext: () => ({ tenantId: c.tenant }), route: { path: '/dcc/browser' },
    getDirectoryTree: () => d.promise, toDirectoryNode: x => x,
    applyDirectoryTree: x => { c.directories.value = x }, persistBrowserMetadataCache: () => {}
  }
  vm.createContext(c)
  vm.runInContext(`let directoryLoadSequence = 0; let browserDirectoriesLoaded = false; globalThis.load = async () => {${directoryBody}}`, c)
  const request = c.load(); c.tenant = 2; d.resolve([{ id: 1 }]); await request
  assert.equal(c.directories.value.length, 0)
})

test('overlapping route synchronization remains guarded until both navigations finish', async () => {
  const guardBody = source.match(/async function withBrowserRouteSyncGuard\(action: \(\) => Promise<unknown>\) \{([\s\S]*?)\n\}/)?.[1]
  assert.ok(guardBody)
  const c = vm.createContext({})
  vm.runInContext(`let browserRouteSyncing = false; let browserRouteSyncCount = 0; globalThis.guard = async action => {${guardBody}}; globalThis.syncing = () => browserRouteSyncing`, c)
  const a = deferred(), b = deferred()
  const first = c.guard(() => a.promise), second = c.guard(() => b.promise)
  a.resolve(); await first
  assert.equal(c.syncing(), true)
  b.resolve(); await second
  assert.equal(c.syncing(), false)
})
