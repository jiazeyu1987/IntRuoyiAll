const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const read=file=>fs.readFileSync(file,'utf8')
function helper(){const path='src/views/dcc/controlled-file/detail/native-approval-progress.ts';assert.ok(fs.existsSync(path),'formal native presentation helper');const c={exports:{},Error,String,Number,Set,Array};vm.runInNewContext(ts.transpileModule(read(path),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
test('actual parent preserves native stages rather than reprojecting a fabricated legacy fourth approval',()=>{
 const ast=ts.createSourceFile('detail.ts',parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='displayStageProgressList'));assert.ok(st)
 const rows=[{stageCode:'MATRIX_REVIEW'},{stageCode:'MATRIX_APPROVAL'},{stageCode:'DOC_CONTROL_REVIEW'},{stageCode:'CONTROLLED'},{stageCode:'DISTRIBUTED'}]
 const c={exports:{},computed:vue.computed,Map,approvalProgressScope:vue.ref({applicationType:'UPLOAD'}),stageProgressList:vue.ref(rows),originalReleaseApprovalStages:[{stageCode:'DOC_CONTROL_REVIEW'},{stageCode:'MATRIX_REVIEW'},{stageCode:'MATRIX_APPROVAL'},{stageCode:'DOC_CONTROL_APPROVAL'}],buildEmptyOriginalReleaseStage:s=>s}
 vm.runInNewContext(ts.transpileModule(st.getText(ast)+'\nexports.result=displayStageProgressList.value',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 assert.deepEqual(Array.from(c.exports.result,r=>r.stageCode),rows.map(r=>r.stageCode))
})
test('formal native round and exact legacy metadata remain distinct and unknown metadata rejects',()=>{
 const h=helper(),base={fileId:'10',bpmRound:'round-O',nativeBpmRound:'round-U'}
 assert.equal(h.resolveApprovalProgressScope({...base,rounds:[{controlledFileId:'10',bpmRound:'round-O',applicationType:'OBSOLETE',attributeRound:2}]}).applicationType,'OBSOLETE')
 assert.throws(()=>h.resolveApprovalProgressScope({...base,rounds:[]}),/身份|流程|轮次/)
 assert.equal(h.resolveApprovalProgressScope({...base,bpmRound:'legacy',nativeBpmRound:'legacy',rounds:[],approvalDetail:{processInstance:{id:'legacy',processDefinitionId:'def'},processDefinition:{id:'def',key:'dcc-controlled-file-approval'}}}).applicationType,'LEGACY')
})
test('native upload/revision shows training only from actual facts and obsolete ends at approval',()=>{
 const h=helper(),scope={fileId:'10',bpmRound:'round',applicationType:'REVISION'},facts={fileId:'10',needTraining:true,status:'PENDING_MATRIX_APPROVAL'}
 const tasks=[{id:'task1',processInstanceId:'round',taskDefinitionKey:'MATRIX_REVIEW',status:2},{id:'task2',processInstanceId:'round',taskDefinitionKey:'MATRIX_APPROVAL',status:1}]
 const stages=h.buildNativeApprovalProgress(scope,facts,tasks)
 assert.deepEqual(Array.from(stages,s=>s.stageCode),['MATRIX_REVIEW','MATRIX_APPROVAL','APPLICANT_TRAINING_RECORD','DOC_CONTROL_REVIEW','CONTROLLED','DISTRIBUTED'])
 assert.equal(stages.find(s=>s.isCurrent).stageCode,'MATRIX_APPROVAL')
 assert.equal(h.buildNativeApprovalProgress({...scope,applicationType:'OBSOLETE'},{...facts,status:'ACTIVE'},tasks).length,2)
 assert.throws(()=>h.buildNativeApprovalProgress(scope,{...facts,needTraining:undefined},tasks),/培训/)
})
test('signature duty uses the actual same-round task and assignment action, otherwise remains unrecorded',()=>{
 const h=helper(),scope={fileId:'10',bpmRound:'round',applicationType:'UPLOAD'},tasks=[{id:'task',processInstanceId:'round',taskDefinitionKey:'MATRIX_REVIEW',status:2}]
 assert.equal(h.resolveSignatureDuty({taskId:'task',actionType:'ASSIGN'},tasks,scope),'部门负责人指派')
 assert.equal(h.resolveSignatureDuty({taskId:'task',actionType:'APPROVE'},tasks,scope),'会签人')
 assert.equal(h.resolveSignatureDuty({taskId:'task'},tasks,scope),'签名职责未记录')
 assert.equal(h.resolveSignatureDuty({taskId:'foreign'},tasks,scope),'签名职责未记录')
 assert.equal(h.resolveSignatureDuty({taskId:'task'},[{...tasks[0],processInstanceId:'other'}],scope),'签名职责未记录')
})
test('receive-task training wait follows exact file round status without inventing a user task',()=>{
 const h=helper(),scope={fileId:'10',bpmRound:'round',applicationType:'UPLOAD'}
 const tasks=[{id:'review',processInstanceId:'round',taskDefinitionKey:'MATRIX_REVIEW',status:2},{id:'approval',processInstanceId:'round',taskDefinitionKey:'MATRIX_APPROVAL',status:2}]
 const stages=h.buildNativeApprovalProgress(scope,{fileId:'10',needTraining:true,status:'PENDING_APPLICANT_TRAINING_RECORD',trainingRecordAvailable:false},tasks)
 assert.equal(stages.find(s=>s.stageCode==='APPLICANT_TRAINING_RECORD').isCurrent,true)
 assert.equal(stages.find(s=>s.stageCode==='APPLICANT_TRAINING_RECORD').isCompleted,false)
 const completed=h.buildNativeApprovalProgress(scope,{fileId:'10',needTraining:true,status:'PENDING_DOC_CONTROL_REVIEW',trainingRecordAvailable:true},tasks)
 assert.equal(completed.find(s=>s.stageCode==='APPLICANT_TRAINING_RECORD').isCompleted,true)
 const unknown=h.buildNativeApprovalProgress(scope,{fileId:'10',needTraining:true,status:'PENDING_DOC_CONTROL_REVIEW'},tasks)
 assert.equal(unknown.find(s=>s.stageCode==='APPLICANT_TRAINING_RECORD').isCompleted,false)
})
test('actual detail responsibility computed assigns native training to document control from the exact stage',()=>{
 const ast=ts.createSourceFile('detail.ts',parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='detailHandlingSummary'));assert.ok(st)
 const c={exports:{},computed:vue.computed,fileDetail:vue.ref({id:'10',status:'PENDING_APPLICANT_TRAINING_RECORD'}),approvalProgressScope:vue.ref({applicationType:'UPLOAD',bpmRound:'round'}),currentStage:vue.ref({stageCode:'APPLICANT_TRAINING_RECORD',stageName:'培训（文控上传线下记录）'}),getControlledFileHandlingSummary:()=>({responsibilityHint:'责任：申请人'})}
 vm.runInNewContext(ts.transpileModule(st.getText(ast)+'\nexports.result=detailHandlingSummary.value',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 assert.equal(c.exports.result.responsibilityHint,'责任：文控上传线下培训记录')
})
test('actual stage-grid template renders the native ordered stages without the obsolete legacy fourth stage',()=>{
 const h=helper(),{descriptor}=parse(read('src/views/dcc/controlled-file/detail/index.vue'))
 const find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='class'&&p.value?.content==='stage-grid'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const stages=h.buildNativeApprovalProgress({fileId:'10',bpmRound:'round',applicationType:'UPLOAD'},{fileId:'10',needTraining:false,status:'PENDING_MATRIX_REVIEW'},[{id:'task',processInstanceId:'round',taskDefinitionKey:'MATRIX_REVIEW',status:1}])
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const displayStageProgressList=__stages;const formatStageProgressActors=()=>"";const formatStageProgressTime=()=>"";const formatStageSignatureStatus=()=>"";</script>').descriptor,{id:'native-stage-grid',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__stages:stages};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);app.component('el-tag',{setup:(_p,ctx)=>()=>vue.h('span',ctx.slots.default?.())});const root={children:[]};app.mount(root)
 const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{const value=text(root);assert.equal(value.includes('文控批准'),false);let cursor=-1;for(const word of ['会签','批准','文控审核','受控','文控下发']){const next=value.indexOf(word,cursor+1);assert.ok(next>cursor);cursor=next}}finally{app.unmount()}
})
