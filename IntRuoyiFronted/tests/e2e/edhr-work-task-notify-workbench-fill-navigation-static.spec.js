const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const workTaskNavigation = readSource('src/utils/edhrWorkTaskNavigation.ts')
const notifyNavigation = readSource('src/utils/notifyMessageNavigation.ts')
const profileWorkbench = readSource('src/views/Profile/components/ProfileWorkbench.vue')

assert.match(
  workTaskNavigation,
  /import\s*\{\s*openEdhrBatchTask\s*\}\s*from\s*['"]@\/api\/mes\/pro\/edhr\/batchExecution['"]/,
  'eDHR 工作任务导航工具必须能调用批次任务打开接口，把未生成执行记录的填写任务打开到填写工作区。'
)

assert.match(
  workTaskNavigation,
  /export const navigateToEdhrWorkTask\s*=/,
  'eDHR 工作任务导航工具必须提供统一跳转函数，供站内信和个人工作台复用。'
)

assert.match(
  workTaskNavigation,
  /EDHR_BATCH_EXECUTION_DETAIL_PATH[\s\S]*batchTaskId[\s\S]*openEdhrBatchTask/,
  '未生成 executionId 的批次填写任务必须通过 batchExecutionId + batchTaskId 打开，不能把批次详情 id 当执行记录 id。'
)

assert.match(
  workTaskNavigation,
  /const\s+workTaskId\s*=\s*toPositiveNumber\(resolveWorkTaskId\(item,\s*url\)\)[\s\S]*openEdhrBatchTask\(\{[\s\S]*workTaskId/,
  '未生成 executionId 的批次填写任务打开接口必须携带 workTaskId，避免旧填写人或构造 URL 绕过任务所有权校验。'
)

assert.match(
  notifyNavigation,
  /navigateToEdhrWorkTask/,
  '站内信 eDHR 工作任务点击必须复用统一跳转函数。'
)

assert.match(
  notifyNavigation,
  /target\.type\s*===\s*'edhrWorkTask'[\s\S]*navigateToEdhrWorkTask/,
  '站内信点击 eDHR 填写任务时必须可直接打开填写工作区。'
)

assert.match(
  notifyNavigation,
  /templateParams\.workTaskId[\s\S]*url\.searchParams\.set\('workTaskId'/,
  '站内信 eDHR 工作任务必须把模板参数里的 workTaskId 归一化进 actionUrl，避免历史入口缺少任务上下文。'
)

assert.match(
  profileWorkbench,
  /edhrWorkTask\?:\s*EdhrWorkTaskRespVO/,
  '个人工作台待办行必须保留原始 eDHR 工作任务，便于点击时打开真实填写任务。'
)

assert.match(
  profileWorkbench,
  /edhrWorkTask:\s*item/,
  '个人工作台映射 eDHR 待办时必须保留工作任务上下文。'
)

assert.match(
  profileWorkbench,
  /if\s*\(\s*row\.edhrWorkTask\s*\)[\s\S]*await\s+navigateToEdhrWorkTask\(router,\s*row\.edhrWorkTask\)/,
  '个人工作台点击 eDHR 填写任务必须调用统一跳转函数，而不是仅 router.push 静态 actionUrl。'
)

console.log('PASS: eDHR work task notify and workbench fill navigation static contract')
