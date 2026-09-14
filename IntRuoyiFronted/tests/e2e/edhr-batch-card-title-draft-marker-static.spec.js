const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const formListStart = detail.indexOf('class="edhr-batch-detail__rail-process-form-list"')
const emptyStateStart = detail.indexOf('<el-empty v-else description="当前工序未配置表单"', formListStart)

assert.ok(formListStart >= 0 && emptyStateStart > formListStart, '必须能识别右侧当前工序表单卡片列表。')

const formList = detail.slice(formListStart, emptyStateStart)

assert.match(formList, /v-for="task in selectedProcessTasks"/, '右侧卡片必须继续逐个展示真实表单任务。')
assert.doesNotMatch(
  formList,
  /edhr-batch-detail__rail-execution-code/,
  '右侧每张表单卡片不得继续使用批次执行编号作为卡片标题。'
)
assert.doesNotMatch(
  formList,
  /detail\?\.batchExecutionCode/,
  '批次执行编号只能保留在页面批次上下文，不得作为每张卡片的主标题。'
)
assert.match(
  formList,
  /:title="resolveTaskCardDisplayName\(task\)"/,
  '右侧卡片 title 属性必须使用草稿标识后的表单名称。'
)
assert.match(
  formList,
  /\{\{\s*resolveTaskCardDisplayName\(task\)\s*\}\}/,
  '右侧卡片可见标题必须使用草稿标识后的表单名称。'
)
assert.match(formList, /resolveTaskStatusLabel\(task\)/, '卡片必须继续显示任务自身状态标签。')
assert.match(formList, /resolveTaskCardFillersText\(task\)/, '卡片必须继续显示填写人。')
assert.match(formList, /handleSelectedPendingTaskAction\(task\)/, '卡片主动作必须保持原入口。')

const helperStart = detail.indexOf('const resolveTaskCardDisplayName = (row: EdhrBatchExecutionTaskRespVO)')
const helperEnd = detail.indexOf('\n\nconst resolvePendingTaskTitle', helperStart)

assert.ok(helperStart >= 0 && helperEnd > helperStart, '必须提供卡片标题本地展示 helper。')

const helper = detail.slice(helperStart, helperEnd)

assert.match(helper, /const name = resolveTaskDisplayName\(row\)/, '卡片标题基础名称必须复用原表单名称解析。')
assert.match(
  helper,
  /row\.status === EDHR_BATCH_TASK_STATUS_DRAFT/,
  '草稿星号必须按当前任务 DRAFT 状态判定。'
)
assert.match(helper, /name === '--'/, '无有效名称时不得追加草稿星号。')
assert.match(helper, /`\$\{name\}\*`/, '草稿任务名称必须追加 ASCII *。')
assert.doesNotMatch(helper, /detail\?\.batchExecutionCode/, '卡片标题 helper 不得读取批次执行编号。')

console.log('PASS: eDHR batch card title uses form name and draft marker.')
