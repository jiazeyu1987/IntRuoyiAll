const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (source, expected, label) => {
  if (!source.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (source, expected, label) => {
  if (source.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const extractConstFunction = (source, functionName) => {
  const marker = `const ${functionName} =`
  const start = source.indexOf(marker)
  if (start === -1) {
    throw new Error(`${functionName} missing`)
  }
  const nextConst = source.indexOf('\nconst ', start + marker.length)
  const end = nextConst === -1 ? source.length : nextConst
  return source.slice(start, end)
}

const refreshBindingHandler = extractConstFunction(
  component,
  'refreshCapacityWorkstationRepairBinding'
)
const submitRepairHandler = extractConstFunction(component, 'submitCapacityWorkstationRepair')

assertIncludes(
  component,
  'const refreshCapacityWorkstationRepairBinding = async (routeProcessId: number) =>',
  'workstation repair local refresh handler'
)
assertNotIncludes(
  refreshBindingHandler,
  'loadGraph(',
  'workstation repair refresh handler must not reload the full graph'
)
assertNotIncludes(
  refreshBindingHandler,
  'handleFitScreen(',
  'workstation repair refresh handler must not reset viewport'
)
assertNotIncludes(
  refreshBindingHandler,
  'fitGraphAfterLayout(',
  'workstation repair refresh handler must not auto-fit layout'
)
assertIncludes(
  refreshBindingHandler,
  'await loadSelectedProcessDetail(targetNode)',
  'workstation repair refresh handler must reload only selected process detail'
)
assertIncludes(
  refreshBindingHandler,
  'selectedRouteProcessId.value = routeProcessId',
  'workstation repair refresh handler must keep current route process selected'
)
assertIncludes(
  submitRepairHandler,
  'await refreshCapacityWorkstationRepairBinding(routeProcessId)',
  'workstation repair submit must use local refresh handler'
)

console.log('mes-route-flow-workstation-repair-no-reload-static PASS')
