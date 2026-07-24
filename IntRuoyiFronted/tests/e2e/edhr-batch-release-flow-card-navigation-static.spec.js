const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detailPage = fs.readFileSync(detailPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

assert.match(
  detailPage,
  /const actualReleaseStageKey = computed\(resolveReleaseStageKey\)/,
  '当前真实阶段必须继续由 resolveReleaseStageKey 统一计算。'
)
assert.match(
  detailPage,
  /const viewedReleaseStageViewModel = computed<ReleaseStageViewModel>\(\(\) =>/,
  '右侧面板必须保留独立的查看阶段 ViewModel。'
)

const releaseWorkspace = sliceBetween(
  detailPage,
  'aria-label="放行预检工作区"',
  '<el-empty v-else-if="!selectedProcessContext"',
  '放行预检工作区'
)
assert.match(releaseWorkspace, /edhr-batch-detail__release-precheck-workspace/, '中间主区域必须改为放行预检工作区。')
assert.match(releaseWorkspace, /openTraceRecordGroup/, '追溯记录入口必须保留在放行主区域。')
assert.doesNotMatch(
  releaseWorkspace,
  /放行流程|releaseFlowStepsViewModel|selectReleaseFlowStep|edhr-batch-detail__release-flow-step/,
  '中间主区域不得再保留旧放行流程卡片。'
)

const rightRailBlock = sliceBetween(
  detailPage,
  '<template v-if="isReleaseProcessSelected">',
  '<template v-else>',
  '放行右侧面板'
)
assert.match(rightRailBlock, /viewedReleaseStageViewModel\.stageLabel/, '右侧标题必须按查看阶段切换。')
assert.match(
  rightRailBlock,
  /:disabled="action\.disabled"/,
  '右侧按钮必须由阶段动作模型统一输出禁用状态。'
)
assert.doesNotMatch(
  rightRailBlock,
  /追溯记录|trace-record|openTraceRecordGroup|关闭批次|放行审批/,
  '右侧阶段动作区不得重复展示追溯记录、关闭批次或旧放行审批入口。'
)

const actionItemsBlock = sliceBetween(
  detailPage,
  'const buildReleaseStageActionItems = (stageKey: ReleaseStageKey)',
  'type ProcessEvidenceItem',
  '放行右侧动作模型'
)
assert.match(actionItemsBlock, /viewedReleaseStageViewModel\.value\.key/, '右侧动作列表必须按查看阶段生成。')
assert.match(actionItemsBlock, /readonlyAllowed/, '只读阶段必须区分查看入口和推进动作。')
assert.match(actionItemsBlock, /isViewedReleaseStageReadonly\.value[\s\S]*!action\.readonlyAllowed[\s\S]*disabled: true/, '非当前阶段推进动作必须被统一禁用。')
assert.match(
  actionItemsBlock,
  /key: 'release-reject'[\s\S]*label: '拒收'[\s\S]*onClick: openQualityRejectDialog/,
  '放行阶段右侧必须提供拒收动作。'
)
assert.match(
  actionItemsBlock,
  /key: 'release-signature'[\s\S]*label: '放行'[\s\S]*onClick: openReleaseSignatureConfirmDialog/,
  '放行阶段右侧必须提供放行动作。'
)
assert.doesNotMatch(
  actionItemsBlock,
  /key: 'trace-record'|traceAction|key: 'close-batch'|label: '关闭批次'|key: 'release-approval'|label: '放行审批'|terminalReleaseActionItems/,
  '追溯记录、关闭批次和旧放行审批不得再混入右侧阶段动作模型。'
)

for (const guardedAction of [
  'openReopenBatchDialog',
  'submitReopenBatch',
  'handleReleasePrecheck',
  'openReleaseSignatureConfirmDialog',
  'confirmReleaseSignatureSubmit',
  'openQualityRejectDialog',
  'submitQualityReject',
  'handleGenerateArchive'
]) {
  assert.match(
    detailPage,
    new RegExp(`const ${guardedAction}[\\s\\S]*ensureViewedReleaseStageWritable\\(`),
    `${guardedAction} 必须在执行写入动作前检查非当前阶段只读态。`
  )
}

assert.doesNotMatch(
  detailPage,
  /normalFlowKeys|releaseFlowStepsViewModel|selectReleaseFlowStep|edhr-batch-detail__release-flow-card|edhr-batch-detail__release-flow-step/,
  '批次详情页不得再保留旧放行流程节点模型和样式。'
)

assert.doesNotMatch(
  detailPage,
  /关闭批次|关闭电子批记录批次|closeEdhrBatchExecution|openCloseDialog|submitClose|closeDialogVisible/,
  '批次详情页不得再保留页面级关闭批次文案、弹窗、入口或提交处理函数。'
)

console.log('PASS: eDHR release single-page precheck navigation static contract')
