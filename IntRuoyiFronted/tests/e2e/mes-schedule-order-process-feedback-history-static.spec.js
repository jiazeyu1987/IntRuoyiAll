const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const viewPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.join(root, 'src/api/mes/pro/scheduleorder/index.ts')

const source = fs.readFileSync(viewPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const dialogStart = source.indexOf('v-model="processDialogVisible"')
assert(dialogStart >= 0, '排产工单查看弹窗必须存在')
const dialogOpenStart = source.lastIndexOf('<Dialog', dialogStart)
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
const dialogSource = source.slice(dialogOpenStart, dialogEnd + '</Dialog>'.length)
const feedbackTitleStart = dialogSource.indexOf('历史报工明细')
assert(feedbackTitleStart >= 0, '工艺流程排产配置弹窗必须支持工序历史报工：历史报工明细')
const feedbackTableStart = dialogSource.lastIndexOf('<el-table', feedbackTitleStart)
const feedbackTableEnd = dialogSource.indexOf('</el-table>', feedbackTableStart)
assert(feedbackTableStart >= 0 && feedbackTableEnd > feedbackTableStart, '必须能定位历史报工明细表格源码。')
const feedbackTableSource = dialogSource.slice(feedbackTableStart, feedbackTableEnd + '</el-table>'.length)

for (const token of [
  'type="expand"',
  '历史报工明细',
  'feedbackHistoryList',
  'feedbackCount',
  'latestFeedbackTime',
  '报工单号',
  '报工时间',
  '本次数量',
  '报工人',
  '暂无报工记录'
]) {
  assert(dialogSource.includes(token), `工艺流程排产配置弹窗必须支持工序历史报工：${token}`)
}

for (const token of ['label="不良数"', 'label="待检数"', 'label="状态"', 'label="备注"']) {
  assert(!feedbackTableSource.includes(token), `历史报工明细表不应显示蓝框冗余列：${token}`)
}

for (const token of [
  'feedbackCount?: number',
  'latestFeedbackTime?: string',
  'feedbackHistoryList?: MesProScheduleOrderProcessFeedbackHistoryVO[]',
  'export interface MesProScheduleOrderProcessFeedbackHistoryVO',
  'feedbackTime?: string',
  'feedbackQuantity?: number',
  'feedbackUserNickname?: string',
  'statusName?: string'
]) {
  assert(apiSource.includes(token), `排产工单工序 API 类型必须暴露历史报工字段：${token}`)
}

console.log('PASS: MES schedule order process feedback history static contract')
