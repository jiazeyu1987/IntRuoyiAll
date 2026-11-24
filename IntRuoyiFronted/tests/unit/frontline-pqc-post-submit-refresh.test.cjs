const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { reactive, ref, watch } = require('vue')
const source = parse(fs.readFileSync('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue','utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('panel.ts',source,ts.ScriptTarget.Latest,true)
const stmt = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast)==='handleConfirmPqcSubmit'))
const code = ts.transpileModule(stmt.getText(ast),{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText
function harness() {
 let resolve, reject
 const errors=[], accepted=[], routes=[]
 const state=reactive({selectedActiveOrder:{activeOrderId:1},selectedProcess:{regulationVersionId:3,qaProcessId:4,pqcTaskOptions:[{pqcTaskId:7,taskStatus:'PENDING'}]}})
 const route=reactive({fullPath:'/pqc?returnTaskId=7',path:'/pqc',query:{returnTaskId:7,handoffType:'PQC_RETURN'}})
 const ref=value=>({value}), noop=()=>{}
 const d={hasReturnNotification:ref(false),isInitialHandoffBlocked:ref(false),payloadLoading:ref(false),pqcSubmitResultUncertain:ref(false),pqcSignatureCredentialType:ref('PIN'),pqcSignaturePassword:ref('888888'),pqcActionInProgress:ref(false),
  approvalInvalidation:{syncSession:()=>({key:'s'}),captureHandoffSourceSuccess:()=>({session:{key:'s'}}),publishEdhrSuccess:noop},
 assertPqcFormalSubmissionReady:noop,assertPqcSignatureAndQuantityReady:noop,assertPqcCurrentProcessAllMethodSubmissionReady:noop,cloneDeep:structuredClone,buildPqcInspectionSubmitPayloads:()=>[{pqcTaskId:7,activeOrderId:1}],pqcMethodKey:()=> 'm',pqcMethodWrites:new Set(),pqcMethodReceipts:new Map(),pqcUncertainMethods:new Set(),deviceState:state,isFrontlinePqcProcess:()=>true,currentLoginUserId:ref(2),clearFrontlineError:noop,showFrontlineError:e=>errors.push(String(e)),resolveErrorMessage:e=>e.message,message:{success:noop},ProFeedbackApi:{submitFrontlinePqcInspection:async()=>({pqcEventId:8}),getPqcProcesses:()=>new Promise((a,b)=>{resolve=a;reject=b})},route,router:{replace:async v=>{routes.push(v)}},processSelectionRequestId:0,applyPqcTaskSnapshotToDraft:p=>accepted.push(p)}
 d.context={};d.isPqcMode=ref(true);d.activePqcTaskOptionId=ref(7);d.selectedPqcInspectionRuleKey=ref('FIRST')
 const declarations=new Map(ast.statements.filter(ts.isVariableStatement).flatMap(s=>s.declarationList.declarations.map(x=>[x.name.getText(ast),s.getText(ast)])))
 const load=(names,deps)=>new Function(...Object.keys(deps),ts.transpileModule(names.map(n=>declarations.get(n)).join('\n')+'\nreturn {'+names.join(',')+'}',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText)(...Object.values(deps))
 const guardSource=fs.readFileSync('src/store/modules/approvalCenterInvalidationState.ts','utf8')
 const guardAst=ts.createSourceFile('g.ts',guardSource,ts.ScriptTarget.Latest,true)
 const guardStmt=guardAst.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(x=>x.name.getText(guardAst)==='createFormalActionGuard'))
 const createGuard=new Function(ts.transpileModule(guardStmt.getText(guardAst).replace(/^export /,''),{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText+';return createFormalActionGuard')()
 const key=load(['pqcSubmitContextKey'],d).pqcSubmitContextKey
 const guard=createGuard(key);d.pqcSubmitGuard=guard
 watch(key,()=>guard.invalidate(),{flush:'sync'})
 const update=load(['resolvePqcTaskSummaryState','updatePqcSubmittedTasksInProcess'],{}).updatePqcSubmittedTasksInProcess
 d.resetPqcSubmissionDrafts=ids=>{state.selectedProcess=update(state.selectedProcess,new Set(ids))}
 return {run:new Function(...Object.keys(d),code+';return handleConfirmPqcSubmit')(...Object.values(d)),errors,accepted,routes,state,route,invalidate:()=>guard.invalidate(),activeTask:d.activePqcTaskOptionId,resolve:v=>resolve(v),reject:e=>reject(e),deps:d}
}
const tick=()=>new Promise(r=>setImmediate(r))
test('successful submission reads formal inspection after local reset',async()=>{const h=harness(),p=h.run();await tick();const formal={regulationVersionId:3,qaProcessId:4,pqcTaskOptions:[{pqcTaskId:7,taskStatus:'SUBMITTED',submittedInspection:{items:['formal']}}]};h.resolve([formal]);await p;assert.deepEqual(h.accepted,[formal]);assert.deepEqual(h.state.selectedProcess.pqcTaskOptions[0].submittedInspection,{items:['formal']});assert.equal(h.errors.length,0);assert.equal(h.routes.length,1)})
for(const change of ['order','task','route','leave']) test('discards refresh after '+change,async()=>{const h=harness(),p=h.run();await tick();if(change==='order')h.state.selectedActiveOrder={activeOrderId:2};if(change==='route')h.route.fullPath='/other';if(change==='task')h.activeTask.value=8;if(change==='leave')h.invalidate();h.resolve([{regulationVersionId:3,qaProcessId:4}]);await p;assert.equal(h.accepted.length,0);assert.equal(h.routes.length,0);assert.equal(h.errors.length,0)})
test('read failure states write succeeded',async()=>{const h=harness(),p=h.run();await tick();h.reject(new Error('network'));await p;assert.equal(h.errors.length,1);assert.match(h.errors[0],/提交已成功/);assert.match(h.errors[0],/network/)})
test('session changes discard old refresh',async()=>{const h=harness(),p=h.run();await tick();h.deps.approvalInvalidation.syncSession=()=>({key:'new'});h.resolve([{regulationVersionId:3,qaProcessId:4}]);await p;assert.equal(h.accepted.length,0)})


test('synchronous formal-data application failure is reported after context changes',async()=>{
 const h=harness();h.deps.applyPqcTaskSnapshotToDraft=()=>{throw new Error('apply failed')}
 const run=new Function(...Object.keys(h.deps),code+';return handleConfirmPqcSubmit')(...Object.values(h.deps))
 const p=run();await tick();h.resolve([{regulationVersionId:3,qaProcessId:4,pqcTaskOptions:[{pqcTaskId:7,taskStatus:'SUBMITTED'}]}]);await p
 assert.equal(h.errors.length,1);assert.match(h.errors[0],/提交已成功/);assert.match(h.errors[0],/apply failed/);assert.equal(h.routes.length,0)
})

test('cancelled navigation reports successful submission with cleanup failure',async()=>{
 const h=harness();h.deps.router.replace=async()=>new Error('navigation cancelled')
 const p=h.run();await tick();h.resolve([{regulationVersionId:3,qaProcessId:4,pqcTaskOptions:[{pqcTaskId:7,taskStatus:'SUBMITTED'}]}]);await p
 assert.equal(h.errors.length,1);assert.match(h.errors[0],/提交已成功/);assert.match(h.errors[0],/navigation cancelled/)
})
