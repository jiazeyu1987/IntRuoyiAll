const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const railStart = detailPage.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = detailPage.indexOf('</aside>', railStart)
assert.ok(railStart >= 0 && railEnd > railStart, '批次详情必须保留右侧一级操作栏。')
const rail = detailPage.slice(railStart, railEnd)
const workTaskBoard = readSource('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')
const profileWorkbench = readSource('src/views/Profile/components/ProfileWorkbench.vue')
const notifyNavigation = readSource('src/utils/notifyMessageNavigation.ts')
const workTaskNavigation = readSource('src/utils/edhrWorkTaskNavigation.ts')

assert.ok(
  rail.includes('primaryFormFillMetaItems') &&
    detailPage.includes("label: '填写人'") &&
    detailPage.includes("label: '提交时间'") &&
    detailPage.includes('resolveTaskGateText'),
  '批次详情右侧一级区域必须使用填写语义，并统一格式化任务阻断原因。'
)

for (const forbidden of ['待处理详情', '我的权限', '当前节点没有待处理任务', '当前登录人暂无该表单填写入口']) {
  assert.ok(!detailPage.includes(forbidden), `批次执行填写界面不得展示审核/待办噪音文案：${forbidden}`)
}

assert.match(
  workTaskBoard,
  /navigateToEdhrWorkTask/,
  '个人工作台打开填写/返工任务时必须复用统一 eDHR 工作任务导航。'
)
assert.match(
  workTaskNavigation,
  /openEdhrBatchTask/,
  '统一 eDHR 工作任务导航必须调用批次任务打开接口，拿到 executionId 后直达填写页。'
)
assert.match(
  workTaskBoard,
  /taskType\s*===\s*EDHR_WORK_TASK_TYPE_FILL[\s\S]*taskType\s*===\s*EDHR_WORK_TASK_TYPE_REWORK/,
  '个人工作台必须显式识别填写和返工任务。'
)
assert.match(
  workTaskNavigation,
  /path:\s*EDHR_EXECUTION_FORM_PATH/,
  '统一 eDHR 工作任务导航必须让填写/返工任务直达 eDHR 填写工作区。'
)
assert.match(
  workTaskNavigation,
  /(?:fillCarrier:\s*|query\.fillCarrier\s*=\s*)EDHR_FILL_CARRIER_FORM[\s\S]*(?:recordCategory:\s*|query\.recordCategory\s*=\s*)EDHR_RECORD_CATEGORY_BATCH_RECORD/,
  '统一 eDHR 工作任务导航直达填写页必须携带批记录填写载体参数。'
)

assert.match(
  profileWorkbench,
  /normalizeEdhrWorkTaskRoute/,
  '个人工作台统一待办必须复用 eDHR 工作任务路由归一化逻辑。'
)
assert.match(
  workTaskNavigation,
  /'\/mes\/pro\/feedback\/edhr-execution\/form'/,
  '站内信 actionUrl 白名单必须支持填写工作区路径。'
)
assert.match(
  notifyNavigation,
  /EDHR_WORK_TASK_NOTIFY_PATHS/,
  '站内信导航必须复用 eDHR 工作任务白名单。'
)

console.log('PASS: eDHR batch fill direct navigation static contract')
