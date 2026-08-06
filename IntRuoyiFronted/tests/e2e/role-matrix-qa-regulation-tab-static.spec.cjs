const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.resolve(workspaceRoot, 'IntRuoyiFronted')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const workbenchPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const qcTemplateApiPath = path.join(frontendRoot, 'src/api/mes/qc/template/index.ts')
const routeProductApiPath = path.join(frontendRoot, 'src/api/mes/pro/route/product/index.ts')
const backendRouteProductControllerPath = path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteProductController.java'
)
const backendRouteProductServicePath = path.join(
  workspaceRoot,
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteProductServiceImpl.java'
)

assert.ok(fs.existsSync(qaPagePath), 'QA regulation must be implemented as a standalone page.')
assert.ok(fs.existsSync(routeProductApiPath), 'QA route binding must use the formal route-product API wrapper.')
assert.ok(
  fs.existsSync(backendRouteProductControllerPath),
  'QA route binding must have a backend route-product controller contract.'
)
assert.ok(
  fs.existsSync(backendRouteProductServicePath),
  'QA route binding must have a backend route-product service contract.'
)

const qaSource = fs.readFileSync(qaPagePath, 'utf8')
const workbenchSource = fs.readFileSync(workbenchPath, 'utf8')
const routeSource = fs.readFileSync(routePath, 'utf8')
const qcTemplateApiSource = fs.readFileSync(qcTemplateApiPath, 'utf8')
const routeProductApiSource = fs.readFileSync(routeProductApiPath, 'utf8')
const backendRouteProductControllerSource = fs.readFileSync(
  backendRouteProductControllerPath,
  'utf8'
)
const backendRouteProductServiceSource = fs.readFileSync(backendRouteProductServicePath, 'utf8')
const dccProjectLoaderStart = qaSource.indexOf('const loadDccProjectCodeOptions')
const dccProjectLoaderEnd = qaSource.indexOf('const retryLoadDccProjectCodes')
const dccProjectLoaderSource = qaSource.slice(dccProjectLoaderStart, dccProjectLoaderEnd)
const dccSelectorStart = qaSource.search(/<ContentWrap[^>]*data-qa-regulation-dcc-project/)
const dccSelectorEnd =
  dccSelectorStart >= 0 ? qaSource.indexOf('</ContentWrap>', dccSelectorStart) : -1
const dccSelectorSource =
  dccSelectorStart >= 0 && dccSelectorEnd > dccSelectorStart
    ? qaSource.slice(dccSelectorStart, dccSelectorEnd)
    : ''
const firstContentWrapEnd = qaSource.indexOf('</ContentWrap>')
const qaItemsSectionStart = qaSource.indexOf('<ContentWrap v-show="qaActiveTab === \'items\'">')
const qaItemsSectionEnd =
  qaItemsSectionStart >= 0
    ? qaSource.indexOf('<ContentWrap v-show="qaActiveTab === \'verification\'">', qaItemsSectionStart)
    : -1
const qaItemsSectionSource =
  qaItemsSectionStart >= 0 && qaItemsSectionEnd > qaItemsSectionStart
    ? qaSource.slice(qaItemsSectionStart, qaItemsSectionEnd)
    : ''
const qaItemsColumnsStart = qaSource.indexOf('const qaItemsDefaultColumns')
const qaItemsColumnsEnd =
  qaItemsColumnsStart >= 0 ? qaSource.indexOf('const qaChecksDefaultColumns', qaItemsColumnsStart) : -1
const qaItemsColumnsSource =
  qaItemsColumnsStart >= 0 && qaItemsColumnsEnd > qaItemsColumnsStart
    ? qaSource.slice(qaItemsColumnsStart, qaItemsColumnsEnd)
    : ''
const pressurePumpItemsStart = qaSource.indexOf('const createPressurePumpQaRegulationItems')
const pressurePumpItemsEnd =
  pressurePumpItemsStart >= 0 ? qaSource.indexOf('const qaRegulationItems', pressurePumpItemsStart) : -1
const pressurePumpItemsSource =
  pressurePumpItemsStart >= 0 && pressurePumpItemsEnd > pressurePumpItemsStart
    ? qaSource.slice(pressurePumpItemsStart, pressurePumpItemsEnd)
    : ''

assert.match(
  routeSource,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/QaRegulationPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolQaRegulation'/,
  'QA regulation must have a standalone route at /mes/pro/process-pool/qa-regulation.'
)
assert.match(routeSource, /title:\s*'QA 规程配置'/, 'Standalone route must have a QA title.')
assert.match(
  routeSource,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*permission:\s*\['mes:qa-inspection-regulation:query'\]/,
  'Standalone QA route must use the formal QA permission role for QA tab visibility.'
)

assert.doesNotMatch(
  workbenchSource,
  /<el-tab-pane\s+label="QA 规程"\s+name="QA"\s*\/>/,
  'The production/PQC workbench must not expose QA as an internal Element tab.'
)
assert.doesNotMatch(
  workbenchSource,
  /activeLeaderTab\s*[!=]==?\s*'QA'|type\s+WorkbenchLeaderTab\s*=\s*TeamLeaderType\s*\|\s*'QA'/,
  'The production/PQC workbench must not keep QA tab branching logic.'
)
assert.doesNotMatch(
  workbenchSource,
  /data-qa-regulation-tab|data-qa-regulation-page/,
  'The QA regulation UI must not remain inside TeamLeaderWorkbenchPage.vue.'
)

assert.match(
  qaSource,
  /data-qa-regulation-page/,
  'Standalone QA regulation page must have a stable root selector.'
)
assert.match(
  qaSource,
  /data-qa-regulation-dcc-project/,
  'Standalone QA page must use a DCC project selector as the formal product scope.'
)
assert.match(
  qaSource,
  /getProjectCodePage[\s\S]*DCC_PROJECT_CODE_STATUS_ENABLE/,
  'Standalone QA page must load enabled DCC project codes from the formal API.'
)
assert.match(
  qaSource,
  /data-qa-regulation-project-load-error[\s\S]*retryLoadDccProjectCodes/,
  'DCC project loading failures must remain visible and retryable.'
)
assert.match(
  qaSource,
  /PRESSURE_PUMP_PROJECT_CODE\s*=\s*'IDI'/,
  'The existing pressure-pump draft must be explicitly mapped to DCC project code IDI.'
)
assert.match(
  qaSource,
  /dccProjectCodeId[\s\S]*selectedDccProjectCode[\s\S]*productMasterId/,
  'The selected DCC project must drive the displayed product scope.'
)
assert.match(
  qcTemplateApiSource,
  /getQaRegulationProjectStatuses[\s\S]*\/mes\/qa\/inspection-regulation\/project-statuses/,
  'The API wrapper must retain the formal QA regulation project-statuses endpoint for non-selector uses.'
)
assert.match(
  qcTemplateApiSource,
  /saveQaRegulationDraft[\s\S]*\/mes\/qa\/inspection-regulation\/draft/,
  'Frontend must call the formal QA regulation draft save API.'
)
assert.match(
  qcTemplateApiSource,
  /publishQaRegulation[\s\S]*\/mes\/qa\/inspection-regulation\/publish/,
  'Frontend must call the formal QA regulation publish API.'
)
assert.match(
  dccSelectorSource,
  /<ContentWrap[^>]*data-qa-regulation-dcc-project[\s\S]*<el-form\s+label-width="0"[\s\S]*<el-form-item\s+class="qa-regulation-page__project-field"[\s\S]*<el-select[\s\S]*aria-label="DCC 项目代码"/,
  'QA project selector area must keep a full-width accessible DCC project select without a visible form label.'
)
assert.doesNotMatch(
  dccSelectorSource,
  /<el-form-item[^>]*label="DCC 项目代码"/,
  'The screenshot blue-box DCC project code label must not remain visible.'
)
assert.match(
  dccSelectorSource,
  /class="qa-regulation-page__project-wrap"[\s\S]*data-qa-regulation-dcc-project/,
  'The top DCC project selector card must use the compact project wrapper that removes the red-box blank band.'
)
assert.ok(
  dccSelectorStart >= 0 && dccSelectorStart < firstContentWrapEnd,
  'The DCC project selector must render inside the top yellow header panel instead of in a detached card.'
)
assert.match(
  dccSelectorSource,
  /qa-regulation-page__header[\s\S]*<el-form\s+label-width="0"[\s\S]*aria-label="DCC 项目代码"/,
  'The top panel must show the title and accessible full-width DCC project selector together.'
)
assert.doesNotMatch(
  dccSelectorSource,
  /qa-regulation-page__subtitle|data-qa-regulation-api-ready|QA 按 DCC 项目代码维护产品规程|正式保存\/发布接口已接入/,
  'The red-box subtitle and green API-ready banner must not render in the compact top panel.'
)
assert.doesNotMatch(
  qaSource,
  /<\/ContentWrap>\s*<ContentWrap\s+data-qa-regulation-dcc-project>/,
  'The DCC project selector must not be split into a separate ContentWrap below the yellow header panel.'
)
assert.equal(
  (dccSelectorSource.match(/<el-form-item\b/g) || []).length,
  1,
  'QA project selector area must only render one form row.'
)
assert.doesNotMatch(
  dccSelectorSource,
  /<el-descriptions|data-qa-regulation-config-status|配置状态总览|当前加载范围|已配置 QA 规程|待配置 QA 规程|产品名称由 DCC 项目代码带出/,
  'QA project selector area must not render old project status summaries, descriptions, or helper details.'
)
assert.match(
  qaSource,
  /buildQaRegulationSavePayload[\s\S]*saveQaRegulationDraft[\s\S]*publishQaRegulation/,
  'Standalone QA page must build one formal backend payload for saving and publishing.'
)
assert.match(
  qaSource,
  /routeId[\s\S]*routeVersionId[\s\S]*routeProcessId[\s\S]*processId/,
  'Standalone QA page must require formal route and process IDs before backend save/publish.'
)
assert.match(
  qaSource,
  /ProRouteProductApi[\s\S]*getRouteProductByItem/,
  'QA route scope must read the current product-route binding from the formal product binding API.'
)
assert.match(
  qaSource,
  /loadQaRouteScopeFromRouteBinding[\s\S]*ProRouteApi[\s\S]*getRoute[\s\S]*getRouteVersion[\s\S]*ProRouteProcessApi[\s\S]*getRouteProcessListByRoute[\s\S]*ProRouteFlowConfigApi[\s\S]*getProcessConfigList/,
  'QA route scope must resolve the selected route through route, active version, route process, and route flow-config APIs.'
)
assert.match(
  qaSource,
  /ProRouteApi[\s\S]*getRouteItemBindingList[\s\S]*ProRouteProductApi[\s\S]*saveQaRegulationRouteProductByItem[\s\S]*loadQaRouteScopeFromRouteBinding/,
  'QA route scope must support explicit manual route binding through the QA route-product binding API.'
)
assert.match(
  routeProductApiSource,
  /saveQaRegulationRouteProductByItem[\s\S]*\/mes\/pro\/route-product\/save-qa-regulation-route-by-item/,
  'Frontend must call the QA-specific route binding endpoint instead of the product maintenance endpoint.'
)
assert.match(
  backendRouteProductControllerSource,
  /save-qa-regulation-route-by-item[\s\S]*saveQaRegulationRouteProductByItem/,
  'Backend controller must expose a QA-specific route binding endpoint.'
)
assert.match(
  backendRouteProductServiceSource,
  /saveQaRegulationRouteProductByItem[\s\S]*savePublishedRouteProductByItem/,
  'Backend service must expose a QA-specific binding method that does not reuse product-side route maintenance validation.'
)
const qaBindingMethodStart = backendRouteProductServiceSource.indexOf('saveQaRegulationRouteProductByItem')
const qaBindingMethodEnd =
  qaBindingMethodStart >= 0
    ? backendRouteProductServiceSource.indexOf('savePublishedRouteProductByItem', qaBindingMethodStart)
    : -1
const qaBindingMethodSource =
  qaBindingMethodStart >= 0 && qaBindingMethodEnd > qaBindingMethodStart
    ? backendRouteProductServiceSource.slice(qaBindingMethodStart, qaBindingMethodEnd)
    : ''
assert.doesNotMatch(
  qaBindingMethodSource,
  /validateRouteNotEnable/,
  'QA route binding must allow a published/enabled route and must not call the editable-route guard.'
)
assert.doesNotMatch(
  qaSource,
  /isManualQaRouteOptionDisabled[\s\S]*CommonStatusEnum\.ENABLE|已启用，仅回显|所选工艺路线已启用，不能在产品侧变更绑定/,
  'Manual QA route binding must not disable published/enabled routes in the select.'
)
assert.match(
  qaSource,
  /data-qa-regulation-manual-route-bind[\s\S]*v-model="manualQaRouteBinding\.routeId"[\s\S]*手动绑定工艺路线/,
  'QA route scope must render one manual route select and binding action when the product route binding is missing or needs correction.'
)
assert.match(
  qaSource,
  /loadQaRouteScopeFromProject[\s\S]*resolveDccProjectProductId[\s\S]*getRouteProductByItem[\s\S]*applyFormalQaRouteScope/,
  'Selecting a DCC project must automatically apply the formal route scope instead of asking QA to type route fields.'
)
assert.match(
  qaSource,
  /resolveQaRouteProcessFromRoute[\s\S]*checkFlag[\s\S]*throw new Error/,
  'QA route scope must fail fast when the route does not expose one unambiguous formal QA/check process.'
)
assert.match(
  qaSource,
  /data-qa-regulation-route-scope-auto[\s\S]*data-qa-regulation-route-scope-error[\s\S]*qaRouteScopeLoadError/,
  'Formal route scope loading failures must be visible on the QA page.'
)
assert.doesNotMatch(
  qaSource,
  /title="工艺路线来源"|优先读取产品当前绑定的工艺路线/,
  'The screenshot yellow-box explanatory route-source alert must not be rendered in the QA applicable scope area.'
)
assert.match(
  qaSource,
  /data-qa-regulation-basic-form[\s\S]*qa-regulation-page__basic-grid[\s\S]*qa-regulation-page__basic-field--full/,
  'The screenshot red-box basic regulation fields must use a dedicated grid layout with uniform spacing.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__basic-grid[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)[\s\S]*gap:\s*12px/,
  'The basic regulation field grid must keep consistent two-column spacing.'
)
assert.match(
  qaSource,
  /loadQaRouteScopeFromProject[\s\S]*const boundRouteId\s*=\s*requireQaRouteScopePositiveNumber\(routeProduct\.routeId[\s\S]*manualQaRouteBinding\.routeId\s*=\s*boundRouteId[\s\S]*loadManualQaRouteOptions\(\)[\s\S]*routeId:\s*boundRouteId/,
  'Selecting a DCC project must preselect the existing persisted product-route binding and load route options for a readable default label.'
)
assert.match(
  qaSource,
  /handleManualQaRouteBind[\s\S]*const boundRouteId\s*=\s*requireQaRouteScopePositiveNumber\(routeProduct\.routeId[\s\S]*manualQaRouteBinding\.routeId\s*=\s*boundRouteId[\s\S]*applyFormalQaRouteScope/,
  'After a manual route bind succeeds, the select must keep the persisted binding returned by the formal product-route API.'
)
assert.doesNotMatch(
  qaSource,
  /<el-input(?:-number)?[\s\S]{0,160}v-model="qaRegulationDraft\.(routeName|routeVersionName|routeProcessName|routeId|routeVersionId|routeProcessId|processId|sopName|productionFactor|sampleOrderQuantity|batchRecordBinding)"/,
  'QA users must not manually edit route/process IDs, route name/version/process, SOP, production factor, example quantity, or batch-record binding in the yellow-box area.'
)
assert.match(
  qaSource,
  /v-if="selectedDccProjectCode"[\s\S]*data-qa-regulation-tabs/,
  'QA tabs and regulation content must be hidden until a DCC project is selected.'
)
assert.doesNotMatch(
  qaSource,
  /QA_CONFIGURED_PROJECT_CODE_SET|hasQaRegulationTemplate\s*=\s*\(project/,
  'Standalone QA page must not hardcode configured QA projects after the formal backend status API exists.'
)
assert.doesNotMatch(
  qaSource,
  /data-qa-regulation-config-status|data-qa-regulation-configured-projects|data-qa-regulation-unconfigured-projects|configuredDccProjectCodes|unconfiguredDccProjectCodes|selectDccProjectForConfiguration|loadQaRegulationProjectStatuses|qaRegulationProjectStatusMap|qaRegulationProjectStatusLoadError|QaInspectionRegulationProjectStatusVO/,
  'QA page must not load or display the old configured/unconfigured project status lists after the selector-only requirement.'
)
assert.doesNotMatch(
  qaSource,
  /<el-descriptions|配置状态总览|当前加载范围|已配置 QA 规程|待配置 QA 规程|产品名称由 DCC 项目代码带出/,
  'QA project selector area must not render project details, status summary, or old helper text.'
)
assert.doesNotMatch(
  qaSource,
  /data-qa-regulation-pressure-pump-source/,
  'The standalone page must not keep a pressure-pump-specific source card.'
)
assert.match(
  qaSource,
  /import\s+UnifiedListTemplate\s+from\s+'@\/components\/UnifiedListTemplate\/index\.vue'/,
  'Standalone QA page must use the standard UnifiedListTemplate for dense QA lists.'
)
assert.match(
  qaSource,
  /<el-tabs[\s\S]*v-model="qaActiveTab"[\s\S]*class="qa-regulation-page__tabs qa-regulation-page__tabs--flat"[\s\S]*data-qa-regulation-tabs/,
  'Standalone QA page must split dense content behind QA-owned flat underline tabs.'
)
assert.match(
  qaSource,
  /<ContentWrap\s+class="qa-regulation-page__tabs-wrap"[\s\S]*<el-tabs[\s\S]*data-qa-regulation-tabs/,
  'QA tabs must use a compact wrapper so the red-box blank band below the tabs is not rendered.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__project-wrap,\s*\.qa-regulation-page__tabs-wrap\s*\{[\s\S]*margin-bottom:\s*0/,
  'Compact QA project and tabs wrappers must remove the red-box blank spacing between the selector and QA tabs.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page\s*\{[\s\S]*display:\s*grid[\s\S]*gap:\s*0/,
  'The QA page root grid must remove the screenshot blue-box gaps above and below the tabs.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs-wrap\s*:deep\(\.el-card__body\)\s*\{[\s\S]*padding-top:\s*12px[\s\S]*padding-bottom:\s*0/,
  'QA tabs wrapper must use compact top padding and remove the empty bottom band.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs-wrap\s*:deep\(\.el-tabs__content\)\s*\{[\s\S]*display:\s*none/,
  'QA tabs wrapper must hide empty Element Plus tab content.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs--flat\s*:deep\(\.el-tabs__header\)\s*\{[\s\S]*margin:\s*0/,
  'QA tabs must use the same compact flat header margin as the module tabs above.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs--flat\s*:deep\(\.el-tabs__item\)\s*\{[\s\S]*color:\s*#172033[\s\S]*font-weight:\s*600/,
  'QA tabs must use the same text weight and inactive color as the module tabs above.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs--flat\s*:deep\(\.el-tabs__item\.is-active\)\s*\{[\s\S]*color:\s*#00a896/,
  'QA active tab text must use the same teal active color as the module tabs above.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__tabs--flat\s*:deep\(\.el-tabs__active-bar\)\s*\{[\s\S]*background-color:\s*#00a896/,
  'QA active tab underline must use the same teal active bar as the module tabs above.'
)
for (const requiredTab of [
  { label: '总览', name: 'overview' },
  { label: '检验项目', name: 'items' }
]) {
  assert.match(
    qaSource,
    new RegExp(`<el-tab-pane[\\s\\S]*label="${requiredTab.label}"[\\s\\S]*name="${requiredTab.name}"`),
    `Standalone QA page must include ${requiredTab.label} tab.`
  )
}
assert.doesNotMatch(
  qaSource,
  /<el-tab-pane[^>]*label="发布检查"[^>]*name="verification"/,
  'Standalone QA page must not display the publish verification tab.'
)
for (const requiredTableKey of [
  'mes.qa.regulation.items.processMethods',
  'mes.qa.regulation.checks',
  'mes.qa.regulation.pqcPreview'
]) {
  assert.match(
    qaSource,
    new RegExp(`table-key="${requiredTableKey.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}"`),
    `Standalone QA page must render standard list template ${requiredTableKey}.`
  )
}
assert.match(
  qaItemsSectionSource,
  /<span>工序检验方法与抽样方案<\/span>/,
  'Inspection items tab title must state process inspection methods and sampling plans.'
)
for (const requiredInspectionItemColumn of [
  '工序',
  '检验项目',
  '接受标准',
  '检验方法',
  '检验器具及设备',
  '抽样方案'
]) {
  assert.match(
    qaItemsSectionSource,
    new RegExp(`label="${requiredInspectionItemColumn}"`),
    `Inspection item table must expose the business column ${requiredInspectionItemColumn}.`
  )
}
assert.match(
  qaItemsSectionSource,
  /formatQaItemProcessName\(row\)/,
  'Inspection item rows must display the formal route process for each method row.'
)
assert.match(
  qaItemsSectionSource,
  /formatQaItemSamplingPlan\(row\)/,
  'Inspection item rows must display a sampling plan derived from the item applicable inspection rules.'
)
for (const requiredInspectionItemColumnKey of [
  'routeProcessName',
  'itemName',
  'standardText',
  'inspectionMethod',
  'inspectionTool',
  'samplingPlan'
]) {
  assert.match(
    qaItemsColumnsSource,
    new RegExp(`key:\\s*'${requiredInspectionItemColumnKey}'`),
    `Inspection item column defaults must include ${requiredInspectionItemColumnKey}.`
  )
}
assert.doesNotMatch(
  qaItemsSectionSource,
  /label="项目编码"|label="适用类型"|label="方法"|label="工具"|label="标准"/,
  'Inspection item default table must not keep the old flat project/method/tool/standard labels.'
)
assert.doesNotMatch(
  qaSource,
  /正式保存\/发布接口未接入|未写入后台|data-qa-regulation-api-blocker/,
  'Standalone QA page must not keep the old "API not connected" blocker after draft/publish APIs exist.'
)

for (const requiredText of [
  'QA 规程配置',
  '按压式球囊扩充压力泵组装过程检验规程',
  'PQC-IDI-001',
  'B/0',
  '2026-01-04',
  'IDI',
  '过程检验规程',
  '请选择 DCC 项目代码',
  'DCC 项目代码加载失败',
  '保存草稿',
  '发布规程',
  '总览',
  '检验项目',
  '是否需要末检',
  '正式批记录表单'
]) {
  assert.match(qaSource, new RegExp(requiredText), `Standalone QA page must include ${requiredText}.`)
}
for (const forbiddenScopeText of ['路线 ID', '路线版本 ID', '路线工序 ID', '工序 ID', '示例订单数']) {
  assert.doesNotMatch(
    qaSource,
    new RegExp(`label="${forbiddenScopeText}"|>${forbiddenScopeText}<|${forbiddenScopeText}：`),
    `QA applicable scope must not expose ${forbiddenScopeText} as a manual setup field.`
  )
}

for (const requiredSelector of [
  'data-qa-regulation-tabs',
  'data-qa-regulation-scope',
  'data-qa-regulation-dcc-project',
  'data-qa-regulation-items',
  'data-qa-regulation-original-excerpt',
  'data-qa-regulation-completeness',
  'data-qa-regulation-final-inspection-switch',
  'data-qa-regulation-final-not-applicable-reason',
  'data-qa-pqc-task-preview'
]) {
  assert.match(qaSource, new RegExp(requiredSelector), `Standalone QA page must include ${requiredSelector}.`)
}

for (const requiredRule of ['首检', '上午巡检', '下午巡检', '末检']) {
  assert.match(qaSource, new RegExp(requiredRule), `Standalone QA page must configure ${requiredRule}.`)
}

for (const requiredField of [
  'processName',
  'inspectionMethod',
  'inspectionTool',
  'samplingPlanText',
  'resultType',
  'standardText',
  'lowerLimit',
  'upperLimit',
  'critical',
  'failureRule',
  'sourceOriginalPage',
  'sourceOriginalItem',
  'sourceOriginalExcerpt',
  'sourceOriginalMethod'
]) {
  assert.match(qaSource, new RegExp(requiredField), `QA regulation item model must retain ${requiredField}.`)
}
assert.match(
  qaSource,
  /item\.processName\?\.trim\(\)/,
  'QA regulation item rows must prefer the PDF item process name instead of one global route process.'
)
assert.match(
  qaSource,
  /item\.samplingPlanText\?\.trim\(\)/,
  'QA regulation item rows must preserve the PDF sampling plan text before deriving UI-only rule quantities.'
)

const pressurePumpPdfItemCodes = Array.from(
  pressurePumpItemsSource.matchAll(/itemCode:\s*'([^']+)'/g),
  ([, itemCode]) => itemCode
)
assert.equal(
  new Set(pressurePumpPdfItemCodes).size,
  22,
  'Pressure-pump IDI seed data must contain all 22 PDF 5.1 process inspection rows.'
)
for (const requiredPressurePumpPdfText of [
  "processName: '清洗'",
  "processName: '清洁'",
  "processName: '组装螺杆八组件'",
  "processName: '光固外套四组件'",
  "processName: '装配'",
  "processName: '整体粘结'",
  "itemName: '光固旋转接头 / 外观'",
  "itemName: '光固旋转接头 / 牢固度'",
  "itemName: '光固压力表 / 外观'",
  "itemName: '光固压力表 / 牢固度'",
  "itemName: '光固延长管 / 外观'",
  "itemName: '光固延长管 / 牢固度'",
  "itemName: '装配活塞 / 外观'",
  "itemName: '硅化活塞环 / 外观'",
  "itemName: '装配活塞环 / 外观'",
  "itemName: '装配活塞环 / 配合'",
  "itemName: '外套组件与套筒组件装配 / 外观'",
  "itemName: '外套组件与套筒组件装配 / 配合'",
  "itemName: '气密性 / 负压检测'",
  "itemName: '气密性 / 高压检测'",
  "itemName: '气密性 / 低压检测'",
  "samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4'",
  "samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4'",
  "samplingPlanText: 'GB/T 2828.1，I，AQL=0.4'",
  '气密性检测工装',
  '检测专用泵筒',
  '负压检测：抽负压-80±5kpa，不应有泄漏。',
  '低压检测：将高压检测合格的压力泵装到气密性检测工装上'
]) {
  assert.match(
    pressurePumpItemsSource,
    new RegExp(requiredPressurePumpPdfText.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `Pressure-pump PDF seed data must include ${requiredPressurePumpPdfText}.`
  )
}
assert.doesNotMatch(
  pressurePumpItemsSource,
  /外观确认|装配完整性|密封\/泄漏确认|压力显示\/保压确认|判定规则与记录确认/,
  'Pressure-pump IDI seed data must not keep the old 5-row demo inspection items.'
)

for (const requiredOriginalExcerpt of [
  '原文依据',
  '20atm 压力打至 20atm 应无跳压现象',
  '负压检测：抽负压-80±5kpa，不应有泄漏',
  '正常或矫正视力，在 300~700lx 的照度下',
  '推杆组件推入外套',
  '每一个检验项目均应合格'
]) {
  assert.match(
    qaSource,
    new RegExp(requiredOriginalExcerpt.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `QA regulation item must expose source excerpt: ${requiredOriginalExcerpt}.`
  )
}

assert.doesNotMatch(
  dccProjectLoaderSource,
  /PQC-IDI-001|按压式球囊扩充压力泵/,
  'DCC project loading failures must not fall back to the pressure-pump draft.'
)

console.log('PASS role-matrix QA regulation standalone page static contract')
