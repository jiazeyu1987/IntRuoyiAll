const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')

assert.match(
  source,
  /const activeSidebarTab = ref\('detail'\)/,
  'Schedule calendar sidebar tabs must default to the day detail tab.'
)

assert.match(
  source,
  /<el-tabs[\s\S]*v-model="activeSidebarTab"[\s\S]*class="sidebar-tabs"/,
  'Schedule calendar sidebar must render an Element Plus tabs container bound to activeSidebarTab.'
)

const detailPaneIndex = source.indexOf('<el-tab-pane :label="selectedDayTitle" name="detail">')
const rulesPaneIndex = source.indexOf('<el-tab-pane label="排程规则" name="rules">')

assert.notEqual(detailPaneIndex, -1, 'The first sidebar tab pane must use selectedDayTitle as the dynamic day detail label.')
assert.equal(rulesPaneIndex, -1, 'Schedule calendar must hide the schedule rules tab pane.')

const shiftOptionsIndex = source.indexOf('const calendarShiftOptions')
assert.notEqual(shiftOptionsIndex, -1, 'Schedule calendar must keep explicit date shift options.')
const shiftOptionsSource = source.slice(shiftOptionsIndex, source.indexOf('const rulesForm', shiftOptionsIndex))
for (const token of [
  "value: 'DAY'",
  "value: 'REST'",
  "label: '白班'",
  "label: '休息'"
]) {
  assert.ok(shiftOptionsSource.includes(token), `Schedule calendar date shift options must preserve ${token}.`)
}
for (const token of [
  "value: 'NIGHT'",
  "value: 'BOTH'",
  "label: '夜班'",
  "label: '双班'",
  '设置白班/夜班/双班/休息'
]) {
  assert.ok(!shiftOptionsSource.includes(token), `Schedule calendar date shift options must not expose ${token}.`)
}

for (const token of [
  '<el-tab-pane label="排程规则" name="rules">',
  '<h3>排程规则</h3>',
  'label="跳过法定节假日"',
  'label="周末模式"'
]) {
  assert.ok(!source.includes(token), `Schedule calendar must not expose hidden rules page token: ${token}.`)
}

for (const token of [
  '生成未来产能',
  '<span>自动排产</span>',
  '预览和发布排产统一在排产工单页面完成'
]) {
  assert.ok(!source.includes(token), `Schedule calendar hidden rules page must not expose ${token}.`)
}

for (const token of [
  'router.push({',
  "name: 'MesProScheduleOrder'",
  "autoOpenReplan: '1'"
]) {
  assert.ok(
    source.includes(token),
    `Schedule calendar must keep manual replan routing logic even after hiding rules page: ${token}.`
  )
}

assert.doesNotMatch(
  source,
  /@click="previewAutoSchedule"|@click="applyAutoSchedule"|:disabled="!canApplyAutoSchedule"/,
  'Schedule calendar auto schedule buttons must not run local preview/apply actions.'
)

for (const token of ['起排时间', '产能模式', '保留手工/锁定任务', '待排工单']) {
  assert.ok(
    !source.includes(token),
    `Schedule calendar must not show hidden local auto schedule option: ${token}.`
  )
}

for (const token of [
  'scheduleStartTime',
  'capacityMode',
  'preserveManualLockedTasks',
  'scopeWorkOrderIds',
  'scopeScheduleOrderIds',
  'loadWorkOrderScope',
  'scopeSummaryText',
  'ScheduleCapacityMode'
]) {
  assert.ok(!source.includes(token), `Schedule calendar must remove non-applied local auto schedule state: ${token}.`)
}

const detailPaneSource = source.slice(detailPaneIndex, source.indexOf('</el-tab-pane>', detailPaneIndex))
for (const token of [
  'detailLoading',
  'detail-summary-grid',
  "openDaySummaryDetail('tasks')",
  "openDaySummaryDetail('orders')",
  "openDaySummaryDetail('dayShift')",
  "openDaySummaryDetail('nightShift')",
  "openDaySummaryDetail('shortages')",
  "openDaySummaryDetail('locked')"
]) {
  assert.ok(detailPaneSource.includes(token), `Day detail tab must preserve ${token}.`)
}
assert.ok(source.includes('nightShiftTaskCount'), 'Schedule calendar must keep night shift task count display.')

assert.match(
  source,
  /const selectedDayTitle = computed\(\(\) => `\$\{selectedDate\.value\} 日详情`\)/,
  'Day detail tab label must keep tracking selectedDate through selectedDayTitle.'
)

assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Schedule calendar tabs must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar detail-first tabs static contract')
