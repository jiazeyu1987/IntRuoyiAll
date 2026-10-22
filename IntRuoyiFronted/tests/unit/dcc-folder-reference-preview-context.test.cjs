const {test}=require('node:test'),assert=require('node:assert/strict')
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript'),{parse}=require('vue/compiler-sfc')
const root=path.resolve(__dirname,'../..'),file='9223372036854775701'
const compile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
function moduleAt(relative){const c={exports:{},URLSearchParams,TextEncoder,TextDecoder,Uint8Array,atob,btoa,Error,BigInt};vm.runInNewContext(compile(fs.readFileSync(path.join(root,relative),'utf8')),c);return c.exports}
const presentation=moduleAt('src/views/dcc/controlled-file/view/presentation.ts'),body=moduleAt('src/api/dcc/controlledFile/bodyReadContext.ts')
function fixture(){
 const script=parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue'),'utf8')).descriptor.scriptSetup.content
 const ast=ts.createSourceFile('actual.ts',script,ts.ScriptTarget.Latest,true)
 const defs=['previewReference','preview'].map(name=>{const statement=ast.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.getText(ast)===name));assert.ok(statement);return statement.getText(ast)})
 const opened=[],windowHandle={opener:'original'},row={reference:{selectedControlledFileId:file},selectedVersion:{controlledFileId:file,versionNo:'A/1',status:'OBSOLETE',canPreview:true}}
 const c={exports:{},...presentation,...body,Error,String,route:{fullPath:'/dcc/controlled-file/browser?scope=directory'},state:{readReferenceForAction:async()=>row},window:{open:(url,target)=>{opened.push({url,target});return windowHandle}}}
 vm.runInNewContext(compile(defs.join('\n')+'\nexports.reference=previewReference;exports.daily=preview;'),c)
 return {c,row,opened,windowHandle,...c.exports}
}
test('actual folder reference callback navigates with the exact saved historical version context',async()=>{
 const f=fixture();await f.reference(f.row,()=>true);assert.equal(f.opened.length,1)
 const url=new URL(f.opened[0].url,'http://local');assert.equal(url.pathname,'/dcc/controlled-file/detail/'+file)
 assert.ok(url.searchParams.has('bodyReadContext'),'fixed reference must not fall back to DAILY')
 const context=body.readDccBodyReadContextRoute(file,Object.fromEntries(url.searchParams));assert.equal(context.intent,'VERSION_HISTORY');assert.equal(context.expectedVersionNo,'A/1');assert.equal(context.controlledFileId,file)
 assert.equal(f.opened[0].target,'_blank');assert.equal(f.windowHandle.opener,null)
})
test('a reference that loses permission or selection while being checked cannot open a body window',async()=>{
 const f=fixture();f.c.state.readReferenceForAction=async()=>{throw Error('VIEW revoked')}
 await assert.rejects(f.reference(f.row,()=>true),/VIEW revoked/);assert.equal(f.opened.length,0)
 f.c.state.readReferenceForAction=async()=>f.row;await f.reference(f.row,()=>false);assert.equal(f.opened.length,0)
 f.row.selectedVersion.canPreview=false;await assert.rejects(f.reference(f.row,()=>true),/没有该文件正文查看权限/);assert.equal(f.opened.length,0)
})
test('ordinary file preview retains DAILY and the exact selected file identity',async()=>{
 const f=fixture();await f.daily({...f.row.selectedVersion,status:'ACTIVE'})
 const url=new URL(f.opened[0].url,'http://local');assert.equal(url.pathname,'/dcc/controlled-file/detail/'+file)
 const context=body.readDccBodyReadContextRoute(file,Object.fromEntries(url.searchParams));assert.equal(context.intent,'DAILY')
})
