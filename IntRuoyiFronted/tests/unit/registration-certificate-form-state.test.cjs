const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const vue=require('vue')
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const transpile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const factsModule={};new Function('exports',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/facts.ts','utf8')))(factsModule)
const drain=async()=>{for(let i=0;i<30;i++)await Promise.resolve();await vue.nextTick()}
function mount(path,props,api={}){
 const source=fs.readFileSync('src/views/dcc/registration-certificate/'+path,'utf8')
 const script=compileScript(parse(source).descriptor,{id:path})
 const template=compileTemplate({id:path,source:parse(source).descriptor.template.content,compilerOptions:{bindingMetadata:script.bindings}});assert.deepEqual(template.errors,[])
 const exports={},messages=[]
 new Function('exports','require','useMessage',transpile(script.content))(exports,id=>{
  if(id==='vue')return vue
  if(id==='@/api/dcc/registrationCertificate')return api
  if(id==='@/utils')return {generateUUID:()=> 'test-key'}
  if(id.endsWith('/facts'))return factsModule
  if(id.endsWith('.vue'))return {default:{}}
  throw Error('unexpected import '+id)
 },()=>({success:value=>messages.push(['success',value]),error:value=>messages.push(['error',value])}))
 exports.default.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:()=>({children:[]}),createComment:()=>({children:[]}),createText:()=>({children:[]}),insert:(c,p)=>p.children.push(c),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const reactive=vue.reactive(props),app=renderer.createApp({render:()=>vue.h(exports.default,reactive)}),host=app.mount({children:[]})
 return {props:reactive,state:host.$.subTree.component.setupState,app,messages}
}
const domestic=()=>({certificateType:'DOMESTIC',ownProduct:false,certificateStatus:'VALID',classification:'II',certificateNo:'原证号',firstObtainedDate:'2024-01-01',approvalDate:'2024-01-01',effectiveDate:'2024-01-02',expiryDate:'2030-01-01',ceCertificates:[]})
const uploadApi=extra=>({getUploadOwnerCompanies:async()=>[{id:10,name:'公司'}],getUploadProjectCodes:async()=>[],getUploadEntrustedEnterprises:async()=>[],...extra})
function fillUpload(s,facts){s.facts=facts;s.companyId=10;s.productName='测试产品';s.entrustedProduction=false;s.selfProduction=true}
const raw=name=>({name,raw:new Blob([name],{type:'application/pdf'})})
test('real upload rejects FDA missing either required file and accepts its two distinct file kinds',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();c.state.facts.certificateType='FDA';await drain();fillUpload(c.state,{certificateType:'FDA',ownProduct:false,classification:'II',submissionType:'EXEMPT',customerHeld:false,approvalLetterStatus:'VALID',factoryRegistrationStatus:'ACTIVE',ceCertificates:[]})
  await c.state.submit();assert.match(c.state.error,/批准信函/);assert.equal(calls.length,0)
  c.state.files=[raw('letter.pdf')];await c.state.submit();assert.match(c.state.error,/产品描述/);assert.equal(calls.length,0)
  c.state.descriptions=[raw('description.pdf')];await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1)
  assert.equal(calls[0].getAll('file').length,1);assert.equal(calls[0].getAll('productDescriptionFiles').length,1);assert.equal(calls[0].get('entrustedProduction'),'false')
 }finally{c.app.unmount()}
})
test('CE revision certificate number and issue date remain independently optional in actual upload payload',async()=>{
 const calls=[],c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi({submitRegistrationCertificateUpload:async data=>calls.push(data)}))
 try{c.props.modelValue=true;await drain();c.state.facts.certificateType='CE';await drain();fillUpload(c.state,{certificateType:'CE',ownProduct:false,certificateStatus:'SUSPENDED',classification:'IIa',expiryDate:'2030-01-01',ceCertificates:[{revision:'1',certificateNo:'CE-1',issueDate:''},{revision:'2',certificateNo:'',issueDate:'2026-10-01'},{revision:'3',certificateNo:'',issueDate:''}]});c.state.files=[raw('ce.pdf')]
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
const current=()=>({certificateId:100,versionId:200,rowVersion:8,factsJson:JSON.stringify(domestic()),productName:'目前正式产品',modelSpecification:'型号',entrustedProduction:false,selfProduction:true,entrustedEnterprisesJson:'[]'})
test('rejected CHANGE request opens change dialog, not first upload form',async()=>{
 const c=mount('upload/MyApplications.vue',{}, {getMyRegistrationCertificateUploads:async()=>[]})
 try{await drain();c.state.retry({id:42,detailJson:JSON.stringify({operation:'CHANGE_CERTIFICATE'})});assert.equal(c.state.changing,true);assert.equal(c.state.editing,false);assert.equal(c.state.sourceId,42)
 }finally{c.app.unmount()}
})
test('rejected change restores submitted facts and values, retains approval file, submits latest row version',async()=>{
 const calls=[],facts={...domestic(),certificateNo:'拒绝当次证号'},frozen={operation:'CHANGE_CERTIFICATE',factsJson:JSON.stringify(facts),draft:{...current(),productName:'拒绝当次产品',modelSpecification:'拒绝当次未改型号'},structuredValues:{PRODUCT_NAME:'拒绝当次产品'},approvalDate:'2026-10-01',otherDescription:'拒绝当次说明'}
 const c=mount('change/ChangeDialog.vue',{modelValue:false,requestId:'42'},{getMyRegistrationCertificateUpload:async()=>({request:{status:'REJECTED',certificateId:100,detailJson:JSON.stringify(frozen)},files:[{id:7,fileKind:'CHANGE_APPROVAL',originalName:'批件.pdf'}]}),getRegistrationCertificateDetail:async()=>current(),getRegistrationCertificateEvidence:async()=>({files:[{fileKind:'REGISTRATION_CERTIFICATE',originalName:'已批准原证.pdf'},{fileKind:'CERTIFICATE_ATTACHMENT',originalName:'已批准附件.pdf'},{fileKind:'FDA_PRODUCT_DESCRIPTION',originalName:'已批准描述.pdf'}]}),submitRegistrationCertificateChange:async(id,data)=>calls.push([id,data])})
 try{c.props.modelValue=true;await drain();assert.equal(c.state.facts.certificateNo,'拒绝当次证号');assert.equal(c.state.values.PRODUCT_NAME,'拒绝当次产品');assert.equal(c.state.values.MODEL_SPECIFICATION,'拒绝当次未改型号');assert.equal(c.state.approvalDate,'2026-10-01');assert.deepEqual(c.state.currentFiles.map(f=>f.originalName),['已批准原证.pdf','已批准附件.pdf','已批准描述.pdf'])
  await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1);assert.equal(calls[0][1].get('sourceRequestId'),'42');assert.deepEqual(calls[0][1].getAll('retainedFileIds'),['7']);assert.equal(calls[0][1].get('expectedRowVersion'),'8');assert.equal(calls[0][1].get('file'),null);assert.equal(calls[0][1].get('structuredValues[PRODUCT_NAME]'),'拒绝当次产品')
 }finally{c.app.unmount()}
})

test('actual new upload type switch retains shared facts, clears domestic fields and FDA selected files',async()=>{
 const c=mount('upload/UploadDialog.vue',{modelValue:false},uploadApi())
 try{c.props.modelValue=true;await drain();fillUpload(c.state,{...domestic(),certificateHolder:'持有人',registrationLink:'https://example.com/certificate',remark:'共用备注'});c.state.files=[raw('domestic.pdf')];c.state.descriptions=[raw('old-description.pdf')]
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
 try{c.props.modelValue=true;await drain();fillUpload(c.state,{...domestic(),projectCode:'CUSTOMER-PRJ-01'});c.state.files=[raw('other.pdf')];await c.state.submit();assert.equal(c.state.error,'');assert.equal(calls.length,1);assert.equal(calls[0].get('projectCodeId'),null);assert.equal(calls[0].get('productMasterId'),null);assert.equal(JSON.parse(calls[0].get('factsJson')).projectCode,'CUSTOMER-PRJ-01')
 }finally{c.app.unmount()}
 const changeCalls=[],currentOther={...current(),factsJson:JSON.stringify({...domestic(),projectCode:'CUSTOMER-PRJ-01'})}
 const change=mount('change/ChangeDialog.vue',{modelValue:false,certificate:{certificateId:100}},{getRegistrationCertificateDetail:async()=>currentOther,getRegistrationCertificateEvidence:async()=>({files:[]}),submitRegistrationCertificateChange:async(id,data)=>changeCalls.push(data)})
 try{change.props.modelValue=true;await drain();assert.equal(change.state.facts.projectCode,'CUSTOMER-PRJ-01');change.state.facts.projectCode='CUSTOMER-PRJ-02';change.state.files=[raw('batch.pdf')];change.state.approvalDate='2026-10-30';await change.state.submit();assert.equal(change.state.error,'');assert.equal(changeCalls.length,1);assert.equal(JSON.parse(changeCalls[0].get('structuredValues[CERTIFICATE_INFORMATION]')).projectCode,'CUSTOMER-PRJ-02')
 }finally{change.app.unmount()}
})
