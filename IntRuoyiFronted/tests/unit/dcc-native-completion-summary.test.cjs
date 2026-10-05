const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')),ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=(name,required=true)=>{const node=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text===name));if(required)assert.ok(node,name);return node?node.getText(ast):''}
const file={id:'4032',versionNo:'A/2',status:'ACTIVE',processDefinitionKey:'dcc-controlled-file-revision',processInstanceId:'actual-revision-round',controlledTime:'2026-10-05T23:50:00',activatedTime:'2026-10-06T00:00:01',effectiveDate:'2026-10-06',currentActiveVersionNo:'A/2',publishedArtifactAvailable:true,stampedArtifactAvailable:true,versionHistory:[{id:'4026',versionNo:'A/1',status:'OBSOLETE',supersededByFileId:'4032',obsoletedTime:'2026-10-06T00:00:01'}]}
function host(value=file){
 const c={exports:{},String,computed:vue.computed,fileDetail:vue.ref({...value}),getDetailStatusLabel:status=>status,getVersionHistoryIdentityText:v=>v.versionNo+' / '+v.status,controlledBrowserDirectoryPath:vue.ref('实际目录'),publishVisibilityScopeText:vue.ref('正式授权范围')}
 const names=['buildNativePublishCompletionSummary','publishCompletionSummaryMode','supersededPredecessorVersions','isPublishCompletionSummaryVisible','publishCompletionSummaryTitle','publishCompletionSummaryDescription','publishCompletionSummaryItems']
 vm.runInNewContext(ts.transpileModule(names.map(n=>declaration(n,false)).join('\n')+'\nexports.summary=publishCompletionSummaryItems;exports.visible=isPublishCompletionSummaryVisible;exports.title=typeof publishCompletionSummaryTitle==="undefined"?null:publishCompletionSummaryTitle;exports.description=typeof publishCompletionSummaryDescription==="undefined"?null:publishCompletionSummaryDescription;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return{c,...c.exports}
}
test('actual native completion reports activated replacement and obsolete predecessor rather than SUPERSEDED',()=>{
 const h=host();assert.equal(h.visible.value,true);const items=h.summary.value
 assert.equal(items.some(i=>/SUPERSEDED/.test(i.label)),false)
 assert.ok(items.some(i=>/旧版.*作废/.test(i.label)&&i.value.includes('A/1')&&i.value.includes('OBSOLETE')))
 assert.ok(items.some(i=>/当前执行/.test(i.label)&&i.value.includes('A/2')&&i.ok===true))
 assert.ok(items.some(i=>i.description.includes('生效')))
})
test('actual native summary does not infer missing current execution or display pending/unknown as completed legacy',()=>{
 const missing=host({...file,currentActiveVersionNo:null});assert.equal(missing.summary.value.find(i=>i.key==='master-current').ok,false)
 assert.equal(host({...file,status:'CONTROLLED_PENDING_EFFECTIVE'}).visible.value,false)
 assert.equal(host({...file,processDefinitionKey:'unrecorded-process'}).visible.value,false)
 const legacy=host({...file,processDefinitionKey:'dcc-external-file-review',versionHistory:[{id:'4026',versionNo:'A/1',status:'SUPERSEDED',supersededByFileId:'4032'}]});assert.ok(legacy.summary.value.some(i=>i.label==='旧版 SUPERSEDED'&&i.ok))
})
test('actual completed panel renderer uses native text and current facts without hard-coded replaced legacy status',()=>{
 const find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-detail-publish-completion-summary'))return n;for(const child of [...(n.children||[]),...(n.branches||[])]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node);const h=host()
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const fileDetail={id:"4032"};const isPublishCompletionSummaryVisible=__host.visible;const publishCompletionSummaryTitle=__host.title;const publishCompletionSummaryDescription=__host.description;const publishCompletionSummaryItems=__host.summary;const openControlledBrowserLocation=()=>{};</script>').descriptor,{id:'native-completion-panel',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__host:h};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const remove=n=>{if(n?.parent){n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=null}}
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p,anchor)=>{remove(n);n.parent=p;const at=p.children.indexOf(anchor);at<0?p.children.push(n):p.children.splice(at,0,n)},remove,setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:n=>n?.parent||null,nextSibling:n=>n?.parent?.children[n.parent.children.indexOf(n)+1]||null})
 const app=renderer.createApp(c.exports.default);for(const tag of ['ContentWrap','el-button','el-tag','Icon'])app.component(tag,{setup:(_,{slots})=>()=>vue.h('span',slots.default?.())})
 const root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{assert.doesNotMatch(text(root),/SUPERSEDED/);assert.match(text(root),/旧版.*作废/);assert.match(text(root),/A\/1/);assert.match(text(root),/当前执行/)}finally{app.unmount()}
})
