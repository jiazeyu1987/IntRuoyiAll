const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse,compileScript,compileTemplate}=require('vue/compiler-sfc')
const filename='src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue'
function moduleFrom(code,require=()=>{}){const c={exports:{},require,Error,BigInt,Set,String};vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
async function mount(confirm=async()=>true){const {descriptor}=parse(fs.readFileSync(filename,'utf8')),script=compileScript(descriptor,{id:'attachments'}),template=compileTemplate({source:descriptor.template.content,filename,id:'attachments',compilerOptions:{bindingMetadata:script.bindings}});assert.deepEqual(template.errors,[]);const calls=[],saves=[],props=vue.reactive({file:{id:'20',attachments:[{attachmentId:'700',controlledFileId:'20',fileName:'notes.pdf',canDownload:true}],canDownload:true}})
 const component=moduleFrom(script.content,id=>{
  if(id==='vue')return vue
  if(id.endsWith('/auth'))return {getTenantId:()=> '31',getVisitTenantId:()=> undefined}
  if(id.endsWith('/user'))return {useUserStoreWithOut:()=>({getUser:{id:'99'}})}
  if(id==='element-plus')return {ElMessageBox:{confirm}}
  if(id.endsWith('/workflow'))return {downloadControlledFileAttachmentWithName:async(...args)=>{calls.push(args);return {blob:new Blob(['data'],{type:'application/pdf'}),fileName:'actual.pdf'}}}
  if(id.endsWith('/filePreview'))return {buildDccControlledFileAttachmentPreviewSource:(controlledFileId,attachmentId)=>({type:'DCC_CONTROLLED_FILE_ATTACHMENT',controlledFileId,attachmentId})}
  if(id==='./attachment-facts')return moduleFrom(fs.readFileSync('src/views/dcc/controlled-file/detail/attachment-facts.ts','utf8'))
  if(id.endsWith('/filt'))return {downloadByData:(...args)=>saves.push(args)}
  if(id.endsWith('.vue'))return {}
  throw Error(id)
 }).default;component.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp({render:()=>vue.h(component,props)}),host=app.mount({children:[]});await vue.nextTick();return {app,props,calls,saves,state:host.$.subTree.component.setupState}
}
test('shared attachment preview uses exact version/attachment source and real snapshots',async()=>{const h=await mount();try{h.state.preview(h.props.file.attachments[0]);assert.equal(h.state.previewVisible,true);assert.equal(h.state.previewSource.controlledFileId,'20');assert.equal(h.state.previewSource.attachmentId,'700');h.state.preview({...h.props.file.attachments[0],controlledFileId:'21'});assert.ok(h.state.actionError.includes('不属于'))}finally{h.app.unmount()}})
test('no download permission prevents request even when handler is invoked',async()=>{const h=await mount();try{h.props.file.attachments[0].canDownload=false;await h.state.download(h.props.file.attachments[0]);assert.equal(h.calls.length,0);assert.ok(h.state.actionError.includes('不可下载'))}finally{h.app.unmount()}})

test('historical attachment capability is independent from main-body download',async()=>{const h=await mount();try{h.props.file.canDownload=false;h.props.file.status='OBSOLETE';h.props.file.versionNo='A/1';await vue.nextTick();await h.state.download(h.props.file.attachments[0]);assert.equal(h.calls.length,1);assert.equal(h.calls[0][0],'20');assert.equal(h.calls[0][1],'700');assert.equal(h.saves.length,1)}finally{h.app.unmount()}})

test('a main-body grant never substitutes for denied or missing attachment capability',async()=>{for(const capability of [false,undefined]){const h=await mount();try{h.props.file.canDownload=true;h.props.file.attachments[0].canDownload=capability;await vue.nextTick();await h.state.download(h.props.file.attachments[0]);assert.equal(h.calls.length,0);assert.equal(h.saves.length,0);assert.match(h.state.actionError,/不可下载/)}finally{h.app.unmount()}}})

test('download button binds the explicit attachment projection',()=>{const {descriptor}=parse(fs.readFileSync(filename,'utf8'));const button=descriptor.template.content.match(/<el-button\s+v-if="([^"]+)"[^>]*@click="download\(row\)"/);assert.ok(button);assert.equal(button[1],'row.canDownload === true')})
test('download warning cancellation or version change before confirm prevents request',async()=>{const canceled=await mount(async()=>{throw 'cancel'});try{await canceled.state.download(canceled.props.file.attachments[0]);assert.equal(canceled.calls.length,0);assert.equal(canceled.state.actionError,'')}finally{canceled.app.unmount()}
 let accept;const h=await mount(()=>new Promise(r=>accept=r));try{const pending=h.state.download(h.props.file.attachments[0]);h.props.file={id:'21',attachments:[],canDownload:true};await vue.nextTick();accept();await pending;assert.equal(h.calls.length,0)}finally{h.app.unmount()}})
test('authorized download saves backend filename and refresh clears old preview/error',async()=>{const h=await mount();try{await h.state.download(h.props.file.attachments[0]);assert.equal(h.calls[0][0],'20');assert.equal(h.calls[0][1],'700');assert.equal(h.saves[0][1],'actual.pdf');h.state.preview(h.props.file.attachments[0]);h.props.file={id:'21',attachments:[],canDownload:false};await vue.nextTick();assert.equal(h.state.previewVisible,false);assert.equal(h.state.selected,undefined);assert.equal(h.state.attachments.length,0)}finally{h.app.unmount()}})
test('same object identity mutation or permission revocation invalidates old attachment preview immediately',async()=>{const h=await mount();try{h.state.preview(h.props.file.attachments[0]);h.props.file.id='21';await vue.nextTick();assert.equal(h.state.previewVisible,false);assert.equal(h.state.selected,undefined)}finally{h.app.unmount()}})
