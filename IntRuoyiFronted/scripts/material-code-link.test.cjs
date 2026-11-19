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
  const { descriptor } = compiler.parse(read('src/components/MaterialCodeLink.vue'))
  const script = compiler.compileScript(descriptor, { id: 'product-link', inlineTemplate: true })
  const exports = {}
  const componentRequire = name => {
    if (name === '@/utils/permission') return { checkPermi: () => true }
    if (name === './BusinessCodeLink.vue') {
      const child = compiler.parse(read('src/components/BusinessCodeLink.vue')).descriptor
      const compiled = compiler.compileScript(child, { id: 'business-link', inlineTemplate: true })
      const childExports = {}
      vm.runInNewContext(transpile(compiled.content), { exports: childExports, require: componentRequire })
      return childExports
    }
    return require(name)
  }
  vm.runInNewContext(transpile(script.content), { exports, require: componentRequire })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/source', component: {} }, { path: '/mes/md/item', component: {} }
  ] })
  await router.push('/source')
  for (const code of ['YXN.044.02.1020', ' A/B & 中文+编码 ', '', null, '-', '--', '—']) {
    const container = { children: [] }
    const app = renderer.createApp(exports.default, { code, emptyText: '未填写' })
    app.use(router).mount(container)
    const node = container.children[0]
    if (code && !['-', '--', '—'].includes(code)) {
      assert.equal(node.type, 'a')
      assert.equal(new URL(node.props.href, 'http://localhost').searchParams.get('code'), code.trim())
      let stopped = false
      const event = { button: 0, preventDefault() {}, stopPropagation() { stopped = true } }
      for (const handler of [node.props.onClick].flat()) await handler(event)
      await vue.nextTick()
      assert(stopped, 'Click must not select the surrounding table row or option')
      assert.equal(router.currentRoute.value.path, '/mes/md/item')
      assert.equal(router.currentRoute.value.query.code, code.trim())
    } else {
      assert.equal(node.type, 'span', 'Empty values and display placeholders must not navigate')
      assert.equal(node.props.href, undefined)
    }
    app.unmount()
  }
}

async function testRouteState() {
  const source = read('src/views/mes/md/item/index.vue')
  const body = source.slice(source.indexOf('const loadListFromRoute = async () => {'), source.indexOf('/** 切换物料状态 */'))
  const route = vue.reactive({ path: '/mes/md/item', query: { code: 'A' } })
  const queryParams = { pageNo: 8, name: 'old', itemTypeId: 7, status: 0 }
  const requests = []
  const context = { route, queryParams, CommonStatusEnum: { ENABLE: 0 }, getList: async () => requests.push({ ...queryParams }), openedDetailId: { value: '' }, listRequest: { invalidate() {} }, openForm() {} }
  vm.runInNewContext(body + ';this.run = loadListFromRoute', context)
  await context.run()
  assert.equal(requests[0].code, 'A')
  assert.equal(requests[0].pageNo, 1)
  for (const key of ['name', 'itemTypeId', 'status']) assert.equal(requests[0][key], undefined)
  route.query.code = 'B'
  await context.run()
  assert.equal(requests[1].code, 'B')
  route.path = '/other'
  await context.run()
  assert.equal(requests.length, 2)
  route.path = '/mes/md/item'
  queryParams.pageNo = 8
  await context.run()
  assert.equal(requests.length, 3)
  assert.equal(requests[2].pageNo, 1)
}
Promise.resolve().then(testLink).then(testRouteState).then(() => console.log('PASS material navigation and route state')).catch(error => { console.error(error); process.exitCode = 1 })
