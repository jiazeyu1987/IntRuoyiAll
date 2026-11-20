const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const ts=require('typescript'),vue=require('vue')
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const filename=path.join(__dirname,'../../src/views/dcc/controlled-file/detail/index.vue')
const {descriptor}=parse(fs.readFileSync(filename,'utf8'),{filename})
const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
function host(){
 const allowed=vue.ref(true)
 const h={computed:vue.computed,isApprovalUploadHandlingPage:vue.ref(true),isExternalReviewProcess:vue.ref(false),
  approvalLoading:vue.ref(false),checkPermi:p=>allowed.value&&p[0]==='dcc:controlled-file:review',
  isWorkingBrowserDetailCurrent:vue.ref(true),approvalProgressError:vue.ref(''),applicationRoundSelection:vue.ref({blockedReason:''}),
  currentUserId:vue.ref(8),approvalProcessInstanceId:vue.ref('round-1'),
  approvalTodoTask:vue.ref({id:'task-1',taskDefinitionKey:'DOC_CONTROL_REVIEW',processInstanceId:'round-1',assigneeUserId:8})}
 const names=['isDocControlReviewPage','docControlReviewBlockedReason']
 const code=ast.statements.filter(s=>s.declarationList?.declarations.some(d=>names.includes(d.name.getText(ast)))).map(s=>s.getText(ast)).join('\n')
 vm.runInNewContext(ts.transpileModule(code+'\nglobalThis.active=isDocControlReviewPage;globalThis.reason=docControlReviewBlockedReason',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h)
 return {...h,allowed}
}
test('only document-control review handling uses the fixed bar and current permitted task enables actions',()=>{
 const h=host();assert.equal(h.active.value,true);assert.equal(h.reason.value,'')
 h.isExternalReviewProcess.value=true;assert.equal(h.active.value,false);h.isExternalReviewProcess.value=false
 h.isApprovalUploadHandlingPage.value=false;assert.equal(h.active.value,false);h.isApprovalUploadHandlingPage.value=true
 for(const task of [null,{id:'other',taskDefinitionKey:'MATRIX_APPROVAL'},
  {id:'task-1',taskDefinitionKey:'DOC_CONTROL_REVIEW',assigneeUserId:9,processInstanceId:'round-1'},
  {id:'task-1',taskDefinitionKey:'DOC_CONTROL_REVIEW',assigneeUserId:8,processInstanceId:'old-round'}]){
  h.approvalTodoTask.value=task;assert.match(h.reason.value,/没有分配/)
 }
 h.allowed.value=false;assert.match(h.reason.value,/缺少文控审核权限/)
 h.allowed.value=true;h.isWorkingBrowserDetailCurrent.value=false;assert.match(h.reason.value,/上下文已变化/)
 h.isWorkingBrowserDetailCurrent.value=true;h.applicationRoundSelection.value.blockedReason='申请轮次不一致';assert.equal(h.reason.value,'申请轮次不一致')
 h.approvalLoading.value=true;assert.match(h.reason.value,/正在读取/)
})
test('page compiles, hides inline duplicates, and keeps approve/reject handlers in the shared fixed footer',()=>{
 const script=compileScript(descriptor,{id:'doc-control-review-actions'})
 assert.deepEqual(compileTemplate({source:descriptor.template.content,filename,id:'doc-control-review-actions',compilerOptions:{bindingMetadata:script.bindings}}).errors,[])
 assert.match(descriptor.template.content,/<SupervisorReviewActions v-if="isDocControlReviewPage"[\s\S]*?:blocked-reason="docControlReviewBlockedReason"[\s\S]*?@approve="openActionDialog\('approve'\)" @reject="openActionDialog\('reject'\)"/)
 assert.match(descriptor.template.content,/!isMatrixApprovalPage && !isDocControlReviewPage" class="mt-14px/)
})
