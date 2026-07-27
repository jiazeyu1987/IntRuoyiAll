const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const selectableMatch = executionPage.match(
  /const isAssistFillerSwitchItemSelectable = \(item: AssistFillerSwitchItem\) =>[\s\S]*?(?=\r?\n\r?\nconst isAssistFillerSwitchItemActive)/
)
assert.ok(selectableMatch, '切换填写人弹窗必须保留候选项可选态判断。')

assert.match(
  selectableMatch[0],
  /isAssistBatchTaskOpenable\(item\.task\)/,
  '候选项可选态必须继续保留批次任务可打开条件，不能绕过后端正式 openTask 校验。'
)
assert.match(
  selectableMatch[0],
  /hasGoldenFingerPermission\.value/,
  '具备金手指/代填权限时，其他可填写候选人不应被当前登录用户 ID 直接禁用。'
)
assert.doesNotMatch(
  selectableMatch[0],
  /currentAssistUserId\(\) === item\.userId\s*&&\s*isAssistBatchTaskOpenable\(item\.task\)/,
  '候选项可选态不得只允许当前登录用户本人，否则截图中的另外 2 个可填写人会被禁用。'
)

const selectHandlerMatch = executionPage.match(
  /const handleSelectAssistFillerSwitchItem = async \(item: AssistFillerSwitchItem\) => \{[\s\S]*?(?=\r?\n\r?\nconst handleSelectAssistProcessSwitchItem)/
)
assert.ok(selectHandlerMatch, '切换填写人点击处理必须保留。')
assert.match(
  selectHandlerMatch[0],
  /await navigateToAssistBatchTask\(/,
  '可选候选人点击后必须继续进入正式批次任务打开流程。'
)
assert.doesNotMatch(
  selectHandlerMatch[0],
  /该填写人不属于当前账号可处理项，不能代填或切换责任人。/,
  '点击处理不得继续使用只按当前账号本人判断的错误提示。'
)

console.log('PASS: edhr switch filler selectability static contract')
