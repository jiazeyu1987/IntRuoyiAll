const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

const releasePreviewBlock = sliceBetween(
  detail,
  'aria-label="当前工序表单"',
  '<el-empty v-else-if="!selectedProcessContext"',
  '中间主区域模板'
)
assert.doesNotMatch(
  releasePreviewBlock,
  /批次当前位置摘要|当前位置：|edhr-batch-detail__batch-position-card|edhr-batch-detail__release-summary-card/,
  '截图红框中的当前位置摘要在放行页不应再渲染。'
)

const rightRailBlock = sliceBetween(
  detail,
  '<template v-if="isReleaseProcessSelected">',
  '<template v-else>',
  '放行右侧面板'
)
assert.match(rightRailBlock, /v-for="action in releaseStageActionItems"/, '右侧动作必须继续来自真实阶段动作模型。')
assert.match(
  rightRailBlock,
  /aria-label="当前放行负责人"[\s\S]*当前放行负责人：\{\{\s*releaseStageOwnerLabel\s*\}\}/,
  '截图红框必须显示当前放行负责人。'
)
assert.doesNotMatch(
  rightRailBlock,
  /当前只展示本阶段需要处理的动作/,
  '截图红框不得继续显示泛化阶段说明。'
)
assert.doesNotMatch(
  rightRailBlock,
  /放行参数/,
  '截图红框不得显示“放行参数”标签。'
)
assert.match(
  rightRailBlock,
  /edhr-batch-detail__release-image-action/,
  '截图黄框中的拒收和放行必须使用图片按钮样式。'
)
assert.match(
  rightRailBlock,
  /edhr-batch-detail__release-image-action-visual/,
  '图片按钮必须包含独立图片视觉元素，而不是普通纯色按钮。'
)
assert.doesNotMatch(
  rightRailBlock,
  /<el-button[\s\S]*\{\{\s*action\.label\s*\}\}[\s\S]*<\/el-button>/,
  '截图黄框不得继续使用普通 Element Plus 文字按钮渲染拒收和放行。'
)

const styleBlock = sliceBetween(
  detail,
  '.edhr-batch-detail__release-image-action',
  '.edhr-batch-detail__release-stage-panel',
  '放行图片按钮样式'
)
assert.match(styleBlock, /background-image:/, '图片按钮样式必须声明图片背景。')
assert.match(styleBlock, /is-release-reject/, '拒收图片按钮必须有独立视觉状态。')
assert.match(styleBlock, /is-release-signature/, '放行图片按钮必须有独立视觉状态。')

assert.match(
  detail,
  /const releaseStageOwnerLabel = computed\([\s\S]*viewedReleaseStageViewModel\.value\.nextOwnerLabel[\s\S]*'当前阶段责任人'/,
  '当前放行负责人必须复用阶段负责人模型。'
)
assert.doesNotMatch(
  sliceBetween(
    detail,
    'const releaseStageOwnerLabel = computed(',
    'const clearReleaseActionErrorAutoHideTimer',
    '放行负责人计算'
  ),
  /stageOwnerRole/,
  '当前放行负责人不得继续兜底显示 stageOwnerRole。'
)

console.log('PASS: eDHR release screenshot action buttons static contract')
