const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const contractPath = path.join(repoRoot, 'docs', 'edhr', 'commercial-page-skeleton-contract.json')

const expectedPages = {
  'edhr-init-batch': ['T1', 'mes:pro-edhr-init-batch:query'],
  'edhr-dhr-template': ['T1', 'mes:pro-edhr-dhr-template:query'],
  'edhr-form-template': ['T2', 'mes:pro-edhr-form-template:query'],
  'edhr-form-instance': ['T2', 'mes:pro-edhr-form-instance:query'],
  'edhr-traveler': ['T3', 'mes:pro-edhr-traveler:query'],
  'edhr-label': ['T3', 'mes:pro-edhr-label:query'],
  'edhr-print-task': ['T3', 'mes:pro-edhr-print-task:query'],
  'edhr-release': ['T4', 'mes:pro-edhr-release:query'],
  'edhr-change': ['T4', 'mes:pro-edhr-change:query'],
  'edhr-flow-intervention': ['T4', 'mes:pro-edhr-flow-intervention:query'],
  'edhr-report': ['T5', 'mes:pro-edhr-report:query'],
  'edhr-dashboard': ['T5', 'mes:pro-edhr-dashboard:query'],
  'edhr-project-package': ['T5', 'mes:pro-edhr-project-package:query'],
  'edhr-delivery': ['T6', 'mes:pro-edhr-delivery:query'],
  'edhr-validation': ['T6', 'mes:pro-edhr-validation:query'],
  'edhr-training': ['T6', 'mes:pro-edhr-training:query'],
  'edhr-deployment-evidence': ['T6', 'mes:pro-edhr-deployment-evidence:query']
}

function readContract() {
  assert.ok(fs.existsSync(contractPath), 'eDHR commercial page skeleton contract must exist')
  return JSON.parse(fs.readFileSync(contractPath, 'utf8'))
}

function assertUnique(pages, field) {
  const values = pages.map((page) => page[field])
  assert.strictEqual(values.length, new Set(values).size, `${field} must be unique`)
}

function assertContract() {
  const contract = readContract()
  assert.strictEqual(contract.schemaVersion, 1)
  assert.strictEqual(contract.styleSource, 'D:/ProjectPackage/Int/IntPP/FRONTEND_STYLE.md')
  assert.strictEqual(contract.menuPublishPolicy, 'component-first')
  assert.strictEqual(contract.errorPolicy, 'visible-error-no-empty-catch')

  for (const required of [
    'filterToolbar',
    'attachedDataTable',
    'paginationFooter',
    'compactStatusTags',
    'inlineRowActions',
    'visibleErrorFeedback'
  ]) {
    assert.ok(contract.layoutRules.includes(required), `layoutRules must include ${required}`)
  }

  const pages = contract.pages
  const byKey = Object.fromEntries(pages.map((page) => [page.key, page]))
  assert.deepStrictEqual(Object.keys(byKey).sort(), Object.keys(expectedPages).sort())

  for (const [key, [module, permission]] of Object.entries(expectedPages)) {
    const page = byKey[key]
    assert.strictEqual(page.module, module)
    assert.strictEqual(page.permission, permission)
    assert.ok(page.routePath.startsWith('/mes/pro/feedback/edhr-'), `${key} routePath must stay under eDHR feedback menu`)
    assert.ok(['list', 'workbench', 'matrix'].includes(page.pageType), `${key} must declare a supported pageType`)
    assert.ok(['planned', 'existing'].includes(page.status), `${key} must declare status`)
    assert.strictEqual(page.requiresVisibleErrorFeedback, true, `${key} must expose backend errors`)
    if (page.status === 'planned') {
      assert.ok(page.componentFile.startsWith('src/views/mes/pro/edhr-commercial/'), `${key} planned component root is invalid`)
      assert.strictEqual(page.requiresComponentBeforeMenuPublish, true, `${key} must require component before menu publish`)
      assert.strictEqual(page.allowBlankPlaceholderPage, false, `${key} must not allow placeholder pages`)
    }
  }

  assert.deepStrictEqual(byKey['edhr-change'], {
    key: 'edhr-change',
    module: 'T4',
    routePath: '/mes/pro/feedback/edhr-change',
    componentFile: 'src/views/mes/pro/edhr/RecordChangePage.vue',
    permission: 'mes:pro-edhr-change:query',
    pageType: 'list',
    status: 'existing',
    requiresComponentBeforeMenuPublish: true,
    requiresVisibleErrorFeedback: true,
    allowBlankPlaceholderPage: false
  })

  for (const field of ['key', 'routePath', 'componentFile', 'permission']) {
    assertUnique(pages, field)
  }
}

assertContract()
console.log('eDHR commercial page skeleton contract is valid')
