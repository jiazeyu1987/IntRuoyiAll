const fs = require('fs')
const path = require('path')
const assert = require('assert')

const frontendRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) =>
  fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const editableForm = readSource(
  'src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue'
)
const ruleHelper = readSource('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')

assert.ok(
  ruleHelper.includes('TemplateRuleTypeBadge'),
  'shared rule helper must expose a type badge contract'
)
assert.ok(
  ruleHelper.includes('resolveTemplateRuleTypeBadge'),
  'shared rule helper must resolve visible type badges from rule value/component type'
)
assert.ok(
  ruleHelper.includes('resolveTemplateRuleState'),
  'shared rule helper must separate rule state from rule type'
)
assert.ok(
  ruleHelper.includes('resolveTemplateRuleTooltip'),
  'shared rule helper must build tooltip content for badges'
)

for (const token of ['STRING', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'SIGNATURE']) {
  assert.ok(ruleHelper.includes(token), `type badge map must cover ${token}`)
}

for (const field of ['source?: string', 'reviewed: boolean', 'componentFlag: string']) {
  assert.ok(
    ruleHelper.includes(field),
    `TemplateEditableCellContext must carry rule metadata: ${field}`
  )
}

assert.ok(
  editableForm.includes('edhr-template-editable-form__rule-type-badge'),
  'editable cells must render a compact type badge'
)
assert.ok(
  editableForm.includes('cell.ruleTooltip'),
  'type badge must expose a tooltip/title so symbols are understandable'
)
assert.ok(
  editableForm.includes('is-rule-auto') &&
    editableForm.includes('is-rule-reviewed') &&
    editableForm.includes('is-rule-manual'),
  'editable cells must use separate visual states for auto, reviewed, and manual rules'
)
assert.ok(
  editableForm.includes('edhr-template-editable-form__rule-legend'),
  'editable form must render a compact legend for type symbols'
)

assert.ok(
  /background:\s*#fff7ed/.test(editableForm) &&
    /background:\s*#f0fdf4/.test(editableForm) &&
    /border-color:\s*#fecaca/.test(editableForm),
  'status colors must be amber for auto, green for reviewed, and red-capable for errors'
)

console.log('PASS: eDHR cell rule type badges static contract')
