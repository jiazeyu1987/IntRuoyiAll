const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),ts=require('typescript'),vue=require('vue'),sfc=require('vue/compiler-sfc')
const {createRouter,createMemoryHistory}=require('vue-router')
const transpile=s=>ts.transpileModule(s,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText
let allowed=true
function load(name){const {descriptor}=sfc.parse(fs.readFileSync(`${__dirname}/../src/components/${name}.vue`,'utf8'));const out={};vm.runInNewContext(transpile(sfc.compileScript(descriptor,{id:name,inlineTemplate:true}).content),{exports:out,require:n=>n==='./BusinessCodeLink.vue'?{default:load('BusinessCodeLink')}:n==='@/utils/permission'?{checkPermi:()=>allowed}:require(n)});return out.default}
const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:text=>({text}),setText:(n,t)=>n.text=t,setElementText:(n,t)=>n.text=t,patchProp:(n,k,o,v)=>n.props[k]=v,insert:(n,p)=>{n.parent=p;p.children.push(n)},remove:()=>{},parentNode:n=>n.parent,nextSibling:()=>null})
;(async()=>{for(const [name,idProp,target] of [['ExecutionCodeLink','executionId','/mes/pro/feedback/edhr-execution/form'],['BatchExecutionCodeLink','batchExecutionId','/mes/pro/feedback/edhr-batch-execution/detail']]){for(const permit of [true,false]){allowed=permit;const router=createRouter({history:createMemoryHistory(),routes:[{path:'/source',component:{}},{path:target,component:{}}]});await router.push('/source');const root={children:[]};const app=renderer.createApp(load(name),{code:'EXEC/中文', [idProp]:'42'});app.use(router).mount(root);const node=root.children.find(n=>n.type);assert.equal(node.type,permit?'a':'span');if(permit){assert.equal(new URL(node.props.href,'http://x').searchParams.get('id'),'42');await node.props.onClick({button:0,stopPropagation(){},preventDefault(){}});await vue.nextTick();assert.equal(router.currentRoute.value.path,target);assert.equal(router.currentRoute.value.query.id,'42');assert(router.currentRoute.value.query.codeLinkVisit)}app.unmount()}}console.log('execution links real Vue/router behavior PASS')})().catch(e=>{console.error(e);process.exitCode=1})

const routeSources = [
 ['src/views/mes/pro/edhr/ExecutionPage.vue', 'MesProFeedbackEdhrExecutionForm'],
 ['src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue', 'MesProEdhrBatchExecutionDetail']
]
;(async () => {
for (const [file, name] of routeSources) {
 const source = fs.readFileSync(`${__dirname}/../${file}`, 'utf8')
 const normalized = source.replace(/\r/g, ''); const start = normalized.lastIndexOf('\nwatch(')
 const end = normalized.indexOf('\n)\n', start) + 3
 const body = normalized.slice(start, end)
 assert(body.includes('codeLinkVisit'))
 const route = vue.reactive({ name, query: { id: '42', codeLinkVisit: 'first' } })
 let loads = 0
 const context = { route, watch: vue.watch, resolveExecutionContextKey: () => String(route.query.id), loadedExecutionContextKey: { value: '42' }, initializeExecutionPage: () => loads++, parsePositiveRouteQueryId: Number, sameRouteQueryId: (a,b) => String(a) === String(b), detail: { value: { id: 42 } }, selectedFillCarrier: { value: undefined }, loadDetail: () => loads++ }
 const scope = vue.effectScope()
 scope.run(() => vm.runInNewContext(transpile(body), context))
 route.query.codeLinkVisit = 'repeat'; await vue.nextTick(); assert.equal(loads, 1)
 route.name = 'Other'; await vue.nextTick(); assert.equal(loads, 1)
 route.name = name; await vue.nextTick(); assert.equal(loads, 2)
 route.query.id = '43'; await vue.nextTick(); assert.equal(loads, 3)
 scope.stop()
}
console.log('execution target real Vue watch behavior PASS')
})().catch(e => { console.error(e); process.exitCode=1 })
