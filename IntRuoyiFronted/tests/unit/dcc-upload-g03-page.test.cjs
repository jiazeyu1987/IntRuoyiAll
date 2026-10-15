const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse,compileScript}=require('vue/compiler-sfc')
const root=path.resolve(__dirname,'../..'),page=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/index.vue'),'utf8')
function moduleFrom(code,requireModule=()=>{}){const c={exports:{},Error,require:requireModule};vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const matrix=moduleFrom(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/approval-matrix.ts'),'utf8'))
const departments=moduleFrom(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/signoff-departments.ts'),'utf8'))
function actualHandlers(){
 const calls=[],ref=vue.ref,scope='custom-page-scope',formData=vue.reactive({categoryId:'2',dccProjectCodeId:'5',fileTypeTaxonomyId:'6',processType:'CONTROLLED_FILE',changeType:'NEW',selectedSignoffDepartmentIds:[],selectedDistributionDepartmentIds:undefined,applicationCustomMatrix:null})
 const c={exports:{},Error,Promise,JSON,String,Number,...matrix,...departments,formData,
  userStore:{getUser:{id:'99',deptId:'51'}},getVisitTenantId:()=>undefined,getTenantId:()=>1,referenceIdentity:String,uploadFileTypeIdentity:String,createControlledFileUploadSessionId:()=>scope,
  selectedFileTypeTaxonomyPath:ref({approvalSourceMode:'APPLICATION_CUSTOM'}),isCustomApprovalSource:ref(true),isExternalReview:ref(false),
  approvalMatrixState:ref(),approvalMatrixScopeKey:'',routeReadinessRequestSeq:0,routeReadiness:ref(),routeReadinessError:ref(''),routeReadinessLoading:ref(false),routeReadinessSelectionKey:ref(''),signoffSelectionCategoryKey:departments.signoffCategoryKey('2','CONTROLLED_FILE'),
  resolveUploadErrorMessage:e=>e.message,
  checkControlledFileRouteReadiness:async request=>{calls.push(JSON.parse(JSON.stringify(request)));const complete=Boolean(request.applicationCustomMatrix?.approverUserId&&request.applicationCustomMatrix?.documentControlUserId&&request.selectedSignoffDepartmentIds?.length);return require('./dcc-upload-a32-fixture.cjs').response({tenantId:'1',projectId:'5',categoryId:'2',taxonomyId:'6',requesterId:'99',actionType:'NEW',applicationContextKey:scope},request.applicationCustomMatrix,request.selectedSignoffDepartmentIds,complete)}
 }
 const descriptor=parse(page).descriptor,ast=ts.createSourceFile('upload.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const named=name=>ast.statements.find(node=>ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>d.name.getText(ast)===name)).getText(ast)
 const start=page.indexOf('const ensureUploadApprovalMatrix ='),end=page.indexOf('const loadCurrentVersionByFileNumber =',start)
 vm.runInNewContext(ts.transpileModule(named('handleCustomMatrixChange')+'\n'+named('handleCustomDepartmentsChange')+'\n'+page.slice(start,end)+'\nexports.updateMatrix=handleCustomMatrixChange;exports.updateDepartments=handleCustomDepartmentsChange;exports.refresh=refreshRouteReadiness;exports.state=()=>approvalMatrixState.value;', {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
 return {formData,calls,c,...c.exports}
}
const settle=async()=>{await new Promise(resolve=>setImmediate(resolve));await vue.nextTick()}
test('actual upload page handlers preflight custom fields and copy distribution once without touching public baseline',async()=>{
 const h=actualHandlers();await h.refresh();assert.equal(h.c.routeReadiness.value.ready,false)
 h.updateMatrix({approverUserId:'77',documentControlUserId:'66'});h.updateDepartments(['51','52']);await settle()
 assert.equal(h.c.routeReadiness.value.ready,true);assert.deepEqual(JSON.parse(JSON.stringify(h.formData.selectedDistributionDepartmentIds)),['51','52'])
 h.updateDepartments(['52']);await settle();assert.deepEqual(JSON.parse(JSON.stringify(h.formData.selectedDistributionDepartmentIds)),['51','52'])
 const selected=h.state().selection('SAVE');assert.equal(selected.applicationCustomMatrixPhase,'PREVIEW');assert.equal(selected.applicationCustomMatrix.approverUserId,'77');assert.equal(selected.expectedApprovalRouteId,undefined);assert.equal(h.calls.at(-1).applicationCustomMatrix.documentControlUserId,'66')
 h.updateMatrix({approverUserId:null,documentControlUserId:'66'});await settle();assert.throws(()=>h.state().selection('SUBMIT'),/批准人/)
})
test('actual upload template contains the custom editor with one shared department model and source-gated public selectors',()=>{
 const descriptor=parse(page).descriptor,found=[];const walk=n=>{if(n.type===1&&n.tag==='CustomApplicationMatrixEditor')found.push(n);for(const c of n.children||[])walk(c)};walk(descriptor.template.ast)
 assert.equal(found.length,1);const block=found[0].loc.source
 const inputs=['formData','isExternalReview','isCustomApprovalSource','signoffDepartments','approvalUsers','submitLoading','signoffDepartmentsLoading','approvalUsersLoading','signoffDepartmentsError','approvalUsersError','handleCustomMatrixChange','handleCustomDepartmentsChange']
 const source='<template>'+block+'</template><script setup>const {'+inputs.join(',')+'}=defineProps('+JSON.stringify(inputs)+')</script>'
 const compiled=compileScript(parse(source).descriptor,{id:'actual-upload-custom-block',inlineTemplate:true});assert.ok(compiled.content.includes('handleCustomDepartmentsChange'))
 assert.ok(block.includes('formData.selectedSignoffDepartmentIds'));assert.ok(block.includes('isCustomApprovalSource'));assert.ok(!block.includes('publicMatrix'))
})
