const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const source=fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')
const {descriptor}=parse(source)
const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=name=>{const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text===name));assert.ok(st);return st.getText(ast)}
function host({viewer=false,management=true,role=true,projection=true}={}){
 const warnings=[],c={exports:{},computed:vue.computed,viewerMode:vue.ref(viewer),showDetailManagementActions:vue.ref(management),userStore:{getRoles:['actual-role']},fileDetail:vue.ref({id:'10'}),hasMetadataEditorRole:()=>role,hasDccControlledFileActionProjection:()=>projection,metadataDialogVisible:vue.ref(false),message:{warning:value=>warnings.push(value)}}
 vm.runInNewContext(ts.transpileModule(declaration('canEditMetadata')+'\n'+declaration('openMetadataDialog')+'\nexports.open=openMetadataDialog;exports.canEdit=canEditMetadata',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 c.canEditMetadata=c.exports.canEdit
 return{c,warnings,open:c.exports.open}
}
test('actual metadata computed and handler deny viewer/readonly trace while retaining management role/projection checks',()=>{
 for(const flags of [{viewer:true},{management:false},{role:false},{projection:false}]){const h=host(flags);assert.equal(h.c.canEditMetadata.value,false);h.open();assert.equal(h.c.metadataDialogVisible.value,false)}
 const h=host();assert.equal(h.c.canEditMetadata.value,true);h.open();assert.equal(h.c.metadataDialogVisible.value,true)
})
test('actual viewer basic-info caller passes no edit capability even with metadata role and action facts',()=>{
 const find=n=>{if(n.type===1&&n.tag==='ControlledFileBasicInfoPanel'&&n.props.some(p=>p.type===6&&p.name==='edit-test-id'&&p.value?.content==='dcc-controlled-preview-detail-edit'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 const h=host({viewer:true})
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const fileDetail={id:"10"};const categoryNameMap=new Map();const directoryNameMap=new Map();const userNameMap=new Map();const canEditMetadata=__canEdit;const openDccProjectCode=()=>{};const openMetadataDialog=()=>{};</script>').descriptor,{id:'readonly-preview-edit-gate',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__canEdit:h.c.canEditMetadata.value};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);let seen
 app.component('ControlledFileBasicInfoPanel',{props:['showEdit'],setup:props=>{seen=props.showEdit;return()=>vue.h('section')}});app.mount({children:[]})
 try{assert.equal(seen,false)}finally{app.unmount()}
})
