const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const processListPath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteProcessList.vue')
const graphDesignerPath = path.resolve(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const apiPath = path.resolve(frontendRoot, 'src/api/mes/pro/route/flowconfig.ts')
const packageJsonPath = path.resolve(frontendRoot, 'package.json')

const processListSource = fs.readFileSync(processListPath, 'utf8')
const graphDesignerSource = fs.readFileSync(graphDesignerPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts?.['e2e:mes:schedule-route-production-factor:static'],
  'node tests/e2e/mes-schedule-route-production-factor-static.spec.js',
  'package.json must expose the MES schedule route production factor static contract'
)

assert.match(
  apiSource,
  /interface ProRouteFlowProcessConfigVO[\s\S]*productionQuantityFactor\?: number \| null/,
  'route flow process config query contract must expose productionQuantityFactor'
)
assert.match(
  apiSource,
  /interface ProRouteFlowProcessConfigSaveVO[\s\S]*productionQuantityFactor\?: number \| null/,
  'route flow process config save contract must submit productionQuantityFactor'
)

const factorColumnMatch = processListSource.match(
  /<el-table-column[\s\S]*?prop="productionQuantityFactor"[\s\S]*?<\/el-table-column>/
)
assert.ok(factorColumnMatch, '工序设置列表必须显示生产系数列。')
const factorColumn = factorColumnMatch[0]
assert.match(
  factorColumn,
  /:model-value="getProcessSettingsDraft\(scope\.row\)\.productionQuantityFactor"/,
  '生产系数输入必须绑定到当前工序设置草稿 productionQuantityFactor。'
)
assert.match(factorColumn, /:min="0\.000001"/, '生产系数输入最小值必须大于 0。')
assert.match(factorColumn, /:step="0\.01"/, '生产系数输入步进必须为 0.01。')
assert.match(factorColumn, /:precision="2"/, '生产系数输入必须保留 2 位精度。')
assert.match(
  factorColumn,
  /data-route-process-setting-field="productionQuantityFactor"/,
  '生产系数输入必须保留稳定测试标识。'
)

assert.match(
  processListSource,
  /const DEFAULT_PRODUCTION_QUANTITY_FACTOR = 1/,
  '前端必须集中定义生产系数默认值 1。'
)
assert.match(
  processListSource,
  /productionQuantityFactor:\s*normalizeProductionQuantityFactor\([\s\S]*productionQuantityFactor/,
  '应用工作台默认值和排产配置合并时必须只补齐空生产系数为 1。'
)
assert.match(
  processListSource,
  /const normalizeProductionQuantityFactor = \(value\?: number \| string \| null\) =>[\s\S]*DEFAULT_PRODUCTION_QUANTITY_FACTOR/,
  '前端必须把空生产系数规范化为默认 1。'
)
assert.match(
  processListSource,
  /if \(productionQuantityFactor <= 0\)/,
  '工序设置保存前必须校验生产系数大于 0。'
)
assert.match(
  processListSource,
  /生产系数必须大于 0/,
  '非法生产系数必须提示对应工序。'
)
assert.match(
  processListSource,
  /const editingRouteVersionId = requireCandidateRouteVersionId\('工序设置保存'\)/,
  '工序设置保存必须先要求 DRAFT 候选路线版本。'
)
assert.match(
  processListSource,
  /ProRouteFlowConfigApi\.saveScheduleConfig\(\{[\s\S]*routeVersionId: editingRouteVersionId/,
  '工序设置保存排产用途配置时必须提交 DRAFT routeVersionId。'
)
assert.match(
  processListSource,
  /productionQuantityFactor,/,
  '保存用途配置时必须随每道工序提交生产系数。'
)
assert.match(
  graphDesignerSource,
  /key: 'productionQuantityFactor'[\s\S]*label: getRouteProcessSettingColumnLabel\('productionQuantityFactor', '生产系数'\)/,
  '流转关系图详情必须继续展示生产系数摘要。'
)
assert.match(
  graphDesignerSource,
  /const productionQuantityFactor = normalizeProductionQuantityFactor\(draft\.productionQuantityFactor\)[\s\S]*positiveNumber\(productionQuantityFactor\)/,
  '流转关系图保存工序属性时必须校验生产系数大于 0。'
)

console.log('PASS: MES schedule route production factor static contract')
