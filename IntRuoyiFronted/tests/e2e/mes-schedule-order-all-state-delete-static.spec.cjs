const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const page = fs.readFileSync(
  path.resolve(root, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const api = fs.readFileSync(path.resolve(root, 'src/api/mes/pro/scheduleorder/index.ts'), 'utf8')

assert.match(page, /data-testid="schedule-order-row-delete"/)
assert.match(page, /v-hasPermi="\['mes:pro-schedule-order:delete'\]"/)
assert.match(page, /查看已删除工单/)
assert.match(page, /当前生产、报工、质检和批记录将继续保留/)
assert.match(page, /未开始的排产任务将被取消/)
assert.match(page, /expectedUpdateTime/)
assert.match(page, /待取消任务/)
assert.match(page, /deleteImpact\?\.activeOrderCount/)
assert.match(page, /删除信息/)
assert.match(api, /removedFromSchedule\?: boolean/)
assert.match(api, /getDeleteImpact/)
assert.match(api, /\/mes\/pro\/schedule-order\/delete-impact/)
assert.match(api, /removedFromScheduleTime\?: string/)
assert.match(api, /expectedUpdateTime: string/)
assert.match(api, /removedFromSchedule\?: boolean/)

console.log('schedule order all-state delete static contract passed')
