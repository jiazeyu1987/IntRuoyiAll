const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const presentation = {}
Function('exports', ts.transpileModule(fs.readFileSync(path.join(root, 'src/utils/businessMessagePresentation.ts'), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText)(presentation)
const flush = async () => { await Promise.resolve(); await vue.nextTick(); await Promise.resolve() }
const deferred = () => { let resolve, reject; const promise = new Promise((ok, bad) => { resolve = ok; reject = bad }); return { promise, resolve, reject } }
function script(file, bindings, names) {
  bindings = { ...presentation, ...bindings }
  const descriptor = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
  const compiled = compileTemplate({ source: descriptor.template.content, filename: file, id: 'handoff-behavior' })
  assert.deepEqual(compiled.errors, [], '实际 SFC 模板须可编译')
  const ast = ts.createSourceFile('actual.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const body = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  return Function(...Object.keys(bindings), code + '; return {' + names.join(',') + '};')(...Object.values(bindings))
}
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
const freezeTask=()=>({...voidTask(),taskType:'NCR_FREEZE_NOTICE',notificationOnly:true,reason:'NCR NCR-001 已冻结工单/批次，暂停受影响生产、检验和放行，等待QA正式处置：尺寸异常',actionUrl:voidTask().actionUrl.replace('QA_DECISION_HANDOFF','NCR_FREEZE_NOTICE')})
test('正式冻结通知可解析并精确打开本人通知只读记录',async()=>{
 const task=freezeTask(),nav=navigation({task,current:true,processable:false});let pushed
 const target=nav.resolveActiveOrderHandoffTarget(params(task));assert.equal(target.query.reviewId,'229')
 await nav.navigateToActiveOrderHandoff({push:async value=>pushed=value},target)
 assert.equal(pushed.path,'/user/profile');assert.equal(pushed.query.handoffReadOnly,'1')
 const p=panel({handoffNavigationContext:async()=>({task,current:true,processable:false})},{query:pushed.query})
 await p.openVoidResult();assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.id,taskId)
})
test('冻结通知拒绝办理状态、伪造通知属性及非法路径和参数',async()=>{
 const task=freezeTask(),nav=navigation(null)
 for(const actionUrl of [task.actionUrl.replace('/user/profile','/mes/pro/process-pool/production-leader'),task.actionUrl.replace('reviewId=229','eventId=229'),task.actionUrl.replace('tab=notifyMessage','tab=workbench'),task.actionUrl+'&reviewId=229']) assert.throws(()=>nav.resolveActiveOrderHandoffTarget({...params(task),actionUrl}))
 for(const change of [{status:'TODO'},{notificationOnly:false}]){
  const invalid={...task,...change},n=navigation({task:invalid,current:true,processable:false});let pushed=0
  await assert.rejects(()=>n.navigateToActiveOrderHandoff({push:async()=>pushed++},n.resolveActiveOrderHandoffTarget(params(invalid))),/正式只读/);assert.equal(pushed,0)
 }
})
function panel(api, route={query:{}}, navigationContext=null, lifecycle={}){route.path ??= '/user/profile';const nav=navigation(navigationContext), unmount=[];const values=script('src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue',{defineExpose:()=>{},defineEmits:()=>()=>{},defineProps:()=>({}),withDefaults:(props,defaults)=>({...defaults,...props}),nextTick:vue.nextTick,ref:vue.ref,computed:vue.computed,watch:()=>{},onMounted:()=>{},onActivated:()=>{},onDeactivated:()=>{},onBeforeUnmount:fn=>unmount.push(fn),api,useRouter:()=>({push:async()=>{}}),useRoute:()=>route,resolveActiveOrderHandoffTarget:nav.resolveActiveOrderHandoffTarget,navigateToActiveOrderHandoff:nav.navigateToActiveOrderHandoff,isClosedHandoffCompletion:nav.isClosedHandoffCompletion,...lifecycle},['openTask','tasks','page','PAGE_SIZE','pagedTasks','changePage','loading','error','resultError','load','showReceipts','receipts','receiptError','retryReason','retryable','retry','resultOpen','result','openVoidResult','completedResultOpen','completedResult','completedResultError','completedResultLoading','completionSnapshot','loadHandoffResult','closeCompletedResult','completedResultVisibilityChanged']);return{...values,unmount:()=>unmount.forEach(fn=>fn())}}

const dispositionNotice = () => ({ ...freezeTask(), taskType: 'NCR_DISPOSITION_NOTICE', completionSourceId: '229',
  reason: 'NCR NCR-001 QA处置结果：让步放行；让步不代替PQC生产放行或上市批准。处置意见：材料已核实',
  actionUrl: freezeTask().actionUrl.replace('NCR_FREEZE_NOTICE', 'NCR_DISPOSITION_NOTICE') })
test('QA结果告知在原周期当前或关闭后均打开同一正式只读结果', async () => {
 for (const current of [true, false]) {
  const task=dispositionNotice(),context={task,current,processable:false},nav=navigation(context);let pushed
  await nav.navigateToActiveOrderHandoff({push:async value=>pushed=value},nav.resolveActiveOrderHandoffTarget(params(task)))
  assert.equal(pushed.path,'/user/profile');assert.equal(pushed.query.reviewId,'229');assert.equal(pushed.query.handoffReadOnly,'1')
  const p=panel({handoffNavigationContext:async()=>context},{query:pushed.query});await p.openVoidResult()
  assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.taskType,'NCR_DISPOSITION_NOTICE')
  const described=presentation.describeHandoffMessage(task.taskType,task.reason)
  assert.equal(described.title,'QA处置结果通知');assert.match(described.body,/让步不代替PQC生产放行或上市批准/)
 }
})
test('QA结果告知拒绝执行待办、伪造身份与业务办理路径', async () => {
 const task=dispositionNotice(),nav=navigation(null)
 for(const actionUrl of [task.actionUrl.replace('/user/profile','/mes/pro/process-pool/production-leader'),task.actionUrl.replace('reviewId=229','eventId=229'),task.actionUrl+'&reviewId=229'])
  assert.throws(()=>nav.resolveActiveOrderHandoffTarget({...params(task),actionUrl}))
 for(const change of [{status:'TODO'},{notificationOnly:false},{completionSourceId:undefined}]){
  const invalid={...task,...change},n=navigation({task:invalid,current:false,processable:false});let pushes=0
  await assert.rejects(()=>n.navigateToActiveOrderHandoff({push:async()=>pushes++},n.resolveActiveOrderHandoffTarget(params(invalid))),/正式只读/)
  assert.equal(pushes,0)
 }
 const context={task,current:true,processable:false}
 const query={...nav.resolveActiveOrderHandoffTarget(params(task)).query,handoffReadOnly:'1',reviewId:'230'}
 const p=panel({handoffNavigationContext:async()=>context},{query});await p.openVoidResult()
 assert.equal(p.resultOpen.value,false);assert.match(p.resultError.value,/入口与原任务/)
})
const handoffRows = count => Array.from({length:count},(_,index)=>({
 id:(2100000000000000000n+BigInt(index)).toString(),activeOrderId:index<76?'422':'418',
 taskType:'PQC_HANDOFF',status:index<76?'TODO':index%2?'DONE':'CANCELED',
 sourceId:String(300000+index),roundId:'0',reason:'原自然顺序 '+index,actionUrl:'/original/'+index
}))
test('4866条原交接固定100条分49页，顺序、长整数身份和DONE/CANCELED全部保留',async()=>{
 const rows=handoffRows(4866),p=panel({myHandoffs:async()=>rows});await p.load()
 assert.equal(p.PAGE_SIZE,100);assert.deepEqual(p.tasks.value,rows);assert.equal(p.pagedTasks.value.length,100)
 assert.equal(p.pagedTasks.value.filter(row=>row.activeOrderId==='422').length,76)
 const ids=[]
 for(let page=1;page<=49;page++){
  p.changePage(page);assert.equal(p.pagedTasks.value.length,page===49?66:100)
  p.pagedTasks.value.forEach((row,index)=>assert.equal(row,p.tasks.value[(page-1)*100+index]))
  ids.push(...p.pagedTasks.value.map(row=>row.id))
 }
 assert.deepEqual(ids,rows.map(row=>row.id));assert.equal(new Set(ids).size,4866)
 assert.equal(typeof ids[0],'string');assert.equal(ids[0],'2100000000000000000')
 assert.ok(p.tasks.value.some(row=>row.status==='DONE'));assert.ok(p.tasks.value.some(row=>row.status==='CANCELED'))
})
test('刷新立即回第一页，返回条数缩小时夹限有效页，错误清空并显示原失败',async()=>{
 let next=handoffRows(4866);const p=panel({myHandoffs:()=>next instanceof Error?Promise.reject(next):Promise.resolve(next)})
 await p.load();p.changePage(100);assert.equal(p.page.value,49);p.changePage(0);assert.equal(p.page.value,1)
 p.changePage(49);const pending=deferred();p.tasks.value=handoffRows(4866);next=pending.promise
 const request=p.load();assert.equal(p.page.value,1);assert.equal(p.loading.value,true)
 p.changePage(49);pending.resolve(handoffRows(101));await request
 assert.equal(p.page.value,2);assert.equal(p.pagedTasks.value.length,1);assert.equal(p.loading.value,false)
 next=new Error('formal list failed');await p.load();assert.equal(p.page.value,1);assert.deepEqual(p.tasks.value,[])
 assert.deepEqual(p.pagedTasks.value,[]);assert.equal(p.error.value,'formal list failed');assert.equal(p.loading.value,false)
 next=[];await p.load();p.changePage(20);assert.equal(p.page.value,1);assert.deepEqual(p.pagedTasks.value,[]);assert.equal(p.error.value,'')
})
test('旧刷新迟到成功或失败都不能覆盖新列表、页码、错误或loading',async()=>{
 for(const rejected of [false,true]){
  const a=deferred(),b=deferred();let calls=0;const p=panel({myHandoffs:()=>++calls===1?a.promise:b.promise})
  const first=p.load(),second=p.load();b.resolve(handoffRows(101));await second;p.changePage(2)
  if(rejected)a.reject(new Error('late old failure'));else a.resolve(handoffRows(4866))
  await first;assert.equal(p.tasks.value.length,101);assert.equal(p.page.value,2);assert.equal(p.pagedTasks.value.length,1)
  assert.equal(p.error.value,'');assert.equal(p.loading.value,false)
 }
})
test('卸载失效列表请求，后页原任务回执仍精确读取原字符串ID',async()=>{
 const rows=handoffRows(4866),reads=[],p=panel({myHandoffs:async()=>rows,handoffReceipts:async id=>{reads.push(id);return[]}})
 await p.load();p.changePage(49);await p.showReceipts(p.pagedTasks.value[65]);assert.deepEqual(reads,[rows[4865].id]);assert.deepEqual(p.tasks.value,rows)
 for(const rejected of [false,true]){
  const late=deferred(),q=panel({myHandoffs:()=>late.promise});const request=q.load();q.unmount()
  if(rejected)late.reject(new Error('after unmount'));else late.resolve(rows)
  await request;assert.deepEqual(q.tasks.value,[]);assert.deepEqual(q.pagedTasks.value,[]);assert.equal(q.error.value,'')
 }
})
test('真实回执失败可见，重试只发送准确本人通知版本及原因',async()=>{
 let writes=[];let failed=true;const task={...returnTask(),taskType:'PRODUCTION_HANDOFF',notificationDeliveryEnabled:true,notificationReceiptAvailable:true};const p=panel({handoffReceipts:async()=>[{id:'44',status:failed?'FAILED':'SENT',rowVersion:2,attemptCount:1,lastErrorSummary:'ServiceException'}],retryHandoff:async data=>{writes.push(data);failed=false}})
 await p.showReceipts(task);assert.equal(p.receipts.value[0].status,'FAILED');assert.equal(p.retryable.value,true);await p.retry();assert.equal(writes.length,0);p.retryReason.value='恢复本人投递';await p.retry();assert.deepEqual(writes,[{id:'44',rowVersion:2,reason:'恢复本人投递'}]);assert.equal(p.receipts.value[0].status,'SENT');assert.equal(task.status,'TODO');assert.equal(p.retryable.value,false)
})
test('作废面板展示原任务冻结结果和完成者，改周期入口显式失效',async()=>{
 const task=voidTask(),nav=navigation(null),query={...nav.resolveActiveOrderHandoffTarget(params(task)).query,handoffReadOnly:'1'};const route={query};const p=panel({handoffNavigationContext:async()=>({task,current:false,processable:false})},route)
 await p.openVoidResult();assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.completedBy,'345');assert.equal(p.result.value.reason,task.reason);route.query={...query,activeOrderId:'414'};await p.openVoidResult();assert.equal(p.resultOpen.value,false);assert.equal(p.result.value,undefined);assert.match(p.resultError.value,/原任务、周期或轮次/)
})
test('回执和列表读失败保留明确错误，取消周期无重试入口',async()=>{
 const p=panel({myHandoffs:async()=>{throw Error('formal list failed')},handoffReceipts:async()=>{throw Error('formal receipt failed')}});await p.load();assert.equal(p.error.value,'formal list failed');await p.showReceipts({...returnTask(),status:'CANCELED'});assert.equal(p.receiptError.value,'formal receipt failed');assert.equal(p.retryable.value,false)
})

test('停发任务无回执时隐藏按钮，已有历史回执或仍发送通知时保留入口',()=>{
 const descriptor=parse(fs.readFileSync(path.join(root,'src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue'),'utf8')).descriptor
 const find=node=>node.tag==='el-button'&&node.children?.some(child=>child.content==='通知回执')?node:(node.children||[]).map(find).find(Boolean)
 const button=find(descriptor.template.ast);assert.ok(button)
 const compiled=compileTemplate({source:button.loc.source,filename:'receipt-button.vue',id:'receipt-button',compilerOptions:{mode:'function',prefixIdentifiers:false,cacheHandlers:false}})
 assert.deepEqual(compiled.errors,[])
 const render=Function('Vue',compiled.code)({...vue,resolveComponent:()=> 'receipt-button'})
 for(const row of [
  {notificationDeliveryEnabled:false,notificationReceiptAvailable:false},
  {notificationDeliveryEnabled:false,notificationReceiptAvailable:true},
  {notificationDeliveryEnabled:true,notificationReceiptAvailable:true}
 ])assert.equal(render({row,showReceipts(){}},[]).type,row.notificationReceiptAvailable?'receipt-button':vue.Comment)
})

test('停发任务历史失败回执可读，但不能从页面触发重试',async()=>{
 const task={...returnTask(),notificationDeliveryEnabled:false,notificationReceiptAvailable:true}
 const p=panel({handoffReceipts:async()=>[{id:'44',status:'FAILED',rowVersion:2}],retryHandoff:async()=>assert.fail('停发任务禁止重试')})
 await p.showReceipts(task);assert.equal(p.receipts.value[0].id,'44');assert.equal(p.retryable.value,false)
 p.retryReason.value='不应发送';await p.retry();assert.equal(task.status,'TODO')
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

function notifyNavigation(){
 const cache=new Map(),origin='http://localhost:8081'
 function load(file){
  if(cache.has(file))return cache.get(file).exports
  const module={exports:{}};cache.set(file,module)
  const code=ts.transpileModule(fs.readFileSync(path.join(root,file),'utf8'),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText
  const requireSource=name=>{
   if(name==='@/store/modules/approvalCenterInvalidation')return{
    useApprovalCenterInvalidationStore:()=>{throw Error('Approval session is outside this notification target test')}
   }
   if(name.startsWith('@/utils/'))return load('src/'+name.slice(2)+'.ts')
   if(name.startsWith('./'))return load(path.posix.normalize(path.posix.join(path.posix.dirname(file),name+'.ts')))
   if(name==='@/api/mes/pro/handoff'||name==='@/api/mes/pro/edhr/batchExecution')return{}
   throw Error('Unexpected source dependency: '+name)
  }
  Function('exports','module','require','window',code)(module.exports,module,requireSource,{location:{origin}})
  return module.exports
 }
 return load('src/utils/notifyMessageNavigation.ts')
}
test('实际通知目标过滤保留四类普通来源及原有顺序',()=>{
 const nav=notifyNavigation(),cases=[
  [{followupUrl:'/dcc/controlled-file/detail/71?viewer=1&from=notification'},{type:'dccPublication',label:'查看发布文件',targetId:'71'}],
  [{notifyTargetType:'PRODUCT',notifyTargetId:72,notifyChangeRequestId:73,notifyOpen:'approval'},{type:'showroomProduct',label:'查看关联产品',targetId:72,changeRequestId:73,notifyOpen:'approval'}],
  [{detailUrl:'/bpm/process-instance/detail?id=74'},{type:'bpmApproval',label:'去审批',query:{id:'74'}}],
  [{actionUrl:'/mes/pro/feedback/edhr-execution/detail?id=75',workTaskId:76},{type:'edhrWorkTask',label:'处理批记录任务',actionUrl:'/mes/pro/feedback/edhr-execution/detail?id=75&workTaskId=76',path:'/mes/pro/feedback/edhr-execution/detail',query:{id:'75',workTaskId:'76'}}]
 ]
 for(const [templateParams,target]of cases)assert.deepEqual(nav.getNotifyMessageTargets({templateParams}),[target])
 assert.deepEqual(nav.getNotifyMessageTargets({templateParams:Object.assign({},...cases.map(([p])=>p))}),cases.map(([,t])=>t))
 for(const message of [undefined,null,{}, {templateParams:[]},{templateParams:{}},{templateParams:{notifyTargetType:'OTHER',detailUrl:'/wrong',actionUrl:'https://outside.example/mes/pro/feedback/edhr-execution/detail',followupUrl:'/dcc/controlled-file/detail/71?viewer=0&from=notification'}}])assert.deepEqual(nav.getNotifyMessageTargets(message),[])
})
test('实际通知过滤保留独立精确交接，非法交接不会降级普通目标',()=>{
 const nav=notifyNavigation(),formal=params(returnTask()),ordinary={notifyTargetType:'PRODUCT',notifyTargetId:72,detailUrl:'/bpm/process-instance/detail?id=74',followupUrl:'/dcc/controlled-file/detail/71?viewer=1&from=notification'}
 assert.deepEqual(nav.getNotifyMessageTargets({templateParams:{...ordinary,...formal}}),[navigation(null).resolveActiveOrderHandoffTarget(formal)])
 for(const change of [{handoffType:undefined},{activeOrderId:'414'},{actionUrl:formal.actionUrl+'&roundId=700'},{actionUrl:'https://outside.example'+formal.actionUrl}])assert.throws(()=>nav.getNotifyMessageTargets({templateParams:{...ordinary,...formal,...change}}))
})

for (const outcome of ['返工', '作废']) test('同一路由关闭后仍可再次打开QA'+outcome+'只读通知', async () => {
 const task={...dispositionNotice(),reason:'QA处置结果：'+outcome+'；原周期不得继续生产'},context={task,current:false,processable:false}
 const nav=navigation(context),target=nav.resolveActiveOrderHandoffTarget(params(task))
 const query={...target.query,handoffReadOnly:'1'},route={path:'/user/profile',fullPath:'/user/profile?'+new URLSearchParams(query),query}
 const p=panel({handoffNavigationContext:async()=>context},route,context)
 await p.openTask(task);assert.equal(p.error.value,'');assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.id,task.id)
 p.resultOpen.value=false
 await p.openTask(task);assert.equal(p.error.value,'');assert.equal(p.resultOpen.value,true);assert.equal(p.result.value.reason,task.reason)
 assert.equal(route.fullPath,'/user/profile?'+new URLSearchParams(query))
})

// 使用 Vue 本身的 KeepAlive、调度器与生命周期运行实际 SFC script，验证缓存页面的归属。
function cachedPanel(api, route) {
 const node = type => ({ type, children: [], parent: null })
 const renderer = vue.createRenderer({
  createElement: node, createText: node, createComment: node,
  insert(child, parent, anchor) {
   if (child.parent) child.parent.children.splice(child.parent.children.indexOf(child), 1)
   const index = anchor ? parent.children.indexOf(anchor) : -1
   parent.children.splice(index < 0 ? parent.children.length : index, 0, child)
   child.parent = parent
  },
  remove(child) { if (child.parent) child.parent.children.splice(child.parent.children.indexOf(child), 1); child.parent = null },
  setText() {}, setElementText() {}, patchProp() {},
  parentNode: child => child.parent,
  nextSibling: child => child.parent?.children[child.parent.children.indexOf(child) + 1] || null
 })
 let instance
 const visible = vue.ref(true)
 const Profile = { name: 'Profile', setup() {
  instance = panel(api, route, null, {
   watch: vue.watch, onMounted: vue.onMounted, onBeforeUnmount: vue.onBeforeUnmount,
   onActivated: vue.onActivated, onDeactivated: vue.onDeactivated,
   defineProps: () => ({ initialTasks: [] })
  })
  return () => vue.h('section')
 } }
 const Other = { name: 'FormalHandoffPage', render: () => vue.h('main') }
 const app = renderer.createApp({ render: () => vue.h(vue.KeepAlive, null, {
  default: () => vue.h(visible.value ? Profile : Other)
 }) })
 app.mount(node('root'))
 return { get panel() { return instance }, visible, unmount: () => app.unmount() }
}

test('真实KeepAlive：正式办理页及其query变化不会被缓存个人中心校验，返回个人中心才恢复原通知', async () => {
 const task = dispositionNotice(), context = { task, current: true, processable: false }
 const nav = navigation(null), query = { ...nav.resolveActiveOrderHandoffTarget(params(task)).query, handoffReadOnly: '1' }
 const route = vue.reactive({ path: '/user/profile', query: {} })
 let calls = 0
 const mounted = cachedPanel({ handoffNavigationContext: async () => { calls++; return context } }, route)
 try {
  await flush(); assert.equal(calls, 0)
  // Vue Router 先更新全局路由，后完成缓存组件的失活；这段窗口也不可误读。
  route.path = '/mes/pro/process-pool/production-leader'
  route.query = navigation(null).resolveActiveOrderHandoffTarget(params(returnTask())).query
  await flush(); assert.equal(calls, 0); assert.equal(mounted.panel.resultError.value, '')
  mounted.visible.value = false; await flush()
  route.query = { ...query }; await flush(); assert.equal(calls, 0)
  // query 未变化，仅页面路径变化；重新激活必须重新校验。
  route.path = '/user/profile'; await flush(); assert.equal(calls, 0)
  mounted.visible.value = true; await flush()
  assert.equal(calls, 1); assert.equal(mounted.panel.resultOpen.value, true)
  assert.equal(mounted.panel.result.value.id, task.id); assert.equal(mounted.panel.resultError.value, '')
 } finally { mounted.unmount() }
})

for (const completed of [false, true]) for (const rejected of [false, true])
 test(`真实KeepAlive：${completed ? '完成记录' : '只读通知'}失活后迟到${rejected ? '错误' : '成功'}无权写入，重新激活重读`, async () => {
 const task = completed ? completedTask() : voidTask()
 const context = completedContext(task)
 const query = completed ? completedQuery(task)
  : { ...navigation(null).resolveActiveOrderHandoffTarget(params(task)).query, handoffReadOnly: '1' }
 const route = vue.reactive({ path: '/user/profile', query })
 const pending = deferred(); let calls = 0
 const mounted = cachedPanel({ handoffNavigationContext: () => ++calls === 1 ? pending.promise : Promise.resolve(context) }, route)
 try {
  await flush(); assert.equal(calls, 1)
  // 保持相同路由，只由 KeepAlive 失活，验证生命周期本身能够撤销请求。
  mounted.visible.value = false; await flush()
  if (rejected) pending.reject(Error('已离开页面的旧请求失败'))
  else pending.resolve(context)
  await flush()
  assert.equal(mounted.panel.resultOpen.value, false); assert.equal(mounted.panel.completedResultOpen.value, false)
  assert.equal(mounted.panel.result.value, undefined); assert.equal(mounted.panel.completedResult.value, undefined)
  assert.equal(mounted.panel.resultError.value, ''); assert.equal(mounted.panel.completedResultError.value, '')
  assert.equal(mounted.panel.completedResultLoading.value, false)
  mounted.visible.value = true; await flush()
  assert.equal(calls, 2)
  assert.equal(completed ? mounted.panel.completedResultOpen.value : mounted.panel.resultOpen.value, true)
  assert.equal((completed ? mounted.panel.completedResult.value : mounted.panel.result.value).id, task.id)
 } finally { mounted.unmount() }
})

test('真实route watch：身份篡改必须报错，修正入口撤销旧结果错误且保留列表失败', async () => {
 const task = voidTask(), context = completedContext(task)
 const query = { ...navigation(null).resolveActiveOrderHandoffTarget(params(task)).query, handoffReadOnly: '1' }
 const route = vue.reactive({ path: '/user/profile', query: { ...query, roundId: '999' } })
 const mounted = cachedPanel({ handoffNavigationContext: async () => context }, route)
 try {
  await flush(); assert.match(mounted.panel.resultError.value, /原任务、周期或轮次/)
  assert.equal(mounted.panel.resultOpen.value, false)
  mounted.panel.error.value = '列表读取失败'
  route.query = query; await flush()
  assert.equal(mounted.panel.resultError.value, ''); assert.equal(mounted.panel.error.value, '列表读取失败')
  assert.equal(mounted.panel.resultOpen.value, true)
  // 只改变path，缓存组件立即关闭自己的弹窗，也不会校验正式办理参数。
  route.path = '/mes/pro/process-pool/production-leader'
  assert.equal(mounted.panel.resultOpen.value, false); assert.equal(mounted.panel.result.value, undefined)
 } finally { mounted.unmount() }
})
