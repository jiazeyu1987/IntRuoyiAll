const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const teamLeaderApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const formLogPage = readUtf8('src/views/mes/pro/edhr/FormFillLogPage.vue')
const formLogApi = readUtf8('src/api/mes/pro/edhr/formFillLog.ts')
const route = readUtf8('src/router/modules/remaining.ts')

const pageWithoutComments = page.replace(/<!--[\s\S]*?-->/g, '')

assert.match(
  pageWithoutComments,
  /data-team-leader-production-personnel-tab/,
  '生产人员档案页必须保留人员管理模块入口。'
)
assert.match(
  pageWithoutComments,
  /data-team-leader-production-personnel-list/,
  '生产人员档案页必须保留人员列表。'
)

assert.doesNotMatch(
  pageWithoutComments,
  /<el-divider>\s*操作追溯\s*<\/el-divider>/,
  '生产人员档案页不得再渲染独立“操作追溯”标题。'
)
assert.doesNotMatch(
  pageWithoutComments,
  /data-team-leader-personnel-audit-list/,
  '生产人员档案页不得再渲染独立人员操作追溯表。'
)
assert.doesNotMatch(
  pageWithoutComments,
  /employeeAudit(?:Rows|Loading)/,
  '生产人员档案页不得保留只服务独立追溯表的本地状态。'
)
assert.doesNotMatch(
  pageWithoutComments,
  /loadEmployeeAuditRecords/,
  '生产人员档案页不得再额外加载独立人员操作追溯数据。'
)
assert.doesNotMatch(
  pageWithoutComments,
  /getTeamEmployeeAuditList|TeamEmployeeAuditRespVO/,
  '生产人员档案页不得再引用人员追溯专用 API 或类型。'
)
assert.doesNotMatch(
  pageWithoutComments,
  /生产人员操作追溯加载失败/,
  '移除独立追溯表后不得留下已失效的追溯加载错误文案。'
)

assert.match(route, /title:\s*'表单日志'/, '追溯查看应由已有表单日志路由承载。')
assert.match(
  route,
  /permission:\s*\[\s*'mes:pro-edhr-form-fill-log:query'\s*\]/,
  '表单日志必须保留正式查询权限。'
)
assert.match(
  formLogPage,
  /<UnifiedListTemplate[\s\S]*table-key="mes\.pro\.edhr\.formFillLog\.main"/,
  '表单日志必须保留标准列表能力。'
)
assert.match(
  formLogApi,
  /\/mes\/pro\/batch-record-execution\/form-fill-log\/page/,
  '表单日志必须继续调用正式分页接口。'
)
assert.match(
  teamLeaderApi,
  /getTeamEmployeeAuditList\s*=\s*async/,
  '后端人员追溯 API wrapper 可以保留给表单日志或其他正式追溯能力复用。'
)

console.log('PASS: production personnel audit is delegated to form log')
