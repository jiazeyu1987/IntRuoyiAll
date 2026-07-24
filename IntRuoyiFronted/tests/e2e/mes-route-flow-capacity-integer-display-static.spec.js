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

const integerCapacityFormatter = extractFunction(routeGraph, 'formatRouteProcessIntegerCapacity')
assertIncludes(
  integerCapacityFormatter,
  'maximumFractionDigits: 0',
  '流转图工作站卡片产能整数格式化必须最多显示 0 位小数'
)
assertNotIncludes(
  integerCapacityFormatter,
  'maximumFractionDigits: 6',
  '流转图工作站卡片产能整数格式化不得继续展示 6 位小数尾差'
)

const integerShiftCapacityFormatter = extractFunction(
  routeGraph,
  'formatRouteProcessIntegerShiftCapacity'
)
assertIncludes(
  integerShiftCapacityFormatter,
  'formatRouteProcessIntegerCapacity(numberValue)',
  '流转图工作站卡片班次产能必须复用整数产能格式化'
)
assertIncludes(
  routeGraph,
  'formatRouteProcessIntegerShiftCapacity(row?.processShiftCapacityTotal)',
  '工作站复合链接中的班次产能必须展示整数'
)
assertIncludes(
  routeGraph,
  'formatRouteProcessIntegerShiftCapacity(selectedRouteProcess?.processShiftCapacityTotal)',
  '工作站卡片原班次产能与资源班次产能必须展示整数'
)
assertIncludes(
  routeGraph,
  'formatRouteProcessIntegerCapacity(selectedProcessAttributes.hourlyCapacity)',
  '工作站卡片覆盖产能/h 必须展示整数'
)
assertIncludes(
  routeGraph,
  'formatRouteProcessIntegerShiftCapacity(selectedProcessCapacityOverrideShiftCapacity)',
  '工作站卡片覆盖班次产能必须展示整数'
)

assertNotIncludes(
  routeGraph,
  '原班次产能：{{ formatRouteProcessShiftCapacity(selectedRouteProcess?.processShiftCapacityTotal) }}',
  '工作站卡片原班次产能不得继续使用小数格式化'
)
assertNotIncludes(
  routeGraph,
  '覆盖产能：{{ formatRouteProcessCapacity(selectedProcessAttributes.hourlyCapacity) }} 产能/h',
  '工作站卡片覆盖产能/h 不得继续使用小数格式化'
)
assertNotIncludes(
  routeGraph,
  '覆盖班次产能：{{ formatRouteProcessShiftCapacity(selectedProcessCapacityOverrideShiftCapacity) }}',
  '工作站卡片覆盖班次产能不得继续使用小数格式化'
)

console.log('mes-route-flow-capacity-integer-display-static PASS')
