const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(scheduleOrderPath, 'utf8')

const dialogStart = source.indexOf('v-model="processDialogVisible"')
assert(dialogStart >= 0, '工艺流程排产配置弹窗必须存在')
const dialogOpenStart = source.lastIndexOf('<Dialog', dialogStart)
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert(dialogOpenStart >= 0 && dialogEnd > dialogOpenStart, '工艺流程排产配置弹窗必须存在')

const dialogSource = source.slice(dialogOpenStart, dialogEnd + '</Dialog>'.length)
const labels = Array.from(dialogSource.matchAll(/<el-table-column\s+label="([^"]+)"/g)).map(
  (match) => match[1]
)

assert.deepEqual(
  labels,
  [
    '报工单号',
    '报工时间',
    '本次数量',
    '合格数',
    '不良数',
    '待检数',
    '报工人',
    '状态',
    '备注',
    '工序编号',
    '工序名称',
    '班次产能',
    '需要多少个',
    '做了多少个',
    '状态',
    '报工次数',
    '最近报工时间',
    '预计结束'
  ],
  `工艺流程排产配置弹窗必须显示工序汇总列和展开报工明细列，当前列为：${labels.join('、')}`
)

for (const hiddenLabel of [
  '序号',
  '产能来源',
  '总产能/h',
  '班次小时',
  '总产能/班次',
  '需要个数',
  '已报工',
  '已完成个数',
  '待审批',
  '待检',
  '超报',
  '剩余数量',
  '工序进度',
  '计划开始',
  '计划完成'
]) {
  assert(!labels.includes(hiddenLabel), `工艺流程排产配置弹窗不得显示额外列：${hiddenLabel}`)
}

assert(dialogSource.includes('shiftCapacityTotal'), '班次产能列必须使用 shiftCapacityTotal')
assert(dialogSource.includes('plannedQuantity'), '需要多少个列必须使用 plannedQuantity')
assert(dialogSource.includes('effectiveCompletedQuantity'), '做了多少个列必须使用 effectiveCompletedQuantity')
assert(dialogSource.includes('getProcessProgressStatusText'), '状态列必须使用工序进度状态')
assert(dialogSource.includes('feedbackCount'), '工序汇总必须显示报工次数')
assert(dialogSource.includes('latestFeedbackTime'), '工序汇总必须显示最近报工时间')
assert(dialogSource.includes('feedbackHistoryList'), '展开行必须显示历史报工明细')
assert(dialogSource.includes('历史报工明细'), '展开行标题必须说明历史报工明细')
assert(dialogSource.includes('empty-text="暂无报工记录"'), '无报工工序必须显示空明细提示')
assert(dialogSource.includes('plannedEndTime'), '预计结束列必须使用 plannedEndTime')
assert(source.includes('scheduled-not-started'), '未开始状态必须细分已排产未开始')
assert(source.includes('unscheduled'), '未开始状态必须细分未排产')
assert(source.includes('已排产未开始'), '状态文案必须包含已排产未开始')
assert(source.includes('未排产'), '状态文案必须包含未排产')

console.log('PASS: MES schedule order route progress columns static contract')
