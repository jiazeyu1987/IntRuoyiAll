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
  context.codeLinkRoute.path=`/erp/${group}/${kind}`;context.codeLinkRoute.query.openId='-1';await assert.rejects(context.run(),/单据详情编号无效/);assert.equal(requests.length,3);assert.equal(opened.length,1)
 }
}
async function testResponses() {
 for (const rel of ['erp/purchase/order','erp/purchase/in','erp/purchase/return','erp/sale/order','erp/sale/out','erp/sale/return','mes/wm/salesnotice']) {
  const source=read(`src/views/${rel}/index.vue`).replace(/\r/g,'')
  const body=source.match(/const getList = async[^]*?\n}/)[0]
  const api=body.match(/await (\w+)\.(\w+)\(/)
  const erp=rel.startsWith('erp/'),pending=[]
  const route={path:'/target'},list={value:[]},total={value:0},loading={value:false}
  const context={queryParams:{no:'A'},list,total,loading,codeLinkRoute:route,documentRoute:route,documentRequestSequence:0,documentSequence:0,message:{error(){}},resolveErrorMessage:()=>''}
  context[api[1]]={[api[2]]:()=>new Promise(resolve=>pending.push(resolve))}
  vm.runInNewContext(transpile(`${body}\nglobalThis.run=getList`),context)
  const old=context.run();context.queryParams.no='B';const current=context.run();pending[1]({list:['B'],total:1});await current;pending[0]({list:['A'],total:9});await old;assert.equal(list.value[0],'B');assert.equal(total.value,1);assert.equal(loading.value,false)
  const leave=context.run();route.path='/other';pending[2]({list:['stale'],total:5});await leave;assert.equal(list.value[0],'B')
 }
}
async function testAdditionalReceivers() {
 for(const [rel,path,field] of [['mes/wm/salesnotice','/mes/wm/sales-notice','code']]){
  const source=read(`src/views/${rel}/index.vue`).replace(/\r/g,'')
  const body=source.match(/const receiveDocumentCodeLink = \(\) => \{[^]*?\n}/)[0]
  const calls=[],context={structuredClone,documentSequence:0,documentRoute:{path,query:{code:' A 中文 '}},documentDefaults:{pageNo:1,status:undefined,dates:[]},queryParams:{pageNo:9,status:'old',dates:['old']},queryFormRef:{value:{resetFields(){}}},getList:()=>calls.push({...context.queryParams})}
  context.documentDefaults[field]=undefined
  vm.runInNewContext(transpile(`${body}\nglobalThis.run=receiveDocumentCodeLink`),context)
  context.run();assert.equal(calls[0][field],'A 中文');assert.equal(calls[0].status,undefined);assert.equal(calls[0].dates.length,0);context.run();assert.equal(calls.length,2)
  context.documentRoute.path='/other';context.run();assert.equal(calls.length,2)
  context.documentRoute.path=path;context.documentRoute.query={};context.run();assert.equal(calls.length,3);assert.equal(calls[2][field],undefined)
 }
}
async function testWrapper() {
 const targets=load('src/utils/documentCodeLink.ts').documentTargets
 const {descriptor}=compiler.parse(read('src/components/BusinessDocumentCodeLink.vue'))
 const script=compiler.compileScript(descriptor,{id:'wrapper',inlineTemplate:true}),exports={}
 vm.runInNewContext(transpile(script.content),{exports,require:name=>name==='./BusinessCodeLink.vue'?{default:vue.defineComponent({props:['code','permission','to'],setup:props=>()=>vue.h('probe',{...props})})}:name==='@/utils/documentCodeLink'?{documentTargets:targets}:require(name)})
 assert.equal(Object.keys(targets).length,7)
 for(const [documentType,target] of Object.entries(targets)){
  const container={children:[]},app=renderer.createApp(exports.default,{documentType,code:' CODE 中文 ',documentId:'90071992547409931'});app.mount(container);const props=container.children[0].props;assert.equal(props.permission,target.permission);assert.equal(props.to.path,target.path);assert.equal(props.to.query.code,'CODE 中文');assert.equal(props.to.query.openId,'90071992547409931');app.unmount()
 }
}
Promise.resolve().then(testLink).then(testRoute).then(testResponses).then(testAdditionalReceivers).then(testWrapper).then(()=>console.log('document target reset, supersession, departure and long ID behavior PASS')).catch(error=>{console.error(error);process.exitCode=1})
