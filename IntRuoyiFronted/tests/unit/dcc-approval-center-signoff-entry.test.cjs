const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')),ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const decl=(name,required=true)=>{const node=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text===name));if(required)assert.ok(node,name);return node?node.getText(ast):''}
function host(type='OBSOLETE',changes={}){
 const c={exports:{},String,computed:vue.computed,viewerMode:vue.ref(false),isBrowserTraceabilityPage:vue.ref(false),showDetailManagementActions:vue.ref(false),isApprovalUploadHandlingPage:vue.ref(true),isSignoffTask:vue.ref(true),fileDetail:vue.ref({id:'4033',needTraining:true}),approvalProcessInstanceId:vue.ref('actual-obsolete-bpm'),approvalTodoTask:vue.ref({id:'actual-task',processInstanceId:'actual-obsolete-bpm',taskDefinitionKey:'MATRIX_REVIEW',status:1,assigneeUserId:'1'}),approvalProgressScope:vue.ref({fileId:'4033',bpmRound:'actual-obsolete-bpm',applicationType:type}),applicationRoundSelection:vue.ref({primaryBpmRound:'actual-obsolete-bpm',blockedReason:'',lockPrimaryRound:true}),applicationRoundContextKey:vue.ref('actual-context'),applicationApprovalRead:vue.ref({contextKey:'actual-context',processInstanceId:'actual-obsolete-bpm',taskId:'actual-task'}),currentUserId:vue.ref('1'),checkPermi:()=>true,...changes}
 const script=decl('currentApprovalApplicationType',false)+decl('canShowSignoffAssignment',false)+decl('approvalHandlingTrainingText',false)+decl('approvalHandlingProcessText',false)+'\nexports.allowed=canShowSignoffAssignment;exports.training=approvalHandlingTrainingText;exports.process=approvalHandlingProcessText;'
 vm.runInNewContext(ts.transpileModule(script,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return{c,...c.exports}
}
function renderAssignment(h){
 const find=n=>{if(n.type===1&&n.tag==='DetailSignoffAssignment')return n;for(const ch of [...(n.children||[]),...(n.branches||[])]){const found=find(ch);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const canShowSignoffAssignment=__host.allowed;const fileDetail=__host.c.fileDetail;const approvalProcessInstanceId=__host.c.approvalProcessInstanceId;const approvalTodoTask=__host.c.approvalTodoTask;const isSignoffTask=true;const showDetailManagementActions=false;let signoffAssignmentState;const reloadAll=()=>{};</script>').descriptor,{id:'actual-todo-signoff',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__host:h};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const seen=[];const app=renderer.createApp(c.exports.default);app.component('DetailSignoffAssignment',{props:['fileId','taskId','processInstanceId'],setup:p=>{seen.push({...p});return()=>vue.h('button','原签名指派面板')}});app.mount({children:[]});app.unmount();return seen
}
test('actual parent normal center todo renders original assignment for each verified native type without granting management',()=>{
 for(const type of ['UPLOAD','REVISION','OBSOLETE']){const h=host(type);const seen=renderAssignment(h);assert.equal(seen.length,1);assert.equal(h.allowed.value,true);assert.deepEqual(seen[0],{fileId:'4033',taskId:'actual-task',processInstanceId:'actual-obsolete-bpm'});assert.equal(h.c.showDetailManagementActions.value,false)}
})
test('actual entry denies readonly, mismatched task/round/file and user or menu while original eligible management remains available',()=>{
 for(const changes of [{viewerMode:vue.ref(true)},{isBrowserTraceabilityPage:vue.ref(true)},{checkPermi:()=>false},{approvalProgressScope:vue.ref(undefined)},{approvalProgressScope:vue.ref({fileId:'other',bpmRound:'actual-obsolete-bpm',applicationType:'OBSOLETE'})},{approvalTodoTask:vue.ref({id:'bad',processInstanceId:'another-bpm',taskDefinitionKey:'MATRIX_REVIEW',status:1,assigneeUserId:'1'})},{currentUserId:vue.ref('2')},{applicationRoundSelection:vue.ref({primaryBpmRound:null,blockedReason:'未核验',lockPrimaryRound:true})}])assert.equal(host('OBSOLETE',changes).allowed.value,false)
 for(const read of [undefined,{contextKey:'stale-context',processInstanceId:'actual-obsolete-bpm',taskId:'actual-task'},{contextKey:'actual-context',processInstanceId:'actual-obsolete-bpm',taskId:'another-dept-task'}])assert.equal(host('OBSOLETE',{applicationApprovalRead:vue.ref(read)}).allowed.value,false)
 assert.equal(host('REVISION',{showDetailManagementActions:vue.ref(true),isApprovalUploadHandlingPage:vue.ref(false)}).allowed.value,true)
})
test('actual handling application copy uses current verified obsolete round rather than original file training flag',()=>{
 const obsolete=host('OBSOLETE');assert.match(obsolete.training.value,/无需培训/);assert.match(obsolete.process.value,/会签.*批准.*结束/)
 for(const type of ['UPLOAD','REVISION']){const h=host(type);assert.equal(h.training.value,'需要培训');assert.match(h.process.value,/培训.*文控审核.*受控.*下发/)}
 assert.equal(host('OBSOLETE',{approvalProgressScope:vue.ref(undefined)}).training.value,'本次申请身份尚未核验')
})
