const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const graph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const formContent = read('src/views/mes/pro/route/RouteFormContent.vue')

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

const extractTemplateBlock = (content, className) => {
  const marker = `class="${className}"`
  const markerIndex = content.indexOf(marker)
  if (markerIndex === -1) {
    throw new Error(`${className} block missing`)
  }
  const start = content.lastIndexOf('<div', markerIndex)
  const end = content.indexOf('</div>', markerIndex)
  if (start === -1 || end === -1) {
    throw new Error(`${className} block is incomplete`)
  }
  return content.slice(start, end + '</div>'.length)
}

const toolbarSummary = extractTemplateBlock(graph, 'route-flow-graph-designer__summary')

assertIncludes(formContent, ':route-name="formData.name"', 'parent must pass current route name')
assertIncludes(graph, 'routeName: string', 'graph designer route name prop')
assertIncludes(graph, 'route-flow-graph-designer__route-name', 'toolbar route name slot')
assertIncludes(toolbarSummary, '{{ props.routeName }}', 'toolbar summary must render route name')
assertIncludes(toolbarSummary, ':title="props.routeName"', 'route name title must keep complete value')
assertNotIncludes(toolbarSummary, 'routeNodes.length', 'toolbar summary process count')
assertNotIncludes(toolbarSummary, 'routeEdges.length', 'toolbar summary relation count')
assertNotIncludes(toolbarSummary, 'selectedNodeFullName', 'route name slot must not reuse selected process name')
assertIncludes(graph, 'data-flow-status="route-version-summary"', 'toolbar version summary visible')
assertIncludes(graph, 'route-flow-graph-designer__selected-full-name', 'selected process full name remains separate')
assertIncludes(graph, '{{ selectedNodeFullName }}', 'selected process full name visible text remains')

console.log('mes-route-flow-toolbar-route-name-static PASS')
