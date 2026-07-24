const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/task/calendar/index.vue')
const apiPath = path.join(frontendRoot, 'src/api/mes/pro/scheduleCalendar/index.ts')
const voPath = path.join(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/schedule/vo/calendar/MesProScheduleCalendarDayDetailRespVO.java'
)
const servicePath = path.join(
  workspaceRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProScheduleCalendarServiceImpl.java'
)

for (const filePath of [pagePath, apiPath, voPath, servicePath]) {
  assert.ok(fs.existsSync(filePath), `文件必须存在: ${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const voSource = fs.readFileSync(voPath, 'utf8')
const serviceSource = fs.readFileSync(servicePath, 'utf8')

for (const token of [
  'reportedQuantity',
  'pendingInspectionQuantity',
  'executionStatus',
  'scheduleOrderFrozen',
  'scheduleOrderFreezeReason'
]) {
  assert.ok(apiSource.includes(token), `日历前端接口类型必须声明执行态字段: ${token}`)
  assert.ok(voSource.includes(token), `日历后端 VO 必须返回执行态字段: ${token}`)
  assert.ok(serviceSource.includes(token), `日历服务必须填充执行态字段: ${token}`)
}

assert.ok(apiSource.includes('scheduleIssueSummary'), '日历前端接口类型必须声明异常摘要字段')
assert.ok(voSource.includes('ScheduleIssueSummary'), '日历后端 VO 必须返回异常摘要结构')
assert.ok(serviceSource.includes('buildScheduleIssueSummary'), '日历服务必须构建异常摘要')
assert.ok(serviceSource.includes('setScheduleIssueSummary'), '日历服务必须填充异常摘要')

for (const token of [
  'MesProFeedbackMapper',
  'selectListByTaskIds',
  'MesProFeedbackStatusEnum.UNCHECK',
  'MesProFeedbackStatusEnum.FINISHED',
  'getFeedbackQuantity',
  'getUncheckQuantity'
]) {
  assert.ok(serviceSource.includes(token), `日历服务必须基于真实报工聚合执行态: ${token}`)
}

for (const token of [
  '执行状态',
  '已报工',
  '待检',
  '排产冻结',
  '异常',
  'selectedDayOpenIssueCount',
  'selectedDayScheduleIssues',
  'daySummaryDialogIssueRows',
  'buildTaskExecutionStatusText',
  'buildTaskFreezeText',
  'reportedQuantity',
  'pendingInspectionQuantity'
]) {
  assert.ok(pageSource.includes(token), `日详情任务表格必须展示执行态和冻结态: ${token}`)
}

assert.ok(!serviceSource.includes('catch {}'), '日历执行态后端不得静默吞掉异常')
assert.ok(!pageSource.includes('catch {}'), '日历执行态前端不得静默吞掉异常')

console.log('PASS: MES schedule calendar execution state contract')
