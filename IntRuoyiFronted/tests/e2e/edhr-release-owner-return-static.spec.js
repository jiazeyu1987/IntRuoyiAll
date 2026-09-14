const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

const releaseImport = sliceBetween(
  detail,
  'getEdhrReleaseCheckItemPage',
  "import dayjs from 'dayjs'",
  '放行 API import'
)
assert.match(releaseImport, /rejectEdhrRelease/, '批次详情页必须导入正式放行退回 API rejectEdhrRelease。')

const actionBuilder = sliceBetween(
  detail,
  'function buildReleaseDecisionActionItems()',
  'const releaseStageActionItems',
  '放行决策动作构建'
)
assert.match(
  actionBuilder,
  /key: 'release-return'[\s\S]*label: '退回'[\s\S]*permission: \['mes:pro-edhr-release:reject'\][\s\S]*disabled: !canReturnRelease\.value[\s\S]*onClick: openReleaseReturnDialog/,
  '放行阶段必须提供正式“退回”动作，使用 mes:pro-edhr-release:reject 权限并打开放行退回弹窗。'
)
assert.match(
  actionBuilder,
  /key: 'quality-reject'[\s\S]*label: '质量拒收'[\s\S]*permission: \['mes:pro-edhr-batch-execution:quality-reject'\][\s\S]*disabled: !canQualityReject\.value[\s\S]*onClick: openQualityRejectDialog/,
  '质量拒收必须保留为独立动作，不能继续冒充放行退回。'
)
assert.doesNotMatch(
  actionBuilder,
  /key: 'release-reject'[\s\S]*label: '拒收'[\s\S]*qualityRejectEdhrBatchExecution|permission: \['mes:pro-edhr-batch-execution:quality-reject'\][\s\S]*label: '拒收'/,
  '旧“拒收”动作不得继续绑定质量拒收权限或质量拒收接口。'
)

assert.match(
  detail,
  /const canReturnRelease = computed\([\s\S]*\(\) =>[\s\S]*hasReleaseTransaction\.value[\s\S]*\['PRECHECK_PASSED', 'PENDING_APPROVAL'\]\.includes\(releaseStatus\.value\)[\s\S]*releaseCanSubmitBatchStatus\.value/,
  '放行退回应仅在已有放行事务、预检通过或审批中且批次处于可放行状态时可用。'
)

assert.match(
  detail,
  /<Dialog title="放行退回"[\s\S]*v-model="releaseReturnDialogVisible"[\s\S]*label="退回原因"[\s\S]*v-model="releaseReturnForm\.rejectReason"[\s\S]*@keyup\.enter="submitReleaseReturn"[\s\S]*确认退回/,
  '放行退回必须有独立弹窗、必填退回原因和确认退回动作。'
)

assert.match(
  detail,
  /const submitReleaseReturn = async \(\) => \{[\s\S]*ensureViewedReleaseStageWritable\('放行退回'\)[\s\S]*!canReturnRelease\.value[\s\S]*releaseReturnForm\.rejectReason\.trim\(\)[\s\S]*rejectEdhrRelease\(\{[\s\S]*releaseTransactionId[\s\S]*idempotencyKey: buildReleaseIdempotencyKey\('reject'\)[\s\S]*rejectReason: releaseReturnForm\.rejectReason\.trim\(\)[\s\S]*message\.success\('放行已退回'\)[\s\S]*await loadDetail\(\)/,
  '确认退回必须调用 rejectEdhrRelease，成功后刷新详情，不得调用质量拒收接口。'
)

const qualityRejectBlock = sliceBetween(
  detail,
  'const submitQualityReject = async () => {',
  'const handleOpenTask = async',
  '质量拒收提交函数'
)
assert.match(qualityRejectBlock, /qualityRejectEdhrBatchExecution/, '质量拒收仍必须调用质量拒收接口。')
assert.doesNotMatch(qualityRejectBlock, /rejectEdhrRelease/, '质量拒收函数不得调用放行退回 API。')

console.log('PASS: eDHR release owner return static contract')
