const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workbench = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const teamLeaderApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const frontlinePanel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')

const requireWorkbenchMarker = (marker, message) => {
  assert.match(workbench, new RegExp(marker), message)
}

const requireApiEndpoint = (source, functionName, endpoint, message) => {
  assert.match(source, new RegExp(`${functionName}\\s*=\\s*async`), `${message}: missing function`)
  assert.match(source, new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${message}: missing endpoint`)
}

requireWorkbenchMarker(
  'data-team-leader-process-config-tab',
  'production leader workbench must expose process configuration'
)
requireWorkbenchMarker(
  'data-team-leader-process-config-table',
  'process configuration must expose the route-process list'
)
requireWorkbenchMarker(
  'data-team-leader-process-config-manage-loss',
  'each route process must expose one loss maintenance entry'
)
requireWorkbenchMarker(
  'data-loss-reason-maintenance-dialog',
  'loss maintenance must use the unified dialog'
)
requireWorkbenchMarker(
  'data-loss-reason-maintenance-table',
  'the dialog must show the current route process loss list'
)
requireWorkbenchMarker('data-loss-reason-inline-add', 'the dialog must support bottom create')
requireWorkbenchMarker('data-loss-reason-inline-edit', 'the dialog must support inline edit')
requireWorkbenchMarker('data-loss-reason-inline-delete', 'the dialog must support confirmed delete')

for (const removedLabel of ['新增损耗', '修改损耗', '删除损耗']) {
  assert.doesNotMatch(
    workbench,
    new RegExp(`>\\s*${removedLabel}\\s*<\\/el-button>`),
    `the old ${removedLabel} operation button must not remain`
  )
}

assert.doesNotMatch(
  workbench,
  /<el-option\s+label="损耗"\s+value="LOSS"\s*\/>[\s\S]*保存工序异常原因/,
  'AC-D04 loss reason maintenance must not be hidden inside the old fixed abnormal-reason form'
)

requireApiEndpoint(
  teamLeaderApi,
  'getTeamLeaderProcessConfigList',
  '/process-config/list',
  'formal route-process configuration list API'
)
requireApiEndpoint(teamLeaderApi, 'createTeamLeaderLossReason', '/loss-reasons', 'loss reason create API')
requireApiEndpoint(teamLeaderApi, 'updateTeamLeaderLossReason', '/loss-reasons/', 'loss reason update API')
requireApiEndpoint(teamLeaderApi, 'deleteTeamLeaderLossReason', '/loss-reasons/', 'loss reason delete API')

assert.match(
  teamLeaderApi,
  /interface TeamLeaderProcessConfigRowRespVO[\s\S]*routeProcessId:\s*number[\s\S]*lossReasons:\s*TeamLeaderLossReasonVO\[\]/,
  'formal process configuration rows must be keyed by routeProcessId and carry loss reasons'
)
assert.match(
  teamLeaderApi,
  /interface TeamLeaderLossReasonSaveReqVO[\s\S]*routeProcessId:\s*number[\s\S]*reasonName:\s*string/,
  'loss reason create payload must bind to routeProcessId and submit reason name'
)
assert.doesNotMatch(
  teamLeaderApi,
  /interface TeamLeaderLossReasonSaveReqVO[\s\S]*reasonCode:\s*string/,
  'loss reason create payload must not submit a manual reason code'
)

assert.match(
  frontlinePanel,
  /configuredDefectReasons\s*=\s*computed[\s\S]*deviceState\.runtimeConfig\?\.defectReasons/,
  'frontline reporting dropdown must come from backend runtimeConfig.defectReasons'
)
assert.doesNotMatch(
  frontlinePanel,
  /const\s+configuredDefectReasons\s*=\s*(ref|reactive)\(\s*\[/,
  'frontline reporting must not use a fixed frontend loss reason list'
)
assert.match(
  frontlinePanel,
  /buildProductionLossDetailsPayload[\s\S]*reasonId:\s*defect\.reasonId[\s\S]*lossDetails:\s*buildProductionLossDetailsPayload\(\)/,
  'frontline submit payload must map configured backend reason IDs into loss details'
)
assert.match(
  feedbackApi,
  /lossReasonId\?:\s*number/,
  'frontline feedback API payload type must include lossReasonId'
)

console.log('PASS: process loss reason maintenance static contract is wired')
