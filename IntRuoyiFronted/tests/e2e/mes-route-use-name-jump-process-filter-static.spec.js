const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteFlowConfigPanel.vue')
const routePagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/index.vue')

assert(fs.existsSync(pagePath), `required file missing: ${pagePath}`)
assert(fs.existsSync(routePagePath), `required file missing: ${routePagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const routePageSource = fs.readFileSync(routePagePath, 'utf8')

assert.match(
  pageSource,
  /const router = useRouter\(\)/,
  '工艺流程排产配置/工艺流程批记录配置用途页必须持有 router，用于路线名称跳转。'
)
assert.match(
  pageSource,
  /<el-button\s+link\s+type="primary"\s+@click="openSourceRoutePage\(row\)">/,
  '路线名称文本必须跳转到基础工艺流程/工艺路线页，而不是打开详情弹窗。'
)
assert.match(
  pageSource,
  /const openSourceRoutePage = \(row: ProRouteVO\) => \{[\s\S]*router\.push\(\{[\s\S]*name: 'MesProRoute'[\s\S]*code: row\.code[\s\S]*name: row\.name[\s\S]*\}\)/,
  '路线名称跳转必须进入 MesProRoute，并携带 row.code 与 row.name 作为筛选参数。'
)
assert.doesNotMatch(
  pageSource,
  /@click="openRouteDetail\(row\)"/,
  '路线名称不得继续打开源工艺路线详情弹窗。'
)
assert.doesNotMatch(
  pageSource,
  /routeFormRef\.value\?\.open\('detail', row\.id\)/,
  '用途路线页不得继续通过 RouteForm 弹窗承载路线名称点击。'
)
assert.match(
  pageSource,
  /<el-button\s+link\s+type="primary"\s+@click="openUseConfig\(row\)">/,
  '路线编码文本必须继续打开用途配置。'
)
assert.match(
  pageSource,
  /@click\.stop="copyRouteName\(row\)"/,
  '路线名称复制按钮必须继续阻止触发行跳转。'
)
assert.match(
  routePageSource,
  /const loadListFromRoute = async \(\) => \{[\s\S]*queryParams\.code = typeof route\.query\.code === 'string' \? route\.query\.code : undefined[\s\S]*queryParams\.name = typeof route\.query\.name === 'string' \? route\.query\.name : undefined[\s\S]*await getList\(\)/,
  '基础工艺流程/工艺路线页必须接收 code/name 查询参数并自动筛选列表。'
)

console.log('PASS: MES route flow name jump process filter static contract')
