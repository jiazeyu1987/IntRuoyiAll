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
 const { descriptor } = compiler.parse(read('src/components/ProductionBatchCodeLink.vue'))
 const script = compiler.compileScript(descriptor, { id: 'production-batch', inlineTemplate: true })
 const exports = {}
 vm.runInNewContext(transpile(script.content), { exports, require: n => n.includes('permission') ? { checkPermi: () => permitted } : require(n) })
 const router = createRouter({ history: createMemoryHistory(), routes: ['/', '/mes/pro/work-order', '/mes/pro/feedback/edhr-batch-execution/detail'].map(path => ({ path, component: {} })) })
 await router.push('/')
 for (const props of [{ code: '生产 / A' }, { code: 'B', batchExecutionId: 77 }, { code: '' }, { code: null }, { code: '-' }, { code: '--' }, { code: '—' }, { code: 'denied' }]) {
  permitted = props.code !== 'denied'
  const container = { children: [] }; const app = renderer.createApp(exports.default, props); app.use(router).mount(container)
  const node = container.children[0]
  if (!props.code || ['-', '--', '—', 'denied'].includes(props.code)) assert.equal(node.type, 'span')
  else {
   assert.equal(new URL(node.props.href, 'http://localhost').pathname, props.batchExecutionId ? '/mes/pro/feedback/edhr-batch-execution/detail' : '/mes/pro/work-order')
   let stopped = false
   for (const handler of [node.props.onClick].flat()) await handler({ button: 0, preventDefault() {}, stopPropagation() { stopped = true } })
   await vue.nextTick(); assert(stopped)
   const visitKey=props.batchExecutionId?'codeLinkVisit':'detailVisit'
   const previousVisit=router.currentRoute.value.query[visitKey]
   assert(previousVisit)
   await node.props.onClick({button:0,preventDefault(){},stopPropagation(){}})
   assert.notEqual(router.currentRoute.value.query[visitKey],previousVisit)
   assert.equal(router.currentRoute.value.query[props.batchExecutionId ? 'id' : 'batchCode'], props.batchExecutionId ? '77' : props.code)
  }
  app.unmount()
 }

 const source = read('src/views/mes/pro/workorder/index.vue').replace(/\r\n/g, '\n')
 const build = source.match(/const buildWorkOrderListQuery = \(\) => \{[^]*?\n}/)[0]
 const apply = source.match(/const loadListFromRoute = async[^]*?\n}/)[0]
 const watcher = source.match(/watch\(\n  \(\) => \[route.path, route.query.code[^]*?\n\)/)[0]
 const route = vue.reactive({ path: '/mes/pro/work-order', query: { batchCode: ' A ' } })
 const queryParams = vue.reactive({ pageNo: 7, pageSize: 10, code: 'old', batchCode: undefined, productCodeKeyword: 'old', listFilters: 'old', quickFilter: 'old', requestDate: ['old'] })
 const requests = []; let resets = 0
 const state = vue.reactive({ conditions: ['old'], appliedConditions: ['old'], value: 'old' })
 const scope = vue.effectScope()
 const context = {
  route, queryParams, watch: vue.watch,
  codeSyncListTemplate: { value: { resetColumnFilters() { resets++ } } },
  workOrderQuickFilter: { state, resetQuickFilter() { requests.push(context.build()) } },
  openedRouteDetailId: { value: '' }, openForm() {},
  getList: () => requests.push(context.build())
 }
 scope.run(() => vm.runInNewContext(transpile(build + '\n' + apply + '\n' + watcher + '\nglobalThis.run = loadListFromRoute; globalThis.build = buildWorkOrderListQuery'), context))
 await context.run()
 assert.equal(requests.length, 1)
 assert.deepEqual(JSON.parse(requests[0].listFilters), [{ key: 'batchCode', operator: 'eq', value: 'A' }])
 assert.equal(requests[0].batchCode, undefined, 'Only the supported listFilters contract reaches the API')
 route.path = '/other'; await vue.nextTick(); route.query.batchCode = 'B'; await vue.nextTick(); assert.equal(requests.length, 1)
 route.path = '/mes/pro/work-order'; await vue.nextTick(); assert.equal(requests.length, 2)
 assert.equal(JSON.parse(requests[1].listFilters)[0].value, 'B')
 queryParams.productCodeKeyword = 'stale'; queryParams.pageNo = 9; state.conditions = ['stale']; state.appliedConditions = ['stale']
 route.query.batchCode = ' C '; await vue.nextTick(); assert.equal(requests.length, 3); assert.equal(requests[2].pageNo, 1)
 assert.equal(requests[2].productCodeKeyword, undefined)
 assert.equal(JSON.parse(requests[2].listFilters)[0].value, 'C')
 assert.equal(state.conditions.length, 0); assert.equal(state.appliedConditions.length, 0); assert.equal(resets, 3)
 route.query.detailVisit = 'repeat'; await vue.nextTick(); assert.equal(requests.length, 4)
 queryParams.listFilters = JSON.stringify([{ key: 'status', operator: 'eq', value: 1 }])
 const combined = JSON.parse(context.build().listFilters)
 assert.deepEqual(combined, [{ key: 'status', operator: 'eq', value: 1 }, { key: 'batchCode', operator: 'eq', value: 'C' }])
 assert.equal(JSON.parse(queryParams.listFilters).length, 1, 'Building a query must not append duplicate conditions')
 queryParams.listFilters = 'invalid'
 assert.throws(() => context.build(), /JSON|Unexpected/)
 queryParams.listFilters = undefined
 const reset = source.match(/const resetQuery = \(\) => \{[^]*?\n}/)[0]
 vm.runInNewContext(transpile(reset + '\nglobalThis.reset = resetQuery'), context)
 context.reset()
 assert.equal(queryParams.batchCode, undefined)
 assert.equal(context.build().listFilters, undefined)
 scope.stop()
 console.log('PASS production batch: execution ID detail, production-order batch filtering, route exit/reentry, repeated navigation, reset, URL encoding, empty values, permission, click isolation')
}
main().catch(e => { console.error(e); process.exitCode = 1 })
