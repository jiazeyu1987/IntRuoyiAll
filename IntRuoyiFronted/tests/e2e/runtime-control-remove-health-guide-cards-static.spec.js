const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = 'src/views/infra/runtime-control/index.vue'
const componentDir = 'src/views/infra/runtime-control/components'
const e2eExpectationFiles = [
  'tests/e2e/runtime-control-all-buttons-real.e2e.js',
  'tests/e2e/runtime-control-real-data-all-features.e2e.js',
  'tests/e2e/runtime-control-yudao-admin-readonly.e2e.js'
]

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
  ['decision wizard card', 'OpsDecisionWizard', '决策向导'],
  ['inspection report card', 'OpsInspectionReportPanel', '巡检报告'],
  ['business health card', 'OpsBusinessHealthPanel', '业务健康']
]) {
  assertNotContains(page, `import ${componentName}`, `${label} import`)
  assertNotContains(page, `<${componentName}`, `${label} usage`)
  assertNotContains(page, title, `${label} title`)
  assertFileMissing(`${componentDir}/${componentName}.vue`, `${label} component file`)
}

for (const forbidden of [
  'wizardScenarios',
  'wizardRecommendation',
  'loadWizardScenarios',
  'loadWizardRecommendation',
  'inspectionRun',
  'runInspection',
  'businessHealth',
  'opsLoading.wizard',
  'opsLoading.inspection',
  'opsLoading.businessHealth',
  'loadBusinessHealth'
]) {
  assertNotContains(page, forbidden, `removed card-only state ${forbidden}`)
}

for (const relativePath of e2eExpectationFiles) {
  const source = readUtf8(relativePath)
  assertNotContains(source, '决策向导', `${relativePath} removed decision wizard expectation`)
  assertNotContains(source, '巡检报告', `${relativePath} removed inspection expectation`)
  assertNotContains(source, '业务健康', `${relativePath} removed business health expectation`)
  assertNotContains(source, '/wizard/scenarios', `${relativePath} removed wizard scenarios wait`)
  assertNotContains(source, '/wizard/recommendation', `${relativePath} removed wizard recommendation wait`)
  assertNotContains(source, '/inspection-runs', `${relativePath} removed inspection wait`)
  assertNotContains(source, '/business-health', `${relativePath} removed business health wait`)
}

assertContains(page, 'OpsProbeStatusPanel', 'probe status card remains')
assertContains(page, 'OpsLogDiskRiskPanel', 'log disk risk card remains')
assertContains(page, 'OpsIncidentDrawer', 'incident drawer remains')
assertContains(page, 'loadCandidates()', 'release and restore candidates still load')
assertContains(page, 'loadLatestProbes()', 'probe status still loads')
assertContains(page, 'loadCapacityStatus()', 'capacity status still loads')

console.log('PASS: runtime control decision, inspection, and business health cards are removed from frontend')
