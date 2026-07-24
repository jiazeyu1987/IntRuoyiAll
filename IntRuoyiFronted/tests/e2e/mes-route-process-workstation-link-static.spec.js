const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeProcessList = read('src/views/mes/pro/route/RouteProcessList.vue')
const workstationPage = read('src/views/mes/md/workstation/index.vue')

const assertIncludes = (source, expected, label) => {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

const assertNotIncludes = (source, unexpected, label) => {
  assert.ok(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

assertIncludes(routeProcessList, 'const router = useRouter()', 'route process list router access')
assertIncludes(
  routeProcessList,
  '@click="openWorkstationDetail(scope.row)"',
  'workstation tag click passes the route process row'
)
assertIncludes(
  routeProcessList,
  "path: '/mes/md/workstation'",
  'workstation click navigates to workstation settings page'
)
assertIncludes(routeProcessList, 'code: workstationCode', 'workstation click carries exact code filter')
assertIncludes(
  routeProcessList,
  '...buildWorkstationProcessQuery(row.processId)',
  'workstation click carries only positive process filter'
)
assertNotIncludes(
  routeProcessList,
  'processId: String(row.processId)',
  'workstation click must not serialize raw process filter'
)
assertIncludes(
  routeProcessList,
  '工作站跳转缺少工作站编码',
  'workstation click fails fast when code is missing'
)
assertNotIncludes(
  routeProcessList,
  "workstationFormRef.value?.open('detail', workstationId)",
  'workstation click must not open an in-page detail dialog'
)

assertIncludes(workstationPage, 'const route = useRoute()', 'workstation page reads route query')
assertIncludes(workstationPage, 'route.query.code', 'workstation page supports code query filter')
assertIncludes(workstationPage, 'queryParams.processId', 'workstation page supports process query filter')
assertIncludes(workstationPage, 'watch(', 'workstation page watches route query changes')

console.log('PASS route process workstation link static contract')
