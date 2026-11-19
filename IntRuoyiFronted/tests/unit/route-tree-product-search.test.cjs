const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteTreePanel.vue')
const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
function setup(api = {}) {
  const exports = {}
  const timers = new Set()
  const helper = {}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(path.dirname(filename), 'routeTreeOrderSearch.ts'), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, { exports: helper, require: () => ({ ProWorkOrderApi: api }), Set, Error })
  const context = { ...vue, exports, onMounted() {}, onActivated() {}, defineExpose() {},
    defineEmits: () => () => {}, useMessage: () => ({}),
    setTimeout: callback => { timers.add(callback); return callback }, clearTimeout: callback => timers.delete(callback),
    require: name => name === './routeTreeOrderSearch' ? helper : name === './routeError'
      ? { resolveRouteOperationErrorMessage: failure => failure.message } : {} }
  const code = descriptor.scriptSetup.content + '\nObject.assign(exports, { filterNode, filter, treeRef, rows, tree, searchType, orderSearchResult, orderSearchLoading, orderSearchError, searchEmptyText })'
  vm.runInNewContext(ts.transpileModule(code, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, context)
  return { ...exports, findOrderProductCodes: helper.findOrderProductCodes,
    runSearch: async () => { const callbacks = [...timers]; timers.clear(); await Promise.all(callbacks.map(callback => callback())) } }
}

test('product search matches any formally bound code, including partial and mixed case input', () => {
  const { filterNode } = setup()
  const flow = { name: '按压式压力泵', code: 'ROUTE-1', nodeType: 'FLOW', productCodes: 'IDI、YXN.002.021.1001、A006.015.20012' }
  for (const input of ['YXN.002.021.1001', ' yxn.002.021.1001 ', '021.1001', 'A006.015.20012']) {
    assert.equal(filterNode(input, flow), true, input)
  }
  assert.equal(filterNode('YXN.002.021.1002', flow), false)
  assert.equal(filterNode('YXN.002.021.1001', { ...flow, productCodes: undefined }), false)
  assert.equal(filterNode('YXN.002.021.1001', { ...flow, nodeType: 'GROUP' }), false)
})

test('name and route code search and clearing the search remain available', () => {
  const { filterNode } = setup()
  const group = { name: '二代压力泵', code: 'GROUP-1', nodeType: 'GROUP' }
  assert.equal(filterNode('二代', group), true)
  assert.equal(filterNode('group-1', group), true)
  assert.equal(filterNode('', group), true)
  assert.equal(filterNode('  ', group), true)
  assert.equal(filterNode('不存在', group), false)
})

test('refresh reapplies the current search after the tree data changes', async () => {
  const h = setup()
  const calls = []
  h.treeRef.value = { filter: value => calls.push(value) }
  h.filter.value = 'YXN.002.021.1001'
  await vue.nextTick()
  h.rows.value = [{ id: 1, nodeType: 'GROUP', name: '压力泵', code: 'G1' },
    { id: 2, parentId: 1, nodeType: 'FLOW', name: '流程', code: 'R2', productCodes: 'YXN.002.021.1001' }]
  await vue.nextTick()
  assert.deepEqual(calls, ['YXN.002.021.1001', 'YXN.002.021.1001'])
  assert.equal(h.tree.value[0].children[0].productCodes, 'YXN.002.021.1001')
  h.filter.value = ''
  await vue.nextTick()
  assert.equal(calls.at(-1), '')
})

test('the search template compiles', () => {
  const result = compileTemplate({ source: descriptor.template.content, filename, id: 'route-tree-search' })
  assert.deepEqual(result.errors, [])
})

test('Element Plus keeps matching flows and their ancestors and hides unrelated branches', async () => {
  const TreeStore = require('element-plus/lib/components/tree/src/model/tree-store.js').default
  const { filterNode } = setup()
  const store = new TreeStore({ key: 'id', props: { label: 'name', children: 'children' },
    filterNodeMethod: filterNode, data: [
      { id: 1, name: '压力泵', code: 'G1', nodeType: 'GROUP', children: [
        { id: 2, name: '二代压力泵', code: 'G2', nodeType: 'GROUP', children: [
          { id: 3, name: '流程甲', code: 'R3', nodeType: 'FLOW', productCodes: 'YXN.002.021.1001' },
          { id: 4, name: '流程乙', code: 'R4', nodeType: 'FLOW', productCodes: 'YXN.002.021.1002' }
        ] }
      ] },
      { id: 5, name: '其他', code: 'G5', nodeType: 'GROUP', children: [
        { id: 6, name: '流程丙', code: 'R6', nodeType: 'FLOW' }
      ] }
    ] })
  store.initialize()
  const settle = () => new Promise(resolve => setImmediate(resolve))
  store.filter('YXN.002.021.1001')
  await settle()
  assert.deepEqual([1, 2, 3, 4, 5, 6].map(id => store.getNode(id).visible), [true, true, true, false, false, false])
  store.filter('未绑定的产品编码')
  await settle()
  assert.equal(store.root.childNodes.some(node => node.visible), false)
  store.filter('')
  await settle()
  assert.equal([1, 2, 3, 4, 5, 6].every(id => store.getNode(id).visible), true)
})

test('order search resolves product codes across pages and matches bound codes exactly', async () => {
  const calls = []
  const h = setup({ getWorkOrderPage: async params => {
    calls.push(params)
    return params.pageNo === 1 ? { list: [{ code: 'MO-1', productCode: 'A006.1' }], total: 2 }
      : { list: [{ code: 'MO-2', productCode: 'YXN.002' }], total: 2 }
  } })
  h.searchType.value = 'order'
  h.filter.value = ' MO '
  assert.equal(h.orderSearchLoading.value, true)
  await h.runSearch()
  assert.equal(calls.length, 2)
  assert.equal(calls[0].code, 'MO')
  assert.equal(calls[1].pageNo, 2)
  assert.equal(h.orderSearchResult.value.orderCount, 2)
  const flow = { nodeType: 'FLOW', name: 'MO', code: 'MO', productCodes: 'OTHER、a006.1' }
  assert.equal(h.filterNode('MO', flow), true)
  assert.equal(h.filterNode('MO', { ...flow, productCodes: 'A006.12' }), false)
  assert.equal(h.filterNode('MO', { ...flow, productCodes: undefined }), false)
  assert.equal(h.filterNode('MO', { ...flow, nodeType: 'GROUP' }), false)
})

test('order misses and query failures are explicit and never show all flows', async () => {
  for (const response of ['missing', 'failed', 'no-product']) {
    const h = setup({ getWorkOrderPage: async () => {
      if (response === 'failed') throw new Error('无生产订单查询权限')
      return response === 'missing' ? { list: [], total: 0 } : { list: [{ code: 'MO-1' }], total: 1 }
    } })
    h.searchType.value = 'order'
    h.filter.value = 'MO-1'
    await h.runSearch()
    assert.equal(h.filterNode('MO-1', { nodeType: 'FLOW', code: 'MO-1', name: 'MO-1', productCodes: 'A' }), false)
    assert.equal(h.orderSearchLoading.value, false)
    if (response === 'missing') assert.equal(h.searchEmptyText.value, '未找到匹配的生产订单')
    else assert.match(h.orderSearchError.value, /权限|缺少产品编码/)
  }
})

test('stale order results cannot overwrite a newer search or clearing the input', async () => {
  const pending = []
  const h = setup({ getWorkOrderPage: () => new Promise(resolve => pending.push(resolve)) })
  h.searchType.value = 'order'
  h.filter.value = 'OLD'
  const oldSearch = h.runSearch()
  h.filter.value = 'NEW'
  const newSearch = h.runSearch()
  pending[1]({ list: [{ code: 'NEW', productCode: 'B' }], total: 1 })
  await newSearch
  pending[0]({ list: [{ code: 'OLD', productCode: 'A' }], total: 1 })
  await oldSearch
  assert.equal(h.orderSearchResult.value.productCodes[0], 'b')
  h.filter.value = ''
  assert.equal(h.orderSearchResult.value, undefined)
  assert.equal(h.filterNode('', { nodeType: 'FLOW' }), true)
})
