const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeApi = read('src/api/mes/pro/route/index.ts')
const routeFlowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
const routeProductApi = read('src/api/mes/pro/route/product/index.ts')
const routeIndex = read('src/views/mes/pro/route/index.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const flowDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const productList = read('src/views/mes/pro/route/RouteProductList.vue')
const routeCandidateEntry = read('src/views/mes/pro/route/routeCandidateEntry.ts')

assert.match(
  routeApi,
  /export interface RouteVersionEditContext[\s\S]*routeVersionId:\s*MesRouteId[\s\S]*versionNo:\s*string[\s\S]*lifecycleStatus:\s*ProRouteVersionLifecycleStatus/,
  '前端必须提供统一 RouteVersionEditContext，显式承载候选 routeVersionId、versionNo 和生命周期。'
)
assert.match(
  routeApi,
  /export interface RouteFlowGraphSaveReqVO[\s\S]*routeVersionId\?:\s*MesRouteId/,
  '流转关系图保存契约必须支持 routeVersionId，用于写候选版本快照。'
)
assert.match(
  routeProductApi,
  /export interface ProRouteProductVO[\s\S]*routeVersionId\?:\s*MesRouteId/,
  '产品绑定保存契约必须支持 routeVersionId，用于写候选版本快照。'
)
assert.match(
  routeFlowConfigApi,
  /getProcessConfigList:\s*async\s*\(\s*routeId:\s*number,\s*useType:\s*ProRouteFlowConfigType,\s*routeVersionId\?:\s*MesRouteId/,
  '流转/批记录配置读取契约必须支持候选 routeVersionId，用于编辑已有草稿时读取候选快照。'
)
assert.match(
  routeFlowConfigApi,
  /params:\s*\{\s*routeId,\s*useType,\s*routeVersionId\s*\}/,
  '流转/批记录配置读取请求必须把候选 routeVersionId 传给后端。'
)

assert.match(
  routeIndex,
  /openRouteCandidateVersionEditor\(version\)/,
  '版本工作区候选版本行必须提供进入候选编辑上下文的入口。'
)
assert.match(
  routeIndex,
  /routeVersionId:\s*String\(version\.id\)/,
  '候选编辑入口必须把候选 routeVersionId 写入路由 query。'
)
assert.match(
  routeIndex,
  /routeVersionStatus:\s*version\.lifecycleStatus/,
  '候选编辑入口必须把候选 lifecycleStatus 写入路由 query。'
)
assert.doesNotMatch(
  routeIndex,
  /@click="openEditPage\(scope\.row\.id\)"/,
  '列表操作列“编辑”不能再只打开 active 编辑页，必须直接进入或创建候选版本。'
)
assert.match(
  routeIndex,
  /@click="handleEditRouteProductionConfig\(scope\.row\)"/,
  '列表操作列“编辑”必须调用候选版本编辑入口。'
)
assert.match(
  routeIndex,
  /ensureSameSourceDraftCandidateForProductionConfig/,
  '列表操作列“编辑”必须复用统一候选版本 helper。'
)
assert.match(
  routeIndex,
  /buildRouteCandidateEditQuery\(candidate/,
  '列表操作列“编辑”进入候选版本时必须复用候选编辑路由参数构造函数。'
)
assert.match(
  routeCandidateEntry,
  /OPEN_ROUTE_VERSION_STATUSES[\s\S]*DRAFT_ROUTE_VERSION_STATUS[\s\S]*PENDING_APPROVAL_ROUTE_VERSION_STATUS[\s\S]*READY_TO_PUBLISH_ROUTE_VERSION_STATUS/,
  '统一候选版本 helper 必须把 DRAFT / PENDING_APPROVAL / READY_TO_PUBLISH 作为未完成候选统一治理。'
)
assert.match(
  routeCandidateEntry,
  /draftCandidates\.length > 1/,
  '统一候选版本 helper 必须继续对多个 DRAFT 显式阻塞。'
)
assert.match(
  routeCandidateEntry,
  /blockingCandidate[\s\S]*PENDING_APPROVAL_ROUTE_VERSION_STATUS[\s\S]*READY_TO_PUBLISH_ROUTE_VERSION_STATUS[\s\S]*请先撤回、发布恢复或取消后再编辑/,
  '统一候选版本 helper 必须阻断审核中或待发布候选下的新建替代草稿。'
)
assert.match(
  routeCandidateEntry,
  /export class RouteMultipleDraftCandidatesError extends Error/,
  '统一候选版本 helper 多草稿分支必须抛出可识别错误，调用方才能打开候选版本选择工作区。'
)
assert.match(
  routeCandidateEntry,
  /export const isRouteMultipleDraftCandidateError/,
  '统一候选版本 helper 必须导出多草稿错误识别函数。'
)
assert.match(
  routeIndex,
  /isRouteMultipleDraftCandidateError\(error\)[\s\S]*openRouteVersionWorkspace\(row,\s*OPEN_CANDIDATE_CONFLICT_NOTICE\)/,
  '列表“编辑”遇到打开候选冲突时必须打开候选版本工作区，而不是只显示错误提示。'
)
assert.match(
  routeIndex,
  /OPEN_CANDIDATE_CONFLICT_NOTICE[\s\S]*多个打开中的候选版本[\s\S]*处理打开候选/,
  '打开候选冲突提示必须明确引导用户处理多个打开候选。'
)
assert.match(
  routeIndex,
  /routeVersionNoticeMessage[\s\S]*type="warning"/,
  '候选版本工作区必须用 warning 提示冲突候选状态。'
)
assert.doesNotMatch(
  routeIndex,
  /requireSingleDraftRouteVersionForProductionConfig/,
  '工艺路线列表不得保留只要求已有唯一草稿的旧生产配置入口。'
)

assert.match(
  editPage,
  /const routeVersionEditContext = computed<RouteVersionEditContext \| undefined>/,
  'RouteEditPage 必须从路由 query 解析 RouteVersionEditContext。'
)
assert.match(
  editPage,
  /:route-version-edit-context="routeVersionEditContext"/,
  'RouteEditPage 必须把候选版本上下文传给 RouteFormContent。'
)
assert.match(
  editPage,
  /ensureSameSourceDraftCandidateForProductionConfig/,
  '生效版本“编辑生产配置”入口必须复用统一候选版本 helper。'
)
assert.match(
  editPage,
  /buildRouteCandidateEditQuery\(candidate/,
  '生效版本进入候选版本时必须复用候选编辑路由参数构造函数。'
)
assert.match(
  editPage,
  /handleEditProductionConfig/,
  'RouteEditPage 必须提供生效版本进入候选编辑的处理函数。'
)
assert.match(
  editPage,
  /handleSubmitRouteCandidateVersion/,
  'RouteEditPage 必须提供候选版本统一提交发布入口。'
)
assert.doesNotMatch(
  editPage,
  /promptRouteVersionSignaturePassword|电子签名发布|signaturePassword/,
  '候选版本提交入口不得要求提交者电子签名。'
)
assert.match(
  editPage,
  /ProRouteApi\.getRouteVersion\(context\.routeVersionId\)[\s\S]*latestVersion\.lifecycleStatus !== 'DRAFT'[\s\S]*return[\s\S]*ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{[\s\S]*id:\s*latestVersion\.id[\s\S]*\}\)/,
  '候选版本提交入口必须先重读最新版本状态，并且只传最新候选版本 ID。'
)

assert.match(
  formContent,
  /routeVersionEditContext\?:\s*RouteVersionEditContext/,
  'RouteFormContent 必须声明 routeVersionEditContext 入参。'
)
assert.match(
  formContent,
  /'request-candidate-edit'[\s\S]*'request-route-version-submit'/,
  'RouteFormContent 必须保留进入候选编辑和提交候选版本的事件入口。'
)
assert.match(
  formContent,
  /assertRouteCandidateVersionWritable[\s\S]*当前候选版本已离开草稿状态，仅允许查看/,
  'RouteFormContent 必须阻止非草稿候选继续写入生产配置。'
)
assert.match(
  formContent,
  /const productionConfigFormType = computed\([\s\S]*'detail'[\s\S]*formType\.value/,
  '生效版本和非 DRAFT 候选版本必须把生产配置子组件置为只读 detail，DRAFT 才可连续编辑。'
)
for (const child of ['RouteFlowGraphDesigner', 'RouteProductList']) {
  assert.match(
    formContent,
    new RegExp(`<${child}[\\s\\S]*:route-version-edit-context="routeVersionEditContext"`),
    `RouteFormContent 必须把候选版本上下文传给 ${child}。`
  )
  assert.match(
    formContent,
    new RegExp(`<${child}[\\s\\S]*:form-type="productionConfigFormType"`),
    `RouteFormContent 必须按版本状态把 ${child} 切换为只读或 DRAFT 可编辑。`
  )
}

assert.match(
  flowDesigner,
  /requireCandidateRouteVersionId\('工序属性保存'\)/,
  'RouteFlowGraphDesigner 内置工序设置保存生产配置时必须要求候选 routeVersionId。'
)

assert.match(
  flowDesigner,
  /routeVersionEditContext\?:\s*RouteVersionEditContext/,
  'RouteFlowGraphDesigner 必须接收候选版本上下文。'
)
assert.match(
  flowDesigner,
  /routeVersionId:\s*resolveRouteVersionIdForSave\(\)/,
  'RouteFlowGraphDesigner 保存流转图时必须显式提交候选 routeVersionId。'
)
assert.match(
  flowDesigner,
  /请先创建候选版本/,
  'RouteFlowGraphDesigner active 编辑保存生产配置时必须提示先创建候选版本。'
)

assert.match(
  productList,
  /routeVersionEditContext\?:\s*RouteVersionEditContext/,
  'RouteProductList 必须接收候选版本上下文。'
)
assert.match(
  productList,
  /buildRouteProductSavePayload\([\s\S]*requireCandidateRouteVersionId\('产品绑定保存'\)/,
  'RouteProductList 产品绑定保存必须携带候选 routeVersionId。'
)

console.log('mes-pro-route-version-candidate-edit-static PASS')
