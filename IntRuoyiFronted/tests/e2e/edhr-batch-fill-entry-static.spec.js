const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')
const resolverMatch = source.match(
  /const resolveCurrentUserFillTask = \(row: EdhrBatchExecutionRespVO\) =>[\s\S]*?const canOpenCurrentUserFillTask/
)
assert.ok(resolverMatch, 'missing resolveCurrentUserFillTask contract block')
const resolverSource = resolverMatch[0]

function assertIncludes(fragment, message) {
  assert.ok(source.includes(fragment), message)
}

function assertNotIncludes(fragment, message) {
  assert.ok(!source.includes(fragment), message)
}

function assertResolverNotIncludes(fragment, message) {
  assert.ok(!resolverSource.includes(fragment), message)
}

assertIncludes('const EDHR_BATCH_TASK_OPEN_FORM_ACTION = \'OPEN_FORM\'', 'row entry must require backend OPEN_FORM action')
assertIncludes('task.activeWorkTaskId &&', 'row entry must require an active workTaskId')
assertIncludes('hasAllowedTaskAction(task, EDHR_BATCH_TASK_OPEN_FORM_ACTION)', 'row entry must be action-driven')
assertIncludes('isFillOrReworkWorkTask(task)', 'row entry must be limited to FILL/REWORK work tasks')
assertIncludes('isPendingFillTaskStatus(task.status)', 'row entry must reject completed or blocked batch tasks')
assertIncludes('task.activeWorkTaskType !== EDHR_BATCH_WORK_TASK_TYPE_BLOCKED', 'row entry must reject blocked work tasks')
assertIncludes('const workTaskId = task?.activeWorkTaskId', 'navigation must carry the active work task id')
assertIncludes('workTaskId: String(workTaskId)', 'fill form route must receive workTaskId')
assertIncludes('returnPath: route.fullPath || \'/mes/pro/feedback/edhr-batch-execution\'', 'fill form route must preserve returnPath')

assertResolverNotIncludes('currentFillers', 'row entry must not depend on filler display names')
assertResolverNotIncludes('displayName', 'row entry must not depend on filler display names')
assertResolverNotIncludes('黎敏', 'row entry must not hardcode a named filler')
assertResolverNotIncludes('limin', 'row entry must not hardcode a filler username')
assertResolverNotIncludes('jiazeyu', 'row entry must not hardcode a filler username')

console.log('PASS: eDHR batch fill row entry static contract')
