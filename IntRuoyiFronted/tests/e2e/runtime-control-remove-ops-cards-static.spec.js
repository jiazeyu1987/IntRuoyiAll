const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = 'src/views/infra/runtime-control/index.vue'
const componentDir = 'src/views/infra/runtime-control/components'
const visibleE2EPath = 'tests/e2e/runtime-control-ops-cards-visible.e2e.js'

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function exists(relativePath) {
  return fs.existsSync(path.join(repoRoot, relativePath))
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

function assertFileMissing(relativePath, label) {
  if (exists(relativePath)) {
    throw new Error(`forbidden ${label}: ${relativePath}`)
  }
}

const page = readUtf8(pagePath)

for (const [label, componentName, title] of [
  ['alert inbox card', 'OpsAlertInboxCard', '站内信告警'],
  ['owner matrix card', 'OpsOwnerMatrixPanel', '责任人矩阵'],
  ['backup drill card', 'OpsBackupDrillPanel', '备份演练']
]) {
  assertNotContains(page, `import ${componentName}`, `${label} import`)
  assertNotContains(page, `<${componentName}`, `${label} usage`)
  assertNotContains(page, title, `${label} title`)
  assertFileMissing(`${componentDir}/${componentName}.vue`, `${label} component file`)
}

for (const forbidden of [
  'alertPage',
  'opsLoading.alerts',
  'loadAlertsPage',
  'acknowledgeAlert',
  'resendAlertSiteMessage',
  'backupPoints',
  'opsLoading.backupPoints',
  'loadBackupPoints'
]) {
  assertNotContains(page, forbidden, `removed card-only state ${forbidden}`)
}

assertContains(page, 'ownerMatrix', 'owner matrix data remains for operation owner gates')
assertContains(page, 'loadOwnerMatrix()', 'owner matrix loading remains for operation owner gates')
assertContains(page, 'operationRequiredOwnerText', 'operation owner gate remains wired')
assertFileMissing(visibleE2EPath, 'old visible-card E2E after requirement reversal')

console.log('PASS: runtime control alert, owner matrix, and backup drill cards are removed from frontend')
