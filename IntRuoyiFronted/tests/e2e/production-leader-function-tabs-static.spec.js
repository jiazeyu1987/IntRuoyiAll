const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const productionLeaderPage = read('src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue')
const pqcLeaderPage = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(
  productionLeaderPage,
  /data-production-leader-workbench-page[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-production-module-tabs="true"/,
  'Production leader standalone page must enable production function module tabs.'
)
assert.doesNotMatch(
  pqcLeaderPage,
  /show-production-module-tabs/,
  'PQC leader page must not opt into production-specific module tabs.'
)

assert.match(
  teamLeaderWorkbench,
  /data-production-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="报工管理"\s+name="report"[\s\S]*<el-tab-pane\s+label="损耗管理"\s+name="loss"[\s\S]*<el-tab-pane\s+label="班组配置"\s+name="config"/,
  'Shared workbench must render production function tabs for personnel, report, loss, and configuration modules.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+activeProductionModuleTab\s*=\s*ref<'personnel'\s*\|\s*'report'\s*\|\s*'loss'\s*\|\s*'config'>\('personnel'\)/,
  'Production module tabs must default to 人员管理.'
)

for (const moduleName of ['Personnel', 'Report', 'Loss', 'Config']) {
  assert.match(
    teamLeaderWorkbench,
    new RegExp(`const\\s+showProduction${moduleName}Module\\s*=\\s*computed\\([\\s\\S]*activeProductionModuleTab`),
    `Production ${moduleName} module must be controlled by the active production module tab.`
  )
}

assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showProductionPersonnelModule"\s+data-team-leader-production-personnel-tab/,
  '人员管理 tab must own the production personnel management block.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*showProductionReportModule[\s\S]*activePqcModuleTab[\s\S]*'management'/,
  '报工管理 tab must share the report workbench through the existing PQC management gate without changing PQC behavior.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcManagementModule"\s+data-team-leader-report-workbench/,
  '报工管理 tab must own the report confirmation workbench.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcDashboardModule\s*=\s*computed\([\s\S]*showProductionReportModule[\s\S]*activePqcModuleTab[\s\S]*'dashboard'/,
  '报工管理 tab must continue to own the production daily close dashboard through the existing PQC dashboard gate.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcDashboardModule"\s+data-role-matrix-daily-close/,
  '报工管理 tab must own the daily close dashboard.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showProductionReportModule"\s+data-team-leader-abnormal-report/,
  '报工管理 tab must own the abnormal report block.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showProductionLossModule"\s+data-team-leader-loss-reason-tab/,
  '损耗管理 tab must own loss reason maintenance.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showProductionConfigModule"\s+data-team-leader-config-center/,
  '班组配置 tab must own the team configuration center.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="isProductionLeader"\s+data-team-leader-(production-personnel-tab|abnormal-report|loss-reason-tab|config-center)/,
  'Production-only blocks must be gated by function module tabs, not only by production leader role.'
)

console.log('PASS: production leader function tabs static contract')
