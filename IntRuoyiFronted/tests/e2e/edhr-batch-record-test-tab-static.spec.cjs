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
assert.ok(fs.existsSync(visibleMenuSqlPath), '必须提供 admin 可见菜单迁移，否则从芋道源码/admin 进入看不到“批记录测试”。')
const page = fs.readFileSync(pagePath, 'utf8')
const visibleMenuSql = fs.readFileSync(visibleMenuSqlPath, 'utf8')

assert.doesNotMatch(tabs, /<el-tab-pane\s+label="批记录测试"\s+name="test"\s*\/>/, '批记录测试必须是类似 PQC 组长的独立菜单页，不能挂在批次执行顶部页签内。')
assert.doesNotMatch(tabs, /\|\s*'test'|test:\s*'\/mes\/pro\/feedback\/edhr-batch-test'/, '批次执行顶部页签类型和跳转表不得包含批记录测试。')
assert.match(routes, /path:\s*'pro\/feedback\/edhr-batch-test'[\s\S]*BatchRecordTestPage\.vue[\s\S]*permission:\s*\['mes:pro-edhr-batch-execution:query'\]/, '必须新增隐藏路由并沿用 eDHR 批次执行查询权限。')
assert.match(visibleMenuSql, /release-migration:[\s\S]*type=menu/, '批记录测试 admin 可见入口必须通过正式菜单迁移交付。')
assert.match(visibleMenuSql, /900440[\s\S]*'批记录测试'[\s\S]*'mes:pro-edhr-batch-execution:query'[\s\S]*'\/mes\/pro\/feedback\/edhr-batch-test'[\s\S]*'mes\/pro\/edhr-batch\/BatchRecordTestPage'[\s\S]*'MesProEdhrBatchRecordTest'/, 'admin 可见菜单必须指向批记录测试页面并沿用批次执行查询权限。')
assert.match(visibleMenuSql, /system_tenant_package[\s\S]*CAST\('900440' AS JSON\)/, '批记录测试菜单必须加入目标租户套餐，否则 admin 菜单不可见。')
assert.match(visibleMenuSql, /system_role_menu[\s\S]*900440/, '批记录测试菜单必须绑定 admin 角色菜单，否则 admin 角色看不到入口。')
assert.match(visibleMenuSql, /SIGNAL SQLSTATE '45000'/, '菜单迁移必须 fail fast，不能静默跳过缺失前置。')
assert.match(visibleMenuSql, /900440 AS `menu_id`, 6 AS `sort`[\s\S]*900033 AS `menu_id`, 7 AS `sort`/, '批记录测试必须作为独立菜单排在批次执行前，而不是批次执行内部页签。')

assert.doesNotMatch(page, /EdhrBatchRecordTabs|active-tab="test"/, '批记录测试页面不得渲染批次执行顶部页签。')
assert.match(page, /data-edhr-batch-record-test-page/, '批记录测试独立页面必须提供稳定页面级 DOM 锚点。')
assert.match(page, /edhr-batch-record-test-page__title[\s\S]*批记录测试/, '批记录测试独立页面必须展示页面标题。')
assert.match(page, /<el-tabs[\s\S]*v-model="activeInnerTab"[\s\S]*<el-tab-pane\s+label="生产组长"\s+name="productionLeader"/, '页面必须提供生产组长内部 tab。')
assert.match(page, /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhrBatchRecordTest\.productionLeader"[\s\S]*@pagination="handleProductionLeaderPagination"/, '生产组长列表必须使用标准列表模板和稳定 table-key。')
assert.match(page, /data-edhr-batch-record-test-production-leader-list/, '生产组长列表必须提供稳定 DOM 锚点。')
assert.match(page, /<template\s+#actions\s*>[\s\S]*测试租户[\s\S]*refreshRunnerStatus/, '操作面板必须提供测试租户选择和 Runner 状态刷新。')
assert.match(page, /<el-button[\s\S]*v-hasPermi="\['system:codex-test:execute'\]"[\s\S]*@click="handleTestRow\(row\)"[\s\S]*>\s*测试\s*<\/el-button>/, '每行操作列必须提供测试按钮并受执行权限控制。')

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
const dutyTitleCount = (page.match(/title:\s*'/g) || []).length
assert.equal(dutyTitleCount, 5, '生产组长职责列表必须固定 5 行。')

assert.match(api, /export type CodexTestAnalysisMode = 'PLAYWRIGHT_E2E' \| 'CODE_READONLY'/, '前端 API 必须声明 analysisMode 枚举。')
assert.match(api, /analysisMode\??:\s*CodexTestAnalysisMode/, '测试项 VO 必须包含 analysisMode。')
assert.match(api, /analysisModeSnapshot\??:\s*CodexTestAnalysisMode/, '执行快照 VO 必须包含 analysisModeSnapshot。')

assert.match(page, /getCodexTestCasePage\(\{[\s\S]*project:\s*'批记录'[\s\S]*name:\s*definition\.caseName/, '测试前必须按项目和精确名称查找测试项。')
assert.match(page, /existingCase\s*=\s*pageResult\.list\.find\([\s\S]*item\.name\s*===\s*definition\.caseName[\s\S]*item\.project\s*===\s*'批记录'/, 'upsert 必须二次精确匹配名称和项目，避免误用模糊结果。')
assert.match(page, /analysisMode:\s*'CODE_READONLY'/, '生产组长测试项必须创建或更新为 CODE_READONLY。')
assert.match(page, /updateCodexTestCase\(\{[\s\S]*id:\s*existingCase\.id[\s\S]*casePayload/, '已存在测试项时必须更新定义。')
assert.match(page, /createCodexTestCase\(casePayload\)/, '不存在测试项时必须创建定义。')
assert.match(page, /startCodexTestExecution\(\{[\s\S]*targetTenantId:\s*selectedTenantId\.value[\s\S]*executionMode:\s*'SEQUENTIAL'[\s\S]*caseIds:\s*\[caseId\]/, '测试按钮必须走后端执行接口启动单项顺序执行。')

assert.doesNotMatch(page, /child_process|spawn\s*\(|execFile|codex(?:\.cmd)?\s+exec/, '浏览器前端不得直接裸调 Codex CLI。')
assert.doesNotMatch(page, /catch\s*\{\s*\}/, '页面不得使用空 catch 吞异常。')

console.log('edhr-batch-record-test-tab-static PASS')
