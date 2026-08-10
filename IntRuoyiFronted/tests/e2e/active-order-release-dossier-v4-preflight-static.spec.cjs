const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const frontendRoot = path.resolve(__dirname, '../..')
const preflightPath = path.join(
  frontendRoot,
  'tests/e2e/active-order-release-dossier-v4-preflight.cjs'
)

assert.ok(fs.existsSync(preflightPath), 'missing executable A6 V4 preflight gate')

const source = fs.readFileSync(preflightPath, 'utf8')
const requiredEnvKeys = [
  'AORD_V4_M0_AUTHORIZATION_TOKEN',
  'AORD_V4_M0_TENANT_ID',
  'AORD_V4_M0_TENANT_NAME',
  'AORD_V4_M0_FRONTEND_URL',
  'AORD_V4_M0_BACKEND_URL',
  'AORD_V4_M0_BROWSER_PATH',
  'AORD_V4_M0_DB_CONTAINER',
  'AORD_V4_M0_DB_SCHEMA',
  'AORD_V4_M0_ACCOUNT_MODE',
  'AORD_V4_M0_PRODUCT_ID',
  'AORD_V4_M0_ROUTE_ID',
  'AORD_V4_M0_ROUTE_VERSION_ID',
  'AORD_V4_M0_ROUTE_PROCESS_IDS',
  'AORD_V4_M0_PRODUCTION_WORKER_USERNAME',
  'AORD_V4_M0_PRODUCTION_WORKER_PASSWORD',
  'AORD_V4_M0_PRODUCTION_WORKER_SIGNATURE_PASSWORD',
  'AORD_V4_M0_PRODUCTION_LEADER_USERNAME',
  'AORD_V4_M0_PRODUCTION_LEADER_PASSWORD',
  'AORD_V4_M0_PRODUCTION_LEADER_SIGNATURE_PASSWORD',
  'AORD_V4_M0_PQC_INSPECTOR_USERNAME',
  'AORD_V4_M0_PQC_INSPECTOR_PASSWORD',
  'AORD_V4_M0_PQC_INSPECTOR_SIGNATURE_PASSWORD',
  'AORD_V4_M0_PQC_LEADER_USERNAME',
  'AORD_V4_M0_PQC_LEADER_PASSWORD',
  'AORD_V4_M0_PQC_LEADER_SIGNATURE_PASSWORD',
  'AORD_V4_M0_RELEASE_OWNER_USERNAME',
  'AORD_V4_M0_RELEASE_OWNER_PASSWORD',
  'AORD_V4_M0_RELEASE_OWNER_SIGNATURE_PASSWORD'
]

for (const key of requiredEnvKeys) {
  assert.match(source, new RegExp(`['"]${key}['"]`), `preflight must require ${key}`)
}

assert.match(source, /const REQUIRED_ENV_KEYS = Object\.freeze\(/)
assert.match(source, /const ACCOUNT_MODE_DISTINCT_ROLES = 'DISTINCT_ROLES'/)
assert.match(source, /const ACCOUNT_MODE_SINGLE_ADMIN_APPROVED = 'SINGLE_ADMIN_APPROVED'/)
assert.match(source, /SINGLE_ADMIN_ACCOUNT_MISMATCH/)
assert.match(source, /DISTINCT_ROLE_ACCOUNTS_REQUIRED/)
assert.match(source, /validateExplicitEnvironment\(process\.env\)/)
assert.match(source, /assertNoSecretLeak\(result, secretValues\)/)
assert.match(source, /sanitizeForOutput\(error, secretValues\)/)
assert.doesNotMatch(source, /VITE_APP_DEFAULT_LOGIN|USER_APPROVED_YUDAO_SOURCE_20260802|readFileSync\([^)]*\.env/)
assert.match(source, /CAST\(COALESCE\(a\.electronic_signature_enabled, 0\) AS UNSIGNED\)/)
assert.doesNotMatch(source, /password\.length\s*<|signaturePassword\.length\s*</)
assert.match(source, /!password\s*\|\|\s*!signaturePassword/)
assert.match(source, /frontendUrl\.port !== '8081'/)
assert.match(source, /backendUrl\.port !== '48081'/)
assert.match(source, /\[A-Za-z0-9\]\[A-Za-z0-9\._:-\]\*/)

const mainSource = source.slice(source.indexOf('async function main()'))
const envIndex = mainSource.indexOf('validateExplicitEnvironment(process.env)')
const runtimeIndex = mainSource.indexOf('runReadOnlyRuntimeChecks(environment)')
const databaseIndex = mainSource.indexOf('runReadOnlyDatabaseChecks(environment)')
const browserIndex = mainSource.indexOf('runReadOnlyBrowserLogins(environment, databaseEvidence)')
assert.ok(
  envIndex >= 0 && runtimeIndex > envIndex && databaseIndex > runtimeIndex && browserIndex > databaseIndex
)

for (const token of [
  'mes_pro_route_flow_process_batch_record',
  'batch_record_report_id',
  "'MAIN'",
  "'PROCESS_INSPECTION'",
  "'LOSS_REPORT'",
  'form_binding_key',
  'form_template_id',
  'last_published_template_version_id',
  'last_published_template_version_no',
  'bpm_form_template_version',
  "'FORM_TEMPLATE_VERSION'",
  'FORMTPL:',
  'ROW_NUMBER() OVER',
  'mes_pro_batch_record_report',
  'mes_pro_batch_record_definition',
  'mes_pro_batch_record_version',
  "'APPROVED'",
  'mes_qa_inspection_regulation',
  'mes_qa_inspection_regulation_version',
  'mes_qa_inspection_regulation_item',
  'mes_qa_inspection_regulation_item_equipment',
  "'PUBLISHED'",
  'mes_pro_batch_record_cell_link_rule',
  "'PROCESS_POOL_REPORT'",
  "'PQC_AGGREGATE_DETAIL'",
  "'PRODUCTION_LOSS'",
  'mes_pro_edhr_work_task_assignment_rule',
  "'ROUTE'",
  "'RELEASE_APPROVE'"
]) {
  assert.ok(source.includes(token), `preflight source gate is missing ${token}`)
}

assert.match(source, /PROCESS_INSPECTION:\s*28/)
assert.match(source, /LOSS_REPORT:\s*25/)
assert.match(source, /DYNAMIC_FORM_BINDINGS_REQUIRED/)
assert.match(source, /DYNAMIC_FORM_TEMPLATE_SNAPSHOT_INVALID/)
assert.match(source, /LATEST_PUBLISHED_QA_BY_STABLE_PROCESS_REQUIRED/)
assert.match(source, /assertReadOnlySql\(sql\)/)
assert.match(source, /\b(?:INSERT|UPDATE|DELETE|REPLACE|MERGE|CALL)\b/)
assert.match(source, /page\.on\('console'/)
assert.match(source, /page\.on\('pageerror'/)
assert.match(source, /page\.on\('requestfailed'/)
assert.match(source, /page\.on\('response'/)
const browserLoginSource = source.slice(
  source.indexOf('async function runReadOnlyBrowserLogins'),
  source.indexOf('function writeResult')
)
const observerIndex = browserLoginSource.indexOf('installReadOnlyObservers(page, actor.role)')
const writeGuardIndex = browserLoginSource.indexOf('installBusinessWriteGuard(context, actorEvidence)')
const loginIndex = browserLoginSource.indexOf('loginReadOnly(page, environment, actor, account.userId)')
assert.ok(observerIndex >= 0, 'read-only observers must be installed for each role')
assert.ok(writeGuardIndex > observerIndex, 'business write guard must be installed after observers')
assert.ok(loginIndex > writeGuardIndex, 'observers and write guard must be active before UI login navigation')
const loginSource = source.slice(
  source.indexOf('async function loginReadOnly'),
  source.indexOf('async function runReadOnlyBrowserLogins')
)
assert.match(loginSource, /await page\.goto\(/)
assert.match(source, /ROLE_LOGIN_PREREQUISITE_FAILED/)
assert.doesNotMatch(
  source,
  /frontline\/submit|pqc\/submit|submission\/review|active-order\/release\/apply|route\/flow-config\/batch-record\/save/
)

for (const field of ['browserBusinessWrites', 'businessApiWrites', 'sqlWrites']) {
  assert.match(source, new RegExp(`${field}: 0`))
}

const resultPath = path.join(os.tmpdir(), `aord-v4-m0-a6-preflight-contract-${process.pid}.json`)
try {
  const run = spawnSync(process.execPath, [preflightPath, '--result-path', resultPath], {
    cwd: frontendRoot,
    encoding: 'utf8',
    env: {
      PATH: process.env.PATH || '',
      SystemRoot: process.env.SystemRoot || '',
      ComSpec: process.env.ComSpec || ''
    }
  })
  assert.strictEqual(run.status, 2, `missing-env preflight must exit 2: ${run.stderr}`)
  assert.ok(fs.existsSync(resultPath), 'missing-env preflight must write its requested result file')
  const result = JSON.parse(fs.readFileSync(resultPath, 'utf8'))
  assert.strictEqual(result.status, 'BLOCKED')
  assert.strictEqual(result.canRunRealE2E, false)
  assert.deepStrictEqual(result.sideEffects, {
    browserBusinessWrites: 0,
    businessApiWrites: 0,
    sqlWrites: 0,
    manifestCreated: false
  })
  assert.deepStrictEqual(
    result.blockers[0].missingEnvKeys,
    [...requiredEnvKeys].sort(),
    'the missing-env blocker must expose names only and keep stable sorting'
  )
  const serialized = JSON.stringify(result)
  assert.doesNotMatch(serialized, /password123|signature123|USER_APPROVED_TEST_VALUE/)
} finally {
  if (fs.existsSync(resultPath)) fs.unlinkSync(resultPath)
}

console.log('PASS: active-order release dossier V4 executable preflight static contract')
