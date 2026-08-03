const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const router = read('src/router/modules/remaining.ts')
const tabsPath = 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue'
const pagePath = 'src/views/mes/pro/edhr-batch/BatchTeamLeaderWorkbenchPage.vue'
const teamLeaderPath = 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
const pageGraphPath = 'src/views/mes/pro/edhr-batch/BatchPageGraphPage.vue'

assert.ok(exists(tabsPath), `${tabsPath} must exist.`)
assert.ok(exists(teamLeaderPath), `${teamLeaderPath} must exist.`)
assert.ok(exists(pagePath), `${pagePath} must exist.`)

const tabs = read(tabsPath)
const page = read(pagePath)
const teamLeader = read(teamLeaderPath)
const pageGraph = read(pageGraphPath)

assert.match(tabs, /组长工作台/, 'eDHR batch tabs must include a leader workbench sub-tab.')
assert.match(tabs, /'teamLeader'/, 'eDHR batch tab union must include the teamLeader key.')
assert.match(
  tabs,
  /teamLeader:\s*'\/mes\/pro\/feedback\/edhr-batch-team-leader'/,
  'leader workbench tab must map to the eDHR batch leader route.'
)

const routePath = "path: 'pro/feedback/edhr-batch-team-leader'"
const routeIndex = router.indexOf(routePath)
assert.ok(routeIndex >= 0, 'eDHR batch leader workbench route must exist.')
const nextRouteIndex = router.indexOf('\n      {', routeIndex + routePath.length)
const routeBlock = router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)

assert.match(routeBlock, /BatchTeamLeaderWorkbenchPage\.vue/, 'leader route must use the eDHR wrapper page.')
assert.match(routeBlock, /name:\s*'MesProEdhrBatchTeamLeaderWorkbench'/, 'leader route name must be stable.')
assert.match(routeBlock, /title:\s*'组长工作台'/, 'leader route title must be visible.')
assert.match(
  routeBlock,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'leader route must require the formal team-leader permission.'
)

assert.match(
  page,
  /<EdhrBatchRecordTabs\s+active-tab="teamLeader"/,
  'leader wrapper page must render the shared eDHR batch tabs.'
)
assert.match(
  page,
  /<TeamLeaderWorkbenchPage\s*\/>/,
  'leader wrapper page must reuse the existing formal team leader workbench.'
)
assert.match(page, /data-edhr-batch-team-leader-page/, 'leader wrapper page must expose a stable selector.')
assert.match(
  page,
  /@\/views\/mes\/pro\/processpool\/TeamLeaderWorkbenchPage\.vue/,
  'leader wrapper page must import the existing team leader workbench component.'
)
assert.doesNotMatch(page, /生产组长[\s\S]*PQC 组长[\s\S]*router\.push/, 'wrapper must not create fake link cards instead of mounting the formal workbench.')

assert.match(teamLeader, /<el-tab-pane label="生产组长" name="PRODUCTION"/, 'formal workbench must expose production leader tab.')
assert.match(teamLeader, /<el-tab-pane label="PQC 组长" name="PQC"/, 'formal workbench must expose PQC leader tab.')
assert.match(teamLeader, /leaderType:\s*'PRODUCTION'/, 'formal workbench must keep production as the default leader type.')
assert.match(teamLeader, /leaderType === 'PQC'[\s\S]*PQC_SIMPLIFIED/, 'formal workbench must keep PQC-specific query state.')

assert.match(
  pageGraph,
  /id:\s*'team-lead-review'[\s\S]*route:\s*'\/mes\/pro\/feedback\/edhr-batch-team-leader'[\s\S]*isDisabled:\s*false/,
  'page graph team leader node must become an official clickable eDHR batch route.'
)

console.log('PASS: eDHR batch record leader tabs static contract')
