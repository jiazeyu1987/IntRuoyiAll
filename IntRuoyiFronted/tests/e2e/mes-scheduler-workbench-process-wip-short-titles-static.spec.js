const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')

assert(fs.existsSync(pagePath), '排产员工作台页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const label of ['工序编号', '工序名称', '班次产能', '班次状态', '夜班', '开排日期']) {
  assert(new RegExp(`label="${label}"`).test(pageSource), `工序列表必须保留标题：${label}`)
}

for (const label of ['在制单数', '未完需求', '预计完工', '今日报工']) {
  assert(new RegExp(`label="${label}"`).test(pageSource), `工序列表必须使用不超过 4 个字的短标题：${label}`)
}

for (const longLabel of ['正在进行的订单数', '未完成订单需求', '预计完成时间', '今日历史报工数量']) {
  assert(!new RegExp(`label="${longLabel}"`).test(pageSource), `工序列表不得继续使用长标题：${longLabel}`)
}

const shiftCapacityColumn = pageSource.match(
  /prop="shiftCapacityTotal"[\s\S]*?<\/el-table-column>/
)?.[0]
assert(shiftCapacityColumn, '工序列表必须保留班次产能列。')
assert(
  shiftCapacityColumn.includes('formatProcessWipShiftCapacity(row.shiftCapacityTotal)'),
  '红框班次产能必须使用整数展示函数。'
)
assert(
  pageSource.includes("toLocaleString('zh-CN', { maximumFractionDigits: 0 })"),
  '整数展示函数必须按 zh-CN 保留千分位且最多显示 0 位小数。'
)
assert.equal(
  Number(270.000003).toLocaleString('zh-CN', { maximumFractionDigits: 0 }),
  '270',
  '红框班次产能必须隐藏浮点尾差。'
)

console.log('PASS: MES scheduler workbench process WIP short titles static contract')
