const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const signaturePage = fs.readFileSync(
  path.join(repoRoot, 'src/views/dcc/controlled-file/signatures/index.vue'),
  'utf8'
)
const mySignaturePane = fs.readFileSync(
  path.join(repoRoot, 'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue'),
  'utf8'
)

const extractTemplate = (source, tableKey, label) => {
  const pattern = new RegExp(
    `<UnifiedListTemplate[\\s\\S]*?table-key="${tableKey.replaceAll('.', '\\.')}"[\\s\\S]*?<\\/UnifiedListTemplate>`
  )
  const match = source.match(pattern)
  assert.ok(match, `${label} must keep UnifiedListTemplate`)
  return match[0]
}

const recordTemplate = extractTemplate(
  signaturePage,
  'dcc.electronicSignature.records',
  'DCC signature records standard list'
)
const authorizationTemplate = extractTemplate(
  signaturePage,
  'dcc.electronicSignature.authorizations',
  'DCC signature authorization standard list'
)

for (const label of ['上传图片', '启用图片', '停用图片']) {
  assert.match(mySignaturePane, new RegExp(`>\\s*${label}\\s*<`), `my signature toolbar action must stay visible: ${label}`)
  assert.ok(label.length <= 4, `signature image action label must be at most 4 chars: ${label}`)
}

const assertRedBoxOnlyTemplate = (template, label, quickFilterName) => {
  assert.match(
    template,
    new RegExp(`:filter-definitions="${quickFilterName}"`),
    `${label} must keep the standard quick-filter field selector`
  )
  assert.match(
    template,
    /@quick-filter-query=/,
    `${label} must keep the standard quick-filter query button`
  )
  assert.match(
    template,
    /:show-column-reset="false"/,
    `${label} must hide reset-column because it is outside the user red box`
  )
  assert.doesNotMatch(
    template,
    /:show-column-settings="false"/,
    `${label} must keep display-field control because it is inside the user red box`
  )
  assert.doesNotMatch(
    template,
    /@column-reset=/,
    `${label} must not keep reset-column event bindings after the control is removed`
  )
  assert.doesNotMatch(
    template,
    /<template\s+#extra-filters>/,
    `${label} must remove all extra filters outside the user red box`
  )
}

assertRedBoxOnlyTemplate(recordTemplate, 'record list', 'recordQuickFilterDefinitions')
assert.doesNotMatch(
  recordTemplate,
  /<template\s+#actions>/,
  'record list must keep the red-box toolbar free of signature image actions'
)
assertRedBoxOnlyTemplate(
  authorizationTemplate,
  'authorization list',
  'authorizationQuickFilterDefinitions'
)
assert.doesNotMatch(
  authorizationTemplate,
  /data-testid="dcc-signature-image-toolbar-actions"|上传图片|启用图片|停用图片/,
  'authorization list must not render personal signature image actions'
)
assert.match(
  mySignaturePane,
  /data-testid="dcc-my-signature-image-actions"[\s\S]*上传图片[\s\S]*启用图片[\s\S]*停用图片/,
  'my signature pane must render personal signature image actions in its toolbar'
)

assert.doesNotMatch(
  signaturePage,
  /class="signature-image-panel"/,
  'authorization list must not render the standalone signature image intro panel above the toolbar'
)

assert.doesNotMatch(
  signaturePage,
  /data-testid="dcc-signature-view-mode"|signatureViewModeOptions|signature-view-toolbar|常用视图|高级视图/,
  'signature page must remove common/advanced view switch and toolbar outside the user red box'
)

console.log('PASS: DCC signature red-box controls-only static contract')
