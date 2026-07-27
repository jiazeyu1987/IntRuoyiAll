const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(frontendRoot, 'src/views/mes/pro/task/calendar/index.vue')
const packageJsonPath = path.join(frontendRoot, 'package.json')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts?.['e2e:mes:schedule-calendar-visible-months:static'],
  'node tests/e2e/mes-schedule-calendar-visible-months-static.spec.js',
  'package.json must expose the schedule calendar visible-months static contract'
)

assert.match(
  pageSource,
  /const visibleMonthDays = ref<ProScheduleCalendarMonthDayVO\[\]>\(\[\]\)/,
  '排程日历必须保存可见 42 天网格涉及月份的日数据集合。'
)

assert.match(
  pageSource,
  /function resolveCalendarVisibleMonths\(month: Dayjs\)/,
  '排程日历必须集中计算当前 42 天网格涉及的月份。'
)

assert.match(
  pageSource,
  /resolveCalendarVisibleMonths[\s\S]*Array\.from\(\{ length: 42 \}/,
  '可见月份计算必须覆盖日历实际渲染的 42 个日期格。'
)

assert.match(
  pageSource,
  /const monthPayloads = await Promise\.all\([\s\S]*visibleMonths\.map\([\s\S]*ProScheduleCalendarApi\.getMonthCalendar\(\{[\s\S]*month[\s\S]*\}\)/,
  '月视图加载必须调用排程日历自己的月接口读取所有可见月份。'
)

assert.match(
  pageSource,
  /monthData\.value = currentMonthPayload\.data/,
  '当前月份统计必须继续使用当前月接口响应。'
)

assert.match(
  pageSource,
  /visibleMonthDays\.value = monthPayloads\.flatMap\(\(item\) => item\.data\.days\)/,
  '日历单元格必须合并所有可见月份的日数据，避免跨月格子显示未加载的 0。'
)

assert.match(
  pageSource,
  /visibleMonthDays\.value\.forEach\(\(item\) =>/,
  'calendarDayMap 必须来源于合并后的可见月份日数据。'
)

assert.doesNotMatch(
  pageSource,
  /const calendarDayMap = computed\(\(\) => \{[\s\S]*monthData\.value\.days\.forEach/,
  'calendarDayMap 不得只读取当前月 days，否则跨月格子会被当成 0。'
)

assert.match(
  pageSource,
  /visibleMonthDays\.value = \[\]/,
  '加载失败时必须清空可见月份日数据，不能保留陈旧跨月数据。'
)

console.log('PASS: schedule calendar visible months static contract')
