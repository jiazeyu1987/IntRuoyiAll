const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const assertIncludes = (source, expected, message) => {
  assert(source.includes(expected), message)
}

const assertMatches = (source, pattern, message) => {
  assert.match(source, pattern, message)
}

assertIncludes(api, 'interface TeamLeaderProcessConfigRowRespVO', 'API must expose the unified process config row type.')
assertIncludes(api, 'interface TeamLeaderProcessConfigDeviceVO', 'API must expose mapped device rows under each process row.')
assertIncludes(api, 'interface TeamLeaderProcessConfigParameterVO', 'API must expose device parameter rows under each device.')
assertIncludes(api, 'actualAverage?: number | null', 'Parameter type must expose nullable actualAverage.')
assertIncludes(api, 'sampleCount: number', 'Parameter type must expose sampleCount.')
assertIncludes(api, 'statisticsWindowDays: number', 'Parameter type must expose statisticsWindowDays.')
assertIncludes(api, 'targetValue', 'Frontend API must use targetValue as the public business name.')

assertMatches(
  api,
  /getTeamLeaderProcessConfigList\s*=\s*async[\s\S]*\/mes\/pro\/process-pool\/team-leader\/process-config\/list/,
  'API must read the unified process config list.'
)
assertMatches(
  api,
  /saveTeamProcessConfigDeviceBinding\s*=\s*async[\s\S]*\/mes\/pro\/process-pool\/team-leader\/process-config\/device-binding\/save/,
  'API must save device binding through the routeProcessId scoped endpoint.'
)
assertMatches(
  api,
  /saveTeamProcessConfigDeviceParameterRule\s*=\s*async[\s\S]*\/mes\/pro\/process-pool\/team-leader\/process-config\/device-parameter-rule\/save/,
  'API must save device parameter standards through the routeProcessId scoped endpoint.'
)
assertMatches(
  api,
  /interface TeamProcessDeviceBindingSaveReqVO[\s\S]*routeProcessId:\s*number[\s\S]*deviceId:\s*number/,
  'Device binding request must require routeProcessId + deviceId.'
)
assertMatches(
  api,
  /interface TeamDeviceParameterRuleSaveReqVO[\s\S]*routeProcessId:\s*number[\s\S]*deviceId:\s*number[\s\S]*standardText:\s*string[\s\S]*targetValue\?:\s*number \| string \| null/,
  'Parameter save request must require routeProcessId + deviceId + standardText and allow a missing range target.'
)
assert.doesNotMatch(
  api,
  /\/mes\/pro\/process-pool\/team-leader\/(?:process-device-binding|runtime-device-parameter-rule)\/save/,
  'Old process-only device binding and runtime parameter endpoints must not remain in the frontend API.'
)

assertIncludes(page, 'data-production-leader-module-tab-process-config', 'Production module tabs must expose 工序配置.')
assertMatches(
  page,
  /<el-tab-pane\s+label="工序配置"\s+name="processConfig"[\s\S]*data-production-leader-module-tab-process-config/,
  '工序配置 must be the single process-scoped configuration tab.'
)
assert.doesNotMatch(page, /label="损耗管理"\s+name="loss"/, 'Old standalone 损耗管理 tab must be removed.')
assertMatches(
  page,
  /activeProductionModuleTab\s*=\s*ref<[\s\S]*'processConfig'[\s\S]*>\('report'\)/,
  'Production module tab state must include processConfig.'
)
assertIncludes(page, 'showProductionProcessConfigModule', 'Page must gate the unified process config module explicitly.')
assertIncludes(page, 'data-team-leader-process-config-tab', 'Unified process config module must have a stable root selector.')
assertIncludes(page, 'data-team-leader-process-config-table', 'Unified process config table must have a stable selector.')
assertIncludes(page, ':row-key="(row) => String(row.routeProcessId)"', 'Unified table rows must be keyed by routeProcessId.')
assertIncludes(page, 'data-team-leader-process-config-loss-reasons', 'Unified row must display loss reasons.')
assertIncludes(page, 'data-team-leader-process-config-devices', 'Unified row must display mapped devices.')
assertIncludes(page, 'data-team-leader-process-config-parameters', 'Unified row must display device parameter standards.')
assertIncludes(page, 'data-team-leader-process-config-bind-device', 'Device mapping must start from the current process row.')
assertIncludes(page, 'data-team-leader-process-config-edit-parameter', 'Parameter maintenance must start from the current process row/device.')
assertIncludes(page, 'data-team-leader-process-config-manage-loss', 'Loss maintenance must start from one current-row entry.')
assert.doesNotMatch(
  page,
  /data-team-leader-process-config-add-loss|openCreateLossReason\(|openEditLossReason\(/,
  'The unified table must not retain the legacy separate loss actions.'
)
assertIncludes(page, 'processConfigDeviceDialogVisible', 'Device mapping must use a routeProcess-scoped dialog.')
assertIncludes(page, 'processConfigParameterDialogVisible', 'Parameter standards must use a routeProcess-scoped dialog.')
assertIncludes(page, 'processConfigSelectedRow', 'Dialogs must freeze the selected routeProcessId row context.')
assertIncludes(page, 'processConfigParameterForm.targetValue', 'Parameter form must edit targetValue, not defaultValue.')
assertIncludes(page, 'formatProcessConfigAverage', 'Actual average display must be read-only and null-aware.')
assertIncludes(page, '暂无样本', 'Null average must be rendered as a no-sample state.')
assertIncludes(page, 'await loadProcessConfigRows()', 'Successful saves must reload the formal unified rows.')

assert.doesNotMatch(page, /data-team-leader-loss-reason-tab/, 'Old standalone loss reason table must be removed.')
assert.doesNotMatch(page, /processDeviceBindingForm/, 'Old hand-entered process/device binding form must be removed.')
assert.doesNotMatch(page, /saveTeamProcessDeviceBinding/, 'Page must not call the old process-device binding endpoint.')
assert.doesNotMatch(page, /saveTeamRuntimeDeviceParameterRule/, 'Page must not call the old runtime parameter endpoint.')
assert.doesNotMatch(page, /deviceRuleForm\.processId|deviceRuleForm\.defaultValue|默认值/, 'Parameter UI must not use processId/defaultValue wording.')

console.log('team-leader-process-config-unified-static PASS')
