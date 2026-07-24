const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const workbenchPath = path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleOrderApiPath = path.join(repoRoot, 'src/api/mes/pro/scheduleorder/index.ts')

const workbench = fs.readFileSync(workbenchPath, 'utf8')
const scheduleOrderApi = fs.readFileSync(scheduleOrderApiPath, 'utf8')

function expectIncludes(source, needle, message) {
  assert(source.includes(needle), `${message}\nExpected to find: ${needle}`)
}

expectIncludes(scheduleOrderApi, 'estimatedStartTime?: string', 'API 类型必须暴露预计开始时间')
expectIncludes(
  scheduleOrderApi,
  'estimatedStartTime: normalizeDateTimeValue(row.estimatedStartTime)',
  '工序在制统计必须归一化预计开始时间'
)
expectIncludes(workbench, "key: 'estimatedStartTime'", '列配置必须包含预计开始')
expectIncludes(workbench, 'label="预计开始"', '工序在制列表必须显示预计开始列')
expectIncludes(workbench, 'prop="estimatedStartTime"', '预计开始列必须绑定后端字段')
expectIncludes(
  workbench,
  'formatEstimatedTime(row.estimatedStartTime)',
  '预计开始必须复用统一时间格式化出口'
)
expectIncludes(workbench, "{ key: 'estimatedStartTime'", '快速过滤必须覆盖预计开始')
expectIncludes(
  workbench,
  "quickFilter.fieldKey === 'estimatedStartTime'",
  '预计开始必须按日期范围执行快速过滤'
)
expectIncludes(
  workbench,
  'formatEstimatedTime(row.estimatedCompletionTime)',
  '预计完工必须继续复用统一时间格式化出口'
)

console.log('mes-scheduler-workbench-process-wip-estimated-time-static passed')
