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
  /selectedBoundaryType === 'START'(?:(?!<template v-else)[\s\S])*data-flow-boundary-field="releaseOwner"[\s\S]*放行责任人/,
  '选中工序开始节点时，左侧面板必须包含放行责任人配置 item。'
)
assertMatch(
  source,
  /handleSelectBoundaryDetailField\('releaseOwner'\)/,
  '左侧放行责任人 item 必须能切换右侧字段明细。'
)
assertMatch(
  source,
  /selectedBoundaryType === 'START' && selectedBoundaryDetailFieldKey === 'releaseOwner'[\s\S]*<strong>工序开始<\/strong>[\s\S]*<RouteReleaseOwners\b/,
  '右侧字段明细必须提供工序开始放行责任人明细面板。'
)
assert.doesNotMatch(source, /saveEdhrRouteReleaseApprovalRule|getEdhrRouteReleaseApprovalRule/,
  '放行责任人面板应使用正式的生产放行与上市放行角色，不得读写旧的最终放行规则。')
assert.doesNotMatch(
  source,
  /releaseApprovalRuleForm\.dueMinutes|DEFAULT_RELEASE_APPROVAL_RULE_DUE_MINUTES|处理时限：/,
  '放行责任人配置不得继续显示或保存处理时限。'
)

assert.doesNotMatch(
  source,
  /selectedBoundaryType === 'END' && selectedBoundaryDetailFieldKey === 'releaseOwner'|boundaryType === 'START' \? 'basic' : 'releaseOwner'|流转关系图工序结束放行责任人/,
  '工序结束不得再展示或默认选中放行责任人，保存备注必须对应工序开始。'
)
assertMatch(
  source,
  /boundaryType === 'START' \? 'basic' : undefined/,
  '工序结束不应默认选中字段，工序开始仍默认展示基本设置。'
)
console.log('PASS: MES route flow start release owner static contract')
