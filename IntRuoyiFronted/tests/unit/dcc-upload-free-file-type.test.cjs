const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const page = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
const ref = value => ({ value })
const block = (start, next) => { const from=page.indexOf(start), to=page.indexOf(next,from); assert.ok(from>0 && to>from); return page.slice(from,to) }
function execute(source, state, name) { const c={exports:{},Error,JSON,String,Number,Promise,...state}; vm.runInNewContext(ts.transpileModule(source+'\nexports.handler='+name,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c); return {c,handler:c.exports.handler} }
test('empty optional project template is not a core upload error and does not supply taxonomy', async () => {
 const reads=[]
 const s={isLibraryUpload:ref(false),formData:{dccProjectCodeId:'5'},projectFileTemplateLoading:ref(false),projectFileTemplateError:ref(''),projectFileTemplateItems:ref([]),fileTypeTaxonomies:ref([{id:'9007199254740993',active:true}]),getProjectCodeFileTemplate:async id=>{reads.push(id);return {items:[],taxonomyOptions:[]}},resolveUploadErrorMessage:e=>e.message,message:{error:()=>{}},projectTemplateRequestSequence:0}
 await execute(block('const loadProjectFileTemplate =','const applyDccProjectCodeProductNumber ='),s,'loadProjectFileTemplate').handler('5')
 assert.deepEqual(reads,['5'])
 assert.equal(s.projectFileTemplateError.value,'')
 assert.equal(s.fileTypeTaxonomies.value.length,1)
 assert.equal(s.projectFileTemplateLoading.value,false)
})
test('actual chosen file name is the source context before preview even without any template item', async () => {
 const calls=[],file={uid:1,name:'Arbitrary.SOP.docx',raw:{name:'Arbitrary.SOP.docx',type:'application/docx'}}
 const s={sourceUploadRequestSeq:0,isExternalReview:ref(false),formData:{fileName:'old-template',fileTypeTaxonomyId:'11',categoryId:2},validateControlledFileSelection:()=>({valid:true}),uploadRef:ref(),resetSelectedPreview:()=>{},cleanupCurrentUploadSession:async()=>true,resetDrawingPdfUpload:()=>{},resetAttachmentUploads:()=>{},clearSubmitFieldErrors:()=>{},submitFieldErrors:{},uploadPreviewError:ref(''),fileList:ref([]),previewUpload:ref(),previewFileBlob:ref(),uploadPreviewLoading:ref(false),uploadSessionBinding:ref(),uploadSessionId:'session-client',buildUploadPreviewContext:()=>{},uploadSubmitterService:{uploadPreview:async(raw,purpose,context)=>{calls.push({raw,purpose,context});return{fileName:raw.name,sessionId:'real-scoped-session',uploadTicket:'ticket'}}},cleanupStaleUploadResponse:async()=>{},message:{error:()=>{}},resolveUploadPreviewErrorMessage:e=>e.message,resetUploadNameLinkage:()=>{},clearCurrentVersionInfo:()=>{}}
 const {c,handler}=execute(block("const handleFileChange: UploadProps['onChange']",'const handleBeforeFileRemove:'),s,'handleFileChange')
 c.submitLoading=ref(false);c.projectAttributesLoading=ref(false);c.uploadSubmitted=ref(false)
 c.cleanupTemporaryUploadTicket=async()=>true
 c.captureUploadSelectionContext=()=>JSON.stringify([c.formData.categoryId,c.formData.fileTypeTaxonomyId])
 c.createControlledFileUploadSessionId=()=> 'new-independent-session'
 const helpers={exports:{},Error,require:()=>({validateAttributes:value=>value})}
 vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/submitter.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,helpers)
 c.validateUploadPreviewResponse=helpers.exports.validateUploadPreviewResponse
 const transport=c.uploadSubmitterService.uploadPreview
 c.uploadSubmitterService.uploadPreview=async(...args)=>({...await transport(...args),previewKind:'OFFICE'})
 c.buildUploadPreviewContext=()=>({fileName:c.formData.fileName,fileTypeTaxonomyId:'11',categoryId:2})
 await handler(file,[file])
 assert.equal(calls.length,1)
 assert.equal(calls[0].context.fileName,'Arbitrary.SOP.docx')
 assert.equal(c.formData.fileName,'Arbitrary.SOP.docx')
 assert.equal(c.previewUpload.value.fileName,'Arbitrary.SOP.docx')
})
test('actual name validator accepts a real selected source without preset template membership', () => {
 const from=page.indexOf('  fileName: [',page.indexOf('const formRules')),to=page.indexOf('  fileNumber:',from)
 const field=page.slice(from,to).trim().replace(/^fileName:/,'').replace(/,$/,'')
 const c={exports:{},isExternalReview:ref(false),projectFileTemplateItems:ref([]),selectedProjectTemplateItemId:ref(),formData:{fileTypeTaxonomyId:'11'},previewFileBlob:ref({name:'Arbitrary.SOP.docx'}),String,Error}
 vm.runInNewContext(ts.transpileModule('exports.rules='+field,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 let error
 c.exports.rules[0].validator({},'Arbitrary.SOP.docx',e=>{error=e})
 assert.equal(error,undefined)
})
test('actual form has one enabled-type selector and readonly actual-source name without mandatory stages',()=>{
 const {descriptor}=parse(page)
 const walk=(n,out=[])=>{if(n.type===1 && n.tag==='el-form-item')out.push(n);for(const c of n.children||[])walk(c,out);return out}
 const fields=walk(descriptor.template.ast),labels=n=>n.props.find(p=>p.type===6&&p.name==='label')?.value?.content
 assert.equal(fields.filter(n=>labels(n)==='阶段').length,0)
 const type=fields.find(n=>labels(n)==='文件类型');assert.ok(type)
 assert.match(type.loc.source,/formData\.fileTypeTaxonomyId/)
 assert.doesNotMatch(type.loc.source,/selectedProjectTemplateStageId|selectedProjectTemplateTypeId/)
 const name=fields.find(n=>labels(n)==='文件名称' && n.loc.source.includes('v-else'));assert.ok(name);assert.match(name.loc.source,/readonly/)
})
test('formal enabled-type paths preserve Longs and allow leaf selection without project preset rows',()=>{
 const c={exports:{},Error,String,Number,BigInt,Map,Set}
 vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/upload/file-type-options.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
 const rows=[{id:'1',parentId:0,name:'设计',active:true},{id:'2',parentId:'1',name:'报告',active:true},{id:'9007199254740993',parentId:'2',name:'任意报告',active:true}]
 const path=c.exports.buildUploadFileTypePaths(rows).get('9007199254740993')
 assert.equal(path.id,'9007199254740993');assert.equal(path.leaf,true);assert.equal(path.names.join(' / '),'设计 / 报告 / 任意报告')
 for(const invalid of [[{...rows[2],parentId:'8'}],[{...rows[0],id:9007199254740992}],rows.concat(rows[2]),[{...rows[0],active:false}]])assert.throws(()=>c.exports.buildUploadFileTypePaths(invalid))
})
test('actual type selection resolves the exact formal category and never takes the first unrelated category',async()=>{
 for(const response of ['7','8']){
  const calls=[],state={isExternalReview:ref(false),isNormalNewUpload:ref(true),fileTypeCategoryRequestSequence:0,formData:{fileTypeTaxonomyId:'9007199254740993',categoryId:null},fileTypeCategoryError:ref(''),isFileTypeTaxonomyDepthValid:ref(true),availableCategories:ref([{id:7,fileTypeTaxonomyId:'9007199254740993',active:true}]),resolveFileTypeActiveCategory:async id=>{calls.push(id);return response},uploadFileTypeIdentity:String,applyDccProjectCodeProductNumber:()=>{},loadUploadDirectoryTree:async id=>calls.push('directory:'+id),resolveUploadErrorMessage:e=>e.message}
  const {handler}=execute(block('const syncAutoCategoryFromSelectedFileTypeTaxonomy =','const hasTemporaryUploadState ='),state,'syncAutoCategoryFromSelectedFileTypeTaxonomy')
  await handler();assert.equal(calls[0],'9007199254740993')
  if(response==='7'){assert.equal(state.formData.categoryId,7);assert.equal(state.fileTypeCategoryError.value,'')}
  else {assert.equal(state.formData.categoryId,null);assert.match(state.fileTypeCategoryError.value,/唯一/);assert.equal(calls.length,1)}
 }
})
test('optional template failure stays auxiliary and preserves independently loaded core types',async()=>{
 const reads=[]
 const s={isLibraryUpload:ref(false),formData:{dccProjectCodeId:'5'},projectFileTemplateLoading:ref(false),projectFileTemplateError:ref(''),projectFileTemplateItems:ref([]),fileTypeTaxonomies:ref([{id:'11',active:true}]),getProjectCodeFileTemplate:async id=>{reads.push(id);throw Error('参考模板无法读取')},resolveUploadErrorMessage:e=>e.message,projectTemplateRequestSequence:0}
 await execute(block('const loadProjectFileTemplate =','const applyDccProjectCodeProductNumber ='),s,'loadProjectFileTemplate').handler('5')
 assert.deepEqual(reads,['5'])
 assert.match(s.projectFileTemplateError.value,/参考模板/);assert.equal(s.fileTypeTaxonomies.value.length,1)
 assert.equal(s.projectFileTemplateLoading.value,false)
})
test('actual single type form renders complete enabled path even without a project template',()=>{
 const {descriptor}=parse(page)
 const find=n=>{if(n.type===1 && n.tag==='el-form-item' && n.props.some(p=>p.type===6&&p.name==='label'&&p.value?.content==='文件类型'))return n;for(const c of n.children||[]){const found=find(c);if(found)return found}}
 const node=find(descriptor.template.ast),values={isExternalReview:false,formData:{dccProjectCodeId:'5',fileTypeTaxonomyId:'9007199254740993'},submitLoading:false,fileTypeOptionsLoading:false,categoryOptionsLoading:false,categoryOptionsError:'',fileTypeOptionsError:'',fileTypeCategoryError:'',uploadFileTypeOptions:[{id:'9007199254740993',names:['设计','报告','任意报告']},{id:'15',names:['通用文件','其他类型','其他'],approvalSourceMode:'APPLICATION_CUSTOM',uploadBlocker:null}],handleFileTypeTaxonomyChange:()=>{}}
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const values=__values;'+Object.keys(values).map(k=>'const '+k+'=values.'+k).join('\n')+'</script>').descriptor,{id:'actual-single-type',inlineTemplate:true})
 const env={exports:{},require:()=>vue,__values:values};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,env)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(c,p)=>p.children.push(c),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(env.exports.default)
 for(const tag of ['el-form-item','el-select','el-alert'])app.component(tag,{setup:(_p,c)=>()=>vue.h(tag,c.attrs,c.slots.default?.())})
 app.component('el-option',{props:['label','value','disabled'],setup:p=>()=>vue.h('option',{'value':p.value,disabled:p.disabled},p.label)})
 const root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{
  assert.match(text(root),/设计 \/ 报告 \/ 任意报告/)
  assert.match(text(root),/通用文件 \/ 其他类型 \/ 其他/)
  const options=[];const visit=n=>{if(n.type==='option')options.push(n);for(const c of n.children||[])visit(c)};visit(root)
  assert.equal(Boolean(options.find(n=>n.props.value==='15').props.disabled),false)
  const context={exports:{},computed:fn=>({get value(){return fn()}}),selectedFileTypeTaxonomyPath:{value:values.uploadFileTypeOptions[1]}}
  vm.runInNewContext(ts.transpileModule(block('const isCustomApprovalSource =','const handleCustomMatrixChange =')+'\nexports.custom=isCustomApprovalSource',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
  assert.equal(context.exports.custom.value,true)
 }finally{app.unmount()}
})

test('file information keeps ordered fields, paired inputs, readonly matches and separate supplementary uploads', () => {
 const {descriptor}=parse(page)
 const walk=n=>[n,...(n.children||[]).flatMap(walk)]
 const attr=(n,key)=>n.props?.find(p=>p.type===6&&p.name===key)?.value?.content
 const nodes=walk(descriptor.template.ast)
 const section=nodes.find(n=>attr(n,'data-testid')==='dcc-upload-section-file')
 assert.ok(section)
 const fields=walk(section).filter(n=>n.tag==='el-form-item')
 assert.deepEqual(fields.map(n=>attr(n,'prop')).filter(Boolean),[
  'fileName','fileName','fileTypeTaxonomyId','categoryId','categoryId','fileNumber','versionNo','productCode','projectFolderChangeReason'
 ])
 assert.deepEqual(fields.filter(n=>!attr(n,'prop')).map(n=>attr(n,'label')),['关联文件','附件'])
 const pairs=walk(section).filter(n=>attr(n,'class')==='upload-file-pair')
 assert.equal(pairs.length,2)
 assert.deepEqual(walk(pairs[0]).filter(n=>n.tag==='el-form-item').map(n=>attr(n,'prop')),['fileNumber','versionNo'])
 assert.deepEqual(walk(pairs[1]).filter(n=>n.tag==='el-form-item').map(n=>attr(n,'label')),['关联文件','附件'])
 const type=fields.find(n=>attr(n,'prop')==='fileTypeTaxonomyId')
 assert.ok(walk(type).some(n=>n.tag==='el-select'&&attr(n,'class')==='!w-full'))
 const category=fields.find(n=>attr(n,'prop')==='categoryId'&&n.loc.source.includes('v-else'))
 assert.match(category.loc.source,/selectedCategory\?\.name/)
 assert.match(category.loc.source,/自动匹配/)
 assert.ok(!walk(category).some(n=>['el-input','el-select'].includes(n.tag)))
 const number=fields.find(n=>attr(n,'prop')==='fileNumber')
 for(const key of ['currentVersionLookupLoading','currentVersionLookupError','currentVersionInfo.fileName','currentVersionInfo.currentVersionNo',
  'currentVersionInfo.status','currentVersionInfo.modifying','currentVersionProjectionBlockReason','isRequestedVersionDuplicate',
  'originalFilePath','sourceFilePath','stampedFilePath','publishedFilePath']) assert.ok(number.loc.source.includes(key),key)
 const product=fields.find(n=>attr(n,'prop')==='productCode')
 assert.ok(!walk(product).some(n=>n.tag==='el-input'))
 const details=walk(product).find(n=>n.tag==='details')
 assert.ok(details&&!details.props.some(p=>p.name==='open'))
 assert.match(details.loc.source,/查看详情/)
 for(const key of ['source','productMasterId','productCatalogId','productRelationId','productCreateRequestId']) assert.ok(details.loc.source.includes(key),key)
 assert.match(product.loc.source,/projectProduct.productName/)
 const reason=fields.find(n=>attr(n,'prop')==='projectFolderChangeReason')
 assert.ok(walk(reason).some(n=>n.tag==='el-input'&&attr(n,'maxlength')==='500'))
 assert.match(reason.loc.source,/选填/)
 assert.match(section.loc.source,/ref="attachmentUploadRef"/)
 assert.match(section.loc.source,/:on-remove="handleAttachmentRemove"/)
 assert.doesNotMatch(section.loc.source,/ref="uploadRef"/)
})
