const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), '排产工单页必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')

const defaultDateStart = source.indexOf('const getDefaultReplanStartDate = () =>')
assert.ok(defaultDateStart >= 0, '排产工单页必须保留统一的重排默认日期函数。')
const defaultDateEnd = source.indexOf('\nconst buildWholeDayReplanStartTime', defaultDateStart)
assert.ok(defaultDateEnd > defaultDateStart, '重排默认日期函数必须位于整天开始时间构造函数之前。')
const defaultDateBlock = source.slice(defaultDateStart, defaultDateEnd)

assert.ok(
  /dayjs\(\)\.add\(1,\s*'day'\)\.format\('YYYY-MM-DD'\)/.test(defaultDateBlock),
  '重排默认日期必须是明天。'
)

const drawerStart = source.indexOf('const openReplanDrawer = () => {')
assert.ok(drawerStart >= 0, '手动重排抽屉打开函数必须存在。')
const drawerEnd = source.indexOf('\nconst openReplanSettingsDialog', drawerStart)
assert.ok(drawerEnd > drawerStart, '手动重排抽屉打开函数必须在设置弹窗函数之前结束。')
const drawerBlock = source.slice(drawerStart, drawerEnd)

assert.ok(
  drawerBlock.includes('replanForm.startTime = getDefaultReplanStartDate()'),
  '打开手动重排抽屉时，设置里的“重排开始”默认日期必须取统一默认函数。'
)
assert.ok(
  !/replanForm\.startTime\s*=\s*dayjs\(\)\.format\('YYYY-MM-DD'\)/.test(drawerBlock),
  '打开手动重排抽屉不能直接默认今天。'
)

const applyStart = source.indexOf('const applyReplan = async () => {')
assert.ok(applyStart >= 0, '开始重排处理函数必须存在。')
const applyEnd = source.indexOf('\nconst confirmApplyReplanStartChoice', applyStart)
assert.ok(applyEnd > applyStart, '开始重排处理函数必须在确认日期函数之前结束。')
const applyBlock = source.slice(applyStart, applyEnd)

assert.ok(
  applyBlock.includes('const defaultStartDate = getDefaultReplanStartDate()'),
  '开始重排二次确认默认日期必须取统一默认函数。'
)
assert.ok(
  applyBlock.includes('replanStartDate.value = defaultStartDate'),
  '开始重排二次确认弹窗必须展示统一默认日期。'
)

console.log('PASS: MES schedule order replan default start date static contract')
