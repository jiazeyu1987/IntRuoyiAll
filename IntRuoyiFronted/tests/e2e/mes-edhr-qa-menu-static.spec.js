const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const sqlPath = path.join(backendRoot, 'sql/mysql/20260804_mes_edhr_qa_menu.sql')
const routePath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const qaPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const productionLeaderPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/ProductionLeaderWorkbenchPage.vue'
)
const pqcLeaderPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue'
)
const workbenchPath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)

for (const requiredPath of [
  sqlPath,
  routePath,
  qaPagePath,
  productionLeaderPagePath,
  pqcLeaderPagePath,
  workbenchPath
]) {
  assert.ok(fs.existsSync(requiredPath), `required file must exist: ${requiredPath}`)
}

const sql = fs.readFileSync(sqlPath, 'utf8')
const route = fs.readFileSync(routePath, 'utf8')
const qaPage = fs.readFileSync(qaPagePath, 'utf8')
const productionLeaderPage = fs.readFileSync(productionLeaderPagePath, 'utf8')
const pqcLeaderPage = fs.readFileSync(pqcLeaderPagePath, 'utf8')
const workbench = fs.readFileSync(workbenchPath, 'utf8')

const expectedOrder = [
  ['批记录表单', 0, '/mes/pro/batch-record-form-list'],
  ['QA', 1, '/mes/pro/process-pool/qa-regulation'],
  ['生产组长', 2, '/mes/pro/process-pool/production-leader'],
  ['一线生产', 3, '/mes/pro/feedback/edhr-batch-production-fill'],
  ['PQC组长', 4, '/mes/pro/process-pool/pqc-leader'],
  ['批次执行', 5, '/mes/pro/feedback/edhr-batch-execution'],
  ['表单追溯', 6, '/mes/pro/feedback/edhr-form-trace'],
  ['表单日志', 7, '/mes/pro/feedback/edhr-form-fill-log']
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
  /900436[\s\S]*'生产组长'[\s\S]*'mes:pro-process-pool-team-leader:query'[\s\S]*'\/mes\/pro\/process-pool\/production-leader'[\s\S]*'mes\/pro\/processpool\/ProductionLeaderWorkbenchPage'[\s\S]*'MesProProcessPoolProductionLeaderWorkbench'/,
  'Production leader dynamic menu must point at the standalone production leader page.'
)
assert.match(
  sql,
  /900437[\s\S]*'一线生产'[\s\S]*'mes:pro-edhr-batch-execution:query'[\s\S]*'\/mes\/pro\/feedback\/edhr-batch-production-fill'[\s\S]*'mes\/pro\/edhr-batch\/BatchProductionFillPage'[\s\S]*'MesProEdhrBatchProductionFill'/,
  'Standalone frontline production dynamic menu must point at the production fill page.'
)
assert.match(
  sql,
  /900435[\s\S]*'PQC组长'[\s\S]*'mes:pro-process-pool-team-leader:query'[\s\S]*'\/mes\/pro\/process-pool\/pqc-leader'[\s\S]*'mes\/pro\/processpool\/PqcLeaderWorkbenchPage'[\s\S]*'MesProProcessPoolPqcLeaderWorkbench'/,
  'PQC leader dynamic menu must point at the standalone QA-side PQC leader page.'
)
assert.match(sql, /system_tenant_package/, 'QA menu must be added to tenant packages.')
assert.match(sql, /900435[\s\S]*900436[\s\S]*900437|900437[\s\S]*900436[\s\S]*900435|900436[\s\S]*900437[\s\S]*900435/, 'Leader and frontline production menus must be added to tenant packages and role bindings.')
assert.match(sql, /system_role_menu/, 'QA/leader menus must be added to role-menu bindings.')
assert.match(sql, /'tenant_admin'|'super_admin'/, 'QA/leader menus must include admin-role visibility.')

assert.match(
  route,
  /path:\s*'pro\/process-pool\/qa-regulation'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/QaRegulationPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolQaRegulation'/,
  'Frontend route must load the same standalone QA regulation component.'
)
assert.match(
  route,
  /path:\s*'pro\/process-pool\/production-leader'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/processpool\/ProductionLeaderWorkbenchPage\.vue'\)[\s\S]*name:\s*'MesProProcessPoolProductionLeaderWorkbench'/,
  'Frontend route must load the standalone production leader component.'
)
assert.match(
  route,
  /path:\s*'pro\/feedback\/edhr-batch-production-fill'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/mes\/pro\/edhr-batch\/BatchProductionFillPage\.vue'\)[\s\S]*name:\s*'MesProEdhrBatchProductionFill'[\s\S]*title:\s*'一线生产'/,
  'Frontend route must expose the standalone frontline production title.'
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
assert.match(productionLeaderPage, /data-production-leader-workbench-page/, 'Standalone production leader page must remain routable.')
assert.match(pqcLeaderPage, /data-pqc-leader-workbench-page/, 'Standalone PQC leader page must remain routable.')
assert.match(
  productionLeaderPage,
  /leader-type="PRODUCTION"[\s\S]*:show-leader-type-tabs="false"/,
  'Standalone production leader page must lock the shared workbench to PRODUCTION without internal tabs.'
)
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
  `${productionLeaderPage}\n${pqcLeaderPage}`,
  /EdhrBatchRecordTabs|active-tab=/,
  'Standalone leader pages must not be reintroduced as eDHR internal tabs.'
)
assert.match(
  qaPage,
  /data-qa-regulation-dcc-project|DCC 项目代码/,
  'QA regulation page must keep the DCC project-code product scope selector.'
)
assert.doesNotMatch(
  qaPage,
  /文件分类|受控文件|controlled-file|fileTypeTaxonomy/i,
  'QA page must not regress into DCC document taxonomy or controlled-file page semantics.'
)

console.log('PASS eDHR QA dynamic menu static contract')
