const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const scheduleOrderPath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(scheduleOrderPath, 'utf8')

const dialogStart = source.indexOf('v-model="processDialogVisible"')
assert(dialogStart >= 0, '工艺流程排产配置弹窗必须存在。')
const dialogOpenStart = source.lastIndexOf('<Dialog', dialogStart)
const dialogEnd = source.indexOf('</Dialog>', dialogStart)
assert(dialogOpenStart >= 0 && dialogEnd > dialogOpenStart, '工艺流程排产配置弹窗必须存在。')

const dialogSource = source.slice(dialogOpenStart, dialogEnd + '</Dialog>'.length)

assert(
  dialogSource.includes('width="min(1360px, calc(100vw - 24px))"'),
  '工艺流程排产配置弹窗必须尽量使用可用视口宽度，确保桌面端完整显示右侧列。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__process-dialog-table"'),
  '工艺流程排产配置弹窗表格必须带独立类名，便于约束横向可视区域。'
)

assert(
  dialogSource.includes('class="schedule-order-pool__process-summary-table"'),
  '工艺流程排产配置弹窗主表必须带独立类名，避免展开明细表继承主表最小宽度。'
)

assert(
  source.includes('.schedule-order-pool__process-dialog-table') &&
    source.includes('overflow-x: auto'),
  '工艺流程排产配置弹窗必须为窄视口提供横向滚动承载，不能直接裁掉最后一列。'
)

assert(
  !source.includes('.schedule-order-pool__process-dialog-table :deep(.el-table)'),
  '工艺流程排产配置主表最小宽度不得通过通用 el-table 选择器影响展开报工明细表。'
)

assert(
  source.includes('.schedule-order-pool__process-dialog-table :deep(.schedule-order-pool__process-summary-table)') &&
    source.includes('min-width: 1120px'),
  '工艺流程排产配置弹窗主表必须声明覆盖当前 9 个工序汇总列的最小宽度。'
)

const summaryStart = dialogSource.indexOf('<el-table-column label="工序编号"')
const summaryEnd = dialogSource.indexOf('</el-table>', summaryStart)
assert(summaryStart >= 0 && summaryEnd > summaryStart, '工艺流程排产配置主表汇总列必须存在。')
const summarySource = dialogSource.slice(summaryStart, summaryEnd)
const declaredWidths = new Map()

for (const match of summarySource.matchAll(
  /<el-table-column\s+label="([^"]+)"[^>]*(?:width|min-width)="(\d+)"/g
)) {
  declaredWidths.set(match[1], Number(match[2]))
}

const expectedCompactWidths = new Map([
  ['工序编号', 112],
  ['工序名称', 152],
  ['班次产能', 104],
  ['需要多少个', 104],
  ['做了多少个', 104],
  ['状态', 88],
  ['报工次数', 88],
  ['最近报工时间', 146],
  ['预计结束', 146]
])

for (const [label, width] of expectedCompactWidths) {
  assert.equal(declaredWidths.get(label), width, `工艺流程排产配置主表 ${label} 列宽必须压缩到 ${width}px，避免右侧列被裁切。`)
}

const summaryWidthTotal = 44 + Array.from(expectedCompactWidths.values()).reduce((total, width) => total + width, 0)
assert(
  summaryWidthTotal <= 1132,
  `工艺流程排产配置主表列宽合计必须控制在 1132px 内，当前为 ${summaryWidthTotal}px。`
)

console.log('PASS: MES schedule order route progress dialog width static contract')
