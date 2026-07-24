const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const workTaskApiPath = path.join(frontendRoot, 'src/api/mes/pro/edhr/workTask.ts')
const componentSource = fs.readFileSync(componentPath, 'utf8')
const workTaskApiSource = fs.readFileSync(workTaskApiPath, 'utf8')

const assertMatch = (content, pattern, message) => {
  assert.match(content, pattern, message)
}

const assertNotMatch = (content, pattern, message) => {
  assert.doesNotMatch(content, pattern, message)
}

assertMatch(
  workTaskApiSource,
  /type EdhrWorkTaskReleaseApprovalCandidateSourceType = 'USER' \| 'ROLE_GROUP'/,
  '最终放行规则前端请求类型必须明确限制为具体人员或权限角色。'
)
assertMatch(
  workTaskApiSource,
  /interface EdhrWorkTaskReleaseApprovalRuleReqVO[\s\S]*candidateSourceType: EdhrWorkTaskReleaseApprovalCandidateSourceType[\s\S]*candidateSourceId: number/,
  '最终放行规则保存请求必须使用候选来源类型和候选来源 ID。'
)
assertNotMatch(
  workTaskApiSource,
  /interface EdhrWorkTaskReleaseApprovalRuleReqVO[\s\S]*dueMinutes:/,
  '最终放行规则前端保存请求不得继续暴露处理时限。'
)

assertMatch(
  componentSource,
  /getSimpleUserList[\s\S]*getSimpleRoleList[\s\S]*type RoleVO/,
  '放行责任人配置必须同时加载用户精简列表和角色精简列表。'
)
assertMatch(
  componentSource,
  /releaseApprovalRuleForm\.candidateSourceType[\s\S]*具体人员[\s\S]*权限角色/,
  '放行责任人字段明细必须允许在具体人员和权限角色之间切换。'
)
assertMatch(
  componentSource,
  /releaseApprovalRuleForm\.candidateSourceId[\s\S]*placeholder="请选择放行责任人"/,
  '放行责任人选择器必须绑定候选来源 ID，而不是单一 assigneeUserId。'
)
assertMatch(
  componentSource,
  /handleReleaseApprovalRuleSave[\s\S]*candidateSourceType: releaseApprovalRuleForm\.candidateSourceType[\s\S]*candidateSourceId: releaseApprovalRuleForm\.candidateSourceId/,
  '保存放行责任人时必须写入候选来源类型和候选来源 ID。'
)
assertNotMatch(
  componentSource,
  /releaseApprovalRuleForm\.dueMinutes|DEFAULT_RELEASE_APPROVAL_RULE_DUE_MINUTES|处理时限：/,
  '工序结束放行责任人字段明细不得显示或保存处理时限。'
)
assertNotMatch(
  componentSource,
  /<span>字段值<\/span>[\s\S]*releaseApprovalRuleAssigneeLabel/,
  '工序结束放行责任人字段明细不得显示默认字段值快照。'
)

console.log('PASS: MES route flow release owner candidate static contract')
