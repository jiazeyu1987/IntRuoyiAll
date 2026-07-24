const fs = require('fs')
const path = require('path')

const component = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/route/RouteProcessList.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.resolve(__dirname, '../../src/api/mes/pro/route/process/index.ts'),
  'utf8'
)
const graphComponent = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

function assertIncludes(text, expected, label) {
  if (!text.includes(expected)) {
    throw new Error(`${label}: missing ${expected}`)
  }
}

function assertExcludes(text, unexpected, label) {
  if (text.includes(unexpected)) {
    throw new Error(`${label}: unexpected ${unexpected}`)
  }
}

assertIncludes(api, 'predecessor?: ProRouteProcessRelationVO', 'route process API predecessor')
assertIncludes(api, 'predecessors?: ProRouteProcessRelationVO[]', 'route process API predecessors')
assertIncludes(api, 'successors: ProRouteProcessRelationVO[]', 'route process API successors')
assertIncludes(component, 'label="前置工序"', 'route process table predecessor column')
assertIncludes(component, 'label="后续工序"', 'route process table successor column')
assertIncludes(component, 'getRouteProcessPredecessors(scope.row)', 'route process table predecessor list')
assertIncludes(component, 'scope.row.successors', 'route process table successor list')
assertIncludes(graphComponent, 'formatRouteProcessPredecessors(routeProcess)', 'graph detail predecessor list')
assertExcludes(component, 'label="下一道工序"', 'legacy single next process column')
assertExcludes(component, 'prop="linkType"', 'legacy single link type form field')

console.log('mes route process multiple successors static contract passed')
