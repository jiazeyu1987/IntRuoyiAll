const assert=require('node:assert/strict')
const test=require('node:test')
const ts=require('typescript')
const {parse}=require('vue/compiler-sfc')
const {fixture,read,compile,deferred}=require('./helpers/workbench-s3-fixture.cjs')
const consumers=[
 ['src/views/Profile/Index.vue','refreshProfileWorkbenchTodoBadge'],
 ['src/layout/components/Menu/src/Menu.vue','loadProfileWorkbenchTodoBadge'],
 ['src/layout/components/TabMenu/src/TabMenu.vue','loadProfileWorkbenchTodoBadge'],
 ['src/layout/components/TagsView/src/TagsView.vue','loadProfileWorkbenchTodoBadge']
]
for(const [file,loader] of consumers)test('actual '+file+' badge catch logs safe code and preserves real store failure',async()=>{
 const f=fixture(),logs=[],logged=deferred()
 const error=Object.assign(new Error('secret-message'),{code:1002034008,config:{headers:{Authorization:'secret-auth'},params:{queryVersion:'secret-query'},data:{queryVersion:'secret-body'}}})
 f.setTransport(async(r,next)=>{if(r.url.endsWith('/count'))throw error;return next(r)})
 const descriptor=parse(read(file)).descriptor,source=descriptor.scriptSetup?.content||descriptor.script.content
 const ast=ts.createSourceFile('consumer.tsx',source,ts.ScriptTarget.Latest,true,ts.ScriptKind.TSX)
 const declarations=[];const walk=node=>{if(ts.isVariableStatement(node)&&node.declarationList.declarations.some(d=>[loader,'reportProfileWorkbenchTodoBadgeError'].includes(d.name.getText(ast))))declarations.push(node.getText(ast));ts.forEachChild(node,walk)};walk(ast)
 assert.equal(declarations.length,2)
 const bindings={profileWorkbenchTodoBadgeStore:f.badge,console:{error:(...args)=>{logs.push(args);logged.resolve()}},hasProfileWorkbenchTodoBadgeRoute:()=>true,permissionStore:{getRouters:[]},unref:x=>x,routers:[]}
 const handlers=Function(...Object.keys(bindings),compile(declarations.join('\n'))+';return {'+loader+',reportProfileWorkbenchTodoBadgeError};')(...Object.values(bindings))
 handlers[loader]();await logged.promise
 assert.equal(f.badge.loaded,false);assert.equal(f.badge.todoTotal,undefined);assert.equal(f.badge.error,'secret-message');assert.equal(f.badge.loading,false)
 assert.deepEqual(logs,[['个人工作台待处理数量加载失败',1002034008]])
 for(const rejected of [new Error('secret-message'),{code:'secret-code'},null])handlers.reportProfileWorkbenchTodoBadgeError(rejected)
 assert.equal(JSON.stringify(logs).includes('secret'),false);assert.equal(logs.at(-1)[1],'WORKBENCH_REQUEST_FAILED');f.scope.stop()
})
test('all current workbench query/action/logging closures are inventoried',()=>{
 // Scope all store consumers, plus actual page, directory, module actions and HTTP wrappers.
 for(const file of ['src/views/Profile/components/ProfileWorkbench.vue','src/store/modules/profileWorkbenchTodoBadge.ts','src/workbench/registry.ts','src/api/system/profileWorkbenchTodo/index.ts','src/utils/edhrWorkTaskNavigation.ts','src/workbench-sources/dcc/registration.ts','src/workbench-sources/mes/registration.ts','src/workbench-sources/showroom/registration.ts'])assert.doesNotMatch(read(file),/console\.(?:error|log|warn)\s*\(/,file)
 const fs=require('node:fs'),path=require('node:path'),found=[]
 const walk=dir=>{for(const entry of fs.readdirSync(dir,{withFileTypes:true})){const file=path.join(dir,entry.name);if(entry.isDirectory())walk(file);else if(/\.(vue|ts|tsx)$/.test(file)){const source=fs.readFileSync(file,'utf8');if(/\.ensureTodoTotalLoaded\(\)/.test(source)&&source.includes('profileWorkbenchTodoBadgeStore'))found.push(file.replaceAll('\\','/').replace(/^src\//,''))}}}
 walk('src')
 assert.deepEqual(found.sort(),consumers.map(([file])=>file.replace(/^src\//,'')).sort())
})
