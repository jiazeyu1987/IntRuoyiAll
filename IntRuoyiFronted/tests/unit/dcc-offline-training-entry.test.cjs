const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const read=p=>fs.readFileSync(p,'utf8'),fileId='9007199254740993',bpm='actual-bpm-round',type='DCC_OFFLINE_TRAINING_RECORD'
const query={management:'1',from:'workbench',returnTo:'/dcc/controlled-file/workbench',processInstanceId:bpm}
const row={id:'native-id',moduleCode:'DCC',sourceTaskType:type,sourceTaskId:'DCC_OFFLINE_TRAINING_RECORD:1:'+fileId+':'+bpm,businessKey:fileId,businessTitle:'实际培训文件.docx',businessCode:'SOP-TRAIN',businessContextTags:['版本：A/1'],requiresSignature:false,availableActions:['PROCESS_IN_MODULE'],processInstanceId:bpm,detailRoute:'/dcc/controlled-file/detail/'+fileId,detailQuery:query}
function mod(p,deps={}){const c={exports:{},Error,String,Number,BigInt,Array,Object,Set,URL,URLSearchParams,JSON,Promise,console,window:{location:{origin:'http://localhost:8061'}},require:id=>deps[id]||(id.endsWith('edhrWorkTaskNavigation')?{EDHR_WORK_TASK_NOTIFY_PATHS:new Set()}:id==='./dccOfflineTrainingRecord'?helper():(()=>{throw Error(id)})())};vm.runInNewContext(ts.transpileModule(read(p),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
function helper(){const p='src/utils/dccOfflineTrainingRecord.ts';assert.ok(fs.existsSync(p),'formal offline training route/queue helper');return mod(p)}
function declaration(path,names){const ast=ts.createSourceFile('actual.ts',parse(read(path)).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true);return names.map(name=>{const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text===name));assert.ok(st,'actual '+name);return st.getText(ast)}).join('\n')}
test('actual approval-center native training navigation opens management with real file/BPM and no fake taskId',()=>{
 const c={exports:{},String,queryParams:{viewType:'TODO'},DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX:'/dcc/controlled-file/detail/',DCC_APPROVAL_HANDLING_MODE:'approval',DCC_OFFLINE_TRAINING_RECORD:type,resolveOfflineTrainingRecordLocation:(...args)=>helper().resolveOfflineTrainingRecordLocation(...args)}
 const p='src/views/approval-center/index.vue';vm.runInNewContext(ts.transpileModule(declaration(p,['isDccModuleHandlingAction','resolveDccApprovalDetailLocation'])+'\nexports.resolve=resolveDccApprovalDetailLocation',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const location=c.exports.resolve(row,row.detailRoute,row.detailQuery);assert.equal(location.query.management,'1');assert.equal(location.query.processInstanceId,bpm);assert.equal(Object.hasOwn(location.query,'taskId'),false);assert.equal(Object.hasOwn(location.query,'handling'),false)
 const legacy=c.exports.resolve({...row,sourceTaskType:'DCC_CONTROLLED_FILE_TASK',sourceTaskId:'real-user-task'},row.detailRoute,{processInstanceId:bpm});assert.equal(legacy.query.taskId,'real-user-task');assert.equal(legacy.query.handling,'approval')
})
test('formal offline queue reads all pages of the same DCC provider before filtering and preserves exact version facts',async()=>{
 const h=helper(),calls=[],ordinary=Array.from({length:100},(_,i)=>({...row,id:'ordinary-'+i,sourceTaskType:'DCC_CONTROLLED_FILE_TASK'}))
 const result=await h.loadOfflineTrainingRecordTodos(async params=>{calls.push(params);return params.pageNo===1?{list:ordinary,total:101}:{list:[row],total:101}},()=>true)
 assert.equal(calls.length,2);assert.equal(calls[0].moduleCode,'DCC');assert.equal(calls[0].viewType,'TODO');assert.equal(result.length,1);assert.equal(result[0].fileId,fileId);assert.equal(result[0].versionNo,'A/1');assert.equal(result[0].fileNumber,'SOP-TRAIN')
})
test('actual dedicated training notification parser binds marker, file and current BPM to the strict management URL',async()=>{
 const h=mod('src/utils/notifyMessageNavigation.ts'),url=row.detailRoute+'?'+new URLSearchParams(query)
 const params={notifyTargetType:type,notifyTargetId:fileId,notifyProcessInstanceId:bpm,detailUrl:url,actionUrl:url}
 const target=h.getNotifyMessageTarget({templateParams:params});assert.ok(target);assert.equal(target.label,'上传线下培训记录')
 const calls=[];await h.navigateToNotifyMessageTarget({push:async value=>calls.push(value)},target);assert.equal(calls[0].path,row.detailRoute);assert.equal(calls[0].query.processInstanceId,bpm);assert.equal(Object.hasOwn(calls[0].query,'taskId'),false)
 for(const actionUrl of ['https://foreign.example'+url,url+'&taskId=fake',url+'&processInstanceId=other',url.replace(fileId,'9007199254740995')])assert.equal(h.getNotifyMessageTarget({templateParams:{...params,actionUrl}}),null)
})
test('actual workbench native queue handler reads the formal provider separately from old reading-confirmation tasks',async()=>{
 const c={exports:{},Error,String,offlineTrainingSequence:0,canReadOfflineTrainingTodos:vue.ref(true),offlineTrainingRows:vue.ref([]),offlineTrainingLoadedContext:vue.ref(''),offlineTrainingLoading:vue.ref(false),offlineTrainingError:vue.ref(''),readOfflineTrainingContext:()=> 'actor-context',getApprovalTaskPage:async()=>({list:[row],total:1}),loadOfflineTrainingRecordTodos:(...args)=>helper().loadOfflineTrainingRecordTodos(...args)}
 vm.runInNewContext(ts.transpileModule(declaration('src/views/dcc/controlled-file/workbench/index.vue',['loadOfflineTrainingTodos'])+'\nexports.load=loadOfflineTrainingTodos',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 await c.exports.load();assert.equal(c.offlineTrainingRows.value.length,1);assert.equal(c.offlineTrainingRows.value[0].fileId,fileId);assert.equal(c.offlineTrainingError.value,'')
})
function renderButton(path,testId,state){
 const {descriptor}=parse(read(path)),find=n=>{if(n.type===1&&n.tag==='el-button'&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content===testId))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const setup=Object.keys(state).map(k=>'const '+k+'=__host.'+k).join('\n')
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>'+setup+'</script>').descriptor,{id:testId,inlineTemplate:true})
 const c={exports:{},require:()=>vue,__host:state};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);app.component('el-button',{setup:(_p,ctx)=>()=>vue.h('button',ctx.attrs,ctx.slots.default?.())});const root={children:[]};app.mount(root)
 return{click:()=>root.children[0].props.onClick(),close:()=>app.unmount()}
}
test('actual workbench upload button uses the actual current-row handler and strict management location',async()=>{
 const record=helper().readOfflineTrainingRecordTodo(row),calls=[],c={exports:{},offlineTrainingRows:vue.ref([record]),offlineTrainingLoadedContext:vue.ref('actor-context'),offlineTrainingLoading:vue.ref(false),offlineTrainingError:vue.ref(''),readOfflineTrainingContext:()=> 'actor-context',resolveOfflineTrainingRecordLocation:helper().resolveOfflineTrainingRecordLocation,router:{push:async target=>calls.push(target)}}
 const p='src/views/dcc/controlled-file/workbench/index.vue';vm.runInNewContext(ts.transpileModule(declaration(p,['openOfflineTrainingRecord'])+'\nexports.open=openOfflineTrainingRecord',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const button=renderButton(p,'dcc-offline-training-open',{row:record,openOfflineTrainingRecord:c.exports.open})
 try{await button.click();assert.equal(calls[0].query.processInstanceId,bpm);assert.equal(calls[0].query.management,'1');assert.equal(Object.hasOwn(calls[0].query,'taskId'),false);c.readOfflineTrainingContext=()=> 'new-actor';await button.click();assert.equal(calls.length,1)}finally{button.close()}
})
test('actual notification detail training handler closes the message then opens the exact file round',async()=>{
 const h=mod('src/utils/notifyMessageNavigation.ts'),url=row.detailRoute+'?'+new URLSearchParams(query),target=h.getNotifyMessageTarget({templateParams:{notifyTargetType:type,notifyTargetId:fileId,notifyProcessInstanceId:bpm,detailUrl:url,actionUrl:url}}),calls=[]
 const c={exports:{},dccOfflineTrainingNavigation:vue.ref(target),router:{push:async value=>calls.push(value)},navigateToNotifyMessageTarget:h.navigateToNotifyMessageTarget,resetDialogState:()=>calls.push('closed'),nextTick:vue.nextTick}
 vm.runInNewContext(ts.transpileModule(declaration('src/views/system/notify/my/MyNotifyMessageDetail.vue',['navigateToDccOfflineTraining'])+'\nexports.open=navigateToDccOfflineTraining',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 await c.exports.open();assert.equal(calls[0],'closed');assert.equal(calls[1].path,row.detailRoute);assert.equal(calls[1].query.processInstanceId,bpm)
})
