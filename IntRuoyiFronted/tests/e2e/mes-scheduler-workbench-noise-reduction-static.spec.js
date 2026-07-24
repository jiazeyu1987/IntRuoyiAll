const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduler-workbench/index.vue')

assert(fs.existsSync(pagePath), '排产员工作台页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const settingsDialogMatch = pageSource.match(
  /<Dialog[\s\S]*v-model="schedulerSettingsDialogVisible"[\s\S]*title="排产设置"[\s\S]*?<\/Dialog>/
)

assert(!pageSource.includes('scheduler-workbench__settings-entry-panel'), '工作台顶部排产入口卡片必须删除。')
assert(!pageSource.includes('scheduler-workbench__settings-entry-copy'), '工作台顶部入口卡片文案容器必须删除。')
assert(!pageSource.includes('工序在制、产能和异常概览'), '工作台顶部不得继续显示红框卡片说明文案。')
assert(settingsDialogMatch, '排产设置弹框必须存在。')

const settingsDialogSource = settingsDialogMatch[0]
const processWipPaneMatch = pageSource.match(
  /<el-tab-pane[\s\S]*label="工序列表"[\s\S]*name="process-list"[\s\S]*?<\/el-tab-pane>/
)
assert(processWipPaneMatch, '工序列表 Tab 必须存在。')
const processWipPaneSource = processWipPaneMatch[0]

assert(
  !/<div class="scheduler-workbench__tab-head">\s*<span>工序在制订单<\/span>[\s\S]*?<\/div>/.test(
    processWipPaneSource
  ),
  '工序列表页签内不得继续显示工序在制标题行。'
)
assert(!processWipPaneSource.includes('按当前工序统计几个订单在做'), '工序列表页签内不得继续显示说明文案。')
assert(
  /<template #actions>[\s\S]*@click="openSchedulerSettingsDialog"[\s\S]*>\s*排产设置\s*<\/el-button>/.test(
    processWipPaneSource
  ),
  '排产设置按钮必须迁移到工序列表标准模板工具栏。'
)
for (const token of [
  'scheduler-workbench__settings-grid',
  'saveShiftHoursSetting',
  'savePolicySettings',
  'saveScheduleRules'
]) {
  assert(settingsDialogSource.includes(token), `排产设置弹框必须保留业务设置能力：${token}`)
}
assert(pageSource.includes('loadScheduleRules'), '页面必须保留排程规则加载逻辑。')
for (const token of ['scheduler-workbench__smoke-block', '冒烟', 'smokeTestStatusClass', '@click="toggleSmokeTest"']) {
  assert(!settingsDialogSource.includes(token), `排产设置弹框不得保留冒烟测试入口：${token}`)
}

assert(
  !pageSource.includes('scheduler-workbench__settings-entry-panel') || !pageSource.includes('toggleSmokeTest'),
  '首页入口区不应把冒烟测试作为页面级主操作。'
)

console.log('PASS: MES scheduler workbench noise reduction static contract')
