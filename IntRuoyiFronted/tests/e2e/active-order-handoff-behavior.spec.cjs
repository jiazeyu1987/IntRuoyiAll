const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const flush = async () => { await Promise.resolve(); await vue.nextTick(); await Promise.resolve() }
const deferred = () => { let resolve, reject; const promise = new Promise((ok, bad) => { resolve = ok; reject = bad }); return { promise, resolve, reject } }
function script(file, bindings, names) {
  const descriptor = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
  const compiled = compileTemplate({ source: descriptor.template.content, filename: file, id: 'handoff-behavior' })
  assert.deepEqual(compiled.errors, [], '实际 SFC 模板须可编译')
  const ast = ts.createSourceFile('actual.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  return Function(...Object.keys(bindings), code + '; return {' + names.join(',') + '};')(...Object.values(bindings))
}
function qa(api) {
  const messages = []
  // Explicitly drive actual async functions; lifecycle/watch are exercised by the real page integration separately.
  const values = script('src/views/mes/pro/handoff/QaHandoffAssignmentConfig.vue', { ref: vue.ref, computed: vue.computed, watch: () => {}, onMounted: () => {}, api, useMessage: () => ({ success: text => messages.push(text) }) }, ['routeId','sourceId','sourceType','reason','ruleId','userOptions','readyToSave','error','loading','save','loadRule','searchUsers'])
  return { ...values, messages }
}
const rule = (routeId, id, owner, label) => ({ id, routeId, candidateSourceType: 'USER', candidateSourceId: owner, candidateLabel: label, enabled: true })
test('真实 QA 配置加载保存保持负责人姓名与准确路线/旧版本', async () => {
  let request
  const q = qa({ qaAssignment: async () => rule(98, 10, 345, 'QA本人'), saveQaAssignment: async data => { request = data; return rule(98, 10, 345, 'QA本人') } })
  q.routeId.value = 98; await q.loadRule(); assert.deepEqual(q.userOptions.value, [{ id:345,label:'QA本人' }]); assert.equal(q.readyToSave.value, true)
  q.reason.value = '正式责任确认'; await q.save(); assert.deepEqual(request,{ routeId:98,candidateSourceType:'USER',candidateSourceId:345,enabled:true,reason:'正式责任确认',expectedRuleId:10 }); assert.deepEqual(q.messages,['QA 负责人已保存'])
})
test('A 路线迟到读取不能污染 B 路线负责人或版本', async () => {
  const a = deferred(), b = deferred(); const q = qa({ qaAssignment: route => route === 98 ? a.promise : b.promise })
  q.routeId.value = 98; const first = q.loadRule(); q.routeId.value = 99; const second = q.loadRule(); b.resolve(rule(99,11,349,'B负责人')); await second; a.resolve(rule(98,10,345,'A负责人')); await first
  assert.equal(q.ruleId.value,11); assert.equal(q.sourceId.value,349); assert.deepEqual(q.userOptions.value,[{id:349,label:'B负责人'}])
})
test('配置读取失败必须显式阻止保存', async () => {
  let writes=0; const q=qa({qaAssignment:async()=>{throw Error('formal rule load failed')},saveQaAssignment:async()=>{writes++}})
  q.routeId.value=98; await q.loadRule(); assert.equal(q.error.value,'formal rule load failed'); assert.equal(q.readyToSave.value,false); q.sourceId.value=345;q.reason.value='不能绕过读取';await q.save();assert.equal(writes,0)
})
test('A 路线保存和用户查询迟到响应不能污染 B 路线', async () => {
  const saved=deferred(), searched=deferred();const q=qa({qaAssignment:async route=>rule(route,route,route+200,'路线'+route),saveQaAssignment:()=>saved.promise,qaUserOptions:()=>searched.promise})
  q.routeId.value=98;await q.loadRule();q.reason.value='保存A';const save=q.save(), search=q.searchUsers('A');q.routeId.value=99;await q.loadRule();saved.resolve(rule(98,500,345,'迟到A'));searched.resolve([{id:345,label:'迟到A'}]);await Promise.all([save,search]);assert.equal(q.ruleId.value,99);assert.equal(q.sourceId.value,299);assert.deepEqual(q.userOptions.value,[{id:299,label:'路线99'}]);assert.deepEqual(q.messages,[])
})
function navigation(context) {
 const exports={};const module={exports};const text=fs.readFileSync(path.join(root,'src/utils/activeOrderHandoffNavigation.ts'),'utf8');const code=ts.transpileModule(text,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText
 Function('exports','module','require','window',code)(exports,module,() => ({handoffNavigationContext:async id => typeof context==='function'?context(id):context}),{location:{origin:'http://localhost:8081'}});return module.exports
}
const taskId='1900000000000000001'
const returnTask=()=>({id:taskId,activeOrderId:'413',sourceId:'176',roundId:'700',taskType:'PRODUCTION_RETURN',status:'TODO',reason:'次数需核对',actionUrl:'/mes/pro/feedback/edhr-batch-production-fill?activeOrderId=413&eventId=176&handoffTaskId='+taskId+'&roundId=700&handoffType=PRODUCTION_RETURN&returnTaskId='+taskId+'&rejectedReviewId=700'})
const params=task=>({actionUrl:task.actionUrl,handoffTaskId:task.id,activeOrderId:task.activeOrderId,handoffType:task.taskType})
test('通知精确深链保留 Snowflake 身份，拒绝重复/改轮次/异站',()=>{
 const nav=navigation(null),task=returnTask();assert.equal(nav.resolveActiveOrderHandoffTarget(params(task)).query.returnTaskId,taskId)
 for(const url of [task.actionUrl+'&roundId=700',task.actionUrl.replace('rejectedReviewId=700','rejectedReviewId=701'),'https://outside.example'+task.actionUrl])assert.throws(()=>nav.resolveActiveOrderHandoffTarget({...params(task),actionUrl:url}))
})
test('旧周期和取消通知不跳转，不投影最新工单',async()=>{
 for(const status of ['TODO','CANCELED']){const task={...returnTask(),status};const nav=navigation({task,current:false,processable:false});let pushed=0;await assert.rejects(()=>nav.navigateToActiveOrderHandoff({push:async()=>pushed++},nav.resolveActiveOrderHandoffTarget(params(task))),/旧周期/);assert.equal(pushed,0)}
})
const voidTask=()=>({id:taskId,activeOrderId:'413',sourceId:'229',roundId:'229',taskType:'QA_DECISION_HANDOFF',status:'DONE',reason:'void：正式作废意见\n【工单：EDHR-413；工序：整单；发起人：QA本人（SYSTEM_USER）；状态：QA已正式作废，结果只读】',completedBy:'345',completedAt:'2026-10-05 11:00:00',actionUrl:'/user/profile?activeOrderId=413&reviewId=229&handoffTaskId='+taskId+'&roundId=229&handoffType=QA_DECISION_HANDOFF&tab=notifyMessage'})
test('作废 DONE 在原周期关闭后跳转真实个人通知页并强制只读',async()=>{
 const task=voidTask(),nav=navigation({task,current:false,processable:false});let pushed;await nav.navigateToActiveOrderHandoff({push:async value=>pushed=value},nav.resolveActiveOrderHandoffTarget(params(task)));assert.equal(pushed.path,'/user/profile');assert.equal(pushed.query.tab,'notifyMessage');assert.equal(pushed.query.handoffReadOnly,'1');assert.equal(pushed.query.reviewId,'229')
})
function panel(api, route={query:{}}){const nav=navigation(null), unmount=[];const values=script('src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue',{ref:vue.ref,computed:vue.computed,watch:()=>{},onMounted:()=>{},onBeforeUnmount:fn=>unmount.push(fn),api,useRouter:()=>({push:async()=>{}}),useRoute:()=>route,resolveActiveOrderHandoffTarget:nav.resolveActiveOrderHandoffTarget,navigateToActiveOrderHandoff:nav.navigateToActiveOrderHandoff,isClosedHandoffCompletion:nav.isClosedHandoffCompletion},['tasks','error','load','showReceipts','receipts','receiptError','retryReason','retryable','retry','resultOpen','result','openVoidResult','completedResultOpen','completedResult','completedResultError','completedResultLoading','completionSnapshot','loadHandoffResult','closeCompletedResult','completedResultVisibilityChanged']);return{...values,unmount:()=>unmount.forEach(fn=>fn())}}
test('真实回执失败可见，重试只发送准确本人通知版本及原因',async()=>{
 let writes=[];let failed=true;const task=returnTask();const p=panel({handoffReceipts:async()=>[{id:'44',status:failed?'FAILED':'SENT',rowVersion:2,attemptCount:1,lastErrorSummary:'ServiceException'}],retryHandoff:async data=>{writes.push(data);failed=false}})
 await p.showReceipts(task);assert.equal(p.receipts.value[0].status,'FAILED');assert.equal(p.retryable.value,true);await p.retry();assert.equal(writes.length,0);p.retryReason.value='恢复本人投递';await p.retry();assert.deepEqual(writes,[{id:'44',rowVersion:2,reason:'恢复本人投递'}]);assert.equal(p.receipts.value[0].status,'SENT');assert.equal(task.status,'TODO');assert.equal(p.retryable.value,false)
})
test('作废面板展示原任务冻结结果和完成者，改周期入口显式失效',async()=>{
 const task=voidTask(),nav=navigation(null),query={...nav.resolveActiveOrderHandoffTarget(params(task)).query,handoffReadOnly:'1'};const route={query};const p=panel({handoffNavigationContext:async()=>({task,current:false,processable:false})},route)
 await p.openVoidResult();assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.completedBy,'345');assert.equal(p.result.value.reason,task.reason);route.query={...query,activeOrderId:'414'};await p.openVoidResult();assert.equal(p.resultOpen.value,false);assert.equal(p.result.value,undefined);assert.match(p.error.value,/原任务、周期或轮次/)
})
test('回执和列表读失败保留明确错误，取消周期无重试入口',async()=>{
 const p=panel({myHandoffs:async()=>{throw Error('formal list failed')},handoffReceipts:async()=>{throw Error('formal receipt failed')}});await p.load();assert.equal(p.error.value,'formal list failed');await p.showReceipts({...returnTask(),status:'CANCELED'});assert.equal(p.receiptError.value,'formal receipt failed');assert.equal(p.retryable.value,false)
})

function completedTask(type='PQC_REVIEW',sourceType='PROCESS_POOL_EVENT',roundId='176',completionSourceId='700'){
 const returned=type==='PQC_RETURN',t={id:taskId,activeOrderId:'413',workOrderId:'274',routeProcessId:'91',taskType:type,sourceType,sourceId:'176',roundId,status:'DONE',reason:'原轮次通知意见；完成来源保留原业务记录',completedBy:returned?'344':'343',completedAt:1791221585000,completionSourceId,
  actionUrl:(returned?'/mes/pro/feedback/edhr-batch-pqc-fill':'/mes/pro/process-pool/pqc-leader')+'?activeOrderId=413&eventId=176&handoffTaskId='+taskId+'&roundId='+roundId+'&handoffType='+type+(returned?'&returnTaskId='+taskId+'&rejectedReviewId='+roundId:'')}
 t.responsibilitySnapshotJson=JSON.stringify({identityDomain:'SYSTEM_USER',activeOrderId:t.activeOrderId,workOrderId:t.workOrderId,sourceType:t.sourceType,sourceId:t.sourceId,roundId:t.roundId,routeProcessId:t.routeProcessId,workOrderCode:'EDHR-413',processName:'原冻结工序'})
 return t
}
const completedContext=task=>({task,current:false,processable:false,profileLeaderCorrection:false})
function completedQuery(task){return{...navigation(null).resolveActiveOrderHandoffTarget(params(task)).query,tab:'notifyMessage',handoffReadOnly:'1',handoffResult:'completed'}}
test('已归档普通DONE保留所有原轮次参数进入独立完成记录，不进入办理页',async()=>{
 for(const task of [completedTask(),completedTask('PQC_REVIEW','PROCESS_POOL_EVENT_SIGNED_REVISION','800','701'),completedTask('PQC_RETURN','PROCESS_POOL_EVENT','700','800')]){
  const nav=navigation(completedContext(task));let pushed;await nav.navigateToActiveOrderHandoff({push:async value=>pushed=value},nav.resolveActiveOrderHandoffTarget(params(task)))
  assert.deepEqual(pushed,{path:'/user/profile',query:completedQuery(task)});assert.equal(task.status,'DONE')
 }
})
test('归档TODO/CANCELED和缺完成字段的DONE不能进入完成记录',async()=>{
 for(const change of [{status:'TODO'},{status:'CANCELED'},{completedBy:undefined},{completedBy:0},{completedAt:undefined},{completedAt:'invalid'},{completionSourceId:undefined},{completionSourceId:0}]){
  const task={...completedTask(),...change},nav=navigation(completedContext(task));let pushed=0
  await assert.rejects(()=>nav.navigateToActiveOrderHandoff({push:async()=>pushed++},nav.resolveActiveOrderHandoffTarget(params(task))),/旧周期/);assert.equal(pushed,0)
 }
})
test('当前周期DONE继续原来源只读页，原QA作废入口保持不变',async()=>{
 const task=completedTask(),nav=navigation({...completedContext(task),current:true});let pushed
 await nav.navigateToActiveOrderHandoff({push:async value=>pushed=value},nav.resolveActiveOrderHandoffTarget(params(task)));assert.equal(pushed.path,'/mes/pro/process-pool/pqc-leader');assert.equal(pushed.query.handoffReadOnly,'1');assert.equal(pushed.query.handoffResult,undefined)
 const voidDone={...voidTask(),completedAt:1791221585000,completionSourceId:'91'},voidNav=navigation(completedContext(voidDone))
 await voidNav.navigateToActiveOrderHandoff({push:async value=>pushed=value},voidNav.resolveActiveOrderHandoffTarget(params(voidDone)));assert.equal(pushed.path,'/user/profile');assert.equal(pushed.query.handoffResult,undefined);assert.equal(pushed.query.reviewId,'229')
})
test('完成记录单独分派官方读取，不误触作废结果，不替换REJECT/批准/更正来源',async()=>{
 for(const task of [completedTask(),completedTask('PQC_REVIEW','PROCESS_POOL_EVENT_SIGNED_REVISION','800','701'),completedTask('PQC_RETURN','PROCESS_POOL_EVENT','700','800')]){
  let calls=[];const route={query:completedQuery(task)},p=panel({handoffNavigationContext:async id=>{calls.push(id);return completedContext(task)}},route)
  await p.loadHandoffResult();assert.deepEqual(calls,[taskId]);assert.equal(p.resultOpen.value,false);assert.equal(p.error.value,'');assert.equal(p.completedResultError.value,'');assert.equal(p.completedResultOpen.value,true);assert.equal(p.completedResultLoading.value,false);assert.deepEqual(p.completedResult.value,task);assert.equal(p.completedResult.value.completionSourceId,task.completionSourceId);assert.equal(p.completedResult.value.approved,undefined);assert.deepEqual(p.completionSnapshot.value,{workOrderCode:'EDHR-413',processName:'原冻结工序'})
 }
})
test('完成记录拒绝任一原周期/来源/轮次/额外query篡改并显示本弹窗错误',async()=>{
 const task=completedTask()
 for(const change of [{activeOrderId:'414'},{eventId:'177'},{roundId:'800'},{handoffTaskId:'1900000000000000002'},{handoffType:'PRODUCTION_REVIEW'},{handoffReadOnly:'0'},{tab:'other'},{handoffResult:'other'},{extra:'1'},{roundId:['176','176']}]){
  const p=panel({handoffNavigationContext:async()=>completedContext(task)},{query:{...completedQuery(task),...change}});await p.loadHandoffResult()
  assert.equal(p.completedResultOpen.value,true);assert.ok(p.completedResultError.value);assert.equal(p.completedResult.value,undefined);assert.equal(p.resultOpen.value,false);assert.equal(p.error.value,'')
 }
})
test('完成记录必须是closed不可办理且冻结工序/工单/来源完整，不猜当前配置',async()=>{
 for(const context of [{...completedContext(completedTask()),current:true},{...completedContext(completedTask()),processable:true},completedContext({...completedTask(),responsibilitySnapshotJson:'not JSON'}),completedContext({...completedTask(),responsibilitySnapshotJson:JSON.stringify({identityDomain:'SYSTEM_USER',activeOrderId:'414'})})]){
  const p=panel({handoffNavigationContext:async()=>context},{query:completedQuery(completedTask())});await p.loadHandoffResult();assert.ok(p.completedResultError.value);assert.equal(p.completedResult.value,undefined)
 }
})
test('完成记录官方GET拒绝必须显示原错误，没有成功默认值',async()=>{
 const p=panel({handoffNavigationContext:async()=>{throw Error('official own-task authorization denied')}},{query:completedQuery(completedTask())});await p.loadHandoffResult();assert.equal(p.completedResultError.value,'official own-task authorization denied');assert.equal(p.completedResult.value,undefined);assert.equal(p.completedResultLoading.value,false)
})
test('A完成记录迟到不能覆盖B，切离入口或用户关闭弹窗立即撤销读取',async()=>{
 const a=deferred(),b=deferred(),taskA=completedTask(),taskB={...completedTask('PQC_REVIEW','PROCESS_POOL_EVENT_SIGNED_REVISION','800','701'),id:'1900000000000000002'}
 taskB.actionUrl=taskB.actionUrl.replace(taskId,taskB.id)
 const route={query:completedQuery(taskA)},p=panel({handoffNavigationContext:id=>id===taskA.id?a.promise:b.promise},route)
 const first=p.loadHandoffResult();route.query=completedQuery(taskB);const second=p.loadHandoffResult();b.resolve(completedContext(taskB));await second;a.resolve(completedContext(taskA));await first;assert.equal(p.completedResult.value.id,taskB.id)
 route.query={};await p.loadHandoffResult();assert.equal(p.completedResultOpen.value,false);assert.equal(p.completedResult.value,undefined)
 const late=deferred();route.query=completedQuery(taskA);const closing=panel({handoffNavigationContext:()=>late.promise},route),request=closing.loadHandoffResult();closing.completedResultVisibilityChanged(false);late.resolve(completedContext(taskA));await request;assert.equal(closing.completedResultOpen.value,false);assert.equal(closing.completedResult.value,undefined)
})
test('卸载撤销完成和作废独立代际，迟到响应不能重新打开',async()=>{
 const completed=deferred(),task=completedTask(),p=panel({handoffNavigationContext:()=>completed.promise},{query:completedQuery(task)});const first=p.loadHandoffResult();p.unmount();completed.resolve(completedContext(task));await first;assert.equal(p.completedResultOpen.value,false);assert.equal(p.completedResult.value,undefined);assert.equal(p.completedResultError.value,'')
 const lateVoid=deferred(),voidDone=voidTask(),q=panel({handoffNavigationContext:()=>lateVoid.promise},{query:{...navigation(null).resolveActiveOrderHandoffTarget(params(voidDone)).query,handoffReadOnly:'1'}});const second=q.loadHandoffResult();q.unmount();lateVoid.resolve(completedContext(voidDone));await second;assert.equal(q.resultOpen.value,false);assert.equal(q.result.value,undefined)
})
