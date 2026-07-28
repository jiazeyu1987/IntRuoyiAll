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

const stateClassMatch = detail.match(
  /const resolveProcessGroupStateClass = \(group: ProcessTaskGroup\) => \{[\s\S]*?\n\}/
)
assert.ok(stateClassMatch, '批次详情必须保留工序组状态 class 解析函数。')
const stateClass = stateClassMatch[0]

assert.match(
  detail,
  /const isCurrentProcessGroup = \(group: ProcessTaskGroup\) => \{[\s\S]*currentProcessRouteProcessId[\s\S]*group\.routeProcessId[\s\S]*currentProcessCode[\s\S]*group\.processCode[\s\S]*currentProcessName[\s\S]*group\.processName[\s\S]*\}/,
  '批记录管理员只读详情必须用详情接口 currentProcess* 字段识别当前工序组，而不是依赖当前登录人能否打开填写。'
)

assert.match(
  stateClass,
  /if \(isCurrentProcessGroup\(group\)\) return 'is-in-progress'/,
  '当前工序组即使所有任务仍为 WAITING/待打开，也必须显示黄色运行态。'
)

assert.doesNotMatch(
  stateClass,
  /canOpenTask|hasAllowedTaskAction|activeWorkTaskId/,
  '工序运行态展示不得依赖 OPEN_FORM、activeWorkTaskId 或当前账号是否为填写人。'
)

assert.match(
  detail,
  /const canOpenTask = \(row: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*hasAllowedTaskAction\(row,\s*'OPEN_FORM'\)/,
  '管理员看到黄色当前工序不等于获得填写权；打开填写仍必须由 OPEN_FORM 权限投影控制。'
)

assert.match(
  detail,
  /isProductInfoProcessGroup\(group\)/,
  '产品信息虚拟工序不得因复用来源 routeProcessId 被误判为当前正式工序。'
)

console.log('PASS: eDHR batch admin current process highlight static contract')
