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
  'PQC bottom tab title must render through the formal method display helper, not the raw item label.'
)
assert.doesNotMatch(
  tabBlock,
  /<strong>\{\{\s*item\.label\s*\}\}<\/strong>/,
  'PQC bottom tab title must not render the legacy label that can fall back to itemCode.'
)

const itemMapStart = panelSource.indexOf('const pqcInspectionItems = computed<PqcInspectionItem[]>')
const itemMapEnd = panelSource.indexOf('const pqcInspectionItemMap', itemMapStart)
assert.ok(itemMapStart >= 0 && itemMapEnd > itemMapStart, 'PQC inspection item mapping block must exist.')
const itemMappingBlock = panelSource.slice(itemMapStart, itemMapEnd)

assert.match(
  itemMappingBlock,
  /itemName:\s*normalizePqcInspectionItemName\(item\.itemName\)/,
  'PQC item mapping must retain formal itemName separately from itemCode.'
)
assert.doesNotMatch(
  itemMappingBlock,
  /item\.itemName\s*\|\|\s*item\.itemCode/,
  'PQC visible item name must not fall back to internal itemCode.'
)

const tabLabelHelperStart = panelSource.indexOf('const formatPqcInspectionItemTabLabel')
const tabLabelHelperEnd = panelSource.indexOf('const formatPqcStandardSummary', tabLabelHelperStart)
assert.ok(tabLabelHelperStart >= 0 && tabLabelHelperEnd > tabLabelHelperStart, 'PQC tab label helper must exist.')
const tabLabelHelperBlock = panelSource.slice(tabLabelHelperStart, tabLabelHelperEnd)
assert.match(
  tabLabelHelperBlock,
  /formatPqcMethodSummary\(item\)/,
  'PQC visible tab title must display the formal inspection method, e.g. 目视检验.'
)
assert.doesNotMatch(
  tabLabelHelperBlock,
  /item\.itemName|item\.label|item\.key|itemCode/,
  'The tab label helper must not read itemName, label, itemCode, or key for visible text.'
)

assert.match(
  panelSource,
  /buildPqcItemDetailsPayload[\s\S]*itemCode: item\.key[\s\S]*itemName: item\.itemName[\s\S]*inspectionMethod: item\.inspectionMethod/,
  'PQC submit details must preserve itemCode identity and send itemName as a separate formal name.'
)

console.log('PASS: PQC tab displays formal inspection method while preserving item identity')
