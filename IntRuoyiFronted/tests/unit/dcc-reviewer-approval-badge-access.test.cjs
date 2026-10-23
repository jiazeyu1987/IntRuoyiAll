const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const code=ts.transpileModule(fs.readFileSync('src/store/modules/approvalTodoBadge.ts','utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
function host(allowed){
 const context={exports:{},require:id=>{
  if(id==='pinia')return{defineStore:(_name,definition)=>definition}
  if(id==='@/api/approval-center')return{getApprovalTaskPage:async()=>{throw Error('The real authorized endpoint failed')}}
  if(id==='@/utils/permission')return{checkPermi:ps=>allowed&&ps.includes('bpm:task:query')}
  if(id==='../index')return{store:{}}
  throw Error('Unexpected real store dependency '+id)
 },Promise,Date,Number,Error}
 vm.runInNewContext(code,context);return context.exports
}
const tree=[{meta:{},children:[{meta:{approvalTodoBadge:true}}]}]
test('Reviewer-only shell badge does not authorize the protected task query',()=>assert.equal(host(false).hasApprovalTodoBadgeRoute(tree),false))
test('Actual task-query permission retains approval badge discovery',()=>assert.equal(host(true).hasApprovalTodoBadgeRoute(tree),true))
test('Query permission alone does not create a nonexistent badge route',()=>assert.equal(host(true).hasApprovalTodoBadgeRoute([{meta:{}}]),false))
test('Real authorized request failure remains rejected and does not mark counts loaded',async()=>{
 const d=host(true).useApprovalTodoBadgeStore,x={...d.state()};Object.assign(x,d.actions)
 await assert.rejects(x.refreshTodoTotal(),/real authorized endpoint failed/);assert.equal(x.loaded,false);assert.equal(x.loading,false);assert.match(x.error,/real authorized endpoint failed/)
})
