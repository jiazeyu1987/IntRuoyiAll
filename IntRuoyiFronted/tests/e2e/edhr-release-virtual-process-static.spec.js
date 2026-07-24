const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(pagePath, 'utf8')

assert.match(
  detail,
  /RELEASE_VIRTUAL_PROCESS[\s\S]*label: '放行'[\s\S]*selectReleaseProcess/,
  '详情页必须定义并渲染一个固定的“放行”虚拟工序。'
)

assert.match(
  detail,
  /class="edhr-batch-detail__review-item edhr-batch-detail__release-process-item"[\s\S]*RELEASE_VIRTUAL_PROCESS\.label/,
  '左侧工序列表末尾必须展示“放行”虚拟工序。'
)

assert.match(
  detail,
  /const selectReleaseProcess = \(\) => \{[\s\S]*selectedReleaseStep\.value = true[\s\S]*selectedTaskId\.value = ''[\s\S]*selectedExecutionId\.value = ''[\s\S]*\}/,
  '选中放行时必须独立于真实工序选中态，并清空真实工序选择。'
)

assert.match(
  detail,
  /batchCurrentPositionViewModel[\s\S]*releaseFlowStepsViewModel[\s\S]*releaseStageActionItems/,
  '选中放行时必须按“当前位置摘要、放行流程图、当前阶段动作”三段职责展示。'
)

const mainReleaseSummaryStart = detail.indexOf('aria-label="批次当前位置摘要"')
const releaseFlowStart = detail.indexOf('aria-label="放行流程指示图"')
const railMatch = detail.match(/<aside class="edhr-batch-detail__review-rail"[\s\S]*?<\/aside>/)
assert.ok(mainReleaseSummaryStart > 0, '中间主区域必须保留批次当前位置摘要。')
assert.ok(releaseFlowStart > mainReleaseSummaryStart, '放行流程图必须位于批次当前位置摘要之后。')
assert.ok(railMatch, '详情页必须保留右侧当前工序摘要栏。')

const railBlock = railMatch[0]
const releaseRailMatch = railBlock.match(/<template v-if="isReleaseProcessSelected">([\s\S]*?)<\/template>\s*<template v-else>/)
assert.ok(releaseRailMatch, '右侧栏必须保留放行虚拟工序的阶段动作分支。')
const releaseRailBlock = releaseRailMatch[1]

assert.match(
  releaseRailBlock,
  /v-for="action in releaseStageActionItems"[\s\S]*\{\{ action\.label \}\}[\s\S]*@click="action\.onClick"/,
  '右侧放行操作区必须由 releaseStageActionItems 统一驱动。'
)

for (const label of ['关闭批次', '质量拒收', '申请重开', '归档打印', '放行预检', '放行审批']) {
  assert.match(detail, new RegExp(`label: '${label}'`), `必须保留当前阶段动作入口：${label}`)
}
assert.match(
  detail,
  /class="edhr-batch-detail__release-flow-trace-action"[\s\S]*@click="openTraceRecordGroup"[\s\S]*追溯记录/,
  '必须保留放行流程区域右下角的追溯记录入口。'
)

for (const forbidden of ['终态处理', '放行检查', '体验检查', 'openTerminalActionDrawer']) {
  assert.doesNotMatch(releaseRailBlock, new RegExp(forbidden), `右侧放行操作区不得再保留旧入口：${forbidden}`)
}

assert.doesNotMatch(
  releaseRailBlock,
  /放行状态|归档状态|下一步责任人|edhr-batch-detail__rail-summary/,
  '右侧放行操作区不得重复展示状态摘要或归档摘要。'
)

assert.doesNotMatch(
  detail,
  /<section class="edhr-batch-detail__closing"/,
  '详情页不应继续保留底部收尾/放行归档操作区。'
)

console.log('PASS: eDHR release virtual process static contract')
