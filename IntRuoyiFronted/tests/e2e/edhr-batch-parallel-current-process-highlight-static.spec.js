const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const executableGroupMatch = detail.match(
  /const isCurrentExecutableProcessGroup = \(group: ProcessTaskGroup\) => \{[\s\S]*?\n\}/
)
assert.ok(
  executableGroupMatch,
  '批次详情必须按后端任务门禁 available 识别当前可执行工序组，不能只消费单个 currentProcess*。'
)
const executableGroup = executableGroupMatch[0]

assert.match(
  executableGroup,
  /isProductInfoProcessGroup\(group\)[\s\S]*return false/,
  '产品信息虚拟 80 工序不得因复用来源 routeProcessId 被当作当前可执行正式工序。'
)
assert.match(
  executableGroup,
  /group\.tasks\.some\([\s\S]*task\.available === true[\s\S]*!isCompletedProcessTask\(task\)[\s\S]*!isOptionalTask\(task\)/,
  '工序开始后的并行第一组任务只要前置门禁 available=true 且未完成，就必须整组显示黄色运行态。'
)
assert.doesNotMatch(
  executableGroup,
  /canOpenTask|hasAllowedTaskAction|OPEN_FORM|activeWorkTaskId/,
  '当前可执行工序组展示不得依赖 OPEN_FORM、activeWorkTaskId 或当前账号是否为填写人。'
)

const stateClassMatch = detail.match(
  /const resolveProcessGroupStateClass = \(group: ProcessTaskGroup\) => \{[\s\S]*?\n\}/
)
assert.ok(stateClassMatch, '批次详情必须保留工序组状态 class 解析函数。')
const stateClass = stateClassMatch[0]
assert.match(
  stateClass,
  /if \(isCurrentExecutableProcessGroup\(group\) \|\| isCurrentProcessGroup\(group\)\) return 'is-in-progress'/,
  '黄色运行态必须先覆盖所有当前可执行工序组，再兼容详情单个 currentProcess*。'
)

console.log('PASS: eDHR batch parallel current process highlight static contract')
