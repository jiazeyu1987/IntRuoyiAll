const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /const RUNNER_HTTP_CONNECTION_HEADERS = \{\s*Connection:\s*'close'\s*\}/,
  'Runner HTTP 请求必须显式关闭连接，避免长时间 Codex 超时后复用污染 socket 导致 heartbeat\/register 卡死。'
)
assert.match(
  runner,
  /const RUNNER_TOKEN = process\.env\.CODEX_TEST_RUNNER_TOKEN \|\| ''/,
  '本地裸调 Codex CLI 模式下，Runner token 必须是可选环境变量。'
)
assert.doesNotMatch(
  runner,
  /requiredEnv\('CODEX_TEST_RUNNER_TOKEN'\)/,
  'Runner 进程不得因为缺少 CODEX_TEST_RUNNER_TOKEN 直接退出。'
)
assert.match(
  runner,
  /function runnerHeaders\(extraHeaders = \{\}\)[\s\S]*const headers = \{[\s\S]*\.\.\.RUNNER_HTTP_CONNECTION_HEADERS[\s\S]*'tenant-id': MANAGEMENT_TENANT_ID[\s\S]*\}[\s\S]*if \(RUNNER_TOKEN\) \{[\s\S]*headers\['X-Codex-Runner-Token'\] = RUNNER_TOKEN[\s\S]*return \{[\s\S]*\.\.\.headers[\s\S]*\.\.\.extraHeaders/,
  'Runner 协议请求必须仅在 token 存在时注入 token 头，并始终保留连接关闭、租户和调用方内容类型。'
)
assert.match(
  runner,
  /return await fetch\(`\$\{API_BASE\}\$\{url\}`,[\s\S]*signal: controller\.signal[\s\S]*cache:\s*'no-store'/,
  'Runner fetch 必须保持每次请求独立且不缓存，超时后下次 heartbeat/register 应重新建链路。'
)
assert.doesNotMatch(
  runner,
  /axios\.create|keepAlive:\s*true|new Agent\(\{\s*keepAlive:\s*true/,
  'Runner 不得为注册和心跳启用 keep-alive 客户端。'
)

console.log('PASS: Codex runner HTTP client static contract')
