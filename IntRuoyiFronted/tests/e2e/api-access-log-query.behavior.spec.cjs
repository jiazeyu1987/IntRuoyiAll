const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path')
const ts=require('typescript'),vue=require('vue'),{parse}=require('vue/compiler-sfc')
const deferred=()=>{let resolve,reject;const promise=new Promise((yes,no)=>{resolve=yes;reject=no});return{promise,resolve,reject}}
function load(file,names,context){
  const source=parse(fs.readFileSync(path.resolve(__dirname,'../../src/views/infra/apiAccessLog',file),'utf8')).descriptor.scriptSetup.content
  const ast=ts.createSourceFile(file+'.ts',source,ts.ScriptTarget.Latest,true)
  const statements=ast.statements.filter(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>names.includes(d.name.getText(ast))))
  const actual=statements.flatMap(s=>[...s.declarationList.declarations].map(d=>d.name.getText(ast)))
  for(const name of names)assert.ok(actual.includes(name),'actual component declaration '+name)
  const code=ts.transpileModule(statements.map(s=>s.getText(ast)).join('\n'),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
  return Function(...Object.keys(context),code+`;return {${names.join(',')}}`)(...Object.values(context))
}
const listNames=['loading','total','list','queryParams','queryError','querySequence','pendingQueryKey','userStore','queryOwner','getList']
function listHarness(){
  const requests=[],identity={tenant:168,id:7}
  const state=load('index.vue',listNames,{...vue,getTenantId:()=>identity.tenant,useUserStore:()=>({get getUser(){return{id:identity.id}}}),
    ApiAccessLogApi:{getApiAccessLogPage:(params,current)=>{const response=deferred();requests.push({params,current,...response});return response.promise}}})
  return{...state,requests,identity}
}
test('latest query owns loading total and rows; older success cannot overwrite it',async()=>{
  const h=listHarness();const first=h.getList();h.queryParams.userId=8;const second=h.getList()
  assert.equal(h.requests[0].current(),false);assert.equal(h.requests[1].current(),true)
  assert.equal(h.requests[0].params.userId,null);assert.equal(h.requests[1].params.userId,8)
  h.requests[1].resolve({list:[{id:2}],total:1});await second
  h.requests[0].resolve({list:[{id:1}],total:999});await first
  assert.deepEqual(h.list.value,[{id:2}]);assert.equal(h.total.value,1);assert.equal(h.loading.value,false)
})
test('repeated clicks on the same pending query issue one request and remain refreshable after completion',async()=>{
  const h=listHarness();const first=h.getList();await h.getList();assert.equal(h.requests.length,1)
  h.requests[0].resolve({list:[{id:1}],total:1});await first
  const refresh=h.getList();assert.equal(h.requests.length,2)
  h.requests[1].resolve({list:[{id:2}],total:2});await refresh
  assert.equal(h.total.value,2)
})
test('new query clears old records; failure stays visible without stale total',async()=>{
  const h=listHarness();h.list.value=[{id:1}];h.total.value=999
  const pending=h.getList();assert.deepEqual(h.list.value,[])
  h.requests[0].reject(Error('查询失败'));await pending
  assert.equal(h.queryError.value,'查询失败');assert.equal(h.loading.value,false);assert.equal(h.total.value,0)
})

test('loading another page preserves total so pagination does not clamp back to page one',async()=>{
  const h=listHarness();h.total.value=13729;h.queryParams.pageNo=2;h.queryParams.pageSize=20
  const pending=h.getList()
  assert.equal(h.total.value,13729);assert.equal(h.queryParams.pageNo,2);assert.equal(h.requests[0].params.pageNo,2)
  h.requests[0].resolve({list:[{id:174629}],total:13729});await pending
  assert.equal(h.queryParams.pageNo,2);assert.equal(h.requests.length,1)
})
test('tenant or account changes reject the pending list ownership',async()=>{
  const h=listHarness();const pending=h.getList();h.identity.tenant=1
  assert.equal(h.requests[0].current(),false);h.requests[0].resolve({list:[{id:1}],total:1});await pending
  assert.deepEqual(h.list.value,[])
})
const detailNames=['dialogVisible','detailLoading','detailData','detailError','userStore','detailOwner','detailSequence','open']
function detailHarness(){
  const requests=[]
  const h=load('ApiAccessLogDetail.vue',detailNames,{...vue,getTenantId:()=>168,useUserStore:()=>({getUser:{id:7}}),
    ApiAccessLog:{getApiAccessLog:(id,current)=>{const response=deferred();requests.push({id,current,...response});return response.promise}}})
  return{...h,requests}
}
test('detail loads by selected ID and does not mistake list summary for full data',async()=>{
  const h=detailHarness();const pending=h.open({id:12,requestParams:'stale summary'})
  assert.equal(h.detailData.value,undefined);assert.equal(h.requests[0].id,12)
  h.requests[0].resolve({id:12,requestParams:'original full request',responseBody:'original full response'});await pending
  assert.equal(h.detailData.value.responseBody,'original full response');assert.equal(h.detailLoading.value,false)
})
test('late detail and closed dialog cannot replace the current selected record',async()=>{
  const h=detailHarness();const first=h.open({id:1}),second=h.open({id:2})
  h.requests[1].resolve({id:2});await second;h.requests[0].resolve({id:1});await first
  assert.equal(h.detailData.value.id,2)
  const third=h.open({id:3});h.dialogVisible.value=false
  assert.equal(h.requests[2].current(),false);h.requests[2].resolve({id:3});await third
  assert.equal(h.detailData.value,undefined)
})
test('missing or mismatched full detail is an explicit error',async()=>{
  const h=detailHarness();const pending=h.open({id:1});h.requests[0].resolve({id:2});await pending
  assert.match(h.detailError.value,/不一致/);assert.equal(h.detailData.value,undefined)
})
