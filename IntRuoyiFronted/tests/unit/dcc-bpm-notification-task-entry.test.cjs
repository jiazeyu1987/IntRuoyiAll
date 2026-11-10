const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const c={exports:{}};vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/bpm/processInstance/detail/dcc-business-identity.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
test('already authorized native DCC notification resolves exact business file and current signed task',()=>{assert.equal(typeof c.exports.resolveDccTaskHandlingLocation,'function');const r=c.exports.resolveDccTaskHandlingLocation({id:'bpm-real',businessKey:'2054545668044084165'},'dcc-controlled-file-upload',{id:'task-real',processInstanceId:'bpm-real'});assert.equal(r.path,'/dcc/controlled-file/detail/2054545668044084165');assert.equal(r.query.handling,'approval');assert.equal(r.query.from,'approval-center');assert.equal(r.query.processInstanceId,'bpm-real');assert.equal(r.query.taskId,'task-real');assert.equal(r.query.viewer,undefined)})
test('completed history and unrelated workflow remain their real BPM views; mismatched identity never redirects',()=>{const f=c.exports.resolveDccTaskHandlingLocation;assert.equal(typeof f,'function');assert.equal(f({id:'bpm-real',businessKey:'10'},'dcc-controlled-file-upload',null),null);assert.equal(f({id:'bpm-real',businessKey:'10'},'dcc-controlled-file-approval',{id:'task',processInstanceId:'bpm-real'}),null);assert.equal(f({id:'bpm-real',businessKey:'10'},'leave',{id:'task',processInstanceId:'bpm-real'}),null);assert.throws(()=>f({id:'bpm-real',businessKey:'10'},'dcc-controlled-file-revision',{id:'task',processInstanceId:'other'}));assert.throws(()=>f({id:'bpm-real',businessKey:'bad'},'dcc-controlled-file-upload',{id:'task',processInstanceId:'bpm-real'}))})
test('obsolete FormCenter task redirects to its exact file and signed task rather than its form business key',()=>{
 for(const key of ['dcc-controlled-file-obsolete','dcc-controlled-file-obsolete-approval']){
  const instance={id:'obsolete-bpm',businessKey:'FORM_ACTION:FORM-9',formVariables:{systemCode:'DCC',objectType:'CONTROLLED_FILE',actionCode:'OBSOLETE',objectId:'2054545668044084165',objectVersion:'A/1'}}
  const task={id:'approval-task',processInstanceId:'obsolete-bpm'}
  const location=c.exports.resolveDccTaskHandlingLocation(instance,key,task)
  assert.equal(location.path,'/dcc/controlled-file/detail/2054545668044084165');assert.equal(location.query.processInstanceId,'obsolete-bpm');assert.equal(location.query.taskId,'approval-task')
  assert.throws(()=>c.exports.resolveDccTaskHandlingLocation({...instance,formVariables:{...instance.formVariables,objectId:undefined}},key,task))
  assert.throws(()=>c.exports.resolveDccTaskHandlingLocation(instance,key,{...task,processInstanceId:'upload-bpm'}))
 }
})
test('actual generic BPM approve and reject handlers never send unsigned obsolete actions',async()=>{
 const vue=require('vue'),{parse}=require('vue/compiler-sfc')
 const script=parse(fs.readFileSync('src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue','utf8')).descriptor.scriptSetup.content
 const ast=ts.createSourceFile('buttons.ts',script,ts.ScriptTarget.Latest,true)
 const declaration=name=>ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name)).getText(ast)
 for(const pass of [true,false]){
  const routes=[],writes=[],state={exports:{},computed:vue.computed,ref:vue.ref,CONTROLLED_FILE_PROCESS_DEFINITION_KEY:'dcc-controlled-file-approval',
   props:{processDefinition:{key:'dcc-controlled-file-obsolete'},processInstance:{id:'obsolete-bpm',businessKey:'FORM_ACTION:F-9',formVariables:{systemCode:'DCC',objectType:'CONTROLLED_FILE',actionCode:'OBSOLETE',objectId:'20',objectVersion:'A/1'}}},
   runningTask:vue.ref({id:'approval-task',processInstanceId:'obsolete-bpm'}),formLoading:vue.ref(false),router:{push:async r=>routes.push(r)},
   message:{warning:()=>{},error:()=>{}},TaskApi:{approveTask:async r=>writes.push(r),rejectTask:async r=>writes.push(r)},...c.exports}
  vm.runInNewContext(ts.transpileModule(declaration('isDccControlledFileProcess')+'\n'+declaration('handleAudit')+'\nexports.run=handleAudit',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,state)
  await state.exports.run(pass,undefined);assert.equal(writes.length,0);assert.equal(routes.length,1);assert.equal(routes[0].query.taskId,'approval-task');assert.equal(state.formLoading.value,false)
 }
})
