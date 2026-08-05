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
const processPoolProductionLeaderPagePath = 'src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue'
const processPoolPqcLeaderPagePath = 'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'
const teamLeaderPath = 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
const pageGraphPath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

for (const requiredPath of [
  tabsPath,
  teamLeaderPagePath,
  productionLeaderPagePath,
  pqcLeaderPagePath,
  processPoolProductionLeaderPagePath,
  processPoolPqcLeaderPagePath,
  teamLeaderPath,
  pageGraphPath
]) {
  assert.ok(exists(requiredPath), `${requiredPath} must exist.`)
}

const tabs = read(tabsPath)
const teamLeaderPage = read(teamLeaderPagePath)
const productionLeaderPage = read(productionLeaderPagePath)
const pqcLeaderPage = read(pqcLeaderPagePath)
const processPoolProductionLeaderPage = read(processPoolProductionLeaderPagePath)
const processPoolPqcLeaderPage = read(processPoolPqcLeaderPagePath)
const teamLeader = read(teamLeaderPath)
const pageGraph = read(pageGraphPath)

for (const [label, key, route] of [
  ['组长工作台', 'teamLeader', '/mes/pro/feedback/edhr-batch-team-leader'],
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
const processPoolProductionLeaderRouteBlock = routeBlockFor('pro/process-pool/production-leader')
const processPoolPqcLeaderRouteBlock = routeBlockFor('pro/process-pool/pqc-leader')

assert.match(teamLeaderRouteBlock, /BatchTeamLeaderWorkbenchPage\.vue/, 'group leader route must use the eDHR wrapper page.')
assert.match(teamLeaderRouteBlock, /title:\s*'组长工作台'/, 'group leader route title must remain visible.')
assert.match(
  productionLeaderRouteBlock,
  /BatchProductionLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProEdhrBatchProductionLeaderWorkbench'[\s\S]*title:\s*'生产组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/feedback\/edhr-batch-production-leader'/,
  'production leader route must use the dedicated eDHR wrapper page.'
)
assert.match(
  pqcLeaderRouteBlock,
  /BatchPqcLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProEdhrBatchPqcLeaderWorkbench'[\s\S]*title:\s*'PQC组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/feedback\/edhr-batch-pqc-leader'/,
  'PQC leader route must use the dedicated eDHR wrapper page.'
)
assert.match(
  processPoolProductionLeaderRouteBlock,
  /ProductionLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProProcessPoolProductionLeaderWorkbench'[\s\S]*title:\s*'生产组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/process-pool\/production-leader'/,
  'process-pool production leader route must remain available without replacing the eDHR tab.'
)
assert.match(
  processPoolPqcLeaderRouteBlock,
  /PqcLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProProcessPoolPqcLeaderWorkbench'[\s\S]*title:\s*'PQC组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/process-pool\/pqc-leader'/,
  'process-pool PQC leader route must remain available without replacing the eDHR tab.'
)

assert.match(teamLeaderPage, /<EdhrBatchRecordTabs\s+active-tab="teamLeader"/, 'group leader page must render shared tabs.')
assert.match(teamLeaderPage, /leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/, 'group leader page must no longer show production leader content.')
assert.doesNotMatch(teamLeaderPage, /leader-type="PRODUCTION"|生产组长/, 'group leader page must not point at production content.')
assert.match(
  productionLeaderPage,
  /data-edhr-batch-production-leader-page[\s\S]*<EdhrBatchRecordTabs\s+active-tab="productionLeader"[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/,
  'production leader page must be a dedicated production wrapper.'
)
assert.match(
  pqcLeaderPage,
  /data-edhr-batch-pqc-leader-page[\s\S]*<EdhrBatchRecordTabs\s+active-tab="pqcLeader"[\s\S]*leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/,
  'PQC leader page must be a dedicated PQC wrapper.'
)
assert.doesNotMatch(pqcLeaderPage, /leader-type="PRODUCTION"|生产组长/, 'PQC leader page must not point at production content.')
assert.match(
  processPoolProductionLeaderPage,
  /data-production-leader-workbench-page[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/,
  'process-pool production leader page must lock the shared workbench to PRODUCTION.'
)
assert.match(
  processPoolPqcLeaderPage,
  /data-pqc-leader-workbench-page[\s\S]*leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/,
  'process-pool PQC leader page must lock the shared workbench to PQC.'
)
assert.doesNotMatch(
  `${processPoolProductionLeaderPage}\n${processPoolPqcLeaderPage}`,
  /EdhrBatchRecordTabs|active-tab=/,
  'process-pool leader pages must not render eDHR internal tabs.'
)

assert.match(teamLeader, /leaderType:\s*'PRODUCTION'/, 'shared workbench must retain PRODUCTION as its default type.')
assert.match(teamLeader, /leaderType === 'PQC'[\s\S]*PQC_SIMPLIFIED/, 'shared workbench must retain formal PQC query state.')

for (const [nodeId, label, route] of [
  ['team-lead-review', '组长工作台', '/mes/pro/feedback/edhr-batch-team-leader'],
  ['production-lead-review', '生产组长', '/mes/pro/feedback/edhr-batch-production-leader'],
  ['pqc-lead-review', 'PQC组长', '/mes/pro/feedback/edhr-batch-pqc-leader']
]) {
  assert.match(
    pageGraph,
    new RegExp(
      `id:\\s*'${nodeId}'[\\s\\S]*title:\\s*'${label}'[\\s\\S]*route:\\s*'${route.replace(/\//g, '\\/')}'[\\s\\S]*isDisabled:\\s*false`
    ),
    `page graph must expose the eDHR ${label} route.`
  )
}
assert.doesNotMatch(
  pageGraph,
  /route:\s*'\/mes\/pro\/process-pool\/(?:production|pqc)-leader'/,
  'page graph must not replace eDHR leader tabs with process-pool standalone routes.'
)

console.log('PASS: eDHR production/PQC leader tab contract')
