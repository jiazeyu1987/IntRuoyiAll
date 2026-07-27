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

console.log('PASS: Codex runner read-only timeout static contract')
