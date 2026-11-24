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
        if(q.url.endsWith('/create')){
          records.set(nextId,row(nextId,{reviewContract:'DIRECT_CREATE_V1',reviewerRoleId:null,reviewerRoleCode:null,canReview:false,status:'COMPLETED',generatedProjectCodeId:'101',generatedProductCatalogId:'102',relationId:'103'}));return nextId
        }
        if(q.url.endsWith('/resubmit'))return nextId
        const requestId=q.url.split('/').at(-3),result=row(requestId,{status:q.url.endsWith('/reject')?'REJECTED':'COMPLETED',canReview:false,reviewerUserId:actor})
        records.set(requestId,result);return result}}}})
  Object.assign(c,api)
  const names=['projectRequestAccess','projectProductActorScope','canPerformProjectAction','resetProjectProductForm','loadProjectProductRequests','openProjectProductDialog','closeProjectProductRequestEntry','consumeProjectProductRequestRoute','editRejectedProjectProduct','submitProjectProductRequest','handleProjectProductAction','handleProjectProductRetry']
  const code=names.map(name=>ast.statements.find(s=>(ts.isFunctionDeclaration(s)&&s.name.text===name)||(ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.text===name)))).filter(Boolean).map(s=>s.getText(ast)).join('\n')
  vm.runInNewContext(ts.transpileModule(code+'\nexports={open:openProjectProductDialog,close:closeProjectProductRequestEntry,read:loadProjectProductRequests,consume:consumeProjectProductRequestRoute,submit:submitProjectProductRequest,retry:handleProjectProductRetry,can:typeof canPerformProjectAction==="function"?canPerformProjectAction:undefined};',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
  return{c,calls,permissions,tenant,records,...c.exports}
}


test('created project replaces create URL and refresh reads precise record',async()=>{
 const h=host();h.c.route.query={requestOpen:'create',from:'project-code'}
 h.c.router.replace=async target=>{h.c.route.path=target.path;h.c.route.query=target.query;await h.consume()}
 await h.submit()
 assert.equal(h.c.route.query.requestId,nextId)
 assert.equal(h.c.route.query.requestOpen,'records')
 assert.equal(h.c.route.query.from,'project-code')
 assert.equal(h.calls.success.length,1,h.c.projectProductError.value)
 h.close();await h.consume()
 assert.equal(h.c.projectProductRequests.value[0].id,nextId)
 await h.submit();assert.equal(h.calls.writes.length,1)
})


test('navigation aborted or rejected reports saved failure without repeating write',async()=>{
 for(const reject of [false,true]){
 const h=host();h.c.router.replace=async()=>{if(reject)throw new Error('guard error');return new Error('aborted')}
 await h.submit();assert.match(h.c.projectProductError.value,/已保存.*定位切换失败.*勿重复创建/)
 assert.equal(h.calls.success.length,0);await h.submit();assert.equal(h.calls.writes.length,1)
 }
})
test('navigation to another record cannot read or announce old created object',async()=>{
 const h=host();h.c.router.replace=async()=>{h.c.route.query={requestOpen:'records',requestId:id,from:'project-code'}}
 await h.submit();assert.equal(h.calls.reads.length,0);assert.equal(h.calls.success.length,0)
})
test('account or tenant changes during confirmation prevents writes and navigation',async()=>{
 for(const kind of ['account','tenant']){
 const h=host({confirm:async c=>{if(kind==='account')c.userStore.getUser.id='other';else h.tenant.id='other'}})
 h.c.router.replace=async()=>{throw new Error('unexpected navigation')}
 await h.submit();assert.equal(h.calls.writes.length,0);assert.equal(h.calls.success.length,0)
 }
})
test('write failure does not replace route; read failure keeps saved precise record',async()=>{
 const failed=host({write:async()=>{throw new Error('write failed')}});let replaced=false
 failed.c.router.replace=async()=>{replaced=true};await failed.submit()
 assert.equal(replaced,false);assert.match(failed.c.projectProductError.value,/write failed/)
 const h=host({read:async()=>{throw new Error('read failed')}})
 h.c.router.replace=async target=>{h.c.route.query=target.query;await h.consume()}
 await h.submit();assert.equal(h.c.route.query.requestId,nextId);assert.match(h.c.projectProductError.value,/读取创建记录失败/)
 await h.submit();assert.equal(h.calls.writes.length,1)
})

test('real Vue Router beforeEach false is reported as saved navigation failure',async()=>{
 const {createRouter,createMemoryHistory}=require('vue-router')
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/mdm/product-catalog',component:{}}]})
 await router.push('/mdm/product-catalog?requestOpen=create&from=project-code')
 router.beforeEach(()=>false)
 const h=host();h.c.route=router.currentRoute.value;h.c.router=router
 await h.submit()
 assert.equal(router.currentRoute.value.query.requestOpen,'create')
 assert.match(h.c.projectProductError.value,/已保存.*定位切换失败/)
 assert.equal(h.calls.success.length,0)
 await h.submit();assert.equal(h.calls.writes.length,1)
})
