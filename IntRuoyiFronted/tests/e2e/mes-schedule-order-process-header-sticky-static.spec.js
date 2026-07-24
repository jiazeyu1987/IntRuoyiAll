const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const viewPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')

const source = fs.readFileSync(viewPath, 'utf8')

const dialogStart = source.indexOf('v-model="processDialogVisible"')
assert(dialogStart >= 0, '排产工单工艺流程排产配置弹窗必须存在。')

const dialogOpenStart = source.lastIndexOf('<Dialog', dialogStart)
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert(dialogOpenStart >= 0 && dialogEnd > dialogStart, '必须能定位工艺流程排产配置弹窗源码。')

const dialogSource = source.slice(dialogOpenStart, dialogEnd + '</Dialog>'.length)
const tableClass = 'schedule-order-pool__process-summary-table'
const tableStart = dialogSource.indexOf(`<el-table\n          class="${tableClass}"`)
assert(tableStart >= 0, '工艺流程排产配置弹窗必须渲染工序汇总表。')

const tableEnd = dialogSource.indexOf('>', tableStart)
const tableOpenTag = dialogSource.slice(tableStart, tableEnd + 1)

assert(
  /(?:^|\s)(?::height|height|(?::)?max-height)=/.test(tableOpenTag),
  '工序汇总表必须设置 height 或 max-height，让 Element Plus 启用固定表头和表体独立滚动。'
)

for (const token of [
  'label="工序编号"',
  'label="工序名称"',
  'label="班次产能"',
  'label="需要多少个"',
  'label="做了多少个"',
  'label="状态"',
  'label="最近报工时间"',
  'label="预计结束"'
]) {
  assert(dialogSource.includes(token), `工序汇总表固定表头必须覆盖列标题：${token}`)
}

console.log('PASS: MES schedule order process header sticky static contract')
