const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const releaseRailMatch = detail.match(
  /<template v-if="isReleaseProcessSelected">([\s\S]*?)<\/template>\s*<template v-else>/
)

assert.ok(releaseRailMatch, '批次详情必须在放行虚拟工序的右侧操作区展示阶段动作。')
assert.doesNotMatch(detail, /<section class="edhr-batch-detail__closing"/, '批次详情不得继续保留底部收尾/放行归档操作区。')

const releaseRailBlock = releaseRailMatch[1]
assert.match(
  releaseRailBlock,
  /v-for="action in releaseStageActionItems"[\s\S]*\{\{ action\.label \}\}/,
  '放行操作区按钮必须由 releaseStageActionItems 动态渲染。'
)

const directButtonTexts = Array.from(releaseRailBlock.matchAll(/<el-button[\s\S]*?>([^<{}]+)<\/el-button>/g))
  .map((match) => match[1].replace(/\s+/g, ' ').trim())
  .filter(Boolean)

assert.deepEqual(
  directButtonTexts,
  [],
  `放行操作区不得保留写死的顶层按钮，当前仍有：${directButtonTexts.join('、')}`
)

for (const label of ['关闭批次', '质量拒收', '申请重开原记录', '归档打印', '放行预检', '放行审批']) {
  assert.match(detail, new RegExp(`label: '${label}'`), `放行阶段动作集合必须保留：${label}`)
}
assert.match(
  detail,
  /class="edhr-batch-detail__release-flow-trace-action"[\s\S]*@click="openTraceRecordGroup"[\s\S]*追溯记录/,
  '追溯记录必须保留为放行流程区域的独立入口。'
)

for (const forbidden of ['终态处理', '放行检查', '体验检查', 'openTerminalActionDrawer', 'uxChecklistDrawerVisible = true']) {
  assert.doesNotMatch(releaseRailBlock, new RegExp(forbidden), `放行操作区不得继续保留旧入口：${forbidden}`)
}

const requiredBindings = [
  'terminalReleaseActionItems',
  'openArchivePrintDrawer',
  'openReleaseCheckGroup',
  'releaseApprovalDrawerVisible.value = true',
  'openTraceRecordGroup',
  'openCloseDialog',
  'handleGenerateArchive',
  'handleDownloadArchive',
  'handleReleasePrecheck',
  'openReleaseCheckItems',
  'ReleaseEventListPane',
  'handleReleaseDropdownCommand',
  'openReleaseSignatureConfirmDialog',
  'confirmReleaseSignatureSubmit',
  'openPrimaryReleaseAction',
  'openReleaseTransactionDialog(command)',
  'openQualityRejectDialog',
  'openReopenBatchDialog',
  'FormTraceChangeTab',
  'OperationAuditListPane',
  'DomainTraceListPane'
]

for (const binding of requiredBindings) {
  assert.match(detail, new RegExp(binding.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `放行阶段动作不得移除原操作绑定：${binding}`)
}

console.log('edhr closing action compact copy static contract passed')
