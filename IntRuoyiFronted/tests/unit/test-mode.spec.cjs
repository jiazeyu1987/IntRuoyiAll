const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const pinia = require('pinia')

const source = fs.readFileSync(path.join(__dirname, '../../src/store/modules/testMode.ts'), 'utf8')
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText

function loadModule(getConfigKey) {
  const exports = {}
  vm.runInNewContext(code, {
    exports,
    require: (name) => {
      if (name === 'vue') return vue
      if (name === 'pinia') return pinia
      if (name === '@/api/infra/config') return { getConfigKey }
      throw new Error(`Unexpected dependency: ${name}`)
    }
  })
  pinia.setActivePinia(pinia.createPinia())
  return exports
}

test('only explicit persisted true/false values are accepted', () => {
  const { parseTestMode } = loadModule()
  assert.equal(parseTestMode('true'), true)
  assert.equal(parseTestMode('false'), false)
  for (const value of [undefined, null, '', true, false, 1, 'TRUE', ' false ']) {
    assert.throws(() => parseTestMode(value), /system.ui.test-mode-enabled/)
  }
})

test('configuration reads update visibility reactively; failed reads are surfaced', async () => {
  let value = 'true'
  const apiError = new Error('configuration service unavailable')
  let failure = false
  const api = loadModule(async (key) => {
    assert.equal(key, 'system.ui.test-mode-enabled')
    if (failure) throw apiError
    return value
  })
  const store = api.useTestModeStore()
  const visible = api.useTestMode()
  assert.equal(store.enabled, null)
  assert.equal(visible.value, false)
  await store.load()
  assert.equal(visible.value, true)
  value = 'false'
  await store.load()
  assert.equal(visible.value, false)
  store.enabled = true
  failure = true
  await assert.rejects(store.load(), (error) => error === apiError)
  assert.equal(store.enabled, null)
  assert.equal(visible.value, false)
  failure = false
  value = null
  await assert.rejects(store.load(), /system.ui.test-mode-enabled/)
  store.enabled = true
  store.$reset()
  assert.equal(visible.value, false)
})

test('test menus are filtered recursively without removing normal business routes or mutating source', () => {
  const { filterTestModeMenus } = loadModule()
  const routes = [
    { path: '/system', children: [
      { path: 'user' }, { path: 'codex-test-management' }, { path: 'codex-test-record' }
    ] },
    { path: '/mes/pro/feedback/edhr-batch-test' },
    { path: '/mes/pro/feedback/edhr-batch-execution' },
    { path: '/mes/pro/feedback/edhr-batch-execution/template-simulate' },
    { path: '/srm/supplier-sample-test' }
  ]
  const original = JSON.stringify(routes)
  const filtered = filterTestModeMenus(routes)
  assert.equal(JSON.stringify(filtered), JSON.stringify([
    { path: '/system', children: [{ path: 'user' }] },
    { path: '/mes/pro/feedback/edhr-batch-execution' },
    { path: '/srm/supplier-sample-test' }
  ]))
  assert.equal(JSON.stringify(routes), original)
})

test('a slow initialization read cannot overwrite a configuration saved in the meantime', async () => {
  let completeRead
  const api = loadModule(() => new Promise((resolve) => { completeRead = resolve }))
  const store = api.useTestModeStore()
  const pending = store.load()
  store.apply(true)
  completeRead('false')
  await pending
  assert.equal(store.enabled, true)
})

test('basic configuration switch persists immediately and restores the previous value on save failure', async () => {
  const api = loadModule()
  const config = { id: 10, key: api.TEST_MODE_CONFIG_KEY, visible: true, value: 'false' }
  let saved = null
  let failSave = false
  const errors = []
  const { parse, compileScript } = require('vue/compiler-sfc')
  const filename = path.join(__dirname, '../../src/views/infra/config/basic/components/IdleLogoutSetting.vue')
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const compiled = compileScript(descriptor, { id: 'basic-config-unit' }).content
  const componentExports = {}
  vm.runInNewContext(ts.transpileModule(compiled, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText, {
    exports: componentExports,
    ref: vue.ref,
    reactive: vue.reactive,
    onMounted: () => {},
    useMessage: () => ({ success: () => {}, error: (text) => errors.push(text) }),
    require: (name) => {
      if (name === 'vue') return vue
      if (name === '@/store/modules/testMode') return api
      if (name === '@/api/infra/config') return {
        getConfigPage: async () => ({ list: [{ ...config }] }),
        updateConfig: async (row) => {
          if (failSave) throw new Error('save rejected')
          saved = row
        }
      }
      throw new Error(`Unexpected dependency: ${name}`)
    }
  })
  const form = componentExports.default.setup({ canUpdate: true }, { expose: () => {} })
  await form.loadTestMode()
  assert.equal(form.testModeValue.value, false)
  form.testModeValue.value = true
  await form.saveTestMode()
  assert.equal(saved.value, 'true')
  assert.equal(api.useTestModeStore().enabled, true)
  failSave = true
  form.testModeValue.value = false
  await form.saveTestMode()
  assert.equal(form.testModeValue.value, true)
  assert.equal(api.useTestModeStore().enabled, true)
  assert.equal(errors.length, 1)
})
