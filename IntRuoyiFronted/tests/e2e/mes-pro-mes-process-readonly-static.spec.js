const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = read('src/views/mes/pro/mes-process/index.vue').replace(/\r\n/g, '\n')
const api = read('src/api/mes/pro/mes-process/index.ts').replace(/\r\n/g, '\n')
const resourceApi = read('src/api/mes/pro/route/resource.ts').replace(/\r\n/g, '\n')
const routerSearch = read('src/components/RouterSearch/index.vue').replace(/\r\n/g, '\n')
const menuSql = read('../IntRuoyiBackend/sql/mysql/20260730_mes_process_readonly_catalog_menu.sql')
const resourceController = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/route/MesProRouteResourceController.java'
)

assert.ok(page.includes('【生产】标准模板列表'), '页面标题必须显示为标准模板列表')
assert.ok(
  page.includes('只读列表：展示一线 MES 工序、设备、工序设置执行工序和批记录工序名称的对应关系。'),
  '标准模板列表页面必须保留只读说明'
)

for (const label of [
  '产品名称',
  '设备',
  'MES工序名称',
  '设备数量',
  '10.5小时日产能',
  '日产人力',
  'MES工序编码',
  '工序单价',
  '报工',
  '批记录',
  '批记录工序名称',
  '执行工序'
]) {
  assert.ok(page.includes(label), `标准模板列表只读页缺少列：${label}`)
}

assert.ok(page.includes('MesProcessApi.getMesProcessPage'), '页面必须调用独立只读分页 API')
assert.ok(page.includes('machineryList'), '页面必须结构化展示多设备列表')
assert.ok(page.includes('executionProcessName'), '页面必须展示关联的执行工序')
assert.ok(api.includes('ProRouteResourceApi.getResourcePage'), 'API 必须复用现有产品工艺资源读模型')
assert.ok(resourceApi.includes('/mes/pro/route-resource/page'), '资源读模型必须保留现有分页端点')
assert.ok(resourceApi.includes('batchRecordReportName'), '资源读模型类型必须返回批记录工序名称')

for (const forbidden of [
  'createMesProcess',
  'updateMesProcess',
  'deleteMesProcess',
  'handleExport',
  "openForm('create')",
  "openForm('update')",
  '新增',
  '编辑',
  '删除',
  '导入',
  '启用',
  '停用'
]) {
  assert.ok(!page.includes(forbidden), `标准模板列表只读页不得包含维护能力：${forbidden}`)
}

assert.ok(menuSql.includes("CONVERT(UNHEX('E6A087E58786E6A8A1E69DBFE58897E8A1A8') USING utf8mb4)"))
assert.ok(
  menuSql.includes("CONVERT(UNHEX('E6A087E58786E6A8A1E69DBFE58897E8A1A8E69FA5E8AFA2') USING utf8mb4)")
)
assert.ok(menuSql.includes('5718'))
assert.ok(menuSql.includes("'mes-process'"))
assert.ok(menuSql.includes("'mes/pro/mes-process/index'"))
assert.ok(menuSql.includes("'MesProMesProcess'"))
assert.ok(menuSql.includes('5719'))
assert.ok(menuSql.includes("'mes:pro-mes-process:query'"))
assert.ok(resourceController.includes("'mes:pro-mes-process:query'"))
assert.ok(menuSql.includes('5710 THEN 20'))
assert.ok(menuSql.includes('5718 THEN 25'))
assert.ok(menuSql.includes('5720 THEN 30'))
assert.ok(routerSearch.includes('ROUTER_SEARCH_ALIASES'), '菜单搜索必须支持重命名后的旧关键词别名')
assert.ok(routerSearch.includes("'/mes/pro/mes-process'"), '标准模板列表路由必须登记搜索别名')
assert.ok(routerSearch.includes("'MES工序'"), '标准模板列表必须可通过 MES工序 旧关键词搜索')
assert.ok(routerSearch.includes('normalizeSearchText'), '搜索匹配必须统一大小写，支持 mes工序 小写输入')
assert.ok(
  routerSearch.includes('routeMatchesSearchQuery(item, keyword.value)'),
  '菜单搜索过滤必须同时匹配标题、路径和路由别名'
)
assert.ok(
  !/const\s+routers\s*=\s*router\.getRoutes\(\)/.test(routerSearch),
  '菜单搜索不得在 setup 初始化阶段缓存静态路由表，必须覆盖登录后的动态菜单路由'
)
assert.ok(
  /function\s+getSearchRoutes\(\)\s*\{[\s\S]*router\.getRoutes\(\)/.test(routerSearch),
  '菜单搜索必须通过 getSearchRoutes 实时读取最新 Vue Router 路由表'
)
assert.ok(
  /getSearchRoutes\(\)\.filter\(\(item: any\)/.test(routerSearch),
  '菜单搜索过滤必须基于最新动态路由表生成选项'
)

console.log('PASS: 标准模板列表只读页面静态合同')
