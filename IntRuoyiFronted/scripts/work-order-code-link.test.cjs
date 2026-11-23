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
  const { descriptor } = compiler.parse(read('src/components/WorkOrderCodeLink.vue'))
  const script = compiler.compileScript(descriptor, { id: 'product-link', inlineTemplate: true })
  const exports = {}
  const permissions = vue.reactive(new Set(['mes:pro-work-order:query']))
  vm.runInNewContext(transpile(script.content), {
    exports,
    require: name => name === '@/utils/permission' ? {
      checkPermi: required => {
        assert.deepEqual(Array.from(required), ['mes:pro-work-order:query'])
        return required.some(permission => permissions.has(permission))
      }
    } : require(name)
  })
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/source', component: {} }, { path: '/mes/pro/work-order', component: {} }
  ] })
  await router.push('/source')
  for (const code of ['YXN.044.02.1020', ' A/B & 中文+编码 ', '', null, '-', '--', '—']) {
    const container = { children: [] }
    const app = renderer.createApp(exports.default, { code, workOrderId: 42, emptyText: '未填写' })
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
      assert.equal(router.currentRoute.value.path, '/mes/pro/work-order')
      assert.equal(router.currentRoute.value.query.code, code.trim())
      assert.equal(router.currentRoute.value.query.openId, '42')
    } else {
      assert.equal(node.type, 'span', 'Empty values and display placeholders must not navigate')
      assert.equal(node.props.href, undefined)
    }
    app.unmount()
  }

  await router.push('/source')
  permissions.clear()
  permissions.add('mes:pro-work-order:update')
  const container = { children: [] }
  const app = renderer.createApp(exports.default, { code: '881MO085822', workOrderId: 42 })
  app.use(router).mount(container)
  const assertPlainText = () => {
    const node = container.children[0]
    assert.equal(node.type, 'span', 'Without query permission the code must be plain text')
    assert.equal(node.text, '881MO085822', 'The order code must remain visible')
    assert.equal(node.props.href, undefined, 'No address may be offered for opening in another tab')
    assert.equal(node.props.onClick, undefined, 'Plain text must not have a navigation handler')
    assert.equal(router.currentRoute.value.path, '/source')
  }
  assertPlainText()
  permissions.add('mes:pro-work-order:query')
  await vue.nextTick()
  assert.equal(container.children[0].type, 'a', 'Granting query permission must restore the link')
  assert.equal(new URL(container.children[0].props.href, 'http://localhost').searchParams.get('openId'), '42')
  permissions.delete('mes:pro-work-order:query')
  await vue.nextTick()
  assertPlainText()
  app.unmount()
}

async function testRoute() {
 const source=read('src/views/mes/pro/workorder/index.vue')
 const body=source.match(/const loadListFromRoute = async[^]*?\n}/)[0]
 const requests=[],opened=[]
 const context={route:{path:'/mes/pro/work-order',query:{code:'WO-1',openId:'42'}},queryParams:{pageNo:9,listFilters:'old',syncCode:'old',productNameKeyword:'old',quickFilter:'old'},workOrderQuickFilter:{state:{conditions:[{value:'old'}],appliedConditions:[{value:'old'}],value:'old'}},codeSyncListTemplate:{value:{resetColumnFilters(){context.columnReset=true}}},getList:async()=>requests.push({...context.queryParams}),openedRouteDetailId:{value:''},openForm:(type,id)=>opened.push(id)}
 vm.runInNewContext(transpile(`${body}\nglobalThis.run=loadListFromRoute`),context)
 await context.run()
 assert.equal(requests.length,1);assert.equal(context.columnReset,true);assert.equal(context.workOrderQuickFilter.state.conditions.length,0);assert.equal(context.workOrderQuickFilter.state.value,undefined);assert.equal(requests[0].code,'WO-1');assert.equal(requests[0].pageNo,1);assert.equal(requests[0].listFilters,undefined);assert.equal(requests[0].syncCode,undefined);assert.equal(requests[0].productNameKeyword,undefined);assert.equal(opened[0],42)
 await context.run();assert.equal(opened.length,2)
 context.route.path='/other';context.route.query={};await context.run();assert.equal(requests.length,2)
 context.route.path='/mes/pro/work-order';context.route.query={code:'WO-2',openId:'42'};await context.run();assert.equal(requests[2].code,'WO-2');assert.equal(opened.length,3)
}
Promise.resolve().then(testLink).then(testRoute).then(()=>console.log('work order link and route behavior PASS')).catch(error=>{console.error(error);process.exitCode=1})
