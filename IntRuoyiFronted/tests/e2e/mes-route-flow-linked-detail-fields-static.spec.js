const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeGraphPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'route', 'RouteFlowGraphDesigner.vue')
const machineryPath = path.join(repoRoot, 'src', 'views', 'mes', 'dv', 'machinery', 'index.vue')

const routeGraph = fs.readFileSync(routeGraphPath, 'utf8')
const machinery = fs.readFileSync(machineryPath, 'utf8')

function assertIncludes(source, expected, label) {
  assert(source.includes(expected), `${label}: expected to find ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert(!source.includes(unexpected), `${label}: expected not to find ${JSON.stringify(unexpected)}`)
}

assertIncludes(routeGraph, 'data-flow-action="open-process-detail-link"', 'selected detail link action marker')
assertIncludes(routeGraph, 'field.links?.length', 'selected detail renders links before plain text')
assertIncludes(routeGraph, 'buildProcessDetailMachineryLinks', 'machinery links builder')
assertIncludes(routeGraph, 'openMachineryTargetLink', 'machinery navigation handler')
assertIncludes(routeGraph, "path: '/mes/dv/machinery'", 'machinery target path')
assertIncludes(routeGraph, 'openId: machinery.machineryId', 'machinery target query')
assertIncludes(routeGraph, 'selectedProcessMachineryList', 'selected process machinery state')
assertIncludes(routeGraph, 'ProProcessApi.getProcessMachineryList(node.processId)', 'selected node loads machinery links')
assertNotIncludes(routeGraph, "editor: 'record-form'", 'batch record form fields must render as column values only')
assertNotIncludes(routeGraph, 'BatchRecordReportApi.getGeneratedReportPage', 'red-box batch record select options must not load')
assertNotIncludes(routeGraph, 'catch {}', 'route graph must not swallow navigation or load errors')

assertIncludes(machinery, 'const route = useRoute()', 'machinery page route query access')
assertIncludes(machinery, 'tryOpenDetailFromRoute', 'machinery openId detail opener')
assertIncludes(machinery, 'route.query.openId', 'machinery openId query read')
assertIncludes(machinery, "openForm('detail', Number(openId))", 'machinery detail opens from openId')
assertIncludes(machinery, 'watch(', 'machinery watches route query changes')

console.log('PASS route flow linked detail fields static contract')
