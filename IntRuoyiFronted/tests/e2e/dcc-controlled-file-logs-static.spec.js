const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readFrontendSource = (relativePath) => {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required frontend file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const readWorkspaceSource = (relativePath) => {
  const absolutePath = path.join(workspaceRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required workspace file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertMissingFrontendFile = (relativePath) => {
  assert.equal(
    fs.existsSync(path.join(frontendRoot, relativePath)),
    false,
    `old frontend page must be removed: ${relativePath}`
  )
}

const packageJson = JSON.parse(readFrontendSource('package.json'))
const logsPage = readFrontendSource('src/views/dcc/controlled-file/logs/index.vue')
const logsApi = readFrontendSource('src/api/dcc/controlledFile/logs.ts')
const menuSql = readWorkspaceSource(
  'IntRuoyiBackend/sql/mysql/20260714_dcc_controlled_file_logs_consolidation.sql'
)

assert.equal(
  packageJson.scripts['e2e:dcc:controlled-file-logs:static'],
  'node tests/e2e/dcc-controlled-file-logs-static.spec.js',
  'package.json must expose the DCC controlled-file logs static contract'
)

assertMissingFrontendFile('src/views/dcc/controlled-file/audit/index.vue')
assertMissingFrontendFile('src/views/dcc/controlled-file/project-code-assignment-audit/index.vue')
assertMissingFrontendFile('src/views/dcc/controlled-file/project-code-assignments/mine/index.vue')

assert.match(
  logsApi,
  /url:\s*'\/dcc\/controlled-file-logs\/page'/,
  '文控日志 API 必须调用统一后端分页接口'
)
assert.match(logsApi, /DccControlledFileLogPageReqVO/, '文控日志 API 必须声明请求类型')
assert.match(logsApi, /DccControlledFileLogRespVO/, '文控日志 API 必须声明响应类型')

const unifiedTemplateMatch = logsPage.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(unifiedTemplateMatch, '文控日志页面必须使用 UnifiedListTemplate')
const unifiedTemplate = unifiedTemplateMatch[0]

for (const templateToken of [
  'table-key="dcc.controlledFile.logs"',
  ':filter-definitions="logQuickFilterDefinitions"',
  ':columns="logColumns"',
  'v-model:page="queryParams.pageNo"',
  'v-model:limit="queryParams.pageSize"',
  '@pagination="getList"',
  '@header-dragend="handleLogHeaderDragend"',
  'data-user-table-key="dcc.controlledFile.logs"'
]) {
  assert.match(
    unifiedTemplate,
    new RegExp(templateToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `文控日志标准列表必须包含 ${templateToken}`
  )
}

for (const columnKey of [
  'occurredAt',
  'logType',
  'actionLabel',
  'fileNumber',
  'fileName',
  'operatorName',
  'relatedObject',
  'summary',
  'resultLabel',
  'actions'
]) {
  assert.match(
    logsPage,
    new RegExp(`isLogColumnVisible\\('${columnKey}'\\)`),
    `文控日志默认列必须受显示字段控制：${columnKey}`
  )
}

assert.doesNotMatch(
  logsPage,
  /<template\s+#extra-filters\b[\s\S]*?<\/template>/,
  '文控日志页面不得再渲染截图红框内的额外筛选项'
)

assert.doesNotMatch(
  logsPage,
  /<template\s+#actions\b[\s\S]*?<\/template>/,
  '文控日志页面不得再渲染截图红框内的重复查询和重置按钮'
)

for (const removedFilterPattern of [
  /<el-form-item\s+label="日志类型"/,
  /<el-form-item\s+label="关键字"/,
  /<el-form-item\s+label="动作"/,
  /<el-form-item\s+label="结果"/,
  /<el-form-item\s+label="发生时间"/
]) {
  assert.doesNotMatch(
    logsPage,
    removedFilterPattern,
    '文控日志红框筛选表单项必须移除，只保留快速过滤入口'
  )
}

for (const quickFilterToken of [
  "key: 'keyword'",
  "key: 'logType'",
  "key: 'actionType'",
  "key: 'result'"
]) {
  assert.ok(logsPage.includes(quickFilterToken), `文控日志快速过滤仍需保留 ${quickFilterToken}`)
}

for (const behaviorToken of [
  "defineOptions({ name: 'DccControlledFileLogs' })",
  'getControlledFileLogPage',
  'openLogDetail(row)',
  'data-testid="dcc-controlled-file-log-detail"',
  'logTypeOptions',
  'CONTROLLED_FILE_AUDIT',
  'FILE_SUBMISSION',
  'FILE_APPROVAL',
  'FILE_RELEASE',
  'FILE_DISTRIBUTION',
  'FILE_REVISION',
  'FILE_OBSOLETE',
  'PROJECT_CODE_ASSIGNMENT',
  'PROJECT_CODE_CHANGE',
  'TRAINING_EXECUTION'
]) {
  assert.ok(logsPage.includes(behaviorToken), `文控日志页面必须包含 ${behaviorToken}`)
}

const logTypeOptionBlock = logsPage.match(/const logTypeOptions = \[[\s\S]*?\]/)
assert.ok(logTypeOptionBlock, '文控日志页面必须声明日志类型选项')
const logTypeLabelMatches = [...logTypeOptionBlock[0].matchAll(/\{\s*label:\s*'([^']+)'\s*,\s*value:\s*'([^']+)'\s*\}/g)]
const logTypeLabelsByValue = new Map(logTypeLabelMatches.map((match) => [match[2], match[1]]))
const requiredShortLogTypes = new Map([
  ['CONTROLLED_FILE_AUDIT', '访问'],
  ['FILE_SUBMISSION', '提交'],
  ['FILE_APPROVAL', '审批'],
  ['FILE_RELEASE', '放行'],
  ['FILE_DISTRIBUTION', '分发'],
  ['FILE_REVISION', '升版'],
  ['FILE_OBSOLETE', '作废'],
  ['PROJECT_CODE_ASSIGNMENT', '修正任务'],
  ['PROJECT_CODE_CHANGE', '修正追溯'],
  ['TRAINING_EXECUTION', '培训']
])
for (const [value, label] of requiredShortLogTypes) {
  assert.equal(logTypeLabelsByValue.get(value), label, `文控日志类型 ${value} 应展示为 ${label}`)
  assert.ok([...label].length <= 4, `文控日志类型 ${label} 必须控制在 4 个字以内`)
}

for (const sqlToken of [
  '文控日志',
  'controlled-file/logs',
  'dcc/controlled-file/logs/index',
  'DccControlledFileLogs',
  'dcc:controlled-file:log:query',
  'dcc:controlled-file:audit:query',
  'dcc:project-code-assignment:audit:query',
  'dcc:project-code-assignment:execute',
  'controlled-file/project-code-assignment-audit',
  'controlled-file/project-code-assignments/mine',
  'system_role_menu'
]) {
  assert.ok(menuSql.includes(sqlToken), `菜单迁移 SQL 必须包含 ${sqlToken}`)
}

for (const retiredLabel of ['文件审计', '项目代码修正追溯', '我的DCC修正']) {
  const type2MenuPattern = new RegExp(`'${retiredLabel}'[\\s\\S]*?,\\s*2\\s*,`)
  assert.doesNotMatch(menuSql, type2MenuPattern, `${retiredLabel} 不能继续作为 type=2 页面菜单`)
}

assert.doesNotMatch(
  [logsPage, logsApi].join('\n'),
  /mock|placeholder data|fallback|降级|吞异常/i,
  '文控日志前端不得引入 mock、placeholder data、fallback、降级或吞异常'
)

console.log('PASS: DCC controlled-file logs consolidation static contract')
