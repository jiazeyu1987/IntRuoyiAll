const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm')
const ts=require('typescript'),vue=require('vue'),{parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const filename='src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue'
const departmentHelpers=require('./dcc-upload-test-loader.cjs').loadUploadDepartments()
const matrix=(id,taxonomy,account)=>({formatVersion:'DCC_APPROVAL_MATRIX_BASELINE_V2',tenantId:'1',categoryId:taxonomy==='6'?'2':'3',fileTypeTaxonomyId:taxonomy,
 routeId:id,routeVersionNo:1,actionType:'LEGACY',matrixDigest:(id==='32'?'a':'b').repeat(64),nodes:[{id:'node-'+id,stageType:'APPROVAL',subjectName:account,subjectLabel:account}]})
const preview=()=>({controlledFileId:'4206',sourceAttributeRound:1,sourceCategoryId:'2',sourceTaxonomyId:'6',sourceSha256:'c'.repeat(64),sourceMatrixDigest:'a'.repeat(64),
 targetCategoryId:'3',targetTaxonomyId:'8',targetTypePath:'Documents / Output / C10 / Config / T2',beforeMatrix:matrix('32','6','Original approver'),targetMatrix:matrix('44','8','Target approver'),
 selectedSignoffDepartmentIds:['51'],selectedDistributionDepartmentIds:['52'],readiness:{ready:true,nodes:[],blockers:[]}})
async function mount(port={}){
 const {descriptor}=parse(fs.readFileSync(filename,'utf8')),script=compileScript(descriptor,{id:'c10-type-change'})
 const template=compileTemplate({source:descriptor.template.content,filename,id:'c10-type-change',compilerOptions:{bindingMetadata:script.bindings}})
 assert.deepEqual(template.errors,[])
 const calls={preview:[],confirm:[],reload:0,messages:[]};let sequence=0
 const dummy={render:()=>null},noop=()=>{},attributes={targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}
 const deps={
  vue,'element-plus':{ElMessageBox:{confirm:async()=>true}},'@/utils':{generateUUID:()=>`request-${++sequence}`},
  '@/utils/auth':{getTenantId:()=> '1',getVisitTenantId:()=>undefined},
  '@/api/system/dept':{getSimpleDeptList:async()=>[]},'@/api/system/user':{getSimpleUserList:async()=>[]},
  '@/api/dcc/controlledFile/projectAttributes':{getProjectDefaults:async()=>attributes},
  '@/api/dcc/controlledFile/applicationRead':{getWorkingApplicationAttributes:async()=>({controlledFileId:'4206',applicationType:'UPLOAD',actual:attributes,
   effectiveMode:'AFTER_STANDARDIZATION_SIGNATURE',effectiveDate:null,needTraining:false,selectedSignoffDepartmentIds:['51'],selectedDistributionDepartmentIds:['52'],changeDescription:'Saved description',...port.saved})},
  '@/api/dcc/controlledFile/workflow':{
   previewWorkingApplicationTypeChange:async(id,r)=>{calls.preview.push({id,request:JSON.parse(JSON.stringify(r))});return port.preview?port.preview(id,r):{...preview(),
    ...(r.selectedSignoffDepartmentIds?{selectedSignoffDepartmentIds:r.selectedSignoffDepartmentIds}:{}),...(r.selectedDistributionDepartmentIds?{selectedDistributionDepartmentIds:r.selectedDistributionDepartmentIds}:{})}},
   confirmWorkingApplicationTypeChange:async(id,r)=>{calls.confirm.push({id,request:JSON.parse(JSON.stringify(r))});return port.confirm?port.confirm(id,r):{...preview(),selectedSignoffDepartmentIds:r.selectedSignoffDepartmentIds,selectedDistributionDepartmentIds:r.selectedDistributionDepartmentIds}}},
  '@/api/dcc/controlledFile/fileTypeTaxonomies':{getFileTypeTaxonomyUploadOptions:async()=>[{id:'8',name:'T2',levelNo:5,parentId:null,active:true,approvalSourceMode:'PUBLIC_MATRIX',uploadBlocker:null}]},
  '../upload/approval-matrix':{isNewActionBaselinePreparationAllowed:()=>false},
  '../upload/submitter':{buildUploadEffectiveChoice:(mode,date)=>({effectiveMode:mode,effectiveDate:date}),resolveUploadTodayDate:()=> '2026-10-18'},
  '../shared/version-display':{getDccVersionDisplayText:v=>v},
  '@/api/common/filePreview':{},
  '../revision/revision-model':{},'../workflow/workflow-actions':{isWorkflowId:id=>/^\d+$/.test(String(id))},
  '../upload/signoff-departments':departmentHelpers,
  '../project-attributes/state':{validateAttributes:v=>v},'./presentation':{formatControlledFileDateTime:v=>v}}
 const context={exports:{},require:id=>{if(id.endsWith('.vue'))return{default:dummy};if(Object.hasOwn(deps,id))return deps[id];throw Error('Unexpected import '+id)},
  Error,String,Number,BigInt,Set,Map,Promise,JSON,structuredClone,window:{location:{reload:()=>calls.reload++}},
  useMessage:()=>({success:m=>calls.messages.push(m),warning:m=>calls.messages.push(m),error:m=>calls.messages.push(m)})}
 vm.runInNewContext(ts.transpileModule(script.content,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
 const component=context.exports.default,render={exports:{},require:()=>vue}
 vm.runInNewContext(ts.transpileModule(template.code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,render);component.render=render.exports.render
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:text=>({text}),
  insert:(n,p,a)=>{if(n.parent){const i=n.parent.children.indexOf(n);if(i>=0)n.parent.children.splice(i,1)}n.parent=p;const i=a?p.children.indexOf(a):-1;i<0?p.children.push(n):p.children.splice(i,0,n)},
  remove:n=>{if(n.parent)n.parent.children.splice(n.parent.children.indexOf(n),1)},setText:(n,t)=>n.text=t,setElementText:(n,t)=>{n.text=t;n.children=[]},
  patchProp:(n,k,_o,v)=>n.props[k]=v,parentNode:n=>n.parent,nextSibling:n=>n.parent?.children[n.parent.children.indexOf(n)+1]||null})
 const props=vue.reactive({file:{id:'4206',status:'WORKING',changeType:'NEW',dccProjectCodeId:'5',categoryId:'2',fileTypeTaxonomyId:'6',predecessorControlledFileId:'4197',fileName:'D.pdf',versionNo:'A/1-1'}})
 const root={children:[]},app=renderer.createApp({render:()=>vue.h(component,props)});app.config.warnHandler=noop;app.directive('loading',{})
 for(const tag of ['ContentWrap','el-button','el-alert','el-form','el-form-item','el-select','el-option','el-input','el-descriptions','el-descriptions-item','el-tag','el-radio-group','el-radio','el-collapse','el-collapse-item'])
  app.component(tag,{setup:(_p,c)=>()=>vue.h(tag,c.attrs,c.slots.default?.())})
 app.component('el-dialog',{props:['modelValue'],setup:(p,c)=>()=>p.modelValue?vue.h('dialog',c.attrs,[c.slots.default?.(),c.slots.footer?.()]):null})
 const host=app.mount(root),state=host.$.subTree.component.setupState
 state.opened=true;state.snapshot={projectId:'5',applicationType:'UPLOAD',approvalSourceMode:'PUBLIC_MATRIX',actual:attributes,defaultSource:attributes}
 state.selectedDepartments=['51'];state.selectedDistributionDepartments=['52'];state.effectiveMode='AFTER_STANDARDIZATION_SIGNATURE';state.effectiveDate=null;state.needTraining=false;state.description='Saved description'
 if(state.typeChangeFormKey)state.savedTypeChangeFormKey=state.typeChangeFormKey()
 await vue.nextTick()
 const find=id=>{const visit=n=>{if(n.props?.['data-testid']===id)return n;for(const c of n.children||[]){const got=visit(c);if(got)return got}return null};return visit(root)}
 const click=async id=>{const node=find(id);assert.ok(node,'Actual rendered control '+id+' must exist');await node.props.onClick();await vue.nextTick()}
 return{app,state,calls,click,find,props}
}

test('actual panel preview/cancel never confirms and retains other unsaved inputs',async()=>{
 const h=await mount();try{
  h.state.description='Unsaved note to retain';await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview')
  assert.equal(h.calls.preview.length,2);assert.equal(h.calls.confirm.length,0)
  await h.click('dcc-working-type-change-cancel');assert.equal(h.state.typeChangeVisible,false)
  assert.equal(h.state.description,'Unsaved note to retain');assert.equal(h.calls.confirm.length,0);assert.equal(h.calls.reload,0)
 }finally{h.app.unmount()}
})
test('actual confirmation sends exact source and target expectations, reason and independent lists',async()=>{
 const h=await mount();try{
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview')
  assert.deepEqual(Array.from(h.state.typeChangeDistribution),['51'],'A new type starts distribution from its own default signoff, not old 52')
  h.state.typeChangeDistribution=['52'];await h.click('dcc-working-type-change-preview');h.state.typeChangeReason='Correct the original file type'
  await h.click('dcc-working-type-change-confirm');assert.equal(h.calls.confirm.length,1)
  const sent=h.calls.confirm[0];assert.equal(sent.id,'4206');assert.equal(sent.request.confirmed,true);assert.equal(sent.request.expectedSourceAttributeRound,1)
  assert.equal(sent.request.expectedSourceCategoryId,'2');assert.equal(sent.request.expectedSourceTaxonomyId,'6');assert.equal(sent.request.expectedSourceSha256,'c'.repeat(64))
  assert.equal(sent.request.expectedApprovalRouteId,'44');assert.equal(sent.request.expectedApprovalMatrixDigest,'b'.repeat(64));assert.equal(sent.request.targetFileTypeTaxonomyId,'8')
  assert.deepEqual(sent.request.selectedSignoffDepartmentIds,['51']);assert.deepEqual(sent.request.selectedDistributionDepartmentIds,['52'])
  assert.equal(sent.request.changeReason,'Correct the original file type');assert.ok(sent.request.requestId);assert.equal(h.calls.reload,1)
 }finally{h.app.unmount()}
})
test('actual server rejection remains visible with reason, target and original inputs preserved',async()=>{
 const h=await mount({confirm:async()=>{throw Error('WORKING_TYPE_CHANGE_TARGET_MATRIX_EXPECTATION_STALE')}});try{
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview');h.state.typeChangeReason='Keep my reason'
  await h.click('dcc-working-type-change-confirm');assert.match(h.state.typeChangeError,/TARGET_MATRIX_EXPECTATION_STALE/)
  assert.equal(h.state.typeChangeVisible,true);assert.equal(h.state.typeChangeReason,'Keep my reason');assert.equal(h.state.typeChangeTarget,'8')
  assert.equal(h.state.description,'Saved description');assert.equal(h.calls.reload,0)
  await vue.nextTick();assert.ok(h.find('dcc-working-type-change-error'),'Rendered visible error must remain in the dialog')
 }finally{h.app.unmount()}
})
test('cancelling an in-flight readonly preview ignores its late result and never confirms',async()=>{
 let finish;const h=await mount({preview:()=>new Promise(resolve=>finish=resolve)});try{
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';const pending=h.state.loadTypeChangePreview();await vue.nextTick()
  await h.click('dcc-working-type-change-cancel');finish(preview());await pending;await vue.nextTick()
  assert.equal(h.state.typeChangeVisible,false);assert.equal(h.state.typeChangePreview,undefined);assert.equal(h.calls.confirm.length,0);assert.equal(h.calls.reload,0)
 }finally{h.app.unmount()}
})
test('confirmation cannot discard other unsaved fields while cancellation retains them',async()=>{
 const h=await mount();try{
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview');h.state.typeChangeReason='Change type'
  h.state.description='Another unsaved note';await h.click('dcc-working-type-change-confirm')
  assert.equal(h.calls.confirm.length,0);assert.match(h.state.typeChangeError,/先保存/);assert.equal(h.state.description,'Another unsaved note')
  await h.click('dcc-working-type-change-cancel');assert.equal(h.state.description,'Another unsaved note')
 }finally{h.app.unmount()}
})
test('actual entry stays absent for ACTIVE, REVISION and application-custom scopes',async()=>{
 for(const scope of ['ACTIVE','REVISION','APPLICATION_CUSTOM']){
  const h=await mount();try{
   if(scope==='ACTIVE')h.props.file.status='ACTIVE'
   else if(scope==='REVISION')h.props.file.changeType='REVISION'
   else h.state.snapshot={...h.state.snapshot,approvalSourceMode:'APPLICATION_CUSTOM'}
   await vue.nextTick();assert.equal(h.find('dcc-working-type-change-open'),null,scope)
   assert.equal(h.calls.preview.length,0);assert.equal(h.calls.confirm.length,0)
  }finally{h.app.unmount()}
 }
})
test('saved roster order normalization does not invent unsaved edits or block real confirmation',async()=>{
 const h=await mount({saved:{selectedSignoffDepartmentIds:['52','51'],selectedDistributionDepartmentIds:['54','53']}});try{
  h.state.selectedDepartments=['51','52'];h.state.selectedDistributionDepartments=['53','54']
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview');h.state.typeChangeReason='Explicit type correction'
  await h.click('dcc-working-type-change-confirm');assert.equal(h.calls.confirm.length,1);assert.equal(h.calls.reload,1)
 }finally{h.app.unmount()}
})
test('changing the target discards old preview and both target lists without touching original fields',async()=>{
 const h=await mount();try{
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview')
  h.state.typeChangeTarget='9';h.state.invalidateTypeChangePreview();await vue.nextTick()
  assert.equal(h.state.typeChangePreview,undefined);assert.equal(h.state.typeChangeSignoff.length,0);assert.equal(h.state.typeChangeDistribution.length,0)
  assert.deepEqual(Array.from(h.state.selectedDepartments),['51']);assert.deepEqual(Array.from(h.state.selectedDistributionDepartments),['52'])
  assert.equal(h.calls.confirm.length,0)
 }finally{h.app.unmount()}
})
test('changed roster membership remains unsaved and cannot trigger a confirming write or reload',async()=>{
 const h=await mount();try{
  h.state.selectedDepartments=['51','52']
  await h.click('dcc-working-type-change-open');h.state.typeChangeTarget='8';await h.click('dcc-working-type-change-preview');h.state.typeChangeReason='Change type'
  await h.click('dcc-working-type-change-confirm')
  assert.equal(h.calls.confirm.length,0);assert.equal(h.calls.reload,0);assert.match(h.state.typeChangeError,/先保存/)
  assert.deepEqual(Array.from(h.state.selectedDepartments),['51','52'])
 }finally{h.app.unmount()}
})
