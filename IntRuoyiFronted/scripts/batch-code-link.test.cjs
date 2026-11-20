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


async function main() {
 let permitted = true
 const calls = []
 const Detail = { setup(_, { expose }) { expose({ open: id => calls.push(id) }); return () => vue.h('detail') } }
 const { descriptor } = compiler.parse(read('src/components/BatchCodeLink.vue'))
 const script = compiler.compileScript(descriptor, { id: 'batch', inlineTemplate: true })
 const exports = {}
 vm.runInNewContext(transpile(script.content), { exports, require: n => n.includes('permission') ? { checkPermi: () => permitted } : n.includes('BatchForm') ? { default: Detail } : require(n) })
 const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: {} }, { path: '/mes/qc/batch-trace', component: {} }] })
 await router.push('/')
 const walk = n => [n, ...(n.children || []).flatMap(walk)]
 for (const props of [{ code: 'B', batchId: 7 }, { code: 'A / 中文' }, { code: '' }, { code: null }, { code: '-' }, { code: '--' }, { code: '—' }, { code: 'denied' }]) {
  permitted = props.code !== 'denied'
  const container = { children: [] }; const app = renderer.createApp(exports.default, props)
  app.use(router).component('el-link', { setup(_, { attrs, slots }) { return () => vue.h('button', attrs, slots.default()) } }).mount(container)
  assert(!walk(container).some(n => n.type === 'detail'), 'Detail must mount only on click')
  const link = walk(container).find(n => n.type === 'button' || n.type === 'a')
  if (!props.code || ['-', '--', '—', 'denied'].includes(props.code)) assert(!link)
  else if (props.batchId) {
   for (let i = 0; i < 2; i++) { await link.props.onClick({ stopPropagation() {} }); await vue.nextTick() }
   assert.deepEqual(calls, [7, 7])
  } else {
   let stopped = false
   for (const handler of [link.props.onClick].flat()) await handler({ button: 0, preventDefault() {}, stopPropagation() { stopped = true } })
   await vue.nextTick(); assert(stopped); assert.equal(router.currentRoute.value.query.code, props.code)
   const firstVisit=router.currentRoute.value.query.codeLinkVisit
   await link.props.onClick({button:0,preventDefault(){},stopPropagation(){}})
   assert.notEqual(router.currentRoute.value.query.codeLinkVisit,firstVisit)
  }
  app.unmount()
 }
 const source = read('src/views/mes/qc/batchtrace/index.vue').replace(/\r\n/g, '\n')
 const body = source.slice(source.indexOf('const route = useRoute()'), source.indexOf('</script>'))
 const route = vue.reactive({ path: '/mes/qc/batch-trace', query: { code: 'A' } })
 const queryParams = vue.reactive({ pageNo: 8, pageSize: 10, code: undefined, itemId: 8, vendorId: 3, clientId: 4, salesOrderCode: 'old', purchaseOrderCode: 'old' })
 const requests = []
 const scope = vue.effectScope()
 scope.run(() => vm.runInNewContext(transpile(body), {
  useRoute: () => route, watch: vue.watch, queryParams, listRequestSerial: 0,
  queryFormRef: { value: { resetFields() {} } }, getList: () => requests.push({ ...queryParams })
 }))
 assert.equal(requests.length, 1)
 route.path = '/elsewhere'; await vue.nextTick(); assert.equal(requests.length, 1)
 route.query.code = 'B'; await vue.nextTick(); assert.equal(requests.length, 1)
 route.path = '/mes/qc/batch-trace'; await vue.nextTick(); assert.equal(requests.at(-1).code, 'B')
 queryParams.pageNo = 9; queryParams.itemId = 99
 route.query.code = ' C '; await vue.nextTick()
 assert.equal(requests.at(-1).code, 'C'); assert.equal(requests.at(-1).pageNo, 1)
 for (const key of ['itemId', 'vendorId', 'clientId', 'salesOrderCode', 'purchaseOrderCode']) assert.equal(requests.at(-1)[key], undefined)
 const beforeRepeat=requests.length;route.query.codeLinkVisit='repeat';await vue.nextTick();assert.equal(requests.length,beforeRepeat+1)
 scope.stop()
 const getListBody=source.match(/let listRequestSerial = 0[^]*?\n}\n/)[0]
 const pending=[]
 const ctx={loading:{value:false},list:{value:[]},total:{value:0},queryParams:{},BatchApi:{getBatchPage:()=>new Promise(resolve=>pending.push(resolve))}}
 vm.runInNewContext(transpile(getListBody+'\nglobalThis.run=getList;globalThis.leave=()=>listRequestSerial++'),ctx)
 const a=ctx.run(),b=ctx.run();pending[1]({list:['B'],total:1});await b;pending[0]({list:['A'],total:1});await a;assert.equal(ctx.list.value[0],'B')
 const c=ctx.run();ctx.leave();pending[2]({list:['C'],total:1});await c;assert.equal(ctx.list.value[0],'B')

 console.log('PASS batch links: lazy detail, repeated opening, code routing, empty values, permissions, route exit/reentry, stale filters and pagination')
}
main().catch(e => { console.error(e); process.exitCode = 1 })
