const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),ts=require('typescript'),vue=require('vue')
const {test}=require('node:test'),{parse,compileTemplate}=require('vue/compiler-sfc')
const filename=path.resolve(__dirname,'../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const descriptor=parse(fs.readFileSync(filename,'utf8')).descriptor
const ast=ts.createSourceFile('parent.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
function declarations(names){return ast.statements.filter(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>names.includes(d.name.getText(ast)))).map(s=>s.getText(ast)).join('\n')}
function functions(names,values,preamble=''){
 const code=ts.transpileModule(preamble+declarations(names),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
 return Function(...Object.keys(values),code+';return {'+names.join(',')+'};')(...Object.values(values))
}
test('真实父SFC隐藏普通生产/PQC填写，仅保留本人严格退回面板',()=>{
 assert.deepEqual(compileTemplate({source:descriptor.template.content,filename,id:'return-parent-isolation'}).errors,[])
 assert.match(descriptor.template.content,/v-if="isPqcMode && !hasReturnNotification"/)
 assert.match(descriptor.template.content,/v-else-if="!hasReturnNotification"[\s\S]*?data-frontline-production-stage/)
 assert.match(descriptor.template.content,/<FrontlineReturnCorrectionPanel ref="returnCorrectionPanel" :mode="props.mode"/)
 assert.match(descriptor.scriptSetup.content,/returnTaskId !== undefined[\s\S]*\.openTask\(route.query\)/)
})
test('旧、错误或改造RETURN query均启动隔离，不能作为普通填写入口',()=>{
 for(const id of ['1900000000000000001','0',null,['1900000000000000001']]){
  const page=functions(['hasReturnNotification'],{computed:vue.computed,route:{query:{returnTaskId:id}}});assert.equal(page.hasReturnNotification.value,true)
 }
 const page=functions(['hasReturnNotification'],{computed:vue.computed,route:{query:{}}});assert.equal(page.hasReturnNotification.value,false)
})
for(const pqc of [false,true])test(`实际${pqc?'PQC':'生产'}mount旧RETURN不加载或默认选择当前新周期`,async()=>{
 const calls=[],errors=[],catalog=vue.ref([])
 const values={document:{addEventListener:()=>{}},window:{addEventListener:()=>{}},syncPqcFullscreenState:()=>{},handlePqcForegroundRefresh:()=>{},scheduleProductionViewportScaleUpdate:()=>{},isPqcMode:{value:pqc},ResizeObserver:class{observe(){}},frontlinePanelRef:{value:null},hasInitialHandoffQuery:{value:false},hasReturnNotification:{value:true},hydrateContextFromRoute:()=>{throw Error('generic hydrate must not run')},FrontlineTemplateApi:{getCatalog:async()=>[{code:'formal'}]},isolateOwnReturnNotification:()=>calls.push('isolate'),catalog,initializeInitialHandoffSelection:()=>{throw Error('initial handoff must not run')},refreshPqcActiveOrdersAndEnsureSelection:()=>{throw Error('generic PQC must not run')},initializeProductionSelection:()=>{throw Error('generic production must not run')},showFrontlineError:e=>errors.push(e)}
 const mount=ast.statements.find(s=>ts.isExpressionStatement(s)&&s.expression.expression?.getText(ast)==='onMounted').expression.arguments[0].getText(ast)
 const code=ts.transpileModule('let productionViewportResizeObserver;let initialHandoffPageMounted=false;const mount='+mount,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
 await Function(...Object.keys(values),code+';return mount;')(...Object.values(values))()
 assert.deepEqual(calls,['isolate']);assert.deepEqual(errors,[]);assert.equal(catalog.value[0].code,'formal')
})
test('实际隔离方法清空既有新周期与填写上下文并终止旧选择响应',()=>{
 const state={selectedActiveOrder:{activeOrderId:414},selectedProcess:{routeProcessId:92},selectedEmployee:{userId:342},processOptions:[1],productionProcessOptions:[1],activeOrderOptions:[{activeOrderId:414}],employeeOptions:[1],runtimeConfig:{},template:{},productionActiveOrderSelectionRequestToken:1,pqcActiveOrderSelectionRequestToken:2,processSelectionRequestToken:3,employeeSwitchRequestToken:4},context={workOrderId:274,routeId:98,routeProcessId:92,processId:16,actualEmployeeId:342},calls=[]
 const values={deviceState:state,context,initialHandoffTask:{value:{id:'new'}},initialHandoffLoading:{value:true},initialHandoffInvalid:{value:false},payloadPreview:{value:{}},productionSignaturePassword:{value:'transient'},pqcSignaturePassword:{value:'transient'},pqcSignatureDialogVisible:{value:true},productionSubmitSuccessOpen:{value:true},clearPqcExecutionSelection:()=>calls.push('clearPqc'),closePicker:()=>calls.push('closePicker'),cancelProductionFormalSubmitConfirmation:()=>calls.push('cancelConfirmation')}
 const page=functions(['isolateOwnReturnNotification'],values,'let initialHandoffEpoch=0,activeOrderSelectionRequestId=0,processSelectionRequestId=0,productionEmployeeSelectionRequestId=0;')
 page.isolateOwnReturnNotification();assert.equal(state.selectedActiveOrder,undefined);assert.equal(state.selectedProcess,undefined);assert.equal(state.selectedEmployee,undefined);assert.deepEqual(state.activeOrderOptions,[]);assert.deepEqual(state.processOptions,[]);assert.equal(context.workOrderId,undefined);assert.equal(context.actualEmployeeId,undefined);assert.equal(state.productionActiveOrderSelectionRequestToken,2);assert.equal(state.pqcActiveOrderSelectionRequestToken,3);assert.equal(values.initialHandoffTask.value,undefined);assert.equal(values.productionSignaturePassword.value,'');assert.equal(values.pqcSignaturePassword.value,'');assert.deepEqual(calls,['clearPqc','closePicker','cancelConfirmation'])
})
test('实际普通提交、签名确认和订单切换入口均不能绕过RETURN隔离',async()=>{
 const names=['handleValidate','handleConfirmPqcSubmit','handleProductionFormalSubmit','handleSelectActiveOrder'],errors=[]
 const page=functions(names,{hasReturnNotification:{value:true},isPqcMode:{value:true},payloadLoading:{value:false},pqcSubmitResultUncertain:{value:false},showFrontlineError:e=>errors.push(e)})
 await page.handleValidate();await page.handleConfirmPqcSubmit();await page.handleProductionFormalSubmit();await page.handleSelectActiveOrder({activeOrderId:414});assert.equal(errors.length,4);assert.ok(errors.every(e=>e.includes('退回通知仅允许更正原任务')))
})
test('实际自动生产初始化与PQC刷新入口不会再查询其他周期',async()=>{
 const page=functions(['initializeProductionSelection','refreshPqcActiveOrdersAndEnsureSelection'],{hasReturnNotification:{value:true}})
 await page.initializeProductionSelection();await page.refreshPqcActiveOrdersAndEnsureSelection()
})
