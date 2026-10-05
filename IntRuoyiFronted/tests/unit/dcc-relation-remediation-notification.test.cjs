const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const read=p=>fs.readFileSync(p,'utf8'),sourceId='9007199254740993',relatedMaster='9007199254740995',origin='http://localhost:8061'
const params={fileNumber:'SOURCE-01',versionNo:'B/1',dueAt:'2026-10-20 12:00:00',relatedMasterId:relatedMaster,sourceControlledFileId:sourceId,detailUrl:'/dcc/controlled-file/detail/'+sourceId+'?viewer=1&from=notification'}
const message={templateCode:'dcc_relation_remediation',templateParams:params}
function mod(path){const c={exports:{},Error,URL,URLSearchParams,window:{location:{origin}},require:id=>id.endsWith('edhrWorkTaskNavigation')?{EDHR_WORK_TASK_NOTIFY_PATHS:new Set()}:id==='./dccOfflineTrainingRecord'?mod('src/utils/dccOfflineTrainingRecord.ts'):(()=>{throw Error(id)})()};vm.runInNewContext(ts.transpileModule(read(path),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const page='src/views/system/notify/my/MyNotifyMessageDetail.vue'
function declaration(name){const ast=ts.createSourceFile('notify.ts',parse(read(page)).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true);const n=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name));assert.ok(n,name);return n.getText(ast)}
test('actual existing sender payload resolves a typed readonly exact source target and navigation retains all IDs',async()=>{
 const h=mod('src/utils/notifyMessageNavigation.ts'),target=h.getNotifyMessageTarget(message);assert.ok(target,'existing remediation notification must have usable source entry');assert.equal(target.type,'dccRelationRemediation');assert.equal(target.targetId,sourceId);assert.equal(target.relatedMasterId,relatedMaster);assert.equal(target.dueAt,params.dueAt)
 const calls=[];await h.navigateToNotifyMessageTarget({push:async value=>calls.push(value)},target);assert.equal(calls[0].params.id,sourceId);assert.deepEqual(JSON.parse(JSON.stringify(calls[0].query)),{viewer:'1',from:'notification'})
})
test('exact template and complete tuple reject foreign, malformed, mismatched and writable URLs without another target branch',()=>{
 const h=mod('src/utils/notifyMessageNavigation.ts')
 for(const next of [{detailUrl:'https://foreign.example'+params.detailUrl},{detailUrl:params.detailUrl.replace(sourceId,relatedMaster)},{detailUrl:params.detailUrl+'&viewer=1'},{detailUrl:params.detailUrl+'&management=1'},{detailUrl:params.detailUrl+'#unsafe'},{sourceControlledFileId:9007199254740992},{relatedMasterId:'9223372036854775808'},{dueAt:'2026-02-30 12:00:00'},{fileNumber:''}])assert.equal(h.getNotifyMessageTarget({...message,templateParams:{...params,...next}}),null)
 assert.equal(h.getNotifyMessageTarget({templateCode:'another_template',templateParams:params}),null)
})
test('actual message handler and button close then navigate, with exact source/Master/deadline labels',async()=>{
 const h=mod('src/utils/notifyMessageNavigation.ts'),target=h.getNotifyMessageTarget(message),calls=[]
 const c={exports:{},computed:vue.computed,dccRelationRemediationNavigation:vue.ref(target),router:{push:async value=>calls.push(value)},navigateToNotifyMessageTarget:h.navigateToNotifyMessageTarget,resetDialogState:()=>calls.push('closed'),nextTick:vue.nextTick}
 vm.runInNewContext(ts.transpileModule(declaration('navigateToDccRelationRemediation')+'\nexports.open=navigateToDccRelationRemediation;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const {descriptor}=parse(read(page));const find=n=>{if(n.type===1&&n.tag==='el-button'&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-relation-remediation-notification-open'))return n;for(const ch of [...(n.children||[]),...(n.branches||[])]){const found=find(ch);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node);const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const dccRelationRemediationNavigation=__target;const navigateToDccRelationRemediation=__open;</script>').descriptor,{id:'actual-remediation-source-button',inlineTemplate:true})
 const render={exports:{},require:()=>vue,__target:target,__open:c.exports.open};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,render)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(render.exports.default);app.component('el-button',{setup:(_,{attrs,slots})=>()=>vue.h('button',attrs,slots.default?.())});const root={children:[]};app.mount(root)
 try{assert.equal(root.children[0].type,'button');await root.children[0].props.onClick();assert.equal(calls[0],'closed');assert.equal(calls[1].params.id,sourceId)}finally{app.unmount()}
 const labels={exports:{}};vm.runInNewContext(ts.transpileModule(declaration('templateParamLabels')+'\nexports.labels=templateParamLabels;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,labels)
 assert.equal(labels.exports.labels.sourceControlledFileId,'来源受控文件身份');assert.equal(labels.exports.labels.relatedMasterId,'关联文件 Master 身份');assert.equal(labels.exports.labels.dueAt,'整改期限')
})
