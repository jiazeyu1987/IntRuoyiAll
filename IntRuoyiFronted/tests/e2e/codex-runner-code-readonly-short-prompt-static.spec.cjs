const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const backendSystemRoot = path.join(workspaceRoot, 'IntRuoyiBackend', 'yudao-module-system')
const page = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)
const runner = fs.readFileSync(path.join(frontendRoot, 'scripts/codex-test-runner.mjs'), 'utf8')
const claimResponse = fs.readFileSync(
  path.join(
    backendSystemRoot,
    'src/main/java/cn/iocoder/yudao/module/system/controller/admin/codextest/vo/CodexTestRunnerClaimRespVO.java'
  ),
  'utf8'
)
const runnerService = fs.readFileSync(
  path.join(
    backendSystemRoot,
    'src/main/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestRunnerServiceImpl.java'
  ),
  'utf8'
)
const schema = JSON.parse(
  fs.readFileSync(path.join(frontendRoot, 'scripts/codex-test-readonly-result.schema.json'), 'utf8')
)

assert.match(
  page,
  /function buildCodeReadonlyAnalysisPrompt\(description: string\)[\s\S]*分析\$\{description\}在当前系统里是否已经实现,是否过度限制,回复限制在100字以内/,
  '批记录测试页签列表的描述必须直接进入用户指定的短提示词。'
)
assert.match(
  page,
  /methodText:\s*buildCodeReadonlyAnalysisPrompt\(definition\.description\)/,
  '只读分析方法文本必须使用短提示词。'
)
assert.match(
  page,
  /testDataText:\s*definition\.description/,
  '只读分析测试数据必须只保留页签列表描述，避免重新注入旧长规则。'
)
assert.match(
  page,
  /expectedText:\s*buildCodeReadonlyAnalysisPrompt\(definition\.description\)/,
  '只读分析检查点预期必须使用同一短提示词。'
)
assert.doesNotMatch(
  page,
  /CODE_READONLY_BUSINESS_JUDGEMENT_RULE|缺少 Service、Mapper、SQL|先把职责描述翻译成业务流程/,
  '前端不得继续拼接旧版长判定规则。'
)

assert.match(
  runner,
  /function resolveCodeReadonlyDescription\(task\)[\s\S]*task\.testDataText[\s\S]*description is missing/,
  'Runner 必须从领取协议正式提供的 testDataText 读取批记录测试页签列表描述。'
)
assert.doesNotMatch(
  runner,
  /task\.checkpoints\?\.\[0\]\?\.remark/,
  'Runner 不得读取领取协议不存在的 checkpoint remark。'
)
assert.match(
  claimResponse,
  /class Task[\s\S]*String testDataText[\s\S]*List<Checkpoint> checkpoints/,
  'Runner 领取协议任务必须正式提供 testDataText。'
)
assert.doesNotMatch(
  claimResponse,
  /class Checkpoint[\s\S]*String remark/,
  'Runner 领取协议检查点不得被测试误认为提供 remark。'
)
assert.match(
  runnerService,
  /task\.setTestDataText\(executionCase\.getTestDataTextSnapshot\(\)\)/,
  'Runner 领取任务必须从执行快照传递 testDataText。'
)
assert.match(
  runner,
  /function buildCodeReadonlyPrompt\(task[\s\S]*const description = resolveCodeReadonlyDescription\(task\)[\s\S]*分析\$\{description\}在当前系统里是否已经实现,是否过度限制,回复限制在100字以内/,
  'CODE_READONLY Runner prompt 必须使用用户指定的短提示词。'
)
assert.doesNotMatch(
  runner,
  /职责描述类、业务方向类|缺少 Service、Mapper、SQL|先把职责描述翻译成业务流程|不要直接使用 API、Controller、Mapper/,
  'Runner 不得继续注入旧版长判定规则。'
)
assert.match(
  runner,
  /实时只读代码证据：[\s\S]*\$\{codeReadonlyEvidence \|\| 'Runner 未提供任何匹配代码证据。'\}/,
  'Runner 必须继续提供实时只读代码证据作为上下文。'
)

const checkpointSchema = schema.properties.checkpointResults.items.properties
assert.strictEqual(checkpointSchema.actualText.maxLength, 100)
assert.strictEqual(checkpointSchema.mismatchDescription.maxLength, 100)
assert.strictEqual(schema.properties.summary.maxLength, 100)

console.log('codex-runner-code-readonly-short-prompt-static PASS')
