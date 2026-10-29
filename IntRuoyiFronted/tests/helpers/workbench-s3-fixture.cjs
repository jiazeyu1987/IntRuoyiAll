const assert = require('node:assert/strict')
const test = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const pinia = require('pinia')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = file => fs.readFileSync(path.join(root, file), 'utf8')
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
const tick = async () => { for (let i=0;i<20;i++) await Promise.resolve(); await vue.nextTick() }
const descriptor = { sourceId: 'SIXTH', ownerModule: 'sixth', contractVersion: 1, sortRank: 6, label: '第六来源', taskType: '测试任务', policyId: 'sixth.policy', actionKind: 'sixth.open', payloadVersion: 1 }
const dir = { contractVersion: 1, registryRevision: 'fixture-r1', sources: [descriptor] }
const row = { sourceId: 'SIXTH', taskKey: 'SIXTH:opaque:work-A', businessId: 'opaque:work-A', taskType: descriptor.taskType, source: descriptor.label, detail: '任务A', statusLabel: '待办', createdAt: null, dueAt: null, action: { actionKind: 'sixth.open', payloadVersion: 1, payload: { key: 'opaque:work-A' } } }
function fixture(options = {}) {
 let activeDirectory=dir,activeRow=row,activeManifest={contractVersion:1,registryRevision:dir.registryRevision,modules:[{ownerModule:'sixth',enabled:true,sourceIds:['SIXTH']}]},activeUnits
 const routes=[]
 const scope=vue.effectScope(),user=vue.reactive({getUser:{id:1},getPermissions:[],getRoles:[]})
 const cache=options.cache||new Map(),wsCache={get:key=>cache.get(key),set:(key,value)=>cache.set(key,value),delete:key=>cache.delete(key)}
 const sessionStorage=options.sessionStorage||new Map()
 const requests=[],contexts=new Map(),hooks={},messages=[],successes=[]
 const listeners=new Map()
 const dom={location:{origin:'https://example.test'},visibilityState:'visible',
  sessionStorage:{getItem:key=>sessionStorage.get(key)??null,setItem:(key,value)=>sessionStorage.set(key,value),removeItem:key=>sessionStorage.delete(key)},
  addEventListener:(name,fn)=>{if(!listeners.has(name))listeners.set(name,new Set());listeners.get(name).add(fn);hooks[name]=()=>[...listeners.get(name)].forEach(fn=>fn())},
  removeEventListener:(name,fn)=>listeners.get(name)?.delete(fn)}
 let transport=async r=>{
  if(r.url==='/workbench/sources')return activeDirectory
  if(r.url==='/workbench/context'){
   const c={...r.data,contractVersion:3,visibilityVersion:'1',queryVersion:String(contexts.size+1).padStart(43,'x'),expiresAt:'2099-01-01T00:00:00Z'}
   contexts.set(c.queryVersion,c);return c
  }
  const c=contexts.get(r.params?.queryVersion||r.data?.queryVersion)
  if(r.url.endsWith('/visibility'))return {changed:true,visibilityVersion:'2',contextEpoch:c.contextEpoch}
  const count={contractVersion:3,registryRevision:activeDirectory.registryRevision,queryVersion:c.queryVersion,contextEpoch:c.contextEpoch,visibilityVersion:c.visibilityVersion,filteredTotal:c.sources.length?1:0,businessTotal:c.sources.length?3:0,hiddenTotal:c.sources.length?1:0,readAt:'2026-10-09T12:00:00'}
  return r.url.endsWith('/page')?{...count,rows:c.sources.length?[activeRow]:[],effectivePageNo:1,pageSize:r.params.pageSize}:count
 }
 const request=Object.fromEntries(['get','post'].map(method=>[method,async r=>{requests.push({method,...r});return transport(r)}]))
 const p=pinia.createPinia();pinia.setActivePinia(p)
 const deps={vue,pinia,'fixture-units':{get units(){return activeUnits}},'@/config/axios':{default:request},'@/hooks/web/useCache':{useCache:()=>({wsCache}),CACHE_KEY:{USER:'user',ROLE_ROUTERS:'routers',TenantId:'tenant',VisitTenantId:'visit',LoginForm:'form',LoginTenantHistory:'history'}},'@/utils/jsencrypt':{encrypt:x=>x,decrypt:x=>x},'@/store/modules/user':{useUserStoreWithOut:()=>user},'../index':{store:p}}
 const loaded={},files={'@/workbench/queryContract':'src/workbench/queryContract.ts','@/workbench/queryContext':'src/workbench/queryContext.ts','@/workbench/registry':'src/workbench/registry.ts','@/api/system/profileWorkbenchTodo':'src/api/system/profileWorkbenchTodo/index.ts'}
 function evaluate(source){
  const m={exports:{}}
  Function('require','module','exports','crypto','window','document',compile(source))(id=>{
   if(id==='./queryContext')return load('@/workbench/queryContext')
   if(id==='./registryContract')return evaluate(read('src/workbench/registryContract.ts'))
   if(id==='./manifest.json')return {default:activeManifest}
   if(files[id])return load(id)
   assert.ok(deps[id],'unexpected dependency '+id);return deps[id]
  },m,m.exports,require('node:crypto').webcrypto,dom,dom)
  return m.exports
 }
 function load(id){
  if(loaded[id])return loaded[id]
  let s=read(files[id])
  if(id==='@/workbench/queryContract')s=s.replace('import.meta.env.VITE_WORKBENCH_QUERY_CONTRACT',JSON.stringify(options.queryContract))
  if(id==='@/workbench/registry')s=s.replace(/import\.meta\.glob<[^\n]+/,"require('fixture-units').units")
  return loaded[id]=scope.run(()=>evaluate(s))
 }
 activeUnits={sixth:{default:{ownerModule:'sixth',contractVersion:1,sources:[{descriptor,open:async()=>{}}]}}}
 if(options.mes){
  deps['@/utils/exactJavaLong']=evaluate(read('src/utils/exactJavaLong.ts'))
  deps['@/workbench/contracts']=evaluate(read('src/workbench/contracts.ts'))
  deps['@/api/mes/pro/edhr/batchExecution']={openEdhrBatchTask:options.open}
  deps['@/utils/edhrWorkTaskNavigation']=evaluate(read('src/utils/edhrWorkTaskNavigation.ts'))
  deps['@/api/mes/pro/handoff']={handoffNavigationContext:options.handoffContext || (async()=>{throw new Error('handoff fixture context required')})}
  deps['@/utils/activeOrderHandoffNavigation']=evaluate(read('src/utils/activeOrderHandoffNavigation.ts'))
  const unit=evaluate(read('src/workbench-sources/mes/registration.ts')).default
  activeUnits={mes:{default:unit}}
  activeDirectory={...dir,sources:unit.sources.map(s=>s.descriptor)}
  activeManifest={...activeManifest,modules:[{ownerModule:'mes',enabled:true,sourceIds:unit.sources.map(s=>s.descriptor.sourceId)}]}
  const d=unit.sources[0].descriptor
  activeRow={...row,sourceId:d.sourceId,taskKey:d.sourceId+':'+options.item.id,businessId:options.item.id,taskType:d.taskType,source:d.label,action:{actionKind:d.actionKind,payloadVersion:1,payload:options.item}}
 }
 const auth=evaluate(read('src/utils/auth.ts'));deps['@/utils/auth']=auth
 if(!options.cache){auth.setTenantId(1);auth.setToken({accessToken:'session-A',refreshToken:'refresh-A'})}
 const context=load('@/workbench/queryContext'),registry=load('@/workbench/registry'),api=load('@/api/system/profileWorkbenchTodo')
 const badgeModule=scope.run(()=>evaluate(read('src/store/modules/profileWorkbenchTodoBadge.ts'))),badge=badgeModule.useProfileWorkbenchTodoBadgeStore(p)
 const s=parse(read('src/views/Profile/components/ProfileWorkbench.vue')).descriptor.scriptSetup.content
 const ast=ts.createSourceFile('page.ts',s,ts.ScriptTarget.Latest,true),body=ast.statements.filter(s=>!ts.isImportDeclaration(s)).map(s=>s.getText(ast)).join('\n')
 const bindings={...vue,...api,...registry,...context,...badgeModule,useProfileWorkbenchTodoBadgeStore:()=>badge,useUserStore:()=>user,getAccessToken:auth.getAccessToken,
  defineOptions(){},useRouter:()=>({push:async route=>routes.push(route)}),onMounted:fn=>hooks.mount=fn,onBeforeUnmount:fn=>hooks.unmount=fn,
  ElMessage:{error:m=>messages.push(m),success:m=>successes.push(m)},ElMessageBox:{confirm:async()=>{}},useUserTableColumns:()=>({}),useTableQuickFilter:()=>({}),
  window:dom,document:dom
 }
 const createPage=()=>scope.run(()=>{
  const pageScope=vue.effectScope()
  const pageBindings={...bindings,onBeforeUnmount:fn=>hooks.unmount=()=>{fn();pageScope.stop()}}
  return pageScope.run(()=>Function(...Object.keys(pageBindings),compile(body)+';return { loadWorkbench,openTodo,handleHideTodo,handleRestoreTodo,handleVisibilityTabChange,todoRows,total,hiddenTotal,loading,silentLoading,actionsReady,loadErrorMessages,actionTaskKey,queryParams,activeVisibilityTab,sortState };')(...Object.values(pageBindings)))
 })
 const page=createPage()
 const mountMenu=()=>{
  const source=parse(read('src/layout/components/Menu/src/Menu.vue')).descriptor.script.content
  const ast=ts.createSourceFile('menu.tsx',source,ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX)
  let setup;const find=node=>{if(ts.isMethodDeclaration(node)&&node.name.getText(ast)==='setup')setup=node;ts.forEachChild(node,find)};find(ast)
  const statements=[]
  for(const statement of setup.body.statements){
   const text=statement.getText(ast)
   if(text.startsWith('const menuMode'))break
   statements.push(text)
  }
  for(const statement of setup.body.statements){const text=statement.getText(ast);if(text.startsWith('onMounted(')||text.startsWith('const releaseFocusRefresh')||text.startsWith('onBeforeUnmount('))statements.push(text)}
  const menuHooks={},logs=[],menuBindings={...bindings,props:{},console:{error:(...args)=>logs.push(args)},useAppStore:()=>({getLayout:'classic'}),usePermissionStore:()=>({getRouters:[{meta:{personalWorkbenchTodoBadge:true}}]}),useApprovalTodoBadgeStore:()=>({ensureTodoTotalLoaded:async()=>{}}),hasApprovalTodoBadgeRoute:()=>false,hasProfileWorkbenchTodoBadgeRoute:badgeModule.hasProfileWorkbenchTodoBadgeRoute,onMounted:fn=>menuHooks.mount=fn,onBeforeUnmount:fn=>menuHooks.unmount=fn}
  const handlers=scope.run(()=>Function(...Object.keys(menuBindings),compile(statements.join('\n'))+';return {loadProfileWorkbenchTodoBadge,reportProfileWorkbenchTodoBadgeError};')(...Object.values(menuBindings)))
  menuHooks.mount();return {...menuHooks,...handlers,logs}
 }
 return {page,createPage,badge,context,registry,api,requests,contexts,messages,successes,hooks,bindings,scope,user,auth,cache,sessionStorage,routes,mountMenu,listeners,dom,
  setTransport:fn=>{const old=transport;transport=r=>fn(r,old)},
  identity:(id,token='session-'+id)=>{auth.setToken({accessToken:token,refreshToken:'refresh-'+id});user.getUser.id=id},
  tenant:value=>auth.setTenantId(value),visit:value=>auth.setVisitTenantId(value)
 }
}

module.exports={fixture,deferred,tick,descriptor,dir,row,read,compile}
