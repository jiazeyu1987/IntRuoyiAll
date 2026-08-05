const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const teamLeaderApi = read('src/api/mes/pro/processpool/teamLeader.ts')
const pqcPersonnelStart = teamLeaderWorkbench.indexOf('data-pqc-leader-personnel-tab')
const pqcPersonnelEnd = teamLeaderWorkbench.indexOf('v-if="showPqcManagementModule"', pqcPersonnelStart)
const pqcPersonnelBlock = teamLeaderWorkbench.slice(pqcPersonnelStart, pqcPersonnelEnd)

assert.match(
  teamLeaderWorkbench,
  /const\s+activePqcModuleTab\s*=\s*ref<'personnel'\s*\|\s*'management'\s*\|\s*'dashboard'>\('personnel'\)/,
  'PQC module tabs must default to 人员管理.'
)

assert.match(
  teamLeaderWorkbench,
  /data-pqc-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="PQC管理"\s+name="management"[\s\S]*<el-tab-pane\s+label="看板"\s+name="dashboard"/,
  'PQC module tabs must render 人员管理 / PQC管理 / 看板 in order.'
)

assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcPersonnelModule\s*=\s*computed\([\s\S]*activePqcModuleTab[\s\S]*'personnel'/,
  'PQC personnel content must be gated by the personnel tab.'
)

assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcPersonnelModule"[\s\S]*data-pqc-leader-personnel-tab[\s\S]*data-pqc-leader-module-tabs[\s\S]*<UnifiedListTemplate[\s\S]*table-key="mes\.processPool\.teamLeader\.pqcPersonnel"/,
  'PQC 人员管理 tab must own a standard list template directly under the flat module tabs.'
)

assert.match(
  teamLeaderWorkbench,
  /<template\s+#actions(?:\s*=\s*"[^"]*")?\s*>[\s\S]*data-pqc-personnel-add-button[\s\S]*新增[\s\S]*pqcPersonnelQuery\.enabled/,
  'PQC personnel standard list actions must expose 新增 and enabled filter controls.'
)

assert.match(
  teamLeaderWorkbench,
  /data-pqc-personnel-add-dialog[\s\S]*remote-method="searchPqcFormalEmployeeCandidatesForSelect"[\s\S]*submitLinkPqcFormalEmployee/,
  'PQC 新增 dialog must search formal employee candidates and submit explicit link action.'
)

assert.match(
  teamLeaderWorkbench,
  /data-pqc-leader-personnel-list[\s\S]*<el-table-column\s+label="PQC检验员"[\s\S]*<el-table-column\s+label="账号"[\s\S]*<el-table-column\s+label="状态"[\s\S]*<el-table-column\s+label="操作"/,
  'PQC personnel list must show inspector, account, status, and operation columns.'
)

assert.doesNotMatch(
  pqcPersonnelBlock,
  /临时工/,
  'PQC personnel tab must not introduce temporary employee management.'
)

assert.match(
  teamLeaderApi,
  /getPqcPersonnelList[\s\S]*url:\s*'\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/list'/,
  'Frontend API wrapper must expose PQC personnel list endpoint.'
)

assert.match(
  teamLeaderApi,
  /searchPqcFormalEmployeeCandidates[\s\S]*url:\s*'\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/formal-candidates'/,
  'Frontend API wrapper must expose PQC personnel candidate endpoint.'
)

assert.match(
  teamLeaderApi,
  /linkPqcFormalEmployee[\s\S]*url:\s*'\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/formal\/link'/,
  'Frontend API wrapper must expose PQC personnel formal link endpoint.'
)

assert.match(
  teamLeaderApi,
  /updatePqcPersonnelStatus[\s\S]*url:\s*'\/mes\/pro\/process-pool\/team-leader\/pqc-personnel\/status\/update'/,
  'Frontend API wrapper must expose PQC personnel status endpoint.'
)

console.log('PASS: PQC leader personnel tab static contract')
