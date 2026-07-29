const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertIncludes(
  executionPage,
  'type AssistProcessSwitchItem',
  '工序切换必须使用按工序分组的列表模型，不能直接把表单任务当工序。'
)
assertIncludes(
  executionPage,
  'assistProcessSwitchItems = ref<AssistProcessSwitchItem[]>([])',
  '工序切换状态必须保存分组后的全部工序项。'
)
assertIncludes(
  executionPage,
  'buildAssistProcessSwitchItems',
  '加载当前批次详情后必须统一构建全部工序列表。'
)

const processLoaderMatch = executionPage.match(
  /const loadAssistProcessSwitchItems = async \(\) => \{[\s\S]*?(?=\r?\n\r?\nconst resolveCurrentAssistBatchTask)/
)
assert.ok(processLoaderMatch, '必须保留工序切换列表加载函数。')
assertIncludes(
  processLoaderMatch[0],
  'buildAssistProcessSwitchItems(batchDetail.tasks || [])',
  '工序切换必须从当前批次全部任务构建列表。'
)
assertNotIncludes(
  processLoaderMatch[0],
  '.filter(isAssistBatchTaskOpenable)',
  '工序切换列表不得只展示当前可打开任务。'
)
assert.doesNotMatch(
  processLoaderMatch[0],
  /status\s*!==\s*EDHR_BATCH_TASK_STATUS_APPROVED|status\s*!==\s*EDHR_BATCH_TASK_STATUS_SKIPPED|available\s*!==\s*false|activeWorkTaskId|allowedActions/,
  '工序切换加载阶段不得按任务状态、available、activeWorkTaskId 或 allowedActions 过滤工序。'
)

assertIncludes(
  executionPage,
  '展示当前批次全部工序',
  '工序切换弹窗说明必须表达全部工序，而不是仅可打开任务。'
)
assertNotIncludes(
  executionPage,
  '仅显示当前批次可打开任务',
  '工序切换弹窗不能再沿用仅可打开任务文案。'
)

assertIncludes(
  executionPage,
  'resolveAssistProcessSwitchItemStateClass(item)',
  '工序选项必须按批次执行状态背景设置 class。'
)
assertIncludes(
  executionPage,
  'resolveAssistProcessSwitchItemStatusLabel(item)',
  '工序选项必须展示聚合后的状态标签。'
)
assertIncludes(
  executionPage,
  'resolveAssistProcessSwitchItemStatusType(item)',
  '工序选项必须展示 Element Plus 状态标签类型。'
)

for (const token of [
  'EDHR_BATCH_TASK_STATUS_WAITING',
  'EDHR_BATCH_TASK_STATUS_DRAFT',
  'EDHR_BATCH_TASK_STATUS_SUBMITTED',
  'EDHR_BATCH_TASK_STATUS_REJECTED',
  'EDHR_BATCH_TASK_STATUS_REWORK_REQUIRED',
  'EDHR_BATCH_TASK_STATUS_APPROVED',
  'EDHR_BATCH_TASK_STATUS_SKIPPED',
  'EDHR_BATCH_TASK_STATUS_BLOCKED'
]) {
  assertIncludes(executionPage, token, `状态展示必须覆盖 ${token}。`)
}

const styleChecks = [
  ['.edhr-fill-workspace__assist-switch-option', '--edhr-assist-process-state-background: #f7f9fc'],
  ['.edhr-fill-workspace__assist-switch-option.is-completed', '--edhr-assist-process-state-background: #f0f9eb'],
  ['.edhr-fill-workspace__assist-switch-option.is-in-progress', '--edhr-assist-process-state-background: #fff8e6']
]
for (const [selector, token] of styleChecks) {
  const index = executionPage.indexOf(selector)
  assert.notEqual(index, -1, `${selector} 样式必须存在。`)
  const styleBlock = executionPage.slice(index, executionPage.indexOf('}', index) + 1)
  assertIncludes(styleBlock, token, `${selector} 必须复用批次执行工序状态背景口径。`)
}

const navigateMatch = executionPage.match(
  /const navigateToAssistBatchTask = async \([\s\S]*?(?=\r?\n\r?\nconst handleSelectAssistFillerSwitchItem)/
)
assert.ok(navigateMatch, '必须保留正式批次任务打开函数。')
assertIncludes(
  navigateMatch[0],
  'navigateToReadonlyAssistBatchTask',
  '非可打开但已有执行记录的工序必须走只读执行页，不得伪造 openTask 成功。'
)
assertIncludes(
  navigateMatch[0],
  'isAssistBatchTaskOpenable(row)',
  '可编辑打开仍必须先满足正式可打开条件。'
)

console.log('PASS: edhr assist process switch all statuses static contract')
