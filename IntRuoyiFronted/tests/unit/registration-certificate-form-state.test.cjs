const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const vue=require('vue')
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const transpile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const factsModule={};new Function('exports',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/facts.ts','utf8')))(factsModule)
function loadDisplayModule(path){const out={};new Function('exports','require',transpile(fs.readFileSync(path,'utf8')))(out,id=>loadDisplayModule(id==='@/utils/businessTime'?'src/utils/businessTime.ts':'src/views/dcc/registration-certificate/shared/dateTimeDisplay.ts'));return out}
const pathModule=require('node:path')
const loadTs=file=>{const exports={};new Function('exports','require',transpile(fs.readFileSync(file,'utf8')))(exports,id=>loadTs(id.startsWith('@/')?'src/'+id.slice(2)+'.ts':pathModule.join(pathModule.dirname(file),id+'.ts')));return exports}
const drain=async()=>{for(let i=0;i<30;i++)await Promise.resolve();await vue.nextTick()}
function mount(path,props,api={}){
 const source=fs.readFileSync('src/views/dcc/registration-certificate/'+path,'utf8')
 const script=compileScript(parse(source).descriptor,{id:path})
 const template=compileTemplate({id:path,source:parse(source).descriptor.template.content,compilerOptions:{bindingMetadata:script.bindings}});assert.deepEqual(template.errors,[])
 const exports={},messages=[]
 new Function('exports','require','useMessage',transpile(script.content))(exports,id=>{
  if(id==='vue')return vue
  if(id==='@/api/dcc/registrationCertificate')return {getUploadEntrustedEnterprises:async()=>[],...api}
  if(id==='@/api/common/filePreview')return {buildDccRegistrationCertificatePreviewSource:businessFileId=>({type:'DCC_REGISTRATION_CERTIFICATE',businessFileId})}
  if(id==='@/utils')return {generateUUID:()=> 'test-key'}
  if(id.endsWith('/applicationDisplay'))return loadDisplayModule('src/views/dcc/registration-certificate/shared/applicationDisplay.ts')
  if(id.endsWith('/state')){const state={};new Function('exports',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/state.ts','utf8')))(state);return state}
  if(id.endsWith('/facts'))return factsModule
  if(id.startsWith('../shared/')&&!id.endsWith('.vue'))return loadTs('src/views/dcc/registration-certificate/shared/'+id.split('/').pop()+'.ts')
  if(id.endsWith('.vue'))return {default:{}}
  throw Error('unexpected import '+id)
 },()=>({success:value=>messages.push(['success',value]),error:value=>messages.push(['error',value])}))
 exports.default.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:()=>({children:[]}),createComment:()=>({children:[]}),createText:()=>({children:[]}),insert:(c,p)=>p.children.push(c),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const reactive=vue.reactive(props),app=renderer.createApp({render:()=>vue.h(exports.default,reactive)}),host=app.mount({children:[]})
 return {props:reactive,state:host.$.subTree.component.setupState,app,messages}
}
const domestic=()=>({certificateType:'DOMESTIC',ownProduct:false,certificateStatus:'VALID',classification:'II',certificateNo:'原证号',firstObtainedDate:'2024-01-01',effectiveDate:'2024-01-02',expiryDate:'2030-01-01',ceCertificates:[]})
const uploadApi=extra=>({getUploadOwnerCompanies:async()=>[{id:10,name:'公司'}],getUploadProjectCodes:async()=>[],getUploadEntrustedEnterprises:async()=>[],...extra})
async function fillUpload(s,facts){s.facts=facts;await drain();s.companyId=10;s.productName='测试产品';s.entrustedProduction=false;s.selfProduction=true}
const raw=name=>({name,raw:new Blob([name],{type:'application/pdf'})})

test('upload notifies its parent only after closing, and cancellation never navigates',async()=>{
 let saved=0
 const c=mount('upload/UploadDialog.vue',{modelValue:false,onSaved:()=>saved++},uploadApi({submitRegistrationCertificateUpload:async()=>{}}))
 try{
  c.props.modelValue=true;await drain();await fillUpload(c.state,domestic());c.state.files=[raw('certificate.pdf')]
  await c.state.submit();assert.equal(saved,0)
  c.state.handleClosed();assert.equal(saved,1)
  c.state.handleClosed();assert.equal(saved,1)
 }finally{c.app.unmount()}
 const cancelled=mount('upload/UploadDialog.vue',{modelValue:false,onSaved:()=>saved++},uploadApi({}))
 try{cancelled.state.handleClosed();assert.equal(saved,1)}finally{cancelled.app.unmount()}
})
test('real upload rejects FDA missing either required file and accepts its two distinct file kinds',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();c.state.facts.certificateType='FDA';await drain();await fillUpload(c.state,{certificateType:'FDA',ownProduct:false,classification:'II',submissionType:'EXEMPT',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE',ceCertificates:[]});await drain();c.state.companyId=10;c.state.productName='测试产品';c.state.entrustedProduction=false;c.state.selfProduction=true
  await c.state.submit();assert.match(c.state.error,/批准信函/);assert.equal(calls.length,0)
  c.state.files=[raw('letter.pdf')];await c.state.submit();assert.match(c.state.error,/产品描述/);assert.equal(calls.length,0)
  c.state.descriptions=[raw('description.pdf')];await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1)
  assert.equal(calls[0].getAll('file').length,1);assert.equal(calls[0].getAll('productDescriptionFiles').length,1);assert.equal(calls[0].get('entrustedProduction'),'false')
 }finally{c.app.unmount()}
})
test('CE revision certificate number and issue date remain independently optional in actual upload payload',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();c.state.facts.certificateType='CE';await drain();await fillUpload(c.state,{certificateType:'CE',ownProduct:false,certificateStatus:'SUSPENDED',classification:'IIa',expiryDate:'2030-01-01',ceCertificates:[{revision:'1',certificateNo:'CE-1',issueDate:''},{revision:'2',certificateNo:'',issueDate:'2026-10-01'},{revision:'3',certificateNo:'',issueDate:''}]});c.state.files=[raw('ce.pdf')]
  await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1)
  assert.deepEqual(JSON.parse(calls[0].get('factsJson')).ceCertificates,c.state.facts.ceCertificates)
 }finally{c.app.unmount()}
})
test('rejected upload prefill retains actual facts, company, production choices and selected original files',async()=>{
 const calls=[],facts=domestic(),c=mount('upload/UploadDialog.vue',{modelValue:false,requestId:'42'},uploadApi({getMyRegistrationCertificateUpload:async()=>({request:{status:'REJECTED',detailJson:JSON.stringify({factsJson:JSON.stringify(facts),ownerCompanyName:'原公司',draft:{productName:'原产品',ownerCompanyId:20,projectCodeId:null,entrustedProduction:false,selfProduction:true,entrustedEnterpriseIds:[]}})},files:[{id:1,fileKind:'REGISTRATION_CERTIFICATE',originalName:'原证.pdf'},{id:2,fileKind:'CERTIFICATE_ATTACHMENT',originalName:'原附件.pdf'}]}),submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();assert.deepEqual(c.state.facts,facts);assert.equal(c.state.productName,'原产品');assert.equal(c.state.companyId,20);assert.equal(c.state.selfProduction,true);assert.equal(c.state.retainedFiles.length,2)
  c.state.retainedFiles[1].retained=false;await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls[0].get('sourceRequestId'),'42');assert.deepEqual(calls[0].getAll('retainedFileIds'),['1']);assert.equal(calls[0].get('file'),null)
 }finally{c.app.unmount()}
})
test('new upload refuses removed certificate fields before calling the submission API',async()=>{
 for(const type of ['DOMESTIC','CE','FDA']){
  const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
  try{
   c.props.modelValue=true;await drain()
   const facts=type==='DOMESTIC'?domestic():type==='CE'?{certificateType:type,ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01'}:{certificateType:type,ownProduct:false,classification:'II',submissionType:'PMA',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE'}
   c.state.facts.certificateType=type;await drain();await fillUpload(c.state,facts);c.state.files=[raw('certificate.pdf')];c.state.descriptions=[raw('description.pdf')]
   for(const field of ['changeDescription',...(type!=='FDA'?['approvalDate']:[]),...(type==='CE'?['firstObtainedDate','modelSpecifications']:[])]){
    c.state.facts[field]=field==='modelSpecifications'?['A']:field.endsWith('Date')?'2025-01-01':'内容'
    await c.state.submit();assert.match(c.state.error,/新建.*不支持/);assert.equal(calls.length,0);delete c.state.facts[field]
   }
   await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1)
   const submitted=JSON.parse(calls[0].get('factsJson'));assert.equal(submitted.changeDescription,undefined)
   if(type!=='FDA')assert.equal(submitted.approvalDate,undefined)
   if(type==='CE'){assert.equal(submitted.firstObtainedDate,undefined);assert.equal(submitted.modelSpecifications,undefined);assert.equal(calls[0].get('firstObtainedDate'),null)}
  }finally{c.app.unmount()}
 }
})
const current=()=>({certificateId:100,versionId:200,rowVersion:8,factsJson:JSON.stringify(domestic()),productName:'目前正式产品',modelSpecification:'型号',entrustedProduction:false,selfProduction:true,entrustedEnterprisesJson:'[]'})
test('all certificate types submit actual field changes and optional notes without a change selector',async()=>{
 const source=fs.readFileSync('src/views/dcc/registration-certificate/change/ChangeDialog.vue','utf8')
 const template=parse(source).descriptor.template.content
 assert.doesNotMatch(template,/请选择变更内容|v-model="selected"/)
 assert.match(template,/<el-input v-model="values\[field.key\]"/)
 assert.match(template,/<el-form-item label="其他变更说明">/)
 for(const type of ['DOMESTIC','CE','FDA']){
  const original=type==='DOMESTIC'?domestic():type==='CE'?{certificateType:type,ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01',notifiedBodyCode:'0123'}:{certificateType:type,ownProduct:false,classification:'II',submissionType:'PMA',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE',changeDescription:'本次变更说明'}
  const calls=[],c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>({...current(),factsJson:JSON.stringify(original)}),getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>calls.push(data)})
  try{
   c.props.modelValue=true;await drain();c.state.files=[raw('change.pdf')];c.state.approvalDate='2026-11-01'
   c.state.other='  ';await c.state.submit();assert.match(c.state.error,/请至少修改一项内容/);assert.equal(calls.length,0)
   c.state.values.PRODUCT_NAME='修改后的产品';await c.state.submit();assert.equal(c.state.error,'');assert.deepEqual(calls[0].getAll('changeTypes'),['PRODUCT_NAME']);assert.equal(calls[0].get('structuredValues[PRODUCT_NAME]'),'修改后的产品');assert.equal(calls[0].get('otherDescription'),null)
   c.state.values.PRODUCT_NAME=current().productName;c.state.other='  其他变更事实  ';await c.state.submit();assert.equal(c.state.error,'');assert.deepEqual(calls[1].getAll('changeTypes'),['CERTIFICATE_INFORMATION','OTHER_CONTENT']);assert.equal(JSON.parse(calls[1].get('structuredValues[CERTIFICATE_INFORMATION]')).otherContentDescription,'其他变更事实');assert.equal(calls[1].get('otherDescription'),'其他变更事实')
   if(type==='CE'){
    c.state.facts.notifiedBodyCode=' ';await c.state.submit();assert.match(c.state.error,/请填写公告机构代码/);assert.equal(calls.length,2)
    c.state.facts.notifiedBodyCode='0456';await c.state.submit();assert.equal(c.state.error,'');assert.equal(JSON.parse(calls[2].get('structuredValues[CERTIFICATE_INFORMATION]')).notifiedBodyCode,'0456')
   }
  }finally{c.app.unmount()}
 }
})

test('rejected CHANGE request opens change dialog, not first upload form',async()=>{
 const c=mount('upload/MyApplications.vue',{}, {getMyRegistrationCertificateUploads:async()=>[]})
 try{await drain();c.state.retry({id:42,detailJson:JSON.stringify({operation:'CHANGE_CERTIFICATE'})});assert.equal(c.state.changing,true);assert.equal(c.state.editing,false);assert.equal(c.state.sourceId,42)
 }finally{c.app.unmount()}
})
test('rejected change restores submitted facts and values, retains approval file, submits latest row version',async()=>{
 const calls=[],facts=domestic(),frozen={operation:'CHANGE_CERTIFICATE',factsJson:JSON.stringify(facts),draft:{...current(),productName:'拒绝当次产品',modelSpecification:'拒绝当次未改型号'},structuredValues:{PRODUCT_NAME:'拒绝当次产品'},approvalDate:'2026-10-01',otherDescription:'拒绝当次说明'}
 const c=mount('change/ChangeDialog.vue',{modelValue:false,requestId:'42'},{getMyRegistrationCertificateUpload:async()=>({request:{status:'REJECTED',certificateId:100,detailJson:JSON.stringify(frozen)},files:[{id:7,fileKind:'CHANGE_APPROVAL',originalName:'批件.pdf'}]}),getRegistrationCertificateDetail:async()=>current(),getRegistrationCertificateEvidence:async()=>({files:[{fileKind:'REGISTRATION_CERTIFICATE',originalName:'已批准原证.pdf'},{fileKind:'CERTIFICATE_ATTACHMENT',originalName:'已批准附件.pdf'},{fileKind:'FDA_PRODUCT_DESCRIPTION',originalName:'已批准描述.pdf'}]}),submitRegistrationCertificateChange:async(id,data)=>calls.push([id,data])})
 try{c.props.modelValue=true;await drain();assert.equal(c.state.facts.certificateNo,'原证号');assert.equal(c.state.values.PRODUCT_NAME,'拒绝当次产品');assert.equal(c.state.values.MODEL_SPECIFICATION,'拒绝当次未改型号');assert.equal(c.state.other,'拒绝当次说明');assert.equal(c.state.approvalDate,'2026-10-01');assert.deepEqual(c.state.currentFiles.map(f=>f.originalName),['已批准原证.pdf','已批准附件.pdf','已批准描述.pdf'])
  await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1);assert.equal(calls[0][1].get('sourceRequestId'),'42');assert.deepEqual(calls[0][1].getAll('retainedFileIds'),['7']);assert.equal(calls[0][1].get('expectedRowVersion'),'8');assert.equal(calls[0][1].get('file'),null);assert.equal(calls[0][1].get('structuredValues[PRODUCT_NAME]'),'拒绝当次产品');assert.equal(calls[0][1].get('otherDescription'),'拒绝当次说明');assert.ok(calls[0][1].getAll('changeTypes').includes('OTHER_CONTENT'))
 }finally{c.app.unmount()}
})

test('actual new upload type switch retains shared facts, clears domestic fields and FDA selected files',async()=>{
 const c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi())
 try{c.props.modelValue=true;await drain();await fillUpload(c.state,{...domestic(),certificateHolder:'持有人',registrationLink:'https://example.com/certificate',remark:'共用备注'});c.state.files=[raw('domestic.pdf')];c.state.descriptions=[raw('old-description.pdf')]
  c.state.facts.certificateType='FDA';await drain();assert.equal(c.state.facts.classification,'II');assert.equal(c.state.facts.certificateHolder,'持有人');assert.equal(c.state.facts.remark,'共用备注');assert.equal(c.state.facts.certificateNo,undefined);assert.equal(c.state.facts.expiryDate,undefined);assert.equal(c.state.files.length,0);assert.equal(c.state.descriptions.length,0)
  c.state.facts.submissionType='PMA';c.state.facts.approvalDate='2026-01-01';c.state.facts.certificateType='CE';await drain();assert.equal(c.state.facts.submissionType,undefined);assert.equal(c.state.facts.approvalDate,undefined);assert.deepEqual(c.state.facts.ceCertificates,[])
 }finally{c.app.unmount()}
})

test('renewal rejection cannot open upload dialog just because its frozen submission has a draft',async()=>{
 const c=mount('upload/MyApplications.vue',{}, {getMyRegistrationCertificateUploads:async()=>[]})
 try{await drain();c.state.retry({id:43,detailJson:JSON.stringify({operation:'RENEWAL_CERTIFICATE',draft:current()})});assert.equal(c.state.editing,false);assert.equal(c.state.changing,false);assert.match(c.state.error,/续证/)}finally{c.app.unmount()}
})

test('normal change loads all current approved material names from the selected current version',async()=>{
 const calls=[],files=[{fileKind:'REGISTRATION_CERTIFICATE',originalName:'原证.pdf'},{fileKind:'CERTIFICATE_ATTACHMENT',originalName:'附件.pdf'},{fileKind:'FDA_PRODUCT_DESCRIPTION',originalName:'描述.pdf'}]
 const c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>current(),getRegistrationCertificateEvidence:async(...args)=>{calls.push(args);return {files}}})
 try{c.props.modelValue=true;await drain();assert.equal(c.state.error,'');assert.equal(c.state.detail.versionId,200);assert.deepEqual(calls,[[100,200]]);assert.deepEqual(c.state.currentFiles,files);assert.equal(c.state.retainedFiles.length,0)}finally{c.app.unmount()}
})

test('denied current approved materials do not leave a submittable partially loaded change',async()=>{
 const c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>current(),getRegistrationCertificateEvidence:async()=>{throw Error('当前材料无权查看')}})
 try{c.props.modelValue=true;await drain();assert.match(c.state.error,/当前材料无权查看/);assert.equal(c.state.detail,undefined);assert.deepEqual(c.state.currentFiles,[]);assert.equal(c.state.loading,false)}finally{c.app.unmount()}
})


test('Other domestic submits stated project code while keeping system association empty and reopens it for changes',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();await fillUpload(c.state,{...domestic(),projectCode:'CUSTOMER-PRJ-01'});c.state.files=[raw('other.pdf')];await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1);assert.equal(calls[0].get('projectCodeId'),null);assert.equal(calls[0].get('productMasterId'),null);assert.equal(JSON.parse(calls[0].get('factsJson')).projectCode,'CUSTOMER-PRJ-01')
 }finally{c.app.unmount()}
 const changeCalls=[],currentOther={...current(),factsJson:JSON.stringify({...domestic(),projectCode:'CUSTOMER-PRJ-01'})}
 const change=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>currentOther,getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>changeCalls.push(data)})
 try{change.props.modelValue=true;await drain();assert.equal(change.state.facts.projectCode,'CUSTOMER-PRJ-01');assert.ok(change.state.lockedFields.includes('projectCode'));change.state.values.PRODUCT_NAME='变更后产品';change.state.files=[raw('batch.pdf')];change.state.approvalDate='2026-10-30';await change.state.submit();assert.equal(change.state.error,'');assert.equal(changeCalls.length,1);assert.equal(changeCalls[0].get('structuredValues[CERTIFICATE_INFORMATION]'),null);assert.equal(changeCalls[0].get('structuredValues[PRODUCT_NAME]'),'变更后产品');assert.equal(change.state.facts.projectCode,'CUSTOMER-PRJ-01')
 }finally{change.app.unmount()}
})


test('task upload resolves catalog name and blocks submit while identity is loading or denied',async()=>{
 const pending=[],calls=[]
 const c=mount('upload/UploadDialog.vue',{modelValue:false,taskProject:{id:'349',projectCode:'11.9',projectName:'2026.11.9'}},uploadApi({getUploadProjectProduct:id=>new Promise((resolve,reject)=>pending.push({id,resolve,reject})),submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{
  c.props.modelValue=true;await drain();c.state.facts={...domestic(),ownProduct:true};await drain();assert.equal(pending[0].id,'349');assert.equal(c.state.productLoading,true)
  await c.state.submit();assert.match(c.state.error,/请先选择/);assert.equal(calls.length,0)
  pending[0].resolve({projectCodeId:'349',productCatalogId:'663',productCode:'0313170202',productName:'11.9测试',bindings:[]});await drain();assert.equal(c.state.productName,'11.9测试');assert.equal(c.state.facts.projectProduct.productName,'11.9测试')
  c.state.projectCodeIds=['350'];await drain();pending[1].reject(Error('项目绑定产品不可用'));await drain();await c.state.submit();assert.match(c.state.error,/项目绑定产品不可用/);assert.equal(calls.length,0)
 }finally{c.app.unmount()}
})

test('changing project ignores late product response and non-owned product retains editable name',async()=>{
 const pending=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({getUploadProjectProduct:id=>new Promise(resolve=>pending.push({id,resolve}))}))
 try{
  c.props.modelValue=true;await drain();c.state.facts.ownProduct=true;c.state.projectCodeIds=['1'];await drain();c.state.projectCodeIds=['2'];await drain()
  pending[1].resolve({projectCodeId:'2',productName:'新产品'});await drain();pending[0].resolve({projectCodeId:'1',productName:'旧产品'});await drain();assert.equal(c.state.productName,'新产品')
  c.state.facts.ownProduct=false;await drain();assert.equal(c.state.facts.projectProduct,undefined);c.state.productName='手填产品';assert.equal(c.state.productName,'手填产品')
 }finally{c.app.unmount()}
})

test('binding an existing project submits normally while the task entry submits its explicit task identity',async()=>{
 for(const handlingTaskId of [undefined,'22']){
  const calls=[],project={id:'356',projectCode:'PRJ-356',projectName:'小马宝莉云宝'}
  const c=mount('upload/UploadDialog.vue',{modelValue:false,...(handlingTaskId?{handlingTaskId,taskProject:project}:{})},uploadApi({getUploadProjectCodes:async()=>[project],getUploadProjectProduct:async()=>({projectCodeId:'356',productCatalogId:'663',productCode:'PRODUCT-1',productName:'云宝产品',bindings:[]}),submitRegistrationCertificateUpload:async data=>calls.push(data)}))
  try{
   c.props.modelValue=true;await drain();Object.assign(c.state.facts,domestic(),{ownProduct:true});c.state.companyId=10;c.state.entrustedProduction=false;c.state.selfProduction=true;c.state.projectCodeIds=['356'];await drain();c.state.files=[raw('registration.pdf')]
   await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1)
   assert.equal(calls[0].get('projectCodeId'),'356');assert.equal(calls[0].get('handlingTaskId'),handlingTaskId??null)
  }finally{c.app.unmount()}
 }
})
 test('direct CE model addition deletion and Rev number edits submit without selecting change content',async()=>{
 const calls=[],original={certificateType:'CE',ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01',modelSpecifications:['A'],ceCertificates:[{revision:'1',certificateNo:'CE-OLD'}]}
 const c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>({...current(),factsJson:JSON.stringify(original)}),getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>calls.push(data)})
 try{c.props.modelValue=true;await drain();c.state.facts.changeDescription='增加B型号';c.state.facts.modelSpecifications.push('B');c.state.facts.ceCertificates[0].certificateNo='CE-NEW';c.state.files=[raw('change.pdf')];c.state.approvalDate='2026-11-01';await c.state.submit();assert.equal(c.state.error,'');let sent=JSON.parse(calls[0].get('structuredValues[CERTIFICATE_INFORMATION]'));assert.deepEqual(sent.modelSpecifications,['A','B']);assert.equal(sent.classification,'IIa');assert.equal(sent.ceCertificates[0].certificateNo,'CE-NEW');assert.equal(c.state.selected,undefined)
 c.state.facts.changeDescription='删除A型号';c.state.facts.modelSpecifications.splice(0,1);await c.state.submit();sent=JSON.parse(calls[1].get('structuredValues[CERTIFICATE_INFORMATION]'));assert.deepEqual(sent.modelSpecifications,['B'])
 }finally{c.app.unmount()}
 })


test('upload binds multiple distinct products to one certificate',async()=>{
 let payload
 const product=id=>({projectCodeId:id,productCatalogId:id+100,productCode:'P'+id,productName:'产品'+id,bindings:[]})
 const c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({getUploadProjectProduct:async id=>product(id),submitRegistrationCertificateUpload:async data=>{payload=data}}))
 try{
  c.props.modelValue=true;await drain()
  c.state.facts={...domestic(),ownProduct:true};await drain()
  c.state.projectCodeIds=[11,12];await drain()
  c.state.companyId=10;c.state.selfProduction=true;c.state.entrustedProduction=false;c.state.files=[raw('certificate.pdf')]
  await c.state.submit()
  assert.ok(payload)
  assert.equal(payload.get('projectCodeId'),'11')
  assert.deepEqual(JSON.parse(payload.get('factsJson')).projectProducts.map(p=>p.productCatalogId),[111,112])
  assert.equal(payload.get('productName'),'产品11、产品12')
 }finally{c.app.unmount()}
})

test('upload blocks two project codes resolving to the same product',async()=>{
 let submitted=false
 const c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({getUploadProjectProduct:async id=>({projectCodeId:id,productCatalogId:100,productCode:'P100',productName:'同一个产品',bindings:[]}),submitRegistrationCertificateUpload:async()=>{submitted=true}}))
 try{
  c.props.modelValue=true;await drain()
  c.state.facts={...domestic(),ownProduct:true};await drain()
  c.state.projectCodeIds=[11,12];await drain()
  assert.match(c.state.productError,/同一产品/)
  await c.state.submit();assert.equal(submitted,false)
 }finally{c.app.unmount()}
})


test('existing certificate appends products using the current revision and keeps prior products',async()=>{
 const existing=[{projectCodeId:100,productCatalogId:10,productCode:'P10',productName:'甲',bindings:[]}]
 let submitted,saved=0
 const view=mount('components/ProductBindings.vue',{certificateId:1,rowVersion:3,products:existing,editable:true,onSaved:()=>saved++},{getUploadProjectCodes:async()=>[],getUploadProjectProduct:async id=>({projectCodeId:id,productCatalogId:20,productCode:'P20',productName:'乙',bindings:[]}),addRegistrationCertificateProducts:async(id,request)=>{submitted={id,request}}})
 await view.state.open();view.state.selected=[200];await drain()
 assert.equal(view.state.preview.length,1);assert.equal(view.state.error,'')
 await view.state.save()
 assert.deepEqual(submitted,{id:1,request:{expectedRowVersion:3,projectCodeIds:[200]}})
 assert.equal(saved,1);assert.equal(view.state.visible,false);assert.equal(existing.length,1)
 view.app.unmount()
})
test('append dialog rejects another project that resolves to an already associated product',async()=>{
 let calls=0
 const view=mount('components/ProductBindings.vue',{certificateId:1,rowVersion:3,products:[{projectCodeId:100,productCatalogId:10}],editable:true},{getUploadProjectCodes:async()=>[],getUploadProjectProduct:async id=>({projectCodeId:id,productCatalogId:10}),addRegistrationCertificateProducts:async()=>calls++})
 await view.state.open();view.state.selected=[200];await drain()
 assert.match(view.state.error,/已关联本注册证/);assert.deepEqual(view.state.preview,[]);assert.equal(calls,0)
 view.app.unmount()
})


test('FDA approval edits and new description files require a fresh explanation while status and link edits do not',async()=>{
 const original={certificateType:'FDA',ownProduct:false,classification:'II',submissionType:'PMA',kpNumber:'P00001',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE',changeDescription:'历史说明'}
 const calls=[],c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>({...current(),factsJson:JSON.stringify(original)}),getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>calls.push(data)})
 try{c.props.modelValue=true;await drain();assert.deepEqual(c.state.fields.map(f=>f.key),['PRODUCT_NAME']);c.state.files=[raw('letter-1.pdf'),raw('letter-2.pdf')];c.state.approvalDate=''
 c.state.facts.factoryRegistrationStatus='INACTIVE';c.state.facts.registrationLink='https://example.test/fda';await c.state.submit();assert.equal(c.state.error,'');if(calls.length)assert.equal(calls.at(-1).has('approvalDate'),false);assert.equal(calls.length,1);assert.equal(calls[0].getAll('additionalPrimaryFiles').length,1)
 c.state.facts.kpNumber='P00002';await c.state.submit();assert.match(c.state.error,/本次变更描述/);assert.equal(calls.length,1)
 c.state.facts.changeDescription='新P号替代原批准信息';await c.state.submit();assert.equal(c.state.error,'');if(calls.length)assert.equal(calls.at(-1).has('approvalDate'),false);assert.equal(calls.length,2)
 c.state.facts.kpNumber=original.kpNumber;c.state.facts.changeDescription=original.changeDescription;c.state.descriptions=[raw('models.pdf')];await c.state.submit();assert.match(c.state.error,/本次变更描述/)
 c.state.facts.changeDescription='新增规格沿用原P号';await c.state.submit();assert.equal(c.state.error,'');if(calls.length)assert.equal(calls.at(-1).has('approvalDate'),false);assert.equal(calls.length,3)
 }finally{c.app.unmount()}
})

test('CE model adjustment requires a new explanation but unrelated edits preserve an empty original model list',()=>{
 const before={certificateType:'CE',ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01',changeDescription:'历史说明'}
 assert.doesNotThrow(()=>factsModule.validateChangeFacts({...before,certificateStatus:'SUSPENDED'},before))
 assert.throws(()=>factsModule.validateChangeFacts({...before,modelSpecifications:['A']},before),/本次调整说明/)
 assert.doesNotThrow(()=>factsModule.validateChangeFacts({...before,modelSpecifications:['A'],changeDescription:'本次增加A'},before))
})

test('new upload accepts several certificate files and a stated name independent of associated catalog products',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({getUploadProjectProduct:async id=>({projectCodeId:id,productCatalogId:88,productCode:'P88',productName:'目录名称',bindings:[]}),submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();c.state.facts={...domestic(),ownProduct:true};await drain();c.state.projectCodeIds=[11];await drain();c.state.companyId=10;c.state.productName='证载产品中文名称';c.state.selfProduction=true;c.state.entrustedProduction=false;c.state.files=[raw('cert-1.pdf'),raw('cert-2.pdf')];await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls[0].get('productName'),'证载产品中文名称');assert.equal(calls[0].getAll('additionalPrimaryFiles').length,1);assert.equal(JSON.parse(calls[0].get('factsJson')).projectProducts[0].productName,'目录名称')
 }finally{c.app.unmount()}
})

test('domestic and CE changes require this approval date',async()=>{
 for(const type of ['DOMESTIC','CE']){
 const facts=type==='DOMESTIC'?domestic():{certificateType:'CE',ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01',ceCertificates:[]};
 const calls=[],c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>({...current(),factsJson:JSON.stringify(facts)}),getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>calls.push(data)});
 try{c.props.modelValue=true;await drain();c.state.values.PRODUCT_NAME='新产品';c.state.files=[raw('change.pdf')];await c.state.submit();assert.match(c.state.error,/批准日期/);assert.equal(calls.length,0);c.state.approvalDate='2026-11-26';await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls[0].get('approvalDate'),'2026-11-26')}finally{c.app.unmount()}
 }
})

test('change fields keep certificate facts separate and expose the correct domestic and CE supplements',async()=>{
 for(const type of ['DOMESTIC','CE']){
 const facts=type==='DOMESTIC'?domestic():{certificateType:'CE',ownProduct:false,classification:'IIa',certificateStatus:'VALID',expiryDate:'2030-01-01',modelSpecifications:['CE-A'],ceCertificates:[]};
 const c=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>({...current(),factsJson:JSON.stringify(facts)}),getRegistrationCertificateEvidence:async()=>({files:[]})});
 try{c.props.modelValue=true;await drain();const labels=c.state.fields.map(f=>f.label);assert.equal(labels.includes('注册人名称'),false);assert.equal(labels.includes('持证人名称'),false);if(type==='CE'){assert.equal(labels.includes('产品技术要求'),false);assert.ok(c.state.facts.modelSpecifications.includes('CE-A'));assert.ok(c.state.fields.some(f=>f.label==='持证人地址'))}else assert.ok(labels.includes('产品技术要求'))}finally{c.app.unmount()}
 }
})

test('material errors clear as files are chosen and retained originals satisfy the existing required rules',async()=>{
 const c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi())
 try{c.props.modelValue=true;await drain();c.state.facts.certificateType='FDA';await drain();await c.state.submit();assert.match(c.state.primaryMaterialError,/批准信函/);assert.match(c.state.descriptionMaterialError,/产品描述/)
 c.state.files=[raw('letter.pdf')];await drain();assert.equal(c.state.primaryMaterialError,'');assert.match(c.state.descriptionMaterialError,/产品描述/)
 c.state.retainedFiles=[{id:1,fileKind:'FDA_PRODUCT_DESCRIPTION',retained:true,originalName:'原描述.pdf'}];await drain();assert.equal(c.state.descriptionMaterialError,'')
 c.state.retainedFiles[0].retained=false;await drain();assert.match(c.state.descriptionMaterialError,/产品描述/)
 c.state.handleClosed();assert.equal(c.state.materialValidationAttempted,false);assert.equal(c.state.primaryMaterialError,'')
 }finally{c.app.unmount()}
 const changed=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>current(),getRegistrationCertificateEvidence:async()=>({files:[]})})
 try{changed.props.modelValue=true;await drain();await changed.state.submit();assert.match(changed.state.primaryMaterialError,/变更批件/)
 changed.state.retainedFiles=[{id:8,fileKind:'CHANGE_APPROVAL',retained:true,originalName:'原批件.pdf'}];await drain();assert.equal(changed.state.primaryMaterialError,'')
 }finally{changed.app.unmount()}
})
