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

assert.doesNotMatch(
  activePanelBlock,
  /class="pqc-active-summary"|<h3>|data-pqc-inspection-meta|formatPqcInspectionMeta\(activePqcTabItem\)/,
  'PQC active panel must not visibly render the red-box title/status summary.'
)
assert.doesNotMatch(
  activePanelBlock,
  /<h3>\{\{\s*activePqcTabItem\.label\s*\}\}<\/h3>/,
  'PQC active panel title must not render AO5 final inspection/itemName as the user-facing title.'
)

assert.match(
  panelSource,
  /const normalizePqcInspectionMethodLabel = \(inspectionMethod: string\) =>[\s\S]*['"]Visual inspection['"][\s\S]*['"]目视检验['"]/,
  'Visual inspection from the formal regulation snapshot must display as 目视检验.'
)
assert.match(
  panelSource,
  /const formatPqcMethodSummary = \(item: PqcInspectionItem\) =>\s*normalizePqcInspectionMethodLabel\(item\.processInspectionMethod\) \|\| '未配置检验方法'/,
  'The 检验方法 card must keep using the normalized formal QA process inspection method.'
)
assert.match(
  panelSource,
  /data-pqc-method-button[\s\S]*formatPqcMethodSummary\(activePqcTabItem\)/,
  'Hiding the red-box title must keep the formal 检验方法 card visible.'
)
assert.match(
  panelSource,
  /data-pqc-standard-button[\s\S]*formatPqcStandardSummary\(activePqcTabItem\)/,
  'Hiding the red-box title must keep the formal 接收标准 card visible.'
)
assert.match(
  panelSource,
  /class="frontline-pqc-choice-actions"[\s\S]*全部合格[\s\S]*全部不良[\s\S]*data-pqc-piece-open-button/,
  'Hiding the red-box title must keep bulk choices and piece inspection actions visible.'
)

const tabStart = panelSource.indexOf('data-pqc-inspection-tabs')
const tabEnd = panelSource.indexOf('</nav>', tabStart)
assert.ok(tabStart >= 0 && tabEnd > tabStart, 'PQC inspection tab block must exist.')
const tabBlock = panelSource.slice(tabStart, tabEnd)
assert.match(
  tabBlock,
  /<strong>\{\{\s*formatPqcInspectionItemTabLabel\(item\)\s*\}\}<\/strong>/,
  'PQC red-box item tabs must use the formal tab label helper.'
)

const tabLabelHelperStart = panelSource.indexOf('const formatPqcInspectionItemTabLabel')
const tabLabelHelperEnd = panelSource.indexOf('const formatPqcStandardSummary', tabLabelHelperStart)
assert.ok(tabLabelHelperStart >= 0 && tabLabelHelperEnd > tabLabelHelperStart, 'PQC tab label helper must exist.')
const tabLabelHelperBlock = panelSource.slice(tabLabelHelperStart, tabLabelHelperEnd)
assert.match(
  tabLabelHelperBlock,
  /item\.itemName\s*\|\|\s*'未配置检验项目名称'/,
  'The red-box tab helper must display the formal inspection item name while active title remains method-based.'
)

assert.match(
  panelSource,
  /buildPqcItemDetailsPayload[\s\S]*itemCode: item\.key[\s\S]*itemName: item\.itemName[\s\S]*inspectionMethod: item\.processInspectionMethod/,
  'Changing the visible title must not alter itemCode/itemName/processInspectionMethod submission identity.'
)

console.log('PASS: PQC active red-box summary is hidden while preserving item identity')
