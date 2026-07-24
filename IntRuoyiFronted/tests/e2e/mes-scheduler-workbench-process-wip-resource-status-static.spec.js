const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = process.cwd()
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)
const scheduleOrderApi = fs.readFileSync(
  path.join(repoRoot, 'src/api/mes/pro/scheduleorder/index.ts'),
  'utf8'
)

assert.match(
  scheduleOrderApi,
  /resourceStatus\?:\s*'NORMAL'\s*\|\s*'CAPACITY_MISSING'/,
  '工序在制接口类型必须暴露资源配置状态'
)
assert.match(
  scheduleOrderApi,
  /resourceStatusReason\?:\s*string/,
  '工序在制接口类型必须暴露资源配置状态原因'
)
assert.match(
  pageSource,
  /v-if="isProcessWipResourceMissing\(row\)"[\s\S]*row\.resourceStatusReason \|\| '资源缺失'/,
  '排产员工作台工序在制列表必须显示资源缺失原因'
)
assert.match(
  pageSource,
  /const isProcessWipResourceMissing = \(row: MesProScheduleOrderProcessWipVO\) =>\s*row\.resourceStatus === 'CAPACITY_MISSING'/,
  '排产员工作台必须按后端 resourceStatus 判断资源缺失'
)

console.log('mes-scheduler-workbench-process-wip-resource-status-static: PASS')
