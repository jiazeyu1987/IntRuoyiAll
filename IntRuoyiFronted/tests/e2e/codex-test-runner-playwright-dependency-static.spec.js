const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /function resolveFrontendNodePath\(\)[\s\S]*FRONTEND_PROJECT_ROOT[\s\S]*node_modules[\s\S]*NODE_PATH/,
  'Runner 必须把前端 node_modules 加入 Codex 子任务环境，确保隔离工作目录里的临时 Playwright 脚本能解析依赖。'
)
assert.match(
  runner,
  /spawn\(command, commandArgs, \{[\s\S]*env:\s*\{[\s\S]*\.\.\.process\.env[\s\S]*NODE_PATH:\s*resolveFrontendNodePath\(\)[\s\S]*\}/,
  'Runner 启动 codex exec 时必须显式传入包含前端 node_modules 的 NODE_PATH。'
)
assert.match(
  runner,
  /function resolveBrowserExecutablePath\(\)[\s\S]*PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH[\s\S]*chrome\.exe[\s\S]*msedge\.exe/,
  'Runner 必须解析本机 Chrome/Edge 可执行文件，不能要求隔离脚本依赖 Playwright 浏览器缓存。'
)
assert.match(
  runner,
  /spawn\(command, commandArgs, \{[\s\S]*env:\s*\{[\s\S]*PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH:\s*resolveBrowserExecutablePath\(\)[\s\S]*\}/,
  'Runner 启动 codex exec 时必须把浏览器可执行文件路径传给子任务。'
)
assert.match(
  runner,
  /Playwright dependency note:[\s\S]*require\('playwright'\)[\s\S]*FRONTEND_PROJECT_ROOT/,
  'Runner prompt 必须提醒子任务隔离目录已通过 NODE_PATH 暴露前端 Playwright 依赖，避免重复探索仓库依赖。'
)
assert.match(
  runner,
  /Browser executable path:[\s\S]*\$\{resolveBrowserExecutablePath\(\)\}[\s\S]*chromium\.launch\(\{ executablePath:/,
  'Runner prompt 必须明确要求临时 Playwright 脚本使用传入的浏览器 executablePath。'
)
assert.match(
  runner,
  /function resolveNavigationHints\(task\)[\s\S]*工艺路线[\s\S]*\/mes\/pro\/route[\s\S]*history/i,
  'Runner 必须按测试项文本提供正式页面导航提示，工艺路线不能继续猜测 hash 路由。'
)
assert.match(
  runner,
  /Navigation hints:[\s\S]*\$\{resolveNavigationHints\(task\)\}/,
  'Runner prompt 必须把正式页面导航提示传给 Codex 子任务。'
)
assert.match(
  runner,
  /Element Plus dialog\/drawer footer action buttons[\s\S]*entire visible dialog\/drawer or page[\s\S]*not only the field form scope/,
  'Runner prompt 必须提醒子任务保存/确定等 footer 按钮可能不在字段表单 scope 内，必须在整个可见弹窗/抽屉或页面中定位。'
)
assert.match(
  runner,
  /page\.locator\('\.el-dialog__footer button, \.el-drawer__footer button'\)[\s\S]*filter\(\{ hasText: \/保存\|确定\|提交\//,
  'Runner prompt 必须给出 Element Plus footer 保存按钮的确定性 Playwright selector。'
)
assert.match(
  runner,
  /custom footer rows[\s\S]*page\.locator\('button, \.el-button'\)[\s\S]*hasText: \/\^保存\$\|\^确定\$\|\^提交\$/,
  'Runner prompt 必须要求子任务在 Element Plus 标准 footer 缺失时继续定位可见自定义底部保存按钮。'
)
assert.match(
  runner,
  /field-selector list filters[\s\S]*visible selected field[\s\S]*路线编码[\s\S]*TN-ROUTE-BASIC-001/,
  'Runner prompt 必须提醒子任务遇到字段选择器列表筛选时可按当前可见字段搜索，工艺路线固定样本应优先用路线编码。'
)
assert.match(
  runner,
  /query buttons may be labeled 查询 or 搜索[\s\S]*getByRole\('button', \{ name: \/查询\|搜索\/ \}\)/,
  'Runner prompt 必须提醒子任务列表查询按钮可能叫“查询”或“搜索”，并给出确定性 selector。'
)

console.log('PASS: Codex runner Playwright dependency static contract')
