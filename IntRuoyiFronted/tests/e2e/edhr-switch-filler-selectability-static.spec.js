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
assert.doesNotMatch(
  selectableMatch[0],
  /currentAssistUserId\(\)|currentAssistSwitchUserId\(\)/,
  '候选项可选态不得按当前登录用户或当前选中用户禁用其它后端可打开候选人。'
)

const activeMatch = executionPage.match(
  /const isAssistFillerSwitchItemActive = \(item: AssistFillerSwitchItem\) =>[\s\S]*?(?=\r?\n\r?\nconst resolveAssistRecordCategory)/
)
assert.ok(activeMatch, '切换填写人弹窗必须保留当前选中项判断。')
assert.match(
  activeMatch[0],
  /sameRouteQueryId\(currentAssistSwitchUserId\(\),\s*item\.userId\)/,
  '当前选中填写人必须用 route query ID 语义比较，避免 assistUserId 字符串和 userId 数字严格等于失败。'
)
assert.match(
  activeMatch[0],
  /isAssistBatchTaskActive\(item\.task\)/,
  '当前选中填写人仍必须同时匹配当前批次任务上下文。'
)
assert.doesNotMatch(
  activeMatch[0],
  /currentAssistSwitchUserId\(\)\s*===\s*item\.userId/,
  '当前选中填写人不得使用严格等于比较 route assistUserId 和数字 userId。'
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
assert.match(
  selectHandlerMatch[0],
  /item\.userId/,
  '切换填写人点击后必须把所选填写人 ID 传入正式打开流程，确保表单上下文随填写人变化。'
)
assert.match(
  selectHandlerMatch[0],
  /item\.displayName/,
  '切换填写人点击后必须把所选填写人名称传入正式打开流程，确保顶部填写人标签随所选候选变化。'
)
assert.doesNotMatch(
  selectHandlerMatch[0],
  /该填写人不属于当前账号可处理项，不能代填或切换责任人。/,
  '点击处理不得继续使用只按当前账号本人判断的错误提示。'
)

const loadItemsMatch = executionPage.match(
  /const loadAssistFillerSwitchItems = async \(\) => \{[\s\S]*?(?=\r?\n\r?\nconst openAssistSwitchDialog)/
)
assert.ok(loadItemsMatch, '切换填写人弹窗必须从执行详情快照构造候选列表。')
assert.match(
  loadItemsMatch[0],
  /execution\.value\?\.assistSwitchTasks/,
  '切换填写人必须读取执行详情 assistSwitchTasks 快照，不能重新拉全量批次详情。'
)
assert.match(
  loadItemsMatch[0],
  /sameRouteQueryId\(task\.routeProcessId,\s*routeProcessId\)/,
  '当前工序候选过滤必须使用 route query ID 语义，避免数字/字符串差异漏掉附加表单。'
)
assert.doesNotMatch(
  loadItemsMatch[0],
  /task\.routeProcessId\s*===\s*routeProcessId/,
  '当前工序候选过滤不得用严格等于比较 routeProcessId。'
)
const currentProcessFilterMatch = loadItemsMatch[0].match(
  /const currentProcessTasks = \[\.\.\.assistSwitchTasks\][\s\S]*?\.sort\(/
)
assert.ok(currentProcessFilterMatch, '当前工序候选必须显式过滤同工序任务后再排序。')
assert.doesNotMatch(
  currentProcessFilterMatch[0],
  /formSlotType\s*===\s*'MAIN'[\s\S]*?fillableUsers/,
  '切换填写人候选不得只保留 MAIN 批记录表单，必须允许同工序附加表单槽位。'
)

const navigateMatch = executionPage.match(
  /const navigateToAssistBatchTask = async \([\s\S]*?(?=\r?\n\r?\nconst handleSelectAssistFillerSwitchItem)/
)
assert.ok(navigateMatch, '必须保留辅助模式批次任务正式打开函数。')
assert.match(
  navigateMatch[0],
  /assistUserId:\s*selectedAssistUserId/,
  '正式打开批次任务时必须提交 selectedAssistUserId，不能只在前端切换高亮。'
)
assert.match(
  navigateMatch[0],
  /assistUserId:\s*String\(openedAssistUserId\)/,
  '打开成功后路由必须记录后端确认的 assistUserId，便于当前填写人高亮和上下文识别。'
)
assert.match(
  navigateMatch[0],
  /fillerName:\s*selectedAssistDisplayName/,
  '打开成功后路由必须记录所选填写人名称，不能继续用当前登录用户昵称显示顶部填写人。'
)
assert.match(
  executionPage,
  /watch\(\s*\(\) =>[\s\S]*route\.name[\s\S]*route\.query\.id[\s\S]*route\.query\.workTaskId[\s\S]*route\.query\.assistUserId/,
  '执行页 watch 必须监听 assistUserId，切换填写人后要重新初始化当前执行上下文。'
)

console.log('PASS: edhr switch filler selectability static contract')
