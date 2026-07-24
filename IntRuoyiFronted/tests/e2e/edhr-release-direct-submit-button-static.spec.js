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

const actionBuilder = sliceBetween(
  detail,
  'const buildReleaseStageActionItems = (stageKey: ReleaseStageKey)',
  'const releaseStageActionItems',
  '放行阶段动作构建函数'
)

assert.match(
  actionBuilder,
  /key: 'release-reject'[\s\S]*label: '拒收'[\s\S]*permission: \['mes:pro-edhr-batch-execution:quality-reject'\][\s\S]*disabled: !canQualityReject\.value[\s\S]*onClick: openQualityRejectDialog/,
  '右侧必须提供“拒收”按钮，并绑定现有质量拒收动作。'
)
assert.match(
  actionBuilder,
  /key: 'release-signature'[\s\S]*label: '放行'[\s\S]*permission: \['mes:pro-edhr-release:submit'\][\s\S]*disabled: !canSubmitRelease\.value[\s\S]*onClick: openReleaseSignatureConfirmDialog/,
  '右侧必须提供“放行”按钮，并打开电子签名确认弹窗。'
)
assert.match(
  actionBuilder,
  /return buildReleaseDecisionActionItems\(\)/,
  '放行可操作阶段必须统一使用拒收/放行两个动作。'
)
assert.doesNotMatch(
  actionBuilder,
  /key: 'close-batch'|label: '关闭批次'|terminalReleaseActionItems|key: 'release-approval'|key: 'release-approval-signature'|label: '放行审批'|openReleaseApprovalGroup/,
  '右侧动作模型不得再保留关闭批次、旧放行审批或旧终端动作组入口。'
)

const rightRailBlock = sliceBetween(
  detail,
  '<template v-if="isReleaseProcessSelected">',
  '<template v-else>',
  '放行右侧面板'
)
assert.match(rightRailBlock, /v-for="action in releaseStageActionItems"/, '右侧按钮必须仍由阶段动作模型统一渲染。')
assert.doesNotMatch(
  rightRailBlock,
  /关闭批次|放行审批|releaseApprovalDrawerVisible|openPrimaryReleaseAction/,
  '放行右侧面板不得直接出现关闭批次或旧放行审批入口。'
)

assert.match(
  detail,
  /<Dialog\s+title="电子签名确认"[\s\S]*v-model="releaseSignatureConfirmVisible"[\s\S]*label="电子签名"[\s\S]*v-model="releaseSignatureForm\.password"[\s\S]*type="password"[\s\S]*show-password[\s\S]*@keyup\.enter="confirmReleaseSignatureSubmit"[\s\S]*确认放行/,
  '放行必须使用专用电子签名确认弹窗，包含电子签名密码输入并绑定确认提交。'
)

const qualityRejectDialog = sliceBetween(
  detail,
  '<Dialog title="质量拒收电子批记录批次"',
  '</Dialog>',
  '质量拒收弹窗'
)
assert.match(qualityRejectDialog, /label="签名密码"[\s\S]*v-model="qualityRejectForm\.password"[\s\S]*type="password"[\s\S]*show-password/, '拒收弹窗必须要求签名密码。')
assert.match(qualityRejectDialog, /qualityRejectSignatureTimeForm/, '拒收弹窗必须保留签名显示时间。')

assert.match(
  detail,
  /const openReleaseSignatureConfirmDialog = async \(\) => \{[\s\S]*ensureViewedReleaseStageWritable\('放行'\)[\s\S]*!canSubmitRelease\.value[\s\S]*ensurePendingSpecialNodeAttachmentsSavedBeforeRelease\(\)[\s\S]*releaseSignatureForm\.idempotencyKey = buildReleaseIdempotencyKey\('submit'\)[\s\S]*releaseSignatureConfirmVisible\.value = true/,
  '点击“放行”必须先校验权限和附件，再打开专用电子签名确认弹窗。'
)

assert.match(
  detail,
  /const confirmReleaseSignatureSubmit = async \(\) => \{[\s\S]*ensureViewedReleaseStageWritable\('放行确认'\)[\s\S]*releaseSignatureForm\.password\.trim\(\)[\s\S]*releaseTransactionForm\.password = releaseSignatureForm\.password[\s\S]*releaseTransactionForm\.idempotencyKey = releaseSignatureForm\.idempotencyKey[\s\S]*runReleaseSignatureConfirmAction\(\(\) => submitReleaseByOwnerSignature\(releaseTransactionId\), '放行已完成'\)/,
  '电子签名确认后必须复用现有直接放行提交函数，而不是打开旧弹框。'
)

assert.match(
  detail,
  /const runReleaseSignatureConfirmAction = async \(action: \(\) => Promise<unknown>, successText: string\) => \{[\s\S]*releaseSignatureSubmitting\.value = true[\s\S]*releaseSignatureConfirmVisible\.value = false[\s\S]*message\.success\(successText\)[\s\S]*await loadDetail\(\)[\s\S]*message\.error\(releaseSignatureError\.value\)/,
  '电子签名确认提交必须显示真实错误、成功后关闭专用弹窗并刷新详情。'
)

console.log('PASS: eDHR release reject/release signature button static contract')
