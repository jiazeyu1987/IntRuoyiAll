const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')
const evidenceCollector = fs.readFileSync(
  path.join(root, 'scripts/codex-test-readonly-evidence.mjs'),
  'utf8'
)
const resultSchema = JSON.parse(
  fs.readFileSync(path.join(root, 'scripts/codex-test-readonly-result.schema.json'), 'utf8')
)

assert.match(runner, /const ANALYSIS_MODE_CODE_READONLY = 'CODE_READONLY'/, 'Runner 必须显式声明 CODE_READONLY 模式。')
assert.match(runner, /const ANALYSIS_MODE_PLAYWRIGHT_E2E = 'PLAYWRIGHT_E2E'/, 'Runner 必须显式声明默认 Playwright 模式。')
assert.match(runner, /function resolveAnalysisMode\(task\)[\s\S]*ANALYSIS_MODE_PLAYWRIGHT_E2E/, 'Runner 必须把旧任务默认解析为 PLAYWRIGHT_E2E。')
assert.match(runner, /function buildPrompt\(task[\s\S]*resolveAnalysisMode\(task\)[\s\S]*buildCodeReadonlyPrompt/, 'buildPrompt 必须按 analysisMode 分支选择 prompt。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*分析\$\{description\}在当前系统里是否已经实现,是否过度限制,回复限制在100字以内/, 'CODE_READONLY prompt 必须使用批记录描述短提示词。')
assert.match(runner, /function resolveCodeReadonlyDescription\(task\)[\s\S]*task\.testDataText[\s\S]*description is missing/, 'CODE_READONLY prompt 必须从 Runner 领取协议的测试数据快照读取分析目标，缺失时明确失败。')
assert.match(runner, /CODEX_TEST_CODEX_READONLY_TIMEOUT_MS \|\| '360000'/, 'CODE_READONLY 必须为大型仓库代码审查保留 6 分钟独立预算。')
assert.match(runner, /CODEX_TEST_CODEX_READONLY_REASONING_EFFORT \|\| 'low'/, 'CODE_READONLY 必须使用适合有界代码核查的低推理强度。')
assert.match(runner, /ANALYSIS_MODE_CODE_READONLY[\s\S]*--sandbox[\s\S]*read-only[\s\S]*--output-schema/, 'CODE_READONLY 必须使用 Codex 原生只读沙箱和结构化输出约束。')
assert.match(runner, /function resolveCodexWorkingDirectory\(task\)[\s\S]*ANALYSIS_MODE_CODE_READONLY[\s\S]*PROJECT_ROOT[\s\S]*WORKING_DIRECTORY/, 'CODE_READONLY 必须以正式项目根作为只读沙箱工作目录。')
assert.match(runner, /'-C',[\s\S]*resolveCodexWorkingDirectory\(task\)/, 'Codex 子进程必须按 analysisMode 选择工作目录。')
assert.match(runner, /collectCodeReadonlyEvidence\(task, PROJECT_ROOT\)/, 'CODE_READONLY 必须把白名单目录的实时 rg 证据交给 Codex CLI。')
assert.match(evidenceCollector, /EXCLUDED_BACKEND_DIRECTORY_NAMES[\s\S]*doc\|docs[\s\S]*node_modules[\s\S]*output[\s\S]*target/, 'CODE_READONLY 必须排除任务记录、依赖和构建输出，仅扫描正式源码与测试证据。')
assert.match(evidenceCollector, /MAX_EVIDENCE_FILES = 20[\s\S]*MAX_EVIDENCE_LENGTH = 280000/, 'CODE_READONLY 必须限制证据文件数量和总字节数。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*实时只读代码证据[\s\S]*codeReadonlyEvidence/, 'Codex CLI 必须收到 Runner 提供的真实证据。')
assert.strictEqual(resultSchema.properties.checkpointResults.items.properties.actualText.maxLength, 100)
assert.strictEqual(resultSchema.properties.checkpointResults.items.properties.mismatchDescription.maxLength, 100)
assert.strictEqual(resultSchema.properties.summary.maxLength, 100)
assert.match(runner, /function buildPlaywrightPrompt\(task/, '原 Playwright E2E prompt 必须保留独立函数。')
assert.match(runner, /You are executing an enterprise E2E test with Playwright/, 'PLAYWRIGHT_E2E prompt 必须保留浏览器测试语义。')
assert.deepStrictEqual(resultSchema.required, ['checkpointResults', 'summary'])
assert.deepStrictEqual(
  resultSchema.properties.checkpointResults.items.properties.status.enum,
  ['PASS', 'FAIL', 'BLOCKED']
)

console.log('codex-runner-code-readonly-static PASS')
