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

let allowed=true
function component(file){const {descriptor}=compiler.parse(read(file));const script=compiler.compileScript(descriptor,{id:file,inlineTemplate:true});const exports={};vm.runInNewContext(transpile(script.content),{exports,require:n=>n.endsWith('.vue')?{default:component('src/components/'+n.split('/').pop())}:n==='@/utils/permission'?{checkPermi:()=>allowed}:require(n)});return exports.default}
async function links(){for(const [name,target]of [['Workstation','/mes/md/workstation'],['Machinery','/mes/dv/machinery']]){const router=createRouter({history:createMemoryHistory(),routes:[{path:'/source',component:{}},{path:target,component:{}}]});await router.push('/source');for(const props of [{code:' A/B 中文 ',id:42},{code:'CODE'},{code:''},{code:'-'},{code:null},{code:'DENIED',denied:true}]){allowed=!props.denied;const box={children:[]};const app=renderer.createApp(component('src/components/'+name+'CodeLink.vue'),props);app.use(router).mount(box);const node=box.children[0];if(!props.code||props.code==='-'||props.denied){assert.equal(node.type,'span')}else{assert.equal(node.type,'a');const url=new URL(node.props.href,'http://localhost');assert.equal(url.pathname,target);assert.equal(url.searchParams.get(props.id?'openId':'code'),props.id?'42':props.code.trim());let stop=false,prevent=false;const event={button:0,preventDefault(){prevent=true},stopPropagation(){stop=true}};await node.props.onClick(event);await vue.nextTick();assert(stop);assert(prevent);const visit=router.currentRoute.value.query.codeLinkVisit;await node.props.onClick(event);assert.notEqual(router.currentRoute.value.query.codeLinkVisit,visit);prevent=false;await node.props.onClick({...event,ctrlKey:true});assert.equal(prevent,false)}app.unmount()}}}
function extract(source,name){return source.match(new RegExp('const '+name+' = (?:async )?[^]*?\\n}'))[0]}
async function routes(){
 for(const domain of ['machinery','workstation']){
 const machinery=domain==='machinery';const file=machinery?'src/views/mes/dv/machinery/index.vue':'src/views/mes/md/workstation/index.vue';const source=read(file);const routePath=machinery?'/mes/dv/machinery':'/mes/md/workstation';const requests=[],opened=[];const state={conditions:['old'],appliedConditions:['old'],value:'old'};
 const ctx={parsePositiveRouteQueryId:value=>/^[1-9]\d*$/.test(String(value))?String(value):'',route:{path:routePath,query:{code:'CODE-1',openId:'42'}},queryParams:{pageNo:9,listFilters:'old',status:1,name:'old',code:'old',workshopId:9,processId:8},codeLinkListTemplate:{value:{resetColumnFilters(){ctx.resets++}}},resets:0,routeQuickFilterApplied:{value:true},openedMachineryDetailId:{value:'42'},openedWorkstationDetailId:{value:'42'},getList:async()=>requests.push({...ctx.queryParams}),openForm:(type,id)=>opened.push(id),normalizePositiveProcessId:x=>x||undefined,JSON};ctx.machineryQuickFilter=ctx.workstationQuickFilter={updateState:s=>Object.assign(state,s)};
 const names=machinery?['tryOpenDetailFromRoute','syncMachineryRoute']:['clearRouteQuickFilter','applyRouteQuickFilter','syncQueryParamsFromRoute','tryOpenDetailFromRoute'];let body=(machinery?'let machineryRouteGeneration=0;let getListRequestSequence=0;const buildMachineryRouteSignature=()=>JSON.stringify([route.path,route.query.openId,route.query.code,route.query.codeLinkVisit]);\n':'')+names.map(n=>extract(source,n)).join('\n');body+=machinery?'\nglobalThis.run=syncMachineryRoute':'\nglobalThis.run=async()=>{if(route.path!=="/mes/md/workstation")return;openedWorkstationDetailId.value="";syncQueryParamsFromRoute();await getList();tryOpenDetailFromRoute()}';vm.runInNewContext(transpile(body),ctx);
 await ctx.run();assert.equal(requests.length,1);assert.equal(ctx.resets,1);assert.equal(requests[0].pageNo,1);assert.equal(requests[0].listFilters,undefined);assert.equal(requests[0].status,undefined);assert.equal(state.conditions.length,0);assert.equal(state.appliedConditions.length,0);assert.equal(opened[0],'42');assert.equal(machinery?requests[0].code:requests[0].quickFilter.value,'CODE-1');
 await ctx.run();assert.equal(opened.length,2);ctx.route.path='/away';await ctx.run();assert.equal(requests.length,2);ctx.route.path=routePath;ctx.route.query={code:'CODE-2',openId:'42'};await ctx.run();assert.equal(opened.length,3);assert.equal(machinery?requests[2].code:requests[2].quickFilter.value,'CODE-2');ctx.route.query={};await ctx.run();assert.equal(requests[3].code,undefined);assert.equal(state.value,undefined);ctx.route.query={openId:'9223372036854775807'};await ctx.run();assert.equal(opened[opened.length-1],'9223372036854775807');ctx.route.query={openId:'1e3'};await assert.rejects(ctx.run(),/定位参数无效/);
 }
}
async function races(){for(const machinery of [true,false]){
 const source=read(machinery?'src/views/mes/dv/machinery/index.vue':'src/views/mes/md/workstation/index.vue');const name=machinery?'syncMachineryRoute':'syncWorkstationRoute';const target=machinery?'/mes/dv/machinery':'/mes/md/workstation';const pending=[],opened=[];const ctx={parsePositiveRouteQueryId:value=>/^[1-9]\d*$/.test(String(value))?String(value):'',route:{path:target,query:{code:'A',openId:'1'}},queryParams:{pageSize:10,name:'old',type:'old',quickFilter:'old'},codeLinkListTemplate:{value:{resetColumnFilters(){}}},machineryQuickFilter:{updateState(){}},openedMachineryDetailId:{value:''},openedWorkstationDetailId:{value:''},lastAppliedWorkstationRouteQuerySignature:{value:''},WORKSTATION_ROUTE_PATH:target,syncQueryParamsFromRoute(){},getList:()=>new Promise(resolve=>pending.push(resolve)),tryOpenDetailFromRoute:()=>opened.push(ctx.route.query.openId)};
 const pre=machinery?'let machineryRouteGeneration=0,getListRequestSequence=0;const buildMachineryRouteSignature=()=>JSON.stringify(route);':'let workstationRouteGeneration=0,workstationListSequence=0;const buildWorkstationRouteQuerySignature=()=>JSON.stringify(route);';vm.runInNewContext(transpile(pre+extract(source,name)+'\nglobalThis.run='+name),ctx);
 const a=ctx.run();ctx.route.query={code:'B',openId:'2'};const b=ctx.run();pending[1]();await b;pending[0]();await a;assert.equal(opened.join(','),'2');if(machinery){assert.equal(ctx.queryParams.name,undefined);assert.equal(ctx.queryParams.type,undefined);assert.equal(ctx.queryParams.quickFilter,undefined)}
 ctx.route.query={code:'C',openId:'3'};const c=ctx.run();ctx.route.path='/away';await ctx.run();pending[2]();await c;assert.equal(opened.join(','),'2');
}}

Promise.resolve().then(links).then(routes).then(races).then(()=>console.log('equipment code link and route behavior PASS')).catch(e=>{console.error(e);process.exitCode=1})
