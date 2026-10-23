const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const source=fs.readFileSync('src/views/dcc/controlled-file/workbench/index.vue','utf8')
const {descriptor}=parse(source),ast=ts.createSourceFile('workbench.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=(name,required=true)=>{const node=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text===name));if(required)assert.ok(node,name);return node?node.getText(ast):''}
const metricKeys=['approvalTodoTotal','pendingDistributionTotal','trainingTodoTotal','finalizationFailedTotal']
function host({permissions=[],trainingError,late}={}){
 const calls=[],user=vue.reactive({getIsSetUser:true,getPermissions:new Set(permissions),getUser:{id:'25'}}),tenant={value:'1'}
 const section={approval:{loading:false,error:'',total:null},training:{loading:false,error:'',total:null},finalization:{loading:false,error:'',total:null}}
 const c={exports:{},Error,computed:vue.computed,reactive:vue.reactive,ref:vue.ref,userStore:user,route:{path:'/dcc/controlled-file/workbench',fullPath:'/dcc/controlled-file/workbench'},DCC_WORKBENCH_PATH:'/dcc/controlled-file/workbench',getVisitTenantId:()=>undefined,getTenantId:()=>tenant.value,isWorkflowId:v=>/^\d+$/.test(String(v)),loading:vue.ref(false),loadErrorMessage:vue.ref(''),lastLoadedAt:vue.ref(''),metricItems:vue.ref([]),approvalTodoRows:vue.ref([]),trainingTodoRows:vue.ref([]),finalizationFailedRows:vue.ref([]),legacySectionState:vue.reactive(section),legacyWorkbenchSequence:0,
 buildDccWorkbenchMetricItems:values=>metricKeys.map(key=>({key,count:values[key],label:key})),formatDateTimeValue:()=> 'actualreadtime',resolveWorkbenchErrorMessage:e=>e.message,
 buildTaskRows:async()=>{calls.push('approval');return{rows:[{id:'real-task'}],total:1}},
 loadTrainingTodos:async()=>{calls.push('training');if(trainingError)throw new Error(trainingError);return{rows:[],total:0}},
 loadFilePageByStatus:async()=>{calls.push('finalization');if(late)return late();return{rows:[{id:'real-file'}],total:1}}}
 const names=['hasLegacyWorkbenchPermission','canReadLegacyApprovalTodos','canReadLegacyTrainingTodos','canReadLegacyFinalizationFailed','readLegacyWorkbenchContext','syncLegacyWorkbenchMetrics']
 vm.runInNewContext(ts.transpileModule(names.map(n=>declaration(n,false)).join('\n')+'\n'+declaration('loadWorkbench')+'\nexports.load=loadWorkbench;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return{c,calls,user,tenant,load:c.exports.load}
}
const all=['bpm:task:query','bpm:process-instance:query','dcc:controlled-file:training:mine','dcc:controlled-file:query']
test('actual legacy loader with native-only menu never reads unauthorized prior sections',async()=>{
 const h=host({permissions:['bpm:task:query']});await h.load();assert.deepEqual(h.calls,[]);assert.equal(h.c.metricItems.value.find(m=>m.key==='trainingTodoTotal')?.count??null,null)
})
test('actual authorized section failure keeps other real rows and unknown failed count rather than global zero reset',async()=>{
 const h=host({permissions:all,trainingError:'正式培训确认读取拒绝'});await h.load();assert.equal(h.c.approvalTodoRows.value.length,1);assert.equal(h.c.finalizationFailedRows.value.length,1);assert.equal(h.c.metricItems.value.find(m=>m.key==='approvalTodoTotal').count,1);assert.equal(h.c.metricItems.value.find(m=>m.key==='trainingTodoTotal').count,null);assert.match(h.c.legacySectionState.training.error,/正式培训确认读取拒绝/)
})
test('actual old-section late response cannot publish across live tenant or actor changes',async()=>{
 let finish;const h=host({permissions:all,late:()=>new Promise(r=>{finish=r})});const request=h.load();await Promise.resolve();h.tenant.value='2';h.user.getUser.id='26';finish({rows:[{id:'wrong-actor-file'}],total:1});await request;assert.equal(h.c.finalizationFailedRows.value.length,0);assert.equal(h.c.legacySectionState.finalization.total,null)
})
test('actual training section renderer omits unauthorized and failed empty tables while preserving visible formal error',()=>{
 const find=n=>{if(n.type===1&&n.tag==='ContentWrap'&&n.loc.source.includes('待培训确认')&&n.loc.source.includes(':data="trainingTodoRows"'))return n;for(const ch of [...(n.children||[]),...(n.branches||[])]){const found=find(ch);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 for(const [allowed,error,expectedTitle]of [[false,'',false],[true,'正式读取失败',true]]){
  const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const canReadLegacyTrainingTodos=__allowed;const legacySectionState={training:{loading:false,error:__error}};const trainingTodoRows=[];const loading=false;const openPath=()=>{};const openFileDetail=()=>{};const openTrainingTask=()=>{};</script>').descriptor,{id:'actual-legacy-training-section',inlineTemplate:true})
  const c={exports:{},require:()=>vue,__allowed:allowed,__error:error};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
  const remove=n=>{if(n?.parent){n.parent.children.splice(n.parent.children.indexOf(n),1);n.parent=null}}
  const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p,anchor)=>{remove(n);n.parent=p;const at=p.children.indexOf(anchor);at<0?p.children.push(n):p.children.splice(at,0,n)},remove,setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:n=>n?.parent||null,nextSibling:n=>n?.parent?.children[n.parent.children.indexOf(n)+1]||null})
  const app=renderer.createApp(c.exports.default);app.directive('loading',{});let tables=0
  for(const tag of ['ContentWrap','el-button','el-table-column','el-tag'])app.component(tag,{setup:(_,{slots})=>()=>vue.h('section',slots.default?.())})
  app.component('el-alert',{props:['title'],setup:props=>()=>vue.h('p',props.title)})
  app.component('el-table',{setup:()=>{tables++;return()=>vue.h('table')}})
  const root={children:[]};app.mount(root);const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
  try{assert.equal(text(root).includes('待培训确认'),expectedTitle);assert.equal(tables,0);if(error)assert.ok(text(root).includes(error))}finally{app.unmount()}
 }
})
test('actual metric helper keeps null unknown and zero successful; unauthorized metrics remain absent',()=>{
 const ps=fs.readFileSync('src/views/dcc/controlled-file/workbench/presentation.ts','utf8'),pa=ts.createSourceFile('presentation.ts',ps,ts.ScriptTarget.Latest,true)
 const get=name=>{const node=pa.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text===name));assert.ok(node);return node.getText(pa)}
 const c={exports:{}};vm.runInNewContext(ts.transpileModule(get('DCC_WORKBENCH_STATUS_SECTIONS')+'\n'+get('buildDccWorkbenchMetricItems'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const metrics=c.exports.buildDccWorkbenchMetricItems({approvalTodoTotal:0,pendingDistributionTotal:null,trainingTodoTotal:null,finalizationFailedTotal:1})
 assert.equal(metrics.find(m=>m.key==='approvalTodoTotal').count,0);assert.equal(metrics.find(m=>m.key==='trainingTodoTotal').count,null)
 const h=host({permissions:['bpm:task:query']});h.c.canReadLegacyApprovalTodos=vue.ref(false);h.c.canReadLegacyTrainingTodos=vue.ref(false);h.c.canReadLegacyFinalizationFailed=vue.ref(false);h.c.canHandleWorkflowDistribution=vue.ref(false);h.c.metricItems.value=metrics
 vm.runInNewContext(ts.transpileModule(declaration('visibleMetricItems')+'\nexports.visible=visibleMetricItems;',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h.c)
 assert.equal(h.c.exports.visible.value.length,0)
})
