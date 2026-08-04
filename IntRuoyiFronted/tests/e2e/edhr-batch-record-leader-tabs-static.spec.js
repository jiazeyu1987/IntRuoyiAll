const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const pagePath = 'src/views/mes/pro/edhr-batch/BatchTeamLeaderWorkbenchPage.vue'
const pqcPagePath = 'src/views/mes/pro/edhr-batch/BatchPqcLeaderWorkbenchPage.vue'
const teamLeaderPath = 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
const pageGraphPath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

assert.ok(exists(tabsPath), `${tabsPath} must exist.`)
assert.ok(exists(teamLeaderPath), `${teamLeaderPath} must exist.`)
assert.ok(exists(pagePath), `${pagePath} must exist.`)
assert.ok(exists(pqcPagePath), `${pqcPagePath} must exist.`)

const tabs = read(tabsPath)
const page = read(pagePath)
const pqcPage = read(pqcPagePath)
const teamLeader = read(teamLeaderPath)
const pageGraph = read(pageGraphPath)

assert.match(tabs, /组长工作台/, 'eDHR batch tabs must include a leader workbench sub-tab.')
assert.match(tabs, /PQC组长/, 'eDHR batch tabs must include a dedicated PQC leader sub-tab.')
assert.match(tabs, /'teamLeader'/, 'eDHR batch tab union must include the teamLeader key.')
assert.match(tabs, /'pqcLeader'/, 'eDHR batch tab union must include the pqcLeader key.')
assert.match(
  tabs,
  /teamLeader:\s*'\/mes\/pro\/feedback\/edhr-batch-team-leader'/,
  'leader workbench tab must map to the eDHR batch leader route.'
)
assert.match(
  tabs,
  /pqcLeader:\s*'\/mes\/pro\/feedback\/edhr-batch-pqc-leader'/,
  'PQC leader tab must map to the dedicated eDHR batch PQC leader route.'
)

const routePath = "path: 'pro/feedback/edhr-batch-team-leader'"
const routeIndex = router.indexOf(routePath)
assert.ok(routeIndex >= 0, 'eDHR batch leader workbench route must exist.')
const nextRouteIndex = router.indexOf('\n      {', routeIndex + routePath.length)
const routeBlock = router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)
const pqcRoutePath = "path: 'pro/feedback/edhr-batch-pqc-leader'"
const pqcRouteIndex = router.indexOf(pqcRoutePath)
assert.ok(pqcRouteIndex >= 0, 'eDHR batch PQC leader route must exist.')
const nextPqcRouteIndex = router.indexOf('\n      {', pqcRouteIndex + pqcRoutePath.length)
const pqcRouteBlock = router.slice(
  pqcRouteIndex,
  nextPqcRouteIndex > pqcRouteIndex ? nextPqcRouteIndex : undefined
)

assert.match(routeBlock, /BatchTeamLeaderWorkbenchPage\.vue/, 'leader route must use the eDHR wrapper page.')
assert.match(routeBlock, /name:\s*'MesProEdhrBatchTeamLeaderWorkbench'/, 'leader route name must be stable.')
assert.match(routeBlock, /title:\s*'组长工作台'/, 'leader route title must be visible.')
assert.match(
  routeBlock,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'leader route must require the formal team-leader permission.'
)
assert.match(
  pqcRouteBlock,
  /BatchPqcLeaderWorkbenchPage\.vue/,
  'PQC leader route must use the dedicated eDHR wrapper page.'
)
assert.match(
  pqcRouteBlock,
  /name:\s*'MesProEdhrBatchPqcLeaderWorkbench'/,
  'PQC leader route name must be stable.'
)
assert.match(pqcRouteBlock, /title:\s*'PQC组长'/, 'PQC leader route title must be visible.')
assert.match(
  pqcRouteBlock,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'PQC leader route must require the formal team-leader permission.'
)

assert.match(
  page,
  /<EdhrBatchRecordTabs\s+active-tab="teamLeader"/,
  'leader wrapper page must render the shared eDHR batch tabs.'
)
assert.match(
  page,
  /<TeamLeaderWorkbenchPage[\s\S]*leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/,
  'leader wrapper page must lock the formal workbench to production leader content.'
)
assert.match(page, /data-edhr-batch-team-leader-page/, 'leader wrapper page must expose a stable selector.')
assert.match(
  page,
  /@\/views\/mes\/pro\/processpool\/TeamLeaderWorkbenchPage\.vue/,
  'leader wrapper page must import the existing team leader workbench component.'
)
assert.doesNotMatch(
  page,
  /leader-type="PQC"|PQC 组长|PQC组长/,
  'leader wrapper page must not render or link PQC leader content inside the group leader workbench.'
)

assert.match(
  pqcPage,
  /<EdhrBatchRecordTabs\s+active-tab="pqcLeader"/,
  'PQC leader wrapper page must render the shared eDHR batch tabs with pqcLeader active.'
)
assert.match(
  pqcPage,
  /<TeamLeaderWorkbenchPage[\s\S]*leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/,
  'PQC leader wrapper page must lock the formal workbench to PQC leader content.'
)
assert.match(pqcPage, /data-edhr-batch-pqc-leader-page/, 'PQC leader wrapper page must expose a stable selector.')
assert.match(
  pqcPage,
  /@\/views\/mes\/pro\/processpool\/TeamLeaderWorkbenchPage\.vue/,
  'PQC leader wrapper page must import the existing formal team leader workbench component.'
)
assert.match(
  teamLeader,
  /showLeaderTypeTabs[\s\S]*false/,
  'formal workbench must default to hiding internal leader-type tabs.'
)
assert.match(
  teamLeader,
  /v-if="showLeaderTypeTabs"[\s\S]*data-team-leader-type-tabs/,
  'formal workbench may only expose production/PQC switch when an explicit caller opts in.'
)
assert.match(teamLeader, /leaderType:\s*'PRODUCTION'/, 'formal workbench must keep production as the default leader type.')
assert.match(teamLeader, /leaderType === 'PQC'[\s\S]*PQC_SIMPLIFIED/, 'formal workbench must keep PQC-specific query state.')

assert.match(
  pageGraph,
  /id:\s*'team-lead-review'[\s\S]*title:\s*'班组长复核'[\s\S]*route:\s*'\/mes\/pro\/feedback\/edhr-batch-team-leader'[\s\S]*isDisabled:\s*false/,
  'page graph team leader node must become an official clickable eDHR batch route.'
)
assert.match(
  pageGraph,
  /id:\s*'pqc-lead-review'[\s\S]*title:\s*'PQC组长'[\s\S]*route:\s*'\/mes\/pro\/feedback\/edhr-batch-pqc-leader'[\s\S]*isDisabled:\s*false/,
  'page graph must expose the dedicated PQC leader review route instead of grouping it under team leader.'
)

console.log('PASS: eDHR batch record leader tabs static contract')
