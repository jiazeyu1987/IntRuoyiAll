const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)

const mainTableSource = source.slice(
  source.indexOf('<el-table'),
  source.indexOf('<Dialog :title="formTitle"')
)

if (!mainTableSource.includes('isTodayResourceLower(scope.row)')) {
  throw new Error('标准资源列缺少今日资源短缺红色样式判断。')
}

if (!mainTableSource.includes('isTodayCapacityLower(scope.row)')) {
  throw new Error('标准班次产能列缺少班次产能短缺红色样式判断。')
}

if (!mainTableSource.includes('getStandardShiftCapacityLabel(scope.row)')) {
  throw new Error('标准班次产能列必须使用可显示 今日/标准 的格式化方法。')
}

for (const contract of [
  'const isTodayResourceLower',
  'const getStandardResourceQuantity',
  'const getTodayAvailableResourceQuantity',
  'const getShortageRatioLabel',
  'const getStandardShiftCapacityLabel',
  'capacity-shortage'
]) {
  if (!source.includes(contract)) {
    throw new Error(`短缺比值展示缺少契约：${contract}`)
  }
}

if (!source.includes('todayAvailableResourceQuantityTotal')) {
  throw new Error('短缺比值必须使用今日可用资源字段。')
}

if (!source.includes('todayShiftCapacityTotal')) {
  throw new Error('短缺比值必须使用今日班次产能字段。')
}
