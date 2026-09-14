const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const readFrontend = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
const readBackend = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, 'IntRuoyiBackend', relativePath), 'utf8')

const page = readFrontend('src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue')
const api = readFrontend('src/api/system/codexTestManagement/index.ts')
const controller = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/codextest/CodexTestExecutionController.java'
)

assert.match(
  api,
  /startCodeReadonlyCodexTestExecution[\s\S]*\/system\/codex-test-execution\/start-code-readonly/,
  '批记录测试必须通过专用原子接口写入只读测试定义并启动执行。'
)
assert.match(
  api,
  /getCodexTestExecutionResult[\s\S]*\/system\/codex-test-execution\/result\?id=/,
  '页面必须通过执行权限范围内的结果接口读取 Codex CLI 回复。'
)
assert.match(
  api,
  /getCodexTestExecutionResult[\s\S]*\/system\/codex-test-execution\/result\?id=[\s\S]*timeout:\s*120000/,
  '长任务结果轮询必须使用独立 120 秒请求预算，不得被全局 30 秒预算中止。'
)
assert.match(
  controller,
  /@PostMapping\("\/start-code-readonly"\)[\s\S]*@PreAuthorize\("@ss\.hasPermission\('system:codex-test:execute'\)"\)[\s\S]*startCodeReadonlyExecution/,
  '只读测试启动接口必须只要求按钮已经校验的 execute 权限。'
)
assert.match(
  controller,
  /@GetMapping\("\/result"\)[\s\S]*@PreAuthorize\("@ss\.hasPermission\('system:codex-test:execute'\)"\)[\s\S]*getExecutionResult/,
  '当前执行结果必须允许发起者使用 execute 权限读取。'
)

assert.match(
  page,
  /data-edhr-batch-record-test-result-dialog/,
  '页面必须提供稳定的 Codex CLI 回复弹框。'
)
assert.match(
  page,
  /测试结果[\s\S]*执行编号[\s\S]*执行状态[\s\S]*Codex CLI 回复/,
  '结果弹框必须展示执行编号、执行状态和 Codex CLI 回复。'
)
assert.match(
  page,
  /checkpoint\.actualText[\s\S]*checkpoint\.mismatchDescription|checkpoint\.mismatchDescription[\s\S]*checkpoint\.actualText/,
  '结果弹框必须展示检查点实际回复与不符合描述。'
)
assert.match(
  page,
  /async function\s+handleTestRow\([\s\S]*startCodeReadonlyCodexTestExecution\(\{[\s\S]*targetTenantId:\s*selectedTenantId\.value[\s\S]*caseDefinition:\s*buildCodeReadonlyCasePayload\(row\)/,
  '点击测试必须以一次原子请求启动当前行的 CODE_READONLY 执行。'
)
assert.doesNotMatch(
  page,
  /async function\s+handleTestRow\([\s\S]*?upsertCodeReadonlyCase\(row\)[\s\S]*?startCodexTestExecution/,
  '点击测试不得继续依赖 query/create/update 三步前置链路。'
)
assert.match(
  page,
  /getCodexTestExecutionResult\(executionId\)[\s\S]*terminalExecutionStatuses\.has\(execution\.status\)[\s\S]*return\s+execution[\s\S]*waitForResultPollInterval\(pollToken\)/,
  '启动后必须轮询正式执行结果，直到 Runner 返回结构化回复。'
)
assert.match(
  page,
  /PASS[\s\S]*FAIL[\s\S]*BLOCKED[\s\S]*CANCELED[\s\S]*TIMEOUT/,
  '结果轮询必须覆盖全部正式终态。'
)
assert.doesNotMatch(page, /mock|placeholder.*success|默认成功/i, '不得用模拟或默认成功结果代替 Codex CLI 回复。')

console.log('batch-record-test-codex-cli-response-static PASS')
