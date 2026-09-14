const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routes = readSource('src/router/modules/remaining.ts')
const permissionStore = readSource('src/store/modules/permission.ts')
const packageJson = JSON.parse(readSource('package.json'))

const extractBetween = (source, startMarker, endMarker) => {
  const start = source.indexOf(startMarker)
  assert.notEqual(start, -1, `missing source marker: ${startMarker}`)
  const end = source.indexOf(endMarker, start + startMarker.length)
  assert.notEqual(end, -1, `missing source marker: ${endMarker}`)
  return source.slice(start, end)
}

const extractRouteEntry = (source, routeName) => {
  const nameIndex = source.indexOf(`name: '${routeName}'`)
  assert.notEqual(nameIndex, -1, `missing route entry ${routeName}`)
  const start = source.lastIndexOf('{', nameIndex)
  assert.notEqual(start, -1, `missing route entry start ${routeName}`)
  return source.slice(start, nameIndex + 600)
}

const approvalCenterBlock = extractBetween(
  routes,
  "path: '/approval-center'",
  "path: '/signature-governance'"
)

for (const marker of [
  "title: '待办'",
  "title: '已办'",
  "title: '我发起的'",
  "title: '抄送我的'",
  "title: '流程模型'",
  "title: 'OA 示例'"
]) {
  assert.notEqual(approvalCenterBlock.indexOf(marker), -1, `approval center route must keep ${marker}`)
}

const ccIndex = approvalCenterBlock.indexOf("title: '抄送我的'")
const modelIndex = approvalCenterBlock.indexOf("title: '流程模型'")
assert.ok(
  ccIndex < modelIndex,
  '流程模型 must be declared after 抄送我的 in the approval center menu order'
)

const workflowManagementEntry = extractRouteEntry(
  approvalCenterBlock,
  'ApprovalCenterWorkflowManagement'
)
assert.match(
  workflowManagementEntry,
  /alwaysShow:\s*false/,
  '流程管理 parent must not force a visible submenu shell after only 流程模型 remains visible'
)

for (const [routeName, title] of [
  ['ApprovalCenterBpmForm', '流程表单'],
  ['ApprovalCenterBpmCategory', '流程分类'],
  ['ApprovalCenterBpmBusinessApprovalPolicy', '业务审批策略'],
  ['ApprovalCenterBpmUserGroup', '用户分组'],
  ['ApprovalCenterBpmProcessExpression', '流程表达式'],
  ['ApprovalCenterFormCenter', '表单中心'],
  ['ApprovalCenterOaExample', 'OA 示例'],
  ['ApprovalCenterOALeave', '请假查询']
]) {
  const routeEntry = extractRouteEntry(approvalCenterBlock, routeName)
  assert.match(routeEntry, new RegExp(`title:\\s*'${title}'`), `${title} route must remain declared`)
  assert.match(
    routeEntry,
    /hidden:\s*true/,
    `${title} must not display in the approval center side menu`
  )
}

for (const requiredDynamicGuard of [
  'normalizeApprovalCenterWorkflowManagementMenu',
  'APPROVAL_CENTER_WORKFLOW_MANAGEMENT_ROUTE_NAME',
  'APPROVAL_CENTER_BPM_MODEL_ROUTE_NAME',
  'APPROVAL_CENTER_CC_ROUTE_NAME',
  'APPROVAL_CENTER_OA_EXAMPLE_ROUTE_NAME',
  'insertApprovalCenterModelMenuAfterCc'
]) {
  assert.match(
    permissionStore,
    new RegExp(requiredDynamicGuard),
    `dynamic approval center menu merge must keep ${requiredDynamicGuard}`
  )
}

assert.match(
  permissionStore,
  /ApprovalCenterWorkflowManagement[\s\S]*hidden:\s*true/,
  'dynamic merge must hide the 流程管理 parent from the rendered approval center menu'
)

assert.match(
  permissionStore,
  /hiddenApprovalCenterOaExampleRoute[\s\S]*hideApprovalCenterMenuRoute/,
  'dynamic merge must hide OA 示例 even when the backend permission menu sends it as visible'
)

assert.match(
  permissionStore,
  /!isApprovalCenterOaExampleRoute\(child\)/,
  'dynamic merge must remove OA 示例 from the visible approval center children before rendering the side menu'
)

assert.doesNotMatch(
  `${routes}\n${permissionStore}`,
  /mock|placeholder|fallback|降级|吞异常/i,
  'approval center menu placement must not introduce mock, placeholder, fallback, downgrade, or swallowed-error paths'
)

assert.equal(
  packageJson.scripts?.['e2e:approval-center:model-menu-placement:static'],
  'node tests/e2e/approval-center-model-menu-placement-static.spec.js',
  'package.json must expose the approval center model menu placement static contract'
)

console.log('PASS: approval center 流程模型 menu placement static contract')
