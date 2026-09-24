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

console.log('PASS: active-order NCR evidence layout static contract')
