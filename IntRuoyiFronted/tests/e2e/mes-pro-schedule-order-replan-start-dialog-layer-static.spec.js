const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), '排产工单页必须存在。')

const source = fs.readFileSync(pagePath, 'utf8')
const replanDrawerStart = source.indexOf('<ScheduleOrderReplanDrawer')
const replanDrawerEnd = source.indexOf('</ScheduleOrderReplanDrawer>', replanDrawerStart)
const replanStartDialog = source.indexOf('v-model="replanStartDateDialogVisible"')

assert.ok(replanDrawerStart >= 0, '手动重排抽屉必须存在。')
assert.ok(replanDrawerEnd > replanDrawerStart, '手动重排抽屉必须正常闭合。')
assert.ok(replanStartDialog > replanDrawerEnd, '开始重排日期确认窗口必须挂载在手动重排抽屉外层，避免被抽屉遮挡。')

console.log('PASS: MES schedule order replan start dialog layer static contract')
