const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const editPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeList = read('src/views/mes/pro/route/index.vue')

assert.doesNotMatch(
  formContent,
  /const RouteMesProcessList = defineAsyncComponent\(\(\) => import\('\.\/RouteMesProcessList\.vue'\)\)/,
  'RouteFormContent must not lazy-load the hidden MES process mapping list'
)

const basicIndex = formContent.indexOf('label="基础信息" name="basic"')
const mesProcessIndex = formContent.indexOf('label="MES 工序" name="mesProcess"')
const flowIndex = formContent.indexOf('label="流转关系图" name="flow"')
const productIndex = formContent.indexOf('label="关联产品" name="product"')
assert.ok(basicIndex >= 0, 'basic tab must remain visible')
assert.equal(mesProcessIndex, -1, 'MES process tab must not render in route form')
assert.ok(flowIndex >= 0, 'flow tab must remain visible')
assert.ok(productIndex >= 0, 'product tab must remain visible')
assert.ok(
  basicIndex < flowIndex && flowIndex < productIndex,
  'route tabs must keep basic -> flow graph -> product after hiding MES process'
)

assert.match(
  formContent,
  /type RouteFormInitialTab =[\s\S]*\| 'basic'[\s\S]*\| 'flow'[\s\S]*\| 'product'/,
  'RouteFormInitialTab must only include the visible tabs'
)
assert.doesNotMatch(
  formContent,
  /<RouteMesProcessList/,
  'hidden MES process tab must not mount RouteMesProcessList'
)
assert.doesNotMatch(
  editPage,
  /\['basic', 'mesProcess', 'flow', 'product'\]\.includes\(tab\)/,
  'edit page must not accept legacy ?tab=mesProcess'
)
assert.doesNotMatch(
  editPage,
  /!\['flow', 'basic', 'mesProcess', 'product'\]\.includes\(activeRouteTab\)/,
  'edit page page-level save guard must not whitelist hidden mesProcess tab'
)
assert.doesNotMatch(
  routeList,
  /type RouteEditTab = 'basic' \| 'mesProcess' \| 'flow' \| 'product'/,
  'route list edit navigation type must not include hidden mesProcess tab'
)

console.log('mes-route-hide-mes-process-tab-static PASS')
