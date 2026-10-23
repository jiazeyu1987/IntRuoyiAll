const { test }=require('node:test')
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const {parse}=require('vue/compiler-sfc')
const source=fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue','utf8')
const script=parse(source).descriptor.scriptSetup.content,ast=ts.createSourceFile('browser.ts',script,ts.ScriptTarget.Latest,true)
const handler=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)==='handleCheckin'))
function run(file){
  const errors=[],form={needTraining:false},context={exports:{},checkinSubmitting:{value:false},checkinCleanupLoading:{value:false},checkinUploadLoading:{value:false},
    checkinDrawingPdfLoading:{value:false},checkinTarget:{value:undefined},checkinForm:form,checkinDialogVisible:{value:false},
    checkinAttachmentState:{value:{ready:true,uploading:false,cleaning:false,error:'',count:0}},
    isValidBrowserOptionId:id=>String(id)==='900',isCheckedOutByCurrentUser:row=>row.checkedOutBy==='99',
    resetCheckinDialog(){context.checkinDialogVisible.value=false},message:{warning:message=>errors.push(message),error:message=>errors.push(message)},
    resolveBrowserErrorMessage:error=>error.message}
  const helperPath='src/views/dcc/controlled-file/browser/checkin-training.ts'
  if(fs.existsSync(helperPath))vm.runInNewContext(ts.transpileModule(fs.readFileSync(helperPath,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
  context.requireCheckinTrainingDefault=context.exports.requireCheckinTrainingDefault
  vm.runInNewContext(ts.transpileModule(handler.getText(ast)+'\nexports.open=handleCheckin',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
  context.exports.open({id:'900',checkedOutBy:'99',needTraining:true,remark:'saved',...file})
  return {context,form,errors}
}
test('actual checkin handler defaults a controlled UPLOAD source to unchecked training for the new REVISION',()=>{
  for(const status of ['ACTIVE','CONTROLLED_PENDING_EFFECTIVE']){
    const h=run({changeType:'NEW',status,versionNo:'A/1',controlledTime:'2026-10-01 12:00:00'})
    assert.equal(h.form.needTraining,false);assert.equal(h.context.checkinDialogVisible.value,true)
  }
})
test('actual handler keeps the saved selection for the same working or revision application',()=>{
  for(const row of [{changeType:'NEW',status:'WORKING',versionNo:'A/1-1'}, {changeType:'REVISION',status:'WORKING',versionNo:'A/1-2'},
    {changeType:'NEW',status:'REJECTED',versionNo:'A/1'}, {changeType:'REVISION',status:'WITHDRAWN',versionNo:'A/2'},
    {changeType:'REVISION',status:'ACTIVE',versionNo:'B/1',controlledTime:'2026-10-01 12:00:00'}]){
    const h=run(row);assert.equal(h.form.needTraining,true);assert.equal(h.context.checkinDialogVisible.value,true)
  }
})
test('unknown source identity or missing saved training is visible and cannot open the dialog',()=>{
  for(const row of [{status:'ACTIVE',versionNo:'A/1',controlledTime:'2026-10-01 12:00:00'},
    {changeType:'NEW',status:'ACTIVE',versionNo:'A/1'}, {changeType:'NEW',status:'ACTIVE',versionNo:'A/1-1',controlledTime:'2026-10-01 12:00:00'},
    {changeType:'NEW',status:'WORKING',versionNo:'A/1-1',needTraining:undefined}]){
    const h=run(row);assert.equal(h.context.checkinDialogVisible.value,false);assert.equal(h.errors.length,1)
  }
})
test('current option carries formal source facts and historical options never borrow the current row identity',()=>{
  const declaration=name=>ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)===name))
  const context={exports:{}}
  vm.runInNewContext(ts.transpileModule(declaration('buildCurrentVersionOption').getText(ast)+'\n'+declaration('hydrateCurrentBrowserVersionActionState').getText(ast)
    +'\nexports.build=buildCurrentVersionOption;exports.hydrate=hydrateCurrentBrowserVersionActionState',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
  const current={id:'900',title:'current',status:'ACTIVE',changeType:'NEW',controlledTime:'2026-10-01 12:00:00',needTraining:true}
  assert.equal(context.exports.build(current).changeType,'NEW');assert.equal(context.exports.build(current).controlledTime,current.controlledTime)
  const historical={id:'800',status:'WORKING',changeType:'REVISION',needTraining:true}
  const result=context.exports.hydrate(current,historical)
  assert.equal(result.changeType,'REVISION');assert.equal(result.controlledTime,undefined);assert.equal(result.needTraining,true)
})
