const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const { body, resolveDetailReadSelection } = require('./dcc-detail-read-test-loader.cjs')
const FILE = '9007199254740993'
const history = () => ({ intent:'APPROVAL_HISTORY', controlledFileId:FILE, expectedVersionNo:'A/1-2',
  applicationType:'REVISION', bpmRound:'old-round', taskId:'old-task' })
function declarations(file, names) {
  let source=fs.readFileSync(path.join(root,file),'utf8')
  if(file.endsWith('.vue'))source=parse(source).descriptor.scriptSetup.content
  const ast=ts.createSourceFile(file,source,ts.ScriptTarget.Latest,true)
  return names.map(name=>ast.statements.find(s=>s.declarationList?.declarations.some(d=>d.name.text===name)).getText(ast)).join('\n')
}
function wrapper() {
  const calls=[],host={exports:{},...body,request:{get:async req=>{calls.push(req);return{id:FILE,versionNo:'A/1-2'}}}}
  vm.runInNewContext(ts.transpileModule(declarations('src/api/dcc/controlledFile/workflow.ts',['getControlledFile']),{
    compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,host)
  return {calls,get:host.exports.getControlledFile}
}
function loader(query, getControlledFile) {
  const host={exports:{},resolveDetailReadSelection,
    detailLoadSequence:1,controlledFileId:vue.ref(FILE),route:{query,fullPath:'/detail/'+FILE+'?bodyReadContext='+query.bodyReadContext},
    checkPermi:()=>false,viewerMode:vue.ref(true),showLifecycleTraceSections:vue.ref(false),getControlledFile,
    fileDetail:vue.ref(),applicationApprovalRead:vue.ref(),applicationRoundContextKey:vue.ref('exact-context'),
    workingBrowserLoadedContext:vue.ref(),fileAccessExplanation:vue.ref(),categories:vue.ref([]),directories:vue.ref([]),
    activeApprovalPrintTemplate:vue.ref(),paperDistributionRecords:vue.ref([]),
    categoryNameMap:vue.ref(),directoryNameMap:vue.ref(),directoryPathMap:vue.ref(),userNameMap:vue.ref(),departmentList:vue.ref([]),deptNameMap:vue.ref(),
    loadActiveObsoleteAction:async()=>{},loadActivePublishAction:async()=>{},loadControlledPrintRecords:async()=>{},flattenTree:()=>[]}
  host.isCurrentDetailLoad=(sequence,id,route)=>sequence===host.detailLoadSequence&&id===host.controlledFileId.value&&route===host.route.fullPath
  vm.runInNewContext(ts.transpileModule(declarations('src/views/dcc/controlled-file/detail/index.vue',['loadData'])+'\nexports.load=loadData',{
    compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,host)
  return host
}

test('canonical detail wire cannot be mixed with any plain round/task/type, even equal values',async()=>{
  const ctx=history(),query=body.buildDccBodyReadQuery(FILE,ctx)
  for(const extra of [{bpmRound:ctx.bpmRound},{taskId:ctx.taskId},{applicationType:ctx.applicationType},
    {bpmRound:'different-round'},{taskId:'different-task'},{applicationType:'UPLOAD'}]) {
    const h=wrapper()
    await assert.rejects(h.get(FILE,{bodyReadContext:ctx,...extra}))
    assert.equal(h.calls.length,0)
  }
  for(const extra of [{processInstanceId:ctx.bpmRound},{taskId:ctx.taskId},{applicationType:ctx.applicationType}])
    assert.throws(()=>resolveDetailReadSelection(FILE,{...query,...extra}))
})

test('invalid explicit context and orphan application identities never fall through to ordinary metadata',async()=>{
  for(const selection of [null,false,[],{bodyReadContext:null},{bodyReadContext:false},{bodyReadContext:{}},
    {bodyReadContext:{...history(),controlledFileId:'9007199254740995'}},{taskId:'old-task'},
    {applicationType:'REVISION'},{bpmRound:''},{bpmRound:['old-round']},
    {bpmRound:'old-round',applicationType:'UNKNOWN'},{bpmRound:'old-round',taskId:' old-task'}]) {
    const h=wrapper();await assert.rejects(h.get(FILE,selection));assert.equal(h.calls.length,0)
  }
  assert.throws(()=>resolveDetailReadSelection(FILE,{applicationType:'REVISION'}))
  const h=wrapper();await assert.rejects(h.get(Number(FILE),{bpmRound:'old-round'}));assert.equal(h.calls.length,0)
})

test('actual detail loader sends one exact canonical source which matches all reading lifecycle headers',async()=>{
  const ctx=history(),query=body.buildDccBodyReadQuery(FILE,ctx),api=wrapper()
  const host=loader(query,api.get)
  assert.equal(await host.exports.load(1,FILE,host.route.fullPath),true)
  assert.equal(api.calls[0].params.bodyReadContext,query.bodyReadContext)
  const requests=[],session={sessionId:'9223372036854775000',openedAt:'2026-10-15T10:00:00Z',startedAt:'2026-10-15T10:00:00Z',
    lastHeartbeatAt:'2026-10-15T10:00:00Z',endedAt:null,accumulatedSeconds:0,heartbeatIntervalSeconds:10,maxHeartbeatGapSeconds:15}
  const readingHost={exports:{},require:id=>id==='./bodyReadContext'?body:{default:{get:async req=>{requests.push(req);return{
    firstOpenedAt:null,lastOpenedAt:null,accumulatedSeconds:0,openedSessionCount:0}},post:async req=>{requests.push(req);return session}}}}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,'src/api/dcc/controlledFile/readingSession.ts'),'utf8'),{
    compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText,readingHost)
  const reading=readingHost.exports.createPreviewReadingApi({kind:'CONTROLLED_FILE',fileId:FILE,bodyReadContext:ctx},
    {viewerToken:'exact-token',accessEventCode:'event',watermarkTraceCode:'trace',viewerTokenId:'token-id',viewerTokenNonce:'nonce'})
  await reading.start('client')
  ctx.bpmRound='new-round';ctx.taskId='new-task'
  await reading.heartbeat(session.sessionId);await reading.stop(session.sessionId);await reading.summary()
  for(const req of requests) {
    assert.ok(req.url.includes('/'+FILE+'/'))
    assert.equal(req.headers['X-DCC-Body-Read-Context'],api.calls[0].params.bodyReadContext)
    assert.equal(req.data?.taskId,undefined)
  }
})

test('same Long file switching historical rounds discards the old detail response before publishing application identity',async()=>{
  const first=history(),firstQuery=body.buildDccBodyReadQuery(FILE,first),requests=[]
  let finishOld
  const pending=new Promise(resolve=>{finishOld=resolve})
  const second={...first,bpmRound:'other-round',taskId:'other-task'}
  const nextQuery=body.buildDccBodyReadQuery(FILE,second)
  const host=loader(firstQuery,async(id,selection)=>{
    requests.push(selection)
    return requests.length===1?pending:{id,versionNo:'A/1-2',fileName:'second-round.pdf'}
  })
  const oldRead=host.exports.load(1,FILE,host.route.fullPath)
  host.detailLoadSequence=2;host.route.query=nextQuery;host.route.fullPath='/detail/'+FILE+'?bodyReadContext='+nextQuery.bodyReadContext
  host.applicationRoundContextKey.value='second-round-context'
  assert.equal(await host.exports.load(2,FILE,host.route.fullPath),true)
  finishOld({id:FILE,versionNo:'A/1-2',fileName:'old-round.pdf'})
  assert.equal(await oldRead,false)
  assert.equal(host.fileDetail.value.fileName,'second-round.pdf')
  assert.equal(host.applicationApprovalRead.value.processInstanceId,'other-round')
  assert.equal(host.applicationApprovalRead.value.taskId,'other-task')
  assert.equal(host.workingBrowserLoadedContext.value.route,host.route.fullPath)
  assert.equal(requests[0].bodyReadContext.bpmRound,'old-round')
  assert.equal(requests[1].bodyReadContext.bpmRound,'other-round')
})
