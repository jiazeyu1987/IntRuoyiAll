const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const routeGraph = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

function extractFunction(source, functionName) {
  const start = source.indexOf(`const ${functionName} =`)
  assert(start >= 0, `必须定义 ${functionName}`)
  const nextConst = source.indexOf('\nconst ', start + 1)
  return source.slice(start, nextConst >= 0 ? nextConst : source.length)
}

function assertIncludes(source, expected, label) {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert.ok(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

const diffHelper = extractFunction(routeGraph, 'isRouteProcessCapacityOverrideDifferentFromDefault')
assertIncludes(
  diffHelper,
  'calculateCapacityOverrideShiftCapacity(hourlyCapacity, shiftHours)',
  '覆盖产能差异必须按手工产能/h折算班次产能后比较'
)
assertIncludes(
  diffHelper,
  'defaultShiftCapacity',
  '覆盖产能差异必须与默认班次产能比较'
)
assertIncludes(
  diffHelper,
  'CAPACITY_OVERRIDE_DIFF_TOLERANCE',
  '覆盖产能差异必须容忍浮点尾差'
)

const activeComputed = extractFunction(routeGraph, 'isSelectedProcessCapacityOverrideActive')
assertIncludes(
  activeComputed,
  'isRouteProcessCapacityOverrideDifferentFromDefault(',
  '工作站卡片覆盖模式必须由差异判定决定'
)
assertIncludes(
  activeComputed,
  'selectedRouteProcess.value?.processShiftCapacityTotal',
  '工作站卡片覆盖模式必须拿当前工序默认班次产能作为比较基准'
)
assertNotIncludes(
  activeComputed,
  'positiveNumber(selectedProcessAttributes.hourlyCapacity)',
  '工作站卡片覆盖模式不得只因 MANUAL_OVERRIDE 和正数产能就展示'
)

const scheduleSummary = extractFunction(routeGraph, 'formatRouteProcessScheduleStrategySummary')
assertIncludes(
  scheduleSummary,
  'isSelectedProcessCapacityOverrideActive.value',
  '排产策略摘要必须同样尊重差异判定'
)

const submitFunction = extractFunction(routeGraph, 'submitCapacityOverride')
assertIncludes(
  submitFunction,
  'capacityModeToSave',
  '产能覆盖保存必须先判断是否真的不同'
)
assertIncludes(
  submitFunction,
  "capacityModeToSave === 'MANUAL_OVERRIDE'",
  '只有手工值与默认值不同时才保存 MANUAL_OVERRIDE 的 hourlyCapacity'
)
assertIncludes(
  submitFunction,
  ": 'RESOURCE_CALCULATED'",
  '手工值等于默认值时必须按资源计算保存'
)
assertNotIncludes(
  submitFunction,
  "capacityMode: 'MANUAL_OVERRIDE',\n      hourlyCapacity,",
  '产能覆盖保存不得无条件写 MANUAL_OVERRIDE'
)

console.log('mes-route-flow-capacity-override-diff-only-static PASS')
