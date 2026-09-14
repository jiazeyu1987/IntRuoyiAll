const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runnerSource = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')
const harnessPath = path.join(root, 'scripts/codex-test-playwright-harness.cjs')

assert.equal(
  fs.existsSync(harnessPath),
  true,
  'Runner 必须提供可复用 Playwright harness，不能让每个测试节点重复生成完整登录、deadline、截图、checkpoint 和 Element Plus helper。'
)

const harnessSource = fs.existsSync(harnessPath) ? fs.readFileSync(harnessPath, 'utf8') : ''

assert.match(
  harnessSource,
  /module\.exports\s*=\s*\{[\s\S]*createCodexTestPlaywrightHarness/s,
  'Playwright harness 必须导出 createCodexTestPlaywrightHarness，供临时场景脚本直接 require。'
)

assert.match(
  harnessSource,
  /typeof\s+rowEntry\.locator\s*===\s*'function'[\s\S]*rowEntry\.row[\s\S]*rowEntry\.locator/s,
  'Playwright harness 处理表格行时必须区分 Locator 本体和 { row, locator, text } 包装对象，不能把包装对象误当 Locator。'
)

assert.match(
  harnessSource,
  /findClickableCodeEntry[\s\S]*browserPage\.locator\([\s\S]*button\.el-button\.is-link[\s\S]*\[role="link"\][\s\S]*routeCode/s,
  'Playwright harness 打开路线详情时必须在目标行找不到编码 link 后，从全页可见 link/button 候选按 routeCode 兜底匹配。'
)

for (const helperName of [
  'captureScreenshot',
  'recordCheckpoint',
  'printOutputAndExit',
  'clickVisibleTextAction',
  'clickRouteRowAction',
  'clickDialogBusinessAction',
  'runBrowserFlow'
]) {
  assert.match(
    harnessSource,
    new RegExp(helperName),
    `Playwright harness 必须集中提供 ${helperName}，避免 Codex 在每个节点脚本中重复生成该 helper。`
  )
}

assert.match(
  runnerSource,
  /const PLAYWRIGHT_HARNESS_PATH\s*=/,
  'Runner 必须定义稳定的官方 Playwright harness 路径。'
)
assert.match(
  runnerSource,
  /Official reusable Playwright harness:[\s\S]*\$\{PLAYWRIGHT_HARNESS_PATH\}/,
  'Runner prompt 必须把官方 harness 绝对路径传给 Codex 子任务。'
)
assert.match(
  runnerSource,
  /require\(\$\{JSON\.stringify\(PLAYWRIGHT_HARNESS_PATH\)\}\)/,
  'Runner prompt 必须给出可直接复制的 require(harnessPath) 用法。'
)
assert.match(
  runnerSource,
  /Do not reimplement shared helpers[\s\S]*captureScreenshot[\s\S]*recordCheckpoint[\s\S]*clickVisibleTextAction[\s\S]*clickRouteRowAction[\s\S]*clickDialogBusinessAction/s,
  'Runner prompt 必须禁止临时脚本重复实现公共 helper。'
)
assert.match(
  runnerSource,
  /Keep the temporary scenario script under 250 lines and 12000 bytes/,
  'Runner prompt 必须约束临时场景脚本体量，防止再次生成 50-80KB 独立脚本。'
)
assert.match(
  runnerSource,
  /createCodexTestPlaywrightHarness\(\{[\s\S]*checkpointCount:[\s\S]*task\.checkpoints\.length/s,
  'Runner prompt 必须要求临时脚本通过 createCodexTestPlaywrightHarness 初始化场景执行上下文。'
)

console.log('PASS: Codex runner short-script harness static contract')
