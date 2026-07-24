const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'), 'utf8')

if (source.includes('MACHINERY_CAPACITY_SHIFT_HOURS = 10.5')) {
  throw new Error('设备列表不得继续使用固定 10.5 小时默认班次产能')
}

if (!source.includes('未配置班次小时')) {
  throw new Error('设备列表缺少班次小时未配置提示')
}

if (!source.includes('label="单台产能/h"')) {
  throw new Error('设备列表缺少单台产能/h 列')
}

if (!source.includes('label="单台产能/班次"')) {
  throw new Error('设备列表缺少单台产能/班次列')
}

if (/el-table-column\s+label="总产能\/h"/.test(source)) {
  throw new Error('总产能/h 不应作为行级表格列展示，应移到底部汇总')
}

if (!source.includes('machineryCapacitySummary')) {
  throw new Error('设备列表缺少底部产能汇总计算')
}

if (!source.includes('总产能/h')) {
  throw new Error('底部缺少总产能/h 文案')
}

if (!source.includes('总产能/班次')) {
  throw new Error('底部缺少总产能/班次文案')
}

if (!source.includes('getShiftHourLabel')) {
  throw new Error('底部缺少班次小时说明函数')
}

if (!source.includes('getMachineryShiftCapacity')) {
  throw new Error('设备列表缺少单台班次产能计算函数')
}

console.log('PASS: route process machinery capacity summary contract is satisfied')
