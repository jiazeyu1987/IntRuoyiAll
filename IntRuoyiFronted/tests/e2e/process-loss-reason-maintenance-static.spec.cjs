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

requireWorkbenchMarker('data-team-leader-loss-reason-tab', 'production leader workbench must expose a loss reason tab')
requireWorkbenchMarker('损耗原因维护', 'loss reason tab must use the approved label')
requireWorkbenchMarker('data-loss-reason-standard-list', 'loss reason maintenance must use a standard list area')
requireWorkbenchMarker('data-loss-reason-route-process-row', 'standard list rows must represent route-process records')
requireWorkbenchMarker('data-loss-reason-column', 'standard list must show loss reasons in an independent column')
requireWorkbenchMarker('data-loss-reason-operation-panel', 'loss reason list must expose an operation panel')
requireWorkbenchMarker('新增损耗原因', 'operation panel must support add')
requireWorkbenchMarker('修改损耗原因', 'operation panel must support edit')
requireWorkbenchMarker('删除损耗原因', 'operation panel must support delete')

assert.doesNotMatch(
  workbench,
  /<el-option\s+label="损耗"\s+value="LOSS"\s*\/>[\s\S]*保存工序异常原因/,
  'AC-D04 loss reason maintenance must not be hidden inside the old fixed abnormal-reason form'
)

requireApiEndpoint(
  teamLeaderApi,
  'getTeamLeaderLossReasonPage',
  '/loss-reasons/page',
  'loss reason route-process list API'
)
requireApiEndpoint(teamLeaderApi, 'createTeamLeaderLossReason', '/loss-reasons', 'loss reason create API')
requireApiEndpoint(teamLeaderApi, 'updateTeamLeaderLossReason', '/loss-reasons/', 'loss reason update API')
requireApiEndpoint(teamLeaderApi, 'deleteTeamLeaderLossReason', '/loss-reasons/', 'loss reason delete API')

assert.match(
  teamLeaderApi,
  /interface TeamLeaderLossReasonRowVO[\s\S]*routeProcessId:\s*number[\s\S]*reasons:\s*TeamLeaderLossReasonVO\[\]/,
  'loss reason list row must be keyed by routeProcessId and carry shared reasons'
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
  /lossReasonId:\s*selectedLossReasonId\.value/,
  'frontline submit payload must include selected backend loss reason id'
)
assert.match(
  feedbackApi,
  /lossReasonId\?:\s*number/,
  'frontline feedback API payload type must include lossReasonId'
)

console.log('PASS: process loss reason maintenance static contract is wired')
