const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/task/autoSchedule/index.ts'),
  'utf8'
)

assert.match(apiSource, /getLatestSuccessfulScheduleApply/)
assert.match(apiSource, /\/mes\/pro\/auto-schedule\/apply\/latest-success/)
assert.match(apiSource, /appliedAt\?: string/)

const scheduleTabStart = pageSource.indexOf('<el-tab-pane label="排产工单" name="scheduleOrders">')
const admissionTabStart = pageSource.indexOf('<el-tab-pane label="同步工单" name="workOrderAdmission">')
assert.ok(scheduleTabStart >= 0, '必须存在排产工单页签。')
assert.ok(admissionTabStart > scheduleTabStart, '同步工单页签必须位于排产工单页签之后。')
const scheduleTabSource = pageSource.slice(scheduleTabStart, admissionTabStart)

assert.match(
  scheduleTabSource,
  /schedule-order-pool__last-success-time[\s\S]*最近一次成功排产时间[\s\S]*latestSuccessfulScheduleApplyTimeText/,
  '排产工单工具栏红框位置必须显示最近成功排产时间。'
)
assert.match(
  pageSource,
  /type ProTaskLatestScheduleApplyRespVO/,
  '排产工单页面必须使用最近一次成功排产事件的正式类型。'
)
assert.match(
  pageSource,
  /const latestSuccessfulScheduleApply = ref<ProTaskLatestScheduleApplyRespVO \| null>\(null\)/,
  '最近成功排产时间必须保存正式接口响应。'
)
assert.match(
  pageSource,
  /ProTaskAutoScheduleApi\.getLatestSuccessfulScheduleApply\(\)/,
  '最近成功排产时间必须来自正式成功排产事件接口。'
)
assert.match(
  pageSource,
  /latestSuccessfulScheduleApply\.value\?\.hasData && latestSuccessfulScheduleApply\.value\?\.appliedAt/,
  '展示时间必须以 hasData 和 appliedAt 为准。'
)
assert.match(
  pageSource,
  /formatDateTime\(latestSuccessfulScheduleApply\.value\.appliedAt\)/,
  '展示时间必须格式化 appliedAt。'
)
assert.doesNotMatch(
  pageSource,
  /latestSuccessfulScheduleApply\.value\?\.requestStartTime/,
  '不能回退到请求开始时间冒充成功排产时间。'
)
assert.match(
  pageSource,
  /loadLatestSuccessfulScheduleApplyTime\(\)[\s\S]*catch[\s\S]*latestSuccessfulScheduleApplyError\.value/,
  '最近成功排产时间加载失败必须显式暴露错误状态。'
)
assert.match(
  pageSource,
  /onMounted\(async \(\) => \{[\s\S]*await loadLatestSuccessfulScheduleApplyTime\(\)/,
  '页面首次打开必须加载最近成功排产时间。'
)
assert.match(
  pageSource,
  /await getScheduleOrderList\(\)[\s\S]*await loadLatestSuccessfulScheduleApplyTime\(\)[\s\S]*emitter\.emit\(MES_PRO_TASK_GANTT_REFRESH_EVENT/,
  '应用重排后的列表刷新链路必须同步刷新最近成功排产时间。'
)
const latestTimeTextStart = pageSource.indexOf('const latestSuccessfulScheduleApplyTimeText')
const latestTimeTextEnd = pageSource.indexOf('const operationLogSummary', latestTimeTextStart)
assert.ok(latestTimeTextStart >= 0 && latestTimeTextEnd > latestTimeTextStart, '必须存在最近成功排产时间展示块。')
const latestTimeTextSource = pageSource.slice(latestTimeTextStart, latestTimeTextEnd)
const latestTimeLoaderStart = pageSource.indexOf('async function loadLatestSuccessfulScheduleApplyTime()')
const latestTimeLoaderEnd = pageSource.indexOf('const scheduleOrderQuickFilter', latestTimeLoaderStart)
assert.ok(latestTimeLoaderStart >= 0 && latestTimeLoaderEnd > latestTimeLoaderStart, '必须存在最近成功排产时间加载函数。')
const latestTimeLoaderSource = pageSource.slice(latestTimeLoaderStart, latestTimeLoaderEnd)
assert.doesNotMatch(
  `${latestTimeTextSource}\n${latestTimeLoaderSource}`,
  /setInterval\s*\(/,
  '最近成功排产时间不得使用定时轮询。'
)

console.log('PASS: MES schedule order last success time static contract')
