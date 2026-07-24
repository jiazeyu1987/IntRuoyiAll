const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')

const requiredFiles = [
  // P0 page skeleton contract
  'docs/edhr/commercial-page-skeleton-contract.json',
  'tests/e2e/edhr-commercial-page-skeleton-static.spec.js',
  // T1-T6 APIs
  'src/api/mes/pro/edhr/initBatch.ts',
  'src/api/mes/pro/edhr/form.ts',
  'src/api/mes/pro/edhr/traveler.ts',
  'src/api/mes/pro/edhr/release.ts',
  'src/api/mes/pro/edhr/report.ts',
  'src/api/mes/pro/edhr/delivery.ts',
  // T1-T6 pages
  'src/views/mes/pro/edhr-init-batch/InitBatchPage.vue',
  'src/views/mes/pro/edhr-form/FormPage.vue',
  'src/views/mes/pro/edhr-traveler/TravelerPage.vue',
  'src/views/mes/pro/edhr-release/ReleasePage.vue',
  'src/views/mes/pro/edhr-report/ReportPage.vue',
  'src/views/mes/pro/edhr-delivery/DeliveryPage.vue',
  // T1-T6 static gates
  'tests/e2e/edhr-init-batch-static.spec.js',
  'tests/e2e/edhr-form-static.spec.js',
  'tests/e2e/edhr-traveler-static.spec.js',
  'tests/e2e/edhr-release-precheck-static.spec.js',
  'tests/e2e/edhr-report-static.spec.js',
  'tests/e2e/edhr-delivery-static.spec.js'
]

const expectedApiTokens = {
  'src/api/mes/pro/edhr/initBatch.ts': [
    "const EDHR_INIT_BATCH_BASE_URL = '/mes/pro/edhr-init-batch'",
    '${EDHR_INIT_BATCH_BASE_URL}/page',
    '${EDHR_INIT_BATCH_BASE_URL}/precheck'
  ],
  'src/api/mes/pro/edhr/form.ts': ['/mes/pro/edhr-form-template/page', '/mes/pro/edhr-form-instance/submit'],
  'src/api/mes/pro/edhr/traveler.ts': ['/mes/pro/edhr-traveler/page', '/mes/pro/edhr-traveler/generate'],
  'src/api/mes/pro/edhr/release.ts': ['/mes/pro/edhr-release/page', '/mes/pro/edhr-release/precheck'],
  'src/api/mes/pro/edhr/report.ts': ['/mes/pro/edhr-report-catalog/page', '/mes/pro/edhr-report-query/run'],
  'src/api/mes/pro/edhr/delivery.ts': ['/mes/pro/edhr-delivery-cockpit/project/page', '/mes/pro/edhr-delivery-cockpit/gate-summary']
}

function fileText(relativePath) {
  const fullPath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(fullPath), `missing first-wave frontend artifact: ${relativePath}`)
  return fs.readFileSync(fullPath, 'utf8')
}

function assertNoSilentCatch(relativePath) {
  const text = fileText(relativePath)
  assert.ok(!/catch\s*\{\s*\}/.test(text), `${relativePath} must not swallow backend errors`)
}

function assertFirstWaveFrontendIntegration() {
  const missing = requiredFiles.filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)))
  assert.deepStrictEqual(missing, [], `missing first-wave frontend artifacts: ${missing.join(', ')}`)

  const skeleton = JSON.parse(fileText('docs/edhr/commercial-page-skeleton-contract.json'))
  const pageKeys = new Set(skeleton.pages.map((page) => page.key))
  for (const key of ['edhr-init-batch', 'edhr-form-template', 'edhr-form-instance', 'edhr-traveler', 'edhr-release', 'edhr-report', 'edhr-delivery']) {
    assert.ok(pageKeys.has(key), `page skeleton contract missing ${key}`)
  }

  for (const [relativePath, tokens] of Object.entries(expectedApiTokens)) {
    const text = fileText(relativePath)
    for (const token of tokens) {
      assert.ok(text.includes(token), `${relativePath} missing API token ${token}`)
    }
  }

  for (const relativePath of requiredFiles.filter((file) => file.endsWith('.vue') || file.endsWith('.ts'))) {
    assertNoSilentCatch(relativePath)
  }
}

assertFirstWaveFrontendIntegration()
console.log('eDHR commercial first-wave frontend integration contract is valid')
