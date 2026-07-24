const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert(fs.existsSync(detailPath), '批次详情页必须存在。')

const detail = fs.readFileSync(detailPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

const releaseWorkspace = sliceBetween(
  detail,
  'aria-label="放行预检工作区"',
  '<el-empty v-else-if="!selectedProcessContext"',
  '放行预检工作区'
)

assert.match(releaseWorkspace, /edhr-batch-detail__release-precheck-workspace/, '放行主区域必须渲染预检工作区。')
assert.match(releaseWorkspace, /放行预检/, '预检工作区必须显示放行预检标题。')
assert.match(releaseWorkspace, /@click="handleReleasePrecheck"/, '预检工作区顶部按钮必须触发真实放行预检。')
assert.match(releaseWorkspace, />\s*预检\s*</, '预检按钮文案必须为“预检”。')
assert.match(releaseWorkspace, /v-loading="releaseCheckLoading"/, '预检列表必须暴露加载状态。')
assert.match(releaseWorkspace, /:data="releaseCheckItems"/, '预检列表必须直接绑定 releaseCheckItems。')
assert.match(releaseWorkspace, /resolveReleaseCheckResultLabel/, '预检列表必须展示检查结果。')
assert.match(releaseWorkspace, /failureReason/, '预检列表必须展示失败原因。')
assert.match(releaseWorkspace, /remediationSuggestion/, '预检列表必须展示处理建议。')
assert.doesNotMatch(
  releaseWorkspace,
  /放行流程|releaseFlowStepsViewModel|edhr-batch-detail__release-flow-card|handleReleasePrecheckPass|handleReleasePrecheckReject|>\s*通过\s*<|>\s*驳回\s*</,
  '放行主区域必须是预检列表，不得再展示旧流程卡片或通过/驳回预检按钮。'
)

const precheckHandlerMatch = detail.match(
  /const\s+handleReleasePrecheck\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?const\s+loadReleaseCheckItems/
)
assert.ok(precheckHandlerMatch, '放行预检执行处理函数必须存在并位于预检列表加载函数之前。')

const precheckHandler = precheckHandlerMatch[0]
assert.match(
  precheckHandler,
  /precheckEdhrRelease\(\{\s*batchExecutionId\s*\}\)/,
  '批次详情页执行放行预检必须只按 batchExecutionId 触发，避免旧 releaseTransactionId 污染后端状态。'
)
assert.match(
  precheckHandler,
  /await loadDetail\(\)[\s\S]*await loadReleaseCheckItems\(\)/,
  '预检成功后必须刷新批次详情并重新加载预检列表。'
)
assert.doesNotMatch(
  precheckHandler,
  /releaseTransactionId\s*:/,
  '等待放行预检状态下重新执行预检不得传 releaseTransactionId。'
)

const loaderMatch = detail.match(
  /const\s+loadReleaseCheckItems\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?const\s+openReleaseCheckGroup/
)
assert.ok(loaderMatch, '必须提供预检列表加载函数。')
assert.match(loaderMatch[0], /getEdhrReleaseCheckItemPage/, '预检列表加载必须调用真实检查项分页接口。')
assert.match(loaderMatch[0], /releaseCheckItems\.value = page\.list \|\| \[\]/, '预检列表加载成功后必须刷新 releaseCheckItems。')
assert.match(loaderMatch[0], /releaseCheckItems\.value = \[\]/, '无放行事务或加载失败时必须清空旧预检列表。')

assert.doesNotMatch(
  detail,
  /<el-drawer v-model="releaseCheckDrawerVisible" title="放行预检"/,
  '放行预检不再使用右侧抽屉，必须在放行主区域单页展示。'
)

console.log('PASS: eDHR release precheck list workspace static contract')
