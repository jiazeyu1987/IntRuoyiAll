const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(
  runner,
  /const CODEX_EXEC_READONLY_TIMEOUT_MS = Number\(process\.env\.CODEX_TEST_CODEX_READONLY_TIMEOUT_MS \|\| '\d+'\)/,
  'Runner 必须为只读测试项配置独立 Codex 超时，避免简单查看任务占满 600 秒。'
)
assert.match(
  runner,
  /const CODEX_EXEC_TIMEOUT_MS = Number\(process\.env\.CODEX_TEST_CODEX_TIMEOUT_MS \|\| '360000'\)/,
  'Runner 写入型业务页面测试默认 Codex 外层预算必须收敛到 360 秒，避免临时脚本已超时却继续占满 600 秒。'
)
assert.match(
  runner,
  /const CODEX_READONLY_REASONING_EFFORT = process\.env\.CODEX_TEST_CODEX_READONLY_REASONING_EFFORT \|\| 'medium'/,
  'Runner 只读任务必须默认使用中等推理预算，避免本机只读页面核验被全局 xhigh 配置拖到超时。'
)
assert.match(
  runner,
  /const CODEX_MUTATING_REASONING_EFFORT = process\.env\.CODEX_TEST_CODEX_MUTATING_REASONING_EFFORT \|\| 'low'/,
  'Runner 写入型页面执行必须默认使用低推理预算，避免临时 Playwright 脚本生成阶段耗尽 600 秒。'
)
assert.match(
  runner,
  /const CODEX_IGNORE_RULES = process\.env\.CODEX_TEST_CODEX_IGNORE_RULES !== 'false'/,
  'Runner 必须对所有业务页面测试使用统一的执行策略开关。'
)
assert.match(
  runner,
  /const NEGATED_WRITE_TASK_PATTERN = \/[\s\S]*不修改[\s\S]*不新增[\s\S]*不保存[\s\S]*不提交[\s\S]*不删除[\s\S]*\//,
  'Runner 必须识别“不修改/不新增/不保存/不提交/不删除”等否定写入词，不能把它们当成真实写入意图。'
)
assert.match(
  runner,
  /function isReadOnlyTask\(task\)[\s\S]*READONLY_TASK_PATTERN[\s\S]*textWithoutNegatedWriteIntent[\s\S]*WRITE_TASK_PATTERN[\s\S]*return hasReadOnlyIntent && !hasWriteIntent/,
  'Runner 必须基于测试项文本识别只读任务，同时在排除否定写入词后再判断真实写入意图。'
)
assert.match(
  runner,
  /function resolveCodexExecTimeoutMs\(task\)[\s\S]*isReadOnlyTask\(task\)[\s\S]*CODEX_EXEC_READONLY_TIMEOUT_MS[\s\S]*CODEX_EXEC_TIMEOUT_MS/,
  'Runner 必须按任务类型解析 Codex 执行超时，只读查看任务不得继续使用长运行写入型预算。'
)
assert.match(
  runner,
  /const codexExecTimeoutMs = resolveCodexExecTimeoutMs\(task\)[\s\S]*codex exec timed out after \$\{codexExecTimeoutMs\}ms/,
  'Runner 超时错误必须使用当前任务解析后的预算，便于真实 E2E 证据定位。'
)
assert.match(
  runner,
  /function codexExecutionArgs\(task\)[\s\S]*CODEX_IGNORE_RULES[\s\S]*--ignore-rules[\s\S]*--disable[\s\S]*remote_plugin[\s\S]*isReadOnlyTask\(task\)[\s\S]*CODEX_READONLY_REASONING_EFFORT[\s\S]*CODEX_MUTATING_REASONING_EFFORT[\s\S]*model_reasoning_effort=\$\{JSON\.stringify\(reasoningEffort\)\}/,
  'Runner 必须为所有业务页面任务忽略仓库执行规则、关闭远程插件同步并设置受控推理预算。'
)
assert.match(
  runner,
  /const args = \[[\s\S]*'--ephemeral',[\s\S]*\.\.\.codexExecutionArgs\(task\),[\s\S]*'--output-last-message'/,
  'Runner 启动 codex exec 时必须把统一业务测试执行策略应用到实际参数。'
)
assert.match(
  runner,
  /const taskMode = isReadOnlyTask\(task\) \? 'READ_ONLY' : 'MUTATING_OR_UNKNOWN'/,
  'Prompt 必须显式声明 Runner 对当前任务的只读/未知写入判断。'
)
assert.match(
  runner,
  /Do not ask for clarification[\s\S]*return a BLOCKED checkpoint result instead of waiting/,
  'Codex prompt 必须要求无法继续时回写 BLOCKED JSON，而不是长时间等待人工输入或自由探索。'
)
assert.match(
  runner,
  /Complete the browser verification and return the final JSON within \$\{executionBudgetSeconds\} seconds/,
  'Codex prompt 必须写明有界完成预算，简单只读测试不能无限探索。'
)
assert.match(
  runner,
  /This task is classified as \$\{taskMode\}/,
  'Codex prompt 必须把任务模式传给 Codex，防止只读任务误执行写入动作。'
)
assert.match(
  runner,
  /This is a browser execution task, not a repository development task[\s\S]*Do not create or modify repository files, task documents, source code, configuration, build outputs, Git state, commits, branches, or worktrees[\s\S]*Do not run project builds or project test suites[\s\S]*Use only task-owned temporary files under \$\{WORKING_DIRECTORY\}/,
  '所有业务测试 prompt 必须禁止仓库开发、建档和 Git 操作，仅允许隔离目录中的临时文件。'
)
assert.match(
  runner,
  /Execution strategy:[\s\S]*create one temporary Node\.js Playwright script under \$\{WORKING_DIRECTORY\}[\s\S]*run it with node[\s\S]*Do not inspect the repository before the first browser attempt/,
  '写入型业务测试 prompt 必须要求先生成并运行一个临时 Playwright 脚本，避免 Codex 长时间探索仓库。'
)
assert.match(
  runner,
  /When the temporary script prints raw JSON with checkpointResults, return that JSON immediately/,
  '临时脚本已经给出 PASS/FAIL/BLOCKED JSON 时，Codex 必须立即交回 Runner，不能继续自由调试直到超时。'
)
assert.match(
  runner,
  /For Element Plus dialogs, click visible buttons by accessible role or exact visible text/,
  '业务页面 prompt 必须要求按可访问 role 或可见文本点击 Element Plus 弹框按钮，避免保存按钮可见但脚本定位不到。'
)
assert.match(
  runner,
  /Playwright project root: \$\{FRONTEND_PROJECT_ROOT\}[\s\S]*Project guidance root: \$\{PROJECT_ROOT\}/,
  '隔离工作目录下仍必须向 Codex 明确提供正式 Playwright 工程和最小只读指导路径。'
)

console.log('PASS: Codex runner read-only timeout static contract')
