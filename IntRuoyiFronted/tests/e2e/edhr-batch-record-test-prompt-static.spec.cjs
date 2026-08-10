const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const page = read('src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue')
const runner = read('scripts/codex-test-runner.mjs')

assert.match(
  page,
  /methodText:[\s\S]*从业务逻辑角度判断当前实现方向[\s\S]*不做完整实现审计/,
  '批记录测试点击测试必须要求 Codex 从业务逻辑角度判断实现方向，而不是审计完整实现细节。'
)
assert.match(
  page,
  /const\s+CODE_READONLY_BUSINESS_JUDGEMENT_RULE[\s\S]*判定规则：/,
  '页面必须集中声明 CODE_READONLY 通用判定规则。'
)
assert.match(
  page,
  /testDataText:[\s\S]*职责描述：[\s\S]*CODE_READONLY_BUSINESS_JUDGEMENT_RULE/,
  'CODE_READONLY 测试数据必须明确提供职责描述并附带通用判定规则。'
)
assert.match(
  page,
  /expectedText:[\s\S]*页面文案、路由、按钮、字段、状态、API 命名、权限命名和测试名称[\s\S]*主业务流程、核心对象、用户动作和后续上下文/,
  '检查点必须按业务逻辑方向判断主流程、核心对象、用户动作和后续上下文。'
)
assert.match(
  page,
  /label="简要结论"[\s\S]*<span>测试回复<\/span>[\s\S]*label="通俗解释"[\s\S]*label="哪里不一致"/,
  '历史弹框必须使用业务人员能理解的结果文案。'
)
assert.doesNotMatch(
  page,
  /label="执行摘要"|<span>Codex CLI 回复<\/span>|label="实际回复"|label="不符合描述"|正在等待 Codex CLI 回复/,
  '历史弹框不得继续暴露技术化结果文案。'
)
assert.doesNotMatch(
  page,
  /PASS 仅在每个关键义务都有实时只读代码证据支持|只有完整职责描述均有代码级设计证据时才通过/,
  '点击测试提示词不得把业务方向判断升级为代码级完整证据审计。'
)

assert.match(
  runner,
  /先把职责描述翻译成业务流程/,
  'Runner CODE_READONLY 总提示词必须要求先翻译成业务流程。'
)
assert.match(
  runner,
  /当前代码表达出的业务方向、页面入口、核心对象、用户动作和后续上下文与职责描述一致/,
  'Runner CODE_READONLY 总提示词必须以业务方向、入口、对象、动作和上下文作为 PASS 门槛。'
)
assert.match(
  runner,
  /缺少 Service、Mapper、SQL 或测试只能作为实现细节不足说明，不能单独作为不通过原因/,
  'Runner CODE_READONLY 总提示词必须禁止因实现细节证据缺失单独失败。'
)
assert.match(
  runner,
  /维护、复核、历史记录、事件过滤或另一个角色范围/,
  'Runner CODE_READONLY 总提示词必须把相邻业务方向识别为不通过。'
)
assert.match(
  runner,
  /请用业务人员能听懂的简明语言[\s\S]*一句话结论[\s\S]*冲突在哪里[\s\S]*是否需要修改/,
  'Runner CODE_READONLY 输出必须要求通俗解释、冲突点和是否需要修改。'
)
assert.match(
  runner,
  /不要直接使用 API、Controller、Mapper、Service、SQL、字段名、枚举名、文件名/,
  'Runner CODE_READONLY 输出必须禁止技术术语堆叠。'
)
assert.doesNotMatch(
  runner,
  /"actualText": "real code-analysis evidence"|"summary": "short code analysis summary"/,
  'Runner CODE_READONLY JSON 示例不得继续诱导技术化输出。'
)
assert.doesNotMatch(
  runner,
  /PASS 仅当每个关键义务都有实时代码证据支持|缺少核心入口、API、状态链路、权限或测试证据时返回 FAIL/,
  'Runner CODE_READONLY 总提示词不得继续使用代码级完整义务 PASS/FAIL 门槛。'
)

console.log('edhr-batch-record-test-prompt-static PASS')
