const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const panelPath = path.join(
  repoRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8')

assert.match(
  panel,
  /data-active-order-operation-ncr-trace[\s\S]*data-active-order-operation-ncr-evidence/,
  '不合格评审明细必须有独立的结构化容器。'
)
assert.match(
  panel,
  /class="team-leader-workbench__operation-fact-evidence-grid"/,
  '不合格评审明细必须使用字段网格布局。'
)
for (const label of ['不合格原因', '评审意见', '处置结果', '电子签名']) {
  assert.match(
    panel,
    new RegExp(
      `class="team-leader-workbench__operation-fact-evidence-label">\\s*${label}\\s*<\\/span>`
    ),
    `${label} 必须作为独立字段标签展示。`
  )
}
assert.match(
  panel,
  /class="team-leader-workbench__operation-fact-evidence-value"[\s\S]*fact\.nonconformanceReason/,
  '不合格原因必须放在独立字段值节点中。'
)
assert.match(
  panel,
  /class="team-leader-workbench__operation-fact-evidence-value"[\s\S]*fact\.reviewOpinion/,
  '评审意见必须放在独立字段值节点中。'
)
assert.match(
  panel,
  /class="team-leader-workbench__operation-fact-evidence-value"[\s\S]*resolveNonconformanceDispositionLabel/,
  '处置结果必须放在独立字段值节点中。'
)
assert.match(
  panel,
  /data-active-order-summary-operation-signature[\s\S]*formatOperationFactSignatureText/,
  '电子签名动作和签名文本必须保留。'
)
assert.match(
  panel,
  /data-active-order-ncr-review-material-preview[\s\S]*material\.fileName/,
  '评审材料在线预览动作必须保留。'
)
assert.match(
  panel,
  /\.team-leader-workbench__operation-fact-evidence-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(2,\s*minmax\(0,\s*1fr\)\)/,
  '桌面布局必须使用两列等宽字段网格。'
)
assert.match(
  panel,
  /\.team-leader-workbench__operation-fact-evidence-value\s*\{[\s\S]*white-space:\s*normal[\s\S]*overflow-wrap:\s*anywhere/,
  '字段值必须允许长文本换行。'
)
assert.match(
  panel,
  /@media\s*\(max-width:\s*760px\)[\s\S]*\.team-leader-workbench__operation-fact-evidence-grid\s*\{[\s\S]*grid-template-columns:\s*1fr/,
  '窄屏布局必须切换为单列，避免字段挤压。'
)
assert.doesNotMatch(
  panel,
  /<span>不合格原因：\{\{[\s\S]*<span>评审意见：\{\{/,
  '不合格原因和评审意见不得继续作为连续 inline 文本展示。'
)
assert.match(
  panel,
  /label="偏差"[\s\S]*name="deviation"[\s\S]*data-active-order-deviation-tab/,
  '批记录详情必须提供独立的偏差页签。'
)
assert.equal((panel.match(/name="deviation"/g) || []).length, 1, '偏差页签只能出现一次。')
assert.match(panel, /v-for="batch in deviationBatches"[\s\S]*<DeviationTracePane :batch-execution-id="batch.batchExecutionId"/, '正式关联批记录必须逐个展示偏差追溯。')
assert.match(panel, /getDeviationBatchOptionsByActiveOrder\(activeOrderId!\)/, '必须通过正式活跃订单关联查询批记录。')
assert.doesNotMatch(panel, /nonconformanceOperationFacts/, '不能用不合格事件代替正式偏差。')
assert.match(panel, /v-if="deviationLoading"/, '关联查询必须展示加载状态。')
assert.match(panel, /v-else-if="deviationError"[\s\S]*@click="loadDeviationBatches"/, '关联查询失败必须展示可重试错误。')
assert.match(panel, /description="暂无正式批记录，无法查看偏差"[\s\S]*data-active-order-deviation-empty/, '未关联正式批记录不能误报没有偏差。')
const trace = fs.readFileSync(path.join(repoRoot, 'IntRuoyiFronted/src/views/mes/pro/edhr/components/DeviationTracePane.vue'), 'utf8')
assert.match(trace, /v-else-if="error"[\s\S]*@click="load"/, '正式偏差查询错误必须可见并允许重试。')
assert.match(trace, /v-else-if="!rows.length" description="没有偏差"/, '成功查询空结果才展示没有偏差。')
assert.match(trace, /getDeviationPage\(\{ pageNo: 1, pageSize: 200, batchExecutionId \}\)/, '偏差列表必须按正式批记录身份查询。')
console.log('PASS: active-order NCR evidence layout and formal deviation trace contract')
