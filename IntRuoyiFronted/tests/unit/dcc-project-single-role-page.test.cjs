const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue')
const {parse,compileScript}=require('vue/compiler-sfc')
const base='src/views/dcc/controlled-file/basic-data/components/'
const source=fs.readFileSync(base+'ProductCatalogTabPanel.vue','utf8'),script=parse(source).descriptor.scriptSetup.content
const ast=ts.createSourceFile('panel.ts',script,ts.ScriptTarget.Latest,true)
const id='9007199254740993',nextId='9007199254740995',actor='9223372036854775707'
const attrs={targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'}
const role={reviewContract:'ROLE_SINGLE_REVIEW_V1',roleId:'9223372036854775706',roleCode:'dcc_project_reviewer',roleName:'项目审核员',canReview:true,candidates:[{userId:actor,username:'review-a',nickname:'审核甲'},{userId:'9223372036854775709',username:'review-b',nickname:'审核乙'}]}
const row=(requestId=id,patch={})=>({id:requestId,reviewContract:role.reviewContract,reviewerRoleId:role.roleId,reviewerRoleCode:role.roleCode,canReview:true,status:'PENDING_REVIEW',projectName:'原项目',projectCode:'P-01',productName:'原产品',productCode:'PRODUCT1',classification:'二类',applicantUserId:actor,...patch})
const defer=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return{promise,resolve,reject}}
const load=(file,deps={})=>{const c={exports:{},Error,require:name=>{if(name in deps)return deps[name];throw new Error(name)}};vm.runInNewContext(ts.transpileModule(fs.readFileSync(file,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const reviewers=load(base+'project-reviewer.ts'),roles=load(base+'project-product-role.ts',{'./project-reviewer':reviewers})
function host(options={}){
  const calls={reads:[],writes:[],success:[],confirm:[],catalog:0},permissions=new Set(options.permissions||['dcc:project-code:query','dcc:project-code:review','dcc:project-code:create'])
  const tenant={id:'1'},records=new Map([[id,row()],[nextId,row(nextId)]]),ref=vue.ref
  const c={exports:{},Error,JSON,Object,Array,Promise,...reviewers,...roles,
    checkPermi:values=>values.some(p=>permissions.has(p)),getTenantId:()=>tenant.id,getVisitTenantId:()=>undefined,
    userStore:{getUser:{id:actor,username:'review-a'}},route:{path:'/mdm/product-catalog',query:{}},router:{push:async()=>{}},
    projectProductDialogSequence:1,projectProductRequestReadSequence:0,projectProductDialogVisible:ref(true),projectProductMode:ref(options.mode||'create'),
    projectProductLoading:ref(false),projectProductRequestsLoading:ref(false),projectProductError:ref(''),reviewerConfiguration:ref(),
    projectProductRequests:ref([row()]),projectProductExpandedRequestIds:ref([]),projectProductExactRequestId:ref(options.mode==='records'?id:undefined),projectProductActionReason:ref(''),
    projectProductResubmitRequestId:ref(),projectProductRejectReason:ref(''),
    projectProductForm:vue.reactive({projectName:'新项目',projectCode:'P-02',productCode:'PRODUCT2',productName:'新产品',classification:'二类',projectLeaderUserId:'7',folderTemplateId:'9',defaultAttributes:attrs,creationReason:'真实新建原因',remark:'保留备注'}),
    projectProductFormRef:ref({validate:async()=>true,resetFields(){},clearValidate(){}}),emptyAttributes:()=>({targetMarkets:[]}),validateAttributes:v=>v,
    projectLeaderUsers:ref([{id:'7',nickname:'负责人',username:'leader'}]),folderTemplates:ref([{id:'9',name:'正式模板',active:true,structureJson:'{"nodes":[]}'}]),
    getSimpleUserList:async()=>{calls.reads.push('users');return[{id:'7',nickname:'负责人',username:'leader'}]},getFolderTemplates:async()=>{calls.reads.push('templates');return[{id:'9',name:'正式模板',active:true}]},
    message:{success:m=>calls.success.push(m),warning:m=>calls.success.push(m)},getList:async()=>{calls.catalog++},
    window:{prompt:()=>options.reason??'本次正式审核原因'},confirmProjectProductApplication:async(...values)=>{calls.confirm.push(values);if(options.confirm)await options.confirm(c)},
    canResubmitProjectProductRequest:()=>true,restoreRejectedProjectProductForm:r=>({...r})}
  const api=load('src/api/dcc/controlledFile/projectProductRequests.ts',{
    '@/views/dcc/controlled-file/basic-data/components/project-reviewer':reviewers,'@/views/dcc/controlled-file/basic-data/components/project-product-role':roles,
    '@/config/axios':{default:{get:async q=>{calls.reads.push(q.url);if(q.url.endsWith('/reviewer-role'))return options.role?options.role():structuredClone(role)
      if(q.url.endsWith('/pending'))return options.pending?options.pending():[structuredClone(records.get(id))]
      const requestId=q.url.split('/').pop();return options.read?options.read(requestId):structuredClone(records.get(requestId))},
      post:async q=>{calls.writes.push(q);if(options.write)return options.write(q)
        if(q.url.endsWith('/create')||q.url.endsWith('/resubmit'))return nextId
        const requestId=q.url.split('/').at(-3),result=row(requestId,{status:q.url.endsWith('/reject')?'REJECTED':'COMPLETED',canReview:false,reviewerUserId:actor})
        records.set(requestId,result);return result}}}})
  Object.assign(c,api)
  const names=['projectRequestAccess','projectProductActorScope','canPerformProjectAction','resetProjectProductForm','loadProjectProductRequests','openProjectProductDialog','closeProjectProductRequestEntry','consumeProjectProductRequestRoute','editRejectedProjectProduct','submitProjectProductRequest','handleProjectProductAction','handleProjectProductRetry']
  const code=names.map(name=>ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name))).filter(Boolean).map(s=>s.getText(ast)).join('\n')
  vm.runInNewContext(ts.transpileModule(code+'\nexports={open:openProjectProductDialog,close:closeProjectProductRequestEntry,read:loadProjectProductRequests,consume:consumeProjectProductRequestRoute,submit:submitProjectProductRequest,action:handleProjectProductAction,retry:handleProjectProductRetry,can:typeof canPerformProjectAction==="function"?canPerformProjectAction:undefined};',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  return{c,calls,permissions,tenant,records,...c.exports}
}

test('actual role-only review needs no update and moves the precise request to completed after one decision',async()=>{
  const h=host({mode:'records',permissions:['dcc:project-code:query','dcc:project-code:review']})
  await h.action(h.c.projectProductRequests.value[0],'review',true)
  assert.equal(h.calls.writes.length,1,h.c.projectProductError.value)
  assert.equal(h.c.projectProductRequests.value[0].id,id);assert.equal(h.c.projectProductRequests.value[0].status,'COMPLETED')
  assert.equal(h.calls.writes[0].data.reason,'本次正式审核原因')
})
test('actual handlers cannot borrow update or cached role to review without query and review permissions or server qualification',async()=>{
  for(const permissions of [['dcc:project-code:query','dcc:project-code:update'],['dcc:project-code:review']]){
    const h=host({mode:'records',permissions});await h.action(h.c.projectProductRequests.value[0],'review',true)
    assert.equal(h.calls.writes.length,0);assert.ok(h.c.projectProductError.value)
  }
  const h=host({mode:'records'});h.c.projectProductRequests.value[0].canReview=false
  await h.action(h.c.projectProductRequests.value[0],'review',true);assert.equal(h.calls.writes.length,0)
})
test('new creation acknowledgement switches to the exact new request and never refreshes the prior pending row',async()=>{
  const h=host();await h.submit()
  assert.equal(h.calls.writes.length,1,h.c.projectProductError.value)
  assert.equal(h.c.projectProductMode.value,'records');assert.equal(h.c.projectProductExactRequestId.value,nextId)
  assert.equal(h.c.projectProductRequests.value[0].id,nextId)
  assert.ok(h.calls.reads.includes('/dcc/project-product-requests/'+nextId))
  await h.submit();assert.equal(h.calls.writes.length,1)
})
test('candidate change or actor and tenant drift while confirming preserves input and writes nothing',async()=>{
  for(const change of [c=>c.userStore.getUser.id='8',c=>c.projectProductMode.value='records']){
    const h=host({confirm:async c=>change(c)});await h.submit();assert.equal(h.calls.writes.length,0);assert.equal(h.c.projectProductForm.remark,'保留备注')
  }
  let reads=0;const h=host({role:()=>({...role,candidates:++reads===1?role.candidates:[role.candidates[1]]})});await h.submit()
  assert.equal(h.calls.writes.length,0);assert.match(h.c.projectProductError.value,/候选.*变化|角色.*变化/)
  const t=host({confirm:async()=>{t.tenant.id='2'}});await t.submit();assert.equal(t.calls.writes.length,0)
})
test('close and reopen during create transport cannot publish old saved success or overwrite the new form',async()=>{
  const delayed=defer(),started=defer(),h=host({write:()=>{started.resolve();return delayed.promise}});const old=h.submit()
  await Promise.race([started.promise,old.then(()=>{throw new Error('Submission ended before the actual write began')})])
  assert.equal(h.calls.writes.length,1)
  h.c.projectProductDialogVisible.value=false;h.close();await h.open('create');h.c.projectProductForm.remark='新窗口输入'
  delayed.resolve(nextId);await old
  assert.equal(h.calls.success.length,0);assert.equal(h.c.projectProductForm.remark,'新窗口输入');assert.equal(h.c.projectProductMode.value,'create')
})
test('action failure is local, retains the entered reason and cannot masquerade as a completed request',async()=>{
  const h=host({mode:'records',write:async()=>{throw new Error('实际角色已撤销，审核拒绝')}})
  await assert.doesNotReject(h.action(h.c.projectProductRequests.value[0],'review',true))
  assert.equal(h.calls.success.length,0);assert.match(h.c.projectProductError.value,/实际角色已撤销/)
  assert.equal(h.c.projectProductActionReason.value,'本次正式审核原因');assert.equal(h.c.projectProductRequests.value[0].status,'PENDING_REVIEW')
})
test('notification and project-page routes read the intended record or create role without requiring update or legacy configuration',async()=>{
  const h=host({permissions:['dcc:project-code:query','dcc:project-code:review']})
  h.c.route.query={requestId:id,requestOpen:'records',from:'notification'};await h.consume()
  assert.deepEqual(h.calls.reads,['/dcc/project-product-requests/'+id])
  h.c.route.query={requestOpen:'records',from:'project-code'};await h.consume()
  assert.equal(h.c.projectProductExactRequestId.value,undefined);assert.ok(h.calls.reads.includes('/dcc/project-product-requests/pending'))
  const create=host();create.c.route.query={requestOpen:'create',from:'project-code'};await create.consume()
  assert.ok(create.calls.reads.includes('/dcc/project-product-requests/reviewer-role'));assert.equal(create.calls.reads.some(v=>v.includes('reviewer-config')),false)
})

test('duplicate submit during validation or transport writes once and cancel never writes',async()=>{
  const validation=defer(),started=defer(),response=defer()
  const h=host({write:()=>{started.resolve();return response.promise}})
  h.c.projectProductFormRef.value.validate=()=>validation.promise
  const first=h.submit();await h.submit();validation.resolve(true)
  await Promise.race([started.promise,first.then(()=>{throw new Error('No write started')})])
  await h.submit();assert.equal(h.calls.writes.length,1);response.resolve(nextId);await first
  const canceled=host({confirm:async()=>{throw 'cancel'}});await canceled.submit()
  assert.equal(canceled.calls.writes.length,0);assert.equal(canceled.c.projectProductForm.remark,'保留备注');assert.equal(canceled.c.projectProductLoading.value,false)
})

test('new single-role request cannot reach legacy approval or retry even through direct handlers',async()=>{
  const h=host({mode:'records',permissions:['dcc:project-code:query','dcc:project-code:review','dcc:project-code:update']})
  h.c.userStore.getUser.username='admin'
  await h.action(h.c.projectProductRequests.value[0],'approve',true)
  await h.retry(h.c.projectProductRequests.value[0])
  assert.equal(h.calls.writes.length,0);assert.ok(h.c.projectProductError.value)
})

test('nonreactive tenant change during a pending read closes the old dialog and clears its private rows',async()=>{
  const response=defer(),h=host({mode:'records',read:()=>response.promise})
  const pending=h.read();h.tenant.id='2';response.resolve(row());await pending
  assert.equal(h.c.projectProductDialogVisible.value,false)
  assert.equal(h.c.projectProductRequestsLoading.value,false)
  assert.equal(h.c.projectProductRequests.value.length,0)
})

test('actual project-page buttons navigate to the shared create or records entry and both SFCs compile',async()=>{
  const compiler=require('vue/compiler-sfc'),calls=[]
  const source=fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue','utf8')
  const parsed=compiler.parse(source),script=ts.createSourceFile('project.ts',parsed.descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
  const declaration=script.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text==='openProjectRequestEntry'))
  assert.ok(declaration)
  const context={router:{push:async value=>calls.push(value)}}
  vm.runInNewContext(ts.transpileModule(declaration.getText(script)+'\nglobalThis.openEntry=openProjectRequestEntry',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
  await context.openEntry('create');await context.openEntry('records')
  assert.deepEqual(JSON.parse(JSON.stringify(calls)),[
    {path:'/mdm/product-catalog',query:{requestOpen:'create',from:'project-code'}},
    {path:'/mdm/product-catalog',query:{requestOpen:'records',from:'project-code'}}])
  assert.match(source,/data-testid="dcc-project-request-create-open"[^>]*v-hasPermi="\['dcc:project-code:create'\]"/)
  assert.match(source,/data-testid="dcc-project-request-records-open"[^>]*v-hasPermi="\['dcc:project-code:query'\]"/)
  for(const name of ['ProjectCodeTabPanel','ProductCatalogTabPanel']){
    const file=`src/views/dcc/controlled-file/basic-data/components/${name}.vue`,result=compiler.parse(fs.readFileSync(file,'utf8'),{filename:file})
    assert.deepEqual(result.errors,[])
    const code=compiler.compileScript(result.descriptor,{id:name})
    assert.deepEqual(compiler.compileTemplate({id:name,filename:file,source:result.descriptor.template.content,compilerOptions:{bindingMetadata:code.bindings}}).errors,[])
  }
})
