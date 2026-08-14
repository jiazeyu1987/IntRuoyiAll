const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const read = (filePath) => fs.readFileSync(filePath, 'utf8')

const tabsPath = path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/EdhrBatchRecordTabs.vue')
const pqcPagePath = path.join(frontendRoot, 'src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue')
const routerPath = path.join(frontendRoot, 'src/router/modules/remaining.ts')
const menuSqlPath = path.join(
  backendRoot,
  'sql/mysql/20260805_mes_edhr_frontline_pqc_menu.sql'
)

for (const requiredPath of [tabsPath, pqcPagePath, routerPath, menuSqlPath]) {
  assert.ok(fs.existsSync(requiredPath), `required file must exist: ${requiredPath}`)
}

const tabs = read(tabsPath)
const pqcPage = read(pqcPagePath)
const router = read(routerPath)
const menuSql = read(menuSqlPath)

assert.match(tabs, /<el-tab-pane label="批次执行" name="execution"/)
assert.match(tabs, /<el-tab-pane label="批记录页面关系图" name="pageGraph"/)
assert.doesNotMatch(
  tabs,
  /<el-tab-pane\s+label="生产填写"\s+name="production"/,
  '生产填写 must not remain inside the batch execution tab strip after 一线生产 becomes standalone.'
)
assert.doesNotMatch(
  tabs,
  /<el-tab-pane\s+label="PQC填写"\s+name="pqc"/,
  'PQC填写 must not remain inside the batch execution tab strip.'
)
assert.doesNotMatch(tabs, /\|\s*'pqc'/, 'shared batch tab type must not keep pqc.')
assert.doesNotMatch(
  tabs,
  /pqc:\s*'\/mes\/pro\/feedback\/edhr-batch-pqc-fill'/,
  'shared batch tab routing must not navigate to PQC填写.'
)

assert.doesNotMatch(
  pqcPage,
  /EdhrBatchRecordTabs|active-tab="pqc"/,
  'standalone 一线PQC page must not render the batch execution internal tabs.'
)
assert.match(
  pqcPage,
  /<FrontlineFixedTemplatePanel\s+mode="pqc"/,
  'standalone 一线PQC page must keep the formal PQC panel.'
)
assert.match(pqcPage, /data-edhr-frontline-pqc-page-title/, 'standalone 一线PQC page must expose a visible page title.')
assert.match(pqcPage, /一线PQC/, 'standalone PQC page title must be 一线PQC.')

const routeAnchor = "path: 'pro/feedback/edhr-batch-pqc-fill'"
const routeIndex = router.indexOf(routeAnchor)
assert.ok(routeIndex >= 0, 'frontend router must keep the formal PQC fill route.')
const nextRouteIndex = router.indexOf('\n      {', routeIndex + routeAnchor.length)
const routeBlock = router.slice(routeIndex, nextRouteIndex > routeIndex ? nextRouteIndex : undefined)

assert.match(routeBlock, /BatchPqcFillPage\.vue/, 'PQC route must load BatchPqcFillPage.')
assert.match(routeBlock, /name:\s*'MesProEdhrBatchPqcFill'/, 'PQC route name must remain stable.')
assert.match(routeBlock, /title:\s*'一线PQC'/, 'PQC standalone route title must be 一线PQC.')
assert.match(
  routeBlock,
  /activeMenu:\s*'\/mes\/pro\/feedback\/edhr-batch-pqc-fill'/,
  'PQC route must activate its standalone dynamic menu.'
)
assert.match(
  routeBlock,
  /permission:\s*\['mes:pro-edhr-batch-execution:query'\]/,
  'PQC route must keep the existing formal eDHR batch query permission.'
)

assert.match(menuSql, /release-migration: allowedEnvironments=test,backup,prod/)
assert.match(menuSql, /dependsOn=20260804_mes_edhr_qa_menu/)
assert.match(menuSql, /SIGNAL SQLSTATE '45000'/, 'menu migration must fail fast on broken prerequisites.')
assert.match(
  menuSql,
  /900438[\s\S]*'一线PQC'[\s\S]*'mes:pro-edhr-batch-execution:query'[\s\S]*'\/mes\/pro\/feedback\/edhr-batch-pqc-fill'[\s\S]*'mes\/pro\/edhr-batch\/BatchPqcFillPage'[\s\S]*'MesProEdhrBatchPqcFill'/,
  'menu migration must create the standalone 一线PQC dynamic menu row.'
)
assert.match(menuSql, /system_tenant_package/, '一线PQC must be added to tenant packages.')
assert.match(menuSql, /system_role_menu/, '一线PQC must be bound to admin roles.')
assert.match(menuSql, /'tenant_admin'|'super_admin'/, 'admin roles must see 一线PQC.')
assert.match(menuSql, /CAST\('900438' AS JSON\)/, 'tenant package JSON must include 一线PQC menu id.')

console.log('PASS: eDHR frontline PQC standalone tab static contract')
