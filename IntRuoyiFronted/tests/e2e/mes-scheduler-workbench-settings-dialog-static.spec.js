const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.doesNotMatch(pageSource, /scheduler-workbench__steps-panel/, '首页不应继续显示行动链路卡片。')
assert.doesNotMatch(pageSource, />行动链路</, '首页不应继续显示行动链路标题。')
assert.doesNotMatch(pageSource, /v-for="step in summary\.steps/, '首页不应继续渲染 summary.steps 行动入口。')
assert.doesNotMatch(pageSource, /openWorkbenchStep/, '隐藏行动链路后不应保留行动步骤跳转逻辑。')
assert.doesNotMatch(
  pageSource,
  /<section class="scheduler-workbench__panel scheduler-workbench__settings-panel">/,
  '排产设置不应继续作为首页内嵌面板显示。'
)

assert.match(pageSource, /schedulerSettingsDialogVisible/, '页面必须使用显式状态控制排产设置弹框。')
assert.match(pageSource, />\s*排产设置\s*</, '首页必须提供排产设置按钮。')
assert.match(pageSource, /@click="openSchedulerSettingsDialog"/, '排产设置按钮必须打开设置弹框。')
assert.match(pageSource, /<Dialog[\s\S]*v-model="schedulerSettingsDialogVisible"[\s\S]*title="排产设置"/, '排产设置内容必须放入标题为排产设置的弹框。')

const dialogSource = pageSource.match(
  /<Dialog[\s\S]*v-model="schedulerSettingsDialogVisible"[\s\S]*title="排产设置"[\s\S]*?<\/Dialog>/
)
assert.ok(dialogSource, '排产设置 Dialog 必须存在。')

for (const token of [
  'scheduler-workbench__settings-grid',
  'scheduler-workbench__settings-row',
  'scheduler-workbench__settings-field',
  'scheduler-workbench__settings-control',
  'scheduler-workbench__settings-button',
  'scheduler-workbench__settings-actions--compact',
  '排程规则',
  '跳过法定节假日',
  '周末模式',
  '保存规则',
  'scheduleRulesForm.skipStatutoryHolidays',
  'scheduleRulesForm.weekendRestMode',
  'saveScheduleRules',
  'saveShiftHoursSetting',
  'savePolicySettings',
  'openFullConfigImport',
  '导出全部数据包',
  '导入全部数据包',
  '保存策略'
]) {
  assert.ok(dialogSource[0].includes(token), `排产设置弹框缺少原有设置能力：${token}`)
}
for (const token of [
  'openRouteConfigImport',
  'exportRouteConfigPackage',
  'routeConfigInputRef',
  'handleRouteConfigFileChange',
  '导出排产工艺路线',
  '导入排产工艺路线'
]) {
  assert.ok(!dialogSource[0].includes(token), `排产设置弹框不得保留排产工艺路线导入导出入口：${token}`)
}
for (const token of ['toggleSmokeTest', '开始测试', '结束测试', '冒烟']) {
  assert.ok(!dialogSource[0].includes(token), `排产设置弹框不得保留冒烟测试内容：${token}`)
}

for (const token of [
  "from '@/api/mes/pro/scheduleCalendar'",
  'loadScheduleRules',
  'ProScheduleCalendarApi.getRules',
  'ProScheduleCalendarApi.updateRules',
  'dateShiftModeByDate'
]) {
  assert.ok(pageSource.includes(token), `排产设置弹框必须复用排程日历规则接口并保留日期覆盖：${token}`)
}

const policySaveBlock = pageSource.slice(
  pageSource.indexOf('const savePolicySettings'),
  pageSource.indexOf('const buildScheduleRulesPayload')
)
for (const token of ['skipStatutoryHolidays', 'weekendRestMode']) {
  assert.ok(
    !policySaveBlock.includes(token),
    `排程规则字段不得通过排产策略保存接口提交：${token}`
  )
}

for (const token of [
  '--scheduler-settings-label-width',
  '--scheduler-settings-control-height',
  '--scheduler-settings-button-min-width',
  'grid-template-columns: repeat(3, minmax(280px, 1fr))',
  'min-height: var(--scheduler-settings-control-height)',
  'min-width: var(--scheduler-settings-button-min-width)',
  'align-items: end',
  'justify-content: flex-end'
]) {
  assert.ok(pageSource.includes(token), `排产设置弹框必须统一对齐和尺寸样式：${token}`)
}

console.log('mes-scheduler-workbench-settings-dialog-static: PASS')
