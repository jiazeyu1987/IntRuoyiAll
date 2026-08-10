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

assert.ok(productionTabStripCount > 0, '生产组长模块 tabs 必须存在。')
assert.equal(
  workbenchTabCount,
  productionTabStripCount,
  '生产组长工作台 tab 必须同步出现在所有生产组长模块 tab 条中，不能只改当前可见块。'
)

assert.match(
  teamLeaderWorkbench,
  /<el-tab-pane\s+label="生产组长工作台"\s+name="workbench"\s+data-production-leader-module-tab-workbench\s*\/>/,
  '生产组长模块 tabs 必须提供“生产组长工作台”tab。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*'workbench'[\s\S]*>\('report'\)/,
  'activeProductionModuleTab 类型必须包含 workbench，默认进入报工管理。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showProductionWorkbenchModule\s*=\s*computed\([\s\S]*isProductionLeader\.value[\s\S]*activeProductionModuleTab\.value\s*===\s*'workbench'[\s\S]*\)/,
  '生产组长工作台内容必须由独立 workbench gate 控制。'
)
assert.match(
  teamLeaderWorkbench,
  /const\s+showPqcManagementModule\s*=\s*computed\([\s\S]*showProductionWorkbenchModule\.value[\s\S]*showProductionReportModule\.value[\s\S]*activePqcModuleTab\.value\s*===\s*'management'/,
  '生产组长工作台 tab 必须复用现有报工工作台内容，但不能破坏 PQC 管理 gate。'
)
assert.match(
  teamLeaderWorkbench,
  /watch\(activeProductionModuleTab,\s*async\s*\(tab\)\s*=>\s*\{[\s\S]*tab\s*===\s*'workbench'[\s\S]*tab\s*===\s*'report'[\s\S]*activeLeaderTab\.value\s*===\s*'PRODUCTION'[\s\S]*queryParams\.leaderType\s*=\s*'PRODUCTION'[\s\S]*await\s+getSubmissionList\(\)[\s\S]*\}\)/,
  '切换到生产组长工作台 tab 时必须按生产组长上下文加载正式报工工作台列表。'
)

console.log('PASS: production leader workbench tab static contract')
