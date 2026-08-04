const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const sqlPath = path.join(backendRoot, 'sql/mysql/20260804_mes_edhr_qa_menu.sql')
const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const qaPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const pqcLeaderPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'
)
const workbenchPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)

for (const requiredPath of [sqlPath, routePath, qaPagePath, pqcLeaderPagePath, workbenchPath]) {
  assert.ok(fs.existsSync(requiredPath), `required file must exist: ${requiredPath}`)
}

const sql = fs.readFileSync(sqlPath, 'utf8')
const route = fs.readFileSync(routePath, 'utf8')
const qaPage = fs.readFileSync(qaPagePath, 'utf8')
const pqcLeaderPage = fs.readFileSync(pqcLeaderPagePath, 'utf8')
const workbench = fs.readFileSync(workbenchPath, 'utf8')

const expectedOrder = [
  ['批记录表单', 0, '/mes/pro/batch-record-form-list'],
  ['QA', 1, '/mes/pro/process-pool/qa-regulation'],
  ['PQC组长', 2, '/mes/pro/process-pool/pqc-leader'],
  ['批次执行', 3, '/mes/pro/feedback/edhr-batch-execution'],
  ['表单追溯', 4, '/mes/pro/feedback/edhr-form-trace'],
  ['表单日志', 5, '/mes/pro/feedback/edhr-form-fill-log']
]

for (const [label, sort, menuPath] of expectedOrder) {
  assert.match(sql, new RegExp(`'${label}' AS \`name\``), `menu SQL must include ${label}`)
  assert.match(sql, new RegExp(`${sort} AS \`sort\``), `${label} must have sort ${sort}`)
  assert.match(sql, new RegExp(`'${menuPath}' AS \`path\``), `${label} must use path ${menuPath}`)
}

assert.match(
  sql,
  /900434[\s\S]*'QA'[\s\S]*'mes:pro-process-pool-team-leader:query'[\s\S]*'\/mes\/pro\/process-pool\/qa-regulation'[\s\S]*'mes\/pro\/processpool\/QaRegulationPage'[\s\S]*'MesProProcessPoolQaRegulation'/,
  'QA dynamic menu must point at the standalone QA regulation page.'
)
assert.match(
  sql,
  /900435[\s\S]*'PQC组长'[\s\S]*'mes:pro-process-pool-team-leader:query'[\s\S]*'\/mes\/pro\/process-pool\/pqc-leader'[\s\S]*'mes\/pro\/processpool\/PqcLeaderWorkbenchPage'[\s\S]*'MesProProcessPoolPqcLeaderWorkbench'/,
  'PQC leader dynamic menu must point at the standalone QA-side PQC leader page.'
)
assert.match(sql, /system_tenant_package/, 'QA menu must be added to tenant packages.')
assert.match(sql, /900435/, 'PQC leader menu must be added to tenant packages and role bindings.')
assert.match(sql, /system_role_menu/, 'QA/PQC leader menus must be added to role-menu bindings.')
assert.match(sql, /'tenant_admin'|'super_admin'/, 'QA/PQC leader menus must include admin-role visibility.')

assert.match(
  route,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/QaRegulationPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolQaRegulation'/,
  'Frontend route must load the same standalone QA regulation component.'
)
assert.match(
  route,
  /path:\s*'pro\/process-pool\/pqc-leader'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/PqcLeaderWorkbenchPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolPqcLeaderWorkbench'/,
  'Frontend route must load the standalone PQC leader component under QA.'
)
assert.match(
  route,
  /permission:\s*\['mes:pro-process-pool-team-leader:query'\]/,
  'Frontend route permission must match the dynamic QA menu permission.'
)
assert.match(qaPage, /data-qa-regulation-page/, 'Standalone QA page must remain routable.')
assert.match(pqcLeaderPage, /data-pqc-leader-workbench-page/, 'Standalone PQC leader page must remain routable.')
assert.match(
  pqcLeaderPage,
  /leader-type="PQC"[\s\S]*:show-leader-type-tabs="false"/,
  'Standalone PQC leader page must lock the shared workbench to PQC without internal tabs.'
)
assert.doesNotMatch(
  workbench,
  /<el-tab-pane\s+label="QA 规程"\s+name="QA"|data-qa-regulation-page/,
  'QA must not be reintroduced as an internal workbench tab.'
)
assert.doesNotMatch(
  pqcLeaderPage,
  /EdhrBatchRecordTabs|active-tab=/,
  'PQC leader must not be reintroduced as an eDHR internal tab.'
)
assert.doesNotMatch(
  qaPage,
  /DCC|文件分类|受控文件|文控|controlled-file|fileTypeTaxonomy/i,
  'QA page must remain independent from DCC/document-control semantics.'
)

console.log('PASS eDHR QA dynamic menu static contract')
