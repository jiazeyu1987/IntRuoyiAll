const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'),
  'utf8'
)

const assertIncludes = (token, message) => {
  assert.ok(page.includes(token), message)
}

const assertExcludes = (token, message) => {
  assert.ok(!page.includes(token), message)
}

for (const token of [
  'type="expand"',
  '派工证据',
  '生产上下文',
  '处理提示',
  '时间状态',
  'resolveTaskTimeSummary',
  'edhr-work-task-page__evidence'
]) {
  assertIncludes(token, `工作任务看板必须提供业务主列和派工证据展开区：${token}`)
}

for (const forbiddenMainColumn of [
  '<el-table-column label="审核签字格"',
  '<el-table-column label="审核来源"',
  '<el-table-column label="候选来源"',
  '<el-table-column label="候选快照"',
  '<el-table-column label="返工来源"',
  '<el-table-column label="到期时间"',
  '<el-table-column label="逾期时间"',
  '<el-table-column label="创建时间"',
  '<el-table-column label="完成时间"'
]) {
  assertExcludes(forbiddenMainColumn, `工作任务看板主表不得保留低频技术列：${forbiddenMainColumn}`)
}

assertIncludes(
  'empty-text="暂无工作任务"',
  '工作任务看板表格必须提供明确空态文案。'
)
assertIncludes(
  'resolveCandidateSnapshotLabel(row)',
  '候选快照必须基于完整任务合同格式化后放入派工证据。'
)
assertIncludes(
  'candidateSnapshotDisplay',
  '工作任务合同必须提供候选快照姓名摘要字段。'
)
assertExcludes(
  'return candidateUserSnapshot',
  '候选快照展示不得继续直接回显原始 ID 串。'
)
assertIncludes(
  '责任来源',
  '工作任务看板必须显式展示责任来源。'
)
assertIncludes(
  'resolveResponsibilitySourceLabel',
  '工作任务看板必须格式化责任来源，而不是直接暴露内部字段。'
)
assertIncludes(
  '候选池名称',
  '工作任务看板必须显式展示候选池名称。'
)
assertIncludes(
  'resolveCandidatePoolNameLabel',
  '工作任务看板必须格式化候选池名称。'
)
assertIncludes(
  '当前用户不可操作原因',
  '工作任务看板必须显式展示当前用户不可操作原因。'
)
assertIncludes(
  'resolveInactionReasonLabel',
  '工作任务看板必须根据任务责任和候选资格生成不可操作说明。'
)
assertIncludes(
  'resolveReworkSourceLabel(row)',
  '返工来源必须格式化后放入派工证据。'
)

console.log('PASS: EDHR work task board UI static contract is satisfied.')
