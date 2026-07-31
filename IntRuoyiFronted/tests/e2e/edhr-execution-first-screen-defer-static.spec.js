const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr', 'ExecutionPage.vue')
const page = fs.readFileSync(pagePath, 'utf8')

const extractFunction = (name) => {
  const start = page.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const nextConst = page.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `函数 ${name} 后必须存在后续声明，便于静态截取。`)
  return page.slice(start, nextConst)
}

const loadExecution = extractFunction('loadExecution')

assert.match(
  loadExecution,
  /await\s+ProFeedbackApi\.getEdhrExecution\(/,
  '进入批记录表单首屏必须等待真实执行详情接口，用详情快照先渲染填写工作区。'
)

for (const forbiddenBlockingCall of [
  'loadLatestArchive',
  'loadTrackingAndSignatures',
  'getLatestEdhrExecutionArchive',
  'getEdhrTrackingTimeline',
  'getEdhrExecutionSignaturePage'
]) {
  assert.ok(
    !loadExecution.includes(forbiddenBlockingCall),
    `进入批记录表单首屏不得等待非首屏链路：${forbiddenBlockingCall}`
  )
}

assert.match(
  page,
  /const deferExecutionSecondaryLoad = \([\s\S]*requestAnimationFrame[\s\S]*void loadExecutionSecondaryData/,
  '归档、追踪时间线和签名摘要必须在填写工作区首屏渲染后异步调度。'
)

const secondaryLoader = extractFunction('loadExecutionSecondaryData')
for (const requiredDeferredCall of ['loadLatestArchive', 'loadTrackingAndSignatures']) {
  assert.ok(
    secondaryLoader.includes(requiredDeferredCall),
    `辅助加载函数必须负责延后加载：${requiredDeferredCall}`
  )
}

assert.match(
  page,
  /const isStaleExecutionPageRequest = \(requestSerial: number\)[\s\S]*requestSerial !== executionPageRequestSerial/,
  '执行页必须用请求序号阻止旧批记录表单请求回写。'
)

assert.match(
  page,
  /const cancelDeferredExecutionSecondaryLoad = \(\) =>[\s\S]*cancelAnimationFrame/,
  '执行页离开或刷新时必须取消尚未执行的首屏后置任务。'
)

console.log('PASS: eDHR execution form first screen defers archive, tracking, and signatures.')
