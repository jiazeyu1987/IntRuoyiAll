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
const approvalCenter = readSource('src/views/approval-center/index.vue')
const remainingRouter = readSource('src/router/modules/remaining.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:approval-center-handling-entry:static'],
  'node tests/e2e/dcc-approval-center-handling-entry-static.spec.js',
  'package.json must expose the DCC approval-center handling entry static contract'
)

const resolveDccApprovalDetailLocation = extractBetween(
  approvalCenter,
  'const resolveDccApprovalDetailLocation = (',
  'const resolveDecisionDetailRoute = (row: ApprovalTaskSummaryVO) => {',
  'approval-center DCC detail resolver'
)

assert.match(
  resolveDccApprovalDetailLocation,
  /const isDccModuleHandling = isDccModuleHandlingAction\(row\)/,
  'approval center must explicitly distinguish module handling from readonly viewing'
)
assert.match(
  resolveDccApprovalDetailLocation,
  /handling:\s*DCC_APPROVAL_HANDLING_MODE/,
  'approval center DCC TODO handling navigation must pass handling=approval'
)
assert.match(
  resolveDccApprovalDetailLocation,
  /processInstanceId:\s*nextQuery\.processInstanceId \|\| row\.processInstanceId/,
  'approval center DCC handling navigation must preserve the process instance id'
)
assert.match(
  resolveDccApprovalDetailLocation,
  /taskId:\s*nextQuery\.taskId \|\| row\.sourceTaskId/,
  'approval center DCC handling navigation must preserve the current BPM task id'
)

const handlingBranch = extractBetween(
  resolveDccApprovalDetailLocation,
  'if (isDccModuleHandling) {',
  'return {',
  'approval-center DCC handling branch'
)

assert.doesNotMatch(
  handlingBranch,
  /viewer:\s*'1'/,
  'DCC module handling must not be downgraded to readonly viewer mode'
)

assert.match(
  approvalCenter,
  /const isDccModuleHandlingAction = \(row: ApprovalTaskSummaryVO\) =>/,
  'approval center must define an explicit DCC module handling predicate'
)
assert.match(
  approvalCenter,
  /actions\.includes\('PROCESS_IN_MODULE'\)/,
  'DCC approval-center TODO rows must use PROCESS_IN_MODULE as the handling capability'
)

const detailRoute = extractBetween(
  remainingRouter,
  "path: 'controlled-file/detail/:id(\\\\d+)'",
  "path: 'controlled-file/logs'",
  'controlled file detail route'
)

assert.match(
  detailRoute,
  /const isApprovalHandling =/,
  'controlled file detail route must name the approval handling gate'
)
assert.match(
  detailRoute,
  /String\(to\.query\.handling \|\| ''\) === 'approval'/,
  'controlled file detail route must allow explicit approval handling mode'
)
assert.match(
  detailRoute,
  /String\(to\.query\.from \|\| ''\) === 'approval-center'/,
  'controlled file detail route must require approval-center as the handling source'
)
assert.match(
  detailRoute,
  /String\(to\.query\.viewer \|\| ''\) === '1' \|\| isApprovalHandling/,
  'controlled file detail route must allow viewer mode and the explicit approval handling gate only'
)
assert.match(
  detailRoute,
  /name:\s*'DccControlledFileBrowser'/,
  'ordinary direct detail access must remain redirected to the controlled-file browser'
)

assert.match(
  detailPage,
  /const canLoadApprovalDetail = Boolean\(taskId\) && checkPermi\(\['bpm:process-instance:query'\]\)/,
  'DCC detail must avoid generic BPM approval-detail reads for approvers without process-instance permission'
)
assert.match(
  detailPage,
  /approvalTodoTask\.value = detail\?\.todoTask \|\| findCurrentUserTodoTask\(normalizedTaskList\)/,
  'DCC detail must still resolve the current approver task from the process task list'
)

console.log('PASS: DCC approval-center handling entry static contract')
