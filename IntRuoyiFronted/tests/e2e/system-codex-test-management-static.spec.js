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
assert.match(page, /测试方法项/)
assert.match(page, /测试目标项/)
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
assert.doesNotMatch(page, /caseTableRows/)
assert.doesNotMatch(page, /caseRowSpanMethod/)
assert.doesNotMatch(page, /displayMethodItem/)
assert.doesNotMatch(page, /displayTargetItem/)
assert.doesNotMatch(page, /:span-method="caseRowSpanMethod"/)
assert.doesNotMatch(page, /<Pagination[\s\S]*@pagination="getCaseList"/)
assert.match(page, /startSingleCaseExecution/)
assert.match(page, /@click="startSingleCaseExecution\(row\)"/)
assert.match(page, /caseIds:\s*\[caseId\]/)
assert.match(page, /executionMode:\s*row\.defaultExecutionMode/)
assert.doesNotMatch(page, /<span>执行记录<\/span>/, '测试管理页不得继续内嵌执行记录列表')
assert.doesNotMatch(page, /getCodexTestExecutionPage/, '测试管理页不应继续加载执行记录分页')
assert.doesNotMatch(page, /getExecutionList/, '测试管理页不应继续刷新执行记录列表')
assert.match(page, /检查点/)
assert.match(page, /并行执行/)
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
