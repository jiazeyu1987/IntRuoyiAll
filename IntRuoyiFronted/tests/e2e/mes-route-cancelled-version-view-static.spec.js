const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const routeListPath = path.join(frontendRoot, 'src/views/mes/pro/route/index.vue')
const routeEditPath = path.join(frontendRoot, 'src/views/mes/pro/route/RouteEditPage.vue')
const routeGraphPath = path.join(
  frontendRoot,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)

const routeList = fs.readFileSync(routeListPath, 'utf8')
const routeEdit = fs.readFileSync(routeEditPath, 'utf8')
const routeGraph = fs.readFileSync(routeGraphPath, 'utf8')

const getFunctionBody = (source, functionName, asyncFunction = false) => {
  const marker = `const ${functionName} = ${asyncFunction ? 'async ' : ''}`
  const start = source.indexOf(marker)
  assert.notEqual(start, -1, `必须定义 ${functionName}。`)
  const nextFunction = source.indexOf('\nconst ', start + marker.length)
  assert.notEqual(nextFunction, -1, `必须能截取 ${functionName} 函数体。`)
  return source.slice(start, nextFunction)
}

const canViewRouteVersion = getFunctionBody(routeList, 'canViewRouteVersion')
assert.match(
  canViewRouteVersion,
  /version\.active\s*\|\|\s*version\.lifecycleStatus\s*!==\s*'DRAFT'/,
  '版本工作区必须允许 CANCELLED / REJECTED / SUPERSEDED 等非草稿版本进入只读查看。'
)
assert.doesNotMatch(
  canViewRouteVersion,
  /CANCELLED|REJECTED|SUPERSEDED/,
  '查看按钮不应按单个历史状态硬编码，非草稿历史版本统一走只读查看。'
)

const viewer = getFunctionBody(routeList, 'openRouteVersionViewer', true)
assert.match(
  viewer,
  /query:\s*version\.active\s*\?\s*\{\s*tab:\s*'flow'\s*\}/,
  'ACTIVE 版本查看仍必须使用当前生效版本上下文。'
)
assert.match(
  viewer,
  /routeVersionId:\s*String\(version\.id\)[\s\S]*routeVersionNo:\s*version\.versionNo[\s\S]*routeVersionStatus:\s*version\.lifecycleStatus/,
  '非 ACTIVE 历史版本查看必须把 routeVersionId、versionNo 和 lifecycleStatus 传到编辑页。'
)
assert.doesNotMatch(
  viewer,
  /submitAndPublishRouteCandidateVersion|createRouteCandidateVersion|cancelRouteCandidateVersion/,
  '查看历史版本不得触发创建、提交发布或取消候选版本动作。'
)

assert.match(
  routeEdit,
  /const routeVersionEditContext = computed<RouteVersionEditContext \| undefined>\(\(\) => \{[\s\S]*routeVersionId[\s\S]*routeVersionNo[\s\S]*routeVersionStatus/s,
  '编辑页必须继续从 query 构建只读版本上下文。'
)
assert.match(
  routeGraph,
  /const isFrozenRouteVersionView = computed\([\s\S]*routeVersionEditContext\?\.routeVersionId[\s\S]*lifecycleStatus !== 'DRAFT'/,
  '非 DRAFT 路线版本必须在关系图设计器中识别为冻结只读视图。'
)
assert.match(
  routeGraph,
  /const routeFlowWriteControlsDisabled = computed\([\s\S]*!isEditable\.value \|\| isFrozenRouteVersionView\.value/,
  '冻结历史版本视图必须禁用关系图写控件。'
)
assert.match(
  routeGraph,
  /ProRouteApi\.getRouteProcessFlowGraph\(props\.routeId,\s*resolveRouteFlowGraphReadRouteVersionId\(\)\)/,
  '关系图加载必须把历史 routeVersionId 传给后端读取冻结快照。'
)

console.log('PASS: mes route cancelled version uses readonly historical viewer')
