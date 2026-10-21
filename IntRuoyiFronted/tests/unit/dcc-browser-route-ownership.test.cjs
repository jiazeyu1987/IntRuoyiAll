const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const root=path.resolve(__dirname,'../..')
const script=parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/browser/index.vue'),'utf8')).descriptor.scriptSetup.content
const ast=ts.createSourceFile('browser.ts',script,ts.ScriptTarget.Latest,true)
function declaration(name){
 const statement=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)===name))
 assert.ok(statement,'Missing actual browser function '+name)
 return statement.getText(ast)
}
function fixture(pathname,query){
 const route={path:pathname,query:structuredClone(query)},replaced=[]
 const context={exports:{},route,JSON,router:{replace:async value=>{replaced.push(structuredClone(value));route.path=value.path;route.query=value.query}},
  buildBrowserRouteQuery:()=>({pageNo:'1',pageSize:'20',scope:'global'}),
  buildBrowserRouteQueryFromRoute:()=>route.query,
  withBrowserRouteSyncGuard:action=>action(),getList:async()=>{},persistBrowserRememberedState:()=>{}}
 const source=declaration('syncRouteFromBrowserState')+'\n'+declaration('reloadBrowserListAndCommitState')+
  '\nexports.sync=syncRouteFromBrowserState;exports.reload=reloadBrowserListAndCommitState;'
 vm.runInNewContext(ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
 return {context,route,replaced,...context.exports}
}
test('active storage browser still commits its own page and filter state',async()=>{
 const f=fixture('/dcc/controlled-file/browser',{})
 await f.sync()
 assert.equal(f.replaced.length,1)
 assert.equal(f.replaced[0].path,'/dcc/controlled-file/browser')
 assert.deepEqual(f.replaced[0].query,{pageNo:'1',pageSize:'20',scope:'global'})
})
test('a retained browser never replaces fixed-version detail context',async()=>{
 const query={traceability:'1',traceScope:'trace',bodyReadContext:'exact-historical-context',from:'version-history'}
 const f=fixture('/dcc/controlled-file/detail/2054545668044084233',query)
 await f.sync()
 assert.equal(f.replaced.length,0)
 assert.deepEqual(f.route.query,query)
})
test('a list response completing after navigation preserves the approval task route',async()=>{
 const f=fixture('/dcc/controlled-file/browser',{})
 let complete
 f.context.getList=()=>new Promise(resolve=>{complete=resolve})
 const pending=f.reload()
 const query={handling:'approval',from:'approval-center',processInstanceId:'original-round',taskId:'original-task'}
 f.route.path='/dcc/controlled-file/detail/2054545668044084249';f.route.query=query
 complete();await pending
 assert.equal(f.replaced.length,0)
 assert.deepEqual(f.route.query,query)
})
test('an unchanged active browser does not replace its route',async()=>{
 const f=fixture('/dcc/controlled-file/browser',{pageNo:'1',pageSize:'20',scope:'global'})
 await f.sync();assert.equal(f.replaced.length,0)
})
