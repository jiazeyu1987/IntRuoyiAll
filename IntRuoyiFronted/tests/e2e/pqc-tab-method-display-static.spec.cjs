const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panelSource = fs.readFileSync(panelPath, 'utf8')

const tabStart = panelSource.indexOf('data-pqc-inspection-tabs')
const tabEnd = panelSource.indexOf('</nav>', tabStart)
assert.ok(tabStart >= 0 && tabEnd > tabStart, 'PQC inspection tab block must exist.')
const tabBlock = panelSource.slice(tabStart, tabEnd)

assert.match(
  tabBlock,
  /<strong>\{\{\s*formatPqcInspectionItemTabLabel\(item\)\s*\}\}<\/strong>/,
  'PQC red-box tab title must use the dedicated display helper.'
)

const tabLabelHelperStart = panelSource.indexOf('const formatPqcInspectionItemTabLabel')
const tabLabelHelperEnd = panelSource.indexOf('const formatPqcStandardSummary', tabLabelHelperStart)
assert.ok(
  tabLabelHelperStart >= 0 && tabLabelHelperEnd > tabLabelHelperStart,
  'PQC tab label helper must exist.'
)
const tabLabelHelperBlock = panelSource.slice(tabLabelHelperStart, tabLabelHelperEnd)
assert.match(
  tabLabelHelperBlock,
  /formatPqcMethodSummary\(item\)/,
  'PQC red-box tab title must display the formal inspection method, e.g. 目视检验.'
)
assert.doesNotMatch(
  tabLabelHelperBlock,
  /item\.itemName|item\.label|item\.key|itemCode/,
  'PQC red-box tab title must not display AO5 final inspection, itemName, itemCode, or key.'
)

const methodDialogStart = panelSource.indexOf('data-pqc-method-dialog')
const methodDialogEnd = panelSource.indexOf('<button type="button" @click="closePqcMethodDialog"', methodDialogStart)
assert.ok(
  methodDialogStart >= 0 && methodDialogEnd > methodDialogStart,
  'PQC method dialog block must exist.'
)
const methodDialogBlock = panelSource.slice(methodDialogStart, methodDialogEnd)
assert.match(
  methodDialogBlock,
  /<h3>\{\{\s*formatPqcMethodSummary\(activePqcMethodItem\)\s*\}\}<\/h3>/,
  'PQC method dialog title must display the normalized inspection method.'
)
assert.match(
  methodDialogBlock,
  /<p>\{\{\s*formatPqcMethodSummary\(activePqcMethodItem\)\s*\}\}<\/p>/,
  'PQC method dialog body must display the same normalized inspection method.'
)
assert.doesNotMatch(
  methodDialogBlock,
  /activePqcMethodItem\.label|activePqcMethodItem\.itemName/,
  'PQC method dialog must not display AO5 final inspection from the item label.'
)

assert.match(
  panelSource,
  /const normalizePqcInspectionMethodLabel = \(inspectionMethod: string\) =>[\s\S]*['"]Visual inspection['"][\s\S]*['"]目视检验['"]/,
  'Visual inspection from the formal method field must display as 目视检验.'
)

assert.match(
  panelSource,
  /buildPqcItemDetailsPayload[\s\S]*itemCode: item\.key[\s\S]*itemName: item\.itemName[\s\S]*inspectionMethod: item\.inspectionMethod/,
  'Changing the red-box tab title must not alter itemCode/itemName/inspectionMethod submission identity.'
)

console.log('PASS: PQC red-box tabs display inspection method while preserving item identity')
