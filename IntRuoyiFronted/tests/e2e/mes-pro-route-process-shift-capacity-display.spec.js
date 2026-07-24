const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const routeProcessListSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)
const routeProcessApiSource = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/route/process/index.ts'),
  'utf8'
)
const legacyWorkerCapacitySubmit = ['submit', 'WorkerCapacity'].join('')

if (/el-table-column\s+label="准备时间"/.test(routeProcessListSource)) {
  throw new Error('组成工序表格不应继续显示准备时间列')
}

if (!routeProcessListSource.includes('label="标准班次产能"')) {
  throw new Error('组成工序表格缺少标准班次产能列')
}

if (!routeProcessListSource.includes('@click="openProcessCapacityDetail(scope.row)"')) {
  throw new Error('设备列必须统一点击进入工序产能详情')
}

if (!routeProcessListSource.includes('getStandardResourceLabel(scope.row)')) {
  throw new Error('标准资源列必须通过统一方法显示设备或人工资源')
}

if (routeProcessListSource.includes('openWorkerCapacityEditor(scope.row)') ||
  routeProcessListSource.includes('const openWorkerCapacityEditor')) {
  throw new Error('路线执行工序不得继续打开独立人工产能编辑区')
}

for (const token of [
  'workerCapacityForm',
  'workerCapacityDialogVisible',
  legacyWorkerCapacitySubmit,
  'v-model="workerCapacityForm.shiftHours"',
  'ProRouteResourceApi.saveResource'
]) {
  if (routeProcessListSource.includes(token)) {
    throw new Error(`路线执行工序不得继续保留独立人工产能维护入口: ${token}`)
  }
}

if (!routeProcessListSource.includes("row.capacitySource === 'WORKER'") ||
  !routeProcessListSource.includes('openWorkstationDetail(row)')) {
  throw new Error('人工资源详情必须跳转到工作站详情维护')
}
if (!routeProcessListSource.includes("row.capacitySource === 'WORKER'") ||
  !routeProcessListSource.includes('人工资源详情缺少工作站编号')) {
  throw new Error('人工资源详情缺少工作站绑定时必须失败并暴露错误')
}

const mainTableSource = routeProcessListSource.slice(
  routeProcessListSource.indexOf('<el-table v-loading="loading"'),
  routeProcessListSource.indexOf('<Dialog :title="formTitle"')
)

if (mainTableSource.includes('label="今日班次产能"')) {
  throw new Error('组成工序表格不应继续显示今日班次产能列')
}

if (!routeProcessListSource.includes("capacitySource === 'WORKER'") &&
  !routeProcessListSource.includes("capacitySource === 'WORKER'")) {
  throw new Error('人工资源入口必须按 WORKER 产能来源分流')
}

for (const token of [
  'processHourlyCapacityTotal?: number',
  'processShiftCapacityTotal?: number',
  'workerQuantityTotal?: number',
  'workstationWorkerId?: number',
  'shiftHours?: number',
  "capacitySource?: 'MACHINE' | 'WORKER' | 'UNCONFIGURED'"
]) {
  if (!routeProcessApiSource.includes(token)) {
    throw new Error(`工艺路线工序 API 类型缺少 ${token}`)
  }
}

console.log('PASS: route process shift capacity display contract is satisfied')
