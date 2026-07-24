const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const runtimeControlApiDir = 'src/api/infra/runtimeControl'
const runtimeControlViewDir = 'src/views/infra/runtime-control'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function exists(relativePath) {
  return fs.existsSync(path.join(repoRoot, relativePath))
}

function collectFiles(relativeDir) {
  const absoluteDir = path.join(repoRoot, relativeDir)
  if (!fs.existsSync(absoluteDir)) {
    return []
  }

  const files = []
  const stack = [absoluteDir]
  while (stack.length) {
    const current = stack.pop()
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const absolutePath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(absolutePath)
        continue
      }
      files.push(path.relative(repoRoot, absolutePath).replace(/\\/g, '/'))
    }
  }
  return files.sort()
}

function fail(message) {
  failures.push(message)
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    fail(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    fail(`forbidden ${label}: ${forbidden}`)
  }
}

function assertFileExists(relativePath, label) {
  if (!exists(relativePath)) {
    fail(`missing ${label}: ${relativePath}`)
  }
}

function parseEnv(source) {
  const values = new Map()
  for (const rawLine of source.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) {
      continue
    }
    const separatorIndex = line.indexOf('=')
    if (separatorIndex < 0) {
      continue
    }
    const key = line.slice(0, separatorIndex).trim()
    let value = line.slice(separatorIndex + 1).trim()
    if (
      (value.startsWith("'") && value.endsWith("'")) ||
      (value.startsWith('"') && value.endsWith('"'))
    ) {
      value = value.slice(1, -1)
    }
    values.set(key, value)
  }
  return values
}

function assertEnvEquals(values, key, expected, label) {
  const actual = values.get(key)
  if (actual !== expected) {
    fail(`invalid ${label}: expected ${key}=${expected}, got ${actual || '<missing>'}`)
  }
}

const failures = []
const api = readUtf8(`${runtimeControlApiDir}/index.ts`)
const page = readUtf8(`${runtimeControlViewDir}/index.vue`)
const probePanel = readUtf8(`${runtimeControlViewDir}/components/OpsProbeStatusPanel.vue`)
const envLocal = readUtf8('.env.local')
const realDrFlow = readUtf8('tests/e2e/runtime-control-real-dr-flow.e2e.js')
const opsE2EHelper = readUtf8('tests/e2e/runtime-control-ops-e2e-helper.js')
const publishTestSubmitRoute = readUtf8(
  'tests/e2e/runtime-control-publish-test-submit-route.e2e.js'
)
const publishTestRealFlow = readUtf8('tests/e2e/runtime-control-publish-test-real-flow.e2e.js')
const promoteProdRealFlow = readUtf8(
  'tests/e2e/runtime-control-promote-prod-real-flow.e2e.js'
)
const promoteBackupRealFlow = readUtf8('tests/e2e/runtime-control-promote-backup-real-flow.e2e.js')
const envValues = parseEnv(envLocal)
const viewFiles = collectFiles(runtimeControlViewDir)
const viewSource = viewFiles
  .filter((file) => /\.(vue|ts|js)$/.test(file))
  .map((file) => readUtf8(file))
  .join('\n')

const canonicalEndpoints = [
  '/alerts/page',
  '/owner-matrix',
  '/wizard/scenarios',
  '/rollback-candidates',
  '/restore-candidates',
  '/inspection-runs',
  '/business-health',
  '/probes/latest',
  '/capacity/status',
  '/backup-points',
  '/incidents/page'
]

for (const endpoint of canonicalEndpoints) {
  assertContains(
    api,
    `/infra/runtime-control${endpoint}`,
    `canonical runtime-control API endpoint ${endpoint}`
  )
}

assertEnvEquals(
  envValues,
  'VITE_BASE_URL',
  'http://127.0.0.1:48081',
  'local backend base URL'
)
assertEnvEquals(
  envValues,
  'VITE_PROXY_TARGET',
  'http://127.0.0.1:48081',
  'local proxy target'
)
assertEnvEquals(envValues, 'VITE_PORT', '8081', 'local frontend port')

assertNotContains(api, '/runtime-control/ops/', 'ops sub-namespace in runtime control API')
assertNotContains(api, '/infra/runtime-control/ops/', 'ops sub-namespace in runtime control API')
assertNotContains(api, '/inspection-reports', 'deprecated inspection reports resource')

const foolproofComponents = [
  ['候选选择组件', 'OpsCandidatePicker'],
  ['探针组件', 'OpsProbeStatusPanel'],
  ['日志磁盘组件', 'OpsLogDiskRiskPanel'],
  ['事故闭环组件', 'OpsIncidentDrawer']
]

for (const [label, componentName] of foolproofComponents) {
  assertFileExists(`${runtimeControlViewDir}/components/${componentName}.vue`, label)
  assertContains(page, `import ${componentName}`, `${label} main page import`)
  assertContains(page, `<${componentName}`, `${label} main page usage`)
}

const removedFoolproofCards = [
  ['站内信告警组件', 'OpsAlertInboxCard', '站内信告警'],
  ['责任人矩阵组件', 'OpsOwnerMatrixPanel', '责任人矩阵'],
  ['备份演练组件', 'OpsBackupDrillPanel', '备份演练'],
  ['决策向导组件', 'OpsDecisionWizard', '决策向导'],
  ['巡检组件', 'OpsInspectionReportPanel', '巡检报告'],
  ['业务健康组件', 'OpsBusinessHealthPanel', '业务健康']
]

for (const [label, componentName, title] of removedFoolproofCards) {
  const componentPath = `${runtimeControlViewDir}/components/${componentName}.vue`
  if (exists(componentPath)) {
    fail(`forbidden removed ${label}: ${componentPath}`)
  }
  assertNotContains(page, `import ${componentName}`, `removed ${label} main page import`)
  assertNotContains(page, `<${componentName}`, `removed ${label} main page usage`)
  assertNotContains(page, title, `removed ${label} card title`)
}

assertContains(page, 'loadOwnerMatrix()', 'owner matrix data load remains for operation owner gates')

assertContains(api, 'selectedImageCandidateId', 'rollback candidate ID request field')
assertContains(api, 'selectedRecoverySetCandidateId', 'restore recovery-set candidate ID request field')
assertContains(api, 'url?: string', 'probe response target URL contract')
assertContains(
  probePanel,
  '<el-table-column label="目标地址" prop="url"',
  'probe target URL table column'
)
assertNotContains(api, 'selectedImageTag?:', 'free text rollback image tag request field')
assertNotContains(api, 'selectedBackupId?:', 'free text restore backup ID request field')
assertNotContains(page, 'selectedImageTag', 'page free text rollback image tag field')
assertNotContains(page, 'selectedBackupId', 'page free text restore backup ID field')
assertNotContains(page, 'label="镜像标签"', 'rollback manual image tag label')
assertNotContains(page, 'label="备份点"', 'restore manual backup input form item')
assertNotContains(viewSource, '/foolproof-overview', 'nonexistent foolproof overview endpoint')
assertContains(
  page,
  'loadFoolproofData().catch',
  'foolproof data load must not be blocked by overview or operations failure'
)
assertContains(page, '运维矩阵：', 'runtime matrix load error context')
assertNotContains(page, '傻瓜式运维：', 'top-level foolproof load error context')
assertContains(page, 'foolproofLoadFailed = true', 'foolproof failure state marker')
assertContains(
  page,
  'connected.value = errors.length === 0 && !foolproofLoadFailed',
  'foolproof failure still affects connection status'
)

assertNotContains(
  realDrFlow,
  "process.env.RUNTIME_CONTROL_E2E_BASE_URL || 'http://172.30.30.58:8081'",
  'real DR flow remote frontend default'
)
assertNotContains(
  realDrFlow,
  "process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN || 'http://172.30.30.58:48081'",
  'real DR flow remote backend default'
)
assertContains(
  realDrFlow,
  'RUNTIME_CONTROL_E2E_BASE_URL is required',
  'real DR explicit frontend target requirement'
)
assertContains(
  realDrFlow,
  'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required',
  'real DR explicit backend target requirement'
)
assertContains(
  realDrFlow,
  'RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID is required',
  'real DR explicit restore candidate requirement'
)
assertNotContains(
  realDrFlow,
  'selectedCandidateText: backupId',
  'real DR restore should not reuse the just-created backup before rehearsal'
)

const postActionHealthScripts = [
  ['publish-test real-flow E2E', publishTestRealFlow],
  ['real DR flow E2E', realDrFlow]
]

for (const [label, source] of postActionHealthScripts) {
  assertContains(
    source,
    'RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL is required',
    `${label} explicit backend health URL requirement`
  )
  assertContains(
    source,
    'RUNTIME_CONTROL_TEST_FRONTEND_URL is required',
    `${label} explicit frontend health URL requirement`
  )
  assertContains(
    source,
    'RUNTIME_CONTROL_TEST_WEBSITE_URL is required',
    `${label} explicit website health URL requirement`
  )
  assertContains(
    source,
    'RUNTIME_CONTROL_TEST_SHOWROOM_URL is required',
    `${label} explicit showroom health URL requirement`
  )
  assertContains(source, 'HEALTH_OK ${url}', `${label} HEALTH_OK actual URL output`)
  assertNotContains(
    source,
    'http://172.30.30.58:48081/actuator/health',
    `${label} old remote backend health proof`
  )
  assertNotContains(
    source,
    'http://172.30.30.58:8081/',
    `${label} old remote frontend health proof`
  )
  assertNotContains(
    source,
    'http://172.30.30.58:8083/',
    `${label} old remote website health proof`
  )
  assertNotContains(
    source,
    'http://172.30.30.58:8083/showroom',
    `${label} old remote showroom health proof`
  )
}

assertNotContains(opsE2EHelper, 'DEFAULT_BASE_URL', 'runtime-control E2E helper base URL default')
assertNotContains(
  opsE2EHelper,
  'http://172.30.30.58:8081',
  'runtime-control E2E helper old remote frontend default'
)
assertNotContains(
  opsE2EHelper,
  'process.env.RUNTIME_CONTROL_E2E_BASE_URL ||',
  'runtime-control E2E helper implicit base URL fallback'
)
assertContains(
  opsE2EHelper,
  'RUNTIME_CONTROL_E2E_BASE_URL is required',
  'runtime-control E2E helper explicit frontend target requirement'
)
assertContains(
  opsE2EHelper,
  'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required',
  'runtime-control E2E helper explicit backend action origin requirement'
)
assertContains(
  opsE2EHelper,
  'getRuntimeControlBaseUrl',
  'runtime-control E2E helper base URL guard'
)
assertContains(
  opsE2EHelper,
  'getRuntimeControlActionOrigin',
  'runtime-control E2E helper action origin guard'
)

const actionOriginScripts = [
  ['publish-test submit-route E2E', publishTestSubmitRoute],
  ['publish-test real-flow E2E', publishTestRealFlow],
  ['promote-prod real-flow E2E', promoteProdRealFlow],
  ['promote-backup real-flow E2E', promoteBackupRealFlow]
]

for (const [label, source] of actionOriginScripts) {
  assertContains(
    source,
    'getRuntimeControlActionOrigin',
    `${label} uses explicit action origin guard`
  )
  assertNotContains(
    source,
    'process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN ||',
    `${label} implicit action origin fallback`
  )
  assertNotContains(
    source,
    "|| 'http://172.30.30.58:48081'",
    `${label} old remote action origin default`
  )
  assertNotContains(
    source,
    "|| 'http://127.0.0.1:48081'",
    `${label} fixed local action origin default`
  )
}

assertNotContains(
  promoteProdRealFlow,
  'process.env.RUNTIME_CONTROL_E2E_BASE_URL =',
  'promote-prod real-flow should not inject a fixed frontend target'
)
assertNotContains(
  promoteProdRealFlow,
  "|| 'http://localhost:8081'",
  'promote-prod real-flow fixed local frontend default'
)
assertNotContains(
  promoteProdRealFlow,
  '172.30.30.57',
  'promote-prod real-flow fixed production server target'
)
assertNotContains(
  promoteProdRealFlow,
  '172.30.30.58:48081',
  'promote-prod real-flow fixed test backend forbidden target'
)
assertNotContains(
  promoteProdRealFlow,
  ':48081',
  'promote-prod real-flow fixed backend port target'
)
assertNotContains(
  promoteProdRealFlow,
  ':8081',
  'promote-prod real-flow fixed frontend port target'
)
assertNotContains(
  promoteProdRealFlow,
  ':8083',
  'promote-prod real-flow fixed website/showroom port target'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_BACKEND_HEALTH_URL is required',
  'promote-prod explicit production backend health URL requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_FRONTEND_URL is required',
  'promote-prod explicit production frontend URL requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_WEBSITE_URL is required',
  'promote-prod explicit production website URL requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_SHOWROOM_URL is required',
  'promote-prod explicit production showroom URL requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_LOGIN_URL is required',
  'promote-prod explicit production login URL requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN is required',
  'promote-prod explicit production login backend origin requirement'
)
assertContains(
  promoteProdRealFlow,
  'RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN is required',
  'promote-prod explicit forbidden test backend origin requirement'
)

assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1',
  'promote-backup explicit approval requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_PROMOTE_BACKUP_RELEASE_TAG is required',
  'promote-backup explicit release package requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_BACKUP_BACKEND_HEALTH_URL is required',
  'promote-backup explicit backup backend health URL requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_BACKUP_FRONTEND_URL is required',
  'promote-backup explicit backup frontend URL requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_BACKUP_WEBSITE_URL is required',
  'promote-backup explicit backup website URL requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_BACKUP_SHOWROOM_URL is required',
  'promote-backup explicit backup showroom URL requirement'
)
assertContains(
  promoteBackupRealFlow,
  'RUNTIME_CONTROL_BACKUP_DCC_READBACK_URL is required',
  'promote-backup explicit DCC readback URL requirement'
)
assertContains(
  promoteBackupRealFlow,
  'testedRecoverySetCandidateId',
  'promote-backup release package recovery candidate proof'
)
assertContains(
  promoteBackupRealFlow,
  'testedRecoverySetManifestHash',
  'promote-backup release package recovery hash proof'
)

if (failures.length) {
  throw new Error(
    [
      'runtime control foolproof static contract is not satisfied:',
      ...failures.map((item) => `- ${item}`)
    ].join('\n')
  )
}

console.log(
  'PASS: runtime control foolproof canonical API, components, candidate-only, paired-port, and explicit health proof contracts are wired'
)
