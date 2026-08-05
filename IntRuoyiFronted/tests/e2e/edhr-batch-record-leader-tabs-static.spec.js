const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const productionLeaderPagePath = 'src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue'
const pqcLeaderPagePath = 'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'
const teamLeaderPath = 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
const pageGraphPath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

for (const requiredPath of [
  tabsPath,
  productionLeaderPagePath,
  pqcLeaderPagePath,
  teamLeaderPath,
  pageGraphPath
]) {
  assert.ok(exists(requiredPath), `${requiredPath} must exist.`)
}

for (const obsoletePath of [
  'src/views/mes/pro/edhr-batch/BatchTeamLeaderWorkbenchPage.vue',
  'src/views/mes/pro/edhr-batch/BatchProductionLeaderWorkbenchPage.vue',
  'src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue'
]) {
  assert.equal(exists(obsoletePath), false, `${obsoletePath} must be removed after the standalone menu split.`)
}

const tabs = read(tabsPath)
const productionLeaderPage = read(productionLeaderPagePath)
const pqcLeaderPage = read(pqcLeaderPagePath)
const teamLeader = read(teamLeaderPath)
const pageGraph = read(pageGraphPath)

for (const forbiddenTab of ['组长工作台', '生产组长', 'PQC组长', "'teamLeader'", "'productionLeader'", "'pqcLeader'"]) {
  assert.doesNotMatch(tabs, new RegExp(forbiddenTab), `eDHR batch tabs must not include ${forbiddenTab}.`)
}
assert.doesNotMatch(
  tabs,
  /edhr-batch-team-leader|edhr-batch-production-leader|edhr-batch-pqc-leader/,
  'eDHR batch tabs must not route to any leader workbench.'
)

const routeBlockFor = (routePath) => {
  const routeIndex = router.indexOf(`path: '${routePath}'`)
  assert.ok(routeIndex >= 0, `route ${routePath} must exist.`)
  const nextRouteIndex = router.indexOf('\n      {', routeIndex + routePath.length)
  return router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)
}

const productionLeaderRouteBlock = routeBlockFor('pro/process-pool/production-leader')
const pqcLeaderRouteBlock = routeBlockFor('pro/process-pool/pqc-leader')

assert.doesNotMatch(
  router,
  /pro\/feedback\/edhr-batch-(?:team-leader|production-leader|pqc-leader)|Batch(?:Team|Production|Pqc)LeaderWorkbenchPage\.vue/,
  'remaining routes must not keep the old eDHR internal leader routes.'
)
assert.match(
  productionLeaderRouteBlock,
  /ProductionLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProProcessPoolProductionLeaderWorkbench'[\s\S]*title:\s*'生产组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/process-pool\/production-leader'/,
  'production leader must use a standalone process-pool route and page.'
)
assert.match(
  pqcLeaderRouteBlock,
  /PqcLeaderWorkbenchPage\.vue[\s\S]*name:\s*'MesProProcessPoolPqcLeaderWorkbench'[\s\S]*title:\s*'PQC组长'[\s\S]*activeMenu:\s*'\/mes\/pro\/process-pool\/pqc-leader'/,
  'PQC leader must use a standalone process-pool route and page.'
)

assert.match(
  productionLeaderPage,
  /data-production-leader-workbench-page[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/,
  'production leader standalone page must lock the shared workbench to PRODUCTION.'
)
assert.match(
  pqcLeaderPage,
  /data-pqc-leader-workbench-page[\s\S]*leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/,
  'PQC leader standalone page must lock the shared workbench to PQC.'
)
assert.doesNotMatch(
  `${productionLeaderPage}\n${pqcLeaderPage}`,
  /EdhrBatchRecordTabs|active-tab=/,
  'standalone leader pages must not render eDHR internal tabs.'
)

assert.match(teamLeader, /leaderType:\s*'PRODUCTION'/, 'shared workbench must retain PRODUCTION as its default type.')
assert.match(teamLeader, /leaderType === 'PQC'[\s\S]*PQC_SIMPLIFIED/, 'shared workbench must retain formal PQC query state.')

for (const [nodeId, label, route] of [
  ['production-lead-review', '生产组长', '/mes/pro/process-pool/production-leader'],
  ['pqc-lead-review', 'PQC组长', '/mes/pro/process-pool/pqc-leader']
]) {
  assert.match(
    pageGraph,
    new RegExp(
      `id:\\s*'${nodeId}'[\\s\\S]*title:\\s*'${label}'[\\s\\S]*route:\\s*'${route.replace(/\//g, '\\/')}'[\\s\\S]*isDisabled:\\s*false`
    ),
    `page graph must expose the standalone ${label} route.`
  )
}
assert.doesNotMatch(pageGraph, /id:\s*'team-lead-review'|edhr-batch-(?:team-leader|production-leader|pqc-leader)/)

console.log('PASS: standalone production/PQC leader menu contract')
