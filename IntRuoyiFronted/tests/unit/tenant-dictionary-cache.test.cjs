const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { createPinia, setActivePinia, defineStore } = require('pinia')
const { ref } = require('vue')

function setup() {
  setActivePinia(createPinia())
  let tenant = 1, visit, session = 'login-1'
  const revision = ref(0)
  const requests = [], cache = new Map([['dictCache', { mode: [{label: 'unowned'}] }]])
  const imports = {
    pinia: { defineStore }, '../index': { store: undefined },
    '@/hooks/web/useCache': { CACHE_KEY: { DICT_CACHE: 'dictCache' }, useCache: () => ({ wsCache: {
      get: key => cache.get(key), set: (key, value) => cache.set(key, value), delete: key => cache.delete(key)
    }}) },
    '@/api/system/dict/dict.data': { getSimpleDictDataList: () => new Promise((resolve, reject) => requests.push({resolve,reject})) },
    '@/utils/auth': { authContextRevision: revision, getAccessToken: () => session, getAuthSessionId: () => session,
      getTenantId: () => tenant, getVisitTenantId: () => visit }
  }
  const exports = {}
  const code = ts.transpileModule(fs.readFileSync(path.join(__dirname, '../../src/store/modules/dict.ts'),'utf8'), {
    compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022}
  }).outputText
  vm.runInNewContext(code, {exports, require: id => { assert.ok(id in imports, id); return imports[id] }})
  return {store: exports.useDictStore(), requests, cache,
    switchTo: id => { visit=id; revision.value++ }, logout: () => { session=undefined; revision.value++ }}
}
const rows = label => [{dictType:'mode', value:'1', label, colorType:'', cssClass:''}]

test('dictionary cache isolates tenants and invalidates the former unowned cache', async () => {
  const s=setup()
  assert.equal(s.cache.has('dictCache'),false)
  let pending=s.store.setDictMap(); s.requests[0].resolve(rows('Production')); await pending
  assert.equal(s.store.getDictMap.mode[0].label,'Production')
  s.switchTo(2)
  assert.equal(s.store.getDictMap.mode,undefined)
  pending=s.store.setDictMap(); s.requests[1].resolve(rows('Test')); await pending
  assert.equal(s.store.getDictMap.mode[0].label,'Test')
  s.switchTo(1); await s.store.setDictMap()
  assert.equal(s.store.getDictMap.mode[0].label,'Production')
  assert.equal(s.requests.length,2)
  s.logout(); assert.equal(s.store.getDictMap.mode,undefined)
})

test('late response from the former tenant cannot replace the active dictionary', async () => {
  const s=setup()
  const first=s.store.setDictMap()
  s.switchTo(2)
  const second=s.store.setDictMap()
  s.requests[1].resolve(rows('Test')); await second
  s.requests[0].resolve(rows('Production')); await first
  assert.equal(s.store.getDictMap.mode[0].label,'Test')
  assert.equal(s.cache.size,1)
})

test('empty reload clears old values and failed reload propagates the error', async () => {
  const s=setup()
  let pending=s.store.setDictMap(); s.requests[0].resolve(rows('Old')); await pending
  pending=s.store.resetDict(); s.requests[1].resolve([]); await pending
  assert.equal(s.store.getDictMap.mode,undefined)
  assert.equal(s.store.getIsSetDict,true)
  pending=s.store.resetDict(); s.requests[2].reject(new Error('Unavailable'))
  await assert.rejects(pending,/Unavailable/)
  assert.equal(s.store.getIsSetDict,false)
  assert.equal(s.store.getDictMap.mode,undefined)
})

test('overlapping reloads only accept the newest response', async () => {
  const s=setup()
  const first=s.store.setDictMap(), second=s.store.resetDict()
  s.requests[1].resolve(rows('Latest')); await second
  s.requests[0].resolve(rows('Stale')); await first
  assert.equal(s.store.getDictMap.mode[0].label,'Latest')
})

test('invalidation discards in-flight data and allows fresh values after a successful write', async () => {
  const s=setup()
  const stale=s.store.setDictMap()
  s.store.invalidateDict()
  s.requests[0].resolve(rows('Before save')); await stale
  assert.equal(s.store.getDictMap.mode,undefined)
  const fresh=s.store.setDictMap()
  s.requests[1].resolve(rows('After save')); await fresh
  assert.equal(s.store.getDictMap.mode[0].label,'After save')
})
