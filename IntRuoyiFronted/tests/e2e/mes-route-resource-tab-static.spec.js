const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeList = read('src/views/mes/pro/route/index.vue')
const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const routeProcessApi = read('src/api/mes/pro/route/process/index.ts')
const routeResourceTable = read('src/views/mes/pro/route/RouteResourceTable.vue')

assert.doesNotMatch(
  formContent,
  /import\s+RouteResourceTable\s+from\s+['"]\.\/RouteResourceTable\.vue['"]/,
  'RouteFormContent must not import a route-owned resource table'
)

assert.doesNotMatch(
  formContent,
  /<el-tab-pane\s+label=["']排产资源["']\s+name=["']resource["']\s+lazy>/,
  'route edit form must not expose a separate scheduling resource tab'
)

assert.doesNotMatch(
  formContent,
  /<RouteResourceTable/,
  'route edit form must not mount RouteResourceTable'
)

assert.doesNotMatch(
  formContent,
  /type\s+RouteFormInitialTab\s*=[\s\S]*\|\s*['"]resource['"]/,
  'RouteFormContent initial tab type must not include resource tab'
)

assert.doesNotMatch(
  editPage,
  /\['process',\s*'basic',\s*'flow',\s*'resource',\s*'product'\]\.includes\(tab\)/,
  'route edit page must not accept ?tab=resource'
)

assert.doesNotMatch(
  routeList,
  /type\s+RouteEditTab\s*=[\s\S]*\|\s*['"]resource['"]/,
  'route list edit navigation type must not include resource tab'
)

assert.doesNotMatch(
  routeList,
  /openEditPage\(scope\.row\.id,\s*['"]resource['"]\)/,
  'route list must not expose a route resource action'
)

assert.match(
  routeProcessList,
  /MdWorkstationSelect/,
  'route process editor must bind route process to workstation inside process settings'
)

assert.match(
  routeProcessList,
  /v-model=["']formData\.workstationId["']/,
  'route process form must save workstationId through the route-process payload'
)

assert.match(
  routeProcessList,
  /:process-id=["']formData\.processId["']/,
  'workstation selection must be filtered by the selected process'
)

assert.match(
  routeProcessApi,
  /workstationId\?:\s*number/,
  'route-process API type must expose workstationId'
)

assert.doesNotMatch(
  routeResourceTable,
  /ProRouteResourceApi\.saveResource|applyWorkbenchWorkerDefaults|v-model=["']row\.(singleStandardHourlyCapacity|workerQuantity|machineryQuantity|machineryStandardHourlyCapacity)["']/,
  'route resource table must be a read-only view derived from workstation resources'
)

assert.match(
  routeResourceTable,
  /维护请进入工作站详情/,
  'route resource table must guide users to workstation detail for resource maintenance'
)

console.log('mes-route-workstation-binding-static PASS')
