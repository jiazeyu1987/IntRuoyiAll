import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import test from 'node:test'
const exports = {}
new Function('exports',ts.transpileModule(readFileSync(new URL('../src/views/dcc/controlled-file/relations/project-reference-contract.ts',import.meta.url),'utf8'),
  {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText)(exports)
const context={tenantId:'1',projectId:'9223372036854775700',folderId:'9223372036854775701'}
const view=(overrides={})=>({reference:{id:'9223372036854775702',projectId:context.projectId,folderId:context.folderId,masterId:'9007199254740993',selectedControlledFileId:'9007199254740995',createdBy:'9223372036854775707'},
  selectedVersion:{tenantId:'1',controlledFileId:'9007199254740995',masterId:'9007199254740993',projectId:'6',fileName:'所选源.pdf',fileNumber:'N1',versionNo:'A/1',status:'ACTIVE',controlled:true,pendingEffect:false,executable:true},referenceProjectCount:2,...overrides})
test('formal ReferenceView maps exact pinned identities, project count and named source with no body grant',()=>{
  const row=exports.mapReferenceRows([view()],context,{'6':'正式来源项目'})[0]
  assert.equal(row.reference.id,'9223372036854775702');assert.equal(row.selectedVersion.controlledFileId,'9007199254740995')
  assert.equal(row.referenceProjectCount,2);assert.equal(row.sourceProjectName,'正式来源项目')
  assert.equal(row.selectedVersion.canPreview,undefined)
})
test('obsolete pinned references retain traceability while mismatched tenant version folder and counts reject',()=>{
  const original=view(),obsolete=view({selectedVersion:{...original.selectedVersion,status:'OBSOLETE',controlled:false,executable:false}})
  assert.equal(exports.mapReferenceRows([obsolete],context,{'6':'源'})[0].selectedVersion.status,'OBSOLETE')
  for(const invalid of [
    view({selectedVersion:{...original.selectedVersion,tenantId:'2'}}),
    view({selectedVersion:{...original.selectedVersion,controlledFileId:'9007199254740997'}}),
    view({reference:{...original.reference,folderId:'22'}}),view({referenceProjectCount:-1}),view({referenceProjectCount:1.5})
  ])assert.throws(()=>exports.mapReferenceRows([invalid],context,{'6':'源'}))
  assert.throws(()=>exports.mapReferenceRows([original,original],context,{'6':'源'}))
  assert.throws(()=>exports.mapReferenceRows([original],context,{}))
})
test('target leader UI projection requires the enabled exact account; OWNER/admin labels never substitute',()=>{
  const project={projectLeaderUserId:'9223372036854775707'}
  assert.equal(exports.isEnabledTargetProjectLeader({id:'9223372036854775707',status:0},project),true)
  assert.equal(exports.isEnabledTargetProjectLeader({id:'9223372036854775707',status:1},project),false)
  assert.equal(exports.isEnabledTargetProjectLeader({id:'8',status:0,role:'OWNER'},project),false)
  assert.equal(exports.isEnabledTargetProjectLeader({id:'1',status:0,role:'admin'},project),false)
  assert.equal(exports.isEnabledTargetProjectLeader({id:'1',status:0},{projectLeaderUserId:null}),false)
})
test('directory nodes preserve B parent identity, filter inactive nodes and never infer NAS IDs',()=>{
  const tree=[{id:context.folderId,projectCodeId:context.projectId,parentId:'0',name:'空根',active:true,children:[
    {id:'9223372036854775703',projectCodeId:context.projectId,parentId:context.folderId,name:'子目录',active:true,children:[]}
  ]},{id:'9223372036854775704',projectCodeId:context.projectId,parentId:'0',name:'已停用',active:false,children:[]}]
  const nodes=exports.mapReferenceDirectoryNodes('1',context.projectId,tree)
  assert.equal(nodes.length,1);assert.equal(nodes[0].folderId,context.folderId);assert.equal(nodes[0].children[0].folderId,'9223372036854775703')
  assert.equal(nodes[0].projectId,context.projectId);assert.equal(nodes[0].directoryId,undefined)
})
test('all boundary identities accept safe numbers or exact Long strings but reject precision loss and overflow',()=>{
  assert.equal(exports.referenceIdentity(7),'7');assert.equal(exports.referenceIdentity('9223372036854775807'),'9223372036854775807')
  for(const id of [0,-1,'01',Number.MAX_SAFE_INTEGER+1,'9223372036854775808',null])assert.throws(()=>exports.referenceIdentity(id))
})

test('pinned-reference permission enrichment verifies every identity and version without granting body from reference existence',()=>{
  const row=exports.mapReferenceRows([view()],context,{'6':'源项目'})[0]
  const permission={controlledFileId:row.reference.selectedControlledFileId,tenantId:'1',masterId:row.reference.masterId,projectId:'6',projectName:'正式源项目',projectFolderId:'13',projectFolderName:'源质量目录',fileName:'所选源.pdf',fileNumber:'N1',versionNo:'A/1',status:'ACTIVE',controlled:true,pendingEffect:false,executable:true,canPreview:false,canEdit:false}
  const bound=exports.bindReferencePermissions(row,permission)
  assert.equal(bound.selectedVersion.controlledFileId,'9007199254740995')
  assert.equal(bound.selectedVersion.canPreview,false);assert.equal(bound.selectedVersion.folderName,'源质量目录')
  assert.equal(bound.sourceProjectName,'正式源项目')
  for(const [field,value] of [['controlledFileId','7'],['tenantId','2'],['masterId','8'],['projectId','9'],['fileName','wrong.pdf'],['fileNumber','N2'],['versionNo','A/2'],['status','OBSOLETE'],['canPreview',undefined]])assert.throws(()=>exports.bindReferencePermissions(row,{...permission,[field]:value}))
  assert.throws(()=>exports.bindReferencePermissions(row,{...permission,controlledFileId:9007199254740992}))
  assert.throws(()=>exports.bindReferencePermissions(row,{...permission,projectFolderName:null}))
})

test('strong readonly trace accepts only the separately authorized pinned version response',()=>{
  const row=exports.mapReferenceRows([view()],context,{'6':'源项目'})[0]
  const detail={id:row.reference.selectedControlledFileId,masterId:row.reference.masterId,dccProjectCodeId:'6',fileName:'所选源.pdf',fileNumber:'N1',versionNo:'A/1'}
  assert.doesNotThrow(()=>exports.assertReferenceTraceIdentity(row,detail))
  for(const [field,value] of [['id','7'],['masterId','8'],['dccProjectCodeId','9'],['versionNo','A/2'],['fileName','latest.pdf'],['tenantId','2']])assert.throws(()=>exports.assertReferenceTraceIdentity(row,{...detail,[field]:value}))
})
if(process.argv[2])test('actual B directory and D batch/list JSON retain large leader/reference/file identity into UI mapping',()=>{
  const actual=JSON.parse(readFileSync(process.argv[2],'utf8'))
  const ctx={tenantId:'1',projectId:actual.projectId,folderId:String(actual.folders[0].id)}
  const row=exports.mapReferenceRows(actual.created,ctx,{[actual.sourceProjectId]:actual.sourceProjectName})[0]
  assert.equal(row.selectedVersion.controlledFileId,actual.listed[0].selectedVersion.controlledFileId)
  assert.equal(row.referenceProjectCount,1);assert.equal(exports.isEnabledTargetProjectLeader(actual.leaderAccount,actual),true)
})
