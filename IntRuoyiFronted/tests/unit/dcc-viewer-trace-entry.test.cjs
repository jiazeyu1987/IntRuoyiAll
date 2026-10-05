const { test }=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/index.vue','utf8'))
const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const st=()=>ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='openViewerTraceability'))
const helperExports={};vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/dcc/controlled-file/view/presentation.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,{exports:helperExports,URLSearchParams,String})
function host(){const calls=[],c={exports:{},String,route:{fullPath:'/dcc/controlled-file/detail/9007199254740993?viewer=1'},controlledFileId:vue.ref('9007199254740993'),fileDetail:vue.ref({id:'9007199254740993'}),isWorkingBrowserDetailCurrent:vue.ref(true),buildControlledFileTraceabilityPath:helperExports.buildControlledFileTraceabilityPath,router:{push:async value=>calls.push(value)}};assert.ok(st(),'actual viewer trace handler');vm.runInNewContext(ts.transpileModule(st().getText(ast)+'\nexports.open=openViewerTraceability',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c);return{c,calls,...c.exports}}
test('actual preview archive/history entry retains selected Long ID and formal readonly trace contract',async()=>{
 const h=host();await h.open();const u=new URL(h.calls[0],'http://localhost:8061')
 assert.equal(u.pathname,'/dcc/controlled-file/detail/9007199254740993');assert.equal(u.searchParams.get('traceability'),'1');assert.equal(u.searchParams.get('traceScope'),'trace');assert.equal(u.searchParams.has('management'),false);assert.equal(u.searchParams.has('viewer'),false)
 const ast=ts.createSourceFile('detail.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
 const guard=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text==='isBrowserTraceabilityPage'));assert.ok(guard)
 const g={exports:{},computed:vue.computed,String,route:{query:Object.fromEntries(u.searchParams)}}
 vm.runInNewContext(ts.transpileModule(guard.getText(ast)+'\nexports.readonly=isBrowserTraceabilityPage.value',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,g)
 assert.equal(g.exports.readonly,true)
 h.c.fileDetail.value={id:'9007199254740995'};await h.open();h.c.isWorkingBrowserDetailCurrent.value=false;await h.open();assert.equal(h.calls.length,1)
})
test('actual readonly preview toolbar renders the archive/history action and calls actual navigation',async()=>{
 const h=host(),find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-viewer-trace-entry'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node,'formal viewer trace button')
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const isWorkingBrowserDetailCurrent=true;const openViewerTraceability=__handler;</script>').descriptor,{id:'viewer-archive-button',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__handler:h.open};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);app.component('el-button',{setup:(_p,ctx)=>()=>vue.h('button',ctx.attrs,ctx.slots.default?.())});const root={children:[]};app.mount(root)
 try{await root.children[0].props.onClick();assert.equal(h.calls.length,1)}finally{app.unmount()}
})
test('actual assignment-form template remains absent in readonly trace and present in management',()=>{
 const find=n=>{if(n.type===1&&n.tag==='DetailSignoffAssignment')return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node)
 for(const management of [false,true]){
  const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const fileDetail={id:"10"};const isSignoffTask=true;const approvalProcessInstanceId="round";const approvalTodoTask={id:"task"};let signoffAssignmentState;const reloadAll=()=>{};const showDetailManagementActions=__management;</script>').descriptor,{id:'readonly-assignment-gate',inlineTemplate:true})
  const c={exports:{},require:()=>vue,__management:management};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
  const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
  const app=renderer.createApp(c.exports.default);app.component('DetailSignoffAssignment',{render:()=>vue.h('button','真实指派表单')});const root={children:[]};app.mount(root)
  const text=n=>(n.text||'')+(n.children||[]).map(text).join('')
  try{assert.equal(text(root).includes('真实指派表单'),management)}finally{app.unmount()}
 }
})
