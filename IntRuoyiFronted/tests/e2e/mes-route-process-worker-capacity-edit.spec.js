const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..', '..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeProcessListSource = readText('src/views/mes/pro/route/RouteProcessList.vue')
const processApiSource = readText('src/api/mes/pro/route/process/index.ts')
const resourceApiSource = readText('src/api/mes/pro/route/resource.ts')
const legacyWorkerCapacitySubmit = ['submit', 'WorkerCapacity'].join('')

const forbiddenRouteProcessFragments = [
  'openWorkerCapacityEditor(scope.row)',
  'workerCapacityDialogVisible',
  'workerCapacityForm',
  'workerShiftCapacityTotal',
  legacyWorkerCapacitySubmit,
  'ProRouteResourceApi.saveResource',
  "resourceType: 'WORKER'",
  '班次总产能',
  '单人产能/h'
]

for (const fragment of forbiddenRouteProcessFragments) {
  if (routeProcessListSource.includes(fragment)) {
    throw new Error(`路线工序页面不应继续维护人工资源能力：${fragment}`)
  }
}

if (!routeProcessListSource.includes('MdWorkstationSelect')) {
  throw new Error('路线工序编辑必须提供工作站选择器。')
}

if (!routeProcessListSource.includes('openWorkstationDetail')) {
  throw new Error('路线工序列表必须允许从绑定工作站进入工作站资源设置。')
}

for (const apiFragment of [
  'workstationId?: number',
  'workstationWorkerId?: number',
  'workerSingleStandardHourlyCapacity?: number',
  'shiftHours?: number'
]) {
  if (!processApiSource.includes(apiFragment)) {
    throw new Error(`路线工序 API 类型缺少人工产能字段：${apiFragment}`)
  }
}

for (const apiFragment of ['ProRouteResourceSaveVO', 'saveResource', 'route-resource/save']) {
  if (resourceApiSource.includes(apiFragment)) {
    throw new Error(`资源 API 不应保留路线级资源保存入口：${apiFragment}`)
  }
}

console.log('mes-route-process-workstation-binding-static PASS')
