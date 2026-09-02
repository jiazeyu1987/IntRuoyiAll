const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeEditorPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'route',
  'RouteFlowGraphDesigner.vue'
)
const flowConfigApiPath = path.join(repoRoot, 'src', 'api', 'mes', 'pro', 'route', 'flowconfig.ts')

const routeEditor = fs.readFileSync(routeEditorPath, 'utf8')
const flowConfigApi = fs.readFileSync(flowConfigApiPath, 'utf8')

assert.match(flowConfigApi, /inputMaterialIds\?:\s*number\[\]/, '流程配置 API 必须暴露输入物料 ID 列表。')
assert.match(flowConfigApi, /outputMaterialIds\?:\s*number\[\]/, '流程配置 API 必须暴露输出物料 ID 列表。')
assert.doesNotMatch(flowConfigApi, /frontlineReportMaterialIds\?:\s*number\[\]/, '旧批记录物料字段不得作为正式 API 字段。')

assert.match(routeEditor, />\s*输入物料\s*</, '工艺路线工序属性面板必须显示输入物料。')
assert.match(routeEditor, />\s*输出物料\s*</, '工艺路线工序属性面板必须显示输出物料。')
assert.match(
  routeEditor,
  /data-route-process-setting-field="input-material"[\s\S]*placeholder="输入物料编号、名称或规格"/,
  '输入物料必须使用独立选择器。'
)
assert.match(
  routeEditor,
  /data-route-process-setting-field="output-material"[\s\S]*placeholder="输出物料编号、名称或规格"/,
  '输出物料必须使用独立选择器。'
)
assert.match(
  routeEditor,
  /inputMaterialIds:\s*normalizeRouteProcessMaterialIds\(draft\.inputMaterialIds\)/,
  '保存载荷必须写入输入物料字段。'
)
assert.match(
  routeEditor,
  /outputMaterialIds:\s*normalizeRouteProcessMaterialIds\(draft\.outputMaterialIds\)/,
  '保存载荷必须写入输出物料字段。'
)
assert.doesNotMatch(routeEditor, /frontlineReportMaterialIds/, '前端路线编辑器不得继续读写旧批记录物料字段。')

console.log('PASS: route process input/output materials are explicit and independent')
