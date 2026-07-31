const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const readOptional = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

const page = read('src/views/mes/pro/mes-process/index.vue').replace(/\r\n/g, '\n')
const api = read('src/api/mes/pro/mes-process/index.ts').replace(/\r\n/g, '\n')
const routerSearch = read('src/components/RouterSearch/index.vue').replace(/\r\n/g, '\n')
const menuSql = read('../IntRuoyiBackend/sql/mysql/20260730_mes_process_readonly_catalog_menu.sql')
const catalogSql = readOptional('../IntRuoyiBackend/sql/mysql/20260731_mes_process_catalog_from_pressure_pump_xlsx.sql')
const catalogController = readOptional(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/mesprocess/MesProMesProcessController.java'
)
const catalogService = readOptional(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/mesprocess/MesProMesProcessServiceImpl.java'
)

const expectedRows = [
  [2, '二代压力泵', '粗洗', 'B09393', '粗洗'],
  [3, '二代压力泵', '精洗', 'B09353', '精洗'],
  [4, '二代压力泵', '清洗', 'B09353', '清洗'],
  [5, '二代压力泵', '烘干', 'B09041', '清洗'],
  [6, '二代压力泵', '清洁', '/', '清洁'],
  [7, '二代压力泵', '组装', 'B09340', '组装Ⅰ'],
  [8, '二代压力泵', '编织管自动抽芯点胶', 'A03378/A03377', '/'],
  [9, '二代压力泵', '点胶二代编织管表', 'A05059', '光固'],
  [10, '二代压力泵', '硅化', 'B09026', '硅化Ⅱ'],
  [11, '二代压力泵', '硅化胶塞环', '/', '硅化Ⅲ'],
  [12, '二代压力泵', '螺杆硅化', '/', '硅化Ⅰ'],
  [13, '二代压力泵', '组装后盖', '/', '组装Ⅱ'],
  [14, '二代压力泵', '二代压力泵手柄耐压检测', '/', '/'],
  [15, '二代压力泵', '压活塞', 'G01034', '/'],
  [16, '二代压力泵', '套外套', '/', '/'],
  [17, '二代压力泵', '目测二代异物', '/', '/'],
  [18, '二代压力泵', '点胶二代压力泵', '/', '组装Ⅲ'],
  [19, '二代压力泵', '二代压力泵负压检测', 'B09032/G01160', '检测'],
  [20, '二代压力泵', '测二代压力泵全套', 'G01143', '检测'],
  [21, '二代压力泵', '全检压力泵（内）', '/', '/'],
  [22, '二代压力泵', '压力泵热合(顶头袋封口）', 'A05199/A05203', '单包装'],
  [23, '二代压力泵', '压力泵热合（吸塑盒面纸热合）', 'A05048/A03274', '单包装'],
  [24, '二代压力泵', '全检压力泵', '/', '/'],
  [25, '二代压力泵', '贴条形码', 'G01235', '/'],
  [26, '压力泵（硬吸塑）', 'W贴产品标签（大标签）', '/', '中包装'],
  [27, '压力泵（硬吸塑）', 'W贴产品标签（小标签）', '/', '大包装'],
  [28, '压力泵（硬吸塑）', '压力泵中盒（说明书）', '/', ''],
  [29, '压力泵（硬吸塑）', 'W包装打包', 'G01248', ''],
  [30, '压力泵（散装套袋）', '散装压力泵（套袋）', '/', ''],
  [31, '压力泵（散装套袋）', 'W包装打包', '/', ''],
  [32, '压力泵（散装不套袋）', '散装压力泵', '/', ''],
  [33, '压力泵（散装不套袋）', 'W包装打包', '/', '']
]

assert.ok(page.includes('【生产】MES工序'), '页面标题必须显示为 MES工序')
assert.ok(
  page.includes('压力泵工序.xlsx') && page.includes('二代压力泵'),
  'MES工序页面必须说明当前列表来自压力泵工序.xlsx 的二代压力泵工作表'
)
assert.ok(!page.includes('标准模板列表'), 'MES工序页面不得继续显示标准模板列表旧标题')

for (const label of [
  '产品名称',
  '设备编码',
  '工序名称',
  '设备名称',
  '设备数量',
  '10.5小时日产能',
  '日常工序人力',
  '工序编码',
  '工序单价',
  '工序是否报工',
  '工序是否形成批记录',
  '批记录工序名称'
]) {
  assert.ok(page.includes(label), `MES工序只读页缺少 Excel 原始列：${label}`)
}

assert.ok(page.includes('MesProcessApi.getMesProcessPage'), '页面必须调用独立 MES 工序目录分页 API')
assert.ok(api.includes("url: '/mes/pro/mes-process/page'"), '前端 API 必须调用独立 MES 工序目录端点')
assert.ok(!api.includes('ProRouteResourceApi'), 'MES 工序列表不得复用产品工艺资源聚合 API')
assert.ok(!api.includes('/mes/pro/route-resource/page'), 'MES 工序列表不得调用 route-resource/page')
assert.ok(!page.includes('routeCode'), 'MES 工序列表不得展示非 Excel 来源的路线列')
assert.ok(!page.includes('feedbackEnabled'), '是否报工必须展示 Excel 原文，不得转换成布尔状态')
assert.ok(!page.includes('batchRecordEnabled'), '是否形成批记录必须展示 Excel 原文，不得转换成状态标签')
assert.ok(page.includes('formatSourceText'), '页面必须以源表原值展示 / 与空白')

assert.ok(catalogController.includes('/mes/pro/mes-process'), '后端必须提供独立 MES 工序目录 Controller')
assert.ok(catalogController.includes("'mes:pro-mes-process:query'"), '独立接口必须使用 MES工序查询权限')
assert.ok(catalogService.includes('MesProMesProcessCatalogMapper'), '服务必须读取正式 MES 工序目录表')
assert.ok(catalogService.includes('sourceRowNo'), '服务必须保留 Excel 源行号用于精确核对')
assert.ok(catalogSql.includes('mes_pro_mes_process_catalog'), '迁移必须创建或填充 MES 工序目录主表')
assert.ok(catalogSql.includes('mes_pro_mes_process_catalog_machinery'), '迁移必须拆分设备编码明细表')
assert.equal(
  (catalogSql.match(/PUMP2-MES-/g) || []).length,
  expectedRows.length,
  '目录种子必须恰好包含二代压力泵 32 条有效工序'
)
assert.ok(!catalogSql.includes('source_row_no, 34'), '第 34 行孤立产能值不得作为 MES 工序导入')
assert.ok(!catalogSql.includes('source_row_no, 35'), '第 35 行孤立产能值不得作为 MES 工序导入')
assert.ok(!catalogSql.includes('source_row_no, 36'), '第 36 行孤立产能值不得作为 MES 工序导入')

for (const [sourceRowNo, productName, processName, machineryCodes, batchRecordProcessName] of expectedRows) {
  assert.ok(catalogSql.includes(`PUMP2-MES-${String(sourceRowNo - 1).padStart(4, '0')}`), `缺少目录编码：sourceRowNo=${sourceRowNo}`)
  assert.ok(catalogSql.includes(`'${productName}'`), `缺少产品名称：${productName}`)
  assert.ok(catalogSql.includes(`'${processName}'`), `缺少工序名称：${processName}`)
  assert.ok(catalogSql.includes(`source_row_no=${sourceRowNo}`), `缺少源行号标记：${sourceRowNo}`)
  assert.ok(catalogSql.includes(`'${machineryCodes}'`), `缺少源设备编码：${machineryCodes}`)
  if (batchRecordProcessName) {
    assert.ok(catalogSql.includes(`'${batchRecordProcessName}'`), `缺少批记录工序名称：${batchRecordProcessName}`)
  }
}

for (const machineryCode of ['B09032', 'G01160', 'A05199', 'A05203', 'A05048', 'A03274']) {
  assert.ok(catalogSql.includes(`'${machineryCode}'`), `斜杠设备必须拆分为设备明细：${machineryCode}`)
}

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
  assert.ok(!page.includes(forbidden), `MES工序只读页不得包含维护能力：${forbidden}`)
}

assert.ok(menuSql.includes("CONVERT(UNHEX('4D4553E5B7A5E5BA8F') USING utf8mb4)"))
assert.ok(
  menuSql.includes("CONVERT(UNHEX('4D4553E5B7A5E5BA8FE69FA5E8AFA2') USING utf8mb4)")
)
assert.ok(!menuSql.includes("E6A087E58786E6A8A1E69DBFE58897E8A1A8"), '菜单 SQL 不得继续写入标准模板列表')
assert.ok(menuSql.includes('5718'))
assert.ok(menuSql.includes("'mes-process'"))
assert.ok(menuSql.includes("'mes/pro/mes-process/index'"))
assert.ok(menuSql.includes("'MesProMesProcess'"))
assert.ok(menuSql.includes('5719'))
assert.ok(menuSql.includes("'mes:pro-mes-process:query'"))
assert.ok(menuSql.includes('5710 THEN 20'))
assert.ok(menuSql.includes('5718 THEN 25'))
assert.ok(menuSql.includes('5720 THEN 30'))
assert.ok(routerSearch.includes('ROUTER_SEARCH_ALIASES'), '菜单搜索必须支持重命名后的旧关键词别名')
assert.ok(routerSearch.includes("'/mes/pro/mes-process'"), 'MES工序路由必须登记搜索别名')
assert.ok(routerSearch.includes("'MES工序'"), 'MES工序必须可通过 mes工序 小写关键词搜索')
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

console.log('PASS: MES工序只读目录与压力泵工序.xlsx 静态合同')
