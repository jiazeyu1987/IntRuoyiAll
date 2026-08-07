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
  /data-production-leader-module-tabs[\s\S]*<el-tab-pane\s+label="人员管理"\s+name="personnel"[\s\S]*<el-tab-pane\s+label="报工管理"\s+name="report"[\s\S]*<el-tab-pane\s+label="活跃订单池"\s+name="activeOrder"[\s\S]*<el-tab-pane\s+label="看板"\s+name="dashboard"[\s\S]*<el-tab-pane\s+label="工序配置"\s+name="processConfig"/,
  'Shared workbench must render the retained production function tabs.'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /<el-tab-pane\s+label="班组配置"\s+name="config"|data-production-leader-module-tab-config/,
  'Production module tabs must not expose 班组配置.'
)

const productionModuleTabState = teamLeaderWorkbench.match(
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*?>\('personnel'\)/
)?.[0] || ''
assert.ok(productionModuleTabState, 'Production module tabs must default to 人员管理.')
assert.doesNotMatch(
  productionModuleTabState,
  /'config'/,
  'Production module tab state must not retain the removed config key.'
)

for (const moduleName of ['Personnel', 'Report', 'ActiveOrder', 'Dashboard', 'ProcessConfig']) {
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
  /watch\(activeProductionModuleTab,\s*async\s*\(tab\)\s*=>\s*\{[\s\S]*tab\s*===\s*'report'[\s\S]*activeLeaderTab\.value\s*===\s*'PRODUCTION'[\s\S]*queryParams\.leaderType\s*=\s*'PRODUCTION'[\s\S]*queryParams\.pageNo\s*=\s*1[\s\S]*ensureSubmissionDateCondition\(\)[\s\S]*await\s+getSubmissionList\(\)[\s\S]*\}\)/,
  '生产组长切换到报工管理 tab 时必须按 PRODUCTION 组长类型自动加载当天报工列表。'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showProductionActiveOrderModule"[\s\S]*data-team-leader-active-order-pool-tab/,
  '活跃订单池 tab must own the standard active-order list.'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcDashboardModule\s*=\s*computed\([\s\S]*showProductionDashboardModule[\s\S]*activePqcModuleTab[\s\S]*'dashboard'/,
  '看板 tab must own the production daily close dashboard through the dedicated production dashboard gate.'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showPqcDashboardModule"[\s\S]*data-role-matrix-daily-close/,
  '看板 tab must own the daily close dashboard.'
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
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap[\s\S]*v-if="showProductionProcessConfigModule"[\s\S]*data-team-leader-process-config-tab/,
  '工序配置 tab must own loss, device, and parameter maintenance.'
)
assert.match(
  teamLeaderWorkbench,
  /data-team-leader-process-config-tab[\s\S]*<el-button[\s\S]{0,180}data-team-leader-process-config-create-entry[\s\S]{0,180}@click="openCreateProcessConfigDataDialog"[\s\S]{0,80}>\s*新增\s*<\/el-button>[\s\S]*data-team-leader-process-config-table/,
  '工序配置模块头部“新增”按钮必须打开新增配置入口，不能继续执行列表刷新。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /data-team-leader-process-config-tab[\s\S]*<el-button\s+:loading="processConfigLoading"\s+@click="loadProcessConfigRows">\s*刷新\s*<\/el-button>[\s\S]*data-team-leader-process-config-table/,
  '工序配置模块头部操作按钮不得继续显示“刷新”。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /data-team-leader-process-config-tab[\s\S]*<el-button[\s\S]{0,160}@click="loadProcessConfigRows"[\s\S]{0,80}>\s*新增\s*<\/el-button>[\s\S]*data-team-leader-process-config-table/,
  '工序配置模块头部“新增”按钮不得继续绑定 loadProcessConfigRows。'
)
assert.match(
  teamLeaderWorkbench,
  /<el-dialog[\s\S]{0,220}v-model="processConfigCreateDialogVisible"[\s\S]{0,220}data-team-leader-process-config-create-dialog[\s\S]*data-team-leader-process-config-create-process[\s\S]*v-for="row in processConfigRows"[\s\S]*data-team-leader-process-config-create-type[\s\S]*DEVICE_BINDING[\s\S]*PARAMETER_RULE[\s\S]*@click="confirmCreateProcessConfigData"/,
  '工序配置顶部新增入口必须只提供设备映射和参数标准，并由确认动作进入正式维护弹窗。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /<el-radio-button\s+label="LOSS_REASON">损耗原因<\/el-radio-button>/,
  '顶部新增入口不得继续暴露损耗原因。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+ensureProcessConfigRowsLoadedForCreate\s*=\s*async\s*\(\)\s*=>\s*{[\s\S]*await\s+loadProcessConfigRows\(\)[\s\S]*当前账号没有可新增的路线工序，请先在工艺路线的工序开始配置中授权生产组长[\s\S]*}/,
  '工序配置新增入口在候选路线工序为空时必须先调用正式列表接口重新加载，不能直接阻断新增。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+openCreateProcessConfigDataDialog\s*=\s*async\s*\(\)\s*=>\s*{[\s\S]*await\s+ensureProcessConfigRowsLoadedForCreate\(\)[\s\S]*resetProcessConfigCreateForm\(\)[\s\S]*processConfigCreateDialogVisible\.value\s*=\s*true/,
  '工序配置新增入口必须等待路线工序候选加载完成后再打开新增弹窗。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /暂无可新增的路线工序，请先确认工序配置列表已加载/,
  '工序配置新增入口不得在未主动加载候选路线工序时提示用户先确认列表加载。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+confirmCreateProcessConfigData\s*=\s*\(\)\s*=>\s*{[\s\S]*openProcessConfigDeviceDialog\(row\)[\s\S]*openProcessConfigParameterDialog\(row,\s*device,\s*undefined,\s*\{\s*create:\s*true\s*\}\)/,
  '顶部新增入口必须按类型复用设备映射和参数标准的正式保存弹窗。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /const\s+confirmCreateProcessConfigData\s*=\s*\(\)\s*=>\s*{[\s\S]*openCreateLossReason\(row\)/,
  '顶部新增确认逻辑不得继续转入损耗新增。'
)
assert.match(
  teamLeaderWorkbench,
  /createType:\s*'DEVICE_BINDING'\s+as ProcessConfigCreateType/,
  '顶部新增类型必须默认设备映射。'
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
