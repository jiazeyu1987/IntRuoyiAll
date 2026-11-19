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
 for (const kind of ['vendor','client']) {
  const route={path:`/mes/md/${kind}`,query:{code:'V/01'}}
  const queryParams={pageNo:8,pageSize:20,code:'old',name:'old',status:1}
  const requests=[]
  const context={route,queryParams,queryFormRef:{value:{resetFields(){}}},getList(){requests.push({...queryParams})}}
  const source=read(`src/views/mes/md/${kind}/index.vue`).replace(/\r/g, '')
  const body=source.match(/const receivePartnerCode = \(\) => \{[^]*?\n}/)[0]
  vm.runInNewContext(transpile(body+'\nglobalThis.run=receivePartnerCode'),context)
  for(const code of ['V/01','V02','V02']) {
   route.query.code=code;queryParams.name='old';let before=requests.length;context.run();assert.equal(requests.length,before+1);assert.equal(requests.at(-1).code,code);assert.equal(requests.at(-1).name,undefined);assert.equal(requests.at(-1).status,undefined);assert.equal(queryParams.pageNo,1)
  }
  route.path='/else';let before=requests.length;context.run();assert.equal(requests.length,before)
  route.path=`/mes/md/${kind}`;context.run();assert.equal(requests.length,before+1)
 }
 let permitted=true
 function component(file) {
  const {descriptor}=compiler.parse(read(file));const exports={}
  const script=compiler.compileScript(descriptor,{id:file,inlineTemplate:true})
  vm.runInNewContext(transpile(script.content),{exports,require:name=>name==='@/utils/permission'?{checkPermi:()=>permitted}:name.endsWith('BusinessCodeLink.vue')?{default:component('src/components/BusinessCodeLink.vue')}:require(name)})
  return exports.default
 }
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/:pathMatch(.*)*',component:{}}]});await router.push('/source')
 for(const kind of ['vendor','client']) for(const code of ['A/B 中文','',null,'-']) for(const permission of [true,false]) {
  permitted=permission;const container={children:[]};const app=renderer.createApp(component('src/components/PartnerCodeLink.vue'),{code,partnerType:kind});app.use(router).mount(container);const node=container.children[0]
  if(permission&&code&&code!=='-') {
   assert.equal(node.type,'a');assert.equal(new URL(node.props.href,'http://localhost').searchParams.get('code'),code)
   for(let i=0;i<2;i++){let before=router.currentRoute.value.query.codeLinkVisit;await node.props.onClick({button:0,preventDefault(){},stopPropagation(){}});assert.equal(router.currentRoute.value.query.code,code);assert.notEqual(router.currentRoute.value.query.codeLinkVisit,before)}
  }else assert.equal(node.type,'span')
  app.unmount()
 }

 for(const kind of ['vendor','client']) {
  const source=read(`src/views/mes/md/${kind}/index.vue`).replace(/\r/g, '')
  const body=source.match(/let listRequest = 0[^]*?\n}\n/)[0]
  const handler=source.match(/const receivePartnerCode = \(\) => \{[^]*?\n}/)[0]
  const pending=[];const route={path:`/mes/md/${kind}`,query:{code:' A '}}
  const api={ [kind==='vendor'?'getVendorPage':'getClientPage'](){return new Promise((resolve,reject)=>pending.push({resolve,reject}))} }
  const context={route,queryParams:{pageNo:1,pageSize:10,name:'old'},queryFormRef:{value:{resetFields(){}}},loading:{value:false},list:{value:[]},total:{value:0},[kind==='vendor'?'MdVendorApi':'MdClientApi']:api}
  vm.runInNewContext(transpile(body+'\n'+handler+'\nglobalThis.run=getList;globalThis.receive=receivePartnerCode'),context)
  context.receive();assert.equal(context.queryParams.code,'A')
  route.query.code='B';context.receive()
  pending[1].resolve({list:['B'],total:2});await new Promise(setImmediate)
  pending[0].resolve({list:['A'],total:1});await new Promise(setImmediate)
  assert.equal(context.list.value[0],'B');assert.equal(context.total.value,2)
  context.receive();route.path='/else';pending[2].resolve({list:['wrong'],total:3});await new Promise(setImmediate);assert.equal(context.list.value[0],'B')
  route.path=`/mes/md/${kind}`;const current=context.run();pending[3].reject(new Error('real failure'));await assert.rejects(current,/real failure/);assert.equal(context.loading.value,false)
 }
 console.log('PASS partner Vue permissions/empty/navigation and both target filtering/repeat/leave/reenter/one request')
}
main().catch(error=>{console.error(error);process.exitCode=1})
