const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
function declaration(file,name){const ast=ts.createSourceFile('component.ts',parse(fs.readFileSync(file,'utf8')).descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true);return ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name)).getText(ast)}
const detail='src/views/dcc/controlled-file/detail/index.vue',inbox='src/views/approval-center/index.vue'
function host(fromApproval){const state={taskListRevision:0},routes=[],route={name:'ApprovalCenterTodo',path:'/approval-center/todo',query:{}};
 const c={JSON,String,route,approvalCenterRouteInstanceName:'ApprovalCenterTodo',approvalTodoBadgeStore:state,
 useApprovalTodoBadgeStoreWithOut:()=>({invalidateTaskLists(){state.taskListRevision++}}),router:{push:async r=>routes.push(r)}}
 vm.runInNewContext(ts.transpileModule(declaration(inbox,'normalizeRouteQueryValue')+'\n'+declaration(inbox,'buildApprovalCenterRouteStateKey')+'\nexports={key:buildApprovalCenterRouteStateKey}',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const key=c.exports.key;c.route={query:fromApproval?{handling:'approval',from:'approval-center'}:{from:'project-browser'}}
 vm.runInNewContext(ts.transpileModule(declaration(detail,'returnToCompletedTaskInbox')+'\nexports={complete:returnToCompletedTaskInbox}',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return {c,state,routes,key:()=>{const prev=c.route;c.route=route;try{return key()}finally{c.route=prev}},complete:c.exports.complete}}
test('actual successful task return invalidates cached list identity while unchanged tab return stays stable',async()=>{
 const h=host(true),before=h.key();assert.equal(h.key(),before);assert.equal(await h.complete(),true)
 assert.notEqual(h.key(),before);assert.equal(h.state.taskListRevision,1);assert.equal(h.routes[0].path,'/approval-center/todo')
})
test('normal file management does not invalidate or navigate approval inbox',async()=>{const h=host(false),before=h.key();assert.equal(await h.complete(),false);assert.equal(h.key(),before);assert.equal(h.routes.length,0)})
