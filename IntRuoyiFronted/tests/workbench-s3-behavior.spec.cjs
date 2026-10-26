const assert=require('node:assert/strict')
const test=require('node:test')
const {fixture,deferred,tick,descriptor,dir,row,read,compile}=require('./helpers/workbench-s3-fixture.cjs')

test('visible and hidden snapshots survive round trips and browser reload independently',async()=>{
 const f=fixture();await f.page.loadWorkbench();const visible=f.page.todoRows.value
 f.setTransport(async(r,next)=>{
  const result=await next(r)
  if(r.url.endsWith('/page')&&f.contexts.get(r.params.queryVersion).querySpec.visibility==='hidden')return {...result,rows:[{...row,detail:'已隐藏任务'}]}
  return result
 })
 f.page.activeVisibilityTab.value='hidden';await f.page.loadWorkbench();const hidden=f.page.todoRows.value
 const gate=deferred();f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(()=>next(r)):next(r))
 f.page.activeVisibilityTab.value='visible';const pending=f.page.loadWorkbench()
 assert.equal(f.page.todoRows.value,visible);gate.resolve();await pending
 f.page.activeVisibilityTab.value='hidden';const back=f.page.loadWorkbench()
 assert.equal(f.page.todoRows.value,hidden);await back
 assert.equal(f.sessionStorage.size,2);f.scope.stop()
 const reload=fixture({cache:f.cache,sessionStorage:f.sessionStorage})
 const key=partition=>JSON.stringify([reload.page.queryParams,partition,reload.page.sortState.value])
 assert.equal(reload.badge.readPageSnapshot(key('visible')).rows[0].detail,row.detail)
 assert.equal(reload.badge.readPageSnapshot(key('hidden')).rows[0].detail,'已隐藏任务')
 reload.auth.removeToken();assert.equal(reload.sessionStorage.size,0);reload.scope.stop()
})

test('first partition switch without snapshot is silent and never renders the other partition as current',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(()=>next(r)):next(r))
 f.page.activeVisibilityTab.value='hidden';f.page.handleVisibilityTabChange()
 assert.equal(f.page.loading.value,true);assert.equal(f.page.silentLoading.value,true)
 assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.actionsReady.value,false)
 await tick();gate.resolve();await tick();assert.equal(f.page.loading.value,false);f.scope.stop()
})

test('full page reload restores session snapshot before background request without persisting credentials or query tokens',async()=>{
 const first=fixture();await first.page.loadWorkbench()
 const persisted=[...first.sessionStorage.values()].join('')
 for(const secret of ['session-A','refresh-A','queryVersion','contextEpoch'])assert.equal(persisted.includes(secret),false)
 first.scope.stop()
 const reloaded=fixture({cache:first.cache,sessionStorage:first.sessionStorage}),gate=deferred()
 reloaded.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(()=>next(r)):next(r))
 const pending=reloaded.page.loadWorkbench()
 assert.equal(reloaded.page.todoRows.value[0].taskKey,row.taskKey);assert.equal(reloaded.page.total.value,1)
 assert.equal(reloaded.page.actionsReady.value,false)
 gate.resolve();assert.equal(await pending,true);reloaded.scope.stop()
})

test('token renewal preserves persisted snapshot scope while a new login invalidates it',async()=>{
 const first=fixture();await first.page.loadWorkbench();const session=first.auth.getAuthSessionId()
 first.auth.setRefreshedToken({accessToken:'renewed',refreshToken:'renewed-refresh'})
 assert.equal(first.auth.getAuthSessionId(),session);first.scope.stop()
 const renewed=fixture({cache:first.cache,sessionStorage:first.sessionStorage})
 assert.ok(renewed.badge.readPageSnapshot(JSON.stringify([renewed.page.queryParams,renewed.page.activeVisibilityTab.value,renewed.page.sortState.value])))
 renewed.auth.setToken({accessToken:'new-login',refreshToken:'new-login-refresh'})
 assert.notEqual(renewed.auth.getAuthSessionId(),session);assert.equal(renewed.sessionStorage.size,0)
 renewed.auth.removeToken();assert.equal(renewed.auth.getAuthSessionId(),undefined);renewed.scope.stop()
})

for(const change of ['tenant','permissions'])test('reload with changed '+change+' refuses persisted snapshot',async()=>{
 const first=fixture();await first.page.loadWorkbench();first.scope.stop()
 const second=fixture({cache:first.cache,sessionStorage:first.sessionStorage})
 if(change==='tenant')second.tenant(2)
 else second.user.getPermissions.push('new:permission')
 const gate=deferred();second.setTransport((r,next)=>r.url.endsWith('/sources')?gate.promise.then(()=>next(r)):next(r))
 const pending=second.page.loadWorkbench();assert.equal(second.page.todoRows.value.length,0)
 assert.equal(second.sessionStorage.size,0);second.hooks.unmount();gate.resolve();await pending;second.scope.stop()
})

test('malformed persisted snapshot is removed with explicit error and no hidden server read',async()=>{
 const f=fixture();f.sessionStorage.set('int:profile-workbench:page-snapshot:v1:visible','invalid json')
 assert.equal(await f.page.loadWorkbench(),false);assert.equal(f.requests.length,0)
 assert.equal(f.sessionStorage.size,0);assert.equal(f.page.loadErrorMessages.value.length,1);f.scope.stop()
})

test('logout clears persisted snapshot even before it has been restored into memory',async()=>{
 const first=fixture();await first.page.loadWorkbench();first.scope.stop()
 const second=fixture({cache:first.cache,sessionStorage:first.sessionStorage})
 second.badge.ensureContextWatch();assert.equal(second.badge.pageSnapshots.visible,undefined)
 second.auth.removeToken();assert.equal(second.sessionStorage.size,0);second.scope.stop()
})

test('menu refresh while page is closed retains snapshot for the next open',async()=>{
 const f=fixture();await f.page.loadWorkbench();const rows=f.page.todoRows.value
 f.hooks.unmount();const menu=f.mountMenu();await tick()
 f.hooks.focus();await tick()
 assert.equal(f.badge.pageSnapshots.visible.rows,rows)
 const page=f.createPage(),gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(()=>next(r)):next(r))
 const pending=page.loadWorkbench();assert.equal(page.todoRows.value,rows);assert.equal(page.total.value,1)
 gate.resolve();assert.equal(await pending,true);menu.unmount();f.scope.stop()
})

test('explicit permission-directory invalidation still discards the display snapshot',async()=>{
 const f=fixture();await f.page.loadWorkbench()
 f.context.invalidateWorkbenchContext('授权来源已变化')
 assert.equal(f.badge.pageSnapshots.visible,undefined);assert.equal(f.page.todoRows.value.length,0);f.scope.stop()
})

test('both refresh spinner and table mask are limited to a request without a snapshot',()=>{
 const source=read('src/views/Profile/components/ProfileWorkbench.vue')
 assert.match(source,/:loading="loading && total === undefined && !silentLoading"/)
 assert.match(source,/v-loading="loading && total === undefined && !silentLoading"/)
})

test('focus plus visibilitychange does not restart the active page request',async()=>{
 const f=fixture(),gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(()=>next(r)):next(r))
 f.hooks.mount();await tick()
 f.hooks.focus();f.hooks.visibilitychange();await tick()
 assert.equal(f.requests.filter(r=>r.url.endsWith('/sources')).length,1)
 assert.equal(f.requests.filter(r=>r.url.endsWith('/page')).length,1)
 gate.resolve();await tick();assert.equal(f.page.todoRows.value.length,1)
 assert.equal(f.page.loading.value,false);f.scope.stop()
})

test('concurrent badge consumers share a count read and page loading supplies its own badge',async()=>{
 const f=fixture(),gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/count')?gate.promise.then(()=>next(r)):next(r))
 const first=f.badge.ensureTodoTotalLoaded();await tick()
 await Promise.all([f.badge.ensureTodoTotalLoaded(),f.badge.ensureTodoTotalLoaded()])
 assert.equal(f.requests.filter(r=>r.url.endsWith('/count')).length,1)
 gate.resolve();await first;assert.equal(f.badge.todoTotal,3)
 const pageGate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/page')?pageGate.promise.then(()=>next(r)):next(r))
 const page=f.page.loadWorkbench();await tick()
 await f.badge.ensureTodoTotalLoaded()
 assert.equal(f.requests.filter(r=>r.url.endsWith('/count')).length,1)
 pageGate.resolve();assert.equal(await page,true);assert.equal(f.badge.todoTotal,3);f.scope.stop()
})

const actualRequestContextGuard=(ts,ast)=>{
 let initializer
 const visit=node=>{
  if(ts.isVariableDeclaration(node)&&node.name.getText(ast)==='assertRequestContextCurrent')initializer=node.initializer.getText(ast)
  ts.forEachChild(node,visit)
 }
 visit(ast);assert.ok(initializer,'actual request context guard is present')
 return Function(compile('const guard='+initializer+';')+';return guard;')()
}
test('neutral sixth nonnumeric source, token-only requests and business badge',async()=>{
 const f=fixture();assert.equal(await f.page.loadWorkbench(),true)
 assert.equal(f.page.todoRows.value[0].businessId,'opaque:work-A');assert.equal(f.page.total.value,1);assert.equal(f.badge.todoTotal,3)
 assert.deepEqual(Object.keys(f.requests.find(r=>r.url.endsWith('/page')).params).sort(),['contextEpoch','pageNo','pageSize','queryVersion'])
 await f.api.setProfileWorkbenchTaskVisibility([...f.contexts.values()][0],row.taskKey,'hide')
 assert.deepEqual(Object.keys(f.requests.at(-1).data).sort(),['contextEpoch','operation','queryVersion','taskKey']);f.scope.stop()
})
test('context failure zero reads/writes, unknown counts; successful empty is known zero',async()=>{
 const f=fixture();f.setTransport((r,next)=>r.url.endsWith('/context')?Promise.reject(new Error('初始化失败')):next(r))
 assert.equal(await f.page.loadWorkbench(),false);assert.equal(f.requests.filter(r=>r.url.includes('/tasks/')).length,0)
 assert.equal(f.badge.todoTotal,undefined);assert.equal(f.page.total.value,undefined);assert.match(f.page.loadErrorMessages.value[0],/初始化失败/)
 f.setTransport(async(r,next)=>{
  if(r.url.endsWith('/context'))return {...r.data,contractVersion:3,visibilityVersion:'1',queryVersion:'a'.repeat(43),expiresAt:'2099-01-01T00:00:00Z'}
  if(r.url.endsWith('/page'))return {contractVersion:3,registryRevision:dir.registryRevision,...r.params,visibilityVersion:'1',rows:[],filteredTotal:0,businessTotal:0,hiddenTotal:0,effectivePageNo:1,readAt:'2026-10-09T12:00:00'}
  return next(r)
 })
 assert.equal(await f.page.loadWorkbench(),true);assert.equal(f.badge.todoTotal,0);assert.equal(f.badge.loaded,true);f.scope.stop()
})
for(const stage of ['/sources','/context','/page'])for(const reject of [false,true])test('page '+stage+' stale '+(reject?'error':'success')+' has no commits',async()=>{
 const f=fixture(),gate=deferred();let once=true
 f.setTransport((r,next)=>{if(once&&r.url.endsWith(stage)){once=false;return gate.promise.then(()=>{if(reject)throw new Error('旧请求失败');return next(r)})}return next(r)})
 const old=f.page.loadWorkbench();await tick();await f.page.loadWorkbench();gate.resolve();await old
 assert.equal(f.page.todoRows.value.length,1);assert.equal(f.page.loading.value,false);assert.deepEqual(f.page.loadErrorMessages.value,[]);assert.equal(f.badge.todoTotal,3);f.scope.stop()
})
test('directory query mutation stops next stage; focus preserves same-query snapshot and revokes actions',async()=>{
 const f=fixture(),gate=deferred();f.setTransport((r,next)=>r.url.endsWith('/sources')?gate.promise.then(()=>next(r)):next(r))
 const pending=f.page.loadWorkbench();f.page.queryParams.taskType='另一类型';gate.resolve();await pending
 assert.equal(f.requests.filter(r=>r.url.endsWith('/context')).length,0)
 f.setTransport((r,next)=>next(r));await f.page.loadWorkbench();f.hooks.mount();await tick()
 const wait=deferred();f.setTransport((r,next)=>r.url.endsWith('/sources')?wait.promise.then(()=>next(r)):next(r))
 f.hooks.focus();assert.equal(f.page.todoRows.value.length,1);assert.equal(f.page.total.value,1);assert.equal(f.page.actionsReady.value,false);assert.equal(f.badge.todoTotal,undefined)
 wait.resolve();await tick();f.scope.stop()
})

test('same snapshot renders immediately on reopen and identical response keeps row references without a second read',async()=>{
 const f=fixture();await f.page.loadWorkbench();const rows=f.page.todoRows.value
 f.hooks.unmount();const page=f.createPage(),gate=deferred(),before=f.requests.length
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(async()=>structuredClone(await next(r))):next(r))
 const pending=page.loadWorkbench()
 assert.equal(page.todoRows.value,rows);assert.equal(page.total.value,1);assert.equal(page.actionsReady.value,false)
 await page.openTodo(rows[0]);assert.equal(f.routes.length,0)
 await tick();gate.resolve();assert.equal(await pending,true)
 assert.equal(page.todoRows.value,rows);assert.equal(page.actionsReady.value,true)
 assert.equal(f.requests.slice(before).filter(r=>r.url.endsWith('/page')).length,1);f.scope.stop()
})

test('changed response updates snapshot directly in the single background request',async()=>{
 const f=fixture();await f.page.loadWorkbench();const rows=f.page.todoRows.value,gate=deferred(),before=f.requests.length
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise.then(async()=>({...await next(r),rows:[{...row,detail:'更新后的任务'}]})):next(r))
 const pending=f.page.loadWorkbench();assert.equal(f.page.todoRows.value,rows)
 await tick();gate.resolve();assert.equal(await pending,true)
 assert.notEqual(f.page.todoRows.value,rows);assert.equal(f.page.todoRows.value[0].detail,'更新后的任务')
 assert.equal(f.badge.pageSnapshots.visible.rows,f.page.todoRows.value)
 assert.equal(f.requests.slice(before).filter(r=>r.url.endsWith('/page')).length,1);f.scope.stop()
})

for(const change of ['tenant','visit','permission','session'])test('snapshot is synchronously cleared by '+change+' change',async()=>{
 const f=fixture();await f.page.loadWorkbench();assert.ok(f.badge.pageSnapshots.visible)
 if(change==='tenant')f.tenant(2)
 if(change==='visit')f.visit(2)
 if(change==='permission')f.user.getPermissions.push('new:permission')
 if(change==='session')f.identity(2)
 assert.equal(f.badge.pageSnapshots.visible,undefined);assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.actionsReady.value,false)
 f.scope.stop()
})

for(const change of ['filter','page','sort','visibility'])test('snapshot does not cross '+change+' query',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/sources')?gate.promise.then(()=>next(r)):next(r))
 if(change==='filter')f.page.queryParams.taskType='其他任务'
 if(change==='page')f.page.queryParams.pageNo=2
 if(change==='sort')f.page.sortState.value={prop:'detail',order:'ascending'}
 if(change==='visibility')f.page.activeVisibilityTab.value='hidden'
 const pending=f.page.loadWorkbench();assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.total.value,undefined)
 f.hooks.unmount();gate.resolve();await pending;f.scope.stop()
})

test('background failure clears snapshot, keeps existing explicit error, and leaves actions disabled',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/page')?gate.promise:next(r))
 const pending=f.page.loadWorkbench();assert.equal(f.page.todoRows.value.length,1)
 await tick();gate.reject(new Error('后台查询失败'));assert.equal(await pending,false)
 assert.equal(f.page.todoRows.value.length,0);assert.equal(f.badge.pageSnapshots.visible,undefined)
 assert.equal(f.page.actionsReady.value,false);assert.match(f.page.loadErrorMessages.value[0],/后台查询失败/);f.scope.stop()
})

test('visibility write clears snapshot before request and never restores old rows on refresh failure',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/visibility')?gate.promise:r.url.endsWith('/context')?Promise.reject(new Error('刷新失败')):next(r))
 const pending=f.page.handleRestoreTodo(row)
 assert.equal(f.badge.pageSnapshots.visible,undefined);assert.equal(f.page.todoRows.value.length,0)
 gate.resolve({changed:true,visibilityVersion:'2',contextEpoch:[...f.contexts.values()][0].contextEpoch});await pending
 assert.equal(f.page.todoRows.value.length,0);assert.equal(f.badge.pageSnapshots.visible,undefined);f.scope.stop()
})
test('identity A→B→A/visit/permissions cannot resurrect old lease',async()=>{
 const f=fixture(),gate=deferred();let once=true
 f.setTransport((r,next)=>{if(once&&r.url.endsWith('/page')){once=false;return gate.promise.then(()=>next(r))}return next(r)})
 const pending=f.page.loadWorkbench();await tick();const lease=f.context.beginWorkbenchRead()
 f.identity(2);f.identity(1,'session-A-new');f.visit('7');f.user.getPermissions.push('sixth:new')
 assert.equal(f.context.isWorkbenchReadCurrent(lease),false)
 gate.resolve();await pending;await tick();assert.ok(!f.context.isWorkbenchReadCurrent(lease));f.scope.stop()
})
test('badge and page independent reads; stale badge finally cannot clear newer loading',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/count')?gate.promise.then(()=>next(r)):next(r))
 const pending=f.badge.refreshTodoTotal();await tick();assert.equal(f.page.todoRows.value.length,1)
 f.context.invalidateWorkbenchContext();const token=f.badge.beginBadgeUpdate();f.badge.tryCommit(token,{loading:true})
 gate.resolve();await pending;assert.equal(f.badge.loading,true);assert.equal(f.badge.todoTotal,undefined);f.scope.stop()
})
test('changed=false successful write + refresh failure distinct, one write no reverse/retry',async()=>{
 const f=fixture();await f.page.loadWorkbench();let submitted=false
 f.setTransport((r,next)=>{if(r.url.endsWith('/visibility')){submitted=true;return {changed:false,visibilityVersion:'1',contextEpoch:r.data.contextEpoch}}if(submitted&&r.url.endsWith('/context'))throw new Error('刷新上下文失败');return next(r)})
 await f.page.handleRestoreTodo(row)
 assert.equal(f.requests.filter(r=>r.url.endsWith('/visibility')).length,1);assert.match(f.page.loadErrorMessages.value.join('；'),/任务已恢复，但列表刷新失败/)
 assert.equal(f.page.actionTaskKey.value,'');assert.equal(f.badge.todoTotal,undefined);f.scope.stop()
})
test('identity during write rejects stale error/finally and cannot clear new action',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/visibility')?gate.promise:next(r))
 const pending=f.page.handleRestoreTodo(row);await tick();f.identity(2)
 f.page.actionTaskKey.value='new-action';gate.reject(new Error('旧写失败'));await pending
 assert.equal(f.page.actionTaskKey.value,'new-action');assert.ok(!f.page.loadErrorMessages.value.some(x=>x.includes('旧写失败')))
 assert.equal(f.requests.filter(r=>r.url.endsWith('/visibility')).length,1);f.scope.stop()
})
test('confirmation serialized; real cancel/close silent and Error(cancel) explicit; zero writes',async()=>{
 for(const error of ['cancel','close',new Error('cancel')]){
  const f=fixture();await f.page.loadWorkbench();const gate=deferred();f.bindings.ElMessageBox.confirm=()=>gate.promise
  const pending=f.page.handleHideTodo(row);await f.page.handleHideTodo(row);gate.reject(error);await pending
  assert.equal(f.requests.filter(r=>r.url.endsWith('/visibility')).length,0);assert.equal(f.page.actionTaskKey.value,'')
  assert.equal(f.messages.length,error instanceof Error?1:0);f.scope.stop()
 }
})
test('wrong binding/version/partition/action or legacy list rejects entire page',async()=>{
 for(const mutate of [p=>p.contractVersion=1,p=>p.contextEpoch='wrong',p=>p.visibilityVersion=1,p=>p.hiddenTotal=4,p=>p.rows[0].action.payload.key=9,p=>{p.list=p.rows;delete p.rows}]){
  const f=fixture();f.setTransport(async(r,next)=>{const p=await next(r);if(r.url.endsWith('/page')){const copy=structuredClone(p);mutate(copy);return copy}return p})
  assert.equal(await f.page.loadWorkbench(),false);assert.equal(f.page.todoRows.value.length,0);assert.equal(f.badge.todoTotal,undefined);assert.ok(f.page.loadErrorMessages.value.length);f.scope.stop()
 }
})
test('formal navigator exact Long open/route and stale awaited open has zero routing',async()=>{
 const calls=[],routes=[];let current=true;const gate=deferred(),module={exports:{}}
 const longModule={exports:{}};Function('module','exports',compile(read('src/utils/exactJavaLong.ts')))(longModule,longModule.exports)
 Function('require','module','exports',compile(read('src/utils/edhrWorkTaskNavigation.ts')))(id=>id==='@/utils/exactJavaLong'?longModule.exports:({openEdhrBatchTask:async data=>{calls.push(data);await gate.promise;return {executionId:'9223372036854775807',workTaskId:data.workTaskId,executionPageQuery:{batchExecutionId:data.batchExecutionId,batchTaskId:data.taskId}}}}),module,module.exports)
 const item={id:'9007199254740993',taskType:'FILL',batchExecutionId:'9223372036854775807',batchTaskId:'9007199254740995'}
 const pending=module.exports.navigateToEdhrWorkTask({push:async route=>routes.push(route)},item,'https://example.test',()=>current)
 assert.deepEqual(calls[0],{batchExecutionId:item.batchExecutionId,taskId:item.batchTaskId,workTaskId:item.id})
 gate.resolve();await pending;assert.equal(routes[0].query.workTaskId,item.id);assert.equal(routes[0].query.id,'9223372036854775807')
 current=false;await assert.rejects(module.exports.navigateToEdhrWorkTask({push:async()=>assert.fail()},item,'https://example.test',()=>current),/上下文/);assert.equal(calls.length,1)
})


for(const stage of ['/context','/count'])for(const reject of [false,true])test('badge '+stage+' old '+(reject?'error':'success')+' and finally stay obsolete',async()=>{
 const f=fixture(),gate=deferred();let once=true
 f.setTransport((r,next)=>{if(once&&r.url.endsWith(stage)){once=false;return gate.promise.then(()=>{if(reject)throw new Error('旧badge失败');return next(r)})}return next(r)})
 const old=f.badge.refreshTodoTotal();await tick();f.context.invalidateWorkbenchContext()
 await f.badge.refreshTodoTotal();gate.resolve();await old
 assert.equal(f.badge.todoTotal,3);assert.equal(f.badge.loading,false);assert.equal(f.badge.error,'');f.scope.stop()
})
test('current count failure leaves unknown and throws; rejected token bindings do not leak counts',async()=>{
 for(const mutate of [null,r=>r.contextEpoch='wrong',r=>r.queryVersion='wrong',r=>r.businessTotal=-1]){
  const f=fixture();f.setTransport(async(r,next)=>{if(r.url.endsWith('/count')){if(!mutate)throw new Error('count failed');const p=await next(r);mutate(p);return p}return next(r)})
  await assert.rejects(f.badge.refreshTodoTotal());assert.equal(f.badge.todoTotal,undefined);assert.equal(f.badge.loaded,false);assert.equal(f.badge.loading,false);assert.ok(f.badge.error);f.scope.stop()
 }
})
test('unmount while page/context awaits cannot commit, badge watch survives component disposal',async()=>{
 const f=fixture(),gate=deferred();f.setTransport((r,next)=>r.url.endsWith('/context')?gate.promise.then(()=>next(r)):next(r))
 const pending=f.page.loadWorkbench();await tick();f.hooks.unmount();gate.resolve();await pending
 assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.total.value,undefined);assert.equal(f.page.loading.value,false)
 f.scope.stop()
 const token=f.badge.beginBadgeUpdate();f.badge.applyTodoTotal(token,7);assert.equal(f.badge.todoTotal,7)
 f.context.invalidateWorkbenchContext();assert.equal(f.badge.todoTotal,undefined)
})
test('write failure is explicit, duplicate restore never sends second mutation',async()=>{
 const f=fixture();await f.page.loadWorkbench();const gate=deferred()
 f.setTransport((r,next)=>r.url.endsWith('/visibility')?gate.promise:next(r))
 const pending=f.page.handleRestoreTodo(row);await f.page.handleRestoreTodo(row)
 gate.reject(new Error('版本冲突'));await pending
 assert.equal(f.requests.filter(r=>r.url.endsWith('/visibility')).length,1);assert.match(f.page.loadErrorMessages.value[0],/版本冲突/);assert.equal(f.page.actionTaskKey.value,'');assert.equal(f.badge.todoTotal,undefined);f.scope.stop()
})
test('source set drift revokes page/badge lifetime without joining another count snapshot',async()=>{
 const f=fixture();await f.page.loadWorkbench();const old=f.context.beginWorkbenchRead()
 f.setTransport((r,next)=>r.url.endsWith('/sources')?{...dir,sources:[]}:next(r))
 await f.badge.refreshTodoTotal()
 assert.equal(f.context.isWorkbenchReadCurrent(old),false);assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.total.value,undefined);assert.equal(f.badge.todoTotal,undefined)
 assert.match(f.page.loadErrorMessages.value.join('；'),/授权来源已变化.*刷新/);assert.match(f.badge.error,/授权来源已变化/)
 assert.equal(await f.page.loadWorkbench(),true);assert.equal(f.page.total.value,0);assert.equal(f.badge.todoTotal,0);assert.equal(f.badge.loaded,true);f.scope.stop()
})
test('formal open response arriving after context invalidation never pushes route',async()=>{
 const gate=deferred(),module={exports:{}},routes=[]
 const longs={exports:{}};Function('module','exports',compile(read('src/utils/exactJavaLong.ts')))(longs,longs.exports)
 Function('require','module','exports',compile(read('src/utils/edhrWorkTaskNavigation.ts')))(id=>id==='@/utils/exactJavaLong'?longs.exports:({openEdhrBatchTask:()=>gate.promise}),module,module.exports)
 let current=true
 const pending=module.exports.navigateToEdhrWorkTask({push:async r=>routes.push(r)},{id:'9007199254740993',taskType:'FILL',batchExecutionId:'9223372036854775807',batchTaskId:'7'},'https://example.test',()=>current)
 current=false;gate.resolve({executionId:'9'});await assert.rejects(pending,/上下文/);assert.equal(routes.length,0)
})
test('actual Axios response interceptor rejects S3 401 without authentication replay',async()=>{
 const ts=require('typescript'),s=read('src/config/axios/service.ts'),ast=ts.createSourceFile('service.ts',s,ts.ScriptTarget.Latest,true)
 let callback,creator
 function visit(node){
  if(ts.isCallExpression(node)&&node.expression.getText(ast)==='service.interceptors.response.use')callback=node.arguments[0].getText(ast)
  if(ts.isVariableDeclaration(node)&&node.name.getText(ast)==='createApiError')creator=node.initializer.getText(ast)
  ts.forEachChild(node,visit)
 }
 visit(ast);assert.ok(callback);assert.ok(creator)
 const createApiError=Function('return '+compile('const value='+creator+';').replace(/const value = /,'').replace(/;\s*$/,''))()
 let refreshes=0,replays=0
 const deps={ApiEncrypt:{getEncryptHeader:()=> 'encrypted'},useI18n:()=>({t:x=>x}),result_code:200,errorCode:{default:'error'},ignoreMsgs:[],
  createApiError,assertRequestContextCurrent:actualRequestContextGuard(ts,ast),isRefreshToken:false,getRefreshToken:()=> 'refresh',refreshToken:async()=>{refreshes++;return {data:{code:0,data:{accessToken:'a',refreshToken:'r'}}}},
  setRefreshedToken(){},getAccessToken:()=> 'a',requestList:[],service:()=>{replays++},handleAuthorized:()=>{throw new Error('unexpected relogin')}}
 const handler=Function(...Object.keys(deps),compile('const callback='+callback+';')+';return callback;')(...Object.values(deps))
 await assert.rejects(handler({data:{code:401,msg:'登录过期'},headers:{},config:{skipAuthRefreshReplay:true,ignoreErrorMessage:true}}),e=>e.code===401)
 await assert.rejects(handler({data:{code:401,msg:'登录过期'},headers:{},config:{skipAuthRefreshReplay:true,isRequestContextCurrent:()=>false}}),{code:'REQUEST_CONTEXT_EXPIRED'})
 assert.equal(refreshes,0);assert.equal(replays,0)
 await handler({data:{code:401,msg:'登录过期'},headers:{},config:{headers:{}}})
 assert.equal(refreshes,1);assert.equal(replays,1,'ordinary existing callers keep their original refresh policy')
})


test('real nonreactive auth cache mutations synchronously invalidate even A→B→A without user snapshot change',async()=>{
 for(const mutate of [
  auth=>{auth.setVisitTenantId(2);auth.setVisitTenantId(1)},
  auth=>{auth.setTenantId(2);auth.setTenantId(1)},
  auth=>{auth.setToken({accessToken:'session-B',refreshToken:'rB'});auth.setToken({accessToken:'session-A',refreshToken:'rA'})},
  auth=>{auth.removeToken();auth.setToken({accessToken:'session-A',refreshToken:'rA'})},
  auth=>auth.removeTenantId()
 ]){
  const f=fixture();f.auth.setVisitTenantId(1);await f.page.loadWorkbench()
  const lease=f.context.beginWorkbenchRead(),id=f.user.getUser.id,permissions=[...f.user.getPermissions]
  mutate(f.auth)
  assert.equal(f.context.isWorkbenchReadCurrent(lease),false);assert.equal(f.page.todoRows.value.length,0);assert.equal(f.page.total.value,undefined);assert.equal(f.badge.todoTotal,undefined)
  assert.equal(f.user.getUser.id,id);assert.deepEqual([...f.user.getPermissions],permissions);f.scope.stop()
 }
})
for(const stage of ['/sources','/context','/page'])test('real auth visit A→B→A while '+stage+' awaits never resumes old query',async()=>{
 const f=fixture();f.auth.setVisitTenantId(1);const gate=deferred();let once=true,oldEpoch
 f.setTransport((r,next)=>{if(once&&r.url.endsWith(stage)){once=false;oldEpoch=r.data?.contextEpoch||r.params?.contextEpoch;return gate.promise.then(()=>next(r))}return next(r)})
 const old=f.page.loadWorkbench();await tick();const oldLease=f.context.beginWorkbenchRead()
 f.auth.setVisitTenantId(2);f.auth.setVisitTenantId(1)
 assert.equal(f.context.isWorkbenchReadCurrent(oldLease),false);assert.equal(f.page.todoRows.value.length,0);assert.equal(f.badge.todoTotal,undefined)
 await f.page.loadWorkbench();const latest=[...f.contexts.values()].at(-1);gate.resolve();await old
 assert.equal(f.page.todoRows.value.length,1);assert.deepEqual(f.page.loadErrorMessages.value,[])
 if(stage==='/context')assert.ok(!f.requests.some(r=>r.url.endsWith('/page')&&r.params.contextEpoch===oldEpoch))
 assert.ok(f.requests.some(r=>r.url.endsWith('/page')&&r.params.contextEpoch===latest.contextEpoch));f.scope.stop()
})
test('actual same-session refreshed-token mutation preserves identity lease/visit/data',async()=>{
 const f=fixture();f.auth.setVisitTenantId(8);await f.page.loadWorkbench()
 const lease=f.context.beginWorkbenchRead(),revision=f.auth.authContextRevision.value
 f.auth.setRefreshedToken({accessToken:'renewed-A',refreshToken:'renewed-R'})
 assert.equal(f.auth.authContextRevision.value,revision);assert.equal(f.context.isWorkbenchReadCurrent(lease),true)
 assert.equal(f.auth.getVisitTenantId(),8);assert.equal(f.page.todoRows.value.length,1);assert.equal(f.badge.todoTotal,3);f.scope.stop()
})


for(const stage of ['directory','formal-open'])test('actual Profile openTodo→registry→MES navigator unmount during '+stage+' routes zero',async()=>{
 const gate=deferred(),opens=[],item={id:'9007199254740993',taskType:'FILL',batchExecutionId:'9223372036854775807',batchTaskId:'9007199254740995'}
 const f=fixture({mes:true,item,open:async data=>{opens.push(data);return gate.promise}})
 await f.page.loadWorkbench()
 if(stage==='directory')f.setTransport((r,next)=>r.url.endsWith('/sources')?gate.promise.then(()=>next(r)):next(r))
 const pending=f.page.openTodo(f.page.todoRows.value[0]);await tick()
 if(stage==='formal-open')assert.deepEqual(opens[0],{batchExecutionId:item.batchExecutionId,taskId:item.batchTaskId,workTaskId:item.id})
 f.hooks.unmount();gate.resolve({executionId:'9223372036854775807'});await pending
 assert.equal(f.routes.length,0);assert.equal(f.messages.length,0)
 if(stage==='directory')assert.equal(opens.length,0)
 f.scope.stop()
})

test('module Long bounds/canonical values and unsafe formal response identities reject before routing',async()=>{
 const longs={exports:{}};Function('module','exports',compile(read('src/utils/exactJavaLong.ts')))(longs,longs.exports)
 for(const value of ['9223372036854775808','09223372036854775807','01','+1','1.0',0,9007199254740992])assert.throws(()=>longs.exports.requireExactJavaLong(value,'编号'))
 assert.equal(longs.exports.requireExactJavaLong('9223372036854775807','编号'),'9223372036854775807')
 for(const field of ['taskId','executionId','workTaskId','queryId']){
  const module={exports:{}},routes=[]
  const opened={executionId:'9',taskId:'7',workTaskId:'6',executionPageQuery:{formSlotType:'MAIN'}}
  if(field==='queryId')opened.executionPageQuery.batchTaskId=9007199254740992;else opened[field]=9007199254740992
  Function('require','module','exports',compile(read('src/utils/edhrWorkTaskNavigation.ts')))(id=>id==='@/utils/exactJavaLong'?longs.exports:{openEdhrBatchTask:async()=>opened},module,module.exports)
  await assert.rejects(module.exports.navigateToEdhrWorkTask({push:async r=>routes.push(r)},{id:'6',taskType:'FILL',batchExecutionId:'8',batchTaskId:'7'},'https://example.test'),/精确标识/)
  assert.equal(routes.length,0)
 }
})

test('actual Menu/store focus and visibility refresh without page; page takes sole listener ownership',async()=>{
 const f=fixture(),menu=f.mountMenu();await tick();assert.equal(f.badge.todoTotal,3)
 const gate=deferred();let once=true
 f.setTransport((r,next)=>{if(once&&r.url.endsWith('/count')){once=false;return gate.promise.then(()=>next(r))}return next(r)})
 f.hooks.focus();await tick();assert.equal(f.badge.todoTotal,undefined);assert.equal(f.badge.loading,true)
 f.hooks.visibilitychange();await tick();assert.equal(f.badge.todoTotal,3)
 gate.resolve();await tick();assert.equal(f.badge.todoTotal,3);assert.equal(f.badge.loading,false)
 f.hooks.mount();await tick();const before=f.requests.length
 assert.equal(f.listeners.get('focus').size,1);assert.equal(f.listeners.get('visibilitychange').size,1)
 f.hooks.focus();await tick();const fresh=f.requests.slice(before)
 assert.equal(fresh.filter(r=>r.url.endsWith('/page')).length,1);assert.equal(fresh.filter(r=>r.url.endsWith('/count')).length,0)
 f.hooks.unmount();const beforeMenu=f.requests.length;f.hooks.visibilitychange();await tick()
 assert.equal(f.requests.slice(beforeMenu).filter(r=>r.url.endsWith('/count')).length,1)
 menu.reportProfileWorkbenchTodoBadgeError({code:1002034008,message:'secret-queryVersion',config:{headers:{Authorization:'secret-auth'}}})
 menu.reportProfileWorkbenchTodoBadgeError({message:'secret-queryVersion'})
 assert.equal(JSON.stringify(menu.logs).includes('secret'),false);assert.equal(menu.logs[0][1],1002034008)
 menu.unmount();assert.equal(f.listeners.get('focus').size,0);f.scope.stop()
})

test('actual request and response rejection diagnostics redact workbench config/token and preserve rejection',async()=>{
 const ts=require('typescript'),source=read('src/config/axios/service.ts'),ast=ts.createSourceFile('service.ts',source,ts.ScriptTarget.Latest,true)
 const callbacks=[];const walk=node=>{if(ts.isCallExpression(node)&&['service.interceptors.request.use','service.interceptors.response.use'].includes(node.expression.getText(ast)))callbacks.push(node.arguments[1].getText(ast));ts.forEachChild(node,walk)};walk(ast)
 for(const callback of callbacks){const logs=[],error={message:'Network Error',config:{skipAuthRefreshReplay:true,ignoreErrorMessage:true,params:{queryVersion:'secret-token'},headers:{Authorization:'secret-auth'}}}
  const handler=Function('console','useI18n','ElMessage','assertRequestContextCurrent','return '+compile('const handler='+callback).replace(/^const handler = /,'').replace(/;\s*$/,''))({error:(...args)=>logs.push(args),log:(...args)=>logs.push(args)},()=>({t:x=>x}),{error:()=>assert.fail()},actualRequestContextGuard(ts,ast))
  await assert.rejects(handler(error),e=>e===error);assert.equal(JSON.stringify(logs).includes('secret'),false);assert.equal(logs.length,1)
 }
})


test('page discovering changed directory explicitly asks reload, next user read initializes fresh context',async()=>{
 const f=fixture();await f.page.loadWorkbench()
 const before=f.requests.filter(r=>r.url.endsWith('/context')).length
 f.setTransport((r,next)=>r.url.endsWith('/sources')?{...dir,sources:[]}:next(r))
 assert.equal(await f.page.loadWorkbench(),false)
 assert.equal(f.requests.filter(r=>r.url.endsWith('/context')).length,before)
 assert.match(f.page.loadErrorMessages.value.join('；'),/授权来源已变化.*刷新/)
 assert.equal(f.page.loading.value,false);assert.equal(f.badge.todoTotal,undefined)
 assert.equal(await f.page.loadWorkbench(),true)
 assert.equal(f.requests.filter(r=>r.url.endsWith('/context')).length,before+1)
 assert.equal(f.page.total.value,0);assert.equal(f.badge.todoTotal,0);assert.deepEqual(f.page.loadErrorMessages.value,[]);f.scope.stop()
})
