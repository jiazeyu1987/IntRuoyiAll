const { test } = require('node:test'), assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm'), ts = require('typescript')
const root = path.resolve(__dirname, '../..'), read = file => fs.readFileSync(path.join(root, file), 'utf8')
const file='9007199254740993', round='obsolete-round'
const attrs = { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' }
const selection = { relatedMasterId:'300', selectedControlledFileId:'301', projectId:'20', fileNumber:'SOP-OLD', fileName:'旧关联.pdf', versionNo:'A/1', origin:'UPLOAD', supplemental:false }
const relation = () => ({ tenantId:'1', fileId:file, masterId:'10', projectId:'20', applicationType:'OBSOLETE',
  attributeRound:2, bpmRound:round, recorded:true, unavailableReason:null, relationDigest:'a'.repeat(64),
  originalUploadSelections:[selection], completeSelections:[selection], stages:[{
    signatureEvidenceStatus:'INVALID', signatureIdentityMatches:true,
    snapshot:{header:{id:'30',tenantId:'1',sourceFileId:file,sourceMasterId:'10',projectId:'20',processInstanceId:round,
      taskId:'task-O',obligationId:'O-dept-1',stageCode:'MATRIX_REVIEW',nodeInstanceId:'node-O',departmentId:'21',actorId:'22',
      signatureId:'23',signedAt:'2026-10-11 12:00:00',commandKey:'command-O',payloadHash:'b'.repeat(64),expectedDigest:'c'.repeat(64),
      completeDigest:'d'.repeat(64),actionType:'ASSIGN',reason:'原签名指派',applicationType:'OBSOLETE',attributeRound:2,formActionInstanceId:'24'},
      members:[{tenantId:'1',snapshotId:'30',relatedMasterId:'300',selectedControlledFileId:'301',projectId:'20',fileNumberSnapshot:'SOP-OLD',
        fileNameSnapshot:'旧关联.pdf',versionNoSnapshot:'A/1',origin:'UPLOAD',supplemental:false,assigneeUserId:null,dueAt:null}]}
  }] })
function load(response) {
  const sandbox={exports:{},Error,BigInt,Set,Date,require:name=>name==='@/config/axios'?{default:{get:async()=>response}}:{validateAttributes:value=>value}}
  vm.runInNewContext(ts.transpileModule(read('src/api/dcc/controlledFile/applicationRead.ts'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,sandbox)
  return sandbox.exports
}
const evidence = value => ({ controlledFileId:file,versionNo:'A/1',applicationType:'OBSOLETE',bpmRound:round,attributeRound:2,
  recorded:true,unavailableReason:null,defaultSource:attrs,actualAttributes:attrs,signatures:[],relationEvidence:value })
test('history preserves the original exact selected set and independent invalid signature state',async()=>{
  const result=await load(evidence(relation())).getControlledFileApplicationEvidence(file,'OBSOLETE',round)
  assert.equal(result.relationEvidence.stages[0].signatureEvidenceStatus,'INVALID')
  assert.equal(result.relationEvidence.completeSelections[0].selectedControlledFileId,'301')
  assert.equal((await load(evidence(null)).getControlledFileApplicationEvidence(file,'OBSOLETE',round)).relationEvidence,null)
})
test('cross-round, foreign-file, lossy stage identity and mixed member identities are rejected',async()=>{
  for(const mutate of [r=>{r.bpmRound='upload-round'},r=>{r.fileId='99'},r=>{r.stages[0].snapshot.header.signatureId=9007199254740992},
    r=>{r.stages[0].snapshot.members[0].snapshotId='31'}]) {
    const r=relation();mutate(r)
    await assert.rejects(load(evidence(r)).getControlledFileApplicationEvidence(file,'OBSOLETE',round))
  }
})
test('application history passes only the selected persisted round into readonly stage evidence',()=>{
  const source=read('src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue')
  const binding=source.match(/<DetailRelationStageEvidence\b[\s\S]*?\/>/)?.[0]
  assert.ok(binding,'the actual history panel must bind the formal stage projection')
  for(const prop of ['evidence','tenant-id','file-id','application-type','bpm-round','attribute-round','context-key'])
    assert.match(binding,new RegExp(':'+prop+'='))
  assert.match(binding,/evidence\.relationEvidence/)
  assert.doesNotMatch(binding,/currentActive|latestControlled/)
})
