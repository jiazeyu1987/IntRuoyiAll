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
assert.match(page, /工序开始/, 'page graph must expose route start as a separate configuration entry.')
assert.match(page, /批记录表单/, 'page graph must expose formal batch-record forms as a separate configuration entry.')
assert.match(page, /表单槽位/, 'page graph must expose form slots as a separate configuration entry.')
assert.match(
  page,
  /工序开始[\s\S]*不提供[\s\S]*表单内容/,
  'route start must be described as an action/attachment owner entry, not a form content source.'
)
assert.match(
  page,
  /批记录表单[\s\S]*逐工序[\s\S]*正式绑定/,
  'formal batch-record forms must be sourced from per-process formal bindings.'
)
assert.match(
  page,
  /表单槽位[\s\S]*formBindings[\s\S]*不得替代正式批记录/,
  'form slots must be tied to formBindings and explicitly forbidden from replacing formal batch records.'
)
assert.match(page, /待接入/, 'page must visibly mark unavailable nodes as pending.')
assert.match(page, /isDisabled/, 'page must model unavailable nodes as disabled, not fake routes.')
assert.match(page, /edhr-page-graph-page__canvas/, 'page graph must use a flow-canvas container, not grouped cards.')
assert.match(page, /from '@vue-flow\/core'/, 'page graph must reuse the existing VueFlow graph dependency.')
assert.match(page, /<VueFlow/, 'page graph must render through VueFlow, matching MES route flow graph style.')
assert.match(page, /edhr-page-graph-page__flow/, 'page graph must expose a VueFlow canvas class.')
assert.match(page, /type:\s*'smoothstep'/, 'page graph edges must use smooth flow connectors.')
assert.match(page, /MarkerType\.ArrowClosed/, 'page graph connectors must show arrow direction.')
assert.match(page, /toFlowNode\(node\)/, 'page graph nodes must be positioned as flow nodes.')
assert.match(page, /:pan-on-drag="false"/, 'read-only page graph must not let the VueFlow pane intercept node clicks.')
assert.match(page, /vue-flow__pane[\s\S]*pointer-events:\s*none/, 'VueFlow pane must not block page-node click navigation.')
assert.match(page, /vue-flow__nodes[\s\S]*pointer-events:\s*none/, 'VueFlow nodes container must not block page-node click navigation.')
assert.match(page, /vue-flow__node \*\)[\s\S]*pointer-events:\s*auto/, 'VueFlow node contents must remain clickable.')
assert.match(page, /edhr-page-graph-page__edge-registry/, 'page graph must retain stable edge selectors for E2E evidence.')
assert.doesNotMatch(page, /edhr-page-graph-page__connections/, 'page graph must not use the old hand-drawn SVG connector layer.')
assert.doesNotMatch(page, /data-edhr-page-edge-path/, 'page graph edges must come from VueFlow, not custom SVG path selectors.')
assert.doesNotMatch(page, /resolveEdgePath/, 'page graph must not keep the legacy manual edge path builder.')
assert.doesNotMatch(page, /edhr-page-graph-page__group/, 'page graph must not render the visual relationship as grouped card columns.')

for (const nodeName of [
  '工序开始',
  '批记录表单',
  '表单槽位',
  '生产工单',
  '生产填写',
  'PQC填写',
  '工序池',
  '班组长复核',
  'PQC组长',
  'FIFO分配',
  'EDHR审核副本',
  '正式批记录',
  '归档'
]) {
  assert.match(page, new RegExp(nodeName), `page graph must include node ${nodeName}.`)
}

assert.doesNotMatch(
  page,
  /title:\s*'MES工序\/班组设置'/,
  'page graph must not collapse route start, batch-record form, and form-slot responsibilities into one generic MES settings node.'
)

for (const edgeLabel of [
  '仅决定开始节点动作和附件责任',
  '按工序设置逐工序正式绑定',
  '仅提供补充表单槽位'
]) {
  assert.match(page, new RegExp(edgeLabel), `page graph must include separated edge label ${edgeLabel}.`)
}

for (const route of [
  '/mes/pro/feedback/edhr-batch-execution',
  '/mes/pro/feedback/edhr-batch-production-fill',
  '/mes/pro/feedback/edhr-batch-pqc-fill',
  '/mes/pro/feedback/edhr-batch-pqc-leader',
  '/mes/pro/process-pool/review-copy'
]) {
  assert.match(page, new RegExp(route.replace(/\//g, '\\/')), `page graph must include official route ${route}.`)
}

console.log('PASS: eDHR batch page graph tab static contract')
