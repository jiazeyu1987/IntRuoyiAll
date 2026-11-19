const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const compiler = require('vue/compiler-sfc')
const vue = require('vue')
const { createMemoryHistory, createRouter } = require('vue-router')
const root = path.resolve(__dirname, '..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const transpile = source => ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const cache = new Map()
function load(file) {
  if (cache.has(file)) return cache.get(file)
  const exports = {}
  vm.runInNewContext(transpile(read(file)), {
    exports,
    require: name => name.startsWith('@/') ? load(`src/${name.slice(2)}.ts`) : require(name)
  })
  cache.set(file, exports)
  return exports
}

const renderer = vue.createRenderer({
  createElement: type => ({ type, children: [], props: {} }),
  createText: text => ({ text }), createComment: text => ({ text }),
  setText: (node, text) => { node.text = text },
  setElementText: (node, text) => { node.text = text },
  patchProp: (node, key, old, value) => { node.props[key] = value },
  parentNode: node => node.parent,
  nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1],
  insert(node, parent, anchor) {
    if (node.parent) node.parent.children.splice(node.parent.children.indexOf(node), 1)
    node.parent = parent
    const index = anchor ? parent.children.indexOf(anchor) : -1
    if (index < 0) parent.children.push(node)
    else parent.children.splice(index, 0, node)
  },
  remove(node) { node.parent?.children.splice(node.parent.children.indexOf(node), 1) }
})

async function testLink() {
  const { descriptor } = compiler.parse(read('src/components/ProductCodeLink.vue'))
  const script = compiler.compileScript(descriptor, { id: 'product-link', inlineTemplate: true })
  const exports = {}
  vm.runInNewContext(transpile(script.content), { exports, require })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/source', component: {} }, { path: '/mes/md/erp-product', component: {} }
  ] })
  await router.push('/source')
  for (const code of ['YXN.044.02.1020', ' A/B & 中文+编码 ', '', null, '-', '--', '—']) {
    const container = { children: [] }
    const app = renderer.createApp(exports.default, { code, emptyText: '未填写' })
    app.use(router).mount(container)
    const node = container.children[0]
    if (code && !['-', '--', '—'].includes(code)) {
      assert.equal(node.type, 'a')
      assert.equal(new URL(node.props.href, 'http://localhost').searchParams.get('barCode'), code.trim())
      let stopped = false
      const event = { button: 0, preventDefault() {}, stopPropagation() { stopped = true } }
      for (const handler of [node.props.onClick].flat()) await handler(event)
      await vue.nextTick()
      assert(stopped, 'Click must not select the surrounding table row or option')
      assert.equal(router.currentRoute.value.path, '/mes/md/erp-product')
      assert.equal(router.currentRoute.value.query.barCode, code.trim())
    } else {
      assert.equal(node.type, 'span', 'Empty values and display placeholders must not navigate')
      assert.equal(node.props.href, undefined)
    }
    app.unmount()
  }
}

async function testRouteFiltering() {
  const { useTableQuickFilter } = load('src/hooks/web/useTableQuickFilter.ts')
  const queryParams = vue.reactive({ pageNo: 8, pageSize: 20, name: '旧名称', categoryId: 99, syncCode: '旧编码', listFilters: '旧过滤' })
  const requests = []
  const getList = async () => { requests.push({ ...queryParams }) }
  const definitions = vue.computed(() => ['name', 'barCode', 'categoryId'].map(key => ({ key, label: key, type: 'text', queryParamKey: key, operators: ['contains'] })))
  const quickFilter = useTableQuickFilter('test', definitions, queryParams, getList)
  const route = { query: { barCode: 'YXN.044.02.1020' } }
  const context = { route, queryParams, quickFilter, getList, codeSyncListTemplate: { value: { resetColumnFilters() { queryParams.listFilters = undefined } } } }
  const body = read('src/views/erp/product/product/index.vue').match(/const loadListFromRoute = async[^]*?\n}/)[0]
  vm.runInNewContext(transpile(`${body}\nglobalThis.run = loadListFromRoute`), context)
  for (const code of ['YXN.044.02.1020', 'PK.01.002.02.04.016']) {
    route.query.barCode = code
    queryParams.pageNo = 4
    await context.run()
    assert.equal(requests.at(-1).barCode, code)
    assert.equal(requests.at(-1).pageNo, 1)
    for (const field of ['name', 'categoryId', 'syncCode', 'listFilters']) assert.equal(requests.at(-1)[field], undefined)
    assert.equal(quickFilter.state.appliedConditions[0].value, code)
  }
  await quickFilter.resetQuickFilter()
  assert.equal(requests.at(-1).barCode, undefined)
}

async function main() {
  await testLink()
  await testRouteFiltering()
  const { readBusinessEvidence } = load('src/views/mes/pro/processpool/components/businessEvidenceDisplay.ts')
  const evidence = readBusinessEvidence(JSON.stringify({ productCode: 'YXN.044.02.1020', workOrderCode: 'WO001', child: { productCode: null } }))
  assert.equal(evidence.rows[0].productCode, 'YXN.044.02.1020')
  assert.equal(evidence.rows[1].productCode, undefined)
  assert.equal(evidence.rows[2].productCode, undefined)
  console.log('PASS: product link navigation, URL encoding, click isolation, empty codes, repeated route filtering, stale-filter clearing, pagination reset, reset and evidence codes')
}
main().catch(error => { console.error(error); process.exitCode = 1 })
