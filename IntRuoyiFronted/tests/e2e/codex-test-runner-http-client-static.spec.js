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
  /function runnerHeaders\(extraHeaders = \{\}\)[\s\S]*\.\.\.RUNNER_HTTP_CONNECTION_HEADERS[\s\S]*'tenant-id': MANAGEMENT_TENANT_ID[\s\S]*'X-Codex-Runner-Token': RUNNER_TOKEN[\s\S]*\.\.\.extraHeaders/,
  'Runner 协议请求必须统一注入连接关闭、租户和 token 头，再叠加调用方内容类型。'
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
