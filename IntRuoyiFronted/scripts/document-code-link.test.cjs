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
 const compile=file=>{const {descriptor}=compiler.parse(read(file));const script=compiler.compileScript(descriptor,{id:'document',inlineTemplate:true});const exports={};vm.runInNewContext(transpile(script.content),{exports,require:name=>name==='@/utils/permission'?{checkPermi:()=>allowed}:require(name)});return exports.default}
 let allowed=true
 const Link=compile('src/components/BusinessCodeLink.vue')
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/source',component:{}},{path:'/erp/purchase/order',component:{}}]})
 await router.push('/source')
 for(const permitted of [true,false]){
  allowed=permitted
  const container={children:[]};const app=renderer.createApp(Link,{code:'单号/A',permission:'erp:purchase-order:query',to:{path:'/erp/purchase/order',query:{code:'单号/A',openId:'90071992547409931'}}});app.use(router).mount(container)
  const node=container.children[0];assert.equal(node.type,permitted?'a':'span')
  if(permitted){let prevented=false;const event={button:0,stopPropagation(){},preventDefault(){prevented=true}};for(const handler of [node.props.onClick].flat())await handler({...event,ctrlKey:true});assert.equal(prevented,false);for(const handler of [node.props.onClick].flat())await handler(event);assert.equal(router.currentRoute.value.query.openId,'90071992547409931');const visit=router.currentRoute.value.query.codeLinkVisit;for(const handler of [node.props.onClick].flat())await handler(event);assert.notEqual(router.currentRoute.value.query.codeLinkVisit,visit)}app.unmount()
 }
}
async function testRoute() {
 for (const [group,kind] of [['purchase','order'],['purchase','in'],['purchase','return'],['sale','order'],['sale','out'],['sale','return']]) {
  const source=read(`src/views/erp/${group}/${kind}/index.vue`).replace(/\r/g,'')
  const body=source.match(/let documentGeneration = 0[^]*?\n}\n/)[0]
  const requests=[],opened=[],pending=[]
  const context={structuredClone,documentRequestSequence:0,codeLinkRoute:{path:`/erp/${group}/${kind}`,query:{code:' A/B 中文 ',openId:'90071992547409931'}},queryParams:{pageNo:9,status:3,no:'old',orderTime:['old']},codeLinkDefaults:{pageNo:1,status:undefined,no:undefined,orderTime:[]},queryFormRef:{value:{resetFields(){}}},parsePositiveRouteQueryId:load('src/utils/routeQueryId.ts').parsePositiveRouteQueryId,getList:async()=>{requests.push({...context.queryParams});await new Promise(resolve=>pending.push(resolve))},openForm:(type,id)=>opened.push(id)}
  vm.runInNewContext(transpile(`${body}\nglobalThis.run=receiveDocumentCodeLink`),context)
  const first=context.run();assert.equal(requests[0].no,'A/B 中文');assert.equal(requests[0].status,undefined);assert.equal(requests[0].orderTime.length,0)
  context.codeLinkRoute.query.code='B';const second=context.run();pending[1]();await second;pending[0]();await first
  assert.deepEqual(opened,['90071992547409931'])
  const third=context.run();context.codeLinkRoute.path='/other';await context.run();pending[2]();await third;assert.equal(opened.length,1)
  context.codeLinkRoute.path=`/erp/${group}/${kind}`;context.codeLinkRoute.query.openId='-1';const invalid=context.run();pending[3]();await invalid;assert.equal(opened.length,1)
 }
}
Promise.resolve().then(testLink).then(testRoute).then(()=>console.log('document target reset, supersession, departure and long ID behavior PASS')).catch(error=>{console.error(error);process.exitCode=1})
