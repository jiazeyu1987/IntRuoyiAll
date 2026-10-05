const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const {descriptor}=parse(fs.readFileSync('src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue','utf8'))
const ast=ts.createSourceFile('public.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
function host(){
 const calls=[],state={project:{id:'9007199254740993'},folder:{projectId:'9007199254740993',folderId:'9223372036854775700'},folderLoading:false,folderError:'',canReference:false,selectProject:async id=>calls.push(['refresh',id])}
 const c={exports:{},String,Error,state,checkPermi:()=>true,folderEditor:vue.ref({open:async(...args)=>calls.push(['open',...args]),openDelete:async(...args)=>calls.push(['delete',...args])})}
 const names=['openFolderMaintenance','handleFolderMaintenanceSaved']
 const defs=names.map(name=>{const st=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>ts.isIdentifier(d.name)&&d.name.text===name));assert.ok(st,'actual public '+name);return st.getText(ast)})
 vm.runInNewContext(ts.transpileModule(defs.join('\n')+'\nexports.open=openFolderMaintenance;exports.saved=handleFolderMaintenanceSaved',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return{c,calls,state,...c.exports}
}
test('actual public create/edit/delete entry passes exact selected IDs without borrowing reference authority',async()=>{
 const h=host();await h.open('create');await h.open('update');await h.open('delete')
 assert.deepEqual(h.calls,[['open','9007199254740993'],['open','9007199254740993','9223372036854775700'],['delete','9007199254740993','9223372036854775700']])
 h.c.checkPermi=()=>false;await h.open('create');assert.equal(h.calls.length,3)
})
test('actual save event rereads only the still selected project and never switches a newer project back',async()=>{
 const h=host();await h.saved('9007199254740993');assert.deepEqual(h.calls,[['refresh','9007199254740993']])
 h.state.project={id:'9007199254740995'};await h.saved('9007199254740993');assert.equal(h.calls.length,1)
 h.state.folder={projectId:'other',folderId:'4'};await h.open('update');assert.equal(h.calls.length,1)
})
test('actual project maintenance toolbar renders three formal actions wired to the real handler',async()=>{
 const h=host(),find=n=>{if(n.type===1&&n.props.some(p=>p.type===6&&p.name==='data-testid'&&p.value?.content==='dcc-project-folder-maintenance'))return n;for(const child of n.children||[]){const found=find(child);if(found)return found}}
 const node=find(descriptor.template.ast);assert.ok(node,'formal public maintenance toolbar')
 const compiled=compileScript(parse('<template>'+node.loc.source+'</template><script setup>const state=__host.state;const openFolderMaintenance=__host.open;</script>').descriptor,{id:'public-folder-actions',inlineTemplate:true})
 const c={exports:{},require:()=>vue,__host:h};vm.runInNewContext(ts.transpileModule(compiled.content,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText,c)
 const renderer=vue.createRenderer({createElement:type=>({type,children:[],props:{}}),createText:text=>({text}),createComment:()=>({}),insert:(n,p)=>p.children.push(n),remove(){},setText:(n,t)=>{n.text=t},setElementText:(n,t)=>{n.text=t},patchProp:(n,k,_old,v)=>{n.props[k]=v},parentNode:()=>null,nextSibling:()=>null})
 const app=renderer.createApp(c.exports.default);app.component('el-button',{setup:(_p,ctx)=>()=>vue.h('button',ctx.attrs,ctx.slots.default?.())});app.directive('hasPermi',{mounted(_el,binding){assert.equal(binding.value[0],'dcc:project-code:update')}})
 const root={children:[]};app.mount(root);const buttons=n=>(n.type==='button'?[n]:[]).concat((n.children||[]).flatMap(buttons))
 try{const values=buttons(root);assert.equal(values.length,3);for(const b of values)await b.props.onClick();assert.equal(h.calls.length,3)}finally{app.unmount()}
})
