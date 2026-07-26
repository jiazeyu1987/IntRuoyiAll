const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/system/codexTestManagement/index.ts')
const page = read('src/views/system/codex-test-management/index.vue')
const recordPagePath = path.join(root, 'src/views/system/codex-test-record/index.vue')
assert.ok(fs.existsSync(recordPagePath), '测试记录页签必须拆分为独立页面组件。')
const recordPage = fs.readFileSync(recordPagePath, 'utf8')
const runner = read('scripts/codex-test-runner.mjs')
const menuSqlPath = path.resolve(root, '../IntRuoyiBackend/sql/mysql/20260726_system_codex_test_record_menu.sql')
assert.ok(fs.existsSync(menuSqlPath), '测试记录页签必须有独立菜单迁移。')
const menuSql = fs.readFileSync(menuSqlPath, 'utf8')

for (const endpoint of [
  '/system/codex-test-case/page',
  '/system/codex-test-case/create',
  '/system/codex-test-execution/start',
  '/system/codex-test-execution/artifact'
]) {
  assert.ok(api.includes(endpoint), `missing API endpoint ${endpoint}`)
}

for (const permission of [
  'system:codex-test:create',
  'system:codex-test:update',
  'system:codex-test:delete',
  'system:codex-test:execute'
]) {
  assert.ok(page.includes(permission), `missing page permission ${permission}`)
}

for (const permission of [
  'system:codex-test:cancel',
  'system:codex-test:artifact'
]) {
  assert.ok(recordPage.includes(permission), `missing test record page permission ${permission}`)
}

assert.match(page, /测试租户/)
assert.match(page, /项目/)
assert.match(page, /测试方法项/)
assert.match(page, /测试目标项/)
assert.match(
  api,
  /export type CodexTestProject = '智能排产' \| '文控' \| '批记录'/,
  '测试项 API 类型必须显式约束项目枚举。'
)
assert.match(
  api,
  /interface CodexTestCaseVO[\s\S]*project\??:\s*CodexTestProject[\s\S]*methodText:/,
  '测试项 VO 必须暴露项目字段。'
)
assert.match(
  api,
  /interface CodexTestCasePageReqVO[\s\S]*project\??:\s*CodexTestProject[\s\S]*status\??:/,
  '测试项分页查询必须支持项目过滤字段。'
)
assert.match(
  page,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  '测试管理页必须导入标准列表模板'
)
assert.match(
  page,
  /import \{\s*useUserTableColumns,\s*type UserTableColumnDefinition,\s*type UserTableColumnState\s*\} from '@\/hooks\/web\/useUserTableColumns'/,
  '测试管理页必须接入显示字段和列宽持久化 hook'
)
assert.match(
  page,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '测试管理页必须接入标准快速过滤 hook'
)
assert.match(page, /<UnifiedListTemplate[\s\S]*table-key="system\.codexTestManagement\.cases"/)
assert.match(page, /:filter-definitions="caseQuickFilterDefinitions"/)
assert.match(page, /:columns="caseColumns"/)
assert.match(page, /:column-saving="caseColumnSaving"/)
assert.match(page, /@quick-filter-query="caseQuickFilter\.applyQuickFilter"/)
assert.match(page, /@column-change="saveCaseColumnConfig"/)
assert.match(page, /@pagination="handleCasePagination"/)
assert.match(page, /caseProjectOptions/, '测试管理页必须定义三个项目选项。')
assert.match(
  api,
  /CODEX_TEST_PROJECT_OPTIONS[\s\S]*智能排产[\s\S]*批记录[\s\S]*文控/,
  '测试项项目选项顺序必须为智能排产、批记录、文控。'
)
assert.match(page, /label:\s*'项目'[\s\S]*queryParamKey:\s*'project'/, '测试管理页必须支持按项目快速过滤。')
assert.match(page, /\{\s*key:\s*'project',\s*label:\s*'项目'/, '测试管理列表字段配置必须包含项目列。')
assert.match(page, /<el-table-column[\s\S]*label="项目"[\s\S]*prop="project"/, '测试管理表格必须渲染项目列。')
assert.match(page, /resolveCaseProject\(row\)/, '项目列不得直接渲染空 project，必须解析为三类项目。')
assert.doesNotMatch(page, /\{\{\s*row\.project\s*\}\}/, '项目列不得把空 project 渲染为空标签。')
assert.match(
  page,
  /function resolveCaseProject[\s\S]*'批记录'[\s\S]*'文控'[\s\S]*'智能排产'/,
  '项目列解析必须只输出智能排产、批记录、文控三类。'
)
assert.match(page, /<el-form-item label="项目" prop="project">[\s\S]*请选择项目/, '测试项表单必须允许维护项目。')
assert.match(page, /<template #extra-filters>/)
assert.match(page, /<template #actions>[\s\S]*新增测试项[\s\S]*顺序执行[\s\S]*并行执行/)
assert.match(
  page,
  /<template #table="\{ sortColumnAttrs, handleSortChange: handleTemplateSortChange \}">[\s\S]*data-user-table-column-explicit[\s\S]*data-user-table-key="system\.codexTestManagement\.cases"[\s\S]*:data="caseList"[\s\S]*row-key="id"[\s\S]*@header-dragend="handleCaseHeaderDragend"/,
  '测试管理表格必须通过标准模板 table 插槽接入列宽持久化和稳定行标识'
)
assert.match(page, /formatMethodItems/)
assert.match(page, /formatTargetItems/)
assert.match(page, /class="codex-test-item-list"/)
assert.match(
  page,
  /<el-form-item label="测试方法项" prop="methodText">[\s\S]*class="codex-test-methods"/,
  '测试方法项表单必须从单个 textarea 改为逐项录入容器。'
)
assert.match(
  page,
  /v-for="\((methodItem, index)\) in methodItems"[\s\S]*class="codex-test-method"/,
  '测试方法项必须按 methodItems 渲染多行方法项。'
)
assert.match(
  page,
  /<el-input-number[\s\S]*class="codex-test-method__sort"[\s\S]*controls-position="right"/,
  '测试方法项序号必须使用专用数字控件。'
)
assert.match(
  page,
  /<el-input[\s\S]*class="codex-test-method__text"[\s\S]*placeholder="测试方法，例如：打开排产工单页"/,
  '测试方法项内容必须使用专用输入框逐项维护。'
)
assert.match(page, /@click="addMethodItem"/, '测试方法项必须支持新增方法项。')
assert.match(page, /@click="removeMethodItem\(index\)"/, '测试方法项必须支持删除方法项。')
assert.match(page, /function parseMethodItems\(methodText\?: string\)/, '编辑测试项时必须把 methodText 拆成方法项。')
assert.match(page, /function serializeMethodItems\(\)/, '保存测试项时必须把方法项序列化回 methodText。')
assert.match(page, /caseForm\.methodText = serializeMethodItems\(\)/, '保存前必须同步结构化方法项到 methodText。')
assert.doesNotMatch(
  page,
  /v-model="caseForm\.methodText"[\s\S]*type="textarea"/,
  '测试方法项不应继续使用单个多行文本框直接录入。'
)
assert.doesNotMatch(page, /caseTableRows/)
assert.doesNotMatch(page, /caseRowSpanMethod/)
assert.doesNotMatch(page, /displayMethodItem/)
assert.doesNotMatch(page, /displayTargetItem/)
assert.doesNotMatch(page, /:span-method="caseRowSpanMethod"/)
assert.doesNotMatch(page, /<Pagination[\s\S]*@pagination="getCaseList"/)
assert.match(page, /startSingleCaseExecution/)
assert.match(page, /@click="startSingleCaseExecution\(row\)"/)
assert.match(page, /@click="openEdit\(row\)"/, '修改测试项必须把当前行传入编辑回显流程。')
assert.match(
  page,
  /async function openEdit\(row: CodexTestApi\.CodexTestCaseVO\)/,
  '编辑方法必须接收当前行，确保可按当前测试项内容回显。'
)
assert.match(
  page,
  /function applyCaseFormForEdit\(data: CodexTestApi\.CodexTestCaseVO\)/,
  '编辑回显必须集中归一化测试项表单数据。'
)
assert.match(
  page,
  /methodItems\.value = parseMethodItems\(data\.methodText\)/,
  '编辑回显必须将当前测试项 methodText 拆成逐条方法项。'
)
assert.match(
  page,
  /caseForm\.checkpoints = normalizeCheckpointItems\(data\.checkpoints\)/,
  '编辑回显必须将当前测试项 checkpoints 归一化为逐条目标项。'
)
assert.match(
  page,
  /function normalizeCheckpointItems\(checkpoints\?: CodexTestApi\.CodexTestCheckpointVO\[\]\)/,
  '测试目标项编辑回显必须有专用归一化函数。'
)
assert.match(
  page,
  /splitDisplayItems\(checkpoint\.expectedText,\s*checkpoint\.name\)[\s\S]*expectedText/,
  '目标项归一化必须把多行 expectedText 拆成逐条目标项。'
)
assert.match(page, /caseIds:\s*\[caseId\]/)
assert.match(page, /executionMode:\s*row\.defaultExecutionMode/)
assert.doesNotMatch(page, /<span>执行记录<\/span>/, '测试管理页不得继续内嵌执行记录列表')
assert.doesNotMatch(page, /getCodexTestExecutionPage/, '测试管理页不应继续加载执行记录分页')
assert.doesNotMatch(page, /getExecutionList/, '测试管理页不应继续刷新执行记录列表')
assert.match(page, /检查点/)
assert.match(page, /并行执行/)
assert.match(
  page,
  /<el-input-number[\s\S]*class="codex-test-checkpoint__sort"[\s\S]*controls-position="right"/,
  '测试目标项序号控件必须使用专用类并收敛到网格列内，避免挤压目标项名称。'
)
assert.match(
  page,
  /<el-input[\s\S]*class="codex-test-checkpoint__name"[\s\S]*placeholder="目标项名称"/,
  '测试目标项名称输入框必须使用专用类锁定完整显示布局。'
)
assert.match(
  page,
  /\.codex-test-checkpoint\s*{[\s\S]*grid-template-columns:\s*112px\s+minmax\(220px,\s*0\.8fr\)\s+minmax\(260px,\s*1fr\)\s+48px;/,
  '测试目标项行必须给序号、名称、目标内容和删除按钮分配不会互相挤压的列宽。'
)
assert.match(
  page,
  /\.codex-test-checkpoint__sort\s*{[\s\S]*width:\s*100%;[\s\S]*}/,
  '测试目标项序号控件必须收敛为当前网格列宽。'
)
assert.match(
  page,
  /\.codex-test-checkpoint__name,\s*\n\.codex-test-checkpoint__target\s*{[\s\S]*min-width:\s*0;[\s\S]*width:\s*100%;[\s\S]*}/,
  '测试目标项名称和目标内容输入框必须允许在网格内正确收缩并完整占满分配列。'
)
assert.ok(!page.includes('catch {}'), 'request failures must remain visible')

assert.match(
  recordPage,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
  '测试记录页必须使用标准列表模板。'
)
assert.match(
  recordPage,
  /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
  '测试记录页必须接入列显示配置 hook。'
)
assert.match(
  recordPage,
  /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
  '测试记录页必须接入标准快速过滤 hook。'
)

const recordTemplateMatch = recordPage.match(
  /<UnifiedListTemplate[\s\S]*?table-key="system\.codex-test\.records"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(recordTemplateMatch, '测试记录页必须使用稳定 table-key 接入标准列表模板。')
const recordTemplate = recordTemplateMatch[0]
for (const [pattern, description] of [
  [/:query-model="recordQueryParams"/, 'query model binding'],
  [/:filter-definitions="recordQuickFilterDefinitions"/, 'quick filter definitions binding'],
  [/:quick-filter-state="recordQuickFilter\.state"/, 'quick filter state binding'],
  [/:selected-filter-definition="recordQuickFilter\.selectedDefinition\.value"/, 'selected quick filter binding'],
  [/:operator-options="recordQuickFilter\.operatorOptions\.value"/, 'quick filter operators binding'],
  [/:columns="recordColumns"/, 'user column settings binding'],
  [/:column-saving="recordColumnSaving"/, 'column saving binding'],
  [/v-model:page="recordQueryParams\.pageNo"/, 'page number pagination binding'],
  [/v-model:limit="recordQueryParams\.pageSize"/, 'page size pagination binding'],
  [/@update:quick-filter-state="recordQuickFilter\.updateState"/, 'quick filter state update handler'],
  [/@quick-filter-query="recordQuickFilter\.applyQuickFilter"/, 'quick filter query handler'],
  [/@column-change="saveRecordColumnConfig"/, 'column config save handler'],
  [/@pagination="getRecordList"/, 'pagination reload handler']
]) {
  assert.match(recordTemplate, pattern, `测试记录页缺少 ${description}`)
}
assert.match(
  recordTemplate,
  /<template\s+#table="\{ sortColumnAttrs, handleSortChange: handleTemplateSortChange \}">[\s\S]*?<el-table[\s\S]*?data-user-table-column-explicit[\s\S]*?data-user-table-key="system\.codex-test\.records"[\s\S]*?:data="recordList"[\s\S]*?row-key="id"[\s\S]*?@header-dragend="handleRecordHeaderDragend"[\s\S]*?@sort-change="handleTemplateSortChange"/,
  '测试记录表格必须接入列宽拖拽持久化、行标识和标准排序。'
)
assert.match(recordPage, /getCodexTestExecutionPage/, '测试记录页必须加载执行记录分页。')
assert.match(recordPage, /cancelCodexTestExecution/, '测试记录页必须保留取消执行能力。')
assert.match(recordPage, /downloadCodexTestArtifact/, '测试记录页必须保留失败证据预览能力。')
assert.match(recordPage, /通过/)
assert.match(recordPage, /失败/)
assert.match(recordPage, /失败截图/)
assert.doesNotMatch(
  recordPage,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '测试记录标准列表不得引入 mock、placeholder、fallback、降级或吞异常'
)

for (const [pattern, description] of [
  [/release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=menu; riskLevel=medium/, 'release migration metadata'],
  [/'测试记录',\s*'system:codex-test:query',\s*2,\s*101,\s*1,\s*'codex-test-record'/, '测试记录主菜单 sort 101'],
  [/'system\/codex-test-record\/index',\s*'SystemCodexTestRecord'/, '测试记录组件契约'],
  [/UPDATE `system_menu`[\s\S]*`sort` = 102[\s\S]*`permission` = 'system:backup-plan:query'/, '备份计划排序后移到 102'],
  [/JSON_CONTAINS\(`package`\.`menu_ids`, CAST\('1' AS JSON\), '\$'\)/, '租户套餐菜单合并范围'],
  [/`role`\.`code` = 'codex_test_admin'[\s\S]*`record_menu`\.`menu_id`/, '测试管理员角色绑定测试记录菜单']
]) {
  assert.match(menuSql, pattern, `测试记录菜单 SQL 缺少 ${description}`)
}

assert.match(runner, /codex(?:\.cmd)?\s+exec/)
assert.match(runner, /playwright/)
assert.match(runner, /CODEX_TEST_TENANT_ID/)
assert.match(runner, /tenant-id/)
assert.match(runner, /CODEX_TEST_POLL_INTERVAL_MS/)
assert.match(runner, /CODEX_TEST_HEARTBEAT_INTERVAL_MS/)
assert.match(runner, /CODEX_TEST_CODEX_TIMEOUT_MS/)
assert.match(runner, /class ServerCanceledExecutionError extends Error/)
assert.match(runner, /await sleep\(POLL_INTERVAL_MS\)/)
assert.match(runner, /function spawnCodex/)
assert.match(runner, /isWindowsCommandScript/)
assert.match(runner, /cmd\.exe/)
assert.match(runner, /'\/d', '\/s', '\/c'/)
assert.match(runner, /spawnSync\('taskkill\.exe'/)
assert.match(runner, /function stopWindowsProcessTree/)
assert.match(runner, /CommandLine\.Contains\(\$needle\)/)
assert.match(runner, /await heartbeat\(runnerSessionId, runningExecutionCaseIds\)/)
assert.match(runner, /assertTaskNotCanceled\(task, await heartbeat\(runnerSessionId, runningExecutionCaseIds\)\)/)
assert.match(runner, /cancelExecutionCaseIds\.includes\(task\.executionCaseId\)/)
assert.match(runner, /finally\s*{[\s\S]*clearInterval\(heartbeatTimer\)[\s\S]*clearTimeout\(timeoutTimer\)[\s\S]*}/)
assert.match(runner, /function reportTaskBlocked/)
assert.match(runner, /status:\s*'BLOCKED'/)
assert.match(runner, /error instanceof ServerCanceledExecutionError/)
assert.match(runner, /reportTaskBlocked\(task, error\)/)
assert.match(runner, /checkpoint-result/)
assert.match(runner, /complete-case/)
