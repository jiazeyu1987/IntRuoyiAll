const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const viewSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const orderSummaryStyle = viewSource.match(/\.frontline-production-order-summary\s*\{[\s\S]*?\n\}/)
assert.ok(orderSummaryStyle, 'production order summary style must exist.')
assert.match(
  orderSummaryStyle[0],
  /--frontline-production-order-summary-line-height:\s*30px/,
  'top active-order summary must use a fixed 30px line-height for deterministic three-line clipping.'
)
assert.match(
  orderSummaryStyle[0],
  /max-height:\s*calc\(var\(--frontline-production-order-summary-line-height\) \* 3\)/,
  'top active-order summary must be capped at exactly three visible lines.'
)
assert.match(
  orderSummaryStyle[0],
  /overflow:\s*hidden/,
  'top active-order summary must hide overflow after three lines.'
)

const orderValueStyle = viewSource.match(/\.frontline-production-order-summary \.frontline-production-order-summary__value\s*\{[\s\S]*?\n\}/)
assert.ok(orderValueStyle, 'production order summary value style must exist.')
assert.match(
  orderValueStyle[0],
  /line-height:\s*var\(--frontline-production-order-summary-line-height\)/,
  'top active-order summary values must use the shared fixed line-height.'
)

const materialTabsStyle = viewSource.match(/\.frontline-production-material-tabs\s*\{[\s\S]*?\n\}/)
assert.ok(materialTabsStyle, 'production material tabs style must exist.')
const materialTabsStart = viewSource.indexOf('data-frontline-production-material-tabs')
const materialTabsEnd = viewSource.indexOf('data-frontline-error-slot', materialTabsStart)
assert.ok(materialTabsStart >= 0 && materialTabsEnd > materialTabsStart, 'production material tab template block must exist.')
const materialTabsTemplate = viewSource.slice(materialTabsStart, materialTabsEnd)
const defectSectionStart = viewSource.indexOf('class="frontline-production-defect-section defect-section"')
const defectSectionEnd = viewSource.indexOf('data-frontline-production-material-tabs', defectSectionStart)
assert.ok(defectSectionStart >= 0 && defectSectionEnd > defectSectionStart, 'production defect section template block must exist.')
const defectSectionTemplate = viewSource.slice(defectSectionStart, defectSectionEnd)
assert.doesNotMatch(
  defectSectionTemplate,
  />\s*不良明细\s*</,
  'defect section must not render the visible 不良明细 title inside the red-box area.'
)
assert.match(
  materialTabsTemplate,
  /<strong>\s*\{\{\s*formatProductionMaterialTabLabel\(material\)\s*\}\}\s*<\/strong>/,
  'material tab visible text must render the formatted material name and entered quantities.'
)
assert.match(
  viewSource,
  /const formatProductionMaterialTabLabel = \(material: ProductionMaterialOption\) => \{[\s\S]*outputQuantity[\s\S]*lossQuantity[\s\S]*`\$\{material\.materialName\}\(\$\{outputQuantity\}\/\$\{lossQuantity\}\)`/,
  'completed material tabs must show name(output/loss), for example 杠杆(3012/12).'
)
assert.doesNotMatch(
  materialTabsTemplate,
  /<small>\s*\{\{\s*material\.materialCode\s*\}\}\s*<\/small>/,
  'material tab visible text must not render the material code.'
)
assert.doesNotMatch(
  materialTabsTemplate,
  /frontline-production-material-batches/,
  'material tab visible text must not render batch-code rows in the compact tab.'
)
const quantityPanelStyle = viewSource.match(/\.frontline-production-quantity-panel\s*\{[\s\S]*?\n\}/)
assert.ok(quantityPanelStyle, 'production quantity panel style must exist.')
assert.match(
  quantityPanelStyle[0],
  /align-content:\s*stretch/,
  'quantity panel must stretch its internal rows so the material tabs move down instead of leaving bottom whitespace.'
)
assert.match(
  quantityPanelStyle[0],
  /grid-template-rows:\s*auto auto minmax\(min-content,\s*1fr\) auto auto/,
  'defect area must keep enough intrinsic height to show all configured reasons before material tabs.'
)
assert.match(
  materialTabsStyle[0],
  /grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/,
  'material tabs must render at most four cards per row.'
)
assert.match(
  materialTabsStyle[0],
  /grid-template-rows:\s*repeat\(2,\s*72px\)/,
  'material tabs must reserve two compact visible rows.'
)
assert.match(
  materialTabsStyle[0],
  /grid-auto-rows:\s*72px/,
  'additional material rows must keep the same compact row height.'
)
assert.match(
  materialTabsStyle[0],
  /max-height:\s*calc\(72px \* 2 \+ 10px\)/,
  'material tabs area must not exceed two rows plus one row gap.'
)

const materialTabStyle = viewSource.match(/\.frontline-production-material-tab\s*\{[\s\S]*?\n\}/)
assert.ok(materialTabStyle, 'production material tab style must exist.')
assert.match(materialTabStyle[0], /min-height:\s*72px/, 'material tab card height must be compact.')
assert.match(materialTabStyle[0], /padding:\s*6px 8px/, 'material tab padding must be compact.')
assert.match(materialTabStyle[0], /border:\s*3px solid var\(--frontline-line\)/, 'material tab border must be slimmer.')

assert.match(
  viewSource,
  /\.frontline-production-material-tab[\s\S]*strong[\s\S]*font-size:\s*24px/,
  'material tab name font must be reduced for two-row layout.'
)
const inlineErrorStyle = viewSource.match(/\.frontline-inline-error-slot\s*\{[\s\S]*?\n\}/)
assert.ok(inlineErrorStyle, 'inline error slot style must exist.')
assert.match(inlineErrorStyle[0], /display:\s*none/, 'hidden inline error slot must not reserve whitespace.')
const visibleInlineErrorStyle = viewSource.match(/\.frontline-inline-error-slot\.is-visible\s*\{[\s\S]*?\n\}/)
assert.ok(visibleInlineErrorStyle, 'visible inline error slot style must exist.')
assert.match(visibleInlineErrorStyle[0], /display:\s*grid/, 'inline error slot must render as grid only when visible.')

const defectSectionStyle = viewSource.match(/\.frontline-production-defect-section\s*\{[\s\S]*?\n\}/)
assert.ok(defectSectionStyle, 'production defect section style must exist.')
assert.match(
  defectSectionStyle[0],
  /grid-template-rows:\s*minmax\(min-content,\s*1fr\)/,
  'defect section must allocate the whole red-box area to defect reason controls.'
)
assert.match(
  defectSectionStyle[0],
  /overflow:\s*visible/,
  'defect section must not hide configured defect reason controls.'
)

const defectGridStyle = viewSource.match(/\.frontline-production-defect-grid\s*\{[\s\S]*?\n\}/)
assert.ok(defectGridStyle, 'production defect grid style must exist.')
assert.doesNotMatch(
  defectGridStyle[0],
  /grid-template-rows:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/,
  'defect grid must not force all reasons into four shrinking rows.'
)
assert.match(
  defectGridStyle[0],
  /grid-auto-rows:\s*minmax\(62px,\s*auto\)/,
  'defect grid rows must be tall enough to show labels, buttons, quantity and unit fully.'
)
assert.match(
  defectGridStyle[0],
  /align-content:\s*start/,
  'defect grid must stack visible reason rows from the top of the defect area.'
)

console.log('PASS: frontline production material layout static contract')
