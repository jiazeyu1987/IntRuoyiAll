const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeMesProcessListPath = path.join(root, 'src/views/mes/pro/route/RouteMesProcessList.vue')

assert.match(
  formContent,
  /const RouteMesProcessList = defineAsyncComponent\(\(\) => import\('\.\/RouteMesProcessList\.vue'\)\)/,
  'RouteFormContent must lazy-load the MES process mapping list'
)

const basicIndex = formContent.indexOf('label="基础信息" name="basic"')
const mesProcessIndex = formContent.indexOf('label="MES 工序" name="mesProcess"')
const flowIndex = formContent.indexOf('label="流转关系图" name="flow"')
assert.ok(basicIndex >= 0, 'basic tab must remain visible')
assert.ok(mesProcessIndex >= 0, 'MES process tab must exist')
assert.ok(flowIndex >= 0, 'flow tab must remain visible')
assert.ok(
  basicIndex < mesProcessIndex && mesProcessIndex < flowIndex,
  'MES process tab must sit between basic/process settings and flow graph'
)

assert.match(
  formContent,
  /type RouteFormInitialTab =[\s\S]*\| 'basic'[\s\S]*\| 'mesProcess'[\s\S]*\| 'flow'[\s\S]*\| 'product'/,
  'RouteFormInitialTab must include mesProcess between basic and flow'
)
assert.match(
  formContent,
  /<RouteMesProcessList[\s\S]*:route-id="formData\.id"/,
  'MES process tab must pass the current route id to the mapping list'
)
assert.match(
  editPage,
  /\['basic', 'mesProcess', 'flow', 'product'\]\.includes\(tab\)/,
  'edit page must accept ?tab=mesProcess'
)
assert.match(
  editPage,
  /!\['flow', 'basic', 'mesProcess', 'product'\]\.includes\(activeRouteTab\)/,
  'MES process tab must not show the page-level save button'
)

assert.ok(fs.existsSync(routeMesProcessListPath), 'RouteMesProcessList component must exist')
const mesProcessList = fs.readFileSync(routeMesProcessListPath, 'utf8')

for (const columnLabel of [
  'MES 工序名称',
  '工序设置工序',
  '设备编码',
  '设备名称',
  '设备数量',
  '单台产能/h',
  '批记录工序名称'
]) {
  assert.ok(
    mesProcessList.includes(`label="${columnLabel}"`),
    `MES process mapping table must include ${columnLabel}`
  )
}

assert.match(
  mesProcessList,
  /ProRouteProcessApi\.getRouteProcessListByRoute\(props\.routeId\)/,
  'MES process mapping list must reuse route-process list-by-route API'
)
assert.match(
  mesProcessList,
  /machineryList/,
  'MES process mapping list must flatten route process machineryList'
)
assert.match(
  mesProcessList,
  /batchRecordReportName/,
  'MES process mapping list must expose the batch-record process/form name from route process data'
)

console.log('mes-route-mes-process-tab-static PASS')
