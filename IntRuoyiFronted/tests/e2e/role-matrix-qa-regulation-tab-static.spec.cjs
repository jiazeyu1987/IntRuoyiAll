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
  qaSource,
  /data-qa-regulation-config-status/,
  'Standalone QA page must expose a DCC project QA configuration status summary.'
)
assert.match(
  qcTemplateApiSource,
  /getQaRegulationProjectStatuses[\s\S]*\/mes\/qa\/inspection-regulation\/project-statuses/,
  'Frontend must call the formal QA regulation project-statuses API.'
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
  qaSource,
  /getQaRegulationProjectStatuses[\s\S]*qaRegulationProjectStatusMap/,
  'Standalone QA page must derive configured/unconfigured groups from backend QA regulation status.'
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
  /v-if="\s*!dccProjectCodeLoadError\s*&&\s*!dccProjectCodeOptionsLoading\s*&&\s*!qaRegulationProjectStatusesLoading\s*&&\s*!qaRegulationProjectStatusLoadError\s*"[\s\S]*data-qa-regulation-config-status/,
  'The QA configuration status summary must not report zero projects while DCC or QA status data is loading or failed.'
)
assert.match(
  qaSource,
  /data-qa-regulation-status-load-error[\s\S]*qaRegulationProjectStatusLoadError/,
  'QA regulation status loading failures must remain visible and retryable.'
)
assert.match(
  qaSource,
  /data-qa-regulation-configured-projects/,
  'Standalone QA page must list DCC projects that already have a QA regulation configured.'
)
assert.match(
  qaSource,
  /data-qa-regulation-unconfigured-projects/,
  'Standalone QA page must list DCC projects that still need QA regulation configuration.'
)
assert.match(
  qaSource,
  /configuredDccProjectCodes[\s\S]*unconfiguredDccProjectCodes/,
  'Standalone QA page must split loaded DCC projects into configured and unconfigured groups.'
)
assert.match(
  qaSource,
  /selectDccProjectForConfiguration[\s\S]*applyDccProjectToQaDraft/,
  'Configuration status rows must let QA enter the selected project configuration.'
)
assert.doesNotMatch(
  qaSource,
  /QA_CONFIGURED_PROJECT_CODE_SET|hasQaRegulationTemplate\s*=\s*\(project/,
  'Standalone QA page must not hardcode configured QA projects after the formal backend status API exists.'
)
assert.doesNotMatch(
  qaSource,
  /data-qa-regulation-pressure-pump-source/,
  'The standalone page must not keep a pressure-pump-specific source card.'
)
assert.doesNotMatch(
  qaSource,
  /<el-tabs|<el-tab-pane/,
  'Standalone QA page must not render another internal tab wrapper.'
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
  '产品名称由 DCC 项目代码带出',
  '已配置 QA 规程',
  '待配置 QA 规程',
  '当前加载范围',
  'DCC 项目代码加载失败',
  '正式保存/发布接口已接入',
  '保存草稿',
  '发布规程',
  '路线 ID',
  '路线版本 ID',
  '路线工序 ID',
  '工序 ID'
]) {
  assert.match(qaSource, new RegExp(requiredText), `Standalone QA page must include ${requiredText}.`)
}

for (const requiredSelector of [
  'data-qa-regulation-scope',
  'data-qa-regulation-dcc-project',
  'data-qa-regulation-config-status',
  'data-qa-regulation-configured-projects',
  'data-qa-regulation-unconfigured-projects',
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
