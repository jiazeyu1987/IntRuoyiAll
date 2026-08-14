const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const pqcLeaderPage = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const productionLeaderPage = read('src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue')
const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(
  pqcLeaderPage,
  /data-pqc-leader-workbench-page[\s\S]*leader-type="PQC"[\s\S]*:show-pqc-module-tabs="true"/,
  'PQC leader sidebar entry must route through the standalone PQC module-tab wrapper.'
)

assert.match(
  teamLeaderWorkbench,
  /const\s+activePqcModuleTab\s*=\s*ref<'personnel'\s*\|\s*'management'\s*\|\s*'dashboard'\s*\|\s*'detail'\s*\|\s*'history'>\('management'\)/,
  'PQC leader module tabs must default to PQC管理.'
)

assert.match(
  teamLeaderWorkbench,
  /data-pqc-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="PQC管理"\s+name="management"[\s\S]*<el-tab-pane\s+label="详情"\s+name="detail"[\s\S]*<el-tab-pane\s+label="看板"\s+name="dashboard"[\s\S]*<el-tab-pane\s+label="历史表单"\s+name="history"/,
  'PQC module tabs must keep personnel available while placing PQC管理 as the default active tab.'
)

assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*activeLeaderTab\.value\s*===\s*'PQC'[\s\S]*activePqcModuleTab\.value\s*===\s*'management'/,
  'PQC管理 content must still be controlled by the management module gate.'
)

assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcPersonnelModule\s*=\s*computed\([\s\S]*activeLeaderTab\.value\s*===\s*'PQC'[\s\S]*activePqcModuleTab\.value\s*===\s*'personnel'/,
  '人员管理 content must remain reachable through the personnel module gate.'
)

assert.doesNotMatch(
  productionLeaderPage,
  /show-pqc-module-tabs/,
  'Production leader wrapper must not inherit the PQC default module tab.'
)

console.log('PASS: PQC leader default management tab static contract')
