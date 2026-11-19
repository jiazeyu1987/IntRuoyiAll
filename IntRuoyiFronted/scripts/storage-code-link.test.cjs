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


let permitted=true
function component(file) {
 const {descriptor}=compiler.parse(read(file));const script=compiler.compileScript(descriptor,{id:file,inlineTemplate:true});const exports={}
 vm.runInNewContext(transpile(script.content),{exports,require:name=>name==='@/utils/permission'?{checkPermi:()=>permitted}:name.endsWith('BusinessCodeLink.vue')?{default:component('src/components/BusinessCodeLink.vue')}:require(name)})
 return exports.default
}
async function testLink(){
 const componentUnderTest=component('src/components/StorageCodeLink.vue')
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/source',component:{}},...['','/location','/area'].map(s=>({path:'/mes/wm/warehouse'+s,component:{}}))]});await router.push('/source')
 for(const storageType of ['warehouse','location','area'])for(const code of [' A/B & 中文+编码 ','',null,undefined,'-','--','—'])for(const allowed of [true,false]){
 permitted=allowed;const container={children:[]};const app=renderer.createApp(componentUnderTest,{code,storageType});app.use(router).mount(container);const node=container.children[0]
 if(allowed&&code&& !['-','--','—'].includes(code)){
 assert.equal(node.type,'a');const url=new URL(node.props.href,'http://localhost');assert.equal(url.pathname,'/mes/wm/warehouse'+(storageType==='warehouse'?'':'/'+storageType));assert.equal(url.searchParams.get('code'),code.trim())
 let stop=false;for(const handler of [node.props.onClick].flat())await handler({button:0,preventDefault(){},stopPropagation(){stop=true}});assert(stop);assert.equal(router.currentRoute.value.query.code,code.trim())
 }else assert.equal(node.type,'span');app.unmount()
 }
 for(const storageType of ['warehouse','location','area']){permitted=true;const container={children:[]};const app=renderer.createApp(componentUnderTest,{code:'W',storageType,storageId:'9223372036854775806'});app.use(router).mount(container);assert.equal(new URL(container.children[0].props.href,'http://localhost').searchParams.get('id'),'9223372036854775806');app.unmount()}
}
async function testRoutes(){
 for(const kind of ['warehouse','location','area']){
 const file='src/views/mes/wm/warehouse/'+(kind==='warehouse'?'':kind+'/')+'index.vue';const source=read(file);const body=source.match(/const receiveCodeLink = async[^]*?\n}/)[0];const requests=[],opened=[]
 const context={routeGeneration:0,listGeneration:0,route:{path:'/mes/wm/warehouse'+(kind==='warehouse'?'':'/'+kind),query:{code:'  W-1  ',id:'9223372036854775806',warehouseId:'123',locationId:'456'}},queryFormRef:{value:{resetFields(){context.resets++}}},resets:0,queryParams:{pageNo:9,name:'old',code:'old',frozen:true,positionX:9,positionY:9,positionZ:9,warehouseId:'old',locationId:'old'},currentWarehouse:{value:{id:'old',name:'old'}},currentLocation:{value:{id:'old',name:'old',warehouseId:'old',warehouseName:'old'}},loadUserList:async()=>{},loadWarehouseContext:async()=>{context.queryParams.warehouseId=context.route.query.warehouseId},loadLocationContext:async()=>{context.queryParams.locationId=context.route.query.locationId},getList:async()=>requests.push({...context.queryParams}),openForm:(type,id)=>opened.push([type,id]),parsePositiveRouteQueryId:load('src/utils/routeQueryId.ts').parsePositiveRouteQueryId}
 vm.runInNewContext(transpile(body+'\nglobalThis.run=receiveCodeLink'),context);await context.run();assert.equal(requests.length,1);assert.equal(requests[0].code,'W-1');assert.equal(requests[0].name,undefined);assert.equal(requests[0].pageNo,1);assert.equal(opened[0][1],'9223372036854775806');if(kind==='warehouse')assert.equal(requests[0].frozen,undefined);if(kind==='location')assert.equal(requests[0].warehouseId,'123');if(kind==='area'){assert.equal(requests[0].locationId,'456');for(const axis of ['X','Y','Z'])assert.equal(requests[0]['position'+axis],undefined)}
 await context.run();assert.equal(opened.length,2);const routePath=context.route.path;context.route.path='/elsewhere';await context.run();assert.equal(requests.length,2);context.route.path=routePath;context.route.query={code:'W-2'};await context.run();assert.equal(requests[2].code,'W-2');assert.equal(opened.length,2);if(kind==='location')assert.equal(requests[2].warehouseId,undefined);if(kind==='area')assert.equal(requests[2].locationId,undefined);assert.match(source,/codeLinkVisit/)
 // A delayed list must not reopen a newer route's detail.
 let release;context.getList=()=>new Promise(resolve=>{release=resolve});context.route.query={id:'42'};const old=context.run();await new Promise(resolve=>setImmediate(resolve));context.route.path='/elsewhere';await context.run();release();await old;assert.equal(opened.length,2)
 context.route.path=routePath;const pending=[];context.getList=()=>new Promise(resolve=>pending.push(resolve));context.route.query={id:'43'};const first=context.run();await new Promise(resolve=>setImmediate(resolve));context.route.query={id:'44'};const second=context.run();await new Promise(resolve=>setImmediate(resolve));pending[1]();await second;pending[0]();await first;assert.equal(opened.length,3);assert.equal(opened[2][1],'44');context.route.query={id:'bad-id'};await assert.rejects(context.run(),/ID/)

 }
}
Promise.resolve().then(testLink).then(testRoutes).then(()=>console.log('storage code link Vue and route behavior PASS')).catch(e=>{console.error(e);process.exitCode=1})

