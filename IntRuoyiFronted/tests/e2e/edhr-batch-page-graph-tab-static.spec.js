const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const pagePath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

assert.ok(exists(tabsPath), `${tabsPath} must exist.`)
assert.ok(exists(pagePath), `${pagePath} must exist.`)

const tabs = read(tabsPath)
const page = read(pagePath)

assert.match(tabs, /批记录页面关系图/, 'eDHR batch tabs must include page graph tab label.')
assert.match(tabs, /'pageGraph'/, 'eDHR batch tab union must include pageGraph key.')
assert.match(tabs, /@tab-click="handleTabClick"/, 'eDHR batch tabs must navigate from the real Element Plus tab click event.')
assert.doesNotMatch(tabs, /@tab-change="handleTabChange"/, 'eDHR batch tabs must not depend only on tab-change for navigation.')
assert.match(
  tabs,
  /pageGraph:\s*'\/mes\/pro\/feedback\/edhr-batch-page-graph'/,
  'page graph tab must map to the stable page graph route.'
)

const routePath = "path: 'pro/feedback/edhr-batch-page-graph'"
const routeIndex = router.indexOf(routePath)
assert.ok(routeIndex >= 0, 'page graph route must exist.')
const nextRouteIndex = router.indexOf('\n      {', routeIndex + routePath.length)
const routeBlock = router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)

assert.match(routeBlock, /BatchPageGraphPage\.vue/, 'page graph route must use BatchPageGraphPage.')
assert.match(routeBlock, /name:\s*'MesProEdhrBatchPageGraph'/, 'page graph route name must be stable.')
assert.match(routeBlock, /title:\s*'批记录页面关系图'/, 'page graph route title must be visible.')
assert.match(routeBlock, /permission:\s*\['mes:pro-edhr-batch-execution:query'\]/, 'page graph route must reuse eDHR batch permission.')

assert.match(page, /<EdhrBatchRecordTabs\s+active-tab="pageGraph"/, 'page graph page must render shared tabs.')
assert.match(page, /data-edhr-page-graph/, 'page graph page must expose a stable graph selector.')
assert.match(page, /data-edhr-page-node/, 'page graph page must expose stable node selectors.')
assert.match(page, /data-edhr-page-edge/, 'page graph page must expose stable edge selectors.')
assert.match(page, /页面关系图/, 'page must describe itself as a page relationship graph.')
assert.match(page, /不是工艺路线流转关系图/, 'page must distinguish itself from route process flow graph.')
assert.match(page, /待接入/, 'page must visibly mark unavailable nodes as pending.')
assert.match(page, /isDisabled/, 'page must model unavailable nodes as disabled, not fake routes.')

for (const nodeName of [
  '生产工单',
  '生产填写',
  'PQC填写',
  '工序池',
  '班组长复核',
  'FIFO分配',
  'EDHR审核副本',
  '正式批记录',
  '归档',
  'MES工序/班组设置'
]) {
  assert.match(page, new RegExp(nodeName), `page graph must include node ${nodeName}.`)
}

for (const route of [
  '/mes/pro/feedback/edhr-batch-execution',
  '/mes/pro/feedback/edhr-batch-production-fill',
  '/mes/pro/feedback/edhr-batch-pqc-fill',
  '/mes/pro/process-pool/review-copy'
]) {
  assert.match(page, new RegExp(route.replace(/\//g, '\\/')), `page graph must include official route ${route}.`)
}

console.log('PASS: eDHR batch page graph tab static contract')
