const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const styleIndex = readSource('src/styles/index.scss')
const schemeStyle = readSource('src/styles/scheme-d-controls.scss')

assert.equal(
  packageJson.scripts['e2e:basic-data:scheme-d-controls:static'],
  'node tests/e2e/basic-data-scheme-d-controls-static.spec.js',
  'package.json must expose the Scheme D basic-data control static contract'
)

assert.match(
  styleIndex,
  /@use\s+['"]\.\/scheme-d-controls\.scss['"]/,
  'global style index must import the scoped Scheme D control stylesheet'
)

for (const token of [
  '--scheme-d-primary',
  '--scheme-d-success',
  '--scheme-d-warning',
  '--scheme-d-danger',
  '.scheme-d-basic-data-page',
  '.scheme-d-btn--primary',
  '.scheme-d-btn--success',
  '.scheme-d-btn--warning',
  '.scheme-d-btn--danger',
  '.scheme-d-row-action--danger',
  '.scheme-d-icon-button',
  '.scheme-d-tag',
  '.scheme-d-form-control',
  '.scheme-d-feedback',
  '.scheme-d-dialog-footer'
]) {
  assert.ok(schemeStyle.includes(token), `Scheme D stylesheet must define ${token}`)
}

const scopedPages = [
  {
    name: 'MDM product master',
    file: 'src/views/mdm/product/index.vue',
    required: [
      'scheme-d-basic-data-page--mdm-product',
      'scheme-d-btn--success',
      'scheme-d-btn--primary',
      'scheme-d-btn--warning',
      'scheme-d-row-action--primary',
      'scheme-d-row-action--success',
      'scheme-d-dialog-footer',
      'scheme-d-tag'
    ]
  },
  {
    name: 'DCC project code',
    file: 'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue',
    required: [
      'scheme-d-basic-data-page--dcc-project-code',
      'scheme-d-btn--success',
      'scheme-d-btn--primary',
      'scheme-d-btn--warning',
      'scheme-d-btn--danger',
      'scheme-d-row-action--danger',
      'scheme-d-dialog-footer',
      'scheme-d-tag'
    ]
  },
  {
    name: 'DCC product catalog',
    file: 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue',
    required: [
      'scheme-d-basic-data-page--dcc-product-catalog',
      'scheme-d-btn--success',
      'scheme-d-row-action--primary',
      'scheme-d-row-action--danger',
      'scheme-d-dialog-footer'
    ]
  },
  {
    name: 'DCC file type taxonomy',
    file: 'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/index.vue',
    required: [
      'scheme-d-basic-data-page--dcc-file-type-taxonomy',
      'scheme-d-btn--success',
      'scheme-d-row-action--primary',
      'scheme-d-row-action--danger',
      'scheme-d-dialog-footer',
      'scheme-d-tag'
    ]
  },
  {
    name: 'Form center template',
    file: 'src/views/form-center/template/index.vue',
    required: [
      'scheme-d-basic-data-page--form-template',
      'scheme-d-btn--primary',
      'scheme-d-btn--warning',
      'scheme-d-btn--danger',
      'scheme-d-row-action--danger',
      'scheme-d-icon-button',
      'ep:arrow-left',
      'scheme-d-dialog-footer',
      'scheme-d-tag'
    ]
  }
]

for (const page of scopedPages) {
  const source = readSource(page.file)
  assert.ok(
    source.includes('scheme-d-basic-data-page'),
    `${page.name} must be scoped by scheme-d-basic-data-page`
  )
  for (const token of page.required) {
    assert.ok(source.includes(token), `${page.name} must include Scheme D marker ${token}`)
  }
  assert.doesNotMatch(
    source,
    /mock|placeholder data|fallback|降级|吞异常/i,
    `${page.name} must not introduce fallback/mock/degradation behavior while styling controls`
  )
}

console.log('PASS: basic data Scheme D control static contract')
