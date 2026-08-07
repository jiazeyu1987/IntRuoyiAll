const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const roleMatrixFlow = read('tests/e2e/role-requirement-matrix-real-flow.e2e.js')

assert.doesNotMatch(
  workbench,
  /<el-tab-pane\s+label="班组配置"\s+name="config"|data-production-leader-module-tab-config/,
  'Production module navigation must not expose the removed team configuration tab.'
)

const productionModuleTabState = workbench.match(
  /const\s+activeProductionModuleTab\s*=\s*ref<[\s\S]*?>\('personnel'\)/
)?.[0] || ''
assert.ok(productionModuleTabState, 'Expected the production module tab state declaration.')
assert.doesNotMatch(
  productionModuleTabState,
  /'config'/,
  'Production module tab state must not retain the removed config key.'
)

const productionConfigGate = workbench.match(
  /const\s+showProductionConfigModule\s*=\s*computed\([\s\S]*?(?=const\s+showPqcPersonnelModule)/
)?.[0] || ''
assert.match(
  productionConfigGate,
  /isProductionLeader\.value\s*&&\s*!showProductionModuleTabs\.value/,
  'The legacy team configuration center must remain limited to the non-module workbench.'
)
assert.doesNotMatch(
  productionConfigGate,
  /activeProductionModuleTab|['"]config['"]/,
  'The team configuration center must not retain a selectable module-tab branch.'
)

const productionLeaderPhase = roleMatrixFlow.match(
  /key:\s*'productionLeaderWorkbench'[\s\S]*?(?=key:\s*'pqcLeaderWorkbench')/
)?.[0] || ''
assert.ok(productionLeaderPhase, 'Expected the production leader role-matrix phase.')
assert.doesNotMatch(
  productionLeaderPhase,
  /tabText:\s*'班组配置'|data-team-leader-config-center/,
  'The real production-leader flow must not try to open the removed tab.'
)

for (const tabLabel of ['人员管理', '报工管理', '报工历史', '活跃订单池', '看板', '异常', '工序配置']) {
  assert.match(workbench, new RegExp(`label="${tabLabel}"`), `The retained ${tabLabel} tab must remain.`)
}

console.log('PASS: production leader team configuration tab removal static contract')
