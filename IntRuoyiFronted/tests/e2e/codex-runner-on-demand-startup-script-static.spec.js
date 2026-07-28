const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const starter = fs.readFileSync(path.join(root, 'scripts/start-codex-test-runner.ps1'), 'utf8')

assert.match(
  starter,
  /Assert-CommandExists -CommandName \$NodeCommand[\s\S]*Assert-CommandExists -CommandName \$CodexCommand/,
  '按需 Runner 启动必须继续 fail-fast 校验 Node 与 Codex CLI 命令存在。'
)
assert.match(
  starter,
  /Assert-HttpReachable -Url \(\$ApiBase -replace '\/admin-api\$', '\/actuator\/health'\) -Name 'Backend health'/,
  '按需 Runner 启动必须继续校验后端健康，确保注册与领取任务有正式后端入口。'
)
assert.doesNotMatch(
  starter,
  /Assert-HttpReachable -Url \$FrontendBaseUrl -Name 'Frontend entry'/,
  '按需 Runner 启动不得因前端入口暂时不可达而阻断 Codex CLI/Runner 启动。'
)
assert.match(
  starter,
  /\$env:CODEX_TEST_FRONTEND_BASE_URL = \$FrontendBaseUrl/,
  '前端 URL 仍必须传给 Runner，由具体测试任务在真实页面路径中暴露前端不可达问题。'
)
assert.match(
  starter,
  /\$env:CODEX_CLI_COMMAND = \$CodexCommand/,
  '启动脚本必须继续把 Codex CLI 命令注入 Runner 执行环境。'
)
assert.doesNotMatch(
  starter,
  /Frontend entry is not reachable/,
  '启动失败消息不得继续出现前端入口硬阻断。'
)
