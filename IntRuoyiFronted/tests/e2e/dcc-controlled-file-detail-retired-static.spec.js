const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const remainingRouter = readSource('src/router/modules/remaining.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const viewerPresentation = readSource('src/views/dcc/controlled-file/view/presentation.ts')
const profileWorkbench = readSource('src/views/Profile/components/ProfileWorkbench.vue')
const approvalCenter = readSource('src/views/approval-center/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:detail-retired:static'],
  'node tests/e2e/dcc-controlled-file-detail-retired-static.spec.js',
  'package.json must expose the retired controlled-file detail route contract'
)

const detailRoute = extractBetween(
  remainingRouter,
  "path: 'controlled-file/detail/:id(\\\\d+)'",
  "path: 'controlled-file/logs'",
  'controlled file detail route'
)

assert.match(
  detailRoute,
  /beforeEnter:\s*\(to\)\s*=>/,
  'controlled file detail route must guard direct normal-detail access'
)
assert.match(
  detailRoute,
  /String\(to\.query\.viewer \|\| ''\) === '1'/,
  'controlled file detail route must only allow explicit viewer mode'
)
assert.match(
  detailRoute,
  /const isBrowserTraceability\s*=/,
  'controlled file detail route must declare the controlled browser traceability exception'
)
assert.match(
  detailRoute,
  /String\(to\.query\.traceability \|\| ''\) === '1'/,
  'controlled browser traceability entry must require traceability=1'
)
assert.match(
  detailRoute,
  /String\(to\.query\.from \|\| ''\) === 'browser'/,
  'controlled browser traceability entry must be limited to the browser source'
)
assert.match(
  detailRoute,
  /Boolean\(String\(to\.query\.returnTo \|\| ''\)\)/,
  'controlled browser traceability entry must preserve a return path'
)
assert.match(
  detailRoute,
  /isBrowserTraceability/,
  'route guard must allow the explicit controlled browser traceability entry'
)
assert.match(
  detailRoute,
  /name:\s*'DccControlledFileBrowser'/,
  'normal controlled file detail access must redirect to the browser page'
)
assert.match(
  viewerPresentation,
  /buildControlledFileTraceabilityPath/,
  'shared presentation helpers must expose a traceability path builder'
)
assert.match(
  viewerPresentation,
  /new URLSearchParams\(\{ viewer: '1' \}\)/,
  'shared controlled viewer links must keep the viewer query that passes the route guard'
)

const closeViewerMode = extractBetween(
  detailPage,
  'const closeViewerMode = () => {',
  'const openBpmDetail = () => {',
  'close viewer mode handler'
)

assert.match(
  closeViewerMode,
  /name:\s*'DccControlledFileBrowser'/,
  'closing viewer mode without returnTo must go back to the browser page'
)
assert.doesNotMatch(
  closeViewerMode,
  /name:\s*'DccControlledFileDetail'/,
  'closing viewer mode must not reopen the retired normal detail page'
)

const openHistoryDetail = extractBetween(
  detailPage,
  'const openHistoryDetail = (id: number | string) => {',
  'const closeViewerMode = () => {',
  'version history detail handler'
)

assert.match(
  openHistoryDetail,
  /buildControlledFileViewerPath\(id,\s*'version-history',\s*route\.fullPath\)/,
  'version history rows must open the retained viewer route'
)
assert.doesNotMatch(
  openHistoryDetail,
  /name:\s*'DccControlledFileDetail'/,
  'version history rows must not push the retired normal detail route'
)

const resubmitWithdrawnFlow = extractBetween(
  detailPage,
  'const handleResubmitWithdrawnFlow = async () => {',
  'const handleRetryStamp = async () => {',
  'withdrawn flow resubmit handler'
)

assert.match(
  resubmitWithdrawnFlow,
  /buildControlledFileViewerPath\(newFileId,\s*'resubmit-withdrawn-flow',\s*route\.fullPath\)/,
  'withdrawn flow resubmission must open the retained viewer route for the new file'
)
assert.doesNotMatch(
  resubmitWithdrawnFlow,
  /name:\s*'DccControlledFileDetail'/,
  'withdrawn flow resubmission must not push the retired normal detail route'
)

const distributionWorkbenchRoute = extractBetween(
  profileWorkbench,
  'const mapDccDistributionRow = (item: DistributionTaskVO): UnifiedTodoRow => ({',
  'const mapDccTrainingRow = (item: TrainingTaskProgressVO): UnifiedTodoRow => ({',
  'profile workbench DCC distribution route'
)

assert.match(
  distributionWorkbenchRoute,
  /viewer:\s*'1'/,
  'profile workbench DCC distribution route must pass the viewer route guard'
)
assert.match(
  distributionWorkbenchRoute,
  /from:\s*'profile-distribution'/,
  'profile workbench DCC distribution route must identify the entry source'
)
assert.match(
  distributionWorkbenchRoute,
  /distributionId:\s*String\(item\.distributionId\)/,
  'profile workbench DCC distribution route must preserve distribution context as a query value'
)
assert.match(
  distributionWorkbenchRoute,
  /recipientId:\s*String\(item\.recipientId\)/,
  'profile workbench DCC distribution route must preserve recipient context as a query value'
)

assert.match(
  approvalCenter,
  /DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX\s*=\s*'\/dcc\/controlled-file\/detail\/'/,
  'approval center must define the DCC controlled-file detail viewer boundary'
)
assert.match(
  approvalCenter,
  /const resolveDccApprovalDetailLocation = \(/,
  'approval center must normalize DCC detail locations before navigation'
)
assert.match(
  approvalCenter,
  /viewer:\s*'1'/,
  'approval center DCC navigation must add viewer=1'
)
assert.match(
  approvalCenter,
  /from:\s*nextQuery\.from \|\| 'approval-center'/,
  'approval center DCC navigation must keep or set the approval-center source'
)
assert.match(
  approvalCenter,
  /resolveDccApprovalDetailLocation\(row,\s*resolveDecisionDetailRoute\(row\),\s*resolveDecisionDetailQuery\(row\)\)/,
  'approval center decision detail navigation must pass through the DCC viewer normalizer'
)
assert.match(
  approvalCenter,
  /resolveDccApprovalDetailLocation\(row,\s*row\.detailRoute,\s*row\.detailQuery \|\| \{\}\)/,
  'approval center module detail navigation must pass through the DCC viewer normalizer'
)

console.log('PASS: DCC controlled-file normal detail route is retired')
