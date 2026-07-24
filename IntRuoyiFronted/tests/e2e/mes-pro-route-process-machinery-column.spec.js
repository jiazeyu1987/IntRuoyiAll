const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeProcessListSource = readText('src/views/mes/pro/route/RouteProcessList.vue')
const routeProcessApiSource = readText('src/api/mes/pro/route/process/index.ts')

if (routeProcessListSource.includes('label="下一道工序"')) {
  throw new Error('工艺路线详情组成工序表格不应继续展示“下一道工序”列')
}

if (!routeProcessListSource.includes('label="标准资源"')) {
  throw new Error('工艺路线详情组成工序表格缺少“标准资源”列')
}

if (!routeProcessListSource.includes('machineryQuantityTotal')) {
  throw new Error('设备列应使用后端返回的 machineryQuantityTotal')
}

if (!routeProcessListSource.includes('machineryListDialogVisible')) {
  throw new Error('点击设备数量应打开设备列表弹窗')
}

if (!routeProcessListSource.includes('openProcessCapacityDetail')) {
  throw new Error('缺少打开工序排产资源详情的交互方法')
}

const mainTableSource = routeProcessListSource.slice(
  routeProcessListSource.indexOf('<el-table v-loading="loading"'),
  routeProcessListSource.indexOf('<Dialog :title="formTitle"')
)

if (mainTableSource.includes('label="今日可用"')) {
  throw new Error('工艺路线详情组成工序表格不应继续展示“今日可用”列')
}

if (!routeProcessListSource.includes('MachineryForm')) {
  throw new Error('设备详情应复用现有 MachineryForm 详情弹窗')
}

if (!routeProcessListSource.includes("machineryFormRef.value?.open('detail'")) {
  throw new Error('点击设备编码应打开对应设备详情')
}

if (!routeProcessApiSource.includes('machineryQuantityTotal')) {
  throw new Error('route-process API 类型缺少 machineryQuantityTotal')
}

if (!routeProcessApiSource.includes('machineryList')) {
  throw new Error('route-process API 类型缺少 machineryList')
}

console.log('PASS: route process machinery column contract is satisfied')
