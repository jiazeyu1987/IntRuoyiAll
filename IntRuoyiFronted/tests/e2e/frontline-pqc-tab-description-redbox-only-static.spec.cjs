const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const panelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panelSource = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const tabStart = panelSource.indexOf('data-pqc-inspection-tabs')
const tabEnd = panelSource.indexOf('</nav>', tabStart)
assert.ok(tabStart >= 0 && tabEnd > tabStart, 'PQC inspection tab block must exist.')
const tabBlock = panelSource.slice(tabStart, tabEnd)

const tabButtonMatch = tabBlock.match(/<button[\s\S]*?data-pqc-inspection-tab[\s\S]*?<\/button>/)
assert.ok(tabButtonMatch, 'PQC inspection tab button block must exist.')
const tabButtonBlock = tabButtonMatch[0]

assert.match(
  tabButtonBlock,
  /<strong>\{\{\s*formatPqcInspectionItemTabLabel\(item\)\s*\}\}<\/strong>/,
  'PQC tab must keep the red-box formal item description/title.'
)
assert.doesNotMatch(
  tabButtonBlock,
  /<em>|getPqcTabStateLabel\(item\)|<small|data-pqc-tab-method|formatPqcMethodSummary\(item\)/,
  'PQC tab must not render status badges, method summaries, or extra description text outside the red-box title.'
)

const tabStyleStart = panelSource.indexOf('.pqc-item-tab {')
const tabStyleEnd = panelSource.indexOf('\n.frontline-pqc-fill-panel', tabStyleStart)
assert.ok(
  tabStyleStart >= 0 && tabStyleEnd > tabStyleStart,
  'PQC tab style block must be scoped and extractable.'
)
const tabStyleBlock = panelSource.slice(tabStyleStart, tabStyleEnd)

assert.doesNotMatch(
  tabStyleBlock,
  /\n\s*em\s*\{|\n\s*small\s*\{|grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto/,
  'PQC tab styles must not reserve or style hidden status/method description rows.'
)

console.log('PASS: frontline PQC tab only displays the red-box formal description/title')
