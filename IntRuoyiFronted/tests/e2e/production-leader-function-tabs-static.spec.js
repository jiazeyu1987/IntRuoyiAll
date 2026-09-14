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
  /data-production-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="报工管理"\s+name="report"[\s\S]*<el-tab-pane\s+label="报工历史"\s+name="reportHistory"[\s\S]*<el-tab-pane\s+label="活跃订单池"\s+name="activeOrder"/,
  'Shared workbench must render the retained production function tabs.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /data-production-leader-module-tab-process-config|<el-tab-pane\s+label="工序配置"\s+name="processConfig"/,
  'Shared workbench must not render the migrated 工序配置 as a production function tab.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /<el-tab-pane\s+label="班组配置"\s+name="config"|data-production-leader-module-tab-config/,
  'Production module tabs must not expose 班组配置.'
)

const productionModuleTabState = teamLeaderWorkbench.match(
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*?>\('report'\)/
)?.[0] || ''
assert.ok(productionModuleTabState, 'Production module tabs must default to 报工管理.')
assert.doesNotMatch(
  productionModuleTabState,
  /'config'/,
  'Production module tab state must not retain the removed config key.'
)

for (const moduleName of ['Personnel', 'Report', 'ActiveOrder']) {
  assert.match(
    teamLeaderWorkbench,
    new RegExp(`const\\s+showProduction${moduleName}Module\\s*=\\s*computed\\([\\s\\S]*activeProductionModuleTab`),
    `Production ${moduleName} module must be controlled by the active production module tab.`
  )
}

assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showProductionPersonnelModule"[\s\S]*data-team-leader-production-personnel-tab/,
  '人员管理 tab must own the production personnel management block.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*showProductionReportModule[\s\S]*activePqcModuleTab[\s\S]*'management'/,
  '报工管理 tab must share the report workbench through the existing PQC management gate without changing PQC behavior.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showPqcManagementModule"[\s\S]*data-team-leader-report-workbench/,
  '报工管理 tab must own the report confirmation workbench.'
)
assert.match(
  teamLeaderWorkbench,
  /watch\(activeProductionModuleTab,\s*async\s*\(tab\)\s*=>\s*\{[\s\S]*tab\s*===\s*'report'[\s\S]*activeLeaderTab\.value\s*===\s*'PRODUCTION'[\s\S]*queryParams\.leaderType\s*=\s*'PRODUCTION'[\s\S]*queryParams\.pageNo\s*=\s*1[\s\S]*await\s+getSubmissionList\(\)[\s\S]*\}\)/,
  '生产组长切换到报工管理 tab 时必须按 PRODUCTION 组长类型自动加载报工列表，且不注入默认日期。'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showProductionActiveOrderModule"[\s\S]*data-team-leader-active-order-pool-tab/,
  '活跃订单池 tab must own the standard active-order list.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showLegacyDailyCloseDashboardModule\s*=\s*computed\(\s*\(\)\s*=>\s*isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value\s*\)/,
  'The legacy daily close dashboard must remain available outside production and PQC module tabs.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showLegacyDailyCloseDashboardModule"[\s\S]*data-role-matrix-daily-close/,
  'The legacy daily close dashboard content must remain available.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /showProductionExceptionModule|data-production-leader-module-tab-exception/,
  '独立异常页签和内容门禁必须删除。'
)
assert.match(
  teamLeaderWorkbench,
  /data-team-leader-active-order-pool-tab[\s\S]*data-team-leader-report-active-order-abnormal[\s\S]*data-team-leader-abnormal-report-dialog/,
  '异常上报必须合并到活跃订单池行操作。'
)
const productionProcessConfigGate = teamLeaderWorkbench.match(
  /const\s+showProductionProcessConfigModule\s*=\s*computed\([\s\S]*?(?=const\s+showProductionConfigModule)/
)?.[0] || ''
assert.match(
  productionProcessConfigGate,
  /isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value/,
  '工序配置面板只允许保留在非模块化旧工作台中。'
)
assert.doesNotMatch(
  productionProcessConfigGate,
  /activeProductionModuleTab|['"]processConfig['"]/,
  '工序配置面板不得再作为生产组长模块页签进入。'
)
const productionConfigGate = teamLeaderWorkbench.match(
  /const\s+showProductionConfigModule\s*=\s*computed\([\s\S]*?(?=const\s+showPqcPersonnelModule)/
)?.[0] || ''
assert.match(
  productionConfigGate,
  /isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value/,
  'The legacy team configuration center must only remain in the non-module workbench.'
)
assert.doesNotMatch(
  productionConfigGate,
  /activeProductionModuleTab|['"]config['"]/,
  'The removed config tab must not retain a selectable content gate.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]{0,160}v-if="isProductionLeader"[\s\S]{0,160}data-team-leader-(production-personnel-tab|abnormal-report|process-config-tab|config-center)/,
  'Production-only blocks must be gated by function module tabs, not only by production leader role.'
)

console.log('PASS: production leader function tabs static contract')
