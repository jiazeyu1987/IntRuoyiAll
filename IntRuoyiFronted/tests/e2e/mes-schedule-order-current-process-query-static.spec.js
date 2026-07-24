const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const scheduleOrderPageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const schedulerWorkbenchSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.match(
  scheduleOrderPageSource,
  /const applyRouteProcessFilter = \(\) => \{[\s\S]*const processId = Number\(currentProcessId\)[\s\S]*if \(processId === 0\) \{[\s\S]*return[\s\S]*\}[\s\S]*if \(!Number\.isFinite\(processId\) \|\| !Number\.isInteger\(processId\) \|\| processId < 0\)[\s\S]*scheduleOrderQueryParams\.currentProcessId = processId/,
  'Schedule order page must ignore currentProcessId=0 sentinel and fail fast for malformed process ids before page query.'
)

assert.match(
  schedulerWorkbenchSource,
  /const openProcessWipOrders = \(item: MesProScheduleOrderProcessWipVO\) => \{[\s\S]*const processId = Number\(item\.processId\)[\s\S]*currentProcessId: Number\.isFinite\(processId\) && processId > 0 \? String\(processId\) : undefined/,
  'Scheduler workbench must only pass positive currentProcessId when jumping to schedule orders.'
)

console.log('PASS: schedule order current process query contract')
