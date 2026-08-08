const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const runner = fs.readFileSync(path.join(root, 'scripts/codex-test-runner.mjs'), 'utf8')

assert.match(runner, /const ANALYSIS_MODE_CODE_READONLY = 'CODE_READONLY'/, 'Runner 必须显式声明 CODE_READONLY 模式。')
assert.match(runner, /const ANALYSIS_MODE_PLAYWRIGHT_E2E = 'PLAYWRIGHT_E2E'/, 'Runner 必须显式声明默认 Playwright 模式。')
assert.match(runner, /function resolveAnalysisMode\(task\)[\s\S]*ANALYSIS_MODE_PLAYWRIGHT_E2E/, 'Runner 必须把旧任务默认解析为 PLAYWRIGHT_E2E。')
assert.match(runner, /function buildPrompt\(task[\s\S]*resolveAnalysisMode\(task\)[\s\S]*buildCodeReadonlyPrompt/, 'buildPrompt 必须按 analysisMode 分支选择 prompt。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*只读代码分析[\s\S]*不得创建、修改或删除任何仓库文件[\s\S]*不得运行会写入业务数据/, 'CODE_READONLY prompt 必须明确只读代码分析和写入禁令。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*不要打开浏览器作为优先路径[\s\S]*代码、路由、API、测试/, 'CODE_READONLY prompt 不得使用浏览器优先策略，必须允许扫描代码、路由、API 和测试。')
assert.match(runner, /function buildCodeReadonlyPrompt\(task[\s\S]*checkpointResults[\s\S]*mismatchDescription[\s\S]*PASS\|FAIL\|BLOCKED/, 'CODE_READONLY prompt 仍必须强制 checkpointResults JSON 和失败差异描述。')
assert.match(runner, /function buildPlaywrightPrompt\(task/, '原 Playwright E2E prompt 必须保留独立函数。')
assert.match(runner, /You are executing an enterprise E2E test with Playwright/, 'PLAYWRIGHT_E2E prompt 必须保留浏览器测试语义。')

console.log('codex-runner-code-readonly-static PASS')
