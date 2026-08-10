const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')
const resultSchema = JSON.parse(
  fs.readFileSync(path.join(root, 'scripts/codex-test-readonly-result.schema.json'), 'utf8')
)

assert.match(runner, /const ANALYSIS_MODE_CODE_READONLY = 'CODE_READONLY'/, 'Runner 必须显式声明 CODE_READONLY 模式。')
assert.match(runner, /const ANALYSIS_MODE_PLAYWRIGHT_E2E = 'PLAYWRIGHT_E2E'/, 'Runner 必须显式声明默认 Playwright 模式。')
assert.match(runner, /function resolveAnalysisMode\(task\)[\s\S]*ANALYSIS_MODE_PLAYWRIGHT_E2E/, 'Runner 必须把旧任务默认解析为 PLAYWRIGHT_E2E。')
assert.match(runner, /function buildPrompt\(task[\s\S]*resolveAnalysisMode\(task\)[\s\S]*buildCodeReadonlyPrompt/, 'buildPrompt 必须按 analysisMode 分支选择 prompt。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*只读代码分析[\s\S]*不得创建、修改或删除任何仓库文件[\s\S]*不得运行会写入业务数据/, 'CODE_READONLY prompt 必须明确只读代码分析和写入禁令。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*不要打开浏览器作为优先路径[\s\S]*代码、路由、API、测试/, 'CODE_READONLY prompt 不得使用浏览器优先策略，必须允许扫描代码、路由、API 和测试。')
assert.match(runner, /CODEX_TEST_CODEX_READONLY_TIMEOUT_MS \|\| '360000'/, 'CODE_READONLY 必须为大型仓库代码审查保留 6 分钟独立预算。')
assert.match(runner, /CODEX_TEST_CODEX_READONLY_REASONING_EFFORT \|\| 'low'/, 'CODE_READONLY 必须使用适合有界代码核查的低推理强度。')
assert.match(runner, /ANALYSIS_MODE_CODE_READONLY[\s\S]*--sandbox[\s\S]*read-only[\s\S]*--output-schema/, 'CODE_READONLY 必须使用 Codex 原生只读沙箱和结构化输出约束。')
assert.match(runner, /function resolveCodexWorkingDirectory\(task\)[\s\S]*ANALYSIS_MODE_CODE_READONLY[\s\S]*PROJECT_ROOT[\s\S]*WORKING_DIRECTORY/, 'CODE_READONLY 必须以正式项目根作为只读沙箱工作目录。')
assert.match(runner, /'-C',[\s\S]*resolveCodexWorkingDirectory\(task\)/, 'Codex 子进程必须按 analysisMode 选择工作目录。')
assert.match(runner, /collectCodeReadonlyEvidence\(task, PROJECT_ROOT\)/, 'CODE_READONLY 必须把白名单目录的实时 rg 证据交给 Codex CLI。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*正式源码[\s\S]*doc\/tasks[\s\S]*node_modules[\s\S]*target[\s\S]*output/, 'CODE_READONLY 必须排除任务记录、依赖和构建输出，仅扫描正式源码与测试证据。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*最多选择 20 个[\s\S]*实时只读代码证据/, 'CODE_READONLY 必须限制证据文件数量，并把完整链路实时片段交给 Codex CLI。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*不得运行任何 shell 命令[\s\S]*实时只读代码证据/, 'Codex CLI 必须只判断 Runner 提供的真实证据，不再触发 Windows read-only shell ACL。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*checkpointResults[\s\S]*mismatchDescription[\s\S]*PASS\|FAIL\|BLOCKED/, 'CODE_READONLY prompt 仍必须强制 checkpointResults JSON 和失败差异描述。')
assert.match(runner, /function buildPlaywrightPrompt\(task/, '原 Playwright E2E prompt 必须保留独立函数。')
assert.match(runner, /You are executing an enterprise E2E test with Playwright/, 'PLAYWRIGHT_E2E prompt 必须保留浏览器测试语义。')
assert.deepStrictEqual(resultSchema.required, ['checkpointResults', 'summary'])
assert.deepStrictEqual(
  resultSchema.properties.checkpointResults.items.properties.status.enum,
  ['PASS', 'FAIL', 'BLOCKED']
)

console.log('codex-runner-code-readonly-static PASS')
