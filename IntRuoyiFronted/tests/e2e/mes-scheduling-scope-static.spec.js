const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const scheduleOrderPath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const taskPath = path.resolve(process.cwd(), 'src/views/mes/pro/task/index.vue')
const calendarPath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')
const routeFlowConfigPath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')

for (const filePath of [scheduleOrderPath, taskPath, calendarPath, routeFlowConfigPath]) {
  assert(fs.existsSync(filePath), `排产语义提示页面必须存在：${filePath}`)
}

const scheduleOrderSource = fs.readFileSync(scheduleOrderPath, 'utf8')
const taskSource = fs.readFileSync(taskPath, 'utf8')
const calendarSource = fs.readFileSync(calendarPath, 'utf8')
const routeFlowConfigSource = fs.readFileSync(routeFlowConfigPath, 'utf8')

for (const token of [
  '排产前检查是只读诊断',
  '手动重排会生成变更预览',
  '应用重排前会再次校验阻断问题',
  '主排产入口',
  '确认发布会写入正式排程',
  '正式日历预览',
  '模拟日历预览',
  'calendarRecoveryState',
  '错误对象',
  '影响范围',
  '恢复入口',
  'recoveryButtonText',
  'recoveryQuery',
  'routeProcessId',
  'processCode',
  'openScheduleRouteRecovery',
  'buildCalendarRecoveryState',
  '生成预览',
  '工作站不存在'
]) {
  assert(
    scheduleOrderSource.includes(token)
      || taskSource.includes(token)
      || calendarSource.includes(token)
      || routeFlowConfigSource.includes(token),
    `排产入口语义提示必须覆盖：${token}`
  )
}

assert(
  calendarSource.includes('calendarRecoveryState?.recoveryButtonText') &&
    calendarSource.includes('calendarRecoveryState.value.recoveryQuery'),
  '排程日历恢复卡片必须使用动态按钮文案和恢复查询参数。'
)
assert(
  calendarSource.includes("tab: 'schedule-config'") &&
    calendarSource.includes('routeProcessId'),
  '排程日历/工作台恢复入口必须跳转到工艺流程排产配置页签并携带路线工序上下文。'
)

console.log('PASS: MES scheduling scope static contract')
