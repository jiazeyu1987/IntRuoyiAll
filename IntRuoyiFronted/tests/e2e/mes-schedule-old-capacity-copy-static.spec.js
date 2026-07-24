const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const scheduleOrderSource = read('src/views/mes/pro/scheduleorder/index.vue')
const capacitySourceTagSource = read('src/views/mes/pro/route/components/CapacitySourceTag.vue')
const routeScheduleStrategyEditorSource = read(
  'src/views/mes/pro/route/components/RouteScheduleStrategyEditor.vue'
)

assert.doesNotMatch(
  scheduleOrderSource,
  /旧小时产能/,
  '排产工单查看弹框不得把 FINITE_HOURLY 显示为“旧小时产能”。'
)

assert.match(
  scheduleOrderSource,
  /FINITE_HOURLY:\s*'小时产能'/,
  '排产工单查看弹框应将 FINITE_HOURLY 显示为中性的“小时产能”。'
)

assert.doesNotMatch(
  capacitySourceTagSource,
  /待迁移旧值/,
  '工艺流程产能来源标签不得显示“待迁移旧值”。'
)

assert.match(
  capacitySourceTagSource,
  /case 'FINITE_HOURLY':[\s\S]*return '小时产能'/,
  '工艺流程产能来源标签应将 FINITE_HOURLY 显示为中性的“小时产能”。'
)

assert.doesNotMatch(
  routeScheduleStrategyEditorSource,
  /旧策略仅展示/,
  '路线排产策略编辑器不得显示“旧策略仅展示”。'
)

assert.match(
  routeScheduleStrategyEditorSource,
  /历史策略仅展示，保存前请选择新策略。/,
  '路线排产策略编辑器应使用“历史策略”说明，不显示“旧策略”。'
)

console.log('PASS: scheduling capacity copy hides old wording')
