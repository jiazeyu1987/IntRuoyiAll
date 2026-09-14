const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const uiRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(uiRoot, '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeProductList = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteProductList.vue')
const routeProductBomList = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteProductBomList.vue')
const routeFlowGraphDesigner = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeFormContent = read('yudao-ui-admin-vue3/src/views/mes/pro/route/RouteFormContent.vue')
const routeApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/index.ts')
const routeResourceApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/resource.ts')
const routeProductApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/product/index.ts')
const routeProductBomApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/productbom/index.ts')
const flowConfigApi = read('yudao-ui-admin-vue3/src/api/mes/pro/route/flowconfig.ts')
const productSaveReq = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/product/MesProRouteProductSaveReqVO.java')
const productCopyReq = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/product/MesProRouteProductCopyReqVO.java')
const productBomSaveReq = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/vo/productbom/MesProRouteProductBomSaveReqVO.java')
const productService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductServiceImpl.java')
const productBomService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductBomServiceImpl.java')
const flowService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProcessFlowServiceImpl.java')
const routeFlowConfigService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigServiceImpl.java')
const routeFlowConfigServiceApi = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteFlowConfigService.java')
const routeFlowConfigController = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteFlowConfigController.java')
const routeResourceController = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteResourceController.java')
const routeResourceServiceApi = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteResourceService.java')
const routeResourceService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteResourceServiceImpl.java')
const routeScheduleConfigService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteScheduleConfigServiceImpl.java')
const routeService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteServiceImpl.java')
const publishProjectionService = read('ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java')

assert.match(
  routeFlowGraphDesigner,
  /const routeFlowWriteControlsDisabled = computed\([\s\S]*!isEditable\.value[\s\S]*isFrozenRouteVersionView\.value[\s\S]*\)/,
  '流转图生产配置写控件必须在非 DRAFT 候选上下文禁用。'
)
assert.match(
  routeFlowGraphDesigner,
  /const processDetailInterestMutationDisabled = computed\([\s\S]*routeFlowWriteControlsDisabled\.value/,
  '组成工序字段配置入口必须复用流转图 DRAFT 候选禁用条件。'
)
assert.match(
  routeFlowGraphDesigner,
  /data-flow-action="add-route-process"[\s\S]*:disabled="routeFlowWriteControlsDisabled/,
  '新增工序按钮必须复用 DRAFT 候选写控件禁用条件。'
)
assert.match(
  routeFlowGraphDesigner,
  /data-flow-action="submit-add-route-process"[\s\S]*:disabled="routeFlowWriteControlsDisabled/,
  '添加工序弹框确认按钮必须复用 DRAFT 候选写控件禁用条件。'
)
assert.match(
  routeFlowGraphDesigner,
  /data-flow-action="delete-route-process"[\s\S]*:disabled="routeFlowWriteControlsDisabled/,
  '删除工序按钮必须复用 DRAFT 候选写控件禁用条件。'
)
assert.match(
  routeFlowGraphDesigner,
  /const handleAddProcessDetailField = async \(\) => \{[\s\S]*if \(processDetailInterestMutationDisabled\.value\) return[\s\S]*const handleRemoveProcessDetailField = async \(fieldKey: ProcessDetailFieldKey\) => \{[\s\S]*if \(processDetailInterestMutationDisabled\.value\) return/,
  '组成工序字段添加和移除处理函数必须在禁用条件下 fail-fast。'
)
assert.match(
  routeApi,
  /routeProcessUpdates\?:\s*RouteFlowRouteProcessUpdateReqVO\[\]/,
  '流转图保存请求必须承载已有工序 keyFlag 等更新，避免候选页直写 route-process/update。'
)
assert.match(
  routeApi,
  /export interface RouteFlowRouteProcessUpdateReqVO \{[\s\S]*workstationId\?:\s*number/,
  '候选版本流转图更新合同必须携带工作站编号。'
)
assert.match(
  routeFlowGraphDesigner,
  /routeProcessUpdates:\s*buildRouteProcessUpdatePayload\(\)/,
  '流转图 payload 必须把已有工序关键工序改动交给带 routeVersionId 的保存接口。'
)
assert.match(
  routeFlowGraphDesigner,
  /const buildRouteProcessUpdatePayload = \(\): RouteFlowRouteProcessUpdateReqVO\[\] => \{[\s\S]*workstationId:\s*node\.routeProcessWorkstationId/,
  '候选版本工作站绑定必须进入流转图候选快照更新 payload。'
)
assert.match(
  routeApi,
  /export interface RouteFlowRouteProcessUpdateReqVO \{[\s\S]*processId:\s*number[\s\S]*sort:\s*number[\s\S]*workstationId\?:\s*number/,
  '候选版本流转图更新合同必须携带后端校验要求的 processId 和 sort。'
)
assert.match(
  routeFlowGraphDesigner,
  /const buildRouteProcessUpdatePayload = \(\): RouteFlowRouteProcessUpdateReqVO\[\] => \{[\s\S]*processId:\s*node\.processId[\s\S]*sort:\s*node\.sort \|\| 0[\s\S]*workstationId:\s*node\.routeProcessWorkstationId/,
  '候选版本工作站绑定 payload 必须携带 processId 和 sort，避免后端参数校验阻断保存。'
)
assert.doesNotMatch(
  routeFlowGraphDesigner,
  /ProRouteProcessApi\.updateRouteProcess/,
  '候选版本流转图不得直接调用 route-process/update 保存关键工序。'
)
assert.match(
  routeFormContent,
  /const shouldPersistRouteHeaderOnSubmit = \(\) =>[\s\S]*!routeVersionEditContext\.value/,
  '候选版本生产配置保存不得触发路线主表 updateRoute，避免额外创建草稿候选版本。'
)
assert.match(
  routeFormContent,
  /else if \(shouldPersistRouteHeaderOnSubmit\(\)\) \{[\s\S]*await ProRouteApi\.updateRoute\(data\)/,
  '路线主表 updateRoute 必须被候选版本上下文门禁包住。'
)
assert.doesNotMatch(
  routeFormContent,
  /else\s*\{\s*await ProRouteApi\.updateRoute\(data\)\s*\}/,
  'submitForm 不得保留候选版本也会执行的无条件 updateRoute 分支。'
)
assert.match(
  routeFormContent,
  /const assertRouteCandidateVersionWritable = \(\) => \{[\s\S]*routeVersionEditContext\.value[\s\S]*!isDraftCandidateVersion\.value[\s\S]*当前候选版本已离开草稿状态，仅允许查看[\s\S]*throw new Error[\s\S]*\}[\s\S]*const submitForm = async \(\) => \{[\s\S]*assertRouteCandidateVersionWritable\(\)[\s\S]*await formRef\.value\.validate\(\)/,
  'RouteFormContent 父级保存必须在表单校验和成功提示前阻断 PENDING_APPROVAL / READY_TO_PUBLISH 候选，避免只读候选“空保存成功”。'
)

assert.match(
  routeProductList,
  /const productionConfigActionDisabled = computed\([\s\S]*!isDraftCandidateEdit\.value[\s\S]*\)/,
  '关联产品写入口必须统一声明 DRAFT 候选禁用条件。'
)
for (const token of [
  "requireCandidateRouteVersionId('产品绑定打开')",
  "requireCandidateRouteVersionId('产品复制打开')",
  "requireCandidateRouteVersionId('产品删除')"
]) {
  assert.match(routeProductList, new RegExp(token.replace(/[()']/g, '\\$&')), `关联产品入口必须校验 ${token}。`)
}
assert.match(
  routeProductList,
  /copyFormData\.value = \{[\s\S]*routeVersionId:\s*requireCandidateRouteVersionId\('产品复制打开'\)/,
  '关联产品复制表单必须携带候选 routeVersionId。'
)
assert.match(
  routeProductList,
  /<RouteProductBomList[\s\S]*:route-version-edit-context="routeVersionEditContext"/,
  '产品 BOM 子组件必须继承候选版本上下文。'
)

assert.match(routeProductBomList, /routeVersionEditContext\?:\s*RouteVersionEditContext/, '产品 BOM 组件必须接收候选版本上下文。')
assert.match(routeProductBomList, /requireCandidateRouteVersionId\('BOM 物料打开'\)/, '产品 BOM 打开弹框前必须校验候选上下文。')
assert.match(routeProductBomList, /routeVersionId:\s*requireCandidateRouteVersionId\('BOM 物料保存'\)/, '产品 BOM 保存必须携带候选 routeVersionId。')
assert.match(routeProductBomList, /deleteRouteProductBom\(id,\s*requireCandidateRouteVersionId\('BOM 物料删除'\)\)/, '产品 BOM 删除必须携带候选 routeVersionId。')

assert.match(routeProductApi, /routeVersionId\?:\s*MesRouteId[\s\S]*sourceRouteProductId/, '前端产品复制请求必须包含 routeVersionId。')
assert.match(routeProductApi, /deleteRouteProduct:\s*async\s*\(id:\s*number,\s*routeVersionId:\s*MesRouteId\)/, '前端产品删除 API 必须要求 routeVersionId。')
assert.match(routeProductBomApi, /routeVersionId\?:\s*number/, '前端产品 BOM VO 必须包含 routeVersionId。')
assert.match(routeProductBomApi, /deleteRouteProductBom:\s*async\s*\(id:\s*number,\s*routeVersionId:\s*number\)/, '前端产品 BOM 删除 API 必须要求 routeVersionId。')

for (const source of [productSaveReq, productCopyReq, productBomSaveReq]) {
  assert.match(source, /private\s+Long\s+routeVersionId;/, '后端产品/BOM 写请求必须承载 routeVersionId。')
}

assert.match(productService, /private MesProRouteVersionDO requireDraftCandidateVersion\(Long routeVersionId, Long routeId\)/, '产品服务必须使用 fail-fast 的候选版本解析。')
assert.match(productService, /throw exception\(PRO_ROUTE_VERSION_NOT_EXISTS, routeVersionId\)/, '产品服务必须拒绝不存在的 routeVersionId。')
assert.match(productService, /throw exception\(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE/, '产品服务必须拒绝非 DRAFT 或 routeId 不匹配的候选版本。')
assert.match(productService, /copyRouteProduct[\s\S]*saveConfigSnapshot\(candidateVersion\.getId\(\), "products"/, '产品复制必须写候选 products 快照。')
assert.match(productService, /copyRouteProduct[\s\S]*saveConfigSnapshot\(candidateVersion\.getId\(\), "productBoms"/, '产品复制必须同步写候选 productBoms 快照。')
assert.match(productService, /deleteRouteProduct\(Long id, Long routeVersionId\)/, '产品删除必须接收候选 routeVersionId。')
assert.match(productService, /deleteRouteProduct[\s\S]*saveConfigSnapshot\(candidateVersion\.getId\(\), "productBoms"/, '产品删除必须同步移除候选 productBoms 快照。')

assert.match(productBomService, /private MesProRouteVersionDO requireDraftCandidateVersion\(Long routeVersionId, Long routeId\)/, '产品 BOM 服务必须使用 fail-fast 的候选版本解析。')
assert.match(productBomService, /saveConfigSnapshot\(candidateVersion\.getId\(\), "productBoms"/, '产品 BOM 保存/删除必须写候选 productBoms 快照。')
assert.match(flowService, /if \(routeVersionId == null\) \{[\s\S]*return null;[\s\S]*\}[\s\S]*throw exception\(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE/, '流转图保存携带非 DRAFT routeVersionId 时必须 fail-fast，不得回落 active。')
assert.match(routeFlowConfigService, /private MesProRouteVersionDO requireDraftCandidateVersion\(Long routeVersionId, Long routeId\)/, '用途配置保存必须使用 fail-fast 的候选版本解析。')
assert.match(routeFlowConfigService, /saveRouteFlowConfigInternal[\s\S]*requireDraftCandidateVersion\(saveReqVO\.getRouteVersionId\(\), route\.getId\(\)\)[\s\S]*saveConfigSnapshot/, '用途配置保存必须只写 DRAFT 候选快照。')
assert.doesNotMatch(routeFlowConfigService, /saveRouteFlowConfigInternal[\s\S]*validateActiveRouteVersion\(route\.getId\(\), saveReqVO\.getRouteVersionId\(\)\)[\s\S]*routeFlowConfigMapper/, '用途配置保存不得在非 DRAFT routeVersionId 下回落 active 写入。')
assert.doesNotMatch(routeFlowConfigServiceApi, /updateRouteFlowConfigEnabled/, '用途配置服务接口不得保留无候选版本上下文的 enabled 直写入口。')
assert.doesNotMatch(routeFlowConfigService, /updateRouteFlowConfigEnabled/, '用途配置服务实现不得保留无候选版本上下文的 enabled active 写入口。')
assert.doesNotMatch(routeFlowConfigController, /flow-config\/enabled|updateRouteFlowConfigEnabled/, '用途配置控制器不得暴露无候选版本上下文的 enabled active 写接口。')
assert.doesNotMatch(flowConfigApi, /updateEnabled|flow-config\/enabled/, '前端用途配置 API 不得保留无候选版本上下文的 enabled active 写接口。')
assert.doesNotMatch(routeResourceApi, /saveResource|route-resource\/save|ProRouteResourceSaveVO/, '前端路线资源 API 不得保留无候选版本上下文的资源保存 wrapper。')
assert.doesNotMatch(routeResourceController, /PutMapping\("\/save"\)|saveResource|MesProRouteResourceSaveReqVO/, '路线资源 Controller 不得暴露无候选版本上下文的资源保存端点。')
assert.doesNotMatch(routeResourceServiceApi, /saveResource|MesProRouteResourceSaveReqVO/, '路线资源 Service 接口不得保留无候选版本上下文的资源保存方法。')
assert.doesNotMatch(routeResourceService, /saveResource|MesProRouteResourceSaveReqVO/, '路线资源 Service 实现不得保留无候选版本上下文的资源保存方法。')
assert.match(routeScheduleConfigService, /private MesProRouteVersionDO requireDraftCandidateVersion\(Long routeVersionId\)/, '排产配置保存必须使用 fail-fast 的候选版本解析。')
assert.match(routeScheduleConfigService, /saveConfig\(MesProRouteScheduleConfigSaveReqVO reqVO\)[\s\S]*requireDraftCandidateVersion\(reqVO\.getRouteVersionId\(\)\)[\s\S]*saveConfigSnapshot/, '排产配置保存必须只写 DRAFT 候选快照。')
assert.doesNotMatch(routeScheduleConfigService, /saveConfig\(MesProRouteScheduleConfigSaveReqVO reqVO\)[\s\S]*validateActiveRouteVersion\(routeVersion, reqVO\.getRouteVersionId\(\)\)[\s\S]*routeScheduleConfigMapper\.updateById/, '排产配置保存不得在非 DRAFT routeVersionId 下回落 active 写入。')
assert.match(routeService, /private static final String PRODUCT_BOMS_KEY = "productBoms"/, '路线完整快照必须包含 productBoms key。')
assert.match(routeService, /configSnapshots\.put\(PRODUCT_BOMS_KEY,[\s\S]*routeProductBomMapper\.selectList\(routeId, null, null\)/, '创建候选版本时必须带上当前产品 BOM 快照。')
assert.match(publishProjectionService, /private static final String PRODUCT_BOMS_KEY = "productBoms"/, '候选发布投影必须识别 productBoms key。')
assert.match(publishProjectionService, /projectProductBoms\(routeId, configSnapshots\.get\(PRODUCT_BOMS_KEY\)\)/, '候选发布必须投影产品 BOM 快照。')
assert.match(publishProjectionService, /routeProductBomMapper\.deleteByRouteId\(routeId\)[\s\S]*insertProductBomSnapshot/, '候选发布投影产品 BOM 时必须先清理旧 BOM 再插入快照。')

console.log('PASS: MES production config candidate gate static contract')
