const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPagePath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
)
const detailPage = fs.readFileSync(detailPagePath, 'utf8')
const railStart = detailPage.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = detailPage.indexOf('</aside>', railStart)
assert.ok(railStart >= 0 && railEnd > railStart, '批记录详情必须保留右侧一级操作栏。')
const rail = detailPage.slice(railStart, railEnd)

assert.match(
  detailPage,
  /label:\s*'填写人'/,
  '批记录详情一级界面必须展示填写人。'
)
assert.match(
  rail,
  /primaryFormFillMetaItems/,
  '批记录详情必须在右侧黄框一级区域展示填写人和提交时间。'
)

const canOpenTaskMatch = detailPage.match(
  /const canOpenTask = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?(?=(?:\r?\n){2}const canHandlePendingTask)/
)
assert.ok(canOpenTaskMatch, '必须保留独立 canOpenTask 权限判定。')
const canOpenTask = canOpenTaskMatch[0]
assert.match(
  canOpenTask,
  /hasAllowedTaskAction\(row,\s*'OPEN_FORM'\)/,
  '批记录打开入口必须以 OPEN_FORM 动作为准，管理员只有查看权限时不能打开填写。'
)
assert.doesNotMatch(
  canOpenTask,
  /!hasActiveWorkTask\(row\)\s*\|\|/,
  '批记录打开入口不得因没有活跃待办而默认放行管理员填写。'
)

assert.match(
  detailPage,
  /const resolveSelectedTaskFillerGroupsText = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*生产[\s\S]*设备[\s\S]*质量/,
  '当前表单详情必须在任务填写人为空时，展示当前工序生产/设备/质量应填写人员。'
)
const selectedTaskBelongsToCurrentProcessMatch = detailPage.match(
  /const selectedTaskBelongsToCurrentProcess = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?(?=(?:\r?\n){2}const resolveSelectedTaskFillerGroupsText)/
)
assert.ok(selectedTaskBelongsToCurrentProcessMatch, '必须保留当前表单归属当前工序的判定。')
assert.match(
  selectedTaskBelongsToCurrentProcessMatch[0],
  /currentProcessRouteProcessId[\s\S]*routeProcessId/,
  '当前工序应填写人员兜底展示不能只靠工序名称/编码，还必须支持 currentProcessRouteProcessId 与任务 routeProcessId 匹配。'
)
assert.match(
  detailPage,
  /const resolvePendingTaskFillableUsersText = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*resolveSelectedTaskFillerGroupsText\(row\)/,
  '当前应填写人员必须优先展示任务填写人，并兜底展示当前工序应填写人员。'
)
assert.match(
  detailPage,
  /const resolvePrimaryFormFillersText = \(\) =>[\s\S]*resolvePendingTaskFillableUsersText\(selectedTask\)/,
  '一级填写元信息必须优先展示任务填写人，并兜底展示当前工序应填写人员。'
)

console.log('PASS: eDHR batch admin filler visibility static contract')
