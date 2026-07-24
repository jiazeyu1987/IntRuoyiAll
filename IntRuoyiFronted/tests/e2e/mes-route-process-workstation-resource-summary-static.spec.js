const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const workstationSelectDialog = read(
  'src/views/mes/md/workstation/components/MdWorkstationSelectDialog.vue'
)
const workstationSelect = read('src/views/mes/md/workstation/components/MdWorkstationSelect.vue')
const workstationApi = read('src/api/mes/md/workstation/index.ts')
const routeProcessApi = read('src/api/mes/pro/route/process/index.ts')
const packageJson = JSON.parse(read('package.json'))

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

assert.strictEqual(
  packageJson.scripts?.['e2e:mes:route-process-workstation-resource-summary:static'],
  'node tests/e2e/mes-route-process-workstation-resource-summary-static.spec.js',
  'package.json must expose workstation resource summary static contract'
)
assertIncludes(
  packageJson.scripts?.['ts:check'] || '',
  'NODE_OPTIONS=--max-old-space-size=8192',
  'full TypeScript check must have enough heap for the full project'
)

assertIncludes(routeProcessList, 'MdWorkstationMachineApi', 'route process form must load bound workstation equipment')
assertIncludes(routeFormContent, '<el-tab-pane label="组成工序" name="process" lazy>', 'route process list must be mounted as a visible route tab')
assertIncludes(routeFormContent, '<RouteProcessList', 'route process list component must be used by the route form')
assertIncludes(routeEditPage, "['basic', 'flow', 'process', 'product']", 'route edit page must allow direct navigation to the process tab')
assertIncludes(routeProcessList, '@change="handleWorkstationChange"', 'workstation selection must return the full workstation row')
assertIncludes(routeProcessList, 'loadWorkstationResourceSummary', 'route process form must load workstation resource summary')
assertIncludes(routeProcessList, 'selectedWorkstationResource.value = selectedWorkstation', 'selected workstation row must be shown immediately before equipment list finishes loading')
assertIncludes(routeProcessList, 'Promise.resolve(selectedWorkstation)', 'selected workstation row must not be refetched before showing the summary')
assertIncludes(routeProcessList, 'workstationResourceLoading', 'resource summary must expose loading state')
assertIncludes(routeProcessList, 'workstationResourceError', 'resource loading failures must be visible')
assertIncludes(routeProcessList, 'data-testid="route-process-workstation-resource-summary"', 'resource summary must have a stable UI marker')
assertIncludes(routeProcessList, 'getWorkstationMachineList', 'resource summary must use workstation-machine list API')
assertIncludes(routeProcessList, 'formatWorkstationMachineSummary', 'resource summary must display bound equipment names')
assertIncludes(routeProcessList, 'formatWorkstationStandardShiftCapacity', 'resource summary must display standard shift capacity')
assertIncludes(routeProcessList, 'formatWorkstationTodayShiftCapacity', 'resource summary must display today shift capacity')
assertIncludes(routeProcessList, 'if (workstationResourceLoading.value || workstationResourceError.value)', 'submit must be blocked while resource summary is missing or failed')
assertIncludes(routeProcessList, '工作站资源加载失败', 'resource loading failure must be explained to the user')
assertNotIncludes(routeProcessList, 'catch {}', 'route process form must not silently swallow errors')
assertNotIncludes(routeProcessList, 'v-model="formData.capacitySource"', 'route process form must not save capacity source as an input')
assertNotIncludes(routeProcessList, 'v-model="formData.standardResource"', 'route process form must not save standard resource as an input')
assertNotIncludes(routeProcessList, 'v-model="formData.processShiftCapacityTotal"', 'route process form must not save standard shift capacity as an input')

assertIncludes(workstationSelectDialog, 'label="班次小时"', 'workstation dialog must show shift hours')
assertIncludes(workstationSelectDialog, 'label="班次产能"', 'workstation dialog must show shift capacity')
assertIncludes(workstationSelectDialog, 'label="设备标准小时产能"', 'workstation dialog must show equipment hourly capacity')
assertIncludes(workstationSelectDialog, 'formatWorkstationCapacity', 'workstation dialog must format capacity fields')
assertIncludes(workstationSelect, 'change: [item: MdWorkstationVO | undefined]', 'workstation select must emit the full selected row')
assertIncludes(workstationApi, 'singleStandardHourlyCapacity?: number', 'workstation type must define worker hourly capacity')
assertIncludes(workstationApi, 'shiftHours?: number', 'workstation type must define shift hours')
assertIncludes(workstationApi, 'configuredWorkerCount?: number', 'workstation type must define configured worker count')
assertIncludes(workstationApi, 'machineryStandardHourlyCapacity?: number', 'workstation type must define machinery hourly capacity')
assertIncludes(workstationApi, 'todayCapacity?: number', 'workstation type must define today shift capacity')
assertIncludes(routeProcessApi, 'workerSingleStandardHourlyCapacity?: number', 'route process type must define worker hourly capacity')
assertIncludes(routeProcessApi, 'todayAvailableResourceQuantityTotal?: number', 'route process type must define today resource quantity')
assertIncludes(routeProcessApi, 'todayHourlyCapacityTotal?: number', 'route process type must define today hourly capacity')
assertIncludes(routeProcessApi, 'todayShiftCapacityTotal?: number', 'route process type must define today shift capacity')

console.log('PASS route process workstation resource summary static contract')
