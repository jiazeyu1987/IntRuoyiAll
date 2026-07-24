const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..')
const permissionPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr/PermissionMatrixPage.vue'),
  'utf8'
)
const recordChangePage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr/RecordChangePage.vue'),
  'utf8'
)

const assertContains = (source, token, message) => {
  assert.ok(source.includes(token), message)
}

const assertNotContains = (source, token, message) => {
  assert.ok(!source.includes(token), message)
}

for (const token of [
  'edhr-permission-matrix__section-head',
  '权限规则',
  '评估条件',
  '评估结论',
  '评估证据',
  'edhr-permission-matrix__result-summary'
]) {
  assertContains(permissionPage, token, `对象权限矩阵页必须分区展示：${token}`)
}

assertNotContains(
  permissionPage,
  '<el-table-column label="能力代码"',
  '对象权限矩阵页主表不得把能力代码作为默认主列。'
)
assertContains(
  permissionPage,
  'formatDecisionLabel(row.decision)',
  '对象权限矩阵页必须把后端决策转换为中文业务文案。'
)
assert.match(
  permissionPage,
  /评估证据[\s\S]*(matchedRuleIds|operationAuditEventId)/,
  '对象权限矩阵页必须把匹配规则和审计事件 ID 收纳到评估证据区。'
)

for (const token of [
  'type="expand"',
  '变更证据',
  '链路证据',
  'empty-text="暂无变更记录"',
  'resolveExecutionStatusLabel'
]) {
  assertContains(recordChangePage, token, `变更记录页必须分层展示：${token}`)
}

assertNotContains(
  recordChangePage,
  'batch={{ row.batchExecutionId',
  '变更记录页主表不得显示英文 batch= 原始串。'
)
assertNotContains(
  recordChangePage,
  'execution={{ row.executionId',
  '变更记录页主表不得显示英文 execution= 原始串。'
)
assertNotContains(
  recordChangePage,
  '<el-table-column label="签名"',
  '变更记录页主表不得把签名 ID 作为默认主列。'
)
assert.ok(
  !/<Dialog[\s\S]*?<el-descriptions[\s\S]*?label="原 Head Hash"/.test(recordChangePage),
  '变更详情弹窗不得在默认描述区直出 Head Hash，必须放入链路证据区。'
)

console.log('PASS: EDHR governance pages UI static contract is satisfied.')
