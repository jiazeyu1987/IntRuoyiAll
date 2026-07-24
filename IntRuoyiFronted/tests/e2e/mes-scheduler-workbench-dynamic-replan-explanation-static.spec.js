const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/task/autoSchedule/index.ts'),
  'utf8'
)

assert.match(apiSource, /getLatestReplanExplanation/)
assert.match(apiSource, /\/mes\/pro\/auto-schedule\/replan\/explanation\/latest/)
assert.match(apiSource, /ProTaskReplanExplanationRespVO/)

for (const symbol of [
  'replanExplanationLoading',
  'replanExplanationError',
  'loadLatestReplanExplanation',
  'watch(activeWipTab',
  "window.addEventListener('focus'",
  "window.removeEventListener('focus'"
]) {
  assert.match(pageSource, new RegExp(symbol.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
}

assert.doesNotMatch(pageSource, /setInterval\s*\(/, '排产逻辑刷新不得使用定时轮询')

for (const text of [
  '最近一次成功重排',
  '订单顺序',
  '拆分工序',
  '计算产能',
  '受保护任务',
  '物料需求',
  '需要数量',
  '可用数量',
  '缺少数量',
  '暂无已应用的重排记录',
  '加载重排说明失败'
]) {
  assert.match(pageSource, new RegExp(text), `动态排产说明缺少：${text}`)
}

console.log('mes-scheduler-workbench-dynamic-replan-explanation-static: PASS')
