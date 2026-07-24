const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const workTaskApiPath = path.join(frontendRoot, 'src/api/mes/pro/edhr/workTask.ts')
const source = fs.readFileSync(componentPath, 'utf8')
const workTaskApi = fs.readFileSync(workTaskApiPath, 'utf8')

const assertMatch = (content, pattern, message) => {
  assert.match(content, pattern, message)
}

assertMatch(
  workTaskApi,
  /getEdhrRouteReleaseApprovalRule[\s\S]*route-release-approval-rule[\s\S]*saveEdhrRouteReleaseApprovalRule/,
  '前端必须复用现有路线最终放行审批责任人规则接口。'
)
assertMatch(
  source,
  /getEdhrRouteReleaseApprovalRule[\s\S]*saveEdhrRouteReleaseApprovalRule/,
  '流转关系图必须接入最终放行审批责任人规则读写接口。'
)
assertMatch(
  source,
  /getSimpleUserList[\s\S]*getSimpleRoleList[\s\S]*type RoleVO/,
  '放行责任人选择必须同时支持系统用户精简列表和权限角色精简列表。'
)
assertMatch(
  source,
  /selectedBoundaryType === 'END'[\s\S]*data-flow-boundary-field="releaseOwner"[\s\S]*放行责任人/,
  '选中工序结束节点时，左侧面板必须只显示放行责任人配置 item。'
)
assertMatch(
  source,
  /handleSelectBoundaryDetailField\('releaseOwner'\)/,
  '左侧放行责任人 item 必须能切换右侧字段明细。'
)
assertMatch(
  source,
  /selectedBoundaryDetailFieldKey === 'releaseOwner'[\s\S]*data-flow-panel="release-owner-detail"/,
  '右侧字段明细必须提供工序结束放行责任人明细面板。'
)
assertMatch(
  source,
  /:data-flow-field-editor="selectedBoundaryDetailFieldKey"[\s\S]*releaseApprovalRuleForm\.candidateSourceType[\s\S]*具体人员[\s\S]*权限角色[\s\S]*releaseApprovalRuleForm\.candidateSourceId[\s\S]*placeholder="请选择放行责任人"/,
  '右侧放行责任人明细必须提供候选类型切换和稳定候选选择器。'
)
assertMatch(
  source,
  /loadReleaseApprovalRuleDetail[\s\S]*getEdhrRouteReleaseApprovalRule\(props\.routeId\)/,
  '选择放行责任人 item 后必须按当前路线加载已有放行责任人规则。'
)
assertMatch(
  source,
  /handleReleaseApprovalRuleSave[\s\S]*saveEdhrRouteReleaseApprovalRule\([\s\S]*routeId: props\.routeId[\s\S]*candidateSourceType: releaseApprovalRuleForm\.candidateSourceType[\s\S]*candidateSourceId: releaseApprovalRuleForm\.candidateSourceId/,
  '保存放行责任人时必须写入当前路线的最终放行审批候选规则。'
)
assert.doesNotMatch(
  source,
  /releaseApprovalRuleForm\.dueMinutes|DEFAULT_RELEASE_APPROVAL_RULE_DUE_MINUTES|处理时限：/,
  '放行责任人配置不得继续显示或保存处理时限。'
)

console.log('PASS: MES route flow end release owner static contract')
