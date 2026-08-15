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
  /data-pqc-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="PQC管理"\s+name="management"[\s\S]*<el-tab-pane\s+label="详情"\s+name="detail"[\s\S]*<el-tab-pane\s+label="历史表单"\s+name="history"/,
  'Shared workbench must render the retained PQC module tabs.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="!showPqcModuleTabs\s+&&\s+!showProductionModuleTabs">[\s\S]*team-leader-workbench__header/,
  'PQC leader must not keep a standalone header/tabs card that leaves blank space above the list.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcManagementModule"[\s\S]*'team-leader-workbench__pqc-module-card':\s*showPqcModuleTabs[\s\S]*data-pqc-leader-module-tabs[\s\S]*<el-form/,
  'PQC管理 tabs must live inside the list card and directly precede the filter/list area.'
)
assert.match(
  teamLeaderWorkbench,
  /class="team-leader-workbench__module-tabs team-leader-workbench__module-tabs--flat"/,
  'PQC module tabs must use the flat DCC-style underline tab class.'
)
assert.match(
  teamLeaderWorkbench,
  /v-if="!showPqcModuleTabs\s+&&\s+!showProductionModuleTabs"\s+class="team-leader-workbench__section-head"/,
  'PQC管理 must hide the old explanatory section head so tabs directly reach the list controls.'
)
assert.match(
  teamLeaderWorkbench,
  /\.team-leader-workbench__module-tabs--flat\s+:deep\(\.el-tabs__header\)\s*\{[\s\S]*margin:\s*0 0 12px/,
  'Flat PQC module tabs must use a compact header margin like the DCC category tabs.'
)
assert.match(
  teamLeaderWorkbench,
  /\.team-leader-workbench__module-tabs--flat\s+:deep\(\.el-tabs__active-bar\)\s*\{[\s\S]*background-color:\s*#00a896/,
  'Flat PQC module tabs must use the teal underline active bar style.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+activePqcModuleTab\s*=\s*ref<'personnel'\s*\|\s*'management'\s*\|\s*'detail'\s*\|\s*'history'>\('management'\)/,
  'PQC leader module tabs must default to PQC管理.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcPersonnelModule\s*=\s*computed\([\s\S]*activePqcModuleTab[\s\S]*'personnel'/,
  'PQC personnel tab must gate the PQC personnel list.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*activePqcModuleTab[\s\S]*'management'/,
  'PQC management tab must gate the management workbench content.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /showPqcDashboardModule|activePqcModuleTab\.value\s*===\s*'dashboard'/,
  'Removed PQC dashboard state must not remain reachable.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcDetailModule\s*=\s*computed\([\s\S]*showPqcDetailAsTab[\s\S]*activePqcModuleTab[\s\S]*'detail'/,
  'PQC detail tab must gate the in-page detail content.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcManagementModule"[\s\S]*data-team-leader-report-workbench/,
  'PQC管理 tab must own the report confirmation workbench.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showLegacyDailyCloseDashboardModule"[\s\S]*data-role-matrix-daily-close/,
  'The legacy daily close dashboard must remain outside PQC module tabs.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showPqcDetailModule"[\s\S]*data-pqc-leader-detail-tab[\s\S]*table-key="mes\.processPool\.teamLeader\.pqcSubmissionDetailItems"/,
  '详情 tab must own the in-page PQC detail standard list.'
)
assert.doesNotMatch(
  pqcLeaderPage,
  /show-leader-type-tabs="true"|:show-leader-type-tabs="true"/,
  'PQC module tabs must not restore production/PQC role switching tabs.'
)

console.log('PASS: PQC leader module tabs static contract')
