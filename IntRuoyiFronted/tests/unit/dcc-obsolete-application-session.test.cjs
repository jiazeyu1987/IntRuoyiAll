const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const filename = 'src/views/dcc/controlled-file/detail/obsolete-application.ts'
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const draft = { id: '901', instanceCode: 'FORM-901', status: 'DRAFT', controlledFileId: '20', dccProjectCodeId: '5',
  applicationEntryMode: 'DRAFT', attributesSubmitted: false, bpmProcessInstanceId: null,
  versionNo: 'A/1', attributeRound: 2, applicationContextKey: 'DCC_OBSOLETE:901:FORM-901',
  defaultSource: attrs, actualAttributes: attrs, reason: '实际原因', idempotencyKey: 'real-key',
  selectedSignoffDepartmentIds: ['51'], preparationReady: false, unavailableReason: 'DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY' }
const input = { reason: '实际原因', projectAttributes: attrs, selectedSignoffDepartmentIds: ['51'] }
function host(read = async () => null, write = async (_id, data) => ({ ...draft, reason: data.reason, idempotencyKey:data.idempotencyKey }), currentInput) {
  assert.ok(fs.existsSync(filename), 'formal obsolete dialog session helper exists')
  const c = { exports: {}, Error }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
  const scope = { key: 'actor1-tenant1-file20-A1-generation1', fileId: '20', projectId: '5', versionNo: 'A/1' }
  const calls = []
  const session = new c.exports.ObsoleteApplicationSession(() => scope, read, async (id, data) => {
    calls.push({ id, data }); return write(id, data)
  }, () => 'generated-key', currentInput)
  return { session, scope, calls }
}
test('initial read never writes or creates a BPM and real saved response supplies reopened inputs', async () => {
  const state = host(async () => draft)
  assert.equal(await state.session.load(), true)
  assert.equal(state.calls.length, 0)
  assert.equal(state.session.draft.reason, '实际原因')
  assert.equal(state.session.draft.defaultSource, attrs)
  assert.equal(state.session.idempotencyKey, 'real-key')
})
test('OPEN then SAVE use the same real numeric form without guessing or changing request key', async () => {
  const state = host()
  await state.session.load()
  assert.equal(await state.session.execute('OPEN', input), true)
  assert.equal(await state.session.execute('SAVE', { ...input, reason: '明确修改' }, false), true)
  assert.equal(state.calls[0].data.phase, 'OPEN')
  assert.equal(state.calls[0].data.formInstanceId, undefined)
  assert.equal(state.calls[1].data.prepareExecution, false)
  assert.equal(state.calls[1].data.formInstanceId, '901')
  assert.equal(state.calls[1].data.attributeRound, 2)
  assert.equal(state.calls[1].data.applicationContextKey, draft.applicationContextKey)
  assert.equal(state.calls[1].data.idempotencyKey, state.calls[0].data.idempotencyKey)
})
test('not-ready provider still allows complete draft SAVE but refuses SUBMIT before network write', async () => {
  const state = host(async () => draft)
  await state.session.load()
  assert.equal(await state.session.execute('SAVE', input, false), true)
  const before = state.calls.length
  assert.equal(await state.session.execute('SUBMIT', input), false)
  assert.equal(state.calls.length, before)
  assert.match(state.session.error, /未就绪|未准备/)
})
test('changed local inputs cannot be silently saved by SUBMIT and cancelled confirmation has no submit', async () => {
  const state = host(async () => ({ ...draft, preparationReady: true, unavailableReason: null }))
  await state.session.load()
  assert.equal(await state.session.execute('SUBMIT', { ...input, reason: '未保存的新原因' }), false)
  assert.equal(state.calls.length, 0)
  assert.match(state.session.error, /保存/)
})
test('late response from a closed or different file/actor cannot publish another draft or fake readiness', async () => {
  let finish
  const state = host(() => new Promise(resolve => { finish = resolve }))
  const pending = state.session.load()
  state.scope.key = 'new-actor-or-dialog'
  finish(draft)
  assert.equal(await pending, false)
  assert.equal(state.session.draft, undefined)
})
test('duplicate action runs once and explicit service failure remains visible with prior input retained', async () => {
  let finish
  const state = host(async () => draft, () => new Promise(resolve => { finish = resolve }))
  await state.session.load()
  const pending = state.session.execute('SAVE', input, false)
  assert.equal(await state.session.execute('SAVE', input, false), false)
  assert.equal(state.calls.length, 1)
  finish(draft)
  assert.equal(await pending, true)
  const failed = host(async () => draft, async () => { throw new Error('actual attribute CAS failed') })
  await failed.session.load()
  assert.equal(await failed.session.execute('SAVE', input, false), false)
  assert.equal(failed.session.draft.reason, '实际原因')
  assert.equal(failed.session.error, 'actual attribute CAS failed')
})
test('real response from another file or version cannot become current formal form evidence', async () => {
  for (const change of [{ controlledFileId: '21' }, { versionNo: 'B/1' }, { dccProjectCodeId: '6' }]) {
    const state = host(async () => ({ ...draft, ...change }))
    assert.equal(await state.session.load(), false)
    assert.equal(state.session.draft, undefined)
    assert.match(state.session.error, /身份/)
  }
})
test('a same-dialog input edit during a write cannot publish the earlier response into the new input', async () => {
  let finish
  const state = host(async () => draft, () => new Promise(resolve => { finish = resolve }))
  await state.session.load()
  const pending = state.session.execute('SAVE', input, false)
  state.session.invalidateInputs()
  finish({ ...draft, reason: '旧响应' })
  assert.equal(await pending, false)
  assert.equal(state.session.draft.reason, '实际原因')
})
test('rule preview and explicit preparation refuse unsaved edits and preserve the current saved draft', async () => {
  for (const phase of ['PREVIEW', 'SAVE']) {
    const state = host(async () => draft)
    await state.session.load()
    assert.equal(await state.session.execute(phase, { ...input, reason: '尚未保存的新原因' }, true), false)
    assert.equal(state.calls.length, 0)
    assert.equal(state.session.draft.reason, draft.reason)
    assert.match(state.session.error, /保存/)
  }
})

const sourceExpectation = { reworkSourceFormInstanceId: '801', reworkSourceFormInstanceCode: 'FORM-801',
  reworkSourceBpmProcessInstanceId: 'old-bpm', reworkSourceAttributeRound: 1 }
const sourceFive = { ...sourceExpectation, reworkSourceDigest: 'a'.repeat(64) }
const matrix = { formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2', routeId: '40', routeVersionNo: 1, matrixDigest: '1'.repeat(64) }
const relationEvidence = { tenantId: '1', fileId: '20', masterId: '8', projectId: '5', applicationType: 'OBSOLETE', attributeRound: 1, bpmRound: 'old-bpm',
  recorded: false, unavailableReason: 'RELATION_STAGE_NOT_RECORDED', relationDigest: 'b'.repeat(64), stages: [], originalUploadSelections: [{relatedMasterId:'6',selectedControlledFileId:'61',projectId:'5',fileNumber:'SOP-6',fileName:'原关联.pdf',versionNo:'B/1',origin:'UPLOAD',supplemental:false}], completeSelections: [{relatedMasterId:'6',selectedControlledFileId:'61',projectId:'5',fileNumber:'SOP-6',fileName:'原关联.pdf',versionNo:'B/1',origin:'UPLOAD',supplemental:false}] }
const rework = { ...draft, ...sourceFive, id:'801', instanceCode:'FORM-801', attributeRound:1, applicationContextKey:'DCC_OBSOLETE:801:FORM-801',
  applicationEntryMode:'REWORK_SOURCE', status:'REJECTED', bpmProcessInstanceId:'old-bpm', idempotencyKey:'old-key', attributesSubmitted:true,
  reworkSourceStatus:'REJECTED', reworkSourceVersionNo:'A/1', reworkSourceApprovalMatrix:matrix, reworkSourceRelations:relationEvidence,
  reworkSourceRelationDigest:relationEvidence.relationDigest }
const candidate = { ...sourceFive, reworkSourceStatus:'REJECTED', reworkSourceVersionNo:'A/1', reason:'原拒绝原因', available:true, unavailableReason:null }
const reworkDraft={...draft,...sourceFive,reworkSourceApprovalMatrix:matrix,reworkSourceRelations:relationEvidence,reworkSourceRelationDigest:relationEvidence.relationDigest}

test('IC30 terminal source is separate from the writable draft and keeps M1 and recorded-false real relationships', async () => {
  const state = host(async () => rework)
  await state.session.load()
  assert.equal(state.session.draft, undefined)
  assert.equal(state.session.source.id, '801'); assert.equal(state.session.source.reworkSourceApprovalMatrix.routeVersionNo, 1)
  assert.equal(state.session.source.reworkSourceRelations.recorded, false)
  assert.equal(state.session.source.reworkSourceRelations.completeSelections[0].selectedControlledFileId, '61')
  for (const phase of ['SAVE','PREVIEW','SUBMIT']) assert.equal(await state.session.execute(phase,input,phase==='SAVE'?false:undefined),false)
  assert.equal(state.calls.length,0)
})
test('IC30 multiple-source read preserves every candidate and requires explicit complete source GET without a default', async () => {
  const reads=[]
  const state=host(async (id,source)=> { reads.push({id,source}); return source ? rework : { applicationEntryMode:'SOURCE_SELECTION_REQUIRED',controlledFileId:'20',dccProjectCodeId:'5',versionNo:'A/1',preparationReady:false,unavailableReason:'SOURCE_SELECTION_REQUIRED',reworkSourceCandidates:[candidate,{...candidate,reworkSourceFormInstanceId:'802',reworkSourceFormInstanceCode:'FORM-802',reworkSourceBpmProcessInstanceId:'other-bpm',reworkSourceDigest:null,available:false,unavailableReason:'旧轮签名证据缺失'}] } })
  await state.session.load()
  assert.equal(state.session.draft,undefined);assert.equal(state.session.source,undefined);assert.equal(state.session.candidates.length,2)
  assert.equal(await state.session.execute('OPEN',input),false);assert.equal(state.calls.length,0)
  assert.equal(await state.session.selectSource(state.session.candidates[1]),false);assert.equal(reads.length,1)
  assert.equal(await state.session.selectSource(state.session.candidates[0]),true)
  assert.deepEqual(JSON.parse(JSON.stringify(reads[1].source)),sourceExpectation)
  assert.equal(state.session.source.reworkSourceDigest,sourceFive.reworkSourceDigest)
})
test('IC30 source OPEN creates a distinct new key and sends only source expectations before normal draft phases', async () => {
  const state=host(async()=>rework, async(_id,data)=>({...reworkDraft,reason:data.reason,actualAttributes:data.projectAttributes,idempotencyKey:data.idempotencyKey}))
  await state.session.load();assert.notEqual(state.session.idempotencyKey,rework.idempotencyKey)
  assert.equal(await state.session.execute('OPEN',{...input,reason:'明确重提'}),true)
  const open=state.calls[0].data
  assert.equal(open.phase,'OPEN');assert.equal(open.idempotencyKey,'generated-key')
  for(const field of ['formInstanceId','formInstanceCode','attributeRound','applicationContextKey'])assert.equal(open[field],undefined)
  for(const [key,value] of Object.entries(sourceFive))assert.equal(open[key],value)
  assert.equal(state.session.draft.id,'901');assert.equal(state.session.source,undefined)
  assert.equal(await state.session.execute('SAVE',{...input,reason:'明确重提'},false),true)
  assert.equal(state.calls[1].data.formInstanceId,'901');assert.equal(state.calls[1].data.attributeRound,2)
})
test('IC30 broken source tuple, empty relation evidence and reuse of an old form cannot report a writable draft', async () => {
  for(const patch of [{id:'802'}, {attributesSubmitted:false}, {reworkSourceRelations:null}, {reworkSourceRelationDigest:'c'.repeat(64)}, {status:'REWORKING'}]){
    const state=host(async()=>({...rework,...patch}));assert.equal(await state.session.load(),false);assert.equal(state.session.draft,undefined)
  }
  const state=host(async()=>rework,async()=>({...rework,applicationEntryMode:'DRAFT',status:'DRAFT',attributesSubmitted:false,bpmProcessInstanceId:null}))
  await state.session.load();assert.equal(await state.session.execute('OPEN',input),false);assert.equal(state.session.draft,undefined)
})
test('IC30 source selection and explicit OPEN ignore old scopes and detect business input mutation during transport', async () => {
  let release
  const state=host(async()=>rework,()=>new Promise(r=>release=r));await state.session.load()
  const changing={...input,reason:'本次输入'};const pending=state.session.execute('OPEN',changing)
  for(let i=0;i<8 && !release;i++) await Promise.resolve()
  assert.equal(state.calls.length,1,state.session.error)
  changing.reason='新的输入';release({...draft,...sourceFive});assert.equal(await pending,false);assert.equal(state.session.draft,undefined)
  assert.equal(state.session.source.id,'801');assert.match(state.session.error,/改变|修改/)
})

test('IC30 source changes block OPEN before write and require an explicit read while retaining prior inputs',async()=>{
  let changed=false
  const state=host(async()=>changed?{...rework,reworkSourceDigest:'c'.repeat(64)}:rework)
  await state.session.load();changed=true
  assert.equal(await state.session.execute('OPEN',input),false);assert.equal(state.calls.length,0)
  assert.equal(state.session.sourceRefreshRequired,true);assert.equal(state.session.source.reason,rework.reason)
  assert.equal(await state.session.execute('OPEN',input),false);assert.equal(state.calls.length,0)
  assert.equal(await state.session.load(),true);assert.equal(state.session.sourceRefreshRequired,false)
  assert.equal(state.session.source.reworkSourceDigest,'c'.repeat(64))
})
test('IC30 candidate response from previous actor or tenant cannot replace the current source or clear a newer error',async()=>{
  for(const field of ['actorId','tenantId']){
    let release
    const state=host(async(_id,expected)=>expected?new Promise(r=>release=r):{applicationEntryMode:'SOURCE_SELECTION_REQUIRED',controlledFileId:'20',dccProjectCodeId:'5',versionNo:'A/1',preparationReady:false,unavailableReason:'SOURCE_SELECTION_REQUIRED',reworkSourceCandidates:[candidate,{...candidate,reworkSourceFormInstanceId:'802',reworkSourceFormInstanceCode:'FORM-802'}]})
    state.scope.actorId='99';state.scope.tenantId='1';await state.session.load()
    const pending=state.session.selectSource(state.session.candidates[0]);state.scope[field]='2'
    state.session.clear();state.session.error='新的当前错误';release(rework)
    assert.equal(await pending,false);assert.equal(state.session.source,undefined);assert.equal(state.session.draft,undefined)
    assert.equal(state.session.error,'新的当前错误');assert.equal(state.session.busy,false)
  }
})
test('IC30 OPEN rejects a response with an old key and source-derived preview rejects current M2',async()=>{
  const wrong=host(async()=>rework,async()=>({...draft,...sourceFive,idempotencyKey:'old-key'}));await wrong.session.load()
  assert.equal(await wrong.session.execute('OPEN',input),false);assert.equal(wrong.session.draft,undefined)
  const newDraft={...draft,...sourceFive,reworkSourceApprovalMatrix:matrix,reworkSourceRelations:relationEvidence,reworkSourceRelationDigest:relationEvidence.relationDigest}
  const changed=host(async()=>newDraft,async()=>({...newDraft,approvalMatrixAfter:{...matrix,matrixDigest:'c'.repeat(64)}}));await changed.session.load()
  assert.equal(await changed.session.execute('PREVIEW',input),false);assert.equal(changed.session.draft.approvalMatrixAfter,undefined)
  assert.match(changed.session.error,/原规则/)
})
test('IC30 a separately reactive business input changes during write cannot publish an earlier saved response',async()=>{
  let release
  const current={...input};const state=host(async()=>rework,()=>new Promise(r=>release=r),()=>current)
  await state.session.load();const pending=state.session.execute('OPEN',{...current})
  for(let i=0;i<8&&!release;i++)await Promise.resolve()
  current.reason='正在编辑的新原因';release({...draft,...sourceFive,idempotencyKey:'generated-key'})
  assert.equal(await pending,false);assert.equal(state.session.draft,undefined);assert.equal(current.reason,'正在编辑的新原因')
})
test('IC30 source provenance cannot disappear during a later draft phase and permit a new matrix',async()=>{
  const state=host(async()=>reworkDraft,async()=>({...draft,approvalMatrixAfter:matrix}))
  await state.session.load();assert.equal(await state.session.execute('PREVIEW',input),false)
  assert.equal(state.session.draft.reworkSourceFormInstanceId,'801');assert.match(state.session.error,/来源|摘要/)
})
test('IC30 edited business input during selected-source GET cannot publish the source over that input',async()=>{
  let release
  const current={...input}
  const state=host(async(_id,expected)=>expected?new Promise(r=>release=r):{applicationEntryMode:'SOURCE_SELECTION_REQUIRED',controlledFileId:'20',dccProjectCodeId:'5',versionNo:'A/1',preparationReady:false,unavailableReason:'SOURCE_SELECTION_REQUIRED',reworkSourceCandidates:[candidate,{...candidate,reworkSourceFormInstanceId:'802',reworkSourceFormInstanceCode:'FORM-802'}]},undefined,()=>current)
  await state.session.load();const pending=state.session.selectSource(state.session.candidates[0])
  current.reason='读取时修改的输入';release(rework)
  assert.equal(await pending,false);assert.equal(state.session.source,undefined);assert.equal(state.session.sourceSelectionRequired,true)
  assert.equal(current.reason,'读取时修改的输入')
})

test('IC30 exact same-key SUBMIT replay accepts IN_APPROVAL or EFFECTIVE with new BPM and locks the submitted session',async()=>{
  const prepared={...reworkDraft,preparationReady:true,unavailableReason:null,approvalMatrixBefore:matrix,approvalMatrixAfter:matrix}
  for(const status of ['IN_APPROVAL','EFFECTIVE']){
    let attempts=0
    const state=host(async()=>prepared,async()=>{
      if(++attempts===1)throw new Error('实际SUBMIT响应连接中断')
      return {...prepared,status,bpmProcessInstanceId:'actual-new-bpm',attributesSubmitted:true}
    })
    assert.equal(await state.session.load(),true)
    assert.equal(await state.session.execute('SUBMIT',input),false)
    assert.equal(state.session.draft.status,'DRAFT');assert.equal(state.session.error,'实际SUBMIT响应连接中断')
    assert.equal(await state.session.execute('SUBMIT',input),true,state.session.error)
    assert.equal(state.calls.length,2)
    assert.deepEqual(JSON.parse(JSON.stringify(state.calls[0].data)),JSON.parse(JSON.stringify(state.calls[1].data)))
    assert.equal(state.calls[1].data.formInstanceId,'901');assert.equal(state.calls[1].data.idempotencyKey,'real-key')
    assert.equal(state.session.draft.status,status);assert.equal(state.session.draft.bpmProcessInstanceId,'actual-new-bpm')
    assert.equal(state.session.draft.attributesSubmitted,true);assert.equal(state.session.error,'')
    assert.equal(state.session.draft.reworkSourceApprovalMatrix,matrix)
    assert.equal(state.session.draft.reworkSourceRelationDigest,relationEvidence.relationDigest)
    assert.equal(state.session.isSaved(input),false)
    for(const phase of ['OPEN','SAVE','PREVIEW','SUBMIT'])assert.equal(await state.session.execute(phase,input),false)
    assert.equal(state.calls.length,2)
  }
})

test('IC30 SUBMIT never accepts failed terminal status, unfrozen or borrowed target and source proof as successful replay',async()=>{
  const prepared={...reworkDraft,preparationReady:true,unavailableReason:null,approvalMatrixBefore:matrix,approvalMatrixAfter:matrix}
  const success={...prepared,status:'EFFECTIVE',bpmProcessInstanceId:'actual-new-bpm',attributesSubmitted:true}
  const patches=[...['REJECTED','ABANDONED','EFFECT_FAILED','DRAFT'].map(status=>({status})),
    {bpmProcessInstanceId:null},{bpmProcessInstanceId:'old-bpm'},{attributesSubmitted:false},
    {id:'801'},{instanceCode:'FORM-801'},{attributeRound:1},{applicationContextKey:rework.applicationContextKey},
    {idempotencyKey:'old-key'},{reworkSourceDigest:'c'.repeat(64)},
    {reworkSourceApprovalMatrix:{...matrix,matrixDigest:'c'.repeat(64)}},
    {approvalMatrixAfter:{...matrix,matrixDigest:'c'.repeat(64)}},
    {reworkSourceRelations:null},{reworkSourceRelationDigest:'c'.repeat(64)}]
  for(const patch of patches){
    const state=host(async()=>prepared,async()=>({...success,...patch}));assert.equal(await state.session.load(),true)
    assert.equal(await state.session.execute('SUBMIT',input),false,JSON.stringify(patch))
    assert.equal(state.calls.length,1);assert.equal(state.session.draft,prepared)
    assert.equal(state.session.draft.attributesSubmitted,false);assert.equal(state.session.isSaved(input),true)
    assert.notEqual(state.session.error,'')
  }
})
