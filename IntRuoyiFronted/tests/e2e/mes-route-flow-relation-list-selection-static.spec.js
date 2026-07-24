const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const designerFile = path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const read = (file) => fs.readFileSync(file, 'utf8')

function extractSelectedFieldDetailTemplate(content) {
  const start = content.indexOf('data-flow-panel="selected-field-detail"')
  assert.notEqual(start, -1, '字段明细面板应存在')
  const end = content.indexOf('</aside>', start)
  assert.notEqual(end, -1, '字段明细侧栏应正确闭合')
  return content.slice(start, end)
}

function extractComputedBlock(content, computedName) {
  const marker = `const ${computedName} = computed(() =>`
  const start = content.indexOf(marker)
  assert.notEqual(start, -1, `${computedName} computed 应存在`)
  const nextConst = content.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextConst, -1, `${computedName} computed 后续声明应存在`)
  return content.slice(start, nextConst)
}

function extractConstFunction(content, functionName) {
  const marker = `const ${functionName} =`
  const start = content.indexOf(marker)
  assert.notEqual(start, -1, `${functionName} 函数应存在`)
  const braceStart = content.indexOf('{', start)
  assert.notEqual(braceStart, -1, `${functionName} 函数体应存在`)
  let depth = 0
  for (let index = braceStart; index < content.length; index += 1) {
    const char = content[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) return content.slice(start, index + 1)
  }
  throw new Error(`${functionName} 函数体未闭合`)
}

test('route relation list is rendered on demand inside selected field detail', () => {
  const designer = read(designerFile)
  const selectedFieldDetailTemplate = extractSelectedFieldDetailTemplate(designer)
  const visibleBoundaryRelations = extractComputedBlock(designer, 'visibleBoundaryRelationEdges')
  const visibleRouteRelations = extractComputedBlock(designer, 'visibleRouteRelationEdges')

  assert.match(
    selectedFieldDetailTemplate,
    /v-if="selectedProcessDetailField\.key === 'relationList'"/
  )
  assert.match(selectedFieldDetailTemplate, /data-flow-panel="relation-list-detail"/)
  assert.match(selectedFieldDetailTemplate, /<h4>关系清单<\/h4>/)
  assert.match(selectedFieldDetailTemplate, /visibleBoundaryRelationEdges\.length === 0/)
  assert.match(selectedFieldDetailTemplate, /visibleRouteRelationEdges\.length === 0/)
  assert.match(selectedFieldDetailTemplate, /v-for="edge in visibleBoundaryRelationEdges"/)
  assert.match(selectedFieldDetailTemplate, /v-for="edge in visibleRouteRelationEdges"/)
  assert.match(selectedFieldDetailTemplate, /@click="handleBoundaryEdgeSelect\(edge\)"/)
  assert.match(selectedFieldDetailTemplate, /@click="handleEdgeSelect\(edge\)"/)
  assert.match(selectedFieldDetailTemplate, /@click\.stop="handleBoundaryEdgeDelete\(edge\)"/)
  assert.match(selectedFieldDetailTemplate, /@click\.stop="handleEdgeDelete\(edge\)"/)

  assert.match(visibleBoundaryRelations, /selectedRouteProcessId\.value/)
  assert.match(visibleBoundaryRelations, /return boundaryEdges\.value/)
  assert.match(visibleBoundaryRelations, /boundaryEdges\.value\.filter/)
  assert.match(visibleBoundaryRelations, /edge\.routeProcessId/)

  assert.match(visibleRouteRelations, /selectedRouteProcessId\.value/)
  assert.match(visibleRouteRelations, /return routeEdges\.value/)
  assert.match(visibleRouteRelations, /routeEdges\.value\.filter/)
  assert.match(visibleRouteRelations, /edge\.sourceRouteProcessId/)
  assert.match(visibleRouteRelations, /edge\.targetRouteProcessId/)

  assert.doesNotMatch(selectedFieldDetailTemplate, /v-for="edge in boundaryEdges"/)
  assert.doesNotMatch(selectedFieldDetailTemplate, /v-for="edge in routeEdges"/)
  assert.doesNotMatch(designer, /route-flow-graph-designer__relation-section/)
})

test('route edge selection keeps existing process and field panels unchanged', () => {
  const designer = read(designerFile)
  const handlers = [
    extractConstFunction(designer, 'handleEdgeClick'),
    extractConstFunction(designer, 'handleEdgeSelect'),
    extractConstFunction(designer, 'handleBoundaryEdgeSelect')
  ].join('\n')

  assert.match(handlers, /selectedEdgeKey\.value\s*=/, '连接线选择应更新连接线选中态')
  assert.doesNotMatch(
    handlers,
    /selectedRouteProcessId\.value\s*=\s*null/,
    '连接线选择不得清空当前工序面板'
  )
  assert.doesNotMatch(
    handlers,
    /selectedBoundaryType\.value\s*=\s*null/,
    '连接线选择不得清空当前边界面板'
  )
})
