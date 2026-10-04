// Actual ProductCatalog SFC setup and API wrapper; all external ports are explicit offline test boundaries.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm')
const {parse,compileScript}=require('vue/compiler-sfc'),ts=require('typescript'),vue=require('vue')
const root=path.resolve(__dirname,'../..')
const compile=source=>ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS}}).outputText
const calls=[],messages=[],prompts=[]
let supportReads=0
const apiPath='@/api/dcc/controlledFile/projectProductRequests'
const APIs={
 createDccProjectProductRequest:async data=>{calls.push(['create',data]);return 1},
 resubmitDccProjectProductRequest:async (id,data)=>{calls.push(['resubmit',id,data]);return 2},
 retryDccProjectProductRequestWrite:async (id,data)=>{calls.push(['retry',id,data]);return {id}},
 getDccProjectProductRequests:async()=>[],reviewDccProjectProductRequest:async (...args)=>{calls.push(['review',...args])},approveDccProjectProductRequest:async()=>{},
 getDccProjectReviewerConfiguration:async()=>({configured:true,reviewerUserId:'9',reviewerUsername:'reviewer',reviewerNickname:'审核人',enabled:true})
}
function state(){const e={};vm.runInNewContext(compile(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/project-attributes/state.ts'),'utf8')),{exports:e,Error});return e}
function reviewerModel(){const e={};vm.runInNewContext(compile(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/basic-data/components/project-reviewer.ts'),'utf8')),{exports:e,Error});return e}
function confirmationModel(){const e={};vm.runInNewContext(compile(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/basic-data/components/project-product-confirmation.ts'),'utf8')),{exports:e,Error,require:id=>{
 if(id==='vue')return vue
 if(id==='element-plus')return {ElMessageBox:{confirm:async()=>{}}}
 if(id==='./project-reviewer')return reviewerModel()
 if(id==='../../project-attributes/state')return state()
 throw Error('Unconfigured confirmation dependency: '+id)
}});return e}
const filename=path.join(root,'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue')
const script=compileScript(parse(fs.readFileSync(filename,'utf8'),{filename}).descriptor,{id:'rev01-product'})
const exportsObject={}
vm.runInNewContext(compile(script.content),{
 exports:exportsObject,Error,console,
 ...vue,onMounted:()=>{},onBeforeUnmount:()=>{},useMessage:()=>({success:text=>messages.push(['success',text]),warning:text=>messages.push(['warning',text])}),
 useRouter:()=>({push:()=>{}}),useRoute:()=>vue.reactive({path:'/mdm/product-catalog',query:{}}),window:{prompt:()=>prompts.shift()},
 require:id=>{
  if(id==='vue')return vue
  if(id===apiPath)return APIs
  if(id.endsWith('.vue'))return {}
  if(id==='../../project-attributes/state')return state()
  if(id==='./project-reviewer')return reviewerModel()
  if(id==='./project-product-confirmation')return confirmationModel()
  if(id==='./project-product-resubmit')return {canResubmitProjectProductRequest:()=>false,restoreRejectedProjectProductForm:()=>{throw Error('not used')}}
  if(id==='@/store/modules/user')return {useUserStore:()=>({getUser:{id:9}})}
  if(id==='@vueuse/core')return {useClipboard:()=>({})}
  if(id==='@/hooks/web/useUserTableColumns')return {useUserTableColumns:()=>({columns:vue.ref([])})}
  if(id==='@/hooks/web/useTableQuickFilter')return {useTableQuickFilter:()=>({})}
  if(id==='@/api/dcc/controlledFile/productCatalog')return {getProductCatalogPage:async()=>({list:[],total:0})}
  if(id==='@/api/dcc/controlledFile/projectAttributes')return {getFolderTemplates:async()=>{supportReads++;return []}}
  if(id==='@/api/system/user')return {getSimpleUserList:async()=>{supportReads++;return []}}
  if(id==='@/api/dcc/controlledFile/projectCodes')return {getProjectCodePage:async()=>({list:[],total:0})}
  if(id==='@/api/dcc/dataRelations')return {createDccDataRelation:async()=>{}}
  if(id==='@/api/dcc/registrationCertificate')return {getRegistrationCertificatePage:async()=>({list:[],total:0})}
  throw Error('Unconfigured test dependency: '+id)
 }
})
const scope=vue.effectScope(),bindings=scope.run(()=>exportsObject.default.setup({}, {expose:()=>{},emit:()=>{}}))
async function run(){
 await bindings.openProjectProductDialog('records');assert.equal(bindings.projectProductMode.value,'records');assert.equal(supportReads,0,'审核记录入口不依赖创建所需模板/人员列表');assert.equal(bindings.projectProductError.value,'')
 bindings.projectProductMode.value='create'
 bindings.projectLeaderUsers.value=[{id:7,nickname:'正式负责人',username:'leader'}]
 bindings.folderTemplates.value=[{id:1,name:'正式模板',active:true,structureJson:'{"nodes":[]}'}]
 bindings.projectProductFormRef.value={validate:async()=>true,resetFields:()=>{}}
 Object.assign(bindings.projectProductForm,{projectName:'P',projectCode:'P',projectLeaderUserId:7,folderTemplateId:1,productCode:'PR',productName:'Product',classification:'一类',
  defaultAttributes:{targetMarkets:['CE'],licenseHolder:'Y',actualManufacturer:'N',documentTransfer:'N'},remark:'备注不能作为原因',resubmissionReason:'重提原因不能作为新建原因',creationReason:''})
 await bindings.submitProjectProductRequest()
 assert.equal(calls.length,0,'初始新建不能借备注/重提原因或省略新建原因提交')
 bindings.projectProductForm.creationReason='用户明确新建原因'
 const configured=APIs.getDccProjectReviewerConfiguration
 APIs.getDccProjectReviewerConfiguration=async()=>({configured:false,reviewerUserId:null,reviewerUsername:null,reviewerNickname:null,enabled:false})
 await bindings.submitProjectProductRequest();assert.equal(calls.length,0,'缺审核配置不能提交创建');assert.match(bindings.projectProductError.value,/配置/)
 APIs.getDccProjectReviewerConfiguration=configured
 let releaseConfiguration
 APIs.getDccProjectReviewerConfiguration=()=>new Promise(resolve=>{releaseConfiguration=resolve})
 const pendingSubmit=bindings.submitProjectProductRequest()
 for(let turn=0;turn<4;turn++)await Promise.resolve()
 bindings.projectProductResubmitRequestId.value='9223372036854775700'
 releaseConfiguration(await configured());await pendingSubmit
 assert.equal(calls.length,0,'读取审核配置期间切换申请不得把旧载荷提交给另一个申请')
 assert.match(bindings.projectProductError.value,/变化/)
 bindings.projectProductResubmitRequestId.value=undefined
 APIs.getDccProjectReviewerConfiguration=configured
 await bindings.submitProjectProductRequest()
 assert.equal(calls[0][0],'create');assert.equal(calls[0][1].creationReason,'用户明确新建原因')
 assert.equal(bindings.projectProductForm.creationReason,'','成功后新建原因不能带入下次申请')
 prompts.push('');await bindings.handleProjectProductRetry({id:5})
 assert.equal(calls.filter(call=>call[0]==='retry').length,0,'空重试原因不能提交')
 prompts.push(' 修复后本次明确重试 ');await bindings.handleProjectProductRetry({id:5})
 assert.deepEqual(JSON.parse(JSON.stringify(calls.find(call=>call[0]==='retry'))),['retry',5,{reason:'修复后本次明确重试'}])
 const beforeReview=calls.length
 await bindings.handleProjectProductAction({id:'9223372036854775700',status:'PENDING_REVIEW',configuredReviewerUserId:'7'},'review',true)
 assert.equal(calls.length,beforeReview,'未冻结为审核人的当前用户不能触发审核')
 prompts.push('本次审核意见')
 await bindings.handleProjectProductAction({id:'9223372036854775700',status:'PENDING_REVIEW',configuredReviewerUserId:'9'},'review',true)
 assert.equal(calls.at(-1)[0],'review');assert.equal(calls.at(-1)[1],'9223372036854775700')
 // Actual wrapper must transmit the new retry body, not drop the collected reason.
 const apiExports={},transport=[]
 vm.runInNewContext(compile(fs.readFileSync(path.join(root,'src/api/dcc/controlledFile/projectProductRequests.ts'),'utf8')),
  {exports:apiExports,require:id=>{if(id.endsWith('/project-reviewer'))return reviewerModel();assert.equal(id,'@/config/axios');return {default:{post:async payload=>{transport.push(payload);return {}}}}}})
 await apiExports.retryDccProjectProductRequestWrite(5,{reason:'本次重试意图'})
 assert.equal(transport[0].url,'/dcc/project-product-requests/5/retry-write');assert.equal(transport[0].data.reason,'本次重试意图')
 scope.stop();console.log('PASS: 实际产品SFC初始原因/重试原因门禁、隔离输入、清空及正式API请求体')
}
run().catch(error=>{scope.stop();console.error(error);process.exitCode=1})
