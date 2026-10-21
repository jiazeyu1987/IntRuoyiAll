const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const root=path.resolve(__dirname,'../..')
function moduleAt(relative){
 const context={exports:{},TextEncoder,TextDecoder,btoa,atob}
 vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,relative),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,context)
 return context.exports
}
const body=moduleAt('src/api/dcc/controlledFile/bodyReadContext.ts')
const navigation=moduleAt('src/views/dcc/controlled-file/shared/working-browser-navigation.ts')
const script=parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/browser/index.vue'),'utf8')).descriptor.scriptSetup.content
const ast=ts.createSourceFile('browser.ts',script,ts.ScriptTarget.Latest,true)
const statement=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)==='openPreview'))
assert.ok(statement)
const BASE='9223372036854775701',CHILD='9223372036854775702',MASTER='9223372036854775700'
function open(query,file={id:CHILD,status:'WORKING',versionNo:'A/1-1'}){
 const calls=[],context={...body,...navigation,exports:{},route:{query},message:{error:value=>{throw Error(value)}},
  buildBrowserReturnPath:()=>'/dcc/controlled-file/browser',
  buildControlledFileViewerPath:(id,from,returnTo,wire)=>{calls.push({id,from,returnTo,context:JSON.parse(Buffer.from(wire,'base64url').toString('utf8'))});return '/actual-preview'},
  window:{open:()=>({})}}
 vm.runInNewContext(ts.transpileModule(statement.getText(ast)+'\nexports.open=openPreview',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
 context.exports.open(file);assert.equal(calls.length,1);return calls[0].context
}
test('a stale selected-file identity does not grant a successor an authoring context',()=>{
 assert.deepEqual(open({workingFileId:BASE,workingMasterId:MASTER}),{intent:'DAILY',controlledFileId:CHILD})
})
test('the exact selected working identity has its own authoring body context',()=>{
 assert.deepEqual(open({workingFileId:CHILD,workingMasterId:MASTER}),{intent:'AUTHORING',controlledFileId:CHILD,expectedVersionNo:'A/1-1'})
})
test('an exact rejected authoring version retains its actual application round',()=>{
 assert.deepEqual(open({workingFileId:CHILD,workingMasterId:MASTER},{id:CHILD,status:'REJECTED',versionNo:'A/1-1',changeType:'REVISION',processInstanceId:'original-round'}),
  {intent:'AUTHORING',controlledFileId:CHILD,expectedVersionNo:'A/1-1',applicationType:'REVISION',bpmRound:'original-round'})
})
for(const [label,query,status] of [
 ['daily entry',{},'WORKING'],
 ['controlled version',{workingFileId:CHILD,workingMasterId:MASTER},'ACTIVE']
])test(label+' does not acquire an authoring context',()=>assert.deepEqual(open(query,{id:CHILD,status,versionNo:'A/1-1'}),{intent:'DAILY',controlledFileId:CHILD}))
test('an incomplete working route is rejected instead of inferring a Master',()=>assert.throws(()=>open({workingFileId:BASE}),/身份无效/))
