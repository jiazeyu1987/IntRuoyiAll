const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /const CODEX_CHILD_SETTLE_TIMEOUT_MS = Number\(process\.env\.CODEX_TEST_CHILD_SETTLE_TIMEOUT_MS \|\| '\d+'\)/,
  'Runner 必须配置独立的子进程收敛超时，不能无限等待 Windows wrapper 的 close 事件。'
)
assert.match(
  runner,
  /async function stopChildAndWait\([\s\S]*Promise\.race\(\[[\s\S]*childExitPromise[\s\S]*sleep\(CODEX_CHILD_SETTLE_TIMEOUT_MS\)/,
  '停止 Codex 进程树后必须在有界时间内等待 child close，并允许主循环继续收敛。'
)
assert.match(
  runner,
  /const stopRequestedPromise = new Promise[\s\S]*resolveStopRequested[\s\S]*const stopChild = \(\) => \{[\s\S]*stopPromise = stopChildAndWait\([\s\S]*resolveStopRequested\(stopPromise\)/,
  '停止请求必须把有界停止 Promise 接入当前执行等待链。'
)
assert.match(
  runner,
  /const childResult = await Promise\.race\(\[\s*childExitPromise,\s*stopRequestedPromise\s*\]\)/,
  '当前执行必须同时等待自然退出和停止收敛，不能只等待 close 事件。'
)
assert.match(
  runner,
  /catch \(error\) \{\s*const stopResult = await stopChild\(\)[\s\S]*throw error\s*\}/,
  '异常路径必须等待有界停止结果后再退出任务，确保 Runner 活动计数可归零。'
)
assert.doesNotMatch(
  runner,
  /const exitCode = await new Promise\(\(resolve, reject\) => \{[\s\S]*child\.once\('close', resolve\)[\s\S]*\}\)/,
  'Runner 不得继续无限等待 child close 事件。'
)
