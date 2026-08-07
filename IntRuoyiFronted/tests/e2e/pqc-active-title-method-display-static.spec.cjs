const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panelSource = fs.readFileSync(panelPath, 'utf8')

const activePanelStart = panelSource.indexOf('data-pqc-active-inspection-panel')
const activePanelEnd = panelSource.indexOf('<div class="pqc-utility-strip"', activePanelStart)
assert.ok(activePanelStart >= 0 && activePanelEnd > activePanelStart, 'PQC active panel block must exist.')
const activePanelBlock = panelSource.slice(activePanelStart, activePanelEnd)

assert.match(
  activePanelBlock,
  /<h3>\{\{\s*formatPqcInspectionTitle\(activePqcTabItem\)\s*\}\}<\/h3>/,
  'PQC active panel title must render the formal inspection method, not the QA item name.'
)
assert.doesNotMatch(
  activePanelBlock,
  /<h3>\{\{\s*activePqcTabItem\.label\s*\}\}<\/h3>/,
  'PQC active panel title must not render AO5 final inspection/itemName as the user-facing title.'
)

assert.match(
  panelSource,
  /const formatPqcInspectionTitle = \(item: PqcInspectionItem\) =>\s*formatPqcMethodSummary\(item\)/,
  'The title helper must read the same formal inspectionMethod used by the 检验方法 card.'
)
assert.match(
  panelSource,
  /const normalizePqcInspectionMethodLabel = \(inspectionMethod: string\) =>[\s\S]*['"]Visual inspection['"][\s\S]*['"]目视检验['"]/,
  'Visual inspection from the formal regulation snapshot must display as 目视检验.'
)
assert.match(
  panelSource,
  /const formatPqcMethodSummary = \(item: PqcInspectionItem\) =>\s*normalizePqcInspectionMethodLabel\(item\.inspectionMethod\) \|\| '未配置检验方法'/,
  'The 检验方法 card and active title must share the same normalized display label.'
)

const tabStart = panelSource.indexOf('data-pqc-inspection-tabs')
const tabEnd = panelSource.indexOf('</nav>', tabStart)
assert.ok(tabStart >= 0 && tabEnd > tabStart, 'PQC inspection tab block must exist.')
const tabBlock = panelSource.slice(tabStart, tabEnd)
assert.match(
  tabBlock,
  /<strong>\{\{\s*formatPqcInspectionItemTabLabel\(item\)\s*\}\}<\/strong>/,
  'PQC red-box item tabs must display the formal inspection method, not AO5 final inspection.'
)

const tabLabelHelperStart = panelSource.indexOf('const formatPqcInspectionItemTabLabel')
const tabLabelHelperEnd = panelSource.indexOf('const formatPqcStandardSummary', tabLabelHelperStart)
assert.ok(tabLabelHelperStart >= 0 && tabLabelHelperEnd > tabLabelHelperStart, 'PQC tab label helper must exist.')
const tabLabelHelperBlock = panelSource.slice(tabLabelHelperStart, tabLabelHelperEnd)
assert.match(
  tabLabelHelperBlock,
  /formatPqcMethodSummary\(item\)/,
  'The red-box tab helper must share the same normalized inspection method label as the active title.'
)

assert.match(
  panelSource,
  /buildPqcItemDetailsPayload[\s\S]*itemCode: item\.key[\s\S]*itemName: item\.itemName[\s\S]*inspectionMethod: item\.inspectionMethod/,
  'Changing the visible title must not alter itemCode/itemName/inspectionMethod submission identity.'
)

console.log('PASS: PQC active title displays inspection method while preserving item identity')
