const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript')
const base='src/views/dcc/controlled-file/basic-data/components/'
const role={reviewContract:'ROLE_SINGLE_REVIEW_V1',roleId:'9223372036854775706',roleCode:'dcc_project_reviewer',roleName:'项目审核员',canReview:true,
  candidates:[{userId:'9223372036854775707',username:'review-a',nickname:'审核甲'},{userId:'9223372036854775709',username:'review-b',nickname:'审核乙'}]}
const id='9007199254740993'
const pending={id,projectName:'正式项目',projectCode:'P-1',productName:'正式产品',productCode:'PRODUCT1',classification:'二类',status:'PENDING_REVIEW',
  reviewContract:role.reviewContract,reviewerRoleId:role.roleId,reviewerRoleCode:role.roleCode,canReview:true,
  configuredReviewerUserId:null,configuredReviewerUsername:null,configuredReviewerNickname:null}
const load=(file,deps={})=>{const c={exports:{},Error,require:name=>{if(name in deps)return deps[name];throw new Error('Unexpected import '+name)}}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const reviewer=load(base+'project-reviewer.ts')
const roleHelper=load(base+'project-product-role.ts',{'./project-reviewer':reviewer})
function apiHost(read=async()=>role,write=async()=>id){const calls=[];const api=load('src/api/dcc/controlledFile/projectProductRequests.ts',{
  '@/config/axios':{default:{get:async q=>{calls.push(['get',q]);return read(q)},post:async q=>{calls.push(['post',q]);return write(q)}}},
  '@/views/dcc/controlled-file/basic-data/components/project-reviewer':reviewer,
  '@/views/dcc/controlled-file/basic-data/components/project-product-role':roleHelper});return{api,calls}}

test('role readiness uses original query resource and preserves all exact candidates without a configured first reviewer',async()=>{
  const h=apiHost();const read=await h.api.getDccProjectReviewerRole()
  assert.equal(h.calls[0][1].url,'/dcc/project-product-requests/reviewer-role');assert.equal(h.calls[0][1].ignoreErrorMessage,true)
  assert.equal(read.roleId,role.roleId);assert.deepEqual(Array.from(read.candidates,x=>x.userId),role.candidates.map(x=>x.userId))
  assert.equal(read.reviewerUserId,undefined)
})
test('role readiness rejects malformed or numeric Long transport identities and preserves real service errors',async()=>{
  for(const patch of [{roleId:7},{roleId:9007199254740992},{roleCode:'doc_control'},{candidates:[]},{canReview:undefined},
    {candidates:[role.candidates[0],role.candidates[0]]},{candidates:[{...role.candidates[0],userId:7}]}])
    await assert.rejects(apiHost(async()=>({...role,...patch})).api.getDccProjectReviewerRole())
  await assert.rejects(apiHost(async()=>{throw new Error('角色停用或没有启用成员')}).api.getDccProjectReviewerRole(),/角色停用/)
})
test('detail and list require persisted role facts while genuine null-contract legacy remains readable',async()=>{
  assert.equal((await apiHost(async()=>pending).api.getDccProjectProductRequest(id)).reviewerRoleId,role.roleId)
  for(const patch of [{reviewContract:'UNKNOWN'},{reviewerRoleId:null},{reviewerRoleCode:null},{canReview:undefined},
    {configuredReviewerUserId:'7'},{configuredReviewerUsername:'fake'},{reviewContract:null},{reviewerRoleId:9007199254740992}]){
    const h=apiHost(async q=>q.url.endsWith('/pending')?[{...pending,...patch}]:({...pending,...patch}))
    await assert.rejects(h.api.getDccProjectProductRequest(id));await assert.rejects(h.api.getDccProjectProductRequests())
  }
  const legacy={...pending,reviewContract:null,reviewerRoleId:null,reviewerRoleCode:null,canReview:undefined,configuredReviewerUserId:'7'}
  assert.equal((await apiHost(async()=>legacy).api.getDccProjectProductRequest(id)).configuredReviewerUserId,'7')
})
test('new role action requires server qualification and query plus review without update or cached-role inference',()=>{
  assert.equal(typeof reviewer.canHandleProjectRequest,'function')
  const access={query:true,review:true,update:false,admin:false}
  assert.equal(reviewer.canHandleProjectRequest(pending,'7','review',access),true)
  for(const [row,permissions]of [[{...pending,canReview:false},access],[pending,{...access,query:false}],[pending,{...access,review:false,update:true,admin:true}],
    [{...pending,status:'COMPLETED'},access],[{...pending,reviewContract:'UNKNOWN'},access],[{...pending,configuredReviewerUserId:'7'},access]])
    assert.equal(reviewer.canHandleProjectRequest(row,'7','review',permissions),false)
  for(const action of ['approve','retry'])assert.equal(reviewer.canHandleProjectRequest({...pending,status:action==='approve'?'PENDING_APPROVAL':'WRITE_FAILED'},'7',action,{...access,update:true,admin:true}),false)
})
test('legacy reviewer and admin actions retain old contract, exact account and update permission boundaries',()=>{
  const row={...pending,reviewContract:null,reviewerRoleId:null,reviewerRoleCode:null,configuredReviewerUserId:'7',canReview:undefined}
  const access={query:true,review:false,update:true,admin:false}
  assert.equal(reviewer.canHandleProjectRequest(row,'7','review',access),true)
  assert.equal(reviewer.canHandleProjectRequest(row,'8','review',access),false)
  assert.equal(reviewer.canHandleProjectRequest({...row,status:'PENDING_APPROVAL'},'8','approve',{...access,admin:true}),true)
  assert.equal(reviewer.canHandleProjectRequest({...row,status:'PENDING_APPROVAL'},'8','approve',access),false)
})
test('create and resubmit keep exact resulting ID and reject client review authority instead of posting it',async()=>{
  const h=apiHost(),payload={projectName:'P',projectCode:'P1',productName:'产品',productCode:'P-1',classification:'二类',defaultAttributes:{},projectLeaderUserId:'9007199254740995',folderTemplateId:'9'}
  assert.equal(await h.api.createDccProjectProductRequest(payload),id)
  assert.equal(await h.api.resubmitDccProjectProductRequest(id,payload),id)
  for(const field of ['reviewContract','reviewerRoleId','configuredReviewerUserId','reviewerUserId','canReview']){
    const before=h.calls.length;await assert.rejects(h.api.createDccProjectProductRequest({...payload,[field]:'client-authority'}));assert.equal(h.calls.length,before)
  }
  await assert.rejects(apiHost(undefined,async()=>9007199254740992).api.createDccProjectProductRequest(payload))
})
test('review responses preserve request identity and single-stage outcome while invalid IDs or reasons never write',async()=>{
  const completed={...pending,status:'COMPLETED',canReview:false,reviewerUserId:'7',generatedProjectCodeId:'71',generatedProductCatalogId:'72'}
  const h=apiHost(undefined,async()=>completed)
  assert.equal((await h.api.reviewDccProjectProductRequest(id,{reason:'实际通过原因'},true)).status,'COMPLETED')
  assert.equal(h.calls[0][1].url,`/dcc/project-product-requests/${id}/review/approve`)
  assert.deepEqual(JSON.parse(JSON.stringify(h.calls[0][1].data)),{reason:'实际通过原因'})
  for(const changed of [{...completed,id:'9007199254740995'},{...completed,status:'PENDING_APPROVAL'},{...completed,status:'PENDING_REVIEW',canReview:true}])
    await assert.rejects(apiHost(undefined,async()=>changed).api.reviewDccProjectProductRequest(id,{reason:'实际原因'},true))
  const before=h.calls.length
  await assert.rejects(h.api.reviewDccProjectProductRequest(9007199254740992,{reason:'原因'},true))
  await assert.rejects(h.api.reviewDccProjectProductRequest(id,{reason:''},true));assert.equal(h.calls.length,before)
})

test('legacy approved write failure and retry failure remain truthful states instead of false completion',async()=>{
  const failed={...pending,reviewContract:null,reviewerRoleId:null,reviewerRoleCode:null,canReview:undefined,
    status:'WRITE_FAILED',writeErrorMessage:'原建档写入失败'}
  const h=apiHost(undefined,async()=>failed)
  assert.equal((await h.api.approveDccProjectProductRequest(id,{reason:'原批准意见'},true)).status,'WRITE_FAILED')
  assert.equal((await h.api.retryDccProjectProductRequestWrite(id,{reason:'原重试原因'})).writeErrorMessage,'原建档写入失败')
})
