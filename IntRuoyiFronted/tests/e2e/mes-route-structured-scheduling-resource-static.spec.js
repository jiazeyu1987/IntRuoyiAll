const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')

const readText = (relativePath) => {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

const routeProcessListSource = readText('src/views/mes/pro/route/RouteProcessList.vue')
const routeProcessApiSource = readText('src/api/mes/pro/route/process/index.ts')

const requiredTableLabels = [
  'label="资源类型"',
  'label="标准资源"',
  'label="标准班次产能"',
  'label="资源状态"'
]

for (const label of requiredTableLabels) {
  if (!routeProcessListSource.includes(label)) {
    throw new Error(`组成工序表格缺少排产资源列：${label}`)
  }
}

const routeProcessMainTableSource = routeProcessListSource.slice(
  routeProcessListSource.indexOf('<el-table v-loading="loading"'),
  routeProcessListSource.indexOf('<Dialog :title="formTitle"')
)

for (const removedLabel of ['label="今日可用"', 'label="今日班次产能"']) {
  if (routeProcessMainTableSource.includes(removedLabel)) {
    throw new Error(`组成工序表格不应继续显示排产今日列：${removedLabel}`)
  }
}

const requiredComponentContracts = [
  'todayAvailableResourceQuantityTotal',
  'todayShiftCapacityTotal',
  'resourceStatus',
  'resourceStatusReason',
  'workerSingleStandardHourlyCapacity',
  'availableShiftCapacityTotal',
  'availabilityReason',
  'openProcessCapacityDetail'
]

for (const contract of requiredComponentContracts) {
  if (!routeProcessListSource.includes(contract)) {
    throw new Error(`工艺路线排产资源组件缺少契约：${contract}`)
  }
}

const requiredApiContracts = [
  'todayAvailableResourceQuantityTotal',
  'todayHourlyCapacityTotal',
  'todayShiftCapacityTotal',
  'resourceStatus',
  'resourceStatusReason',
  'workerSingleStandardHourlyCapacity',
  'availableQuantity',
  'availableShiftCapacityTotal',
  'availabilityStatus',
  'availabilityReason'
]

for (const contract of requiredApiContracts) {
  if (!routeProcessApiSource.includes(contract)) {
    throw new Error(`route-process API 类型缺少字段：${contract}`)
  }
}
