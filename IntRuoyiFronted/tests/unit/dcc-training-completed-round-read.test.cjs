const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse}=require('vue/compiler-sfc')
function moduleFrom(file){const env={exports:{},Error,String,Number,Array,Set,BigInt};vm.runInNewContext(ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,env);return env.exports}
const helpers=moduleFrom('src/views/dcc/controlled-file/detail/application-round-context.ts'),native=moduleFrom('src/views/dcc/controlled-file/detail/native-approval-progress.ts')
const ast=ts.createSourceFile('detail.ts',parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const decl=ast.statements.find(s=>s.declarationList?.declarations.some(d=>d.name.text==='loadApprovalDetail'))
const progress=()=>({controlledFileId:'10',applicationType:'UPLOAD',bpmRound:'bpm-own',attributeRound:1,tasks:[{id:'task-signed',processInstanceId:'bpm-own',taskDefinitionKey:'DOC_CONTROL_REVIEW',status:2}]})
function host({eligible=true,viewer=false,transport=async()=>progress()}={}){
 const reads=[],c={String,Promise,detailLoadSequence:1,controlledFileId:vue.ref('10'),route:{fullPath:'own-management',query:{processInstanceId:'bpm-own',management:'1'}},isCurrentDetailLoad:(n,id,route)=>n===c.detailLoadSequence&&id===c.controlledFileId.value&&route===c.route.fullPath,
 viewerMode:vue.ref(viewer),isBrowserTraceabilityPage:vue.ref(false),fileDetail:vue.ref({id:'10',status:'ACTIVE',processInstanceId:'bpm-own',canReadTrainingApplicationProgress:eligible}),applicationRoundContextKey:vue.ref('exact-context'),applicationApprovalRead:vue.ref(),approvalProgressScope:vue.ref(),approvalProgressError:vue.ref(''),approvalLoading:vue.ref(false),approvalTodoTask:vue.ref(null),approvalTaskList:vue.ref([]),stageProgressList:vue.ref([]),canUploadApplicantTrainingRecord:vue.ref(false),checkPermi:()=>true,syncStageProgress(){},
 getControlledFileApplicationProgress:async(id,bpm)=>{reads.push(['authorized DCC progress',id,bpm]);return transport()},validateApplicationApprovalRead:helpers.validateApplicationApprovalRead,resolveApprovalProgressScope:native.resolveApprovalProgressScope,
 TaskApi:{getTaskListByProcessInstanceId:async()=>{reads.push(['private BPM tasks']);throw Error('No private BPM access')}},ProcessInstanceApi:{getApprovalDetail:async()=>{reads.push(['private BPM detail']);throw Error('No private BPM access')}},resolveReadSideErrorMessage:e=>e.message}
 vm.runInNewContext(ts.transpileModule(decl.getText(ast)+'\nthis.load=loadApprovalDetail',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return{c,reads,load:c.load}
}
test('after training activation, original applicant keeps authorized exact-round history and no private BPM or signature task',async()=>{
 const h=host();await h.load();assert.deepEqual(h.reads,[['authorized DCC progress','10','bpm-own']]);assert.equal(h.c.applicationApprovalRead.value.processInstanceId,'bpm-own');assert.equal(h.c.applicationApprovalRead.value.taskId,'');assert.equal(h.c.approvalTodoTask.value,null)
 const selected=helpers.resolveApplicationRoundSelection({fileId:'10',nativeBpmRound:'bpm-own',requestedBpmRound:'bpm-own',contextKey:'exact-context',readContext:h.c.applicationApprovalRead.value});assert.equal(selected.blockedReason,'');assert.equal(selected.primaryBpmRound,'bpm-own')
})
test('ordinary formal readers and viewer mode do not receive applicant-only progress or private BPM access',async()=>{
 for(const input of [{eligible:false},{viewer:true}]){const h=host(input);await h.load();assert.deepEqual(h.reads,[]);assert.equal(h.c.approvalTodoTask.value,null)}
})
test('foreign round tasks or file mapping explicitly block the exact context and never borrow native facts',async()=>{
 for(const invalid of [{...progress(),tasks:[{id:'foreign',processInstanceId:'bpm-foreign'}]},{...progress(),controlledFileId:'99'}]){const h=host({transport:async()=>invalid});await assert.rejects(h.load(),/轮次|身份/);assert.ok(h.c.applicationApprovalRead.value.error);assert.equal(h.c.approvalProgressScope.value,undefined);assert.equal(h.c.approvalTodoTask.value,null)}
})
test('late completed-training progress cannot overwrite a different page or round',async()=>{
 let resolve;const p=new Promise(r=>{resolve=r}),h=host({transport:()=>p}),loading=h.load();h.c.route.fullPath='other-page';h.c.detailLoadSequence++;resolve(progress());await loading;assert.equal(h.c.applicationApprovalRead.value,undefined);assert.equal(h.c.approvalTodoTask.value,null)
})
test('real progress read failure stays an explicit error and leaves no task or old round context',async()=>{
 const h=host({transport:async()=>{throw Error('DCC exact-round progress denied')}});await assert.rejects(h.load(),/denied/);assert.equal(h.c.applicationApprovalRead.value.error,'DCC exact-round progress denied');assert.equal(h.c.approvalLoading.value,false);assert.equal(h.c.approvalTodoTask.value,null)
})
