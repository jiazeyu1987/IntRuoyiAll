const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const { body, resolveDetailReadSelection } = require('./dcc-detail-read-test-loader.cjs')
const root = path.resolve(__dirname, '../..')
const page = parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/index.vue'),'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('detail.ts',page,ts.ScriptTarget.Latest,true)
const declaration = name => ast.statements.find(s=>s.declarationList?.declarations.some(d=>d.name.text===name)).getText(ast)
const deferred = () => { let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject} }
const drain = async () => { for(let i=0;i<25;i++)await Promise.resolve();await vue.nextTick() }
const FILE='9007199254740993'
function host(options={}) {
  let tenant='31'
  const calls=[],messages=[],pending=deferred()
  const query={viewer:'1',...(options.query||{})}
  const route=vue.reactive({path:'/dcc/controlled-file/detail/'+FILE,params:{id:FILE},fullPath:'/dcc/controlled-file/detail/'+FILE+'?'+new URLSearchParams(query),query})
  const c={exports:{},...body,resolveDetailReadSelection,watch:vue.watch,computed:vue.computed,
    route,currentUserId:vue.ref('9007199254740993'),getTenantId:()=> '1',getVisitTenantId:()=>tenant,
    detailLoadSequence:0,dccSignatureEvidenceRequestSequence:0,controlledFileId:vue.ref(FILE),
    distributionReadMode:vue.ref(false),viewerMode:vue.ref(true),showLifecycleTraceSections:vue.ref(false),
    fileDetail:vue.ref(),workingBrowserLoadedContext:vue.ref(),applicationApprovalRead:vue.ref(),applicationRoundContextKey:vue.ref('ctx'),
    approvalProgressScope:vue.ref(),approvalProgressError:vue.ref(''),approvalProgressNotice:vue.ref(''),fileAccessExplanation:vue.ref(),accessExplanationError:vue.ref(''),
    approvalTodoTask:vue.ref(),approvalTaskList:vue.ref([]),stageProgressList:vue.ref([]),approvalLoading:vue.ref(false),
    paperDistributionRecords:vue.ref([]),controlledPrintRecords:vue.ref([]),controlledPrintRecordsLoading:vue.ref(false),controlledPrintRecordsError:vue.ref(''),
    dccSignatureEvidenceList:vue.ref([]),dccSignatureEvidenceTotal:vue.ref(0),dccSignatureEvidenceLoading:vue.ref(false),dccSignatureEvidenceError:vue.ref(''),
    categories:vue.ref([]),directories:vue.ref([]),activeApprovalPrintTemplate:vue.ref(),categoryNameMap:vue.ref(),directoryNameMap:vue.ref(),directoryPathMap:vue.ref(),userNameMap:vue.ref(),departmentList:vue.ref([]),deptNameMap:vue.ref(),
    checkPermi:()=>false,getControlledFile:async id=>{
      const actor=c.currentUserId.value;calls.push({tenant,actor,id})
      return calls.length===1?pending.promise:{id,versionNo:'A/1',fileName:'new-authority.pdf'}
    },getControlledFileAccessExplanation:async()=>{calls.push({kind:'access'});return{}},
    loadActiveObsoleteAction:async()=>{},loadActivePublishAction:async()=>{},loadControlledPrintRecords:async()=>{},loadDccSignatureEvidenceList:async()=>{},loadApprovalDetail:async()=>{},openControlledPrintDialogFromRoute:async()=>{},
    flattenTree:()=>[],resolveReadSideErrorMessage:e=>e.message,message:{error:value=>messages.push(value),warning:value=>messages.push(value)}}
  const names=['readDetailAuthority','detailLoadAuthority','isActiveControlledFileDetailRoute','isCurrentDetailLoad','isWorkingBrowserDetailCurrent','loadData','loadAccessExplanationOnly','reloadAll']
    .filter(name=>ast.statements.some(s=>s.declarationList?.declarations.some(d=>d.name.text===name)))
  const scope=vue.effectScope()
  const authWatch=ast.statements.find(s=>ts.isExpressionStatement(s)&&s.getText(ast).startsWith('watch(')
    &&s.getText(ast).includes('getVisitTenantId() || getTenantId()')&&s.getText(ast).includes('reloadAll()'))
  assert.ok(authWatch,'actual route/actor/tenant watcher')
  scope.run(()=>vm.runInNewContext(ts.transpileModule(names.map(declaration).join('\n')+'\n'+authWatch.getText(ast)+'\nexports.reload=reloadAll;exports.current=isWorkingBrowserDetailCurrent',{
    compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c))
  return{c,calls,messages,pending,scope,tenant:value=>{tenant=value},reload:()=>c.exports.reload()}
}

test('tenant cache changes before page refresh completes: old detail success cannot publish into the new tenant',async()=>{
  const h=host()
  try {
    const old=h.reload();h.tenant('32')
    h.pending.resolve({id:FILE,tenantId:'31',versionNo:'A/1',fileName:'old-tenant.pdf'})
    await old;await drain()
    assert.notEqual(h.c.fileDetail.value?.fileName,'old-tenant.pdf')
    assert.equal(h.c.workingBrowserLoadedContext.value,undefined)
  }finally{h.scope.stop()}
})

test('tenant cache changes before page refresh completes: old denial cannot query access or notify the new tenant',async()=>{
  const h=host()
  try {
    const old=h.reload();h.tenant('32');h.pending.reject(new Error('old tenant denied'))
    await old;await drain()
    assert.equal(h.calls.some(row=>row.kind==='access'),false)
    assert.deepEqual(h.messages,[])
  }finally{h.scope.stop()}
})

test('actual synchronous actor watcher keeps the new actor detail when previous actor response returns late',async()=>{
  const h=host()
  try {
    const old=h.reload();h.c.currentUserId.value='9007199254740995';await drain()
    h.pending.resolve({id:FILE,versionNo:'A/1',fileName:'old-actor.pdf'});await old;await drain()
    assert.equal(h.c.fileDetail.value.fileName,'new-authority.pdf')
    assert.equal(h.calls[1].actor,'9007199254740995')
  }finally{h.scope.stop()}
})

test('same file and pagination cannot accept a previous tenant signature response',async()=>{
  const h=host(),pending=deferred()
  try {
    Object.assign(h.c,{showSignatureTraceSections:vue.ref(true),dccSignatureEvidenceQueryParams:{pageNo:1,pageSize:10},
      getDccElectronicSignaturePage:()=>pending.promise})
    h.c.viewerMode.value=false;h.c.checkPermi=()=>true
    vm.runInNewContext(ts.transpileModule(declaration('loadDccSignatureEvidenceList')+'\nexports.signatures=loadDccSignatureEvidenceList',{
      compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h.c)
    const old=h.c.exports.signatures();h.tenant('32')
    pending.resolve({list:[{id:'old-signature',processInstanceId:'old-round'}],total:1});await old
    assert.equal(h.c.dccSignatureEvidenceList.value.length,0)
    assert.equal(h.c.dccSignatureEvidenceTotal.value,0)
  }finally{h.scope.stop()}
})

function panel(file, api, props, options={}) {
  let tenant='31'
  const events=[]
  const auth={getTenantId:()=> '1',getVisitTenantId:()=>tenant}
  const cache=new Map()
  function load(sourceFile) {
    if(cache.has(sourceFile))return cache.get(sourceFile)
    const c={exports:{},require:id=>{
      if(id==='vue')return vue
      if(id==='@/utils/auth')return auth
      if(id==='@/api/dcc/controlledFile/applicationRead')return api
      if(id==='@/api/dcc/controlledFile/relations')return {listHistoricalRelations:options.getHistory|| (async()=>[])}
      if(id==='@/api/common/filePreview')return {buildDccControlledFilePreviewSource:(id,context)=>({id,context})}
      if(id==='./presentation')return {formatControlledFileDateTime:value=>value}
      if(id.endsWith('.vue'))return{}
      if(id.startsWith('.'))return load(path.resolve(path.dirname(sourceFile),id+'.ts'))
      throw Error('unexpected dependency '+id)
    }}
    const text=fs.readFileSync(sourceFile,'utf8')
    const code=sourceFile.endsWith('.vue')?compileScript(parse(text).descriptor,{id:'authority-panel'}).content:text
    vm.runInNewContext(ts.transpileModule(code,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c)
    cache.set(sourceFile,c.exports);return c.exports
  }
  const component=load(path.join(root,'src/views/dcc/controlled-file/detail',file)).default
  component.render=()=>vue.h('section')
  const renderer=vue.createRenderer({createElement:type=>({type,children:[]}),createText:text=>({text}),createComment:()=>({}),
    insert:(n,p)=>p.children.push(n),remove(){},setText(){},setElementText(){},patchProp(){},parentNode:()=>null,nextSibling:()=>null})
  const app=renderer.createApp({render:()=>vue.h(component,{...props,'onSelection-change':value=>events.push(value)})})
  const mounted=app.mount({children:[]})
  return{app,state:mounted.$.subTree.component.setupState,events,tenant:value=>{tenant=value}}
}

test('history evidence from a previous tenant cannot emit a verified application after tenant cache changes',async()=>{
  let activeTenant='31'
  const pending=deferred(),round={controlledFileId:FILE,applicationType:'REVISION',bpmRound:'round-old',attributeRound:2}
  const h=panel('DetailApplicationHistory.vue',{
    getControlledFileApplicationRounds:async()=>[round],getControlledFileApplicationEvidence:()=>pending.promise
  },{fileId:FILE,expectedVersionNo:'A/1',primaryBpmRound:'round-old',lockPrimaryRound:true,contextKey:'same-route',
    readAuthority:()=>String(activeTenant)})
  try{
    await drain();h.tenant('32');activeTenant='32'
    pending.resolve({...round,versionNo:'A/1',recorded:false,unavailableReason:'NOT_RECORDED',actualAttributes:null,
      defaultSource:null,signatures:[],nodeRecords:[],relationEvidence:null})
    await drain()
    assert.equal(h.state.evidence,undefined)
    assert.ok(h.events.every(value=>value===null))
  }finally{h.app.unmount()}
})

test('return to BPM uses the exact historical wire round/task instead of the file native application',()=>{
  const context={intent:'APPROVAL_HISTORY',controlledFileId:FILE,expectedVersionNo:'A/1',applicationType:'OBSOLETE',
    bpmRound:'old-obsolete-round',taskId:'old-task'}
  const calls=[],h={exports:{},resolveDetailReadSelection,controlledFileId:vue.ref(FILE),route:{query:body.buildDccBodyReadQuery(FILE,context)},
    detailLoadSequence:1,isCurrentDetailLoad:()=>true,
    fileDetail:vue.ref({id:FILE,versionNo:'A/1',processInstanceId:'native-upload-round'}),activeObsoleteAction:vue.ref(null),
    activePublishAction:vue.ref(null),router:{push:value=>calls.push(value)}}
  vm.runInNewContext(ts.transpileModule(declaration('ensureCurrentDetailContext')+'\n'+declaration('openBpmDetail')+'\nexports.open=openBpmDetail',{
    compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h)
  h.exports.open()
  assert.equal(calls[0].query.id,'old-obsolete-round')
  assert.equal(calls[0].query.taskId,'old-task')
})

test('relations metadata from a previous tenant cannot become the selected source after tenant cache changes',async()=>{
  const pending=deferred()
  const h=panel('DetailRelationsPanel.vue',{getControlledFileRelationPermissions:()=>pending.promise},
    {file:{id:FILE,masterId:'700',dccProjectCodeId:'300',versionNo:'A/1',title:'old.pdf'},allowEdit:false})
  try{
    h.tenant('32')
    pending.resolve({tenantId:'31',controlledFileId:FILE,masterId:'700',projectId:'300',versionNo:'A/1',
      fileName:'old.pdf',fileNumber:'OLD',projectName:'old project',projectFolderId:null,projectFolderName:null})
    await drain()
    assert.equal(h.state.selectedSource,undefined)
    assert.equal(h.state.files.length,0)
  }finally{h.app.unmount()}
})

test('cached browse context cannot publish old-tenant reference metadata',async()=>{
  let tenant='31'
  const source=parse(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue'),'utf8')).descriptor.scriptSetup.content
  const tree=ts.createSourceFile('records.ts',source,ts.ScriptTarget.Latest,true)
  const names=['tenant','readBrowseAuthority','contextKey','load']
  const chunks=names.flatMap(name=>{
    const found=tree.statements.find(s=>s.declarationList?.declarations.some(d=>d.name.text===name))
    return found?[found.getText(tree)]:[]
  })
  const pending=deferred(),h={exports:{},computed:vue.computed,props:vue.reactive({file:{id:FILE,masterId:'700',dccProjectCodeId:'300'}}),
    route:vue.reactive({fullPath:'/same-file'}),getTenantId:()=> '1',getVisitTenantId:()=>tenant,
    metadata:vue.ref(),usageVisible:vue.ref(false),error:vue.ref(''),loading:vue.ref(false),generation:0,
    getControlledFileRelationPermissions:()=>{},getProjectReferenceUsage:()=>{},loadBrowseMetadata:()=>pending.promise}
  vm.runInNewContext(ts.transpileModule(chunks.join('\n')+'\nexports.load=load',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h)
  const old=h.exports.load();tenant='32';pending.resolve({referenceProjectCount:9,selected:{tenantId:'31',controlledFileId:FILE}});await old
  assert.equal(h.metadata.value,undefined)
})

test('loaded history rounds cannot issue an evidence read after raw authority changes',async()=>{
  let authority='actor99:tenant31';const calls=[]
  const round={controlledFileId:FILE,applicationType:'REVISION',bpmRound:'round-old',attributeRound:2}
  const h=panel('DetailApplicationHistory.vue',{
    getControlledFileApplicationRounds:async()=>[round],
    getControlledFileApplicationEvidence:async()=>{calls.push('evidence');return{...round,versionNo:'A/1',recorded:false,
      unavailableReason:'NOT_RECORDED',actualAttributes:null,defaultSource:null,signatures:[]}}
  },{fileId:FILE,expectedVersionNo:'A/1',contextKey:'same',readAuthority:()=>authority})
  try{
    await drain();assert.equal(h.state.rounds.length,1)
    authority='actor99:tenant32';await h.state.selectRound(h.state.roundKey(round))
    assert.equal(calls.length,0)
    assert.ok(h.events.every(value=>value===null))
    assert.equal(h.state.rounds.length,0);assert.equal(h.state.evidence,undefined)
  }finally{h.app.unmount()}
})

test('loaded fixed relations cannot issue target permission read after raw authority changes',async()=>{
  let authority='actor99:tenant31';const calls=[]
  const target='9007199254740995'
  const h=panel('DetailRelationsPanel.vue',{
    getControlledFileRelationPermissions:async id=>{calls.push(id);return{tenantId:'31',controlledFileId:id,masterId:id===FILE?'700':'701',
      projectId:'300',versionNo:id===FILE?'A/1':'B/1',fileName:'fixed.pdf',fileNumber:'FIXED',projectName:'P',
      projectFolderId:null,projectFolderName:null,status:'SUPERSEDED',canPreview:true,pendingEffect:false}}
  },{file:{id:FILE,masterId:'700',dccProjectCodeId:'300',versionNo:'A/1',title:'fixed.pdf'},allowEdit:false,readAuthority:()=>authority},
  {getHistory:async()=>[{relationId:'51',controlledFileId:target,masterId:'701',projectCodeId:'300',versionNo:'B/1',fileName:'fixed.pdf',fileNumber:'FIXED'}]})
  try{
    await drain();assert.equal(h.state.files.length,1);const before=calls.length
    authority='actor99:tenant32';await h.state.openPreview(h.state.files[0])
    assert.equal(calls.length,before);assert.equal(h.state.previewId,'')
    assert.equal(h.state.files.length,0);assert.equal(h.state.selectedSource,undefined)
  }finally{h.app.unmount()}
})

test('loaded detail cached-current flag cannot authorize preview/navigation after raw tenant switches',async()=>{
  const h=host(),calls=[]
  try{
    const loading=h.reload()
    h.pending.resolve({id:FILE,versionNo:'A/2',status:'ACTIVE',fileName:'loaded.pdf',versionHistory:[
      {id:'9007199254740995',status:'SUPERSEDED',versionNo:'A/1'}]})
    await loading;assert.equal(h.c.exports.current.value,true)
    Object.assign(h.c,{isWorkingBrowserDetailCurrent:h.c.exports.current,viewerIdentityError:vue.ref(''),
      router:{push:value=>calls.push(value)},window:{open:value=>calls.push(value)},
      isDccControlledFileActionAllowed:()=>true,buildControlledFileViewerPath:require('./dcc-fixed-version-preview-test-loader.cjs')().view.buildControlledFileViewerPath,
      activeObsoleteAction:vue.ref(null),activePublishAction:vue.ref(null)})
    Object.assign(h.c,{actionDialog:{visible:false,form:{}},actionDialogGeneration:1,actionDialogContextKey:vue.ref('same-context'),
      isDelayedEffectiveTask:vue.ref(false),isSignoffTask:vue.ref(false),isExternalReviewProcess:vue.ref(true),
      resetFourthNodeUploads:()=>{},resetTaskActionReadiness:()=>{}})
    const attachments={}
    new Function('exports',ts.transpileModule(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/attachment-facts.ts'),'utf8'),{
      compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText)(attachments)
    Object.assign(h.c,{assertAttachmentBelongsToVersion:attachments.assertAttachmentBelongsToVersion,
      selectedAttachmentPreview:vue.ref(),attachmentPreviewDialogVisible:vue.ref(false),
      buildWorkingBrowserRoute:file=>({path:'/browser',query:{fileId:file.id}})})
    const names=['ensureCurrentDetailContext','ensureDetailActionAllowed','resolveDetailBodyReadContext','openPreview','openViewerTraceability','openHistoryDetail','openBpmDetail','openActionDialog','actionDialogRequestIsCurrent',
      'openAttachmentPreview','openWorkingFileInBrowser','openControlledBrowserLocation','openTraceFileEvidence']
      .filter(name=>ast.statements.some(s=>s.declarationList?.declarations.some(d=>d.name.text===name)))
    vm.runInNewContext(ts.transpileModule(names.map(declaration).join('\n')+
      '\nexports.actions={openPreview,openViewerTraceability,openHistoryDetail,openBpmDetail,openActionDialog,actionDialogRequestIsCurrent,openAttachmentPreview,openWorkingFileInBrowser,openControlledBrowserLocation,openTraceFileEvidence}',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h.c)
    h.tenant('32')
    h.c.exports.actions.openPreview();h.c.exports.actions.openViewerTraceability()
    h.c.exports.actions.openHistoryDetail('9007199254740995');h.c.exports.actions.openBpmDetail()
    h.c.exports.actions.openWorkingFileInBrowser();h.c.exports.actions.openControlledBrowserLocation();h.c.exports.actions.openTraceFileEvidence({hasFileEvidence:true})
    h.c.exports.actions.openAttachmentPreview({attachmentId:'71',controlledFileId:FILE})
    assert.equal(h.c.attachmentPreviewDialogVisible.value,false)
    h.c.exports.actions.openActionDialog('reject');assert.equal(h.c.actionDialog.visible,false)
    h.c.actionDialog.visible=true
    assert.equal(h.c.exports.actions.actionDialogRequestIsCurrent({generation:h.c.actionDialogGeneration,contextKey:'same-context'}),false)
    assert.equal(calls.length,0)
    h.tenant('31');h.c.actionDialog.visible=false
    h.c.exports.actions.openActionDialog('reject');assert.equal(h.c.actionDialog.visible,true)
    assert.equal(h.c.exports.actions.actionDialogRequestIsCurrent({generation:h.c.actionDialogGeneration,contextKey:'same-context'}),true)
  }finally{h.scope.stop()}
})

test('loaded historical controlled-browser portal preserves its original raw version and wire round/task',async()=>{
  const original={intent:'APPROVAL_HISTORY',controlledFileId:FILE,expectedVersionNo:'A/1',applicationType:'OBSOLETE',bpmRound:'old-obsolete',taskId:'old-task'}
  const h=host({query:body.buildDccBodyReadQuery(FILE,original)}),opened=[]
  try{
    const loading=h.reload();h.pending.resolve({id:FILE,versionNo:'A/1',status:'ACTIVE',fileName:'historical.pdf',processInstanceId:'native-upload'});await loading
    Object.assign(h.c,{window:{open:url=>opened.push(url)},buildControlledFileViewerPath:require('./dcc-fixed-version-preview-test-loader.cjs')().view.buildControlledFileViewerPath})
    vm.runInNewContext(ts.transpileModule(['ensureCurrentDetailContext','resolveDetailBodyReadContext','openControlledBrowserLocation'].map(declaration).join('\n')+
      '\nexports.portal=openControlledBrowserLocation',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,h.c)
    h.c.exports.portal();const target=new URL(opened[0],'https://review.invalid')
    assert.deepEqual(JSON.parse(JSON.stringify(body.readDccBodyReadContextRoute(FILE,Object.fromEntries(target.searchParams)))),original)
  }finally{h.scope.stop()}
})
