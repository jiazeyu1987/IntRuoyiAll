const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.doesNotMatch(pageSource, /scheduler-workbench__toolbar/, '顶部标题和日期刷新区域不应继续渲染。')
assert.doesNotMatch(pageSource, /<h3>排产员工作台<\/h3>/, '顶部工作台标题不应显示在首屏。')
assert.doesNotMatch(pageSource, /按生产订单到报工复盘的顺序处理排产事项/, '顶部说明文案不应显示在首屏。')

assert.match(pageSource, /@click="openSchedulerSettingsDialog"/, '页面必须提供排产设置入口。')
assert.match(pageSource, />\s*排产设置\s*</, '排产设置入口必须保留可见按钮文案。')
assert.match(pageSource, /schedulerSettingsDialogVisible/, '班次和策略必须收敛到排产设置弹框。')
assert.match(pageSource, /scheduler-workbench__settings-grid/, '设置弹框必须使用高密度网格布局。')
assert.match(pageSource, /scheduler-workbench__settings-actions/, '设置弹框必须提供内嵌操作区。')
assert.doesNotMatch(pageSource, /scheduler-workbench__smoke-button/, '排产设置弹框不得继续展示冒烟测试按钮。')
assert.doesNotMatch(pageSource, /<details class="scheduler-workbench__test-panel"/, '冒烟测试不得单独显示为折叠卡片。')
assert.doesNotMatch(pageSource, /scheduler-workbench__test-panel/, '冒烟测试不得保留独立卡片样式。')

const settingsSection = pageSource.match(
  /<Dialog[\s\S]*v-model="schedulerSettingsDialogVisible"[\s\S]*title="排产设置"[\s\S]*?<\/Dialog>/
)
assert.ok(settingsSection, '排产设置 Dialog 必须存在。')
for (const token of [
  'saveShiftHoursSetting',
  'savePolicySettings',
  '同步时',
  '重排时',
  '保护项'
]) {
  assert.ok(settingsSection[0].includes(token), `排产设置弹框缺少 ${token}`)
}
assert.ok(!settingsSection[0].includes('toggleSmokeTest'), '排产设置弹框不得保留冒烟测试启停逻辑。')
assert.ok(!settingsSection[0].includes('smokeTestStatusLabel'), '排产设置弹框不得保留冒烟测试状态文案。')

console.log('mes-scheduler-workbench-density-layout-static.spec.js passed')
