const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const prototypeSource = read('../doc/tasks/20260805-pqc-redbox-ui-prototype/pqc-redbox-ui-prototype.html')

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
assert.match(
  panelSource,
  /data-pqc-tab-requirement[\s\S]*formatPqcTabRequirement/,
  'Each PQC tab must show the requirement field independently.'
)
assert.match(
  panelSource,
  /data-pqc-tab-progress[\s\S]*getPqcProgressText/,
  'Each PQC tab must show progress independently without relying on truncated meta text.'
)
assert.match(
  prototypeSource,
  /\.pqc-item-tab small[\s\S]*overflow:\s*visible[\s\S]*\.pqc-item-tab small span[\s\S]*overflow:\s*visible/,
  'The updated HTML preview must keep PQC tab requirement and progress fields fully visible.'
)
assert.match(
  panelSource,
  /\.pqc-item-tab\s*\{[\s\S]*small\s*\{[\s\S]*overflow:\s*visible[\s\S]*span\s*\{[\s\S]*overflow:\s*visible[\s\S]*white-space:\s*nowrap/,
  'The formal PQC tab layout must match the updated HTML preview by keeping requirement and progress fields fully visible.'
)
assert.match(
  panelSource,
  /\.frontline-operator-top[\s\S]*&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*340px\s+430px\s+minmax\(0,\s*1fr\)\s+210px/,
  'The formal PQC top cards must match the updated HTML preview proportions.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-number-field\s*\{[\s\S]*grid-template-columns:\s*128px\s+58px\s+minmax\(54px,\s*1fr\)\s+58px\s+42px[\s\S]*gap:\s*8px/,
  'The formal PQC quantity controls must match the compact updated HTML preview layout.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-type-tabs\s*\{[\s\S]*gap:\s*10px[\s\S]*button\s*\{[\s\S]*font-size:\s*32px/,
  'The formal PQC inspection type tabs must match the updated HTML preview size.'
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
