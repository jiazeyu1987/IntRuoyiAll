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
  /leader-type="PRODUCTION"[\s\S]*:show-production-module-tabs="true"/,
  '生产组长页面必须以 PRODUCTION 身份启用内部模块 tabs。'
)
assert.doesNotMatch(
  pqcLeaderPage,
  /data-production-leader-module-tab-workbench|showProductionWorkbenchModule|show-production-module-tabs/,
  'PQC 组长页面不能暴露生产组长工作台 tab。'
)

const productionTabStripCount = (teamLeaderWorkbench.match(/data-production-leader-module-tabs/g) || []).length
const workbenchTabCount = (
  teamLeaderWorkbench.match(/data-production-leader-module-tab-workbench(?=[\s/>])/g) || []
).length
const dashboardTabCount = (
  teamLeaderWorkbench.match(/data-production-leader-module-tab-dashboard(?=[\s/>])/g) || []
).length
const expectedProductionTabs = [
  ['人员管理', 'personnel', 'personnel'],
  ['报工管理', 'report', 'report'],
  ['报工历史', 'reportHistory', 'report-history'],
  ['活跃订单池', 'activeOrder', 'active-order'],
  ['工序配置', 'processConfig', 'process-config']
]

assert.ok(productionTabStripCount > 0, '生产组长模块 tabs 必须存在。')
assert.equal(
  workbenchTabCount,
  0,
  '所有生产组长模块 tab 条都必须移除“生产组长工作台”。'
)
assert.equal(
  dashboardTabCount,
  0,
  '所有生产组长模块 tab 条都必须移除“看板”。'
)

for (const [label, name, selectorSuffix] of expectedProductionTabs) {
  const escapedLabel = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const tabCount = (
    teamLeaderWorkbench.match(
      new RegExp(
        `<el-tab-pane\\s+label="${escapedLabel}"\\s+name="${escapedName}"\\s+data-production-leader-module-tab-${selectorSuffix}\\s*\\/>`,
        'g'
      )
    ) || []
  ).length
  assert.equal(tabCount, productionTabStripCount, `${label}必须保留在全部生产组长模块 tab 条中。`)
}

assert.doesNotMatch(
  teamLeaderWorkbench,
  /生产组长工作台|showProductionWorkbenchModule|showProductionDashboardModule|tab\s*===\s*'workbench'/
)
assert.match(
  teamLeaderWorkbench,
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*'processConfig'[\s\S]*>\('report'\)/,
  'activeProductionModuleTab 必须保留剩余模块类型，并默认进入报工管理。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /data-pqc-leader-module-tab-dashboard|activePqcModuleTab\.value\s*===\s*'dashboard'|showPqcDashboardModule/,
  'PQC 组长看板页签和专属状态必须移除。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showLegacyDailyCloseDashboardModule\s*=\s*computed\(\s*\(\)\s*=>\s*isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value\s*\)/,
  '旧班组长日结看板必须继续保留，生产组长和 PQC 内部页签模式不得进入该看板。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*showProductionReportModule\.value[\s\S]*showProductionReportHistoryModule\.value[\s\S]*activePqcModuleTab\.value\s*===\s*'management'/,
  '移除重复工作台页签后，报工管理与 PQC 管理 gate 必须保持完整。'
)
assert.match(
  teamLeaderWorkbench,
  /watch\(activeProductionModuleTab,\s*async\s*\(tab\)\s*=>\s*\{[\s\S]*tab\s*===\s*'report'[\s\S]*tab\s*===\s*'reportHistory'[\s\S]*activeLeaderTab\.value\s*===\s*'PRODUCTION'[\s\S]*queryParams\.leaderType\s*=\s*'PRODUCTION'[\s\S]*await\s+getSubmissionList\(\)[\s\S]*\}\)/,
  '切换报工管理或报工历史时必须继续按生产组长上下文加载正式列表。'
)

console.log('PASS: production leader workbench tab removal static contract')
