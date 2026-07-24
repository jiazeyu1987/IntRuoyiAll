const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const pagePath = path.join(root, 'src', 'views', 'mes', 'pro', 'scheduleorder', 'index.vue')
const autoScheduleApiPath = path.join(root, 'src', 'api', 'mes', 'pro', 'task', 'autoSchedule', 'index.ts')
const scheduleOrderApiPath = path.join(root, 'src', 'api', 'mes', 'pro', 'scheduleorder', 'index.ts')
const source = fs.readFileSync(pagePath, 'utf8')
const autoScheduleApiSource = fs.readFileSync(autoScheduleApiPath, 'utf8')
const scheduleOrderApiSource = fs.readFileSync(scheduleOrderApiPath, 'utf8')

function assertContains(haystack, text, message) {
  if (!haystack.includes(text)) {
    throw new Error(message)
  }
}

function assertNotContains(haystack, text, message) {
  if (haystack.includes(text)) {
    throw new Error(message)
  }
}

assertNotContains(source, '从今天开始重排', '应用重排不得再提供“从今天开始重排”单选按钮')
assertNotContains(source, '从明天开始重排', '应用重排不得再提供“从明天开始重排”单选按钮')
assertContains(source, '<el-date-picker', '应用重排开始日期必须改为日期选择器')
assertContains(source, 'v-model="replanStartDate"', '应用重排日期选择器必须绑定可编辑日期状态')
assertContains(source, "value-format=\"YYYY-MM-DD\"", '应用重排日期选择器必须输出 YYYY-MM-DD 日期')
assertContains(source, "dayjs().add(1, 'day').format('YYYY-MM-DD')", '应用重排弹窗默认必须选择明天日期')
assertContains(source, 'replanStartDateDialogVisible', '应用重排日期弹窗状态命名必须表达日期选择语义')
assertNotContains(source, 'replanStartChoiceDialogVisible', '应用重排不得保留旧 choice 弹窗状态命名')
assertContains(source, 'buildWholeDayReplanStartTime', '前端必须用整天日期构造重排开始时间')
assertContains(source, "startOf('day')", '重排开始时间必须归一到所选日期 00:00:00')
assertContains(source, 'confirmApplyReplanStartChoice', '点击应用重排后必须先确认任意开始日期')
assertContains(source, 'buildWholeDayReplanStartTime(replanStartDate.value)', '确认应用重排必须按用户选择日期构造开始时间')
assertContains(source, 'runPreflightForRequest(applyRequest)', '应用前必须按所选日期重新执行排产前检查')
assertContains(source, 'previewReplanForRequest(applyRequest)', '应用前必须按所选日期重新生成重排预览')
assertContains(source, 'calendarContextToken: freshPreview.calendarContextToken', '应用必须使用本次重新预览返回的 token')
assertNotContains(source, "replanForm.startTime = formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss')", '打开手动重排面板不得默认使用当前时刻')
assertContains(
  autoScheduleApiSource,
  'export const REPLAN_REQUEST_TIMEOUT = 180000',
  '手动重排必须定义统一长耗时请求超时'
)
assertContains(
  scheduleOrderApiSource,
  "timeout: REPLAN_REQUEST_TIMEOUT",
  '应用确认链路的排产前检查必须使用手动重排长超时'
)
assertContains(
  autoScheduleApiSource,
  "timeout: REPLAN_REQUEST_TIMEOUT",
  '重排预览和应用必须使用手动重排长超时'
)
assertNotContains(
  autoScheduleApiSource,
  'REPLAN_APPLY_REQUEST_TIMEOUT',
  '手动重排超时常量不能只表达 apply，preflight 和 preview 也必须覆盖'
)

console.log('MES replan whole-day apply static contract passed')
