const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const api = fs.readFileSync(path.join(repoRoot, 'src/api/infra/runtimeControl/index.ts'), 'utf8')
const failures = []

function fail(message) {
  failures.push(message)
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    fail(`missing ${label}: ${expected}`)
  }
}

function extractExportConstFunction(source, name) {
  const start = source.indexOf(`export const ${name}`)
  if (start < 0) {
    fail(`missing exported function: ${name}`)
    return ''
  }
  const next = source.indexOf('\nexport const ', start + 1)
  return source.slice(start, next < 0 ? source.length : next)
}

assertContains(
  api,
  'const RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT = 70000',
  'runtime control foolproof request timeout constant'
)

const foolproofTimeoutApis = [
  'getRuntimeControlOperations',
  'getRuntimeControlAlertsPage',
  'getRuntimeControlOwnerMatrix',
  'getRuntimeControlWizardScenarios',
  'getRuntimeControlWizardRecommendation',
  'getRuntimeControlRollbackCandidates',
  'getRuntimeControlRestoreCandidates',
  'getRuntimeControlReleasePackages',
  'getRuntimeControlBusinessHealth',
  'getRuntimeControlLatestProbes',
  'getRuntimeControlCapacityStatus',
  'getRuntimeControlBackupPoints',
  'getRuntimeControlIncidentsPage'
]

for (const apiName of foolproofTimeoutApis) {
  const section = extractExportConstFunction(api, apiName)
  assertContains(
    section,
    'timeout: RUNTIME_CONTROL_FOOLPROOF_REQUEST_TIMEOUT',
    `${apiName} explicit foolproof timeout`
  )
}

if (failures.length) {
  throw new Error(
    [
      'runtime control foolproof timeout contract is not satisfied:',
      ...failures.map((item) => `- ${item}`)
    ].join('\n')
  )
}

console.log('PASS: runtime control foolproof APIs use explicit operation timeout')
