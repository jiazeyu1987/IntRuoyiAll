const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const routePath = path.join(repoRoot, 'src', 'router', 'modules', 'remaining.ts')
const executionPagePath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr', 'ExecutionPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')
const routes = fs.readFileSync(routePath, 'utf8')
const executionPage = fs.readFileSync(executionPagePath, 'utf8')

const handleStart = detail.indexOf('const handleOpenTask = async')
const handleEnd = detail.indexOf('const handleSkipSpecialNode', handleStart)
assert(handleStart > -1 && handleEnd > handleStart, '必须存在 handleOpenTask 打开工序处理函数。')
const handleOpenTask = detail.slice(handleStart, handleEnd)

assert(handleOpenTask.includes('openEdhrBatchTask'), '打开工序必须先调用 openEdhrBatchTask 获取真实执行记录。')
assert(handleOpenTask.includes("path: '/mes/pro/feedback/edhr-execution/form'"), '打开工序必须跳转到 eDHR 执行表单页。')
assert(!handleOpenTask.includes("path: '/mes/pro/feedback/edhr-execution/detail'"), '打开工序不得再跳转到 eDHR 执行详情页。')

for (const token of [
  'id: String(opened.executionId)',
  'executionId: String(opened.executionId)',
  'batchExecutionId: String(assertBatchExecutionId())',
  'batchTaskId: String(row.id)',
  'workTaskId: String(openedWorkTaskId)',
  "returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'"
]) {
  assert(handleOpenTask.includes(token), `打开工序跳转必须继续透传上下文：${token}`)
}

assert(
  detail.includes("key: 'open-process'") &&
    detail.includes('await handleOpenSelectedExecutionTask()') &&
    detail.includes('await handleOpenTask(selectedOpenableTask.value, currentProcessFillCarrier.value)'),
  '右侧打开工序按钮必须继续复用当前工序任务打开逻辑。'
)

assert(routes.includes("path: 'pro/feedback/edhr-execution/form'"), '路由表必须存在 eDHR 执行表单页路由。')
assert(routes.includes("title: 'eDHR执行表单'"), 'eDHR 执行表单页路由必须保持表单语义标题。')
assert(!routes.includes("path: 'pro/feedback/edhr-execution/detail'"), '路由表不得继续注册废弃的 eDHR 执行详情页。')

assert(
  /<div[\s\S]{0,120}class="edhr-page-shell__toolbar"[\s\S]{0,500}<Icon\s+icon="ep:arrow-left"[\s\S]{0,160}返回/.test(
    executionPage
  ),
  '执行表单页 tracking toolbar 必须保留返回和标题区域。'
)

for (const token of [
  '{{ executionPageTitle }}',
  '{{ executionPageSubtitle }}',
  '<Icon icon="ep:arrow-left" class="mr-5px" />',
  '返回',
  "return `${reportName}填写`",
  "'填写当前工序表单，保存字段变更后提交执行'"
]) {
  assert(executionPage.includes(token), `执行表单页必须保持填写页语义：${token}`)
}

for (const obsoleteReturnLabel of [
  '{{ backToBatchLabel }}',
  "currentBatchExecutionId.value ? '返回批次详情' : '返回批次执行'",
  '返回批次详情',
  '返回批次执行'
]) {
  assert(!executionPage.includes(obsoleteReturnLabel), `执行表单页不得继续使用旧返回按钮文案：${obsoleteReturnLabel}`)
}

for (const obsoleteToken of [
  'const isExecutionFormPage = computed(',
  "'eDHR 执行详情'",
  '>执行摘要<',
  '>技术证据<',
  '>最终表单归档<',
  '<ExecutionRenderer',
  'class="edhr-page-shell__audit-tabs"'
]) {
  assert(!executionPage.includes(obsoleteToken), `填写页不得继续保留执行详情内容：${obsoleteToken}`)
}

console.log('edhr open process form route static contract passed')
