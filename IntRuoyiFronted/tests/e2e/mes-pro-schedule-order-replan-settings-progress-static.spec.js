const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const drawerStart = pageSource.indexOf('<el-drawer v-model="replanDrawerVisible"')
assert.notEqual(drawerStart, -1, '手动重排抽屉必须存在。')
const preflightPanelStart = pageSource.indexOf(
  '<div class="schedule-order-pool__preflight-panel">',
  drawerStart
)
assert.ok(preflightPanelStart > drawerStart, '手动重排抽屉必须保留排产前检查区域。')
const drawerLeadSource = pageSource.slice(drawerStart, preflightPanelStart)

assert.doesNotMatch(
  drawerLeadSource,
  /<el-form[\s\S]*?label="重排开始"[\s\S]*?label="产能口径"[\s\S]*?label="手工锁定"[\s\S]*?label="重排原因"/,
  '重排设置表单不能继续直接展示在抽屉主内容顶部，必须移入设置弹框。'
)

assert.match(
  pageSource,
  /<Dialog[\s\S]*v-model="replanSettingsDialogVisible"[\s\S]*title="重排设置"[\s\S]*label="重排开始"[\s\S]*label="产能口径"[\s\S]*label="手工锁定"[\s\S]*label="重排原因"/,
  '红框内重排设置内容必须集中显示在“重排设置”弹框里。'
)

assert.match(
  pageSource,
  /const replanSettingsDialogVisible = ref\(false\)/,
  '页面必须维护设置弹框显示状态。'
)
assert.match(
  pageSource,
  /const openReplanSettingsDialog = \(\) => \{[\s\S]*replanSettingsDialogVisible\.value = true[\s\S]*\}/,
  '设置按钮必须通过明确方法打开设置弹框。'
)

const actionsStart = pageSource.indexOf('<div class="schedule-order-pool__replan-actions">')
assert.notEqual(actionsStart, -1, '手动重排抽屉必须保留操作区。')
const actionsEnd = pageSource.indexOf('<el-alert', actionsStart)
assert.ok(actionsEnd > actionsStart, '手动重排操作区必须可解析。')
const actionsSource = pageSource.slice(actionsStart, actionsEnd)

const startIndex = actionsSource.indexOf('开始重排')
const settingsIndex = actionsSource.indexOf('设置')
assert.ok(startIndex >= 0, '操作区必须保留合并后的开始重排按钮。')
assert.equal(actionsSource.indexOf('预览重排'), -1, '操作区不能再保留单独的预览重排按钮。')
assert.equal(actionsSource.indexOf('应用重排'), -1, '操作区不能再保留单独的应用重排按钮。')
assert.ok(settingsIndex > startIndex, '设置按钮必须放在开始重排按钮右侧。')
assert.match(
  actionsSource,
  /@click="applyReplan"[\s\S]*开始重排/,
  '开始重排按钮必须触发合并后的重排流程。'
)
assert.match(
  actionsSource,
  /@click="openReplanSettingsDialog"[\s\S]*设置/,
  '操作区设置按钮必须打开重排设置弹框。'
)

assert.match(
  pageSource,
  /showReplanApplyProgress/,
  '页面必须派生开始重排运行中的进度显示状态。'
)
assert.match(
  actionsSource,
  /v-show="showReplanApplyProgress"[\s\S]*重排进度[\s\S]*<el-progress/,
  '重排进度必须固定在开始重排按钮右侧占位，并在点击后显示。'
)
assert.match(
  actionsSource,
  /:percentage="replanApplyProgressPercent"/,
  '重排进度必须绑定显式百分比，避免固定非确定进度条在快速请求中不可见。'
)
assert.doesNotMatch(
  actionsSource,
  /:percentage="100"[\s\S]*:indeterminate="true"/,
  '重排进度不能只使用固定 100 的非确定进度条。'
)

const progressStateStart = pageSource.indexOf('const showReplanApplyProgress')
assert.notEqual(progressStateStart, -1, '必须声明开始重排进度显示状态。')
const progressStateEnd = pageSource.indexOf('\nconst hasReplanPermission', progressStateStart)
assert.ok(progressStateEnd > progressStateStart, '开始重排进度显示状态范围必须可解析。')
const progressStateSource = pageSource.slice(progressStateStart, progressStateEnd)

assert.match(progressStateSource, /replanApplyProgressVisible/, '进度必须有独立可见状态。')
assert.match(progressStateSource, /replanApplyProgressPercent/, '进度必须有独立百分比状态。')
assert.match(
  progressStateSource,
  /replanApplyLoading\.value/,
  '只要应用重排按钮进入 loading，右侧进度必须同步显示。'
)

for (const token of [
  'startReplanApplyProgress',
  'finishReplanApplyProgress',
  'resetReplanApplyProgress',
  'setInterval',
  'clearInterval'
]) {
  assert(pageSource.includes(token), `开始重排进度必须包含 ${token}。`)
}

const applyStart = pageSource.indexOf('const applyReplan = async () => {')
assert.notEqual(applyStart, -1, '开始重排处理函数必须存在。')
const applyEnd = pageSource.indexOf('\nconst openDailyCompareDialog', applyStart)
assert.ok(applyEnd > applyStart, '开始重排处理函数范围必须可解析。')
const applySource = pageSource.slice(applyStart, applyEnd)

assert.match(
  applySource,
  /startReplanApplyProgress\(\)[\s\S]*ProTaskAutoScheduleApi\.replanApply/,
  '开始重排必须在调用写入接口前立即显示进度。'
)
assert.match(
  applySource,
  /await finishReplanApplyProgress\(\)[\s\S]*replanDrawerVisible\.value = false/,
  '开始重排成功后必须先显示 100% 完成态，再关闭抽屉。'
)

assert.doesNotMatch(
  progressStateSource,
  /fallback|mock/i,
  '设置弹框与进度展示不能引入 fallback 或 mock 逻辑。'
)

console.log('PASS: MES schedule order replan settings dialog and progress static contract')
