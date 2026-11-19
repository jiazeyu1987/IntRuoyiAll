const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const vue = require('vue')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.join(__dirname, '../../src/components/MesProductSelector/ProductNumberSelector.vue')
const source = fs.readFileSync(filename, 'utf8')
const descriptor = parse(source).descriptor
const script = compileScript(descriptor, { id: 'product-number-selector' })
const code = ts.transpileModule(script.content, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function component(getItemPage) {
  const exports = {}
  vm.runInNewContext(code, {
    ...vue, exports, Error,
    require: name => {
      if (name === 'vue') return vue
      if (name === '@/components/ProductCodeLink.vue') return { default: vue.defineComponent({ name: 'ProductCodeLink', render: () => null }) }
      if (name === '@/api/mes/md/item') return { MdItemApi: { getItemPage } }
      if (name === '@/utils/constants') return { CommonStatusEnum: { ENABLE: 0 } }
      throw new Error(`Unexpected import: ${name}`)
    }
  })
  const props = vue.reactive({ modelValue: true, linkedItemIds: [2], submitting: false })
  const scope = vue.effectScope()
  const bindings = scope.run(() => exports.default.setup(props, { expose() {}, emit() {} }))
  return { bindings, props, stop: () => scope.stop() }
}

test('selector template compiles with checkbox bindings', () => {
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'product-number-selector',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})

test('default fuzzy search includes code matches and similar names without duplicates', async () => {
  const exact = { id: 1, code: 'A001', name: '球囊扩张压力泵' }
  const similar = { id: 2, code: 'A002', name: '数显球囊扩张压力泵' }
  const byCode = { id: 3, code: '球囊扩张压力泵', name: '其他产品' }
  const instance = component(async params => ({ list: params.code ? [byCode, exact] : [exact, similar], total: 2 }))
  const b = instance.bindings
  assert.equal(b.exactMatch.value, false)
  b.keyword.value = exact.name
  await b.search()
  assert.deepEqual(Array.from(b.results.value, item => item.id).sort(), [1, 2, 3])
  instance.stop()
})

test('exact name search filters all pages before pagination and select-all', async () => {
  const rows = Array.from({ length: 103 }, (_, index) => ({
    id: index + 1, code: `A${index + 1}`, name: index % 2 ? '数显球囊扩张压力泵' : '球囊扩张压力泵'
  }))
  const calls = []
  const instance = component(async params => {
    calls.push(params)
    return { list: rows.slice((params.pageNo - 1) * 100, params.pageNo * 100), total: rows.length }
  })
  const b = instance.bindings
  b.keyword.value = ' 球囊扩张压力泵 '
  b.exactMatch.value = true
  b.pageNo.value = 3
  await b.search()
  assert.equal(calls.length, 2)
  assert.ok(calls.every(params => params.name === '球囊扩张压力泵' && !params.code))
  assert.equal(b.results.value.length, 52)
  assert.ok(b.results.value.every(item => item.name === '球囊扩张压力泵'))
  assert.equal(b.pageNo.value, 1)
  assert.equal(b.pageItems.value.length, 20)
  b.selectAll()
  assert.equal(b.selectedItems.value.length, 52)
  assert.ok(b.selectedItems.value.some(item => item.id === 103))
  b.keyword.value = '球囊'
  await b.search()
  assert.equal(b.results.value.length, 0)
  instance.props.modelValue = false
  await vue.nextTick()
  instance.props.modelValue = true
  await vue.nextTick()
  assert.equal(b.exactMatch.value, false)
  assert.equal(b.keyword.value, '')
  assert.equal(b.selectedItems.value.length, 0)
  instance.stop()
})

test('changing match mode reruns query and failures remain visible', async () => {
  let fail = false
  const instance = component(async () => {
    if (fail) throw new Error('查询服务不可用')
    return { list: [{ id: 1, code: 'A1', name: '数显球囊扩张压力泵' }], total: 1 }
  })
  const b = instance.bindings
  b.handleMatchChange()
  assert.equal(b.error.value, '')
  b.keyword.value = '球囊扩张压力泵'
  await b.search()
  assert.equal(b.results.value.length, 1)
  b.exactMatch.value = true
  b.handleMatchChange()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(b.results.value.length, 0)
  b.exactMatch.value = false
  b.handleMatchChange()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(b.results.value.length, 1)
  fail = true
  await b.search()
  assert.equal(b.error.value, '查询服务不可用')
  assert.equal(b.results.value.length, 0)
  assert.equal(b.loading.value, false)
  instance.stop()
})
