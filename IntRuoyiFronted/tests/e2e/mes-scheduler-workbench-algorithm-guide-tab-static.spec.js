const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

assert.match(
  pageSource,
  /<el-tab-pane[\s\S]*label="排产逻辑"[\s\S]*name="algorithm-guide"/,
  '排产员工作台必须新增“排产逻辑”页签。'
)
assert.match(
  pageSource,
  /scheduler-workbench__algorithm-guide/,
  '排产逻辑页签必须有独立样式类，避免挤压现有列表。'
)

for (const text of [
  '最近一次成功重排',
  '检查数据',
  '订单顺序',
  '拆分工序',
  '计算产能',
  '受保护任务',
  '物料需求与其他问题',
  '生成结果',
  '暂无已应用的重排记录'
]) {
  assert.match(pageSource, new RegExp(text), `排产逻辑说明缺少通俗步骤：${text}`)
}

for (const forbidden of [
  'MesProAutoScheduleServiceImpl',
  'scheduleOrderComparator',
  'MesProScheduleOrderDO',
  'taskMapper',
  'capacityPlanMapper',
  'Comparator.comparing'
]) {
  assert.doesNotMatch(pageSource, new RegExp(forbidden), `说明页签不应暴露代码术语：${forbidden}`)
}

assert.match(pageSource, /activeWipTab = ref\('process-list'\)/)
assert.match(pageSource, /<el-tab-pane[\s\S]*label="工序列表"[\s\S]*name="process-list"/)
assert.match(pageSource, /<el-tab-pane[\s\S]*label="工艺路线在制订单"[\s\S]*name="route-active"/)
assert.match(pageSource, />\s*排产设置\s*</)

console.log('mes-scheduler-workbench-algorithm-guide-tab-static: PASS')
