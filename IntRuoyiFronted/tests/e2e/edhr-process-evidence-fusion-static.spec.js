const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')

const detail = fs.readFileSync(detailPath, 'utf8')

assert(
  detail.includes('工序证据链') && detail.includes('工序上下文') && detail.includes('完整明细入口'),
  '融合详情页必须在工序复盘中提供工序证据链分区'
)
assert(
  detail.includes('当前工序操作台') &&
    detail.includes('仅作用于当前选中的工序') &&
    detail.includes('selectedProcessEvidenceGroups'),
  '工序证据链必须改为当前工序操作台，并明确入口仅作用于当前选中工序'
)
assert(
  [
    '工序执行',
    '审签归档',
    '审计追溯',
    '关联引用'
  ].every((groupLabel) => detail.includes(groupLabel)),
  '当前工序操作台必须按执行、审签、审计追溯、关联引用分组展示'
)
assert(
  detail.includes('edhr-batch-detail__process-evidence-context') &&
    detail.includes('edhr-batch-detail__process-evidence-groups') &&
    detail.includes('edhr-batch-detail__process-evidence-group-title'),
  '当前工序操作台必须有工序上下文条、分组容器和分组标题样式'
)
assert(
  detail.includes('selectedProcessEvidenceItems') && detail.includes('openSelectedProcessEvidence'),
  '融合详情页必须按选中工序生成证据入口并统一跳转'
)
assert(
  [
    '工作任务',
    '签名记录',
    '审批记录',
    '字段审计',
    '操作审计',
    '变更记录',
    '统一变更',
    '主数据追溯',
    '历史同工序',
    '独立表单',
    '记录本填写'
  ].every((label) => detail.includes(label)),
  '建议合并和部分建议合并的证据类型必须都出现在工序证据链中'
)
assert(
  detail.includes('/mes/pro/feedback/edhr-work-task') &&
    detail.includes("viewMode: 'tracking'") &&
    detail.includes('/mes/pro/feedback/edhr-domain-trace/detail') &&
    detail.includes("key: 'record-change'") &&
    detail.includes("path: '/mes/pro/feedback/edhr-form-trace'") &&
    detail.includes("tab: 'change'") &&
    detail.includes('/mes/pro/feedback/edhr-unified-change') &&
    detail.includes('/mes/pro/feedback/edhr-batch-history') &&
    detail.includes('/mes/pro/feedback/edhr-form') &&
    detail.includes('/mes/pro/feedback/edhr-execution/form') &&
    detail.includes('RECORDBOOK_UNRESTRICTED_FILL_MODE'),
  '工序证据链必须保留完整功能入口，记录本填写需复用批次执行表单而不是进入独立记录本页'
)
assert(
  !detail.includes("key: 'tracking'") &&
    !detail.includes("label: '执行追踪'") &&
    !detail.includes("tab: 'audit'"),
  '表单追溯审计 tab 移除后，批次详情不得保留跳往该 tab 的执行追踪入口'
)
assert(
  detail.includes('buildSelectedProcessEvidenceQuery') &&
    detail.includes('routeProcessId') &&
    detail.includes('batchExecutionId') &&
    detail.includes('executionId'),
  '工序证据链跳转必须携带工序/批次/执行上下文，不能只做静态链接'
)

console.log('edhr process evidence fusion static contract passed')
