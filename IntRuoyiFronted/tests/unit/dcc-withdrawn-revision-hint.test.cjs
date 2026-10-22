const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8')),ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const node=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text==='detailHandlingSummary'));assert.ok(node)
const current={id:'4043',status:'WITHDRAWN',processDefinitionKey:'dcc-controlled-file-revision',processInstanceId:'actual-withdrawn-bpm',supersededByFileId:null}
function host(file=current,scope={fileId:'4043',bpmRound:'actual-withdrawn-bpm',applicationType:'REVISION'},stage={stageCode:'MATRIX_REVIEW',stageName:'会签'}){
 const helperSource=fs.readFileSync('src/views/dcc/controlled-file/shared/handlingSummary.ts','utf8'),helper={exports:{},require:()=>({})};vm.runInNewContext(ts.transpileModule(helperSource,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,helper)
 const c={exports:{},computed:vue.computed,fileDetail:vue.ref(file),approvalProgressScope:vue.ref(scope),currentStage:vue.ref(stage),displayStageProgressList:vue.ref([]),getControlledFileHandlingSummary:helper.exports.getControlledFileHandlingSummary}
 vm.runInNewContext(ts.transpileModule(node.getText(ast)+'\nexports.summary=detailHandlingSummary;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return{c,summary:c.exports.summary}
}
test('actual native withdrawn summary leads to existing checkout/checkin repair even when an old stage remains visible',()=>{
 const h=host();assert.match(h.summary.value.nextStep,/检出.*检入.*修正.*重新提交/);assert.match(h.summary.value.responsibilityHint,/重新提交仍申请原目标版本/);assert.doesNotMatch(JSON.stringify(h.summary.value),/删除流程/)
})
test('legacy, missing native identity and already superseded withdrawn facts retain their original helper behavior',()=>{
 for(const file of [{...current,processDefinitionKey:'dcc-controlled-file-approval'},{...current,processInstanceId:''}])assert.equal(host(file,null,null).summary.value.nextStep,'已撤回，可删除流程或重新提交')
 assert.equal(host({...current,supersededByFileId:'4045'},null,null).summary.value.nextStep,'已重新提交，查看新流程')
})
test('actual handling summary template shows the original native repair guidance without adding actions',()=>{
 const h=host(),find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-detail-handling-summary'))return n;for(const ch of [...(n.children||[]),...(n.branches||[])]){const found=find(ch);if(found)return found}}
 const element=find(descriptor.template.ast);assert.ok(element)
 const compiled=compileScript(parse('<template>'+element.loc.source+'</template><script setup>const fileDetail={id:"4043"};const approvalTodoTask=null;const showDetailManagementActions=true;const detailHandlingSummary=__summary;const currentStageLabel="已撤回";const detailBlockingReason="-";</script>').descriptor,{id:'actual-withdrawn-summary',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__summary:h.summary};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default),root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
 try{assert.match(text(root),/检出.*检入/);assert.doesNotMatch(text(root),/删除流程/);assert.equal(root.children[0].type,'div')}finally{app.unmount()}
})
test('actual withdrawn menu renders only the separately server-permitted legacy actions and no native or missing-projection actions',()=>{
 const projection={exports:{}};vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/api/form-center/actionProjection.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,projection)
 const lifecycle={exports:{},require:()=>projection.exports};vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/shared/lifecycle.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,lifecycle)
 const declaration=name=>{const n=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name));return n?n.getText(ast):''}
 const find=(n,command)=>{if(n.type===1&&n.tag==='el-dropdown-item'&&n.props.some(p=>p.type===6&&p.name==='command'&&p.value?.content===command))return n;for(const ch of [...(n.children||[]),...(n.branches||[])]){const found=find(ch,command);if(found)return found}}
 for(const actions of [[],['DELETE_WITHDRAWN_FLOW'],['RESUBMIT_WITHDRAWN_FLOW'],undefined]){
  const c={exports:{},computed:vue.computed,fileStatus:vue.ref('WITHDRAWN'),fileDetail:vue.ref({...current,processDefinitionKey:actions?.length?'dcc-controlled-file-approval':current.processDefinitionKey,requesterId:1,actionProjection:actions===undefined?undefined:{allowedActions:actions,actionLocked:true,canWithdraw:false}}),currentUserId:vue.ref(1),isDccControlledFileActionAllowed:lifecycle.exports.isDccControlledFileActionAllowed}
  vm.runInNewContext(ts.transpileModule(declaration('canHandleWithdrawnFlow')+'\n'+declaration('canDeleteWithdrawnFlow')+'\n'+declaration('canResubmitWithdrawnFlow')+'\nexports.state={canHandleWithdrawnFlow,canDeleteWithdrawnFlow,canResubmitWithdrawnFlow}',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  for(const [command,expected]of [['delete-withdrawn-flow',actions?.includes('DELETE_WITHDRAWN_FLOW')===true],['resubmit-withdrawn-flow',actions?.includes('RESUBMIT_WITHDRAWN_FLOW')===true]]){
   const element=find(descriptor.template.ast,command);assert.ok(element)
   const compiled=compileScript(parse('<template>'+element.loc.source+'</template><script setup>const canHandleWithdrawnFlow=__state.canHandleWithdrawnFlow;const canDeleteWithdrawnFlow=__state.canDeleteWithdrawnFlow;const canResubmitWithdrawnFlow=__state.canResubmitWithdrawnFlow;</script>').descriptor,{id:'actual-withdrawn-action-menu',inlineTemplate:true})
   const r={exports:{},require:()=>vue,__state:c.exports.state};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,r)
   let seen=false;const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
   const app=renderer.createApp(r.exports.default);app.component('el-dropdown-item',{setup:()=>{seen=true;return()=>vue.h('button')}});app.mount({children:[]});try{assert.equal(seen,expected,command)}finally{app.unmount()}
  }
 }
})
