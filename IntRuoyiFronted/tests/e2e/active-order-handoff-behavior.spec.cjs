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
function panel(api, route={query:{}}){const nav=navigation(null);return script('src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue',{ref:vue.ref,computed:vue.computed,watch:()=>{},onMounted:()=>{},api,useRouter:()=>({push:async()=>{}}),useRoute:()=>route,resolveActiveOrderHandoffTarget:nav.resolveActiveOrderHandoffTarget,navigateToActiveOrderHandoff:nav.navigateToActiveOrderHandoff},['tasks','error','load','showReceipts','receipts','receiptError','retryReason','retryable','retry','resultOpen','result','openVoidResult'])}
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
