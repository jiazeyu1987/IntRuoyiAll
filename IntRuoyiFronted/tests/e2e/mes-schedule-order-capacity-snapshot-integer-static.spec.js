const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const extractConstFunction = (source, functionName) => {
  const start = source.indexOf(`const ${functionName} =`)
  assert(start >= 0, `必须定义 ${functionName}`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  return source.slice(start, nextConst >= 0 ? nextConst : source.length)
}

const capacitySnapshotBlock = pageSource.match(
  /schedule-order-pool__capacity-snapshot[\s\S]*?schedule-order-pool__feedback-history/
)?.[0]
assert(capacitySnapshotBlock, '排产工单工序展开区必须保留产能快照。')

for (const requiredUsage of [
  'formatCapacityIntegerNumber(row.hourlyCapacityTotal)',
  'formatCapacityIntegerNumber(row.shiftCapacityTotal)',
  'formatCapacityIntegerNumber(resource.hourlyCapacity)'
]) {
  assert(
    capacitySnapshotBlock.includes(requiredUsage),
    `产能快照必须使用整数展示函数：${requiredUsage}`
  )
}

assert(
  !capacitySnapshotBlock.includes('formatCapacityNumber(resource.hourlyCapacity)'),
  '产能快照资源标签不得继续显示小数产能尾差。'
)

const integerFormatter = extractConstFunction(pageSource, 'formatCapacityIntegerNumber')
assert(
  integerFormatter.includes('maximumFractionDigits: 0'),
  '排产工单产能整数展示函数必须最多显示 0 位小数。'
)
assert(
  !integerFormatter.includes('maximumFractionDigits: 6'),
  '排产工单产能整数展示函数不得继续沿用小数产能格式化精度。'
)

const hourlyCapacityColumn = pageSource.match(
  /prop="hourlyCapacityTotal"[\s\S]*?<\/el-table-column>/
)?.[0]
const shiftCapacityColumn = pageSource.match(
  /prop="shiftCapacityTotal"[\s\S]*?<\/el-table-column>/
)?.[0]
assert(hourlyCapacityColumn, '排产工单工序明细必须保留小时产能列。')
assert(shiftCapacityColumn, '排产工单工序明细必须保留班次产能列。')
assert(
  hourlyCapacityColumn.includes('formatCapacityIntegerNumber(row.hourlyCapacityTotal)') &&
    shiftCapacityColumn.includes('formatCapacityIntegerNumber(row.shiftCapacityTotal)'),
  '排产工单工序明细列表的小时产能和班次产能必须显示为整数。'
)
assert(
  !hourlyCapacityColumn.includes('formatCapacityNumber(row.hourlyCapacityTotal)') &&
    !shiftCapacityColumn.includes('formatCapacityNumber(row.shiftCapacityTotal)'),
  '排产工单工序明细列表产能列不得继续显示小数尾差。'
)

console.log('PASS: MES schedule order capacity integer static contract')
