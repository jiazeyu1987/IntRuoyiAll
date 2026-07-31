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
const catalogDo = readOptional(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/mesprocess/MesProMesProcessCatalogDO.java'
)
const catalogMachineryDo = readOptional(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/mesprocess/MesProMesProcessCatalogMachineryDO.java'
)

const expectedRows = [
  {
    sourceRowNo: 2,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09393',
    mesProcessName: '粗洗',
    sourceMachineryName: '超声波清洗机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是',
    batchRecordProcessName: '粗洗'
  },
  {
    sourceRowNo: 3,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09353',
    mesProcessName: '精洗',
    sourceMachineryName: '超声波清洗机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是',
    batchRecordProcessName: '精洗'
  },
  {
    sourceRowNo: 4,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09353',
    mesProcessName: '清洗',
    sourceMachineryName: '超声波清洗机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '清洗'
  },
  {
    sourceRowNo: 5,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09041',
    mesProcessName: '烘干',
    sourceMachineryName: '箱型干燥机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '清洗'
  },
  {
    sourceRowNo: 6,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '清洁',
    sourceMachineryName: '无尘布/75%酒精',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是',
    batchRecordProcessName: '清洁'
  },
  {
    sourceRowNo: 7,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09340',
    mesProcessName: '组装',
    sourceMachineryName: '杠杆架自动组装机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '5800',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是',
    batchRecordProcessName: '组装Ⅰ'
  },
  {
    sourceRowNo: 8,
    productName: '二代压力泵',
    sourceMachineryCodes: 'A03378/A03377',
    mesProcessName: '编织管自动抽芯点胶',
    sourceMachineryName: '编织管自动抽芯点胶',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '7000',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 9,
    productName: '二代压力泵',
    sourceMachineryCodes: 'A05059',
    mesProcessName: '点胶二代编织管表',
    sourceMachineryName: '光固机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '3500',
    dailyWorkerQuantity: '3',
    mesProcessCode: 'Z1500',
    processPrice: '0.2224',
    feedbackFlag: '是',
    batchRecordFlag: '是',
    batchRecordProcessName: '光固'
  },
  {
    sourceRowNo: 10,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09026',
    mesProcessName: '硅化',
    sourceMachineryName: '喷套筒',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '9000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1520',
    processPrice: '0.0259',
    feedbackFlag: '是',
    batchRecordFlag: '是',
    batchRecordProcessName: '硅化Ⅱ'
  },
  {
    sourceRowNo: 11,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '硅化胶塞环',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '/',
    dailyWorkerQuantity: '/',
    mesProcessCode: '/',
    processPrice: '/',
    feedbackFlag: '/',
    batchRecordFlag: '是',
    batchRecordProcessName: '硅化Ⅲ'
  },
  {
    sourceRowNo: 12,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '螺杆硅化',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '9000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1530',
    processPrice: '0.0259',
    feedbackFlag: '是',
    batchRecordFlag: '是',
    batchRecordProcessName: '硅化Ⅰ'
  },
  {
    sourceRowNo: 13,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '组装后盖',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '4000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1490',
    processPrice: '0.0597',
    feedbackFlag: '是',
    batchRecordFlag: '是',
    batchRecordProcessName: '组装Ⅱ'
  },
  {
    sourceRowNo: 14,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '二代压力泵手柄耐压检测',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '4200',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1570',
    processPrice: '0.0738',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 15,
    productName: '二代压力泵',
    sourceMachineryCodes: 'G01034',
    mesProcessName: '压活塞',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '10000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1510',
    processPrice: '0.0254',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 16,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '套外套',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '4000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1650',
    processPrice: '0.0701',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 17,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '目测二代异物',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '1900',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1550',
    processPrice: '0.1911',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 18,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '点胶二代压力泵',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '4000',
    dailyWorkerQuantity: '3',
    mesProcessCode: 'Z1560',
    processPrice: '0.2272',
    feedbackFlag: '是',
    batchRecordFlag: '是',
    batchRecordProcessName: '组装Ⅲ'
  },
  {
    sourceRowNo: 19,
    productName: '二代压力泵',
    sourceMachineryCodes: 'B09032/G01160',
    mesProcessName: '二代压力泵负压检测',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '2',
    dailyCapacity10_5: '4000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1610',
    processPrice: '0.0834',
    feedbackFlag: '是',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '检测'
  },
  {
    sourceRowNo: 20,
    productName: '二代压力泵',
    sourceMachineryCodes: 'G01143',
    mesProcessName: '测二代压力泵全套',
    sourceMachineryName: '小气压检测',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '2000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1580',
    processPrice: '0.1509',
    feedbackFlag: '是',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '检测'
  },
  {
    sourceRowNo: 21,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '全检压力泵（内）',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '2500',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z1590',
    processPrice: '0.1406',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 22,
    productName: '二代压力泵',
    sourceMachineryCodes: 'A05199/A05203',
    mesProcessName: '压力泵热合(顶头袋封口）',
    sourceMachineryName: '封口热合机',
    sourceMachineryQuantity: '2',
    dailyCapacity10_5: '7600',
    dailyWorkerQuantity: '8',
    mesProcessCode: 'Z560',
    processPrice: '0.3338',
    feedbackFlag: '是',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '单包装'
  },
  {
    sourceRowNo: 23,
    productName: '二代压力泵',
    sourceMachineryCodes: 'A05048/A03274',
    mesProcessName: '压力泵热合（吸塑盒面纸热合）',
    sourceMachineryName: '封口热合机',
    sourceMachineryQuantity: '2',
    dailyCapacity10_5: '7600',
    dailyWorkerQuantity: '8',
    mesProcessCode: 'Z560',
    processPrice: '0.3338',
    feedbackFlag: '是',
    batchRecordFlag: '是（两道合并）',
    batchRecordProcessName: '单包装'
  },
  {
    sourceRowNo: 24,
    productName: '二代压力泵',
    sourceMachineryCodes: '/',
    mesProcessName: '全检压力泵',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '3800',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z5623',
    processPrice: '0.0834',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 25,
    productName: '二代压力泵',
    sourceMachineryCodes: 'G01235',
    mesProcessName: '贴条形码',
    sourceMachineryName: '贴标机',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '10000',
    dailyWorkerQuantity: '1',
    mesProcessCode: 'Z6133',
    processPrice: '0.0235',
    feedbackFlag: '是',
    batchRecordFlag: '/',
    batchRecordProcessName: '/'
  },
  {
    sourceRowNo: 26,
    productName: '压力泵（硬吸塑）',
    sourceMachineryCodes: '/',
    mesProcessName: 'W贴产品标签（大标签）',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '9581',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: '中包装'
  },
  {
    sourceRowNo: 27,
    productName: '压力泵（硬吸塑）',
    sourceMachineryCodes: '/',
    mesProcessName: 'W贴产品标签（小标签）',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '37405',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: '大包装'
  },
  {
    sourceRowNo: 28,
    productName: '压力泵（硬吸塑）',
    sourceMachineryCodes: '/',
    mesProcessName: '压力泵中盒（说明书）',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '3412',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  },
  {
    sourceRowNo: 29,
    productName: '压力泵（硬吸塑）',
    sourceMachineryCodes: 'G01248',
    mesProcessName: 'W包装打包',
    sourceMachineryName: '包装线',
    sourceMachineryQuantity: '1',
    dailyCapacity10_5: '8180',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  },
  {
    sourceRowNo: 30,
    productName: '压力泵（散装套袋）',
    sourceMachineryCodes: '/',
    mesProcessName: '散装压力泵（套袋）',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '1838',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  },
  {
    sourceRowNo: 31,
    productName: '压力泵（散装套袋）',
    sourceMachineryCodes: '/',
    mesProcessName: 'W包装打包',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '20450',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  },
  {
    sourceRowNo: 32,
    productName: '压力泵（散装不套袋）',
    sourceMachineryCodes: '/',
    mesProcessName: '散装压力泵',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '3937',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  },
  {
    sourceRowNo: 33,
    productName: '压力泵（散装不套袋）',
    sourceMachineryCodes: '/',
    mesProcessName: 'W包装打包',
    sourceMachineryName: '/',
    sourceMachineryQuantity: '/',
    dailyCapacity10_5: '24540',
    dailyWorkerQuantity: '',
    mesProcessCode: '',
    processPrice: '',
    feedbackFlag: '',
    batchRecordFlag: '',
    batchRecordProcessName: ''
  }
]

const sqlString = (value) => `'${String(value).replace(/'/g, "''")}'`

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
assert.ok(catalogService.includes('getSourceRowNo'), '服务必须保留 Excel 源行号用于精确核对')
assert.ok(catalogDo.includes('@TenantIgnore'), 'Excel 只读目录主表必须忽略租户过滤，避免 tenant_id=0 源基线在业务租户下不可见')
assert.ok(catalogMachineryDo.includes('@TenantIgnore'), 'Excel 只读目录设备明细必须忽略租户过滤，避免明细表被当前租户条件过滤')
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

for (const [index, row] of expectedRows.entries()) {
  const sourceId = 9003131001 + index
  const sortNo = index + 1
  const catalogCode = `PUMP2-MES-${String(sortNo).padStart(4, '0')}`
  const expectedTuple = [
    sourceId,
    sqlString('压力泵工序.xlsx'),
    sqlString('二代压力泵'),
    row.sourceRowNo,
    sortNo,
    sqlString(catalogCode),
    sqlString(row.productName),
    sqlString(row.sourceMachineryCodes),
    sqlString(row.mesProcessName),
    sqlString(row.sourceMachineryName),
    sqlString(row.sourceMachineryQuantity),
    sqlString(row.dailyCapacity10_5),
    sqlString(row.dailyWorkerQuantity),
    sqlString(row.mesProcessCode),
    sqlString(row.processPrice),
    sqlString(row.feedbackFlag),
    sqlString(row.batchRecordFlag),
    sqlString(row.batchRecordProcessName),
    sqlString('codex'),
    sqlString('codex'),
    "b'0'",
    0
  ].join(', ')
  assert.ok(catalogSql.includes(`source_row_no=${row.sourceRowNo}`), `缺少源行号标记：${row.sourceRowNo}`)
  assert.ok(catalogSql.includes(`(${expectedTuple})`), `SQL 种子与 Excel 第 ${row.sourceRowNo} 行 12 列原始值不一致`)
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
