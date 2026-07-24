const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const workbench = read('src/views/mes/pro/scheduler-workbench/index.vue')
const scheduleOrderApi = read('src/api/mes/pro/scheduleorder/index.ts')
const routeFlowGraphDesigner = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

assert.match(
  scheduleOrderApi,
  /interface\s+MesProScheduleOrderProcessWipVO[\s\S]*capacityMode\?:/,
  'process WIP API contract must expose capacityMode for source navigation'
)

assert.match(
  scheduleOrderApi,
  /interface\s+MesProScheduleOrderProcessWipVO[\s\S]*capacitySource\?:/,
  'process WIP API contract must expose capacitySource for source navigation'
)

assert.match(
  scheduleOrderApi,
  /interface\s+MesProScheduleOrderProcessWipVO[\s\S]*routeVersionNo\?:[\s\S]*routeVersionStatus\?:/,
  'process WIP API contract must expose route version context for exact navigation'
)

assert.match(
  workbench,
  /@click\.stop=["']openProcessWipCapacitySource\(row\)["']/,
  'capacity cell click must stop row-click and open capacity source'
)

assert.match(
  workbench,
  /const\s+openProcessWipCapacitySource\s*=\s*\(item:\s*MesProScheduleOrderProcessWipVO\)/,
  'scheduler workbench must implement capacity source navigation handler'
)

assert.match(
  workbench,
  /name:\s*['"]MesProRouteEdit['"][\s\S]*tab:\s*['"]flow['"]/,
  'capacity source navigation must open the route edit flow page'
)

assert.match(
  workbench,
  /routeProcessId:\s*String\(item\.routeProcessId\)/,
  'capacity source navigation must target the exact route process'
)

assert.match(
  workbench,
  /routeVersionId:\s*String\(item\.routeVersionId\)[\s\S]*routeVersionNo:\s*item\.routeVersionNo[\s\S]*routeVersionStatus:\s*item\.routeVersionStatus/,
  'capacity source navigation must open the exact source route version context'
)

assert.match(
  workbench,
  /throw new Error\(['"]缺少路线版本上下文，无法定位班次产能来源['"]\)/,
  'missing route version context must fail fast before route navigation'
)

assert.match(
  workbench,
  /source:\s*['"]scheduler-workbench-capacity-source['"]/,
  'capacity source navigation must mark the scheduler workbench as the navigation source'
)

assert.match(
  workbench,
  /item\.capacitySource\s*===\s*['"]MACHINE['"]\)\s*return\s*['"]resource['"]/,
  'machine capacity source must focus resource fields'
)

assert.match(
  workbench,
  /item\.capacitySource\s*===\s*['"]WORKER['"]\)\s*return\s*['"]resource['"]/,
  'worker capacity source must focus resource fields'
)

assert.match(
  workbench,
  /item\.capacitySource\s*===\s*['"]MANUAL_OVERRIDE['"]\)\s*return\s*['"]schedule['"]/,
  'manual override capacity source must focus schedule strategy fields'
)

assert.match(
  workbench,
  /ElMessage\.error\(['"]缺少路线或路线工序标识，无法定位班次产能来源['"]\)/,
  'missing route identifiers must fail fast with a visible error'
)

assert.match(
  routeFlowGraphDesigner,
  /capacitySourceFocus/,
  'route flow graph must consume capacitySourceFocus from the URL'
)

assert.match(
  routeFlowGraphDesigner,
  /focusProcessDetailFieldsForCapacitySource/,
  'route flow graph must ensure capacity source detail fields are visible'
)

assert.match(
  routeFlowGraphDesigner,
  /highlightedProcessDetailFieldKey/,
  'route flow graph must highlight the destination field after navigation'
)

console.log('scheduler-workbench-capacity-source-navigation-static PASS')
