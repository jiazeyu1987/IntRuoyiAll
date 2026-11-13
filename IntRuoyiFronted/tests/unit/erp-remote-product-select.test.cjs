const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const vue = require('vue')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const source = fs.readFileSync(path.join(__dirname, '../../src/views/erp/components/RemoteProductSelect.vue'), 'utf8')
function component(api, modelValue) {
  const descriptor = parse(source).descriptor
  const code = ts.transpileModule(compileScript(descriptor, { id: 'remote-product' }).content,
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
  const exports = {}
  vm.runInNewContext(code, { ...vue, onBeforeUnmount: vue.onScopeDispose, exports, setTimeout, clearTimeout,
    require: name => name === 'vue' ? vue : { ProductApi: api } })
  const events = []
  const props = vue.reactive({ modelValue })
  const scope = vue.effectScope()
  const bindings = scope.run(() => exports.default.setup(props, { expose() {}, emit: (...args) => events.push(args) }))
  return { bindings, events, props, stop: () => scope.stop() }
}
const flush = () => new Promise(resolve => setImmediate(resolve))
test('selected ID hydrates independently of search pages and emits full product before change', async () => {
  const product = { id: 903, name: '历史物料', salePrice: 12, purchasePrice: 8, unitName: '盒' }
  const calls = []
  const instance = component({
    getProductSimpleByIds: async ids => { calls.push(ids); return [product] },
    getProductSimplePage: async query => { calls.push(query); return { list: [{ id: 1, name: '另一个' }], total: 201 } }
  }, 903)
  await flush()
  await instance.bindings.loadPage(5)
  assert.deepEqual(Array.from(calls[0]), [903])
  assert.equal(calls[1].pageNo, 5)
  assert.equal(calls[1].pageSize, 20)
  assert.equal(instance.bindings.options.value[0].name, '历史物料')
  instance.bindings.select(903)
  assert.equal(instance.events[0][0], 'selected')
  assert.equal(instance.events[0][1].unitName, '盒')
  assert.equal(instance.events[1][0], 'update:modelValue')
  assert.equal(instance.events[2][0], 'change')
  instance.stop()
})
test('older search response cannot replace newer page and API errors reject', async () => {
  const pending = []
  const instance = component({ getProductSimplePage: () => new Promise((resolve, reject) => pending.push({resolve, reject})) })
  const first = instance.bindings.loadPage(1)
  const second = instance.bindings.loadPage(2)
  pending[1].resolve({ list: [{ id: 22, name: '新页' }], total: 50 })
  await second
  pending[0].resolve({ list: [{ id: 11, name: '过期页' }], total: 50 })
  await first
  assert.equal(instance.bindings.rows.value[0].id, 22)
  const failed = instance.bindings.loadPage(3)
  pending[2].reject(new Error('物料服务不可用'))
  await assert.rejects(failed, /物料服务不可用/)
  assert.equal(instance.bindings.loading.value, false)
  instance.stop()
})
