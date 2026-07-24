const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert.ok(
  !pageSource.includes('scheduler-workbench__metrics'),
  '排产员工作台顶部不应再渲染指标卡片区域。'
)
assert.ok(
  !pageSource.includes('const metricCards'),
  '排产员工作台不应保留顶部指标卡片数据源。'
)
assert.ok(
  !pageSource.includes('handleMetricCardClick'),
  '移除顶部指标卡片后不应保留卡片点击跳转逻辑。'
)
assert.ok(
  !pageSource.includes('feedbackDeviationDialogVisible'),
  '移除顶部报工偏差卡片后不应保留仅由该卡片打开的偏差弹框。'
)

const settingsEntryIndex = pageSource.indexOf('scheduler-workbench__settings-entry-panel')
const sidePanelsIndex = pageSource.indexOf('scheduler-workbench__side-panels')
assert.ok(settingsEntryIndex >= 0, '排产员工作台必须保留顶部排产设置入口。')
assert.ok(sidePanelsIndex > settingsEntryIndex, '工序/工艺路线在制列表必须直接承接排产设置入口。')

console.log('mes-scheduler-workbench-top-metrics-static: PASS')
