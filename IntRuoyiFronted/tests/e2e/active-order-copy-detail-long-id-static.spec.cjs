const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const api = read('src/api/mes/pro/processpool/teamLeader.ts')
const workbench = read('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const detailPage = read('src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const routeId = read('src/utils/routeQueryId.ts')

const activeOrderRow = api
  .split('export interface TeamLeaderActiveOrderRespVO {')[1]
  .split('\n}')[0]
assert.match(
  activeOrderRow,
  /^\s*\n\s*readBlocked\?[\s\S]*?\bid:\s*number\s*\|\s*string/m,
  'active-order list IDs must preserve large Java Long values serialized as decimal strings'
)

const copyHandler = workbench
  .split('const handleCopyLatestSimulationActiveOrder = async (row: TeamLeaderActiveOrderRespVO) => {')[1]
  .split('const handleCleanupLatestSimulationActiveOrder')[0]
assert.match(copyHandler, /await loadActiveOrders\(\)/)

const openDetail = workbench
  .split('const openActiveOrderSubmissionDetail = (row: TeamLeaderActiveOrderRespVO) => {')[1]
  .split('const resolveActiveOrderConflictProcesses')[0]
assert.match(openDetail, /parsePositiveRouteQueryId\(row\.id\)/)
assert.match(openDetail, /navigateActiveOrderSubmissionDetail\(sourceActiveOrderId\)/)
assert.doesNotMatch(openDetail, /Number\(|requirePositiveNumber\(/)
assert.doesNotMatch(openDetail, /stage1GeneratedActiveOrderId|simulationCopyActiveOrderId/)

const detailNavigation = workbench
  .split('const navigateActiveOrderSubmissionDetail = (')[1]
  .split('const openActiveOrderSubmissionDetail')[0]
assert.match(detailNavigation, /activeOrderId:\s*string/)
assert.match(detailNavigation, /params:\s*\{\s*activeOrderId\s*\}/)

assert.match(api, /getTeamLeaderActiveOrderDetail\s*=\s*async\s*\(activeOrderId:\s*number\s*\|\s*string\)/)
assert.match(api, /params:\s*\{\s*activeOrderId\s*\}/)

assert.match(detailPage, /import\s*\{\s*parsePositiveRouteQueryId\s*\}\s*from\s*'@\/utils\/routeQueryId'/)
const activeOrderIdParser = detailPage
  .split('const requireActiveOrderId = () => {')[1]
  .split('const resolveSourceWorkOrderCode')[0]
assert.match(activeOrderIdParser, /parsePositiveRouteQueryId\(route\.params\.activeOrderId\)/)
assert.doesNotMatch(activeOrderIdParser, /Number\(/)
assert.match(detailPage, /getTeamLeaderActiveOrderDetail\(requireActiveOrderId\(\)\)/)
assert.match(routeId, /export const parsePositiveRouteQueryId/)
assert.match(routeId, /POSITIVE_INTEGER_TEXT\.test\(text\)\s*\?\s*text\s*:\s*''/)
assert.doesNotMatch(routeId, /Number\(|parseInt\(|parseFloat\(/)

console.log('active-order-copy-detail-long-id-static: PASS')
