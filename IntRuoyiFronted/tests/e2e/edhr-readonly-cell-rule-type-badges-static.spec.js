const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) =>
  fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const readonlyForm = readSource(
  'src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue'
)
const ruleHelper = readSource('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')

assert.ok(
  ruleHelper.includes('resolveTemplateRuleTypeBadge'),
  'shared rule helper must keep exposing type badge resolver'
)

for (const token of [
  'resolveTemplateRuleTypeBadge',
  'resolveTemplateRuleState',
  'resolveTemplateRuleTooltip',
  'TemplateRuleTypeBadge'
]) {
  assert.ok(
    readonlyForm.includes(token),
    `readonly form must consume shared rule type metadata: ${token}`
  )
}

assert.ok(
  readonlyForm.includes('resolveReadonlyRuleContext'),
  'readonly form must build a rule context from fillForm / edhrCellRule metadata'
)

assert.ok(
  readonlyForm.includes('edhr-template-sheet__rule-type-badge'),
  'readonly cells must render a visible compact type badge'
)

assert.ok(
  readonlyForm.includes('ruleBadge.symbol'),
  'readonly rule badges must display the type symbol instead of a uniform question mark'
)

assert.ok(
  readonlyForm.includes('normalizeReadonlyFillableRawText') &&
    readonlyForm.includes("normalizedText === '?'"),
  'readonly fillable cells must suppress imported generic question-mark text when a rule badge is available'
)

assert.ok(
  readonlyForm.includes('cell.ruleTooltip'),
  'readonly type badges must expose a tooltip/title'
)

assert.ok(
  readonlyForm.includes('is-rule-auto') &&
    readonlyForm.includes('is-rule-reviewed') &&
    readonlyForm.includes('is-rule-manual'),
  'readonly cells must separate rule state from rule type'
)

console.log('PASS: eDHR readonly cell rule type badges static contract')
