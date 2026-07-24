const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const shiftCapacityColumn = pageSource.match(
  /prop="shiftCapacityTotal"[\s\S]*?<\/el-table-column>/
)?.[0]
assert(shiftCapacityColumn, '工序列表必须保留班次产能列。')
assert(
  shiftCapacityColumn.includes('formatProcessWipShiftCapacity(row.shiftCapacityTotal)'),
  '班次产能必须显示为整数并继续保留来源点击入口。'
)
assert(
  shiftCapacityColumn.includes('v-if="isProcessWipDoubleShift(row)"'),
  '有效白夜班必须在班次产能后显示倍数标识。'
)
assert(
  /<el-tag[\s\S]*type="success"[\s\S]*class="scheduler-workbench__shift-capacity-multiplier"[\s\S]*>\s*X2\s*<\/el-tag>/.test(
    shiftCapacityColumn
  ),
  '班次产能倍数必须使用绿色 X2 标签。'
)

const shiftStatusColumn = pageSource.match(
  /prop="shiftStatus"[\s\S]*?<\/el-table-column>/
)?.[0]
assert(shiftStatusColumn, '工序列表必须保留班次状态列。')
assert(
  shiftStatusColumn.includes('getProcessWipShiftStatusText(row)'),
  '班次状态列必须使用统一状态映射。'
)
assert(
  shiftStatusColumn.includes('getProcessWipShiftStatusTagType()'),
  '班次状态标签颜色必须继续使用统一状态函数。'
)

for (const token of [
  'const isProcessWipDoubleShift =',
  'Boolean(row.nightShiftEnabled)',
  "if (isProcessWipDoubleShift(row)) return '白夜班'",
  "return row.shiftStatus || '白班'",
  "{ label: '白夜班', value: '白夜班' }",
  "getProcessWipShiftStatusText(item)"
]) {
  assert(pageSource.includes(token), `白夜班展示必须包含统一契约：${token}`)
}
assert(!pageSource.includes('nightShiftMixed'), '夜班状态不得重新引入跨路线混合状态。')

assert(
  !shiftCapacityColumn.includes('row.shiftCapacityTotal * 2'),
  '前端不得把班次产能再次乘二，X2 仅用于展示双班状态。'
)

console.log('PASS: MES scheduler workbench process WIP double shift static contract')
