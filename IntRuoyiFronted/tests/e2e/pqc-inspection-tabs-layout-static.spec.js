const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

assert.ok(
  panelSource.includes('data-pqc-active-inspection-panel'),
  'PQC page must render a single active inspection detail panel instead of expanding every item.'
)
assert.ok(
  panelSource.includes('data-pqc-inspection-tabs'),
  'PQC page must expose an inspection tab strip for switching items.'
)
assert.match(
  panelSource,
  /v-for="item in pqcInspectionItems"[\s\S]*data-pqc-inspection-tab/,
  'PQC inspection items must render as tabs from the formal inspection item list.'
)
assert.match(
  panelSource,
  /class="pqc-item-tab"[\s\S]*activePqcTabKey === item\.key/,
  'PQC tabs must visibly mark the active inspection item.'
)
assert.match(
  panelSource,
  /\.pqc-item-tab[\s\S]*&\.active[\s\S]*background:\s*#fff4bf/,
  'The selected PQC tab must use a yellow background instead of a green top status bar.'
)
const activeTabBlockMatch = panelSource.match(
  /\.pqc-item-tab\s*\{[\s\S]*?&\.active\s*\{([\s\S]*?)\n\s*\}\n\n\s*&:focus-visible/
)
assert.ok(activeTabBlockMatch, 'The selected PQC tab style block must be defined.')
const activeTabBlock = activeTabBlockMatch[1]
assert.match(
  activeTabBlock,
  /&::before\s*\{[\s\S]*display:\s*none[\s\S]*background:\s*transparent/,
  'The selected PQC tab must hide the old top status pseudo-element.'
)
assert.doesNotMatch(
  activeTabBlock,
  /background:\s*#15815f/,
  'The selected PQC tab must not render the old green top bar.'
)
assert.match(
  panelSource,
  /grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/,
  'PQC tab strip must use a 5-column grid so 10 tabs render as two complete rows.'
)

const tabStart = panelSource.indexOf('data-pqc-inspection-tabs')
const tabEnd = panelSource.indexOf('</nav>', tabStart)
assert.ok(tabStart >= 0 && tabEnd > tabStart, 'PQC inspection tabs template block must exist.')
const tabBlock = panelSource.slice(tabStart, tabEnd)

assert.match(
  tabBlock,
  /<strong>\{\{\s*formatPqcInspectionItemTabLabel\(item\)\s*\}\}<\/strong>/,
  'Each PQC red-box tab title must render the formal item name helper.'
)
assert.doesNotMatch(
  tabBlock,
  /data-pqc-tab-requirement|formatPqcTabRequirement|data-pqc-tab-progress|getPqcProgressText\(item\.key\)|data-pqc-tab-method|formatPqcMethodSummary\(item\)|getPqcTabStateLabel\(item\)/,
  'PQC red-box tabs must only show the formal item name and must not show status, method, requirement, or progress text.'
)

const itemTabStyleMatch = panelSource.match(
  /\.pqc-item-tab\s*\{([\s\S]*?)\n\}\n\n\.frontline-pqc-fill-panel/
)
assert.ok(itemTabStyleMatch, 'The PQC red-box tab style block must be scoped and extractable.')
const itemTabStyleBlock = itemTabStyleMatch[1]
const itemTabTitleStyleMatch = itemTabStyleBlock.match(/\n\s*strong\s*\{([\s\S]*?)\n\s*\}/)
assert.ok(itemTabTitleStyleMatch, 'The PQC red-box tab title style block must exist.')
const itemTabTitleStyleBlock = itemTabTitleStyleMatch[1]
assert.match(
  itemTabTitleStyleBlock,
  /font-size:\s*20px/,
  'PQC red-box tab item names must use a smaller title size so full names can fit.'
)
assert.match(
  itemTabTitleStyleBlock,
  /white-space:\s*normal/,
  'PQC red-box tab item names must wrap instead of staying on one clipped line.'
)
assert.doesNotMatch(
  itemTabTitleStyleBlock,
  /text-overflow:\s*ellipsis|overflow:\s*hidden/,
  'PQC red-box tab item names must not be truncated with ellipsis.'
)

assert.match(
  itemTabStyleBlock,
  /grid-template-columns:\s*minmax\(0,\s*1fr\)/,
  'PQC red-box tab must reserve only one visible text column for the formal item name.'
)
assert.match(
  itemTabStyleBlock,
  /align-items:\s*center[\s\S]*text-align:\s*center/,
  'PQC red-box tab item names must be horizontally and vertically centered.'
)
assert.doesNotMatch(
  itemTabStyleBlock,
  /\n\s*em\s*\{|\n\s*small\s*\{|grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto/,
  'PQC red-box tab styles must not reserve or style hidden status/method description rows.'
)

assert.match(
  panelSource,
  /\.frontline-operator-top[\s\S]*&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(480px,\s*1\.55fr\)\s+minmax\(220px,\s*0\.85fr\)\s+minmax\(200px,\s*1fr\)\s+150px/,
  'The formal PQC top cards must match the current order-summary proportions.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-number-field\s*\{[\s\S]*grid-template-columns:\s*116px\s+52px\s+minmax\(42px,\s*1fr\)\s+52px\s+36px[\s\S]*gap:\s*6px/,
  'The formal PQC quantity controls must stay compact enough to avoid clipping in the real app shell.'
)
assert.match(
  panelSource,
  /\.frontline-operator-main\s*\{[\s\S]*&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(620px,\s*1\.28fr\)\s+minmax\(500px,\s*0\.92fr\)[\s\S]*grid-template-rows:\s*minmax\(0,\s*1fr\)\s+104px[\s\S]*gap:\s*22px\s+24px/,
  'The formal PQC normal layout must widen the fill panel and keep actions in the left-bottom grid row.'
)
assert.match(
  panelSource,
  /\.frontline-operator-panel\.is-pqc-fullscreen,\s*\n\.frontline-operator-panel:fullscreen\s*\{[\s\S]*padding:\s*26px[\s\S]*radial-gradient\(circle at 12% 10%/,
  'The formal PQC maximized canvas must use the same outer spacing and background as the updated HTML preview.'
)
assert.match(
  panelSource,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-screen\.is-pqc,[\s\S]*?\.frontline-operator-panel:fullscreen \.frontline-operator-screen\.is-pqc\s*\{[\s\S]*max-width:\s*1480px[\s\S]*min-height:\s*820px[\s\S]*grid-template-rows:\s*minmax\(118px,\s*auto\)\s+minmax\(0,\s*1fr\)[\s\S]*gap:\s*18px[\s\S]*padding:\s*24px[\s\S]*border-radius:\s*22px[\s\S]*box-shadow:\s*0 26px 70px/,
  'The formal PQC maximized operator screen must match the updated HTML preview frame.'
)
assert.match(
  panelSource,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-top\.is-pqc,[\s\S]*?\.frontline-operator-panel:fullscreen \.frontline-operator-top\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(480px,\s*1\.55fr\)\s+minmax\(220px,\s*0\.85fr\)\s+minmax\(200px,\s*1fr\)\s+150px[\s\S]*gap:\s*12px/,
  'The formal PQC maximized top cards must match the current order-summary proportions.'
)
assert.match(
  panelSource,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-pqc-number-field,[\s\S]*?\.frontline-operator-panel:fullscreen \.frontline-pqc-number-field\s*\{[\s\S]*grid-template-columns:\s*128px\s+58px\s+minmax\(54px,\s*1fr\)\s+58px\s+42px[\s\S]*gap:\s*8px/,
  'The formal PQC maximized quantity controls must match the updated HTML preview sizing.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-type-tabs\s*\{[\s\S]*gap:\s*10px[\s\S]*button\s*\{[\s\S]*font-size:\s*32px/,
  'The formal PQC inspection type tabs must match the updated HTML preview size.'
)
assert.match(
  panelSource,
  /<label for="frontlinePqcInspectionQuantity">检验<\/label>/,
  'The PQC inspection quantity label must be shortened to 检验 in the compact fill panel.'
)
assert.match(
  panelSource,
  /aria-label="检验减少"[\s\S]*aria-label="检验增加"/,
  'The PQC inspection quantity step buttons must use the shortened 检验 copy.'
)
assert.match(
  panelSource,
  /<label for="frontlinePqcScrapQuantity">损耗<\/label>/,
  'The PQC scrap quantity label must be shortened to 损耗 in the compact fill panel.'
)
assert.match(
  panelSource,
  /aria-label="损耗减少"[\s\S]*aria-label="损耗增加"/,
  'The PQC scrap quantity step buttons must use the shortened 损耗 copy.'
)
assert.match(
  panelSource,
  /<label for="frontlinePqcDefectDescription">不良<\/label>/,
  'The PQC defect description label must be shortened to 不良 in the compact fill panel.'
)
assert.doesNotMatch(
  panelSource,
  /<small>检验不合格或损耗数量大于 0 时必填，随本次 PQC 原始快照保存。<\/small>/,
  'The yellow-box helper text under the defect textarea must not be displayed.'
)
assert.doesNotMatch(
  panelSource,
  /<label for="frontlinePqcSignatureId">签名编号<\/label>|id="frontlinePqcSignatureId"|class="frontline-pqc-number-field is-signature"|\.frontline-pqc-number-field\.is-signature|\.frontline-pqc-defect-description small/,
  'The yellow-box signature field must not be displayed in the compact PQC fill panel.'
)
assert.ok(
  panelSource.includes('pqc-select-card') &&
    panelSource.includes('data-pqc-equipment-card') &&
    panelSource.includes('data-pqc-equipment-number-card'),
  'Equipment and equipment number controls must render as touch-style cards.'
)
assert.match(
  panelSource,
  /class="pqc-select-native"[\s\S]*data-pqc-equipment-select/,
  'The formal equipment select must remain available inside the styled card for real selection.'
)
assert.match(
  panelSource,
  /class="pqc-select-native"[\s\S]*data-pqc-equipment-number-select/,
  'The formal equipment number select must remain available inside the styled card for real selection.'
)
assert.match(
  panelSource,
  /\.pqc-select-native[\s\S]*opacity:\s*0/,
  'Native select controls must not visually appear as raw browser selects in the PQC red-box area.'
)
assert.ok(
  !/<h3>检验内容<\/h3>/.test(panelSource) && !/<h3>填检验<\/h3>/.test(panelSource),
  'Prototype-approved large section titles must not be shown in the compact PQC operator layout.'
)
assert.ok(
  !/<template v-for="item in pqcInspectionItems" :key="item.key">/.test(panelSource),
  'PQC page must not keep the old vertically expanded item list.'
)

console.log('PASS: PQC inspection tabs layout static contract')
