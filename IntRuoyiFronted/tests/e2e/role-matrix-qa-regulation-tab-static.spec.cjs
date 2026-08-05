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

assert.ok(fs.existsSync(qaPagePath), 'QA regulation must be implemented as a standalone page.')

const qaSource = fs.readFileSync(qaPagePath, 'utf8')
const workbenchSource = fs.readFileSync(workbenchPath, 'utf8')
const routeSource = fs.readFileSync(routePath, 'utf8')
const qcTemplateApiSource = fs.readFileSync(qcTemplateApiPath, 'utf8')
const dccProjectLoaderStart = qaSource.indexOf('const loadDccProjectCodeOptions')
const dccProjectLoaderEnd = qaSource.indexOf('const retryLoadDccProjectCodes')
const dccProjectLoaderSource = qaSource.slice(dccProjectLoaderStart, dccProjectLoaderEnd)
const dccSelectorStart = qaSource.indexOf('<ContentWrap data-qa-regulation-dcc-project>')
const dccSelectorEnd =
  dccSelectorStart >= 0 ? qaSource.indexOf('</ContentWrap>', dccSelectorStart) : -1
const dccSelectorSource =
  dccSelectorStart >= 0 && dccSelectorEnd > dccSelectorStart
    ? qaSource.slice(dccSelectorStart, dccSelectorEnd)
    : ''
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

assert.match(
  routeSource,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/QaRegulationPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolQaRegulation'/,
  'QA regulation must have a standalone route at /mes/pro/process-pool/qa-regulation.'
)
assert.match(routeSource, /title:\s*'QA 规程配置'/, 'Standalone route must have a QA title.')
assert.match(
  routeSource,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'Standalone QA route must keep the existing process-pool query permission until a formal QA menu permission exists.'
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
  /<ContentWrap[^>]*data-qa-regulation-dcc-project[\s\S]*<el-form[\s\S]*<el-form-item\s+label="DCC 项目代码"\s+required[\s\S]*<el-select/,
  'QA project selector area must only keep the required DCC project code select row.'
)
assert.equal(
  (dccSelectorSource.match(/<el-form-item\b/g) || []).length,
  1,
  'QA project selector area must only render one form row.'
)
assert.doesNotMatch(
  dccSelectorSource,
  /<el-descriptions|el-tag|data-qa-regulation-config-status|配置状态总览|当前加载范围|已配置 QA 规程|待配置 QA 规程|产品名称由 DCC 项目代码带出/,
  'QA project selector area must not render status, summary, tags, descriptions, or helper details.'
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
  /ProRouteProductApi[\s\S]*getRouteProductByItem[\s\S]*ProRouteApi[\s\S]*getRoute[\s\S]*getRouteVersion[\s\S]*ProRouteProcessApi[\s\S]*getRouteProcessListByRoute[\s\S]*ProRouteFlowConfigApi[\s\S]*getProcessConfigList/,
  'QA route scope must be loaded from the formal product route binding, active route version, route process, and route flow-config APIs.'
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
  /<el-tabs[\s\S]*v-model="qaActiveTab"[\s\S]*data-qa-regulation-tabs/,
  'Standalone QA page must split dense content behind QA-owned tabs.'
)
for (const requiredTab of [
  { label: '总览', name: 'overview' },
  { label: '检验规则', name: 'rules' },
  { label: '检验项目', name: 'items' },
  { label: '发布检查', name: 'verification' }
]) {
  assert.match(
    qaSource,
    new RegExp(`<el-tab-pane[\\s\\S]*label="${requiredTab.label}"[\\s\\S]*name="${requiredTab.name}"`),
    `Standalone QA page must include ${requiredTab.label} tab.`
  )
}
for (const requiredTableKey of [
  'mes.qa.regulation.rules',
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
  'QA 按 DCC 项目代码维护产品规程',
  '请选择 DCC 项目代码',
  'DCC 项目代码加载失败',
  '正式保存/发布接口已接入',
  '保存草稿',
  '发布规程',
  '总览',
  '检验规则',
  '检验项目',
  '发布检查',
  '工艺路线来源',
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
  'data-qa-regulation-inspection-rules',
  'data-qa-regulation-items',
  'data-qa-regulation-original-excerpt',
  'data-qa-regulation-completeness',
  'data-qa-pqc-task-preview'
]) {
  assert.match(qaSource, new RegExp(requiredSelector), `Standalone QA page must include ${requiredSelector}.`)
}

for (const requiredRule of ['首检', '上午巡检', '下午巡检', '末检']) {
  assert.match(qaSource, new RegExp(requiredRule), `Standalone QA page must configure ${requiredRule}.`)
}

for (const requiredField of [
  'inspectionMethod',
  'inspectionTool',
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
