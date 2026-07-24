const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const extractFunction = (name) => {
  const start = detail.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const nextConst = detail.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `函数 ${name} 后必须存在后续声明，便于静态截取。`)
  return detail.slice(start, nextConst)
}

const loadDetail = extractFunction('loadDetail')

assert.match(
  loadDetail,
  /await\s+getEdhrBatchExecution\(id\)/,
  '首屏详情加载必须只等待批次详情接口，用详情数据先渲染基础信息和工序框架。'
)

for (const forbiddenBlockingCall of [
  'getEdhrBatchWorkbench',
  'loadReviewTimeline',
  'loadTaskPreview',
  'getEdhrBatchTaskPreview'
]) {
  assert.ok(
    !loadDetail.includes(forbiddenBlockingCall),
    `首屏 loadDetail 不得等待非首屏重型链路：${forbiddenBlockingCall}`
  )
}

assert.match(
  detail,
  /const deferInitialBatchDetailSecondaryLoad = \([\s\S]*requestAnimationFrame[\s\S]*void loadBatchDetailSecondaryData/,
  '辅助 workbench、放行动作和复盘时间线必须在首屏详情渲染后异步调度。'
)

const secondaryLoader = extractFunction('loadBatchDetailSecondaryData')
for (const requiredDeferredCall of [
  'getEdhrBatchWorkbench',
  'loadReviewTimeline'
]) {
  assert.ok(
    secondaryLoader.includes(requiredDeferredCall),
    `辅助加载函数必须负责延后加载：${requiredDeferredCall}`
  )
}

const loadReviewTimeline = extractFunction('loadReviewTimeline')
assert.ok(
  !loadReviewTimeline.includes('await loadTaskPreview'),
  '复盘时间线加载不得继续等待批记录表单预览，避免辅助链路二次阻塞。'
)
assert.match(
  detail,
  /const deferTaskPreviewLoad = \([\s\S]*requestAnimationFrame[\s\S]*void loadTaskPreview/,
  '批记录只读预览必须在选中任务后单独异步调度。'
)

console.log('PASS: eDHR batch execution first screen defers heavy secondary detail data.')
