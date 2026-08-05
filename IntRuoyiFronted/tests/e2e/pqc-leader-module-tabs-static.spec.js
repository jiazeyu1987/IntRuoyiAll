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
  'PQC leader standalone page must enable page-internal module tabs.'
)
assert.doesNotMatch(
  productionLeaderPage,
  /show-pqc-module-tabs/,
  'Production leader page must not opt into PQC-specific module tabs.'
)

assert.match(
  teamLeaderWorkbench,
  /data-pqc-leader-module-tabs[\s\S]*<el-tab-pane\s+label="PQC管理"\s+name="management"[\s\S]*<el-tab-pane\s+label="看板"\s+name="dashboard"/,
  'Shared workbench must render PQC management and dashboard module tabs.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+activePqcModuleTab\s*=\s*ref<'management'\s*\|\s*'dashboard'>\('management'\)/,
  'PQC leader module tabs must default to PQC管理.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*activePqcModuleTab[\s\S]*'management'/,
  'PQC management tab must gate the management workbench content.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcDashboardModule\s*=\s*computed\([\s\S]*activePqcModuleTab[\s\S]*'dashboard'/,
  'PQC dashboard tab must gate the dashboard content.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcManagementModule"\s+data-team-leader-report-workbench/,
  'PQC管理 tab must own the report confirmation workbench.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcDashboardModule"\s+data-role-matrix-daily-close/,
  '看板 tab must own the daily close dashboard.'
)
assert.doesNotMatch(
  pqcLeaderPage,
  /show-leader-type-tabs="true"|:show-leader-type-tabs="true"/,
  'PQC module tabs must not restore production/PQC role switching tabs.'
)

console.log('PASS: PQC leader module tabs static contract')
