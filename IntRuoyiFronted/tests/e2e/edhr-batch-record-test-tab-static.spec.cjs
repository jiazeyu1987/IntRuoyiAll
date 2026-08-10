const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const tabs = read('src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const routes = read('src/router/modules/remaining.ts')
const api = read('src/api/system/codexTestManagement/index.ts')
const pagePath = path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue')
const visibleMenuSqlPath = path.join(
  root,
  '..',
  'IntRuoyiBackend',
  'sql/mysql/20260808_mes_edhr_batch_record_test_menu.sql'
)

assert.ok(fs.existsSync(pagePath), '批记录测试页面组件必须存在。')
assert.ok(
  fs.existsSync(visibleMenuSqlPath),
  '必须提供 admin 可见菜单迁移，否则从芋道源码/admin 进入看不到“批记录测试”。'
)
const page = fs.readFileSync(pagePath, 'utf8')
const visibleMenuSql = fs.readFileSync(visibleMenuSqlPath, 'utf8')

assert.doesNotMatch(
  tabs,
  /<el-tab-pane\s+label="批记录测试"\s+name="test"\s*\/>/,
  '批记录测试必须是类似 PQC 组长的独立菜单页，不能挂在批次执行顶部页签内。'
)
assert.doesNotMatch(
  tabs,
  /\|\s*'test'|test:\s*'\/mes\/pro\/feedback\/edhr-batch-test'/,
  '批次执行顶部页签类型和跳转表不得包含批记录测试。'
)
assert.match(
  routes,
  /path:\s*'pro\/feedback\/edhr-batch-test'[\s\S]*BatchRecordTestPage\.vue[\s\S]*permission:\s*\['mes:pro-edhr-batch-execution:query'\]/,
  '必须新增隐藏路由并沿用 eDHR 批次执行查询权限。'
)
assert.match(
  visibleMenuSql,
  /release-migration:[\s\S]*type=menu/,
  '批记录测试 admin 可见入口必须通过正式菜单迁移交付。'
)
assert.match(
  visibleMenuSql,
  /900440[\s\S]*'批记录测试'[\s\S]*'mes:pro-edhr-batch-execution:query'[\s\S]*'\/mes\/pro\/feedback\/edhr-batch-test'[\s\S]*'mes\/pro\/edhr-batch\/BatchRecordTestPage'[\s\S]*'MesProEdhrBatchRecordTest'/,
  'admin 可见菜单必须指向批记录测试页面并沿用批次执行查询权限。'
)
assert.match(
  visibleMenuSql,
  /system_tenant_package[\s\S]*CAST\('900440' AS JSON\)/,
  '批记录测试菜单必须加入目标租户套餐，否则 admin 菜单不可见。'
)
assert.match(
  visibleMenuSql,
  /system_role_menu[\s\S]*900440/,
  '批记录测试菜单必须绑定 admin 角色菜单，否则 admin 角色看不到入口。'
)
assert.match(
  visibleMenuSql,
  /SIGNAL SQLSTATE '45000'/,
  '菜单迁移必须 fail fast，不能静默跳过缺失前置。'
)
assert.match(
  visibleMenuSql,
  /900440 AS `menu_id`, 6 AS `sort`[\s\S]*900033 AS `menu_id`, 7 AS `sort`/,
  '批记录测试必须作为独立菜单排在批次执行前，而不是批次执行内部页签。'
)

assert.doesNotMatch(
  page,
  /EdhrBatchRecordTabs|active-tab="test"/,
  '批记录测试页面不得渲染批次执行顶部页签。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-page/,
  '批记录测试独立页面必须提供稳定页面级 DOM 锚点。'
)
assert.doesNotMatch(
  page,
  /edhr-batch-record-test-page__header|edhr-batch-record-test-page__title|edhr-batch-record-test-page__subtitle|独立测试页签/,
  '截图黄框中的批记录测试说明页头和独立测试页签标签不应显示。'
)
assert.match(
  page,
  /<el-tabs[\s\S]*v-model="activeInnerTab"[\s\S]*<el-tab-pane\s+label="生产组长"\s+name="productionLeader"/,
  '页面必须提供生产组长内部 tab。'
)
assert.match(
  page,
  /<el-tabs[\s\S]*v-model="activeInnerTab"[\s\S]*<el-tab-pane\s+label="一线PQC"\s+name="frontlinePqc"[\s\S]*<el-tab-pane\s+label="一线生产"\s+name="frontlineProduction"/,
  '页面必须保留一线PQC内部 tab，并在其后新增一线生产内部 tab。'
)
assert.match(
  page,
  /<el-tab-pane\s+label="一线生产"\s+name="frontlineProduction"[\s\S]*<el-tab-pane\s+label="订单分配"\s+name="orderAllocation"/,
  '页面必须在一线生产后提供订单分配内部 tab。'
)
assert.match(
  page,
  /<el-tab-pane\s+label="订单分配"\s+name="orderAllocation"[\s\S]*<el-tab-pane\s+label="批记录映射"\s+name="batchRecordMapping"/,
  '页面必须在订单分配后提供批记录映射内部 tab。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.productionLeader"[\s\S]*@pagination="handleProductionLeaderPagination"/,
  '生产组长列表必须使用标准列表模板和稳定 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-production-leader-list/,
  '生产组长列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.frontlinePqc"[\s\S]*@pagination="handleFrontlinePqcPagination"/,
  '一线PQC列表必须使用标准列表模板和稳定 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-frontline-pqc-list/,
  '一线PQC列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.frontlineProduction"[\s\S]*@pagination="handleFrontlineProductionPagination"/,
  '一线生产列表必须使用标准列表模板和稳定 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-frontline-production-list/,
  '一线生产列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.orderAllocation"[\s\S]*@pagination="handleOrderAllocationPagination"/,
  '订单分配列表必须使用标准列表模板和稳定 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-order-allocation-list/,
  '订单分配列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.batchRecordMapping"[\s\S]*@pagination="handleBatchRecordMappingPagination"/,
  '批记录映射列表必须使用标准列表模板和稳定 table-key。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-mapping-list/,
  '批记录映射列表必须提供稳定 DOM 锚点。'
)
assert.match(
  page,
  /<template\s+#actions\s*>[\s\S]*测试租户[\s\S]*<\/template>/,
  '操作面板必须保留测试租户选择。'
)
assert.equal(
  (page.match(/:single-line-toolbar="true"/g) || []).length,
  5,
  '五张批记录测试列表都必须启用标准单行工具栏，让筛选和操作面板同一行展示。'
)
assert.equal(
  (page.match(/:show-column-settings="false"/g) || []).length,
  5,
  '五张批记录测试列表都必须隐藏标准列表“显示字段”入口。'
)
assert.doesNotMatch(
  page,
  /Runner：|刷新状态|edhr-batch-record-test-page__runner-message/,
  '截图黄框中的 Runner 状态、心跳消息和刷新状态按钮不应显示。'
)
const toolbarActionBlocks = Array.from(
  page.matchAll(/<template\s+#actions\s*>([\s\S]*?)<\/template>/g)
).map((match) => match[1])
assert.equal(
  toolbarActionBlocks.length,
  5,
  '生产组长、一线PQC、一线生产、订单分配、批记录映射五张列表都必须各自提供操作面板。'
)
for (const [index, listKey] of [
  'productionLeader',
  'frontlinePqc',
  'frontlineProduction',
  'orderAllocation',
  'batchRecordMapping'
].entries()) {
  assert.match(
    toolbarActionBlocks[index],
    /测试租户[\s\S]*<el-select[\s\S]*v-model="selectedTenantId"/,
    `${listKey} 操作面板必须保留测试租户下拉选择。`
  )
  assert.match(
    toolbarActionBlocks[index],
    new RegExp(
      `@click="openCreateRowDialog\\('${listKey}'\\)"[\\s\\S]*>\\s*新增\\s*<\\/el-button>`
    ),
    `${listKey} 操作面板必须在同一行提供新增按钮，并绑定正式新增入口。`
  )
}
assert.match(
  page,
  /<el-button[\s\S]*v-hasPermi="\['system:codex-test:execute'\]"[\s\S]*@click="handleTestRow\(row\)"[\s\S]*>\s*测试\s*<\/el-button>/,
  '每行操作列必须提供测试按钮并受执行权限控制。'
)
assert.doesNotMatch(
  page,
  /label="测试项名称"|label:\s*'测试项名称'/,
  '批记录测试所有列表不应展示“测试项名称”列，也不应把该字段放入用户列设置。'
)
assert.equal(
  (page.match(/key:\s*'caseName'/g) || []).length,
  0,
  '三张列表默认列池都必须移除 caseName 列。'
)
assert.equal(
  (page.match(/openCreateRowDialog\('/g) || []).length,
  5,
  '五张列表都必须提供新增按钮。'
)
assert.equal(
  (page.match(/openDescriptionEditor\('/g) || []).length,
  5,
  '五张列表都必须提供修改描述操作。'
)
assert.equal((page.match(/handleDeleteRow\('/g) || []).length, 5, '五张列表都必须提供删除行操作。')
assert.match(
  page,
  /data-edhr-batch-record-test-create-dialog[\s\S]*v-model="createEditor\.title"[\s\S]*v-model="createEditor\.description"[\s\S]*@click="saveCreatedRow"/,
  '新增按钮必须打开新增弹框，填写任务和描述后保存。'
)
assert.match(
  page,
  /function\s+openCreateRowDialog\([\s\S]*createEditor\.listKey[\s\S]*createEditor\.visible\s*=\s*true/,
  '新增入口必须按当前列表初始化新增态。'
)
assert.match(
  page,
  /function\s+buildCreatedBatchRecordTestRow\([\s\S]*caseName:[\s\S]*testScope:/,
  '新增保存必须生成测试项名称和测试范围，确保新增行仍可执行测试。'
)
assert.match(
  page,
  /function\s+saveCreatedRow\([\s\S]*updateBatchRecordTestRows[\s\S]*buildCreatedBatchRecordTestRow[\s\S]*message\.success\('已新增'\)/,
  '新增保存必须写入当前列表，不得只打开弹框或显示提示。'
)
assert.match(
  page,
  /data-edhr-batch-record-test-description-dialog[\s\S]*v-model="descriptionEditor\.description"[\s\S]*@click="saveDescriptionEdit"/,
  '修改操作必须打开描述编辑弹框，并通过保存动作更新当前行描述。'
)
assert.match(
  page,
  /function\s+openDescriptionEditor\([\s\S]*descriptionEditor\.rowId[\s\S]*descriptionEditor\.description\s*=\s*row\.description/,
  '修改描述必须按当前行 ID 初始化编辑态。'
)
assert.match(
  page,
  /function\s+saveDescriptionEdit\([\s\S]*updateBatchRecordTestRows[\s\S]*description:\s*nextDescription/,
  '保存描述必须写回当前列表行，不得只修改弹框临时值。'
)
assert.match(
  page,
  /async function\s+saveDescriptionEdit\([\s\S]*await\s+upsertCodeReadonlyCase\(updatedRow\)[\s\S]*updateBatchRecordTestRows[\s\S]*message\.success\('描述已修改'\)/,
  '修改描述必须先完成正式测试项持久化，再更新当前列表并提示成功。'
)
assert.match(
  page,
  /async function\s+saveDescriptionEdit\([\s\S]*const\s+editingListKey\s*=\s*descriptionEditor\.listKey[\s\S]*const\s+editingRowId\s*=\s*descriptionEditor\.rowId[\s\S]*await\s+upsertCodeReadonlyCase\(updatedRow\)[\s\S]*updateBatchRecordTestRows\(editingListKey[\s\S]*row\.id\s*===\s*editingRowId/,
  '异步保存必须冻结当前列表和行 ID，避免请求期间切换弹框后误改其它行。'
)
assert.match(
  page,
  /import\s*\{\s*getTenantId\s*\}\s*from\s*'@\/utils\/auth'/,
  '描述缓存必须按当前登录租户隔离。'
)
assert.match(
  page,
  /const\s+BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION\s*=\s*1[\s\S]*function\s+getBatchRecordTestDescriptionCacheKey\([\s\S]*getTenantId\(\)/,
  '描述缓存必须使用带版本且包含当前登录租户的稳定 key。'
)
assert.ok(
  page.indexOf('const BATCH_RECORD_TEST_DESCRIPTION_CACHE_VERSION = 1') <
    page.indexOf('hydrateBatchRecordTestDescriptionCache()'),
  '描述缓存常量必须先于 setup 阶段的同步水合调用初始化，避免刷新时触发暂时性死区错误。'
)
assert.match(
  page,
  /function\s+hydrateBatchRecordTestDescriptionCache\([\s\S]*readBatchRecordTestDescriptionCache\([\s\S]*updateBatchRecordTestRows/,
  '页面 setup 阶段必须同步读取描述缓存并水合三组列表，避免刷新首帧显示旧默认值。'
)
assert.match(
  page,
  /captureDefaultBatchRecordTestRows\([\s\S]*hydrateBatchRecordTestDescriptionCache\(\)/,
  '必须先冻结正式默认定义再水合缓存，确保后端不存在记录时可恢复正式默认值。'
)
assert.match(
  page,
  /async function\s+saveDescriptionEdit\([\s\S]*await\s+upsertCodeReadonlyCase\(updatedRow\)[\s\S]*updateBatchRecordTestRows\(editingListKey[\s\S]*syncBatchRecordTestDescriptionCache\(\)[\s\S]*message\.success\('描述已修改'\)/,
  '正式保存成功后必须在成功提示前同步当前登录租户的描述缓存。'
)
assert.match(
  page,
  /描述已保存，但本地缓存更新失败/,
  '后端保存成功但缓存更新失败时必须分层提示，不能误报后端保存失败。'
)
assert.match(
  page,
  /function\s+buildCodeReadonlyCasePayload\([\s\S]*remark:\s*definition\.description/,
  '描述必须写入检查点 remark 结构化字段，不能只存在于前端内存。'
)
assert.match(
  page,
  /async function\s+loadPersistedBatchRecordTestRows\([\s\S]*getCodexTestCasePage\([\s\S]*project:\s*'批记录'[\s\S]*checkpoint\.remark[\s\S]*updateBatchRecordTestRows/,
  '页面加载必须从批记录测试项的检查点 remark 恢复已保存描述。'
)
assert.match(
  page,
  /async function\s+loadPersistedBatchRecordTestRows\([\s\S]*getDefaultBatchRecordTestRows\(listKey\)[\s\S]*syncBatchRecordTestDescriptionCache\(\)/,
  '正式查询成功后必须从冻结默认定义重建列表并校准描述缓存，不得保留服务端已删除记录的旧缓存。'
)
assert.match(
  page,
  /if\s*\(!persistedCase\)\s*return row[\s\S]*if\s*\(!checkpoint\s*\|\|\s*!checkpoint\.remark\?\.trim\(\)\)\s*\{[\s\S]*throw new Error/,
  '不存在持久化测试项时可以使用代码定义；一旦测试项存在却缺少结构化描述，必须 fail fast。'
)
assert.doesNotMatch(
  page,
  /if\s*\(!checkpoint\s*\|\|\s*!checkpoint\.remark\?\.trim\(\)\)\s*return row/,
  '已存在测试项缺少 remark 时不得静默回退到源码旧描述。'
)
assert.match(
  page,
  /onMounted\(async\s*\(\)\s*=>\s*\{[\s\S]*loadPersistedBatchRecordTestRows\(\)/,
  '页面刷新时必须执行正式持久化读取。'
)
assert.match(
  page,
  /async function\s+handleDeleteRow\([\s\S]*message\.confirm[\s\S]*updateBatchRecordTestRows[\s\S]*filter\(\(item\)\s*=>\s*item\.id\s*!==\s*row\.id\)/,
  '删除操作必须确认后从当前列表移除对应行。'
)

const duties = [
  '工艺路线生产组长配置',
  '批记录解析与工序配置',
  '生产人员管理',
  '报工分配与生产进度',
  '活跃订单与检验进度'
]
for (const duty of duties) {
  assert.ok(page.includes(duty), `职责列表必须包含：${duty}`)
}
assert.equal(
  (page.match(/caseName:\s*'批记录测试-生产组长-/g) || []).length,
  5,
  '生产组长职责列表必须固定 5 行。'
)

const frontlinePqcTasks = [
  '活跃订单池选择订单',
  '按产品读取工艺路线工序',
  '按工序加载QA检验项',
  '检验项名称与方法展示',
  '首检检验数量读取',
  '巡检抽样数量计算',
  '电子密码提交',
  '提交进入PQC组长管理列表'
]
for (const task of frontlinePqcTasks) {
  assert.ok(page.includes(task), `一线PQC任务列表必须包含：${task}`)
}
assert.equal(
  (page.match(/caseName:\s*'批记录测试-一线PQC-/g) || []).length,
  8,
  '一线PQC任务列表必须固定 8 行。'
)
for (const requiredText of [
  '所有生产组长维护的活跃订单池',
  '点击工序卡片',
  '检验项tab',
  '检验项名称而不是编号',
  '检验方法',
  '生产10000、抽样率0.4时检验数量为40',
  '电子密码',
  'PQC管理列表'
]) {
  assert.ok(page.includes(requiredText), `一线PQC职责描述必须包含：${requiredText}`)
}

const frontlineProductionTasks = [
  '一线生产入口与组长身份',
  '负责工序卡片来源',
  '负责员工卡片来源',
  '工序上下文数据联动',
  '设备可选性',
  '设备参数可选性',
  '设备参数限制规则',
  '电子密码与待分配报工'
]
for (const task of frontlineProductionTasks) {
  assert.ok(page.includes(task), `一线生产任务列表必须包含：${task}`)
}
assert.equal(
  (page.match(/caseName:\s*'批记录测试-一线生产-/g) || []).length,
  8,
  '一线生产任务列表必须固定 8 行。'
)
for (const requiredText of [
  '自己的账号进入一线生产',
  '工序配置列表中负责的工序',
  '人员管理下维护的启用员工',
  '不良、设备和设备参数',
  '无设备工序',
  '无参数',
  '所选员工的电子密码',
  '报工管理页签等待分配'
]) {
  assert.ok(page.includes(requiredText), `一线生产职责描述必须包含：${requiredText}`)
}

assert.match(
  api,
  /export type CodexTestAnalysisMode = 'PLAYWRIGHT_E2E' \| 'CODE_READONLY'/,
  '前端 API 必须声明 analysisMode 枚举。'
)
assert.match(api, /analysisMode\??:\s*CodexTestAnalysisMode/, '测试项 VO 必须包含 analysisMode。')
assert.match(
  api,
  /analysisModeSnapshot\??:\s*CodexTestAnalysisMode/,
  '执行快照 VO 必须包含 analysisModeSnapshot。'
)

assert.match(
  page,
  /getCodexTestCasePage\(\{[\s\S]*project:\s*'批记录'[\s\S]*name:\s*definition\.caseName/,
  '测试前必须按项目和精确名称查找测试项。'
)
assert.match(
  page,
  /existingCase\s*=\s*pageResult\.list\.find\([\s\S]*item\.name\s*===\s*definition\.caseName[\s\S]*item\.project\s*===\s*'批记录'/,
  'upsert 必须二次精确匹配名称和项目，避免误用模糊结果。'
)
assert.match(
  page,
  /analysisMode:\s*'CODE_READONLY'/,
  '生产组长测试项必须创建或更新为 CODE_READONLY。'
)
assert.match(
  page,
  /methodText:\s*'只读扫描当前代码，判断业务方向是否偏离' \+ definition\.testScope/,
  '批记录测试方法必须按业务方向判定生成自然语言方法，不能要求完整实现证明。'
)
assert.match(
  page,
  /expectedText:[\s\S]*当前代码、路由、API、权限和页面文案的业务方向与[\s\S]*一致/,
  '批记录测试目标必须判断业务方向是否一致。'
)
assert.doesNotMatch(
  page,
  /分析是否已经完整支持|数据模型和测试能够满足/,
  '批记录测试默认方法和目标不得继续使用完整实现正确性口径。'
)
assert.match(
  page,
  /updateCodexTestCase\(\{[\s\S]*id:\s*existingCase\.id[\s\S]*casePayload/,
  '已存在测试项时必须更新定义。'
)
assert.match(page, /createCodexTestCase\(casePayload\)/, '不存在测试项时必须创建定义。')
assert.match(
  page,
  /startCodeReadonlyCodexTestExecution\(\{[\s\S]*targetTenantId:\s*selectedTenantId\.value[\s\S]*caseDefinition:\s*buildCodeReadonlyCasePayload\(row\)/,
  '测试按钮必须通过专用原子接口启动单项只读代码执行。'
)
assert.match(
  page,
  /getCodexTestExecutionResult\(executionId\)/,
  '测试按钮启动后必须读取并展示正式 Codex CLI 回复。'
)

assert.doesNotMatch(
  page,
  /child_process|spawn\s*\(|execFile|codex(?:\.cmd)?\s+exec/,
  '浏览器前端不得直接裸调 Codex CLI。'
)
assert.doesNotMatch(page, /catch\s*\{\s*\}/, '页面不得使用空 catch 吞异常。')

console.log('edhr-batch-record-test-tab-static PASS')
