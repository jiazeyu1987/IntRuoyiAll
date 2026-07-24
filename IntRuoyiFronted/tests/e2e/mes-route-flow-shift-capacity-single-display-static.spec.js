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

const integerShiftCapacityFormatter = extractFunction(
  routeGraph,
  'formatRouteProcessIntegerShiftCapacity'
)

assertIncludes(
  integerShiftCapacityFormatter,
  'formatRouteProcessIntegerCapacity(numberValue)',
  '班次产能整数展示必须继续复用整数格式化函数'
)
assertNotIncludes(
  integerShiftCapacityFormatter,
  '/班次',
  '流转图工作站卡片班次产能后面不得带 /班次 单位'
)
assertIncludes(
  routeGraph,
  'buildWorkstationShiftCapacityLink',
  '默认班次产能保留在工作站复合链接中，确保数值可点击跳转'
)
assertNotIncludes(
  routeGraph,
  'data-flow-capacity="resource-shift-capacity"',
  '默认资源计算模式不得再渲染第二个静态班次产能行'
)
assertNotIncludes(
  routeGraph,
  '<div v-else class="route-flow-graph-designer__workstation-capacity">',
  '工作站卡片默认模式不得在复合链接之外追加重复班次产能容器'
)

console.log('mes-route-flow-shift-capacity-single-display-static PASS')
