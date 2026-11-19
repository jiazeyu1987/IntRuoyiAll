const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const vue=require('vue')
const {parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const transpile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const factsModule={};new Function('exports',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/facts.ts','utf8')))(factsModule)
const businessTimeModule={};new Function('exports',transpile(fs.readFileSync('src/utils/businessTime.ts','utf8')))(businessTimeModule)
const dateTimeModule={};new Function('exports','require',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/dateTimeDisplay.ts','utf8')))(dateTimeModule,id=>{assert.equal(id,'@/utils/businessTime');return businessTimeModule})
const changeModule={};new Function('exports','require',transpile(fs.readFileSync('src/views/dcc/registration-certificate/shared/changeDisplay.ts','utf8')))(changeModule,id=>{if(id==='./facts')return factsModule;throw Error('unexpected display import '+id)})
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
  if(id.endsWith('/dateTimeDisplay'))return dateTimeModule
  if(id.endsWith('/changeDisplay'))return changeModule
  if(id.endsWith('.vue'))return {default:{}}
  throw Error('unexpected import '+id)
 },()=>({success:value=>messages.push(['success',value]),error:value=>messages.push(['error',value])}))
 exports.default.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:()=>({children:[]}),createComment:()=>({children:[]}),createText:()=>({children:[]}),insert:(c,p)=>p.children.push(c),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const reactive=vue.reactive(props),app=renderer.createApp({render:()=>vue.h(exports.default,reactive)}),host=app.mount({children:[]})
 return {props:reactive,state:host.$.subTree.component.setupState,app,messages}
}

test('evidence reads only the selected version and exposes real grant download results',async()=>{
 const calls=[];const c=mount('history/EvidencePanel.vue',{certificateId:100,versionId:9},{getRegistrationCertificateEvidence:async(...args)=>{calls.push(args);return {files:[{id:1},{id:2}],snapshots:[],applications:[]}},getRegistrationCertificateFileDownloadGrantStatuses:async()=>[{businessFileId:1,canDownload:true},{businessFileId:2,canDownload:false}]})
 try{await drain();assert.deepEqual(calls,[[100,9]]);assert.equal(c.state.downloadable.has('1'),true);assert.equal(c.state.downloadable.has('2'),false)
 }finally{c.app.unmount()}
})
test('denied old version clears preceding content and reports actual denial',async()=>{
 const c=mount('history/EvidencePanel.vue',{certificateId:100,versionId:9},{getRegistrationCertificateEvidence:async(id,version)=>{if(version===8)throw Error('旧版查看授权不存在');return {files:[{id:1}],snapshots:[],applications:[]}},getRegistrationCertificateFileDownloadGrantStatuses:async()=>[]})
 try{await drain();assert.equal(c.state.evidence.files.length,1);c.state.inspect({detailJson:JSON.stringify({factsJson:JSON.stringify({certificateType:'FDA',ownProduct:false,classification:'II'})})});c.state.showMaterials=true;c.props.versionId=8;await drain();assert.equal(c.state.evidence.files.length,0);assert.equal(c.state.error,'旧版查看授权不存在');assert.equal(c.state.showSubmission,false);assert.equal(c.state.showMaterials,false);assert.equal(c.state.selected,undefined);assert.equal(c.state.submissionFacts,undefined);assert.equal(c.state.submissionDraft,undefined);assert.deepEqual(c.state.submission,{});assert.deepEqual(c.state.approval,{})
 }finally{c.app.unmount()}
})
test('historical submission displays frozen facts, readable supplemental and preserves exact review/file record',async()=>{
 const c=mount('history/EvidencePanel.vue',{certificateId:100,versionId:9},{getRegistrationCertificateEvidence:async()=>({files:[],snapshots:[],applications:[]})})
 try{await drain();const facts={certificateType:'CE',ownProduct:false,classification:'IIa',expiryDate:'2030-01-01'}
 const row={id:10,status:'REJECTED',reviewerName:'注册负责人',detailJson:JSON.stringify({draft:{factsJson:JSON.stringify(facts),modelSpecification:'当次型号'},approvalDate:'2026-10-30'}),approvalDetailJson:JSON.stringify({reviewerUserId:22,reviewComment:'补充型号'}),files:[{id:3,status:'REJECTED',originalName:'原批件.pdf'}]}
 c.state.inspect(row);assert.deepEqual(c.state.submissionFacts,facts);assert.equal(c.state.submissionDraft.modelSpecification,'当次型号');assert.equal(c.state.approval.reviewComment,'补充型号');assert.equal(c.state.selected.files[0].status,'REJECTED');assert.equal(c.state.showSubmission,true)
 const changed=c.state.formatRegistrationCertificateChangeValue('CERTIFICATE_INFORMATION',JSON.stringify({value:JSON.stringify({...facts,certificateHolder:'客户',certificateStatus:'SUSPENDED',ceCertificates:[{revision:'2',certificateNo:'CE002',issueDate:'2026-10-30'}]})}));assert.match(changed,/证件持证主体：客户/);assert.match(changed,/证书Rev.记录：Rev. 2 \/ 证书编号：CE002 \/ 签发日期：2026-10-30/);assert.match(changed,/证件状态：暂停/);assert.equal(c.state.formatRegistrationCertificateChangeValue('MODEL_SPECIFICATION',JSON.stringify({value:'型号2'})),'型号2');assert.doesNotMatch(changed,/factsJson|certificateStatus|SUSPENDED|\{"/)
 }finally{c.app.unmount()}
})
test('category date ranges, boolean combinations and reset preserve actual query values',async()=>{
 let searches=0;const query={certificateType:'FDA',pageNo:4,fieldFilters:{},sortField:'kpNumber',sortOrder:undefined,listingStart:'2026-01-01',listingEnd:'2026-12-31'}
 const c=mount('shared/CategoryFilters.vue',{query,onSearch:()=>searches++})
 try{assert.equal(c.state.dateFields.some(f=>f.start==='listingStart'),true);assert.equal(c.state.dateFields.some(f=>f.start==='expiryStart'),false)
  c.state.setFilter('customerHeld',false);c.state.setFilter('kpNumber','K0001');assert.equal(query.fieldFilters.customerHeld,'false');assert.equal(query.fieldFilters.kpNumber,'K0001')
  c.state.apply();assert.equal(query.pageNo,1);assert.equal(query.sortOrder,'asc');assert.equal(searches,1)
  c.state.clear();assert.equal(query.certificateType,undefined);assert.deepEqual(query.fieldFilters,{});assert.equal(query.listingStart,undefined);assert.equal(query.sortField,undefined);assert.equal(searches,2)
 }finally{c.app.unmount()}
})


test('all displayed category facts have sort options and changing category clears hidden filters',async()=>{
 const query={certificateType:'FDA',fieldFilters:{customerHeld:'true'},listingStart:'2026-01-01',sortField:'listingDate',expiryEnd:'2025-01-01'}
 const c=mount('shared/CategoryFilters.vue',{query})
 try{
  for(const field of ['ownProduct','certificateHolder','registrationLink','regulationCode','listingNumber','customerHeld','approvalLetterStatus','factoryRegistrationStatus','approvalDate','listingDate'])assert.equal(c.state.sorts.includes(field),true,field)
  c.props.query.certificateType='CE';c.state.changeType();await drain()
  assert.deepEqual(query.fieldFilters,{});assert.equal(query.listingStart,undefined);assert.equal(query.expiryEnd,undefined);assert.equal(query.sortField,undefined)
  for(const field of ['tradeName','notifiedBodyCode','ceRevision','ceCertificateNo','ceIssueDate','entrustedProduction','selfProduction','intendedUse'])assert.equal(c.state.sorts.includes(field),true,field)
  assert.equal(c.state.sorts.includes('listingDate'),false)
 }finally{c.app.unmount()}
})
