const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const teamLeaderPagePath = 'src/views/mes/pro/edhr-batch/BatchTeamLeaderWorkbenchPage.vue'
const productionLeaderPagePath = 'src/views/mes/pro/edhr-batch/BatchProductionLeaderWorkbenchPage.vue'
const pqcLeaderPagePath = 'src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue'
const teamLeaderPath = 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
const pageGraphPath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

for (const requiredPath of [
  tabsPath,
  teamLeaderPagePath,
  productionLeaderPagePath,
  pqcLeaderPagePath,
  teamLeaderPath,
  pageGraphPath
]) {
  assert.ok(exists(requiredPath), `${requiredPath} must exist.`)
}

const tabs = read(tabsPath)
const teamLeaderPage = read(teamLeaderPagePath)
const productionLeaderPage = read(productionLeaderPagePath)
const pqcLeaderPage = read(pqcLeaderPagePath)
const teamLeader = read(teamLeaderPath)
const pageGraph = read(pageGraphPath)

for (const [label, key, route] of [
  ['生产组长', 'productionLeader', '/mes/pro/feedback/edhr-batch-production-leader'],
  ['PQC组长', 'pqcLeader', '/mes/pro/feedback/edhr-batch-pqc-leader']
]) {
  assert.match(tabs, new RegExp(label), `eDHR batch tabs must include ${label}.`)
  assert.match(tabs, new RegExp(`'${key}'`), `eDHR batch tab union must include ${key}.`)
  assert.match(tabs, new RegExp(`${key}:\\s*'${route.replace(/\//g, '\\/')}'`), `${label} tab must route to ${route}.`)
}

const routeBlockFor = (routePath) => {
  const routeIndex = router.indexOf(`path: '${routePath}'`)
  assert.ok(routeIndex >= 0, `route ${routePath} must exist.`)
  const nextRouteIndex = router.indexOf('\n      {', routeIndex + routePath.length)
  return router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)
}

const teamLeaderRouteBlock = routeBlockFor('pro/feedback/edhr-batch-team-leader')
const productionLeaderRouteBlock = routeBlockFor('pro/feedback/edhr-batch-production-leader')
const pqcLeaderRouteBlock = routeBlockFor('pro/feedback/edhr-batch-pqc-leader')

assert.match(teamLeaderRouteBlock, /BatchTeamLeaderWorkbenchPage\.vue/, 'group leader route must use the eDHR wrapper page.')
assert.match(teamLeaderRouteBlock, /title:\s*'组长工作台'/, 'group leader route title must remain visible.')
assert.match(
  productionLeaderRouteBlock,
  /BatchProductionLeaderWorkbenchPage\.vue[\s\S]*title:\s*'生产组长'/,
  'production leader route must use the dedicated eDHR wrapper page.'
)
assert.match(
  pqcLeaderRouteBlock,
  /BatchPqcLeaderWorkbenchPage\.vue[\s\S]*title:\s*'PQC组长'/,
  'PQC leader route must use the dedicated eDHR wrapper page.'
)

assert.match(teamLeaderPage, /<EdhrBatchRecordTabs\s+active-tab="teamLeader"/, 'group leader page must render shared tabs.')
assert.match(teamLeaderPage, /leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/, 'group leader page must stay production-scoped.')
assert.match(
  productionLeaderPage,
  /data-edhr-batch-production-leader-page[\s\S]*<EdhrBatchRecordTabs\s+active-tab="productionLeader"[\s\S]*leader-type="PRODUCTION"/,
  'production leader page must be a dedicated production wrapper.'
)
assert.match(
  pqcLeaderPage,
  /data-edhr-batch-pqc-leader-page[\s\S]*<EdhrBatchRecordTabs\s+active-tab="pqcLeader"[\s\S]*leader-type="PQC"/,
  'PQC leader page must be a dedicated PQC wrapper.'
)
assert.doesNotMatch(pqcLeaderPage, /leader-type="PRODUCTION"|生产组长/, 'PQC leader page must not point at production content.')

assert.match(teamLeader, /showLeaderTypeTabs[\s\S]*false/, 'formal workbench must default to hiding internal leader-type tabs.')
assert.match(teamLeader, /leaderType === 'PQC'[\s\S]*PQC_SIMPLIFIED/, 'formal workbench must keep PQC-specific query state.')

for (const [nodeId, label, route] of [
  ['production-lead-review', '生产组长', '/mes/pro/feedback/edhr-batch-production-leader'],
  ['pqc-lead-review', 'PQC组长', '/mes/pro/feedback/edhr-batch-pqc-leader']
]) {
  assert.match(
    pageGraph,
    new RegExp(`id:\\s*'${nodeId}'[\\s\\S]*title:\\s*'${label}'[\\s\\S]*route:\\s*'${route.replace(/\//g, '\\/')}'[\\s\\S]*isDisabled:\\s*false`),
    `page graph must expose the dedicated ${label} route.`
  )
}

console.log('PASS: eDHR batch dual leader tabs static contract')
