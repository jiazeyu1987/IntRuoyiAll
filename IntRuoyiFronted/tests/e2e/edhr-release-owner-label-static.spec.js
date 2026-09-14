const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/batchExecution.ts')
const detail = fs.readFileSync(detailPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

assert.match(api, /releaseOwnerConfigured\?:\s*boolean/, '工作台放行摘要类型必须包含 releaseOwnerConfigured。')
assert.match(api, /releaseOwnerSourceType\?:\s*string/, '工作台放行摘要类型必须包含 releaseOwnerSourceType。')
assert.match(api, /releaseOwnerLabel\?:\s*string/, '工作台放行摘要类型必须包含 releaseOwnerLabel。')

assert.match(
  detail,
  /const resolveReleaseOwnerLabel = \([^)]*\) =>[\s\S]*releaseSummary\?\.releaseOwnerLabel/,
  '批次详情必须从 releaseSummary.releaseOwnerLabel 解析放行负责人。'
)
assert.match(
  detail,
  /const resolveReleaseOwnerLabel = \(\) =>[\s\S]*return releaseOwnerLabel \|\| '放行责任人未配置'/,
  '放行负责人字段缺失时必须显示明确的未配置状态。'
)

const releaseApprovalBlock = sliceBetween(detail, "case 'release-approval':", "case 'archive':", '放行审批阶段模型')
assert.match(releaseApprovalBlock, /nextOwnerLabel:\s*resolveReleaseOwnerLabel\(/, '放行审批阶段负责人必须使用 releaseOwnerLabel。')
assert.doesNotMatch(releaseApprovalBlock, /stageOwnerRole/, '放行审批阶段不得用 stageOwnerRole 兜底负责人。')

const precheckBlock = sliceBetween(detail, "case 'precheck':", 'default:', '放行预检阶段模型')
assert.match(precheckBlock, /nextOwnerLabel:\s*resolveReleaseOwnerLabel\(/, '放行预检阶段负责人必须使用 releaseOwnerLabel。')
assert.doesNotMatch(precheckBlock, /stageOwnerRole/, '放行预检阶段不得用 stageOwnerRole 兜底负责人。')

const ownerComputedBlock = sliceBetween(
  detail,
  'const releaseStageOwnerLabel = computed(',
  'const clearReleaseActionErrorAutoHideTimer',
  '放行右侧负责人计算'
)
assert.doesNotMatch(
  ownerComputedBlock,
  /stageOwnerRole/,
  '当前放行负责人计算不得继续静默兜底 stageOwnerRole。'
)

console.log('PASS: eDHR release owner label static contract')
