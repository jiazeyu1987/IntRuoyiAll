const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const releaseRailMatch = detail.match(
  /<template v-if="isReleaseProcessSelected">([\s\S]*?)<\/template>\s*<template v-else>/
)

assert(releaseRailMatch, '批次详情必须在右侧栏保留放行虚拟工序参数区')
assert(!detail.includes('<section class="edhr-batch-detail__closing"'), '批次详情不得继续保留底部收尾/放行归档操作区')

const releaseRailBlock = releaseRailMatch[1]
assert(
  releaseRailBlock.includes('releaseStageActionItems') &&
    releaseRailBlock.includes('{{ action.label }}') &&
    releaseRailBlock.includes('@click="action.onClick"'),
  '放行参数区必须通过 releaseStageActionItems 渲染阶段动作入口'
)

const stageActionNames = ['关闭批次', '质量拒收', '申请重开原记录', '归档打印', '放行审批', '放行预检']

for (const name of stageActionNames) {
  assert(detail.includes(`label: '${name}'`), `放行参数区必须保留阶段动作入口：${name}`)
}
assert(
  /class="edhr-batch-detail__release-flow-trace-action"[\s\S]*@click="openTraceRecordGroup"[\s\S]*追溯记录/.test(detail),
  '追溯记录必须保留为放行流程区域的独立入口'
)

const directButtonTexts = Array.from(releaseRailBlock.matchAll(/<el-button[\s\S]*?>([^<{}]+)<\/el-button>/g))
  .map((match) => match[1].replace(/\s+/g, ' ').trim())
  .filter(Boolean)

assert.deepStrictEqual(
  directButtonTexts,
  [],
  `放行参数区顶层按钮必须由阶段动作集合驱动，当前仍有静态按钮：${directButtonTexts.join('、')}`
)

const flattenedLabels = [
  '关闭批次',
  '生成归档',
  '下载',
  '放行预检',
  '放行事件',
  '放行审批',
  '批准放行',
  '驳回放行',
  '撤回放行',
  '质量拒收',
  '申请重开原记录',
  '变更记录',
  '操作审计',
  '域追溯'
]

for (const label of flattenedLabels) {
  assert(!directButtonTexts.includes(label), `放行参数区不得继续平铺动作按钮：${label}`)
}

const requiredBindings = [
  'terminalReleaseActionItems',
  'openArchivePrintDrawer',
  'archivePrintDrawerVisible.value = true',
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
  'command="submit"',
  'command="approve"',
  'command="reject"',
  'command="withdraw"',
  'openQualityRejectDialog',
  'openReopenBatchDialog',
  'FormTraceChangeTab',
  'OperationAuditListPane',
  'DomainTraceListPane'
]

for (const binding of requiredBindings) {
  assert(detail.includes(binding), `放行参数分组不得移除原动作绑定：${binding}`)
}

assert(detail.includes('closingReleaseActionLabel'), '放行审批主入口必须按状态显示当前可执行动作')
assert(detail.includes('edhr-batch-detail__release-precheck'), '放行预检入口必须在同一页面合并预检结果和检查项')
assert(detail.includes('traceRecordTab'), '追溯记录入口必须合并事件、变更、审计和域追溯页签')

console.log('edhr closing action groups static contract passed')
