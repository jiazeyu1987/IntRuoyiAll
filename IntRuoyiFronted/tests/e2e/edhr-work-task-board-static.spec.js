const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/workTask.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')

assert(fs.existsSync(apiPath), 'eDHR工作任务看板 API 文件必须存在。')
assert(fs.existsSync(pagePath), 'eDHR工作任务看板页面必须存在。')

const api = fs.readFileSync(apiPath, 'utf8')
const page = fs.readFileSync(pagePath, 'utf8')

assert.match(api, /\/mes\/pro\/edhr-work-task\/my-page/, '看板必须调用我的 eDHR 工作任务分页接口。')
assert.match(api, /\/mes\/pro\/edhr-work-task\/done-page/, '看板必须调用我的 eDHR 已处理任务接口。')
assert.match(api, /\/mes\/pro\/edhr-work-task\/stats/, '看板必须调用任务统计接口。')

for (const taskType of ['FILL', 'REVIEW', 'REWORK', 'ARCHIVE']) {
  assert.match(page, new RegExp(taskType), `看板必须展示 ${taskType} 任务类型。`)
}

assert.match(api, /archiveCount:\s*number/, '看板统计接口类型必须包含待归档数量。')
assert.match(page, /archiveCount/, '看板必须展示待归档数量。')
assert.match(page, /workTaskId/, '看板直达填写或审批页面时必须携带 workTaskId。')
assert.match(page, /getEdhrWorkTaskMyPage/, '看板必须从后端加载待办任务。')
assert.match(page, /getEdhrWorkTaskDonePage/, '看板必须从后端加载已处理任务。')
assert.match(page, /EDHR_WORK_TASK_STATUS_OVERDUE/, '看板必须使用 OVERDUE 状态常量加载逾期任务。')
assert.match(page, /name="overdue"/, '看板必须提供逾期任务页签。')
assert.match(page, /到期时间/, '看板必须展示任务到期时间。')
assert.match(api, /overdueAt\?:\s*string/, '看板 API 类型必须暴露逾期标记时间。')
assert.match(api, /overdueReason\?:\s*string/, '看板 API 类型必须暴露逾期原因。')
assert.match(page, /逾期时间/, '看板必须展示独立逾期时间，不能用完成时间替代。')
assert.match(page, /status:\s*resolveMyPageStatus\(\)/, '待办和逾期页签必须向 my-page 传入明确状态。')
assert.doesNotMatch(page, /disabled="[^"]*OVERDUE|:disabled="[^"]*EDHR_WORK_TASK_STATUS_OVERDUE/, '逾期任务必须允许继续进入处理。')
assert.doesNotMatch(page, /mock|fixture|demo/i, '看板不得使用 mock、fixture 或 demo 数据。')

console.log('PASS: eDHR work task board static contract')
