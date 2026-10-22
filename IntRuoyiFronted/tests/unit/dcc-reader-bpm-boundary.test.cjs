const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse,compileTemplate}=require('vue/compiler-sfc')
const ast=ts.createSourceFile('detail.ts',parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text==='loadApprovalDetail'))
const {resolveDetailReadSelection}=require('./dcc-detail-read-test-loader.cjs')
function host({viewer=false,trace=false,task='',status='ACTIVE',permission=true}={}){const reads=[],c={String,Promise,detailLoadSequence:1,controlledFileId:vue.ref('10'),route:{fullPath:'current',query:task?{taskId:task,processInstanceId:'bpm-own'}:{}},isCurrentDetailLoad:()=>true,
 resolveDetailReadSelection,viewerMode:vue.ref(viewer),isBrowserTraceabilityPage:vue.ref(trace),fileDetail:vue.ref({processInstanceId:'bpm-own',status}),applicationRoundContextKey:vue.ref('round'),applicationApprovalRead:vue.ref(),approvalProgressScope:vue.ref(),approvalProgressError:vue.ref(''),approvalLoading:vue.ref(false),approvalTodoTask:vue.ref(),approvalTaskList:vue.ref([]),stageProgressList:vue.ref([]),
 approvalProgressNotice:vue.ref(''),checkPermi:()=>permission,canUploadApplicantTrainingRecord:vue.ref(false),syncStageProgress(){if(!c.approvalProgressScope.value)c.stageProgressList.value=[]},TaskApi:{getTaskListByProcessInstanceId:async()=>{reads.push('private tasks');throw Error('BPM private access denied')}},ProcessInstanceApi:{getApprovalDetail:async()=>{reads.push('private detail');throw Error('BPM private access denied')}},resolveReadSideErrorMessage:e=>e.message}
 vm.runInNewContext(ts.transpileModule(declaration.getText(ast)+'\nthis.load=loadApprovalDetail',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return{c,reads,load:c.load}}
test('public published viewer and trace readers do not query private BPM even with query menu permission',async()=>{for(const mode of [{viewer:true},{trace:true},{}]){const h=host(mode);await h.load();assert.deepEqual(h.reads,[]);assert.equal(h.c.approvalLoading.value,false)}})
test('actual pending task handling still reaches original private BPM guard and propagates rejection',async()=>{const h=host({task:'task-own',status:'PENDING_MATRIX_REVIEW'});await assert.rejects(h.load(),/BPM private access denied/);assert.deepEqual(h.reads,['private tasks','private detail'])})
test('pending applicant without task or BPM permission makes zero private requests and gets a scope notice',async()=>{
 const h=host({status:'PENDING_SUPERVISOR_REVIEW',permission:false})
 h.c.approvalTaskList.value=[{id:'previous-task'}];h.c.approvalTodoTask.value={id:'previous-task'};h.c.stageProgressList.value=[{status:'pending'}]
 let failure;try{await h.load()}catch(cause){failure=cause}
 assert.deepEqual(h.reads,[],'applicant must not request private BPM tasks')
 assert.equal(failure,undefined);assert.match(h.c.approvalProgressNotice.value,/权限范围/)
 assert.equal(h.c.approvalProgressError.value,'');assert.equal(h.c.applicationApprovalRead.value?.error,undefined)
 assert.equal(h.c.approvalProgressScope.value,undefined);assert.equal(h.c.approvalTodoTask.value,null)
 assert.equal(h.c.approvalTaskList.value.length,0);assert.equal(h.c.stageProgressList.value.length,0);assert.equal(h.c.approvalLoading.value,false)
})
test('an actual task still exposes BPM denial without task query permission',async()=>{
 const h=host({task:'task-own',status:'PENDING_MATRIX_REVIEW',permission:false})
 await assert.rejects(h.load(),/BPM private access denied/)
 assert.deepEqual(h.reads,['private tasks']);assert.equal(h.c.approvalProgressNotice.value,'')
 assert.match(h.c.applicationApprovalRead.value.error,/BPM private access denied/)
})
test('scope notice is displayed independently from approval errors and pending verification',()=>{
 const page=fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')
 assert.match(page,/<el-alert v-else-if="approvalProgressNotice" :title="approvalProgressNotice" type="info"/)
 assert.match(page,/approvalProgressNotice\.value = ''\s+const requestedId = controlledFileId\.value/)
})
test('actual progress alerts wait only during a read, retain error and scope priority, and stay absent after formal skips',async()=>{
 const page=fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')
 const alerts=page.match(/<el-alert v-if="approvalProgressError"[^>]*\/>\s*<el-alert v-else-if="approvalProgressNotice"[^>]*\/>\s*<el-alert v-else-if="[^"]+"[^>]*\/>/)
 assert.ok(alerts,'real adjacent approval progress alerts must exist')
 const compiled=compileTemplate({source:alerts[0],filename:'detail/index.vue',id:'progress-alerts',compilerOptions:{isCustomElement:tag=>tag==='el-alert'}})
 assert.deepEqual(compiled.errors,[])
 const env={exports:{},require:id=>{assert.equal(id,'vue');return vue}}
 vm.runInNewContext(ts.transpileModule(compiled.code,{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,env)
 const waitingTitle='正在核验本轮审批流程身份；尚未读取的阶段不作推断。'
 const state={approvalProgressError:'',approvalProgressNotice:'',approvalLoading:true,approvalProgressScope:undefined}
 assert.equal(env.exports.render(state,[]).props?.title,waitingTitle)
 assert.equal(env.exports.render({...state,approvalProgressScope:{bpmRound:'bpm-own'}},[]).props?.title,undefined)
 assert.equal(env.exports.render({...state,approvalProgressError:'真实读取失败',approvalProgressNotice:'范围提示'},[]).props?.title,'真实读取失败')
 assert.equal(env.exports.render({...state,approvalProgressNotice:'范围提示'},[]).props?.title,'范围提示')
 const limited=host({status:'PENDING_SUPERVISOR_REVIEW',permission:false});await limited.load()
 assert.equal(env.exports.render(Object.fromEntries(Object.entries(limited.c).map(([key,value])=>[key,vue.unref(value)])),[]).props?.title,limited.c.approvalProgressNotice.value)
 for(const mode of [{status:'ACTIVE'},{status:'CONTROLLED_PENDING_EFFECTIVE'},{status:'SUPERSEDED'},{status:'OBSOLETE'},{viewer:true,status:'PENDING_MATRIX_REVIEW'},{trace:true,status:'PENDING_MATRIX_REVIEW'}]){
  const h=host(mode);await h.load();assert.deepEqual(h.reads,[]);assert.equal(h.c.approvalLoading.value,false)
  const rendered=env.exports.render(Object.fromEntries(Object.entries(h.c).map(([key,value])=>[key,vue.unref(value)])),[])
  assert.equal(rendered.props?.title,undefined,JSON.stringify(mode)+' deliberately skips BPM and must not claim verification is pending')
 }
})
