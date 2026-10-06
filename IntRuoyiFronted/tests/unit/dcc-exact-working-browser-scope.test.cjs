const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const {parse}=require('vue/compiler-sfc'),axios=require('axios')
const read=p=>fs.readFileSync(p,'utf8'),compile=s=>ts.transpileModule(s,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const FILE='9223372036854775701',MASTER='9223372036854775700'
function helper(){const c={exports:{},Error};vm.runInNewContext(compile(read('src/views/dcc/controlled-file/shared/working-browser-navigation.ts')),c);return c.exports}
const {descriptor}=parse(read('src/views/dcc/controlled-file/browser/index.vue')),ast=ts.createSourceFile('browser.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration=(a,name)=>{const node=a.statements.find(n=>ts.isVariableStatement(n)&&n.declarationList.declarations.some(d=>d.name.text===name));assert.ok(node,name);return node.getText(a)}
function build(query={},mode='storage'){
 const c={exports:{},Error,queryParams:{pageNo:1,pageSize:20,keyword:' TASK-01 '},normalizeKeyword:s=>s?.trim(),isCurrentDirectorySearch:{value:false},selectedDirectoryId:{value:7},route:{query},browserMode:{value:mode},workingBrowserIdentityQuery:helper().workingBrowserIdentityQuery}
 vm.runInNewContext(compile(declaration(ast,'buildBrowserRequestParams')+'\nexports.build=buildBrowserRequestParams;'),c);return c.exports.build
}
test('actual canonical working navigation and builder send exact string pair through original browser API into actual Axios config',async()=>{
 const route=helper().buildWorkingBrowserRoute({id:FILE,masterId:MASTER,hasProjectStorageMapping:true,dccProjectCodeId:'271',projectFolderId:'2',fileNumber:'TASK-01'})
 const params=build(route.query)(true);assert.equal(params.workingFileId,FILE);assert.equal(params.workingMasterId,MASTER);assert.equal(params.latestVersionOnly,true);assert.equal(params.keyword,'TASK-01');assert.equal(params.directoryId,undefined)
 const calls=[],client=axios.create({adapter:async config=>{calls.push(config);return{status:200,statusText:'OK',headers:{},config,data:{list:[],total:0}}}})
 const apiAST=ts.createSourceFile('api.ts',read('src/api/dcc/controlledFile/workflow.ts'),ts.ScriptTarget.Latest,true),c={exports:{},request:{get:async option=>(await client.get(option.url,{params:option.params})).data}}
 vm.runInNewContext(compile(declaration(apiAST,'getControlledFileBrowserPage')),c)
 await c.exports.getControlledFileBrowserPage(params);assert.equal(calls.length,1);assert.equal(calls[0].url,'/dcc/controlled-files/browser-page');assert.equal(calls[0].params.workingFileId,FILE);assert.equal(calls[0].params.workingMasterId,MASTER)
})
test('actual no-pair normal list and shared export builder do not acquire working identity fields',()=>{
 const plain=build() (true);assert.equal(Object.hasOwn(plain,'workingFileId'),false);assert.equal(Object.hasOwn(plain,'workingMasterId'),false)
 const route={workingFileId:FILE,workingMasterId:MASTER};const exported=build(route)();assert.equal(Object.hasOwn(exported,'workingFileId'),false);assert.equal(Object.hasOwn(exported,'workingMasterId'),false)
 const project=build(route,'project')(true);assert.equal(Object.hasOwn(project,'workingFileId'),false)
})
test('actual builder validates complete pair and only the browser getList caller opts into it',()=>{
 for(const query of [{workingFileId:FILE},{workingMasterId:MASTER},{workingFileId:9007199254740992,workingMasterId:MASTER},{workingFileId:'9223372036854775808',workingMasterId:MASTER}])assert.throws(()=>build(query)(true),/身份/)
 const getList=declaration(ast,'getList');assert.match(getList,/buildBrowserRequestParams\(true\)/)
 for(const name of ['handleMetadataExport','handleRecognitionRecordExport','handleRecognitionMigrationExport']){
  const code=declaration(ast,name);assert.match(code,/buildBrowserRequestParams\(\)/);assert.doesNotMatch(code,/buildBrowserRequestParams\(true\)/)
 }
})
