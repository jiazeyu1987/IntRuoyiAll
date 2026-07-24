const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const extractFunction = (name) => {
  const start = pageSource.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const nextConst = pageSource.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `函数 ${name} 后必须存在后续声明，便于静态截取。`)
  return pageSource.slice(start, nextConst)
}

const mountedBlockMatch = pageSource.match(/onMounted\(async \(\) => \{[\s\S]*?\n\}\)/)
assert.ok(mountedBlockMatch, '排产员工作台必须保留异步 onMounted 入口。')
const mountedBlock = mountedBlockMatch[0]

assert.match(
  mountedBlock,
  /await\s+loadSummary\(/,
  '排产员工作台首屏必须等待真实 summary 接口，先展示主工作台框架。'
)
for (const forbiddenBlockingCall of [
  'Promise.all([loadSummary(), loadProcessWipStatistics()])',
  'await loadProcessWipStatistics',
  'getProcessWipStatistics'
]) {
  assert.ok(
    !mountedBlock.includes(forbiddenBlockingCall),
    `首屏 onMounted 不得等待非首屏工序在制统计链路：${forbiddenBlockingCall}`
  )
}

assert.match(
  pageSource,
  /const deferSchedulerWorkbenchSecondaryLoad = \([\s\S]*requestAnimationFrame[\s\S]*loadSchedulerWorkbenchSecondaryData/,
  '工序在制统计必须在 summary 首屏渲染后异步调度。'
)

const secondaryLoader = extractFunction('loadSchedulerWorkbenchSecondaryData')
assert.ok(
  secondaryLoader.includes('loadProcessWipStatistics'),
  '辅助加载函数必须负责延后加载工序在制统计。'
)

assert.match(
  pageSource,
  /const isStaleSchedulerWorkbenchRequest = \(requestSerial: number\)[\s\S]*requestSerial !== schedulerWorkbenchRequestSerial/,
  '排产员工作台必须用请求序号阻止旧请求回写。'
)
assert.match(
  pageSource,
  /cancelDeferredSchedulerWorkbenchSecondaryLoad[\s\S]*cancelAnimationFrame/,
  '排产员工作台必须取消未执行的首屏后延迟任务。'
)
assert.match(
  pageSource,
  /v-loading="processWipLoading"/,
  '工序在制统计延后加载期间必须在列表区域显示加载状态，而不是全页阻塞。'
)
assert.match(
  pageSource,
  /processWipErrorMessage[\s\S]*加载工序在制统计失败/,
  '工序在制统计延后加载失败必须在页面暴露错误信息，不得静默失败。'
)

console.log('PASS: scheduler workbench first screen defers process WIP statistics.')
