const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (...segments) => fs.readFileSync(path.join(repoRoot, ...segments), 'utf8')

const api = read('src', 'api', 'mes', 'pro', 'edhr', 'workTask.ts')
const board = read('src', 'views', 'mes', 'pro', 'edhr-work-task', 'WorkTaskBoardPage.vue')
const detail = read('src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')

assert.ok(
  api.includes("EDHR_WORK_TASK_TYPE_RELEASE_APPROVE = 'RELEASE_APPROVE'"),
  'workTask API must expose RELEASE_APPROVE as an existing eDHR work-task type.'
)
assert.ok(
  board.includes('EDHR_WORK_TASK_TYPE_RELEASE_APPROVE') && board.includes('最终放行审批'),
  'work task board must display the release approval task type instead of treating it as a fill task.'
)
assert.ok(
  api.includes('route-release-approval-rule') && board.includes('openReleaseApprovalRuleDialog'),
  'release approver assignment must reuse the existing route work-task rule dialog and API.'
)
assert.ok(
  detail.includes("focus === 'approval'") &&
    detail.includes('selectReleaseProcess()') &&
    detail.includes('releaseApprovalDrawerVisible.value = true'),
  'batch detail must accept focus=approval and open the existing release approval drawer.'
)
