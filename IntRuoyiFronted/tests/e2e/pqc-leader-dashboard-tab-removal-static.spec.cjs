const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const pqcLeaderPage = read('src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue')
const teamLeaderWorkbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(
  pqcLeaderPage,
  /data-pqc-leader-workbench-page[\s\S]*leader-type="PQC"[\s\S]*:show-pqc-module-tabs="true"/,
  'PQC 组长页面必须继续启用独立内部模块页签。'
)

const pqcTabStripCount = (teamLeaderWorkbench.match(/data-pqc-leader-module-tabs\b/g) || []).length
const dashboardTabCount = (
  teamLeaderWorkbench.match(/data-pqc-leader-module-tab-dashboard(?=[\s/>])/g) || []
).length
const expectedPqcTabs = [
  ['人员管理', 'personnel', 'personnel'],
  ['PQC管理', 'management', 'management'],
  ['详情', 'detail', 'detail'],
  ['历史表单', 'history', 'history']
]

assert.ok(pqcTabStripCount > 0, 'PQC 组长模块页签必须存在。')
assert.equal(dashboardTabCount, 0, '所有 PQC 组长模块页签栏都必须移除“看板”。')

for (const [label, name, selectorSuffix] of expectedPqcTabs) {
  const tabCount = (
    teamLeaderWorkbench.match(
      new RegExp(
        `<el-tab-pane\\s+label="${label}"\\s+name="${name}"\\s+data-pqc-leader-module-tab-${selectorSuffix}\\s*\\/>`,
        'g'
      )
    ) || []
  ).length
  assert.equal(tabCount, pqcTabStripCount, `${label}必须保留在全部 PQC 组长模块页签栏中。`)
}

assert.match(
  teamLeaderWorkbench,
  /const\s+activePqcModuleTab\s*=\s*ref<'personnel'\s*\|\s*'management'\s*\|\s*'detail'\s*\|\s*'history'>\('management'\)/,
  'PQC 组长页签状态必须只包含剩余模块，并默认进入PQC管理。'
)
assert.doesNotMatch(
  teamLeaderWorkbench,
  /activePqcModuleTab\.value\s*===\s*'dashboard'|showPqcDashboardModule/,
  'PQC 看板页签状态和显示门禁必须完整移除。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showLegacyDailyCloseDashboardModule\s*=\s*computed\(\s*\(\)\s*=>\s*isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value\s*\)/,
  '旧班组长日结看板必须保留独立且明确的正式显示门禁。'
)
assert.match(
  teamLeaderWorkbench,
  /<ContentWrap\s+v-if="showLegacyDailyCloseDashboardModule"[\s\S]*data-role-matrix-daily-close/,
  '旧班组长日结看板内容块必须继续保留。'
)

console.log('PASS: PQC leader dashboard tab removal static contract')
