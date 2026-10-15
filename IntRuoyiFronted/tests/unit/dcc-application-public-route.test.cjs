const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const {parse}=require('vue/compiler-sfc')
const vue=require('vue')
const file=fs.readFileSync('src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue','utf8')
const ast=ts.createSourceFile('panel.ts',parse(file).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const open=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text==='open'))
function setup(blocked=false){
 const refs=['opened','loading','error','canSubmit','snapshot','selectedApplicationFile','approvalMatrixState','revisionOptions','selectedIterationId','selectedDepartments','selectedDistributionDepartments','departmentChoiceMissing','effectiveMode','effectiveDate','needTraining','description','approverNames','approvalAccounts','approvalGroups','approvalCapabilityLimitations','approvalRule','idempotencyKey','departments','preparationBlockers']
 const attributes={targetMarkets:['FDA'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}
 const saved={controlledFileId:'20',projectId:'5',applicationType:'REVISION',defaultSource:attributes,actual:attributes,selectedSignoffDepartmentIds:['51'],selectedDistributionDepartmentIds:[],effectiveMode:'PRESET_DATE',effectiveDate:'2099-10-08',needTraining:false,changeDescription:'正文修正',canSubmit:true,draftApprovalMatrix:require('./dcc-upload-test-loader.cjs').matrixFixture({categoryId:'10',approvalIds:['1']})}
 const calls=[];const route=[{stageCode:'MATRIX_REVIEW',candidateSourceType:'DEPT',candidateSourceIds:['51'],resolvedUserIds:['1']},{stageCode:'MATRIX_APPROVAL',resolvedUserIds:['1'],approveMethod:'ALL'}]
 const helpers=require('./dcc-upload-test-loader.cjs').loadUploadDepartments()
 const c={exports:{},...helpers,...require('./dcc-upload-test-loader.cjs').loadUploadMatrix(),getTenantId:()=> '1',getVisitTenantId:()=>undefined,busy:vue.ref(false),submitted:vue.ref(false),generation:0,props:{file:{id:'20',dccProjectCodeId:'5',categoryId:10,fileTypeTaxonomyId:'4',masterId:'30',revisionBaseActiveControlledFileId:'10'}},working:vue.ref(true),String,JSON,Error,Promise,
  getWorkingApplicationAttributes:async()=>saved,getSimpleDeptList:async()=>[{id:'51',name:'任务部门'}],getSimpleUserList:async()=>[{id:'1',nickname:'批准人'}],
  previewApprovalRoute:async()=>{throw Error('配置管理接口拒绝普通编制人')},
  checkControlledFileRouteReadiness:async req=>{calls.push(req);return require('./dcc-upload-test-loader.cjs').matrixReadiness({ready:!blocked,nodes:route,blockers:blocked?[{message:'默认会签账号缺少签名图片'}]:[]},{categoryId:'10',actionType:'REVISION',approvalIds:['1']})},
  getControlledFileRevisionOptions:async()=>({masterId:'30',controlledBaselineId:'10',baselineVersionNo:'A/1'}),generateUUID:()=> 'task-key'
 };for(const key of refs)c[key]=vue.ref(key==='preparationBlockers'?[]:undefined)
 vm.runInNewContext(ts.transpileModule(open.getText(ast)+'\nexports.open=open',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return {c,calls}
}
test('ordinary authorized compiler prepares through public business route without management rights',async()=>{
 const {c,calls}=setup();await c.exports.open()
 assert.equal(c.error.value,'');assert.equal(c.canSubmit.value,true)
 assert.equal(c.revisionOptions.value.baselineVersionNo,'A/1')
 assert.equal(calls.length,1);assert.equal(calls[0].categoryId,'10');assert.equal(calls[0].actionType,'REVISION');assert.equal(calls[0].controlledFileId,'20')
})
test('default route missing facts remain visible rather than being declared ready',async()=>{
 const {c}=setup(true);await c.exports.open()
 assert.equal(c.error.value,'');assert.equal(c.preparationBlockers.value[0],'默认会签账号缺少签名图片')
 assert.ok(file.includes('preparationBlockers.length'))
})
