const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = file => fs.readFileSync(file, 'utf8')
const page = read('src/views/dcc/controlled-file/upload/index.vue')
const ref = value => ({ value })
const section = (from,to) => { const a=page.indexOf(from),b=page.indexOf(to,a);assert.ok(a>0&&b>a);return page.slice(a,b) }
const evaluate=(source,state,name)=>{const c={exports:{},Error,JSON,String,Number,Promise,...state};vm.runInNewContext(ts.transpileModule(source+'\nexports.handler='+name,{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return{c,handler:c.exports.handler}}
const attr={targetMarkets:['NMPA'],otherMarket:null,licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N',transferTo:null}
function submitter(){const c={exports:{},Error,JSON,String,Number,BigInt,require:id=>{assert.equal(id,'../project-attributes/state');return{validateAttributes:a=>a}}};vm.runInNewContext(ts.transpileModule(read('src/views/dcc/controlled-file/upload/submitter.ts'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const draft={processType:'CONTROLLED_FILE',changeType:'NEW',directoryId:123,categoryId:7,dccProjectCodeId:'9007199254740993',projectFolderId:'9223372036854775700',projectFolderChangeReason:'本次归档',fileTypeTaxonomyId:'11',fileName:'REAL.SOP.docx',fileNumber:'N01',productMasterId:null,productCode:'REALPRODUCT',projectAttributes:attr,relatedControlledFileIds:[],needTraining:false,effectiveDate:'2099-01-01',versionNo:'A/1'}
test('actual normal NEW payload omits stale storage directory and retains the logical folder',()=>{
 const p=submitter().buildSubmitPayload(draft,{sessionId:'s',uploadTicket:'t',fileName:'REAL.SOP.docx'})
 assert.equal(Object.prototype.hasOwnProperty.call(p,'directoryId'),false)
 assert.equal(p.projectFolderId,'9223372036854775700')
 assert.equal(p.sourceFileName,'REAL.SOP.docx')
})
test('external and explicit revision retain the real provided storage identity',()=>{
 for(const change of [{processType:'EXTERNAL_REVIEW'},{changeType:'REVISION'}]){
  const p=submitter().buildSubmitPayload({...draft,...change},{sessionId:'s',uploadTicket:'t',fileName:'REAL.SOP.docx'})
  assert.equal(p.directoryId,123)
 }
})
test('actual NEW category resolution never loads a second NAS directory tree',async()=>{
 const calls=[],s={isExternalReview:ref(false),isNormalNewUpload:ref(true),fileTypeCategoryRequestSequence:0,formData:{fileTypeTaxonomyId:'11',categoryId:null},fileTypeCategoryError:ref(''),isFileTypeTaxonomyDepthValid:ref(true),availableCategories:ref([{id:7,active:true}]),resolveFileTypeActiveCategory:async()=>7,uploadFileTypeIdentity:String,applyDccProjectCodeProductNumber:()=>{},loadUploadDirectoryTree:async()=>calls.push('NAS'),resolveUploadErrorMessage:e=>e.message}
 await evaluate(section('const syncAutoCategoryFromSelectedFileTypeTaxonomy =','const hasTemporaryUploadState ='),s,'syncAutoCategoryFromSelectedFileTypeTaxonomy').handler()
 assert.equal(s.formData.categoryId,7)
 assert.equal(calls.length,0)
})
test('actual NAS form subtree is absent for NEW and retains its external control',()=>{
 const {descriptor}=parse(page)
 const find=n=>{if(n.type===1&&n.tag==='el-form-item'&&n.props.some(p=>p.type===6&&p.name==='label'&&p.value?.content==='提交目录'))return n;for(const c of n.children||[]){const f=find(c);if(f)return f}}
 const node=find(descriptor.template.ast);assert.ok(node)
 for(const external of [false,true]){
  const values={isExternalReview:external,isNormalNewUpload:!external,uploadDirectoryTree:{leafBinding:true,bindingDirectoryPath:'实际NAS根/叶子'},formData:{directoryId:123},selectedUploadDirectoryPath:'实际NAS根/叶子',directoryCascaderProps:{}}
  const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const values=__values;'+Object.keys(values).map(k=>'const '+k+'=values.'+k).join('\n')+'</script>').descriptor,{id:'normal-new-directory',inlineTemplate:true})
  const c={exports:{},require:()=>vue,__values:values};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
  const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
  const app=renderer.createApp(c.exports.default);app.component('el-form-item',{setup:(_p,ctx)=>()=>vue.h('section',ctx.attrs,ctx.slots.default?.())});app.component('el-cascader',{render:()=>vue.h('select')});const root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
  try{assert.equal(text(root).includes('实际NAS根/叶子'),external)}finally{app.unmount()}
 }
})
test('both actual working and submit wrappers reject caller storage ID for normal NEW before transport',async()=>{
 const source=read('src/api/dcc/controlledFile/workflow.ts'),ast=ts.createSourceFile('workflow.ts',source,ts.ScriptTarget.Latest,true),declarations=[]
 for(const st of ast.statements){if(ts.isVariableStatement(st)&&st.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&(d.name.text.startsWith('assert')||['DCC_FORBIDDEN_FILE_CAPABILITY_FIELDS','createWorkingControlledFile','submitControlledFile'].includes(d.name.text))))declarations.push(st.getText(ast));if(ts.isClassDeclaration(st)&&st.name?.text==='DccControlledFileContractError')declarations.push(st.getText(ast))}
 const calls=[],c={exports:{},Error,JSON,String,Number,BigInt,Object,Array,request:{post:async q=>{calls.push(q);return'9'}}};vm.runInNewContext(ts.transpileModule(declarations.join('\n'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
 const p={...draft,sessionId:'s',idempotencyKey:'i',originalUploadTicket:'t'};delete p.directoryId
 for(const name of ['createWorkingControlledFile','submitControlledFile']){
  await c.exports[name](p)
  const count=calls.length
  for(const directoryId of [null,undefined,0,123,p.projectFolderId])await assert.rejects(()=>c.exports[name]({...p,directoryId}))
  for(const processType of [undefined,null,'',' '])await assert.rejects(()=>c.exports[name]({...p,processType,directoryId:123}))
  assert.equal(calls.length,count)
  await c.exports[name]({...p,processType:'EXTERNAL_REVIEW',directoryId:123})
  await c.exports[name]({...p,changeType:'REVISION',directoryId:123})
  assert.equal(calls[calls.length-1].data.directoryId,123)
 }
})
