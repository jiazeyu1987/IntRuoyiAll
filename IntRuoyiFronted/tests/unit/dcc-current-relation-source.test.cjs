const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const read=p=>fs.readFileSync(p,'utf8')
function mod(source,deps={}){const c={exports:{},Error,String,Number,BigInt,Object,Array,JSON,Promise,require:id=>id==='vue'?vue:deps[id]||(id.endsWith('.vue')?{}:(()=>{throw Error(id)})())};vm.runInNewContext(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const metadata={controlledFileId:'10',tenantId:'1',masterId:'100',projectId:'20',projectName:'项目',projectFolderId:'30',projectFolderName:'文件夹',fileName:'原件.docx',fileNumber:'SOP',versionNo:'A/1',status:'PENDING_MATRIX_REVIEW',controlled:false,pendingEffect:false,executable:false,canEdit:true,canPreview:false}
function mount(availability){
 const calls=[],desc=parse(read('src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue')).descriptor
 const relation=mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts'))
 const compile=compileScript(desc,{id:'current-source'}),component=mod(compile.content,{
 '@/utils/auth':{getTenantId:()=>1,getVisitTenantId:()=>null},
 '@/api/dcc/controlledFile/applicationRead':{getControlledFileRelationPermissions:async()=>({...metadata,hasCurrentControlledSource:availability}),loadDccSelectorPage:async()=>[]},
 '@/api/dcc/controlledFile/relations':{listCurrentRelations:async()=>{calls.push('current');return{sourceControlledFileId:'10',rowVersion:'0',files:[]}},listHistoricalRelations:async()=>{calls.push('history');return[]}},
 '@/api/dcc/controlledFile/projectAttributes':{getProjectFolders:async()=>[{id:'30',projectCodeId:'20',parentId:'0',name:'文件夹',active:true}]},
 '../basic-data/components/project-folder-tree':{buildProjectFolderTree:()=>[]},'../relations/project-reference-contract':{mapReferenceDirectoryNodes:()=>[]},'./relation-contract':relation
 }).default
 component.render=()=>vue.h('section')
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const props=vue.reactive({file:{id:'10',masterId:'100',dccProjectCodeId:'20',projectFolderId:'30',versionNo:'A/1',title:'原件.docx',fileNumber:'SOP'},allowEdit:true})
 const app=renderer.createApp({render:()=>vue.h(component,props)}),host=app.mount({children:[]})
 return{state:host.$.subTree.component.setupState,calls,app}
}
const drain=async()=>{for(let i=0;i<16;i++)await Promise.resolve();await vue.nextTick()}
test('actual false source availability skips current GET yet preserves actual selected history loading',async()=>{
 const h=mount(false);try{await drain();assert.deepEqual(h.calls,[]);assert.ok(h.state.selectedSource);assert.equal(h.state.current,undefined);assert.equal(h.state.currentSourceUnavailable,true);await h.state.loadHistory('10');assert.deepEqual(h.calls,['history'])}finally{h.app.unmount()}
})
test('actual true source availability keeps current GET even when selected version is not controlled',async()=>{
 const h=mount(true);try{await drain();assert.deepEqual(h.calls,['current']);assert.equal(h.state.current.rowVersion,'0');assert.equal(h.state.canEdit,true)}finally{h.app.unmount()}
})
test('formal relation-permission wrapper requires boolean source availability without selected-status inference',async()=>{
 let response={...metadata};const api=mod(read('src/api/dcc/controlledFile/applicationRead.ts'),{'@/config/axios':{default:{get:async()=>response}},'@/views/dcc/controlled-file/project-attributes/state':{validateAttributes:v=>v}})
 await assert.rejects(api.getControlledFileRelationPermissions('10'),/权限|事实|响应/)
 response={...metadata,hasCurrentControlledSource:false};assert.equal((await api.getControlledFileRelationPermissions('10')).hasCurrentControlledSource,false)
 response={...metadata,hasCurrentControlledSource:true};assert.equal((await api.getControlledFileRelationPermissions('10')).hasCurrentControlledSource,true)
})
test('actual unavailable-current template tells users to read the selected approval snapshot without rendering a fake empty set',()=>{
 const {descriptor}=parse(read('src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue'))
 const find=n=>{if(n.type===1&&n.tag==='el-alert'&&n.props.some(p=>p.type===6&&p.name==='title'&&p.value?.content.includes('无可用受控版本')))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const currentSourceUnavailable=true;</script>').descriptor,{id:'unavailable-controlled-source',inlineTemplate:true})
 const component=mod(compiled.content).default
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(component);app.component('el-alert',{props:['title'],setup:props=>()=>vue.h('p',props.title)})
 const root={children:[]};app.mount(root)
 try{assert.equal(root.children[0].text,'无可用受控版本；本次申请关联请查看本版本审批快照。')}finally{app.unmount()}
})
